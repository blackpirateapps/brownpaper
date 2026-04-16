package com.blackpirateapps.brownpaper.domain.repository

import com.blackpirateapps.brownpaper.domain.model.AddArticleResult
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.domain.model.ArticleListFilter
import com.blackpirateapps.brownpaper.domain.model.ArticleSummary
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun observeArticles(filter: ArticleListFilter, searchQuery: String): Flow<List<ArticleSummary>>
    fun observeArticle(articleId: Long): Flow<ArticleDetail?>
    fun observeFolders(): Flow<List<Folder>>
    fun observeTags(): Flow<List<Tag>>
    suspend fun addArticleFromUrl(url: String): AddArticleResult
    suspend fun toggleLiked(articleId: Long)
    suspend fun setArchived(articleId: Long, archived: Boolean)
    suspend fun assignTags(articleId: Long, selectedTagIds: Set<Long>, newTagNames: List<String>)
    suspend fun moveToFolder(articleId: Long, folderId: Long?)
    suspend fun createFolder(name: String): Long?
    suspend fun deleteArticle(articleId: Long)
}

