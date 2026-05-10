package com.blackpirateapps.brownpaper.data.repository

import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.data.local.ArticleAnnotationEntity
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.PendingWallabagAnnotationOperationEntity
import com.blackpirateapps.brownpaper.data.wallabag.WallabagAnnotationSyncOperationType
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncScheduler
import com.blackpirateapps.brownpaper.domain.model.AnnotationAnchor
import com.blackpirateapps.brownpaper.domain.model.AnnotationColor
import com.blackpirateapps.brownpaper.domain.model.AnnotationSyncState
import com.blackpirateapps.brownpaper.domain.model.ArticleAnnotation
import com.blackpirateapps.brownpaper.domain.repository.AnnotationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AnnotationRepositoryImpl @Inject constructor(
    private val dao: BrownPaperDao,
    private val dispatchers: AppDispatchers,
    private val wallabagSyncScheduler: WallabagSyncScheduler,
) : AnnotationRepository {

    override fun observeAnnotations(articleId: Long): Flow<List<ArticleAnnotation>> =
        dao.observeAnnotations(articleId).map { annotations ->
            annotations.map { annotation -> annotation.toDomain() }
        }

    override suspend fun createAnnotation(
        articleId: Long,
        anchor: AnnotationAnchor,
        quote: String,
        noteText: String,
        color: AnnotationColor,
    ): Long? = withContext(dispatchers.io) {
        val article = dao.getArticleById(articleId) ?: return@withContext null
        val normalizedAnchor = anchor.normalized()
        val now = System.currentTimeMillis()
        val insertedId = dao.insertAnnotation(
            ArticleAnnotationEntity(
                articleId = articleId,
                wallabagEntryId = article.wallabagEntryId,
                quote = quote.trim(),
                noteText = noteText.trim(),
                colorHex = color.hex,
                wallabagRangesJson = normalizedAnchor.toWallabagRangesJson(),
                startParagraphIndex = normalizedAnchor.startParagraphIndex,
                endParagraphIndex = normalizedAnchor.endParagraphIndex,
                startCharOffset = normalizedAnchor.startCharOffset,
                endCharOffset = normalizedAnchor.endCharOffset,
                prefixText = normalizedAnchor.prefixText,
                suffixText = normalizedAnchor.suffixText,
                bodyTextHash = article.extractedTextContent.hashCode().toString(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (insertedId > 0) {
            enqueueAnnotationSync(
                articleId = articleId,
                annotationId = insertedId,
                wallabagAnnotationId = null,
                operationType = WallabagAnnotationSyncOperationType.CREATE,
            )
        }
        insertedId.takeIf { it > 0 }
    }

    override suspend fun updateAnnotation(annotationId: Long, noteText: String, color: AnnotationColor) {
        withContext(dispatchers.io) {
            val annotation = dao.getAnnotationById(annotationId) ?: return@withContext
            val article = dao.getArticleById(annotation.articleId) ?: return@withContext
            val anchor = annotation.toAnchor()
            val now = System.currentTimeMillis()
            dao.updateAnnotationLocal(
                annotationId = annotationId,
                noteText = noteText.trim(),
                colorHex = color.hex,
                updatedAt = now,
                wallabagRangesJson = annotation.wallabagRangesJson.ifBlank { anchor.toWallabagRangesJson() },
                startParagraphIndex = anchor.startParagraphIndex,
                endParagraphIndex = anchor.endParagraphIndex,
                startCharOffset = anchor.startCharOffset,
                endCharOffset = anchor.endCharOffset,
                prefixText = anchor.prefixText,
                suffixText = anchor.suffixText,
                bodyTextHash = article.extractedTextContent.hashCode().toString(),
            )
            enqueueAnnotationSync(
                articleId = annotation.articleId,
                annotationId = annotationId,
                wallabagAnnotationId = annotation.wallabagAnnotationId,
                operationType = if (annotation.wallabagAnnotationId == null) {
                    WallabagAnnotationSyncOperationType.CREATE
                } else {
                    WallabagAnnotationSyncOperationType.UPDATE
                },
            )
        }
    }

    override suspend fun deleteAnnotation(annotationId: Long) {
        withContext(dispatchers.io) {
            val annotation = dao.getAnnotationById(annotationId) ?: return@withContext
            if (annotation.wallabagAnnotationId == null) {
                dao.deletePendingAnnotationOperationsForAnnotation(annotationId)
                dao.deleteAnnotationById(annotationId)
                return@withContext
            }

            val now = System.currentTimeMillis()
            dao.tombstoneAnnotation(annotationId, now)
            enqueueAnnotationSync(
                articleId = annotation.articleId,
                annotationId = annotationId,
                wallabagAnnotationId = annotation.wallabagAnnotationId,
                operationType = WallabagAnnotationSyncOperationType.DELETE,
            )
        }
    }

    override suspend fun scheduleSyncAnnotationsForArticle(articleId: Long) {
        withContext(dispatchers.io) {
            wallabagSyncScheduler.schedule()
        }
    }

    private suspend fun enqueueAnnotationSync(
        articleId: Long,
        annotationId: Long,
        wallabagAnnotationId: String?,
        operationType: WallabagAnnotationSyncOperationType,
    ) {
        dao.deletePendingAnnotationOperationsForAnnotation(annotationId)
        dao.insertPendingWallabagAnnotationOperation(
            PendingWallabagAnnotationOperationEntity(
                articleId = articleId,
                annotationId = annotationId,
                wallabagAnnotationId = wallabagAnnotationId,
                operationType = operationType.name,
                createdAt = System.currentTimeMillis(),
            ),
        )
        wallabagSyncScheduler.schedule()
    }
}

fun ArticleAnnotationEntity.toDomain(): ArticleAnnotation {
    val syncState = when {
        deletedAt != null -> AnnotationSyncState.Pending
        wallabagAnnotationId == null -> AnnotationSyncState.LocalOnly
        lastSyncedAt == null || updatedAt > lastSyncedAt -> AnnotationSyncState.Pending
        else -> AnnotationSyncState.Synced
    }
    return ArticleAnnotation(
        id = id,
        articleId = articleId,
        quote = quote,
        noteText = noteText,
        color = AnnotationColor.fromHex(colorHex),
        anchor = toAnchor(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = syncState,
    )
}

fun ArticleAnnotationEntity.toAnchor(): AnnotationAnchor = AnnotationAnchor(
    startParagraphIndex = startParagraphIndex,
    endParagraphIndex = endParagraphIndex,
    startCharOffset = startCharOffset,
    endCharOffset = endCharOffset,
    prefixText = prefixText.orEmpty(),
    suffixText = suffixText.orEmpty(),
).normalized()

fun AnnotationAnchor.normalized(): AnnotationAnchor {
    val startsAfterEnd = startParagraphIndex > endParagraphIndex ||
        (startParagraphIndex == endParagraphIndex && startCharOffset > endCharOffset)
    return if (startsAfterEnd) {
        copy(
            startParagraphIndex = endParagraphIndex,
            endParagraphIndex = startParagraphIndex,
            startCharOffset = endCharOffset,
            endCharOffset = startCharOffset,
        )
    } else {
        this
    }
}

fun AnnotationAnchor.toWallabagRangesJson(): String {
    val startPath = "/p[${startParagraphIndex + 1}]"
    val endPath = "/p[${endParagraphIndex + 1}]"
    return """
        [{"start":"$startPath","end":"$endPath","startOffset":$startCharOffset,"endOffset":$endCharOffset}]
    """.trimIndent()
}

fun findAnnotationAnchor(bodyText: String, quote: String): AnnotationAnchor {
    val paragraphs = bodyText.toReaderParagraphs()
    val trimmedQuote = quote.trim()
    if (trimmedQuote.isBlank()) {
        return AnnotationAnchor(0, 0, 0, 0)
    }

    paragraphs.forEachIndexed { index, paragraph ->
        val start = paragraph.indexOf(trimmedQuote, ignoreCase = false)
        if (start >= 0) {
            return AnnotationAnchor(
                startParagraphIndex = index,
                endParagraphIndex = index,
                startCharOffset = start,
                endCharOffset = start + trimmedQuote.length,
                prefixText = paragraph.substring(0, start).takeLast(32),
                suffixText = paragraph.substring(start + trimmedQuote.length).take(32),
            )
        }
    }

    val joined = paragraphs.joinToString(separator = "\n\n")
    val globalStart = joined.indexOf(trimmedQuote, ignoreCase = false)
    if (globalStart >= 0) {
        return joinedRangeToAnchor(paragraphs, globalStart, globalStart + trimmedQuote.length)
    }

    return AnnotationAnchor(
        startParagraphIndex = 0,
        endParagraphIndex = 0,
        startCharOffset = 0,
        endCharOffset = trimmedQuote.length.coerceAtMost(paragraphs.firstOrNull()?.length ?: 0),
    )
}

fun String.toReaderParagraphs(): List<String> =
    split(Regex("\n\\s*\n"))
        .map(String::trim)
        .filter(String::isNotBlank)

private fun joinedRangeToAnchor(paragraphs: List<String>, globalStart: Int, globalEnd: Int): AnnotationAnchor {
    var cursor = 0
    var startParagraph = 0
    var endParagraph = 0
    var startOffset = 0
    var endOffset = 0

    paragraphs.forEachIndexed { index, paragraph ->
        val paragraphStart = cursor
        val paragraphEnd = cursor + paragraph.length
        if (globalStart in paragraphStart..paragraphEnd) {
            startParagraph = index
            startOffset = (globalStart - paragraphStart).coerceIn(0, paragraph.length)
        }
        if (globalEnd in paragraphStart..paragraphEnd) {
            endParagraph = index
            endOffset = (globalEnd - paragraphStart).coerceIn(0, paragraph.length)
            return@forEachIndexed
        }
        cursor = paragraphEnd + 2
    }

    val startText = paragraphs.getOrNull(startParagraph).orEmpty()
    val endText = paragraphs.getOrNull(endParagraph).orEmpty()
    return AnnotationAnchor(
        startParagraphIndex = startParagraph,
        endParagraphIndex = endParagraph,
        startCharOffset = startOffset,
        endCharOffset = endOffset,
        prefixText = startText.substring(0, startOffset.coerceAtMost(startText.length)).takeLast(32),
        suffixText = endText.substring(endOffset.coerceAtMost(endText.length)).take(32),
    )
}
