package com.blackpirateapps.brownpaper.domain.model

data class AnnotationAnchor(
    val startParagraphIndex: Int,
    val endParagraphIndex: Int,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val prefixText: String = "",
    val suffixText: String = "",
)

data class AnnotationDraft(
    val anchor: AnnotationAnchor,
    val quote: String,
    val noteText: String,
    val color: AnnotationColor = AnnotationColor.Yellow,
)

data class ArticleAnnotation(
    val id: Long,
    val articleId: Long,
    val quote: String,
    val noteText: String,
    val color: AnnotationColor,
    val anchor: AnnotationAnchor,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: AnnotationSyncState,
)

enum class AnnotationColor(val hex: String, val label: String) {
    Yellow("#F6D365", "Yellow"),
    Green("#8FD6A3", "Green"),
    Blue("#89B4FA", "Blue"),
    Pink("#F3A6C8", "Pink"),
    Purple("#CBA6F7", "Purple");

    companion object {
        fun fromHex(value: String?): AnnotationColor =
            entries.firstOrNull { it.hex.equals(value, ignoreCase = true) } ?: Yellow
    }
}

enum class AnnotationSyncState {
    LocalOnly,
    Pending,
    Synced,
    Failed,
}
