package com.blackpirateapps.brownpaper.data.wallabag

import com.blackpirateapps.brownpaper.core.model.AppDispatchers
import com.blackpirateapps.brownpaper.core.util.normalizeUrl
import com.blackpirateapps.brownpaper.data.local.ArticleEntity
import com.blackpirateapps.brownpaper.data.local.ArticleTagCrossRef
import com.blackpirateapps.brownpaper.data.local.ArticleWithRelations
import com.blackpirateapps.brownpaper.data.local.BrownPaperDao
import com.blackpirateapps.brownpaper.data.local.TagEntity
import com.blackpirateapps.brownpaper.domain.repository.WallabagAccountState
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
                dao.deleteAllPendingSyncOperations()
                dao.deleteAllPendingWallabagDeleteOperations()
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
            dao.deleteAllPendingSyncOperations()
            dao.deleteAllPendingWallabagDeleteOperations()
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
                val pushed = deleted + pushPendingOperations(session, startedAt) + linkedOrCreated
                sessionStore.updateSession { it.copy(lastSyncAtMillis = startedAt) }
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
                upsertRemoteEntry(WallabagContentMapper.remoteEntryToDomain(dto), syncedAt)
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
            upsertRemoteEntry(WallabagContentMapper.remoteEntryToDomain(remoteEntry), syncedAt)
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

    private suspend fun upsertRemoteEntry(remote: WallabagRemoteEntry, syncedAt: Long) {
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
            return
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

private fun Throwable.userFacingMessage(): String = when (this) {
    is WallabagApiException -> when (code) {
        400, 401 -> "wallabag rejected those credentials."
        404 -> "That does not look like a wallabag API host."
        else -> message
    }
    else -> message ?: "wallabag sync failed."
}
