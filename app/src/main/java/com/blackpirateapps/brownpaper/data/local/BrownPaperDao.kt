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

    @Query("SELECT * FROM articles WHERE originalUrl = :originalUrl LIMIT 1")
    suspend fun getArticleByUrl(originalUrl: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: ArticleEntity): Long

    @Query(
        """
        UPDATE articles
        SET isLiked = CASE WHEN isLiked = 1 THEN 0 ELSE 1 END
        WHERE id = :articleId
        """,
    )
    suspend fun toggleLiked(articleId: Long)

    @Query("UPDATE articles SET isArchived = :archived WHERE id = :articleId")
    suspend fun setArchived(articleId: Long, archived: Boolean)

    @Query("UPDATE articles SET folderId = :folderId WHERE id = :articleId")
    suspend fun moveToFolder(articleId: Long, folderId: Long?)

    @Query("UPDATE articles SET videoPositionSeconds = :position WHERE id = :articleId")
    suspend fun updateVideoPosition(articleId: Long, position: Float)

    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticle(articleId: Long)

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

