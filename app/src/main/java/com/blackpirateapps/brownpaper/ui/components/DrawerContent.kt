package com.blackpirateapps.brownpaper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.blackpirateapps.brownpaper.domain.model.ArticleListSource
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag

@Composable
fun BrownPaperDrawerContent(
    currentSource: ArticleListSource?,
    currentSourceId: Long?,
    tags: List<Tag>,
    folders: List<Folder>,
    onSelectSource: (ArticleListSource, Long?) -> Unit,
) {
    var tagsExpanded by rememberSaveable { mutableStateOf(true) }
    var foldersExpanded by rememberSaveable { mutableStateOf(true) }

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "BrownPaper",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "Offline-first reading queue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            DrawerItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                selected = currentSource == ArticleListSource.Inbox,
                onClick = { onSelectSource(ArticleListSource.Inbox, null) },
            )
            DrawerItem(
                label = "Likes",
                icon = Icons.Outlined.FavoriteBorder,
                selected = currentSource == ArticleListSource.Likes,
                onClick = { onSelectSource(ArticleListSource.Likes, null) },
            )
            DrawerItem(
                label = "Archived",
                icon = Icons.Outlined.Archive,
                selected = currentSource == ArticleListSource.Archived,
                onClick = { onSelectSource(ArticleListSource.Archived, null) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ExpandableHeader(
                label = "Tags",
                icon = Icons.Outlined.Label,
                expanded = tagsExpanded,
                onToggle = { tagsExpanded = !tagsExpanded },
            )
            AnimatedVisibility(visible = tagsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tags.isEmpty()) {
                        DrawerHint("No tags yet")
                    } else {
                        tags.forEach { tag ->
                            DrawerItem(
                                label = tag.name,
                                icon = Icons.Outlined.Label,
                                selected = currentSource == ArticleListSource.Tag && currentSourceId == tag.id,
                                onClick = { onSelectSource(ArticleListSource.Tag, tag.id) },
                                nested = true,
                            )
                        }
                    }
                }
            }

            ExpandableHeader(
                label = "Folders",
                icon = Icons.Outlined.Folder,
                expanded = foldersExpanded,
                onToggle = { foldersExpanded = !foldersExpanded },
            )
            AnimatedVisibility(visible = foldersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (folders.isEmpty()) {
                        DrawerHint("No folders yet")
                    } else {
                        folders.forEach { folder ->
                            DrawerItem(
                                label = folder.name,
                                icon = Icons.Outlined.Folder,
                                selected = currentSource == ArticleListSource.Folder && currentSourceId == folder.id,
                                onClick = { onSelectSource(ArticleListSource.Folder, folder.id) },
                                nested = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableHeader(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    NavigationDrawerItem(
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label)
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            }
        },
        selected = false,
        onClick = onToggle,
        icon = { Icon(imageVector = icon, contentDescription = null) },
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    nested: Boolean = false,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.padding(start = if (nested) 20.dp else 0.dp),
    )
}

@Composable
private fun DrawerHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp),
    )
}
