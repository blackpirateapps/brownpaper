package com.blackpirateapps.brownpaper.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.core.util.normalizeUrl
import com.blackpirateapps.brownpaper.core.util.toFtsQuery
import com.blackpirateapps.brownpaper.data.local.ArticleEntity
import com.blackpirateapps.brownpaper.data.local.ArticleTagCrossRef
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.FolderEntity
import com.blackpirateapps.brownpaper.data.local.PendingSyncOperationEntity
import com.blackpirateapps.brownpaper.data.local.TagEntity
import com.blackpirateapps.brownpaper.data.parser.JsoupArticleParser
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncOperationType
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncScheduler
import com.blackpirateapps.brownpaper.domain.model.AddArticleResult
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.domain.model.ArticleListFilter
import com.blackpirateapps.brownpaper.domain.model.ArticleSummary
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.blackpirateapps.brownpaper.data.local.BackupData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val dao: BrownPaperDao,
    private val parser: JsoupArticleParser,
    private val dispatchers: AppDispatchers,
    private val wallabagSyncScheduler: WallabagSyncScheduler,
) : ArticleRepository {

    override fun observeArticles(
        filter: ArticleListFilter,
        searchQuery: String,
    ): Flow<List<ArticleSummary>> = dao.observeArticles(buildArticleListQuery(filter, searchQuery))
        .map { articles ->
            articles.map { article ->
                ArticleSummary(
                    id = article.id,
                    title = article.title,
                    originalUrl = article.originalUrl,
                    dateAdded = article.dateAdded,
                    isLiked = article.isLiked,
                    isArchived = article.isArchived,
                    heroImageUrl = article.extractedHeroImageUrl,
                    excerpt = article.extractedTextContent
                        .replace('\n', ' ')
                        .trim()
                        .take(220),
                    isVideo = article.isVideo,
                    videoRuntimeText = article.videoRuntimeText,
                    channelName = article.channelName,
                    viewCount = article.viewCount,
                )
            }
        }

    override fun observeArticle(articleId: Long): Flow<ArticleDetail?> =
        dao.observeArticleWithRelations(articleId).map { relation ->
            relation?.let {
                ArticleDetail(
                    id = it.article.id,
                    title = it.article.title,
                    originalUrl = it.article.originalUrl,
                    dateAdded = it.article.dateAdded,
                    isLiked = it.article.isLiked,
                    isArchived = it.article.isArchived,
                    heroImageUrl = it.article.extractedHeroImageUrl,
                    bodyText = it.article.extractedTextContent,
                    folder = it.folder?.toDomain(),
                    tags = it.tags.map { tag -> tag.toDomain() },
                    isVideo = it.article.isVideo,
                    youtubeVideoId = it.article.youtubeVideoId,
                    videoRuntimeText = it.article.videoRuntimeText,
                    channelName = it.article.channelName,
                    viewCount = it.article.viewCount,
                    videoPositionSeconds = it.article.videoPositionSeconds,
                )
            }
        }

    override fun observeFolders(): Flow<List<Folder>> = dao.observeFolders().map { folders ->
        folders.map { folder -> folder.toDomain() }
    }

    override fun observeTags(): Flow<List<Tag>> = dao.observeTags().map { tags ->
        tags.map { tag -> tag.toDomain() }
    }

    override suspend fun addArticleFromUrl(url: String): AddArticleResult = withContext(dispatchers.io) {
        val normalizedUrl = url.normalizeUrl() ?: return@withContext AddArticleResult.InvalidUrl
        dao.getArticleByUrl(normalizedUrl)?.let { existing ->
            return@withContext AddArticleResult.AlreadySaved(existing.id)
        }

        runCatching {
            val parsed = parser.parse(normalizedUrl)
            val now = System.currentTimeMillis()
            val insertedId = dao.insertArticle(
                ArticleEntity(
                    title = parsed.title,
                    originalUrl = normalizedUrl,
                    dateAdded = now,
                    extractedTextContent = parsed.extractedTextContent,
                    extractedHeroImageUrl = parsed.extractedHeroImageUrl,
                    isVideo = parsed.isVideo,
                    youtubeVideoId = parsed.youtubeVideoId,
                    videoRuntimeText = parsed.videoRuntimeText,
                    channelName = parsed.channelName,
                    viewCount = parsed.viewCount,
                    localModifiedAt = now,
                ),
            )

            if (insertedId > 0) {
                enqueueSync(insertedId, WallabagSyncOperationType.UPSERT_ENTRY)
                AddArticleResult.Success(insertedId)
            } else {
                AddArticleResult.AlreadySaved(dao.getArticleByUrl(normalizedUrl)?.id)
            }
        }.getOrElse { throwable ->
            AddArticleResult.Failure(throwable.message ?: "Unable to save article")
        }
    }

    override suspend fun toggleLiked(articleId: Long) {
        withContext(dispatchers.io) {
            dao.toggleLiked(articleId, System.currentTimeMillis())
            enqueueSync(articleId, WallabagSyncOperationType.UPDATE_ENTRY)
        }
    }

    override suspend fun setArchived(articleId: Long, archived: Boolean) {
        withContext(dispatchers.io) {
            dao.setArchived(articleId, archived, System.currentTimeMillis())
            enqueueSync(articleId, WallabagSyncOperationType.UPDATE_ENTRY)
        }
    }

    override suspend fun assignTags(
        articleId: Long,
        selectedTagIds: Set<Long>,
        newTagNames: List<String>,
    ) {
        withContext(dispatchers.io) {
            val resolvedTagIds = buildSet {
                addAll(selectedTagIds)
                newTagNames
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach { name ->
                        val existing = dao.getTagByName(name)
                        val tagId = existing?.id ?: dao.insertTag(TagEntity(name = name))
                        if (tagId > 0) {
                            add(tagId)
                        } else {
                            dao.getTagByName(name)?.id?.let { existingId ->
                                add(existingId)
                            }
                        }
                    }
            }

            dao.clearTags(articleId)
            if (resolvedTagIds.isNotEmpty()) {
                dao.insertTagCrossRefs(
                    resolvedTagIds.map { tagId -> ArticleTagCrossRef(articleId = articleId, tagId = tagId) },
                )
            }
            dao.touchArticleLocalModifiedAt(articleId, System.currentTimeMillis())
            enqueueSync(articleId, WallabagSyncOperationType.UPDATE_ENTRY)
        }
    }

    override suspend fun moveToFolder(articleId: Long, folderId: Long?) {
        withContext(dispatchers.io) {
            dao.moveToFolder(articleId, folderId)
        }
    }

    override suspend fun updateVideoPosition(articleId: Long, position: Float) {
        withContext(dispatchers.io) {
            dao.updateVideoPosition(articleId, position)
        }
    }

    override suspend fun createFolder(name: String): Long? = withContext(dispatchers.io) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return@withContext null
        }

        val existing = dao.getFolderByName(normalizedName)
        if (existing != null) {
            return@withContext existing.id
        }

        val insertedId = dao.insertFolder(FolderEntity(name = normalizedName))
        if (insertedId > 0) insertedId else dao.getFolderByName(normalizedName)?.id
    }

    override suspend fun deleteArticle(articleId: Long) {
        withContext(dispatchers.io) {
            dao.deleteArticle(articleId)
        }
    }

    override suspend fun exportData(): String = withContext(dispatchers.io) {
        val data = BackupData(
            articles = dao.getAllArticles(),
            folders = dao.getAllFolders(),
            tags = dao.getAllTags(),
            tagCrossRefs = dao.getAllTagCrossRefs()
        )
        Json.encodeToString(data)
    }

    override suspend fun importData(jsonData: String) {
        withContext(dispatchers.io) {
            val data = Json.decodeFromString<BackupData>(jsonData)
            
            dao.deleteAllArticles()
            dao.deleteAllFolders()
            dao.deleteAllTags()
            
            dao.insertFolders(data.folders)
            dao.insertTags(data.tags)
            dao.insertArticles(data.articles)
            dao.insertTagCrossRefs(data.tagCrossRefs)
        }
    }

    private fun buildArticleListQuery(
        filter: ArticleListFilter,
        searchQuery: String,
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val sql = StringBuilder("SELECT DISTINCT a.* FROM articles a")

        if (filter is ArticleListFilter.TagFilter) {
            sql.append(" INNER JOIN article_tag_cross_ref atr ON atr.articleId = a.id")
        }

        if (searchQuery.isNotBlank()) {
            // No inner join needed for LIKE
        }

        val clauses = mutableListOf<String>()

        when (filter) {
            ArticleListFilter.Inbox -> clauses += "a.isArchived = 0"
            ArticleListFilter.Likes -> {
                clauses += "a.isLiked = 1"
            }
            ArticleListFilter.Archived -> clauses += "a.isArchived = 1"
            ArticleListFilter.Videos -> {
                clauses += "a.isArchived = 0"
                clauses += "a.isVideo = 1"
            }
            is ArticleListFilter.FolderFilter -> {
                clauses += "a.isArchived = 0"
                clauses += "a.folderId = ?"
                args += filter.folderId
            }
            is ArticleListFilter.TagFilter -> {
                clauses += "a.isArchived = 0"
                clauses += "atr.tagId = ?"
                args += filter.tagId
            }
        }

        if (searchQuery.isNotBlank()) {
            clauses += "(a.title LIKE ? OR a.extractedTextContent LIKE ?)"
            val likeTerm = "%${searchQuery.trim()}%"
            args += likeTerm
            args += likeTerm
        }

        if (clauses.isNotEmpty()) {
            sql.append(" WHERE ").append(clauses.joinToString(separator = " AND "))
        }

        sql.append(" ORDER BY a.dateAdded DESC")
        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
    }

    private fun FolderEntity.toDomain(): Folder = Folder(
        id = id,
        name = name,
    )

    private fun TagEntity.toDomain(): Tag = Tag(
        id = id,
        name = name,
    )

    private suspend fun enqueueSync(articleId: Long, operationType: WallabagSyncOperationType) {
        dao.insertPendingSyncOperation(
            PendingSyncOperationEntity(
                articleId = articleId,
                operationType = operationType.name,
                createdAt = System.currentTimeMillis(),
            ),
        )
        wallabagSyncScheduler.schedule()
    }
}
