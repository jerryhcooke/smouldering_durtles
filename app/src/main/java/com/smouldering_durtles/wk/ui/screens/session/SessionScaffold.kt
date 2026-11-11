package com.smouldering_durtles.wk.ui.screens.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.model.Session
import java.util.Locale

/**
 * Common session scaffold: top bar with back / abandon / mute and an overflow
 * menu (wrap-up, view last finished item, back to lesson presentation), plus
 * the abandon and wrap-up confirmation dialogs with legacy texts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionScaffold(
    title: String,
    backgroundColor: Color,
    onFinishSession: () -> Unit,
    onSessionTick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit = { _, _ -> },
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val session = remember { Session.getInstance() }
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(WkApplication.getDatabase().propertiesDao().getIsMuted()) }
    var showAbandonDialog by remember { mutableStateOf(false) }
    var showWrapUpDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    fun abandonSession() {
        session.finish()
        showToast(context, "Session abandoned")
        onFinishSession()
    }

    fun wrapUpSession() {
        session.wrapup()
        showToast(context, "Session wrapping up...")
        onSessionTick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onFinishSession) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (GlobalSettings.UiConfirmations.getUiConfirmAbandonSession()) {
                                showAbandonDialog = true
                            } else {
                                abandonSession()
                            }
                        },
                        enabled = session.canBeAbandoned(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Abandon session",
                        )
                    }
                    IconButton(onClick = {
                        isMuted = !isMuted
                        WkApplication.getDatabase().propertiesDao().setIsMuted(isMuted)
                    }) {
                        Icon(
                            painter = painterResource(
                                if (isMuted) R.drawable.ic_volume_off_24dp else R.drawable.ic_volume_up_24dp,
                            ),
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (session.canBeWrappedUp()) {
                                DropdownMenuItem(
                                    text = { Text("Wrap up session") },
                                    onClick = {
                                        menuExpanded = false
                                        if (GlobalSettings.UiConfirmations.getUiConfirmWrapupSession()) {
                                            showWrapUpDialog = true
                                        } else {
                                            wrapUpSession()
                                        }
                                    },
                                )
                            }
                            if (session.getLastFinishedSubjectId() != -1L) {
                                DropdownMenuItem(
                                    text = { Text("View last finished item") },
                                    onClick = {
                                        menuExpanded = false
                                        onViewSubject(session.getLastFinishedSubjectId(), emptyList())
                                    },
                                )
                            }
                            if (session.getType() == SessionType.LESSON && session.isActive()) {
                                DropdownMenuItem(
                                    text = { Text("Back to lesson presentation") },
                                    onClick = {
                                        menuExpanded = false
                                        session.goBackToPresentation()
                                        onSessionTick()
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        modifier = modifier,
        bottomBar = { bottomBar?.invoke() },
    ) { paddingValues ->
        content(paddingValues)
    }

    if (showAbandonDialog) {
        val numPending = session.getNumPendingItems()
        val numReported = session.getNumReportedItems()
        val numStarted = session.getNumStartedItems()
        val numNotStarted = session.getNumActiveItems() - numStarted
        var message = "Are you sure you want to abandon this session? If you do:"
        if (numPending > 0) {
            message += String.format(Locale.ROOT, "\n- %d finished items will not be reported", numPending)
        }
        if (numReported > 0) {
            message += String.format(Locale.ROOT, "\n- %d already finished items will still be reported", numReported)
        }
        if (numStarted > 0) {
            message += String.format(Locale.ROOT, "\n- %d partially quizzed items will not be reported", numStarted)
        }
        if (numNotStarted > 0) {
            message += String.format(Locale.ROOT, "\n- %d unquizzed items will not be reported", numNotStarted)
        }
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Abandon session?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbandonDialog = false
                        abandonSession()
                    },
                ) { Text("Yes") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showAbandonDialog = false }) { Text("No") }
                    TextButton(
                        onClick = {
                            showAbandonDialog = false
                            GlobalSettings.UiConfirmations.setUiConfirmAbandonSession(false)
                            abandonSession()
                        },
                    ) { Text("Yes and don't ask again") }
                }
            },
        )
    }

    if (showWrapUpDialog) {
        val numActive = session.getNumActiveItems()
        val numStarted = session.getNumStartedItems()
        val numNotStarted = numActive - numStarted
        var message = "Are you sure you want to wrap up this session? If you do:"
        if (numStarted > 0) {
            message += String.format(Locale.ROOT, "\n- %d partially quizzed items will remain in the session", numStarted)
        }
        if (numNotStarted > 0) {
            message += String.format(Locale.ROOT, "\n- %d unquizzed items will be removed from the session", numNotStarted)
        }
        AlertDialog(
            onDismissRequest = { showWrapUpDialog = false },
            title = { Text("Wrap up session?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWrapUpDialog = false
                        wrapUpSession()
                    },
                ) { Text("Yes") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showWrapUpDialog = false }) { Text("No") }
                    TextButton(
                        onClick = {
                            showWrapUpDialog = false
                            GlobalSettings.UiConfirmations.setUiConfirmWrapupSession(false)
                            wrapUpSession()
                        },
                    ) { Text("Yes and don't ask again") }
                }
            },
        )
    }
}
