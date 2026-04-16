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

    private val sourceFlow = savedStateHandle.getStateFlow("source", ArticleListSource.Inbox.routeValue)
    private val sourceIdFlow = savedStateHandle.getStateFlow("sourceId", -1L)

    private val searchQuery = MutableStateFlow("")

    private val filterFlow = combine(sourceFlow, sourceIdFlow) { sourceValue, id ->
        val source = ArticleListSource.entries.firstOrNull { it.routeValue == sourceValue } ?: ArticleListSource.Inbox
        source.toFilter(id)
    }

    private val titleFlow = combine(sourceFlow, sourceIdFlow) { sourceValue, id ->
        val source = ArticleListSource.entries.firstOrNull { it.routeValue == sourceValue } ?: ArticleListSource.Inbox
        source to id
    }.flatMapLatest { (source, id) ->
        when (source) {
            ArticleListSource.Folder -> articleRepository.observeFolders().flatMapLatest { folders ->
                flowOf(folders.firstOrNull { it.id == id }?.name ?: source.title)
            }
            ArticleListSource.Tag -> articleRepository.observeTags().flatMapLatest { tags ->
                flowOf(tags.firstOrNull { it.id == id }?.name ?: source.title)
            }
            else -> flowOf(source.title)
        }
    }

    val uiState: StateFlow<ArticleListUiState> = combine(
        searchQuery,
        titleFlow,
        filterFlow.flatMapLatest { filter ->
            searchQuery.flatMapLatest { query ->
                articleRepository.observeArticles(filter, query)
            }
        },
    ) { query, title, articles ->
        ArticleListUiState(
            title = title,
            searchQuery = query,
            articles = articles,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArticleListUiState(),
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

