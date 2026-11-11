package com.smouldering_durtles.wk.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.model.SearchPreset
import com.smouldering_durtles.wk.livedata.LiveSearchPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collect the user's search presets, excluding the hidden internal presets
 * whose names start with NUL, like the legacy LiveSearchPresets.getNames().
 */
@Composable
fun rememberSearchPresets(): List<SearchPreset> {
    val livePresets by LiveSearchPresets.getInstance().observeAsState()
    return remember(livePresets) {
        livePresets.orEmpty().filter { !it.name.startsWith("\u0000") }
    }
}

/**
 * The search presets section from the legacy BrowseOverviewFragment: a preset
 * picker with Search and Delete buttons, shown only when presets exist.
 * Deleting asks for confirmation, like the legacy dialog.
 */
@Composable
fun SearchPresetSection(
    onRunPreset: (preset: SearchPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = rememberSearchPresets()
    if (presets.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selected = presets.firstOrNull { it.name == selectedName } ?: presets.first()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Search presets:",
            style = MaterialTheme.typography.titleLarge,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(selected.name)
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Choose preset",
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                for (preset in presets) {
                    DropdownMenuItem(
                        text = { Text(preset.name) },
                        onClick = {
                            selectedName = preset.name
                            expanded = false
                        },
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(onClick = { onRunPreset(selected) }) {
                Text("Search")
            }
            Button(onClick = { showDeleteDialog = true }) {
                Text("Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete preset?") },
            text = { Text("Are you sure you want to delete the preset named '${selected.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    val name = selected.name
                    scope.launch(Dispatchers.IO) {
                        WkApplication.getDatabase().searchPresetDao().deletePreset(name)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            },
        )
    }
}

/**
 * Dialog to enter a name for a search preset, ported from the legacy
 * "Preset name" dialog in SearchResultFragment.
 */
@Composable
fun SavePresetDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preset name") },
        text = {
            Column {
                Text("Choose a name for the search preset")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotEmpty(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
