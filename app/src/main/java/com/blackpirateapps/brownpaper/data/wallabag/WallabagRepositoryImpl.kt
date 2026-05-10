package com.blackpirateapps.brownpaper.data.wallabag

import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.core.util.normalizeUrl
import com.blackpirateapps.brownpaper.data.local.ArticleAnnotationEntity
import com.blackpirateapps.brownpaper.data.local.ArticleEntity
import com.blackpirateapps.brownpaper.data.local.ArticleTagCrossRef
import com.blackpirateapps.brownpaper.data.local.ArticleWithRelations
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.TagEntity
import com.blackpirateapps.brownpaper.data.repository.findAnnotationAnchor
import com.blackpirateapps.brownpaper.data.repository.toAnchor
import com.blackpirateapps.brownpaper.data.repository.toWallabagRangesJson
import com.blackpirateapps.brownpaper.domain.repository.WallabagAccountState
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Singleton
class WallabagRepositoryImpl @Inject constructor(
    private val apiClient: WallabagApiClient,
    private val authenticator: WallabagAuthenticator,
    private val sessionStore: WallabagSessionStore,
    private val dao: BrownPaperDao,
    private val scheduler: WallabagSyncScheduler,
    private val dispatchers: AppDispatchers,
) : WallabagRepository {
    private val syncMutex = Mutex()

    override val accountState: Flow<WallabagAccountState> = sessionStore.session.map { session ->
        if (session == null) {
            WallabagAccountState()
        } else {
            WallabagAccountState(
                isConnected = true,
                host = session.host,
                username = session.username,
                lastSyncAtMillis = session.lastSyncAtMillis,
            )
        }
    }

    override suspend fun login(
        host: String,
        username: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ): WallabagLoginResult = withContext(dispatchers.io) {
        val normalizedHost = WallabagHostNormalizer.normalize(host)
            ?: return@withContext WallabagLoginResult.InvalidHost("Enter a valid wallabag host.")

        if (clientId.isBlank() || clientSecret.isBlank()) {
            return@withContext WallabagLoginResult.MissingClientCredentials
        }

        runCatching {
            val oldSession = sessionStore.readSession()
            val now = System.currentTimeMillis()
            val token = apiClient.login(
                host = normalizedHost,
                clientId = clientId.trim(),
                clientSecret = clientSecret.trim(),
                username = username.trim(),
                password = password,
            )
            val tempSession = WallabagSession(
                host = normalizedHost,
                username = username.trim(),
                clientId = clientId.trim(),
                clientSecret = clientSecret.trim(),
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                expiresAtMillis = now + token.expiresIn * 1_000L,
                lastSyncAtMillis = if (oldSession?.host == normalizedHost) oldSession.lastSyncAtMillis else 0,
            )
            val remoteUser = runCatching { apiClient.getUser(tempSession) }.getOrNull()
            val finalSession = tempSession.copy(
                username = remoteUser?.username?.takeIf(String::isNotBlank) ?: tempSession.username,
            )

            if (oldSession != null && oldSession.host != normalizedHost) {
                dao.clearWallabagMetadata()
                dao.clearWallabagAnnotationMetadata()
                dao.deleteAllPendingSyncOperations()
                dao.deleteAllPendingWallabagDeleteOperations()
                dao.deleteAllPendingWallabagAnnotationOperations()
            }

            sessionStore.saveSession(finalSession)
            scheduler.schedule()
            WallabagLoginResult.Success
        }.getOrElse { throwable ->
            WallabagLoginResult.Failure(throwable.userFacingMessage())
        }
    }

    override suspend fun disconnect() {
        sessionStore.clear()
        withContext(dispatchers.io) {
            dao.clearWallabagMetadata()
            dao.clearWallabagAnnotationMetadata()
            dao.deleteAllPendingSyncOperations()
            dao.deleteAllPendingWallabagDeleteOperations()
            dao.deleteAllPendingWallabagAnnotationOperations()
        }
    }

    override fun scheduleSync() {
        scheduler.schedule()
    }

    override suspend fun syncNow(): WallabagSyncResult = syncMutex.withLock {
        withContext(dispatchers.io) {
            val startedAt = System.currentTimeMillis()
            val session = runCatching { authenticator.authorizedSession(startedAt) }
                .getOrElse { return@withContext WallabagSyncResult.Failure(it.userFacingMessage()) }
                ?: return@withContext WallabagSyncResult.NotConnected

            runCatching {
                val deleted = pushPendingDeletes(session)
                val pulled = pullRemoteEntries(session, startedAt)
                val linkedOrCreated = linkOrCreateLocalArticles(session, startedAt)
                val pushed = deleted +
                    pushPendingOperations(session, startedAt) +
                    pushPendingAnnotationOperations(session, startedAt) +
                    linkedOrCreated
                sessionStore.updateSession { it.copy(lastSyncAtMillis = startedAt) }
                WallabagSyncResult.Success(pulled = pulled, pushed = pushed)
            }.getOrElse { throwable ->
                WallabagSyncResult.Failure(throwable.userFacingMessage())
            }
        }
    }

    override suspend fun syncAnnotationsForArticle(articleId: Long): WallabagSyncResult = syncMutex.withLock {
        withContext(dispatchers.io) {
            val startedAt = System.currentTimeMillis()
            val session = runCatching { authenticator.authorizedSession(startedAt) }
                .getOrElse { return@withContext WallabagSyncResult.Failure(it.userFacingMessage()) }
                ?: return@withContext WallabagSyncResult.NotConnected
            val article = dao.getArticleById(articleId)
                ?: return@withContext WallabagSyncResult.Success(pulled = 0, pushed = 0)
            val entryId = article.wallabagEntryId
                ?: return@withContext WallabagSyncResult.Success(pulled = 0, pushed = 0)

            runCatching {
                val pulled = pullRemoteAnnotationsForEntry(
                    session = session,
                    articleId = articleId,
                    wallabagEntryId = entryId,
                    syncedAt = startedAt,
                )
                val pushed = pushPendingAnnotationOperations(session, startedAt, articleId)
                WallabagSyncResult.Success(pulled = pulled, pushed = pushed)
            }.getOrElse { throwable ->
                WallabagSyncResult.Failure(throwable.userFacingMessage())
            }
        }
    }

    private suspend fun pushPendingDeletes(session: WallabagSession): Int {
        var deleted = 0
        dao.getPendingWallabagDeleteOperations().forEach { operation ->
            try {
                apiClient.deleteEntry(session, operation.wallabagEntryId)
                dao.deletePendingWallabagDeleteOperation(operation.id)
                deleted += 1
            } catch (throwable: Throwable) {
                if (throwable is WallabagApiException && throwable.code == 404) {
                    dao.deletePendingWallabagDeleteOperation(operation.id)
                    deleted += 1
                } else {
                    dao.markPendingWallabagDeleteOperationFailed(operation.id, throwable.userFacingMessage())
                    throw throwable
                }
            }
        }
        return deleted
    }

    private suspend fun pullRemoteEntries(session: WallabagSession, syncedAt: Long): Int {
        var page = 1
        var pages = 1
        var pulled = 0
        val since = session.lastSyncAtMillis.takeIf { it > 0 }?.div(1_000L)

        while (page <= pages) {
            val response = apiClient.getEntries(session, page = page, sinceEpochSeconds = since)
            pages = response.pages.coerceAtLeast(1)
            response.embedded.items.forEach { dto ->
                val articleId = upsertRemoteEntry(WallabagContentMapper.remoteEntryToDomain(dto), syncedAt)
                if (articleId > 0) {
                    pullRemoteAnnotationsForEntry(
                        session = session,
                        articleId = articleId,
                        wallabagEntryId = dto.id,
                        syncedAt = syncedAt,
                        embeddedAnnotations = dto.annotations,
                    )
                }
                pulled += 1
            }
            page += 1
        }

        return pulled
    }

    private suspend fun linkOrCreateLocalArticles(session: WallabagSession, syncedAt: Long): Int {
        var changed = 0
        dao.getArticlesWithoutWallabagEntryId().forEach { article ->
            val relation = dao.getArticleWithRelationsById(article.id) ?: return@forEach
            val localArticle = relation.toLocalWallabagArticle()
            val remoteId = apiClient.findEntryId(session, article.originalUrl)
            val createdRemote = remoteId == null
            val remoteEntry = if (remoteId != null) {
                apiClient.getEntry(session, remoteId)
            } else {
                apiClient.createEntry(session, localArticle)
            }
            val articleId = upsertRemoteEntry(WallabagContentMapper.remoteEntryToDomain(remoteEntry), syncedAt)
            if (articleId > 0) {
                pullRemoteAnnotationsForEntry(
                    session = session,
                    articleId = articleId,
                    wallabagEntryId = remoteEntry.id,
                    syncedAt = syncedAt,
                    embeddedAnnotations = remoteEntry.annotations,
                )
            }
            if (createdRemote) {
                dao.deletePendingSyncOperationsForArticle(article.id)
            }
            changed += 1
        }
        return changed
    }

    private suspend fun pushPendingOperations(session: WallabagSession, syncedAt: Long): Int {
        var pushed = 0
        dao.getPendingSyncOperations().forEach { operation ->
            val relation = dao.getArticleWithRelationsById(operation.articleId)
            if (relation == null) {
                dao.deletePendingSyncOperation(operation.id)
                return@forEach
            }

            val entryId = relation.article.wallabagEntryId
            if (entryId == null) {
                dao.markPendingSyncOperationFailed(operation.id, "Article is not linked to wallabag yet.")
                return@forEach
            }

            try {
                val patched = apiClient.patchEntry(
                    session = session,
                    entryId = entryId,
                    article = relation.toLocalWallabagArticle(),
                )
                upsertRemoteEntry(WallabagContentMapper.remoteEntryToDomain(patched), syncedAt)
                dao.deletePendingSyncOperationsForArticle(operation.articleId)
                pushed += 1
            } catch (throwable: Throwable) {
                dao.markPendingSyncOperationFailed(operation.id, throwable.userFacingMessage())
                throw throwable
            }
        }
        return pushed
    }

    private suspend fun pullRemoteAnnotationsForEntry(
        session: WallabagSession,
        articleId: Long,
        wallabagEntryId: Long,
        syncedAt: Long,
        embeddedAnnotations: List<WallabagAnnotationDto> = emptyList(),
    ): Int {
        val remoteAnnotations = embeddedAnnotations.takeIf { it.isNotEmpty() }
            ?: try {
                apiClient.getAnnotations(session, wallabagEntryId)
            } catch (throwable: WallabagApiException) {
                if (throwable.code == 404) emptyList() else throw throwable
            }
        val article = dao.getArticleById(articleId) ?: return 0
        val remoteIds = remoteAnnotations.mapNotNull { it.remoteId() }.toSet()

        dao.getRemoteAnnotationsForArticle(articleId).forEach { local ->
            val remoteId = local.wallabagAnnotationId
            if (remoteId != null && remoteId !in remoteIds && dao.countPendingAnnotationOperations(local.id) == 0) {
                dao.deleteAnnotationById(local.id)
            }
        }

        remoteAnnotations.forEach { remote ->
            val remoteId = remote.remoteId() ?: return@forEach
            val existing = dao.getAnnotationByWallabagId(remoteId)
            if (existing != null && dao.countPendingAnnotationOperations(existing.id) > 0) {
                return@forEach
            }
            val quote = remote.quoteText().ifBlank { existing?.quote.orEmpty() }
            val noteText = remote.noteText()
            val remoteUpdatedAt = remote.updatedAtMillis() ?: syncedAt
            val remoteCreatedAt = remote.createdAtMillis() ?: remoteUpdatedAt
            val anchor = if (existing != null && existing.quote == quote) {
                existing.toAnchor()
            } else {
                findAnnotationAnchor(article.extractedTextContent, quote)
            }
            val rangesJson = remote.rangesJson().ifBlank { anchor.toWallabagRangesJson() }
            val entity = ArticleAnnotationEntity(
                id = existing?.id ?: 0,
                articleId = articleId,
                wallabagAnnotationId = remoteId,
                wallabagEntryId = wallabagEntryId,
                quote = quote,
                noteText = noteText,
                colorHex = existing?.colorHex ?: DefaultAnnotationColor,
                wallabagRangesJson = rangesJson,
                startParagraphIndex = anchor.startParagraphIndex,
                endParagraphIndex = anchor.endParagraphIndex,
                startCharOffset = anchor.startCharOffset,
                endCharOffset = anchor.endCharOffset,
                prefixText = anchor.prefixText,
                suffixText = anchor.suffixText,
                bodyTextHash = article.extractedTextContent.hashCode().toString(),
                createdAt = existing?.createdAt ?: remoteCreatedAt,
                updatedAt = remoteUpdatedAt,
                remoteUpdatedAt = remoteUpdatedAt,
                lastSyncedAt = syncedAt,
                deletedAt = null,
            )
            dao.insertAnnotation(entity)
        }

        return remoteAnnotations.size
    }

    private suspend fun pushPendingAnnotationOperations(
        session: WallabagSession,
        syncedAt: Long,
        articleId: Long? = null,
    ): Int {
        var pushed = 0
        val operations = if (articleId == null) {
            dao.getPendingWallabagAnnotationOperations()
        } else {
            dao.getPendingWallabagAnnotationOperationsForArticle(articleId)
        }

        operations.forEach { operation ->
            val article = dao.getArticleById(operation.articleId)
            if (article == null) {
                dao.deletePendingWallabagAnnotationOperation(operation.id)
                return@forEach
            }
            val entryId = article.wallabagEntryId
            if (entryId == null) {
                dao.markPendingWallabagAnnotationOperationFailed(
                    operation.id,
                    "Article is not linked to wallabag yet.",
                )
                return@forEach
            }
            val annotation = operation.annotationId?.let { dao.getAnnotationById(it) }

            try {
                when (operation.operationType) {
                    WallabagAnnotationSyncOperationType.DELETE.name -> {
                        val remoteId = operation.wallabagAnnotationId ?: annotation?.wallabagAnnotationId
                        if (remoteId != null) {
                            try {
                                apiClient.deleteAnnotation(session, remoteId)
                            } catch (throwable: Throwable) {
                                if (throwable !is WallabagApiException || throwable.code != 404) {
                                    throw throwable
                                }
                            }
                        }
                        operation.annotationId?.let { dao.deleteAnnotationById(it) }
                        dao.deletePendingWallabagAnnotationOperation(operation.id)
                        pushed += 1
                    }
                    WallabagAnnotationSyncOperationType.CREATE.name,
                    WallabagAnnotationSyncOperationType.UPDATE.name -> {
                        if (annotation == null) {
                            dao.deletePendingWallabagAnnotationOperation(operation.id)
                            return@forEach
                        }
                        if (annotation.deletedAt != null) {
                            dao.deletePendingWallabagAnnotationOperation(operation.id)
                            return@forEach
                        }
                        val request = annotation.toLocalWallabagAnnotation()
                        val remote = if (
                            operation.operationType == WallabagAnnotationSyncOperationType.UPDATE.name &&
                            annotation.wallabagAnnotationId != null
                        ) {
                            apiClient.updateAnnotation(session, annotation.wallabagAnnotationId, request)
                        } else {
                            apiClient.createAnnotation(session, entryId, request)
                        }
                        val remoteId = remote.remoteId() ?: annotation.wallabagAnnotationId
                        dao.updateAnnotationRemoteMetadata(
                            annotationId = annotation.id,
                            wallabagAnnotationId = remoteId,
                            wallabagEntryId = entryId,
                            wallabagRangesJson = remote.rangesJson().ifBlank { annotation.wallabagRangesJson },
                            remoteUpdatedAt = remote.updatedAtMillis() ?: syncedAt,
                            lastSyncedAt = syncedAt,
                        )
                        dao.deletePendingWallabagAnnotationOperation(operation.id)
                        pushed += 1
                    }
                    else -> dao.deletePendingWallabagAnnotationOperation(operation.id)
                }
            } catch (throwable: Throwable) {
                dao.markPendingWallabagAnnotationOperationFailed(operation.id, throwable.userFacingMessage())
                throw throwable
            }
        }
        return pushed
    }

    private suspend fun upsertRemoteEntry(remote: WallabagRemoteEntry, syncedAt: Long): Long {
        val normalizedUrl = remote.url.normalizeUrl() ?: remote.url
        val existing = dao.getArticleByWallabagEntryId(remote.id)
            ?: dao.getArticleByUrl(normalizedUrl)
        val remoteUpdatedAt = remote.updatedAtMillis ?: syncedAt
        val createdAt = remote.createdAtMillis ?: remoteUpdatedAt

        if (existing == null) {
            val insertedId = dao.insertArticle(
                ArticleEntity(
                    title = remote.title,
                    originalUrl = normalizedUrl,
                    dateAdded = createdAt,
                    isLiked = remote.isStarred,
                    isArchived = remote.isArchived,
                    extractedTextContent = remote.readerText,
                    extractedHeroImageUrl = remote.previewPicture,
                    wallabagEntryId = remote.id,
                    remoteUpdatedAt = remoteUpdatedAt,
                    lastSyncedAt = syncedAt,
                    localModifiedAt = remoteUpdatedAt,
                ),
            )
            if (insertedId > 0) {
                replaceTags(insertedId, remote.tags)
            }
            return insertedId.takeIf { it > 0 }
                ?: dao.getArticleByWallabagEntryId(remote.id)?.id
                ?: dao.getArticleByUrl(normalizedUrl)?.id
                ?: 0
        }

        val hasPendingLocalChanges = dao.countPendingSyncOperations(existing.id) > 0 &&
            existing.localModifiedAt > remoteUpdatedAt

        if (hasPendingLocalChanges) {
            dao.updateArticleContentFromWallabagKeepingLocalState(
                articleId = existing.id,
                title = remote.title,
                originalUrl = normalizedUrl,
                dateAdded = existing.dateAdded,
                extractedTextContent = remote.readerText,
                extractedHeroImageUrl = remote.previewPicture,
                wallabagEntryId = remote.id,
                remoteUpdatedAt = remoteUpdatedAt,
                lastSyncedAt = syncedAt,
            )
        } else {
            dao.updateArticleFromWallabag(
                articleId = existing.id,
                title = remote.title,
                originalUrl = normalizedUrl,
                dateAdded = existing.dateAdded.takeIf { it > 0 } ?: createdAt,
                isLiked = remote.isStarred,
                isArchived = remote.isArchived,
                extractedTextContent = remote.readerText,
                extractedHeroImageUrl = remote.previewPicture,
                wallabagEntryId = remote.id,
                remoteUpdatedAt = remoteUpdatedAt,
                lastSyncedAt = syncedAt,
                localModifiedAt = remoteUpdatedAt,
            )
            replaceTags(existing.id, remote.tags)
        }
        return existing.id
    }

    private suspend fun replaceTags(articleId: Long, tagNames: List<String>) {
        dao.clearTags(articleId)
        val tagIds = tagNames
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .mapNotNull { name ->
                val existing = dao.getTagByName(name)
                existing?.id ?: dao.insertTag(TagEntity(name = name)).takeIf { it > 0 }
                    ?: dao.getTagByName(name)?.id
            }

        if (tagIds.isNotEmpty()) {
            dao.insertTagCrossRefs(tagIds.map { tagId -> ArticleTagCrossRef(articleId, tagId) })
        }
    }

    private fun ArticleWithRelations.toLocalWallabagArticle(): LocalWallabagArticle = LocalWallabagArticle(
        url = article.originalUrl,
        title = article.title,
        content = article.extractedTextContent
            .split(Regex("\n\\s*\n"))
            .joinToString(separator = "") { paragraph -> "<p>${paragraph.escapeHtml()}</p>" },
        previewPicture = article.extractedHeroImageUrl,
        isArchived = article.isArchived,
        isStarred = article.isLiked,
        tags = tags.map { it.name },
    )
}

