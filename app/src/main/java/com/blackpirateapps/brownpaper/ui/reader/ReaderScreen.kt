package com.blackpirateapps.brownpaper.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.blackpirateapps.brownpaper.R
import com.blackpirateapps.brownpaper.core.model.ReaderContentWidth
import com.blackpirateapps.brownpaper.core.model.ReaderFontFamily
import com.blackpirateapps.brownpaper.core.model.ReaderFontWeight
import com.blackpirateapps.brownpaper.core.model.ReaderPreferences
import com.blackpirateapps.brownpaper.core.model.ReaderTheme
import com.blackpirateapps.brownpaper.core.util.toReadableArticleDate
import com.blackpirateapps.brownpaper.domain.model.AnnotationAnchor
import com.blackpirateapps.brownpaper.domain.model.AnnotationColor
import com.blackpirateapps.brownpaper.domain.model.AnnotationSyncState
import com.blackpirateapps.brownpaper.domain.model.ArticleAnnotation
import com.blackpirateapps.brownpaper.domain.model.ArticleDetail
import com.blackpirateapps.brownpaper.ui.components.ManageTagsDialog
import com.blackpirateapps.brownpaper.ui.components.MoveToFolderDialog
import com.blackpirateapps.brownpaper.ui.components.SearchInArticleDialog
import java.text.NumberFormat

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
    onUpdateFontWeight: (ReaderFontWeight) -> Unit,
    onUpdateTheme: (ReaderTheme) -> Unit,
    onUpdateContentWidth: (ReaderContentWidth) -> Unit,
    onSaveTags: (Set<Long>, List<String>) -> Unit,
    onMoveToFolder: (Long?, String) -> Unit,
    onSearchInArticle: (String) -> Unit,
    onUpdateVideoPosition: (Float) -> Unit,
    onDeleteArticle: () -> Unit,
    onCreateAnnotation: (AnnotationAnchor, String, String, AnnotationColor) -> Unit,
    onUpdateAnnotation: (Long, String, AnnotationColor) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    onSyncAnnotations: () -> Unit,
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
    var showAnnotationsSheet by rememberSaveable { mutableStateOf(false) }
    var annotationDraft by remember { mutableStateOf<AnnotationDraftState?>(null) }
    var editingAnnotation by remember { mutableStateOf<ArticleAnnotation?>(null) }
    var jumpToParagraphIndex by remember { mutableStateOf<Int?>(null) }

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
                onUpdateFontWeight = onUpdateFontWeight,
                onUpdateTheme = onUpdateTheme,
                onUpdateContentWidth = onUpdateContentWidth,
            )
        }
    }

    annotationDraft?.let { draft ->
        ModalBottomSheet(
            onDismissRequest = { annotationDraft = null },
        ) {
            AnnotationEditorSheet(
                title = "New annotation",
                initialNote = draft.noteText,
                initialColor = draft.color,
                quote = draft.quote,
                colors = readerColors,
                onDismiss = { annotationDraft = null },
                onSave = { note, color ->
                    onCreateAnnotation(draft.anchor, draft.quote, note, color)
                    annotationDraft = null
                },
            )
        }
    }

    editingAnnotation?.let { annotation ->
        ModalBottomSheet(
            onDismissRequest = { editingAnnotation = null },
        ) {
            AnnotationEditorSheet(
                title = "Edit annotation",
                initialNote = annotation.noteText,
                initialColor = annotation.color,
                quote = annotation.quote,
                colors = readerColors,
                onDismiss = { editingAnnotation = null },
                onSave = { note, color ->
                    onUpdateAnnotation(annotation.id, note, color)
                    editingAnnotation = null
                },
                onDelete = {
                    onDeleteAnnotation(annotation.id)
                    editingAnnotation = null
                },
            )
        }
    }

    if (showAnnotationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnnotationsSheet = false },
        ) {
            AnnotationListSheet(
                annotations = uiState.annotations,
                colors = readerColors,
                onSync = onSyncAnnotations,
                onOpen = { annotation ->
                    jumpToParagraphIndex = annotation.anchor.startParagraphIndex
                    showAnnotationsSheet = false
                },
                onEdit = { annotation ->
                    editingAnnotation = annotation
                    showAnnotationsSheet = false
                },
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
                        IconButton(onClick = { showAnnotationsSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Notes,
                                contentDescription = "Annotations",
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
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Label,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showTagDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to folder") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                        )
                                    },
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(readerColors.background)
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    com.blackpirateapps.brownpaper.ui.components.YoutubeVideoPlayer(
                        videoId = article.youtubeVideoId,
                        startSeconds = article.videoPositionSeconds,
                        onPositionChanged = onUpdateVideoPosition,
                        modifier = Modifier
                            .widthIn(max = uiState.readerPreferences.contentWidth.maxWidth)
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                }
            } else {
                ReaderContent(
                    article = article,
                    searchQuery = uiState.searchQuery,
                    preferences = uiState.readerPreferences,
                    colors = readerColors,
                    annotations = uiState.annotations,
                    innerPadding = innerPadding,
                    jumpToParagraphIndex = jumpToParagraphIndex,
                    onJumpHandled = { jumpToParagraphIndex = null },
                    onOpenOriginalUrl = { openInBrowser(context, article.originalUrl) },
                    onAnnotationSelected = { editingAnnotation = it },
                    onCreateAnnotationDraft = { draft -> annotationDraft = draft },
                )
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
    annotations: List<ArticleAnnotation>,
    innerPadding: PaddingValues,
    jumpToParagraphIndex: Int?,
    onJumpHandled: () -> Unit,
    onOpenOriginalUrl: () -> Unit,
    onAnnotationSelected: (ArticleAnnotation) -> Unit,
    onCreateAnnotationDraft: (AnnotationDraftState) -> Unit,
) {
    val paragraphs = remember(article.bodyText) {
        article.bodyText
            .split(Regex("\n\\s*\n"))
            .map(String::trim)
            .filter(String::isNotBlank)
    }
    val metadata = remember(article.bodyText, article.originalUrl) {
        article.toReaderMetadata()
    }
    val listState = rememberLazyListState()
    val maxContentWidth = preferences.contentWidth.maxWidth

    LaunchedEffect(jumpToParagraphIndex) {
        jumpToParagraphIndex?.let { paragraphIndex ->
            listState.animateScrollToItem((paragraphIndex + 1).coerceAtLeast(0))
            onJumpHandled()
        }
    }

    LazyColumn(
        state = listState,
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxContentWidth)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
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
                    ReaderMetadataPills(
                        metadata = metadata,
                        colors = colors,
                        onOpenOriginalUrl = onOpenOriginalUrl,
                    )
                    ReaderTaxonomyPills(
                        article = article,
                        colors = colors,
                    )
                }
            }
        }

        itemsIndexed(
            items = paragraphs,
            key = { index, _ -> "paragraph-$index" },
        ) { paragraphIndex, paragraph ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (paragraph.startsWith("![img](") && paragraph.endsWith(")")) {
                    val url = paragraph.substringAfter("![img](").substringBeforeLast(")")
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    )
                } else {
                    AnnotatableParagraph(
                        paragraph = paragraph,
                        paragraphIndex = paragraphIndex,
                        searchQuery = searchQuery,
                        preferences = preferences,
                        colors = colors,
                        annotations = annotations,
                        onAnnotationSelected = onAnnotationSelected,
                        onCreateAnnotationDraft = onCreateAnnotationDraft,
                        modifier = Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotatableParagraph(
    paragraph: String,
    paragraphIndex: Int,
    searchQuery: String,
    preferences: ReaderPreferences,
    colors: ReaderColors,
    annotations: List<ArticleAnnotation>,
    onAnnotationSelected: (ArticleAnnotation) -> Unit,
    onCreateAnnotationDraft: (AnnotationDraftState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var selection by remember { mutableStateOf<ParagraphSelection?>(null) }
    val paragraphAnnotations = remember(annotations, paragraphIndex) {
        annotations.filter { annotation ->
            paragraphIndex in annotation.anchor.startParagraphIndex..annotation.anchor.endParagraphIndex
        }
    }
    val annotatedText = remember(paragraph, paragraphIndex, searchQuery, colors, paragraphAnnotations, selection) {
        paragraph.toAnnotatedParagraph(
            paragraphIndex = paragraphIndex,
            searchQuery = searchQuery,
            colors = colors,
            annotations = paragraphAnnotations,
            selection = selection,
        )
    }

    Text(
        text = annotatedText,
        style = TextStyle(
            fontFamily = preferences.asFontFamily(),
            fontWeight = preferences.asFontWeight(),
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * (preferences.fontSizeSp / 18f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * (preferences.fontSizeSp / 18f),
        ),
        color = colors.content,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .pointerInput(annotatedText, paragraphAnnotations) {
                detectTapGestures { offset ->
                    val position = layoutResult?.getOffsetForPosition(offset)
                    if (position != null) {
                        val annotationId = annotatedText
                            .getStringAnnotations(AnnotationTag, position, position)
                            .firstOrNull()
                            ?.item
                            ?.toLongOrNull()
                        val annotation = paragraphAnnotations.firstOrNull { it.id == annotationId }
                        if (annotation != null) {
                            onAnnotationSelected(annotation)
                        }
                    }
                }
            }
            .pointerInput(paragraph, paragraphIndex) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        layoutResult?.getOffsetForPosition(offset)?.let { position ->
                            selection = ParagraphSelection(position, position)
                        }
                    },
                    onDrag = { change, _ ->
                        val current = selection
                        val position = layoutResult?.getOffsetForPosition(change.position)
                        if (current != null && position != null) {
                            selection = current.copy(endOffset = position)
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        val current = selection?.normalized()
                        selection = null
                        if (current != null && current.startOffset != current.endOffset) {
                            val startOffset = current.startOffset.coerceIn(0, paragraph.length)
                            val endOffset = current.endOffset.coerceIn(0, paragraph.length)
                            val quote = paragraph.substring(startOffset, endOffset).trim()
                            if (quote.isNotBlank()) {
                                val anchor = AnnotationAnchor(
                                    startParagraphIndex = paragraphIndex,
                                    endParagraphIndex = paragraphIndex,
                                    startCharOffset = startOffset,
                                    endCharOffset = endOffset,
                                    prefixText = paragraph.substring(0, startOffset).takeLast(32),
                                    suffixText = paragraph.substring(endOffset).take(32),
                                )
                                onCreateAnnotationDraft(
                                    AnnotationDraftState(
                                        anchor = anchor,
                                        quote = quote,
                                        noteText = "",
                                        color = AnnotationColor.Yellow,
                                    ),
                                )
                            }
                        }
                    },
                    onDragCancel = { selection = null },
                )
            },
    )
}

private fun String.toAnnotatedParagraph(
    paragraphIndex: Int,
    searchQuery: String,
    colors: ReaderColors,
    annotations: List<ArticleAnnotation>,
    selection: ParagraphSelection?,
): AnnotatedString {
    val builder = AnnotatedString.Builder(this)

    annotations.forEach { annotation ->
        val range = annotation.anchor.rangeForParagraph(paragraphIndex, length) ?: return@forEach
        val annotationColor = annotation.color.toComposeColor()
        builder.addStyle(
            style = SpanStyle(
                background = annotationColor.copy(alpha = if (annotation.noteText.isBlank()) 0.34f else 0.46f),
                color = colors.content,
            ),
            start = range.first,
            end = range.last,
        )
        builder.addStringAnnotation(
            tag = AnnotationTag,
            annotation = annotation.id.toString(),
            start = range.first,
            end = range.last,
        )
    }

    selection?.normalized()?.takeIf { it.startOffset != it.endOffset }?.let { selected ->
        builder.addStyle(
            style = SpanStyle(background = colors.selection),
            start = selected.startOffset.coerceIn(0, length),
            end = selected.endOffset.coerceIn(0, length),
        )
    }

    val query = searchQuery.trim()
    if (query.isNotEmpty()) {
        var start = indexOf(query, ignoreCase = true)
        while (start >= 0) {
            val end = start + query.length
            builder.addStyle(
                style = SpanStyle(background = colors.highlight),
                start = start,
                end = end,
            )
            start = indexOf(query, startIndex = end, ignoreCase = true)
        }
    }

    return builder.toAnnotatedString()
}

private fun AnnotationAnchor.rangeForParagraph(paragraphIndex: Int, paragraphLength: Int): IntRange? {
    if (paragraphIndex !in startParagraphIndex..endParagraphIndex) {
        return null
    }
    val start = if (paragraphIndex == startParagraphIndex) startCharOffset else 0
    val end = if (paragraphIndex == endParagraphIndex) endCharOffset else paragraphLength
    val safeStart = start.coerceIn(0, paragraphLength)
    val safeEnd = end.coerceIn(0, paragraphLength)
    if (safeStart == safeEnd) {
        return null
    }
    return safeStart.coerceAtMost(safeEnd)..safeStart.coerceAtLeast(safeEnd)
}

private data class ParagraphSelection(
    val startOffset: Int,
    val endOffset: Int,
) {
    fun normalized(): ParagraphSelection = if (startOffset <= endOffset) {
        this
    } else {
        ParagraphSelection(startOffset = endOffset, endOffset = startOffset)
    }
}

data class AnnotationDraftState(
    val anchor: AnnotationAnchor,
    val quote: String,
    val noteText: String,
    val color: AnnotationColor,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnotationEditorSheet(
    title: String,
    initialNote: String,
    initialColor: AnnotationColor,
    quote: String,
    colors: ReaderColors,
    onDismiss: () -> Unit,
    onSave: (String, AnnotationColor) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    var noteText by rememberSaveable(initialNote) { mutableStateOf(initialNote) }
    var selectedColor by rememberSaveable(initialColor) { mutableStateOf(initialColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = selectedColor.toComposeColor().copy(alpha = 0.28f),
            border = BorderStroke(1.dp, selectedColor.toComposeColor().copy(alpha = 0.56f)),
        ) {
            Text(
                text = quote,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            label = { Text("Note") },
            placeholder = { Text("Add a thought, reminder, or summary") },
            minLines = 4,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnnotationColor.entries.forEach { color ->
                FilterChip(
                    selected = selectedColor == color,
                    onClick = { selectedColor = color },
                    label = { Text(color.label) },
                    leadingIcon = {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = color.toComposeColor(),
                            modifier = Modifier.size(14.dp),
                            content = {},
                        )
                    },
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(quote)) }) {
                Text("Copy quote")
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("Delete")
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(onClick = { onSave(noteText, selectedColor) }) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun AnnotationListSheet(
    annotations: List<ArticleAnnotation>,
    colors: ReaderColors,
    onSync: () -> Unit,
    onOpen: (ArticleAnnotation) -> Unit,
    onEdit: (ArticleAnnotation) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredAnnotations = remember(annotations, query) {
        val needle = query.trim()
        if (needle.isBlank()) {
            annotations
        } else {
            annotations.filter { annotation ->
                annotation.quote.contains(needle, ignoreCase = true) ||
                    annotation.noteText.contains(needle, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Annotations",
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = onSync) {
                Icon(Icons.Outlined.Sync, contentDescription = "Sync annotations")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search annotations") },
            singleLine = true,
        )
        if (filteredAnnotations.isEmpty()) {
            Text(
                text = if (annotations.isEmpty()) {
                    "Long-press text in the article to create your first annotation."
                } else {
                    "No annotations match that search."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredAnnotations, key = { it.id }) { annotation ->
                    AnnotationListItem(
                        annotation = annotation,
                        colors = colors,
                        onOpen = { onOpen(annotation) },
                        onEdit = { onEdit(annotation) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotationListItem(
    annotation: ArticleAnnotation,
    colors: ReaderColors,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = annotation.color.toComposeColor().copy(alpha = 0.20f),
        border = BorderStroke(1.dp, annotation.color.toComposeColor().copy(alpha = 0.44f)),
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = annotation.quote,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (annotation.noteText.isNotBlank()) {
                Text(
                    text = annotation.noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = annotation.syncState.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Notes, contentDescription = null)
                    Text("Edit")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderMetadataPills(
    metadata: ReaderMetadata,
    colors: ReaderColors,
    onOpenOriginalUrl: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReaderPill(
            label = metadata.readingTimeText,
            colors = colors,
            icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
        )
        ReaderPill(
            label = metadata.wordCountText,
            colors = colors,
            icon = { Icon(Icons.Outlined.Article, contentDescription = null) },
        )
        ReaderPill(
            label = metadata.domain,
            colors = colors,
            onClick = onOpenOriginalUrl,
            icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderTaxonomyPills(
    article: ArticleDetail,
    colors: ReaderColors,
) {
    if (article.folder == null && article.tags.isEmpty()) {
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        article.folder?.let { folder ->
            ReaderPill(
                label = folder.name,
                colors = colors,
                icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            )
        }
        article.tags.forEach { tag ->
            ReaderPill(
                label = tag.name,
                colors = colors,
                icon = { Icon(Icons.Outlined.Label, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun ReaderPill(
    label: String,
    colors: ReaderColors,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(onClick = onClick)
        },
        shape = RoundedCornerShape(999.dp),
        color = colors.pillContainer,
        contentColor = colors.pillContent,
        border = BorderStroke(1.dp, colors.pillBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    preferences: ReaderPreferences,
    onUpdateFontFamily: (ReaderFontFamily) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateFontWeight: (ReaderFontWeight) -> Unit,
    onUpdateTheme: (ReaderTheme) -> Unit,
    onUpdateContentWidth: (ReaderContentWidth) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ReaderSettingsTab.Text) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Reader settings",
                style = MaterialTheme.typography.titleLarge,
            )
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ReaderSettingsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) },
                    )
                }
            }
            when (selectedTab) {
                ReaderSettingsTab.Text -> ReaderTextSettings(
                    preferences = preferences,
                    onUpdateFontFamily = onUpdateFontFamily,
                    onUpdateFontSize = onUpdateFontSize,
                    onUpdateFontWeight = onUpdateFontWeight,
                )
                ReaderSettingsTab.Theme -> ReaderThemeSettings(
                    preferences = preferences,
                    onUpdateTheme = onUpdateTheme,
                )
                ReaderSettingsTab.Width -> ReaderWidthSettings(
                    preferences = preferences,
                    onUpdateContentWidth = onUpdateContentWidth,
                )
            }
        }
    }
}

@Composable
private fun ReaderTextSettings(
    preferences: ReaderPreferences,
    onUpdateFontFamily: (ReaderFontFamily) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateFontWeight: (ReaderFontWeight) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                        label = { Text(fontFamily.label) },
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font weight", style = MaterialTheme.typography.titleMedium)
            FontWeightDropdown(
                selected = preferences.fontWeight,
                onSelected = onUpdateFontWeight,
            )
        }
    }
}

@Composable
private fun ReaderThemeSettings(
    preferences: ReaderPreferences,
    onUpdateTheme: (ReaderTheme) -> Unit,
) {
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
                    label = { Text(theme.label) },
                )
            }
        }
    }
}

@Composable
private fun ReaderWidthSettings(
    preferences: ReaderPreferences,
    onUpdateContentWidth: (ReaderContentWidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Content width", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            ReaderContentWidth.entries.forEach { width ->
                FilterChip(
                    selected = preferences.contentWidth == width,
                    onClick = { onUpdateContentWidth(width) },
                    label = { Text(width.label) },
                )
            }
        }
        Text(
            text = "${preferences.contentWidth.maxWidth.value.toInt()}dp",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ReaderPreferences.asFontFamily(): FontFamily = when (fontFamily) {
    ReaderFontFamily.SYSTEM -> FontFamily.Default
    ReaderFontFamily.SERIF -> FontFamily.Serif
    ReaderFontFamily.MONO -> FontFamily.Monospace
    ReaderFontFamily.MERRIWEATHER -> FontFamily(
        Font(R.font.merriweather, FontWeight.Light),
        Font(R.font.merriweather, FontWeight.Normal),
        Font(R.font.merriweather, FontWeight.Bold),
    )
    ReaderFontFamily.LORA -> FontFamily(
        Font(R.font.lora, FontWeight.Light),
        Font(R.font.lora, FontWeight.Normal),
        Font(R.font.lora, FontWeight.Bold),
    )
    ReaderFontFamily.FIRA_SANS -> FontFamily(
        Font(R.font.firasans, FontWeight.Light),
        Font(R.font.firasans, FontWeight.Normal),
        Font(R.font.firasans, FontWeight.Bold),
    )
    ReaderFontFamily.INTER -> FontFamily(
        Font(R.font.inter, FontWeight.Light),
        Font(R.font.inter, FontWeight.Normal),
        Font(R.font.inter, FontWeight.Bold),
    )
}

@Composable
private fun FontWeightDropdown(
    selected: ReaderFontWeight,
    onSelected: (ReaderFontWeight) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
            Text(
                text = selected.label,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ReaderFontWeight.entries.forEach { weight ->
                DropdownMenuItem(
                    text = { Text(weight.label) },
                    onClick = {
                        onSelected(weight)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun ReaderPreferences.asFontWeight(): FontWeight = when (fontWeight) {
    ReaderFontWeight.LIGHT -> FontWeight.Light
    ReaderFontWeight.REGULAR -> FontWeight.Normal
    ReaderFontWeight.BOLD -> FontWeight.Bold
}

private enum class ReaderSettingsTab {
    Text,
    Theme,
    Width,
}

private val ReaderSettingsTab.label: String
    get() = when (this) {
        ReaderSettingsTab.Text -> "Text"
        ReaderSettingsTab.Theme -> "Theme"
        ReaderSettingsTab.Width -> "Width"
    }

private val ReaderFontFamily.label: String
    get() = when (this) {
        ReaderFontFamily.SYSTEM -> "System"
        ReaderFontFamily.SERIF -> "Serif"
        ReaderFontFamily.MONO -> "Mono"
        ReaderFontFamily.MERRIWEATHER -> "Merriweather"
        ReaderFontFamily.LORA -> "Lora"
        ReaderFontFamily.FIRA_SANS -> "Fira Sans"
        ReaderFontFamily.INTER -> "Inter"
    }

private val ReaderFontWeight.label: String
    get() = when (this) {
        ReaderFontWeight.LIGHT -> "Light"
        ReaderFontWeight.REGULAR -> "Regular"
        ReaderFontWeight.BOLD -> "Bold"
    }

private val ReaderTheme.label: String
    get() = when (this) {
        ReaderTheme.LIGHT -> "Light"
        ReaderTheme.DARK -> "Dark"
        ReaderTheme.PAPER -> "Paper"
    }

private val ReaderContentWidth.label: String
    get() = when (this) {
        ReaderContentWidth.COMPACT -> "Compact"
        ReaderContentWidth.COMFORTABLE -> "Comfortable"
        ReaderContentWidth.WIDE -> "Wide"
    }

private val ReaderContentWidth.maxWidth: Dp
    get() = when (this) {
        ReaderContentWidth.COMPACT -> 560.dp
        ReaderContentWidth.COMFORTABLE -> 720.dp
        ReaderContentWidth.WIDE -> 920.dp
    }

private val AnnotationSyncState.label: String
    get() = when (this) {
        AnnotationSyncState.LocalOnly -> "Waiting for wallabag"
        AnnotationSyncState.Pending -> "Sync pending"
        AnnotationSyncState.Synced -> "Synced"
        AnnotationSyncState.Failed -> "Sync failed"
    }

private fun AnnotationColor.toComposeColor(): Color =
    Color(android.graphics.Color.parseColor(hex))

private data class ReaderColors(
    val background: Color,
    val content: Color,
    val muted: Color,
    val highlight: Color,
    val selection: Color,
    val pillContainer: Color,
    val pillContent: Color,
    val pillBorder: Color,
)

@Composable
private fun readerColors(preferences: ReaderPreferences): ReaderColors = when (preferences.theme) {
    ReaderTheme.LIGHT -> ReaderColors(
        background = Color(0xFFFFFBFF),
        content = Color(0xFF1D1B20),
        muted = Color(0xFF625B71),
        highlight = Color(0xFF6750A4).copy(alpha = 0.18f),
        selection = Color(0xFF6750A4).copy(alpha = 0.26f),
        pillContainer = Color(0xFFF4EFFA),
        pillContent = Color(0xFF2A2333),
        pillBorder = Color(0xFFD7CFE4),
    )
    ReaderTheme.DARK -> ReaderColors(
        background = Color(0xFF090909),
        content = Color(0xFFF6F6F6),
        muted = Color(0xFFB5B5B5),
        highlight = Color(0xFF3F7CAC).copy(alpha = 0.36f),
        selection = Color(0xFF89B4FA).copy(alpha = 0.34f),
        pillContainer = Color(0xFF1D1715),
        pillContent = Color(0xFFF3E7E2),
        pillBorder = Color(0xFF6F534C),
    )
    ReaderTheme.PAPER -> ReaderColors(
        background = Color(0xFFF4ECD8),
        content = Color(0xFF2D2418),
        muted = Color(0xFF6C604F),
        highlight = Color(0xFFD8B25A).copy(alpha = 0.36f),
        selection = Color(0xFFD8B25A).copy(alpha = 0.46f),
        pillContainer = Color(0xFFECE0C7),
        pillContent = Color(0xFF34291B),
        pillBorder = Color(0xFFCDBB96),
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

private data class ReaderMetadata(
    val readingTimeText: String,
    val wordCountText: String,
    val domain: String,
)

private fun ArticleDetail.toReaderMetadata(): ReaderMetadata {
    val wordCount = bodyText
        .replace(Regex("""!\[img]\([^)]+\)"""), " ")
        .split(Regex("""\s+"""))
        .map(String::trim)
        .count { it.any(Char::isLetterOrDigit) }
    val readingMinutes = (wordCount.coerceAtLeast(1) + WordsPerMinute - 1) / WordsPerMinute
    val formattedWords = NumberFormat.getIntegerInstance().format(wordCount)
    return ReaderMetadata(
        readingTimeText = "$readingMinutes min read",
        wordCountText = "$formattedWords words",
        domain = originalUrl.toDisplayDomain(),
    )
}

private fun String.toDisplayDomain(): String {
    val host = runCatching { Uri.parse(this).host }.getOrNull()
    return host
        ?.removePrefix("www.")
        ?.takeIf(String::isNotBlank)
        ?: this
}

private const val WordsPerMinute = 200

private const val AnnotationTag = "brownpaper-annotation"
