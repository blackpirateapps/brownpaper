package com.blackpirateapps.brownpaper.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var pendingJsonToExport by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val json = pendingJsonToExport
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Backup saved successfully")
                    }
                } catch (e: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Failed to save backup: ${e.message}")
                    }
                } finally {
                    pendingJsonToExport = null
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    viewModel.importData(json)
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to read file: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.ExportData -> {
                    pendingJsonToExport = event.json
                    createDocumentLauncher.launch("brownpaper_backup_${System.currentTimeMillis()}.json")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Export your reading queue to a JSON file or restore from a previous backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.exportData() },
                enabled = !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Backup all data")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                enabled = !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Restore from backup")
            }
            
            if (uiState.isProcessing) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }
        }
    }
}
