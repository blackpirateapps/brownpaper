package com.blackpirateapps.brownpaper.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.blackpirateapps.brownpaper.R
import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import com.blackpirateapps.brownpaper.core.util.highlightMatches
import com.blackpirateapps.brownpaper.core.util.toReadableArticleDate
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.ui.components.ManageTagsDialog
import com.blackpirateapps.brownpaper.ui.components.MoveToFolderDialog
import com.blackpirateapps.brownpaper.ui.components.SearchInArticleDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    snackbarHostState: SnackbarHostState,
    events: kotlinx.coroutines.flow.Flow<ReaderEvent>,
    onBack: () -> Unit,
    onToggleLiked: () -> Unit,
    onToggleArchived: (Boolean) -> Unit,
    onUpdateFontFamily: (ReaderFontFamily) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateEmphasizedWeight: (Boolean) -> Unit,
    onUpdateTheme: (ReaderTheme) -> Unit,
    onSaveTags: (Set<Long>, List<String>) -> Unit,
    onMoveToFolder: (Long?, String) -> Unit,
    onSearchInArticle: (String) -> Unit,
    onUpdateVideoPosition: (Float) -> Unit,
    onDeleteArticle: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val article = uiState.article
    val readerColors = readerColors(uiState.readerPreferences)
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showTagDialog by rememberSaveable { mutableStateOf(false) }
    var showFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                ReaderEvent.Deleted -> onDeleted()
                is ReaderEvent.Message -> snackbarHostState.showSnackbar(event.value)
            }
        }
    }

    if (showTagDialog && article != null) {
        ManageTagsDialog(
            availableTags = uiState.availableTags,
            selectedTagIds = article.tags.map { it.id }.toSet(),
            onDismiss = { showTagDialog = false },
            onSave = { selectedTagIds, newTagNames ->
                onSaveTags(selectedTagIds, newTagNames)
                showTagDialog = false
            },
        )
    }

    if (showFolderDialog && article != null) {
        MoveToFolderDialog(
            availableFolders = uiState.availableFolders,
            currentFolderId = article.folder?.id,
            onDismiss = { showFolderDialog = false },
            onSave = { folderId, newFolderName ->
                onMoveToFolder(folderId, newFolderName)
                showFolderDialog = false
            },
        )
    }

    if (showSearchDialog) {
        SearchInArticleDialog(
            initialQuery = uiState.searchQuery,
            onDismiss = { showSearchDialog = false },
            onApply = {
                onSearchInArticle(it)
                showSearchDialog = false
            },
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
        ) {
            ReaderSettingsSheet(
                preferences = uiState.readerPreferences,
                onUpdateFontFamily = onUpdateFontFamily,
                onUpdateFontSize = onUpdateFontSize,
                onUpdateEmphasizedWeight = onUpdateEmphasizedWeight,
                onUpdateTheme = onUpdateTheme,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = readerColors.background,
        contentColor = readerColors.content,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = readerColors.background,
                    titleContentColor = readerColors.content,
                    actionIconContentColor = readerColors.content,
                    navigationIconContentColor = readerColors.content,
                ),
                title = {
                    Text(
                        text = article?.title ?: "Reader",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = readerColors.content,
                        )
                    }
                },
                actions = {
                    if (article != null) {
                        IconButton(onClick = onToggleLiked) {
                            Icon(
                                imageVector = if (article.isLiked) {
                                    Icons.Outlined.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = "Toggle like",
                                tint = readerColors.content,
                            )
                        }
                        IconButton(onClick = { onToggleArchived(article.isArchived) }) {
                            Icon(
                                imageVector = if (article.isArchived) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.Archive
                                },
                                contentDescription = if (article.isArchived) "Unarchive" else "Archive",
                                tint = readerColors.content,
                            )
                        }
                        TextButton(onClick = { showSettingsSheet = true }) {
                            Text("Aa", color = readerColors.content)
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options",
                                    tint = readerColors.content,
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Manage tags") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showTagDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to folder") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showFolderDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Search in article") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showSearchDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        shareArticle(context, article.originalUrl)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Open in browser") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.OpenInBrowser,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        openInBrowser(context, article.originalUrl)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onDeleteArticle()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (article == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(readerColors.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Article unavailable.",
                    color = readerColors.content,
                )
            }
        } else {
            if (article.isVideo && !article.youtubeVideoId.isNullOrBlank()) {
                com.blackpirateapps.brownpaper.ui.components.YoutubeVideoPlayer(
                    videoId = article.youtubeVideoId,
                    startSeconds = article.videoPositionSeconds,
                    onPositionChanged = onUpdateVideoPosition,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    ReaderContent(
                        article = article,
                        searchQuery = uiState.searchQuery,
                        preferences = uiState.readerPreferences,
                        colors = readerColors,
                        innerPadding = innerPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderContent(
    article: ArticleDetail,
    searchQuery: String,
    preferences: ReaderPreferences,
    colors: ReaderColors,
    innerPadding: PaddingValues,
) {
    val paragraphs = remember(article.bodyText) {
        article.bodyText
            .split(Regex("\n\\s*\n"))
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = innerPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = innerPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!article.heroImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = article.heroImageUrl,
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = preferences.asFontFamily(),
                        fontWeight = preferences.asFontWeight(),
                    ),
                    color = colors.content,
                )
                Text(
                    text = article.dateAdded.toReadableArticleDate(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    article.folder?.let { folder ->
                        AssistChip(
                            onClick = {},
                            label = { Text(folder.name) },
                        )
                    }
                    article.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag.name) },
                        )
                    }
                }
            }
        }

        items(paragraphs) { paragraph ->
            if (paragraph.startsWith("![img](") && paragraph.endsWith(")")) {
                val url = paragraph.substringAfter("![img](").substringBeforeLast(")")
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                )
            } else {
                Text(
                    text = paragraph.highlightMatches(
                        query = searchQuery,
                        highlightColor = colors.highlight,
                    ),
                    style = TextStyle(
                        fontFamily = preferences.asFontFamily(),
                        fontWeight = preferences.asFontWeight(),
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * (preferences.fontSizeSp / 18f),
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * (preferences.fontSizeSp / 18f),
                    ),
                    color = colors.content,
                )
            }
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    preferences: ReaderPreferences,
    onUpdateFontFamily: (ReaderFontFamily) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateEmphasizedWeight: (Boolean) -> Unit,
    onUpdateTheme: (ReaderTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Reader settings",
            style = MaterialTheme.typography.titleLarge,
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Font family", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                ReaderFontFamily.entries.forEach { fontFamily ->
                    FilterChip(
                        selected = preferences.fontFamily == fontFamily,
                        onClick = { onUpdateFontFamily(fontFamily) },
                        label = {
                            Text(
                                when (fontFamily) {
                                    ReaderFontFamily.SYSTEM -> "System"
                                    ReaderFontFamily.SERIF -> "Serif"
                                    ReaderFontFamily.MONO -> "Mono"
                                    ReaderFontFamily.MERRIWEATHER -> "Merriweather"
                                    ReaderFontFamily.LORA -> "Lora"
                                    ReaderFontFamily.FIRA_SANS -> "Fira Sans"
                                    ReaderFontFamily.INTER -> "Inter"
                                },
                            )
                        },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font size", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = preferences.fontSizeSp,
                onValueChange = onUpdateFontSize,
                valueRange = 14f..28f,
            )
            Text(
                text = "${preferences.fontSizeSp.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Font weight", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (preferences.useEmphasizedWeight) "Medium" else "Normal",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = preferences.useEmphasizedWeight,
                onCheckedChange = onUpdateEmphasizedWeight,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Background", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                ReaderTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = preferences.theme == theme,
                        onClick = { onUpdateTheme(theme) },
                        label = {
                            Text(
                                when (theme) {
                                    ReaderTheme.LIGHT -> "Light"
                                    ReaderTheme.DARK -> "Dark"
                                    ReaderTheme.PAPER -> "Paper"
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun ReaderPreferences.asFontFamily(): FontFamily = when (fontFamily) {
    ReaderFontFamily.SYSTEM -> FontFamily.Default
    ReaderFontFamily.SERIF -> FontFamily.Serif
    ReaderFontFamily.MONO -> FontFamily.Monospace
    ReaderFontFamily.MERRIWEATHER -> FontFamily(Font(R.font.merriweather, FontWeight.Normal))
    ReaderFontFamily.LORA -> FontFamily(Font(R.font.lora, FontWeight.Normal))
    ReaderFontFamily.FIRA_SANS -> FontFamily(Font(R.font.firasans, FontWeight.Normal))
    ReaderFontFamily.INTER -> FontFamily(Font(R.font.inter, FontWeight.Normal))
}

private fun ReaderPreferences.asFontWeight(): FontWeight =
    if (useEmphasizedWeight) FontWeight.Medium else FontWeight.Normal

private data class ReaderColors(
    val background: Color,
    val content: Color,
    val muted: Color,
    val highlight: Color,
)

@Composable
private fun readerColors(preferences: ReaderPreferences): ReaderColors = when (preferences.theme) {
    ReaderTheme.LIGHT -> ReaderColors(
        background = MaterialTheme.colorScheme.background,
        content = MaterialTheme.colorScheme.onBackground,
        muted = MaterialTheme.colorScheme.onSurfaceVariant,
        highlight = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
    )
    ReaderTheme.DARK -> ReaderColors(
        background = Color(0xFF090909),
        content = Color(0xFFF6F6F6),
        muted = Color(0xFFB5B5B5),
        highlight = Color(0xFF3F7CAC).copy(alpha = 0.36f),
    )
    ReaderTheme.PAPER -> ReaderColors(
        background = Color(0xFFF4ECD8),
        content = Color(0xFF2D2418),
        muted = Color(0xFF6C604F),
        highlight = Color(0xFFD8B25A).copy(alpha = 0.36f),
    )
}

private fun shareArticle(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, url)
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share article"))
    }
}

private fun openInBrowser(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
        )
    }
}
