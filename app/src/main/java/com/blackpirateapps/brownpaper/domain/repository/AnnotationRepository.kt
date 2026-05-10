package com.blackpirateapps.brownpaper.domain.repository

import com.blackpirateapps.brownpaper.domain.model.AnnotationAnchor
import com.blackpirateapps.brownpaper.domain.model.AnnotationColor
import com.blackpirateapps.brownpaper.domain.model.ArticleAnnotation
import kotlinx.coroutines.flow.Flow

interface AnnotationRepository {
    fun observeAnnotations(articleId: Long): Flow<List<ArticleAnnotation>>
    suspend fun createAnnotation(
        articleId: Long,
        anchor: AnnotationAnchor,
        quote: String,
        noteText: String,
        color: AnnotationColor,
    ): Long?
    suspend fun updateAnnotation(annotationId: Long, noteText: String, color: AnnotationColor)
    suspend fun deleteAnnotation(annotationId: Long)
    suspend fun scheduleSyncAnnotationsForArticle(articleId: Long)
}
