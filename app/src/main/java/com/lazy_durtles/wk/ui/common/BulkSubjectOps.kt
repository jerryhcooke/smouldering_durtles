package com.lazy_durtles.wk.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.util.WebClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The bulk subject operations (resurrect / burn), ported from the legacy
 * ResurrectActivity / BurnActivity. Shared by the search results screen and
 * the subject info screen.
 */
enum class BulkOpType(val title: String, val actionLabel: String, val failLabel: String) {
    RESURRECT("Resurrect subjects", "Resurrected", "Failed to resurrect"),
    BURN("Burn subjects", "Burned", "Failed to burn"),
}

/**
 * Progress state for the bulk resurrect/burn dialog, ported from the legacy
 * ResurrectActivity / BurnActivity.
 */
class BulkOpState {
    var visible by mutableStateOf(false)
    var running by mutableStateOf(false)
    var todo by mutableIntStateOf(0)
    var success by mutableIntStateOf(0)
    var fail by mutableIntStateOf(0)
    var status by mutableStateOf("")

    fun reset(todoCount: Int) {
        visible = true
        running = false
        todo = todoCount
        success = 0
        fail = 0
        status = ""
    }
}

/**
 * The confirm/start/stop dialog for a bulk resurrect/burn operation.
 */
@Composable
fun BulkOpDialog(
    type: BulkOpType,
    state: BulkOpState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { if (!state.running) onDismiss() },
        title = { Text(type.title) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("To do: ${state.todo}")
                Text("${type.actionLabel}: ${state.success}")
                Text("${type.failLabel}: ${state.fail}")
                Text(state.status)
            }
        },
        confirmButton = {
            TextButton(onClick = onStart, enabled = !state.running && state.todo > 0) {
                Text("Start")
            }
        },
        dismissButton = {
            if (state.running) {
                TextButton(onClick = { state.running = false }) {
                    Text("Stop")
                }
            }
            else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * The bulk resurrect/burn loop, ported from the legacy ResurrectActivity /
 * BurnActivity doInBackground(): log in if needed, then per subject call the
 * web client, patch the assignment, and track progress. Runs on IO.
 */
suspend fun runBulkOp(type: BulkOpType, ids: List<Long>, state: BulkOpState) {
    withContext(Dispatchers.IO) {
        val db = WkApplication.getDatabase()
        val webClient = WebClient.getInstance()
        if (webClient.lastLoginState != 1) {
            state.status = "Status: Logging in..."
            webClient.doLogin()
            state.status = "Status: " + webClient.lastLoginMessage
        }
        for (id in ids) {
            if (!state.running) {
                break
            }
            val subject = db.subjectDao().getById(id)
            when {
                webClient.lastLoginState != 1 -> {
                    state.fail++
                    state.todo--
                }
                subject == null
                    || !(if (type == BulkOpType.RESURRECT) subject.isResurrectable() else subject.isBurnable()) -> {
                    state.success++
                    state.todo--
                }
                else -> {
                    state.status = subject.getInfoTitle(
                        if (type == BulkOpType.RESURRECT) "Status: Resurrecting: " else "Status: Burning: ",
                        "",
                    ).toString()
                    val ok = if (type == BulkOpType.RESURRECT) webClient.resurrect(subject) else webClient.burn(subject)
                    if (ok) {
                        state.success++
                        state.todo--
                    }
                    else {
                        state.fail++
                    }
                }
            }
            if (subject != null) {
                if (type == BulkOpType.RESURRECT) {
                    db.subjectSyncDao().patchAssignment(
                        id,
                        subject.getSrsSystem().getFirstStartedStage().getId(),
                        subject.getUnlockedAt(),
                        subject.getStartedAt(),
                        System.currentTimeMillis(),
                        subject.getPassedAt(),
                        0,
                        System.currentTimeMillis(),
                    )
                }
                else {
                    db.subjectSyncDao().patchAssignment(
                        id,
                        subject.getSrsSystem().getCompletedStage().getId(),
                        subject.getUnlockedAt(),
                        subject.getStartedAt(),
                        0,
                        subject.getPassedAt(),
                        System.currentTimeMillis(),
                        0,
                    )
                }
            }
        }
        state.status = "Status: Finished"
        state.running = false
    }
}

/**
 * Filter the subjects that can take part in the given bulk operation.
 */
fun List<Subject>.filterForBulkOp(type: BulkOpType): List<Long> =
    filter { if (type == BulkOpType.RESURRECT) it.isResurrectable() else it.isBurnable() }
        .map { it.getId() }
