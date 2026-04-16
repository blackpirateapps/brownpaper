package com.blackpirateapps.brownpaper.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.ReaderPreferencesRepository
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
    val searchQuery: String = "",
)

sealed interface ReaderEvent {
    data object Deleted : ReaderEvent
    data class Message(val value: String) : ReaderEvent
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val articleRepository: ArticleRepository,
    private val readerPreferencesRepository: ReaderPreferencesRepository,
) : ViewModel() {

    private val articleId = checkNotNull(savedStateHandle.get<Long>("articleId"))
    private val searchQuery = MutableStateFlow("")
    private val _events = MutableSharedFlow<ReaderEvent>()

    val events = _events.asSharedFlow()

    val uiState: StateFlow<ReaderUiState> = combine(
        articleRepository.observeArticle(articleId),
        readerPreferencesRepository.readerPreferences,
        articleRepository.observeTags(),
        articleRepository.observeFolders(),
        searchQuery,
    ) { article, preferences, tags, folders, query ->
        ReaderUiState(
            article = article,
            readerPreferences = preferences,
            availableTags = tags,
            availableFolders = folders,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState(),
    )

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

    fun updateEmphasizedWeight(enabled: Boolean) {
        viewModelScope.launch {
            readerPreferencesRepository.updateEmphasizedWeight(enabled)
        }
    }

    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            readerPreferencesRepository.updateTheme(theme)
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
}
