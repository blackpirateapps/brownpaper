package com.blackpirateapps.brownpaper.domain.model

data class Tag(
    val id: Long,
    val name: String,
)

data class Folder(
    val id: Long,
    val name: String,
)

data class ArticleSummary(
    val id: Long,
    val title: String,
    val originalUrl: String,
    val dateAdded: Long,
    val isLiked: Boolean,
    val isArchived: Boolean,
    val heroImageUrl: String?,
    val excerpt: String,
    val isVideo: Boolean,
    val videoRuntimeText: String?,
    val channelName: String?,
    val viewCount: Long,
)

data class ArticleDetail(
    val id: Long,
    val title: String,
    val originalUrl: String,
    val dateAdded: Long,
    val isLiked: Boolean,
    val isArchived: Boolean,
    val heroImageUrl: String?,
    val bodyText: String,
    val folder: Folder?,
    val tags: List<Tag>,
    val isVideo: Boolean,
    val youtubeVideoId: String?,
    val videoRuntimeText: String?,
    val channelName: String?,
    val viewCount: Long,
    val videoPositionSeconds: Float,
)

sealed interface ArticleListFilter {
    data object Inbox : ArticleListFilter
    data object Likes : ArticleListFilter
    data object Read : ArticleListFilter
    data object Archived : ArticleListFilter
    data object Videos : ArticleListFilter
    data class FolderFilter(val folderId: Long) : ArticleListFilter
    data class TagFilter(val tagId: Long) : ArticleListFilter
}

enum class ArticleListSource(val routeValue: String, val title: String) {
    Inbox("home", "Home"),
    Likes("likes", "Likes"),
    Read("read", "Read"),
    Archived("archived", "Archived"),
    Videos("videos", "Videos"),
    Folder("folder", "Folder"),
    Tag("tag", "Tag"),
}

sealed interface AddArticleResult {
    data class Success(val articleId: Long) : AddArticleResult
    data class AlreadySaved(val articleId: Long?) : AddArticleResult
    data object InvalidUrl : AddArticleResult
    data class Failure(val message: String) : AddArticleResult
}
