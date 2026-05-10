package com.blackpirateapps.brownpaper.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.domain.model.AddArticleResult
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncResult
import com.blackpirateapps.brownpaper.domain.usecase.AddArticleUseCase
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

data class ShellUiState(
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isSavingArticle: Boolean = false,
    val isWallabagConnected: Boolean = false,
    val isSyncingWallabag: Boolean = false,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    articleRepository: ArticleRepository,
    private val addArticleUseCase: AddArticleUseCase,
    private val wallabagRepository: WallabagRepository,
) : ViewModel() {

    private val isSavingArticle = MutableStateFlow(false)
    private val isSyncingWallabag = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<String>()

    val messages = _messages.asSharedFlow()

    val uiState: StateFlow<ShellUiState> = combine(
        articleRepository.observeFolders(),
        articleRepository.observeTags(),
        isSavingArticle,
        wallabagRepository.accountState,
        isSyncingWallabag,
    ) { folders, tags, saving, wallabagAccount, syncing ->
        ShellUiState(
            folders = folders,
            tags = tags,
            isSavingArticle = saving,
            isWallabagConnected = wallabagAccount.isConnected,
            isSyncingWallabag = syncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShellUiState(),
    )

    fun submitUrl(url: String) {
        if (url.isBlank() || isSavingArticle.value) {
            return
        }

        viewModelScope.launch {
            isSavingArticle.value = true
            val message = when (val result = addArticleUseCase(url)) {
                is AddArticleResult.Success -> "Article saved offline."
                is AddArticleResult.AlreadySaved -> "That article is already in your library."
                AddArticleResult.InvalidUrl -> "Enter a valid article URL."
                is AddArticleResult.Failure -> result.message
            }
            _messages.emit(message)
            isSavingArticle.value = false
        }
    }

    fun syncWallabag() {
        if (isSyncingWallabag.value) {
            return
        }

        viewModelScope.launch {
            isSyncingWallabag.value = true
            val message = when (val result = wallabagRepository.syncNow()) {
                is WallabagSyncResult.Success -> "wallabag synced: ${result.pulled} pulled, ${result.pushed} pushed."
                WallabagSyncResult.NotConnected -> "Connect wallabag in Settings first."
                is WallabagSyncResult.Failure -> result.message
            }
            _messages.emit(message)
            isSyncingWallabag.value = false
        }
    }
}
