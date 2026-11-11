package com.lazy_durtles.wk.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.R
import com.lazy_durtles.wk.ui.theme.LazyDurtlesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Which backup operation a state or dialog refers to.
 */
private enum class BackupOp {
    EXPORT,
    IMPORT,
}

/**
 * UI state of the backup and restore screen.
 */
private sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class Working(val op: BackupOp) : BackupUiState
    data class Success(val op: BackupOp, val detail: String) : BackupUiState
    data class Error(val op: BackupOp, val message: String) : BackupUiState
}

/**
 * Backup and restore screen. Lets the user export all app settings to a
 * backup file, or import settings from a previously exported backup file,
 * using the system file picker (Storage Access Framework).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<BackupUiState>(BackupUiState.Idle) }
    var confirmOp by remember { mutableStateOf<BackupOp?>(null) }
    val busy = state is BackupUiState.Working

    fun startExport(uri: Uri) {
        state = BackupUiState.Working(BackupOp.EXPORT)
        scope.launch {
            state = try {
                val detail = withContext(Dispatchers.IO) { SettingsBackup.export(context, uri) }
                BackupUiState.Success(BackupOp.EXPORT, detail)
            } catch (t: Throwable) {
                BackupUiState.Error(BackupOp.EXPORT, friendlyError(t))
            }
        }
    }

    fun startImport(uri: Uri) {
        state = BackupUiState.Working(BackupOp.IMPORT)
        scope.launch {
            state = try {
                val detail = withContext(Dispatchers.IO) { SettingsBackup.import(context, uri) }
                BackupUiState.Success(BackupOp.IMPORT, detail)
            } catch (t: Throwable) {
                BackupUiState.Error(BackupOp.IMPORT, friendlyError(t))
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            startExport(uri)
        }
    }
    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            startImport(uri)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Backup and restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard()

            ActionCard(
                iconRes = R.drawable.ic_backup_24dp,
                title = "Export settings",
                description = "Save all app settings to a backup file. " +
                    "Use the file to restore your settings later, or to copy them to another device.",
                buttonText = "Export",
                filledButton = true,
                enabled = !busy,
                onClick = { confirmOp = BackupOp.EXPORT },
            )

            ActionCard(
                iconRes = R.drawable.ic_restore_24dp,
                title = "Import settings",
                description = "Restore app settings from a backup file. " +
                    "This replaces all current settings with the ones from the backup.",
                buttonText = "Import",
                filledButton = false,
                enabled = !busy,
                onClick = { confirmOp = BackupOp.IMPORT },
            )

            AnimatedVisibility(visible = state !is BackupUiState.Idle) {
                StatusCard(state)
            }
        }
    }

    confirmOp?.let { op ->
        AlertDialog(
            onDismissRequest = { confirmOp = null },
            title = { Text(text = if (op == BackupOp.EXPORT) "Export settings?" else "Import settings?") },
            text = {
                Text(
                    text = if (op == BackupOp.EXPORT) {
                        "All app settings will be written to a backup file. " +
                            "You can choose where to save it in the next step."
                    } else {
                        "Your current settings will be replaced by the settings in the backup file. " +
                            "This cannot be undone."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmOp = null
                    if (op == BackupOp.EXPORT) {
                        saveFileLauncher.launch(backupFileName())
                    } else {
                        pickFileLauncher.launch(arrayOf("*/*"))
                    }
                }) {
                    Text(text = if (op == BackupOp.EXPORT) "Export" else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOp = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

/**
 * Card explaining what a backup contains and what it does not.
 */
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(
                    text = "What is in a backup?",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A backup contains all app settings, such as your API token, themes and " +
                        "session behavior. Your review history and downloaded audio are not included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * An action card with an icon, a title, a description and a button.
 */
@Composable
private fun ActionCard(
    iconRes: Int,
    title: String,
    description: String,
    buttonText: String,
    filledButton: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (filledButton) {
                Button(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
                    Text(text = buttonText)
                }
            } else {
                OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
                    Text(text = buttonText)
                }
            }
        }
    }
}

