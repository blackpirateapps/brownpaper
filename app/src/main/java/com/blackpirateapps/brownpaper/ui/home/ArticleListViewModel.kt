package com.blackpirateapps.brownpaper.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpirateapps.brownpaper.domain.model.ArticleListFilter
import com.blackpirateapps.brownpaper.domain.model.ArticleListSource
import com.blackpirateapps.brownpaper.domain.model.ArticleSummary
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ArticleListUiState(
    val title: String = "Home",
    val searchQuery: String = "",
    val articles: List<ArticleSummary> = emptyList(),
)

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    articleRepository: ArticleRepository,
) : ViewModel() {

    private val source = ArticleListSource.entries.firstOrNull {
        it.routeValue == savedStateHandle.get<String>("source")
    } ?: ArticleListSource.Inbox

    private val sourceId = savedStateHandle.get<Long>("sourceId") ?: -1L
    private val filter = source.toFilter(sourceId)
    private val searchQuery = MutableStateFlow("")

    private val titleFlow = when (source) {
        ArticleListSource.Folder -> articleRepository.observeFolders().flatMapLatest { folders ->
            flowOf(folders.firstOrNull { it.id == sourceId }?.name ?: source.title)
        }
        ArticleListSource.Tag -> articleRepository.observeTags().flatMapLatest { tags ->
            flowOf(tags.firstOrNull { it.id == sourceId }?.name ?: source.title)
        }
        else -> flowOf(source.title)
    }

    val uiState: StateFlow<ArticleListUiState> = combine(
        searchQuery,
        titleFlow,
        searchQuery.flatMapLatest { query -> articleRepository.observeArticles(filter, query) },
    ) { query, title, articles ->
        ArticleListUiState(
            title = title,
            searchQuery = query,
            articles = articles,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArticleListUiState(title = source.title),
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}

private fun ArticleListSource.toFilter(sourceId: Long): ArticleListFilter = when (this) {
    ArticleListSource.Inbox -> ArticleListFilter.Inbox
    ArticleListSource.Likes -> ArticleListFilter.Likes
    ArticleListSource.Archived -> ArticleListFilter.Archived
    ArticleListSource.Folder -> ArticleListFilter.FolderFilter(sourceId)
    ArticleListSource.Tag -> ArticleListFilter.TagFilter(sourceId)
}

