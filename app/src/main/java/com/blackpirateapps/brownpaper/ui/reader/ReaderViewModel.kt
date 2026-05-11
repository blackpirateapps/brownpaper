package com.blackpirateapps.brownpaper.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.core.model.ReaderContentWidth
import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderFontWeight
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import com.blackpirateapps.brownpaper.domain.model.AnnotationAnchor
import com.blackpirateapps.brownpaper.domain.model.AnnotationColor
import com.blackpirateapps.brownpaper.domain.model.ArticleAnnotation
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag
import com.blackpirateapps.brownpaper.domain.repository.AnnotationRepository
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.ReaderPreferencesRepository
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val article: ArticleDetail? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val availableTags: List<Tag> = emptyList(),
    val availableFolders: List<Folder> = emptyList(),
    val annotations: List<ArticleAnnotation> = emptyList(),
    val searchQuery: String = "",
)

private data class ReaderSupportState(
    val tags: List<Tag>,
    val folders: List<Folder>,
    val annotations: List<ArticleAnnotation>,
    val searchQuery: String,
)

sealed interface ReaderEvent {
    data object Deleted : ReaderEvent
    data class Message(val value: String) : ReaderEvent
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val annotationRepository: AnnotationRepository,
    private val wallabagRepository: WallabagRepository,
    private val readerPreferencesRepository: ReaderPreferencesRepository,
) : ViewModel() {

    private val articleId = checkNotNull(savedStateHandle.get<Long>("articleId"))
    private val searchQuery = MutableStateFlow("")
    private val _events = MutableSharedFlow<ReaderEvent>()

    val events = _events.asSharedFlow()

    private val supportState = combine(
        articleRepository.observeTags(),
        articleRepository.observeFolders(),
        annotationRepository.observeAnnotations(articleId),
        searchQuery,
    ) { tags, folders, annotations, query ->
        ReaderSupportState(
            tags = tags,
            folders = folders,
            annotations = annotations,
            searchQuery = query,
        )
    }

    val uiState: StateFlow<ReaderUiState> = combine(
        articleRepository.observeArticle(articleId),
        readerPreferencesRepository.readerPreferences,
        supportState,
    ) { article, preferences, support ->
        ReaderUiState(
            article = article,
            readerPreferences = preferences,
            availableTags = support.tags,
            availableFolders = support.folders,
            annotations = support.annotations,
            searchQuery = support.searchQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState(),
    )

    init {
        viewModelScope.launch {
            wallabagRepository.syncAnnotationsForArticle(articleId)
        }
    }

    fun toggleLiked() {
        viewModelScope.launch {
            articleRepository.toggleLiked(articleId)
        }
    }

    fun toggleArchived(currentValue: Boolean) {
        viewModelScope.launch {
            articleRepository.setArchived(articleId, !currentValue)
            _events.emit(
                ReaderEvent.Message(
                    if (currentValue) "Article restored to Home." else "Article archived.",
                ),
            )
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily) {
        viewModelScope.launch {
            readerPreferencesRepository.updateFontFamily(fontFamily)
        }
    }

    fun updateFontSize(fontSizeSp: Float) {
        viewModelScope.launch {
            readerPreferencesRepository.updateFontSize(fontSizeSp)
        }
    }

    fun updateFontWeight(fontWeight: ReaderFontWeight) {
        viewModelScope.launch {
            readerPreferencesRepository.updateFontWeight(fontWeight)
        }
    }

    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            readerPreferencesRepository.updateTheme(theme)
        }
    }

    fun updateContentWidth(contentWidth: ReaderContentWidth) {
        viewModelScope.launch {
            readerPreferencesRepository.updateContentWidth(contentWidth)
        }
    }

    fun saveTags(selectedTagIds: Set<Long>, newTagNames: List<String>) {
        viewModelScope.launch {
            articleRepository.assignTags(articleId, selectedTagIds, newTagNames)
            _events.emit(ReaderEvent.Message("Tags updated."))
        }
    }

    fun moveToFolder(folderId: Long?, newFolderName: String) {
        viewModelScope.launch {
            val resolvedFolderId = if (newFolderName.isBlank()) {
                folderId
            } else {
                articleRepository.createFolder(newFolderName)
            }
            articleRepository.moveToFolder(articleId, resolvedFolderId)
            _events.emit(ReaderEvent.Message("Folder updated."))
        }
    }

    fun updateVideoPosition(positionSeconds: Float) {
        viewModelScope.launch {
            articleRepository.updateVideoPosition(articleId, positionSeconds)
        }
    }

    fun deleteArticle() {
        viewModelScope.launch {
            articleRepository.deleteArticle(articleId)
            _events.emit(ReaderEvent.Deleted)
        }
    }

    fun createAnnotation(anchor: AnnotationAnchor, quote: String, noteText: String, color: AnnotationColor) {
        viewModelScope.launch {
            val annotationId = annotationRepository.createAnnotation(
                articleId = articleId,
                anchor = anchor,
                quote = quote,
                noteText = noteText,
                color = color,
            )
            if (annotationId != null) {
                _events.emit(ReaderEvent.Message("Annotation saved."))
            }
        }
    }

    fun updateAnnotation(annotationId: Long, noteText: String, color: AnnotationColor) {
        viewModelScope.launch {
            annotationRepository.updateAnnotation(annotationId, noteText, color)
            _events.emit(ReaderEvent.Message("Annotation updated."))
        }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch {
            annotationRepository.deleteAnnotation(annotationId)
            _events.emit(ReaderEvent.Message("Annotation deleted."))
        }
    }

    fun syncAnnotations() {
        viewModelScope.launch {
            wallabagRepository.syncAnnotationsForArticle(articleId)
        }
    }
}