/**
 * Card showing the progress or the result of the current operation.
 */
@Composable
private fun StatusCard(state: BackupUiState) {
    when (state) {
        is BackupUiState.Working -> StatusCardLayout(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = if (state.op == BackupOp.EXPORT) "Exporting settings…" else "Importing settings…",
            detail = null,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        is BackupUiState.Success -> StatusCardLayout(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            title = if (state.op == BackupOp.EXPORT) "Settings exported" else "Settings imported",
            detail = state.detail,
        ) {
            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        is BackupUiState.Error -> StatusCardLayout(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = if (state.op == BackupOp.EXPORT) "Export failed" else "Import failed",
            detail = state.message,
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        BackupUiState.Idle -> {}
    }
}

@Composable
private fun StatusCardLayout(
    containerColor: Color,
    title: String,
    detail: String?,
    icon: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                if (detail != null) {
                    Text(text = detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Suggested file name for a new backup file, matching the legacy format.
 */
private fun backupFileName(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return "lazy_durtles_" + dateFormat.format(Date()) + ".json"
}

/**
 * Map an exception to a message suitable for display in the error card.
 */
private fun friendlyError(t: Throwable): String = when (t) {
    is JsonSyntaxException -> "The selected file is not a valid settings backup."
    is NumberFormatException -> "The backup file contains an invalid number: ${t.message ?: ""}"
    is IllegalArgumentException -> t.message ?: "The backup file contains invalid data."
    is IOException -> t.message ?: "The selected file could not be accessed."
    is SecurityException -> "Access to the selected file was denied."
    else -> t.message ?: t.javaClass.simpleName
}

/**
 * Reads and writes settings backup files. The file format matches the legacy
 * BackupActivity: a JSON object mapping each settings key to an object with a
 * "value" (string form) and a "type" (Java class simple name), so backups made
 * with older versions of the app can still be imported.
 */
private object SettingsBackup {
    private val gson = Gson()

    /**
     * Write all settings to [uri]. Returns a detail message for the UI.
     */
    fun export(context: Context, uri: Uri): String {
        val all = GlobalSettings.getAllSettings()
        val withTypes = LinkedHashMap<String, Map<String, String>>()
        for ((key, value) in all) {
            if (value == null) {
                continue
            }
            withTypes[key] = mapOf(
                "value" to value.toString(),
                "type" to value.javaClass.simpleName,
            )
        }
        val json = gson.toJson(withTypes)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Could not open the selected file for writing")
        stream.use { it.write(json.toByteArray()) }
        return "All ${all.size} settings were saved to ${displayName(context, uri)}."
    }

    /**
     * Read a backup from [uri] and apply it. Returns a detail message for the UI.
     */
    fun import(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected file")
        val text = stream.use { it.bufferedReader().readText() }

        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val data: Map<String, Map<String, String>> =
            gson.fromJson(text, type) ?: throw IllegalArgumentException("The file does not contain a settings backup")

        val settings = HashMap<String, Any>()
        for ((key, item) in data) {
            val valueStr = item["value"] ?: throw IllegalArgumentException("Backup entry '$key' has no value")
            val typeName = item["type"] ?: throw IllegalArgumentException("Backup entry '$key' has no type")
            settings[key] = when (typeName) {
                "Integer" -> valueStr.toInt()
                "Boolean" -> valueStr.toBoolean()
                "Float" -> valueStr.toFloat()
                "Long" -> valueStr.toLong()
                "String" -> valueStr
                else -> throw IllegalArgumentException("Backup entry '$key' has unsupported type '$typeName'")
            }
        }
        GlobalSettings.setAllSettings(settings)
        return "All ${settings.size} settings were restored from ${displayName(context, uri)}."
    }

    /**
     * Best-effort display name of the document behind [uri].
     */
    private fun displayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
                    ?: uri.lastPathSegment
                    ?: "backup file"
        } catch (ignored: Exception) {
            uri.lastPathSegment ?: "backup file"
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupRestoreScreenPreview() {
    LazyDurtlesTheme {
        BackupRestoreScreen()
    }
}
