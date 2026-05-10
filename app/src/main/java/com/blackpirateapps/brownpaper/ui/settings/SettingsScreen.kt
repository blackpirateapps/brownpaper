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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            WallabagSettingsSection(
                uiState = uiState,
                onHostChange = viewModel::updateWallabagHost,
                onUsernameChange = viewModel::updateWallabagUsername,
                onPasswordChange = viewModel::updateWallabagPassword,
                onClientIdChange = viewModel::updateWallabagClientId,
                onClientSecretChange = viewModel::updateWallabagClientSecret,
                onToggleAdvanced = viewModel::toggleWallabagAdvanced,
                onLogin = viewModel::loginWallabag,
                onSync = viewModel::syncWallabag,
                onDisconnect = viewModel::disconnectWallabag,
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

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

@Composable
private fun WallabagSettingsSection(
    uiState: SettingsUiState,
    onHostChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onClientIdChange: (String) -> Unit,
    onClientSecretChange: (String) -> Unit,
    onToggleAdvanced: () -> Unit,
    onLogin: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "wallabag",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (uiState.isWallabagConnected) {
                "Connected as ${uiState.connectedUsername} on ${uiState.connectedHost}"
            } else {
                "Connect an account to sync saved articles across devices."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.lastSyncAtMillis > 0) {
            Text(
                text = "Last sync ${uiState.lastSyncAtMillis.toSettingsDateTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = uiState.wallabagHost,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Host") },
            singleLine = true,
            enabled = !uiState.isLoggingIn && !uiState.isSyncing,
        )
        OutlinedTextField(
            value = uiState.wallabagUsername,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            enabled = !uiState.isLoggingIn && !uiState.isSyncing,
        )
        OutlinedTextField(
            value = uiState.wallabagPassword,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !uiState.isLoggingIn && !uiState.isSyncing,
        )

        TextButton(
            onClick = onToggleAdvanced,
            enabled = !uiState.isLoggingIn && !uiState.isSyncing,
        ) {
            Icon(
                imageVector = if (uiState.showWallabagAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("API client")
        }

        if (uiState.showWallabagAdvanced) {
            OutlinedTextField(
                value = uiState.wallabagClientId,
                onValueChange = onClientIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Client ID") },
                singleLine = true,
                enabled = !uiState.isLoggingIn && !uiState.isSyncing,
            )
            OutlinedTextField(
                value = uiState.wallabagClientSecret,
                onValueChange = onClientSecretChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Client secret") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !uiState.isLoggingIn && !uiState.isSyncing,
            )
        }

        Button(
            onClick = onLogin,
            enabled = !uiState.isLoggingIn &&
                !uiState.isSyncing &&
                uiState.wallabagHost.isNotBlank() &&
                uiState.wallabagUsername.isNotBlank() &&
                uiState.wallabagPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (uiState.isLoggingIn) "Connecting..." else "Connect")
        }

        Button(
            onClick = onSync,
            enabled = uiState.isWallabagConnected && !uiState.isSyncing && !uiState.isLoggingIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Sync, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (uiState.isSyncing) "Syncing..." else "Sync now")
        }

        TextButton(
            onClick = onDisconnect,
            enabled = uiState.isWallabagConnected && !uiState.isSyncing && !uiState.isLoggingIn,
        ) {
            Icon(Icons.Outlined.LinkOff, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Disconnect")
        }

        if (uiState.isLoggingIn || uiState.isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun Long.toSettingsDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    return formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}
