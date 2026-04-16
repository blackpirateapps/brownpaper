package com.blackpirateapps.brownpaper.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blackpirateapps.brownpaper.domain.model.ArticleListSource
import com.blackpirateapps.brownpaper.ui.components.AddUrlDialog
import com.blackpirateapps.brownpaper.ui.components.BrownPaperDrawerContent
import com.blackpirateapps.brownpaper.ui.home.ArticleListScreen
import com.blackpirateapps.brownpaper.ui.home.ArticleListViewModel
import com.blackpirateapps.brownpaper.ui.reader.ReaderScreen
import com.blackpirateapps.brownpaper.ui.reader.ReaderViewModel
import com.blackpirateapps.brownpaper.ui.shell.ShellViewModel
import com.blackpirateapps.brownpaper.ui.theme.BrownPaperTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrownPaperApp(
    initialSharedUrl: String?,
    sharedUrlFlow: Flow<String>,
) {
    BrownPaperTheme {
        val navController = rememberNavController()
        val shellViewModel: ShellViewModel = hiltViewModel()
        val shellUiState by shellViewModel.uiState.collectAsStateWithLifecycle()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentSource = backStackEntry?.arguments?.getString("source")?.let { routeValue ->
            ArticleListSource.entries.firstOrNull { it.routeValue == routeValue }
        }
        val currentSourceId = backStackEntry?.arguments?.getLong("sourceId")
        val isListDestination = backStackEntry?.destination?.route == BrownPaperRoutes.listTemplate
        var showAddDialog by rememberSaveable { mutableStateOf(false) }
        var hasConsumedInitialSharedUrl by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            shellViewModel.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(initialSharedUrl, hasConsumedInitialSharedUrl) {
            if (!hasConsumedInitialSharedUrl && !initialSharedUrl.isNullOrBlank()) {
                shellViewModel.submitUrl(initialSharedUrl)
                hasConsumedInitialSharedUrl = true
            }
        }

        LaunchedEffect(sharedUrlFlow) {
            sharedUrlFlow.collect(shellViewModel::submitUrl)
        }

        if (showAddDialog) {
            AddUrlDialog(
                isSaving = shellUiState.isSavingArticle,
                onDismiss = { showAddDialog = false },
                onConfirm = { url ->
                    shellViewModel.submitUrl(url)
                    showAddDialog = false
                },
            )
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isListDestination,
            drawerContent = {
                BrownPaperDrawerContent(
                    currentSource = currentSource,
                    currentSourceId = currentSourceId,
                    tags = shellUiState.tags,
                    folders = shellUiState.folders,
                    onSelectSource = { source, sourceId ->
                        navController.navigate(
                            BrownPaperRoutes.listRoute(source, sourceId ?: -1L),
                        ) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        coroutineScope.launch { drawerState.close() }
                    },
                )
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = BrownPaperRoutes.listRoute(ArticleListSource.Inbox),
                modifier = Modifier,
            ) {
                composable(
                    route = BrownPaperRoutes.listTemplate,
                    arguments = listOf(
                        navArgument("source") { type = NavType.StringType },
                        navArgument("sourceId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { entry ->
                    val viewModel: ArticleListViewModel = hiltViewModel(entry)
                    val routeSource = entry.arguments?.getString("source") ?: ArticleListSource.Inbox.routeValue
                    val routeSourceId = entry.arguments?.getLong("sourceId") ?: -1L
                    
                    LaunchedEffect(routeSource, routeSourceId) {
                        viewModel.updateSource(routeSource, routeSourceId)
                    }

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    ArticleListScreen(
                        uiState = uiState,
                        isSavingArticle = shellUiState.isSavingArticle,
                        snackbarHostState = snackbarHostState,
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onArticleSelected = { articleId ->
                            navController.navigate(BrownPaperRoutes.readerRoute(articleId))
                        },
                        onAddArticle = { showAddDialog = true },
                    )
                }

                composable(
                    route = BrownPaperRoutes.readerTemplate,
                    arguments = listOf(
                        navArgument("articleId") { type = NavType.LongType },
                    ),
                ) { entry ->
                    val viewModel: ReaderViewModel = hiltViewModel(entry)
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    ReaderScreen(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        events = viewModel.events,
                        onBack = { navController.popBackStack() },
                        onToggleLiked = viewModel::toggleLiked,
                        onToggleArchived = viewModel::toggleArchived,
                        onUpdateFontFamily = viewModel::updateFontFamily,
                        onUpdateFontSize = viewModel::updateFontSize,
                        onUpdateEmphasizedWeight = viewModel::updateEmphasizedWeight,
                        onUpdateTheme = viewModel::updateTheme,
                        onSaveTags = viewModel::saveTags,
                        onMoveToFolder = viewModel::moveToFolder,
                        onSearchInArticle = viewModel::setSearchQuery,
                        onUpdateVideoPosition = viewModel::updateVideoPosition,
                        onDeleteArticle = viewModel::deleteArticle,
                        onDeleted = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
