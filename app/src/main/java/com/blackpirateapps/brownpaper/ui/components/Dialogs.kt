package com.blackpirateapps.brownpaper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackpirateapps.brownpaper.domain.model.Folder
import com.blackpirateapps.brownpaper.domain.model.Tag

@Composable
fun AddUrlDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save link") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Article URL") },
                enabled = !isSaving,
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url) },
                enabled = !isSaving,
            ) {
                Text(if (isSaving) "Saving..." else "Save offline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun ManageTagsDialog(
    availableTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>, List<String>) -> Unit,
) {
    var draftSelection by remember(selectedTagIds) { mutableStateOf(selectedTagIds) }
    var newTags by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage tags") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (availableTags.isEmpty()) {
                    Text(
                        text = "No tags yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    availableTags.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = tag.id in draftSelection,
                                onCheckedChange = { checked ->
                                    draftSelection = if (checked) {
                                        draftSelection + tag.id
                                    } else {
                                        draftSelection - tag.id
                                    }
                                },
                            )
                            Text(text = tag.name)
                        }
                    }
                }

                OutlinedTextField(
                    value = newTags,
                    onValueChange = { newTags = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New tags") },
                    placeholder = { Text("comma, separated, tags") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        draftSelection,
                        newTags.split(",").map(String::trim).filter(String::isNotBlank),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun MoveToFolderDialog(
    availableFolders: List<Folder>,
    currentFolderId: Long?,
    onDismiss: () -> Unit,
    onSave: (Long?, String) -> Unit,
) {
    var selectedFolderId by remember(currentFolderId) { mutableStateOf(currentFolderId) }
    var newFolderName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                    )
                    Text("No folder")
                }
                availableFolders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                        )
                        Text(folder.name)
                    }
                }
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Create folder") },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedFolderId, newFolderName) }) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun SearchInArticleDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search in article") },
        text = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Text to highlight") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onApply(searchQuery) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