private fun ArticleAnnotationEntity.toLocalWallabagAnnotation(): LocalWallabagAnnotation = LocalWallabagAnnotation(
    quote = quote,
    text = noteText,
    rangesJson = wallabagRangesJson.ifBlank { toAnchor().toWallabagRangesJson() },
)

private fun WallabagAnnotationDto.remoteId(): String? = id.flexibleText()?.takeIf(String::isNotBlank)

private fun WallabagAnnotationDto.quoteText(): String = quote.flexibleText()

private fun WallabagAnnotationDto.noteText(): String = text.flexibleText()

private fun WallabagAnnotationDto.rangesJson(): String = ranges?.toString().orEmpty()

private fun WallabagAnnotationDto.createdAtMillis(): Long? = parseWallabagTimestamp(createdAt)

private fun WallabagAnnotationDto.updatedAtMillis(): Long? = parseWallabagTimestamp(updatedAt)

private fun JsonElement?.flexibleText(): String {
    val element = this ?: return ""
    element.jsonPrimitiveOrNull()?.let { primitive ->
        primitive.contentOrNull?.let { return it }
        primitive.longOrNull?.let { return it.toString() }
    }
    element.jsonArrayOrNull()?.let { array ->
        return array.joinToString(separator = "\n") { item -> item.flexibleText() }
    }
    return element.toString().trim('"')
}

private fun JsonElement.jsonPrimitiveOrNull() = runCatching { jsonPrimitive }.getOrNull()

private fun JsonElement.jsonArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()

private fun String.escapeHtml(): String = buildString {
    this@escapeHtml.forEach { char ->
        append(
            when (char) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> char
            },
        )
    }
}

private const val DefaultAnnotationColor = "#F6D365"

private fun Throwable.userFacingMessage(): String = when (this) {
    is WallabagApiException -> when (code) {
        400, 401 -> "wallabag rejected those credentials."
        404 -> "That does not look like a wallabag API host."
        else -> message
    }
    else -> message ?: "wallabag sync failed."
}
