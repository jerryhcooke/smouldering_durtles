package com.smouldering_durtles.wk.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.GlobalSettings

/**
 * Keyboard help, a port of the legacy KeyboardHelpActivity: the keyboard help
 * documents, buttons that jump to the keyboard settings, and the four "do it"
 * buttons that apply a recommended set of keyboard settings directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardHelpScreen(
    onBack: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmation by remember { mutableStateOf<String?>(null) }

    fun confirmUpdated() {
        confirmation = "Your settings have been updated."
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Keyboard help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HtmlNoticeText(html = Constants.KEYBOARD_HELP_DOCUMENT_INTRO)
            HelpSection(
                document = Constants.KEYBOARD_HELP_DOCUMENT_1,
                onOpenKeyboardSettings = onOpenKeyboardSettings,
                doItLabel = "Apply: hide password fields, no auto-correct, no learning mode",
                doIt = {
                    GlobalSettings.Keyboard.setForceVisiblePasswordMeaning(true)
                    GlobalSettings.Keyboard.setForceVisiblePasswordReading(true)
                    GlobalSettings.Keyboard.setEnableAutoCorrectMeaning(false)
                    GlobalSettings.Keyboard.setEnableAutoCorrectReading(false)
                    GlobalSettings.Keyboard.setEnableNoLearning(false)
                    confirmUpdated()
                },
            )
            HelpSection(
                document = Constants.KEYBOARD_HELP_DOCUMENT_2,
                onOpenKeyboardSettings = onOpenKeyboardSettings,
                doItLabel = "Apply: hide password fields, no auto-correct, enable learning mode",
                doIt = {
                    GlobalSettings.Keyboard.setForceVisiblePasswordMeaning(true)
                    GlobalSettings.Keyboard.setForceVisiblePasswordReading(true)
                    GlobalSettings.Keyboard.setEnableAutoCorrectMeaning(false)
                    GlobalSettings.Keyboard.setEnableAutoCorrectReading(false)
                    GlobalSettings.Keyboard.setEnableNoLearning(true)
                    confirmUpdated()
                },
            )
            HelpSection(
                document = Constants.KEYBOARD_HELP_DOCUMENT_3,
                onOpenKeyboardSettings = onOpenKeyboardSettings,
                doItLabel = "Apply: show password fields normally",
                doIt = {
                    GlobalSettings.Keyboard.setForceVisiblePasswordMeaning(false)
                    GlobalSettings.Keyboard.setForceVisiblePasswordReading(false)
                    confirmUpdated()
                },
            )
            HelpSection(
                document = Constants.KEYBOARD_HELP_DOCUMENT_4,
                onOpenKeyboardSettings = onOpenKeyboardSettings,
                doItLabel = "Apply: allow non-ASCII keyboards for meanings",
                doIt = {
                    GlobalSettings.Keyboard.setForceAsciiMeaning(false)
                    confirmUpdated()
                },
            )
            HtmlNoticeText(html = Constants.KEYBOARD_HELP_DOCUMENT_5)
            OutlinedButton(
                onClick = onOpenKeyboardSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open keyboard settings")
            }
        }
    }

    confirmation?.let { message ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Keyboard settings updated") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun HelpSection(
    document: String,
    onOpenKeyboardSettings: () -> Unit,
    doItLabel: String,
    doIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HtmlNoticeText(html = document)
        OutlinedButton(
            onClick = onOpenKeyboardSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open keyboard settings")
        }
        Button(
            onClick = doIt,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(doItLabel)
        }
    }
}
