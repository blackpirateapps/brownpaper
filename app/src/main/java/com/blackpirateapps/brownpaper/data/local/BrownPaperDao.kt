package com.blackpirateapps.brownpaper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface BrownPaperDao {
    @RawQuery(
        observedEntities = [
            ArticleEntity::class,
            ArticleFtsEntity::class,
            ArticleTagCrossRef::class,
        ],
    )
    fun observeArticles(query: SupportSQLiteQuery): Flow<List<ArticleEntity>>

    @Transaction
    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    fun observeArticleWithRelations(articleId: Long): Flow<ArticleWithRelations?>

    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    suspend fun getArticleById(articleId: Long): ArticleEntity?

    @Transaction
    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    suspend fun getArticleWithRelationsById(articleId: Long): ArticleWithRelations?

    @Query("SELECT * FROM articles WHERE originalUrl = :originalUrl LIMIT 1")
    suspend fun getArticleByUrl(originalUrl: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE wallabagEntryId = :wallabagEntryId LIMIT 1")
    suspend fun getArticleByWallabagEntryId(wallabagEntryId: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE wallabagEntryId IS NULL")
    suspend fun getArticlesWithoutWallabagEntryId(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: ArticleEntity): Long

    @Query(
        """
        UPDATE articles
        SET isLiked = CASE WHEN isLiked = 1 THEN 0 ELSE 1 END,
            localModifiedAt = :modifiedAt
        WHERE id = :articleId
        """,
    )
    suspend fun toggleLiked(articleId: Long, modifiedAt: Long)

    @Query("UPDATE articles SET isArchived = :archived, localModifiedAt = :modifiedAt WHERE id = :articleId")
    suspend fun setArchived(articleId: Long, archived: Boolean, modifiedAt: Long)

    @Query("UPDATE articles SET folderId = :folderId WHERE id = :articleId")
    suspend fun moveToFolder(articleId: Long, folderId: Long?)

    @Query("UPDATE articles SET videoPositionSeconds = :position WHERE id = :articleId")
    suspend fun updateVideoPosition(articleId: Long, position: Float)

    @Query("UPDATE articles SET localModifiedAt = :modifiedAt WHERE id = :articleId")
    suspend fun touchArticleLocalModifiedAt(articleId: Long, modifiedAt: Long)

    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticle(articleId: Long)

    @Query(
        """
        UPDATE articles
        SET wallabagEntryId = :wallabagEntryId,
            remoteUpdatedAt = :remoteUpdatedAt,
            lastSyncedAt = :lastSyncedAt
        WHERE id = :articleId
        """,
    )
    suspend fun updateWallabagMetadata(
        articleId: Long,
        wallabagEntryId: Long?,
        remoteUpdatedAt: Long?,
        lastSyncedAt: Long?,
    )

    @Query(
        """
        UPDATE articles
        SET title = :title,
            originalUrl = :originalUrl,
            dateAdded = :dateAdded,
            isLiked = :isLiked,
            isArchived = :isArchived,
            extractedTextContent = :extractedTextContent,
            extractedHeroImageUrl = :extractedHeroImageUrl,
            wallabagEntryId = :wallabagEntryId,
            remoteUpdatedAt = :remoteUpdatedAt,
            lastSyncedAt = :lastSyncedAt,
            localModifiedAt = :localModifiedAt
        WHERE id = :articleId
        """,
    )
    suspend fun updateArticleFromWallabag(
        articleId: Long,
        title: String,
        originalUrl: String,
        dateAdded: Long,
        isLiked: Boolean,
        isArchived: Boolean,
        extractedTextContent: String,
        extractedHeroImageUrl: String?,
        wallabagEntryId: Long,
        remoteUpdatedAt: Long?,
        lastSyncedAt: Long,
        localModifiedAt: Long,
    )

    @Query(
        """
        UPDATE articles
        SET title = :title,
            originalUrl = :originalUrl,
            dateAdded = :dateAdded,
            extractedTextContent = :extractedTextContent,
            extractedHeroImageUrl = :extractedHeroImageUrl,
            wallabagEntryId = :wallabagEntryId,
            remoteUpdatedAt = :remoteUpdatedAt,
            lastSyncedAt = :lastSyncedAt
        WHERE id = :articleId
        """,
    )
    suspend fun updateArticleContentFromWallabagKeepingLocalState(
        articleId: Long,
        title: String,
        originalUrl: String,
        dateAdded: Long,
        extractedTextContent: String,
        extractedHeroImageUrl: String?,
        wallabagEntryId: Long,
        remoteUpdatedAt: Long?,
        lastSyncedAt: Long,
    )

    @Query(
        """
        UPDATE articles
        SET wallabagEntryId = NULL,
            remoteUpdatedAt = NULL,
            lastSyncedAt = NULL
        """,
    )
    suspend fun clearWallabagMetadata()

    @Query("DELETE FROM article_tag_cross_ref WHERE articleId = :articleId")
    suspend fun clearTags(articleId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRefs(crossRefs: List<ArticleTagCrossRef>)

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Query("SELECT * FROM articles")
    suspend fun getAllArticles(): List<ArticleEntity>

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM article_tag_cross_ref")
    suspend fun getAllTagCrossRefs(): List<ArticleTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSyncOperation(operation: PendingSyncOperationEntity)

    @Query("SELECT * FROM wallabag_sync_operations ORDER BY createdAt ASC")
    suspend fun getPendingSyncOperations(): List<PendingSyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingWallabagDeleteOperation(operation: PendingWallabagDeleteOperationEntity)

    @Query("SELECT * FROM wallabag_delete_operations ORDER BY createdAt ASC")
    suspend fun getPendingWallabagDeleteOperations(): List<PendingWallabagDeleteOperationEntity>

    @Query("DELETE FROM wallabag_delete_operations WHERE id = :operationId")
    suspend fun deletePendingWallabagDeleteOperation(operationId: Long)

    @Query("DELETE FROM wallabag_delete_operations")
    suspend fun deleteAllPendingWallabagDeleteOperations()

    @Query(
        """
        UPDATE wallabag_delete_operations
        SET attemptCount = attemptCount + 1,
            lastError = :message
        WHERE id = :operationId
        """,
    )
    suspend fun markPendingWallabagDeleteOperationFailed(operationId: Long, message: String)

    @Query("SELECT COUNT(*) FROM wallabag_sync_operations WHERE articleId = :articleId")
    suspend fun countPendingSyncOperations(articleId: Long): Int

    @Query("DELETE FROM wallabag_sync_operations WHERE id = :operationId")
    suspend fun deletePendingSyncOperation(operationId: Long)

    @Query("DELETE FROM wallabag_sync_operations WHERE articleId = :articleId")
    suspend fun deletePendingSyncOperationsForArticle(articleId: Long)

    @Query("DELETE FROM wallabag_sync_operations")
    suspend fun deleteAllPendingSyncOperations()

    @Query(
        """
        UPDATE wallabag_sync_operations
        SET attemptCount = attemptCount + 1,
            lastError = :message
        WHERE id = :operationId
        """,
    )
    suspend fun markPendingSyncOperationFailed(operationId: Long, message: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
