package com.lazy_durtles.wk.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.lazy_durtles.wk.livedata.LiveApiProgress
import com.lazy_durtles.wk.livedata.LiveTaskCounts
import kotlinx.coroutines.delay

/**
 * How long the "sync complete" state is shown before the popover closes itself.
 */
private const val COMPLETED_DISPLAY_MILLIS = 2_000L

/**
 * Distance from the top of the sync button to the top of the popover.
 */
private val POPOVER_GAP = 52.dp

/**
 * The manual sync button for the top app bar, with an attached popover that
 * shows progress bars while a sync is running.
 *
 * <p>
 *     Clicking the button starts a sync (when none is running) and toggles the
 *     popover. The popover also opens by itself whenever a new sync run starts
 *     while this screen is visible, regardless of what triggered it. It stays
 *     open while the user interacts with the rest of the app, and closes when
 *     the close button is tapped, the sync button is tapped again, or shortly
 *     after the sync completes.
 * </p>
 */
@Composable
fun SyncButtonWithPopover(
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val taskCounts = LiveTaskCounts.getInstance().observeAsState()
    // The actual progress values live in static fields on LiveApiProgress;
    // this LiveData value is only a change marker. It must be read below
    // (passed to the content as a parameter), or progress updates would not
    // trigger recomposition.
    val progressTrigger = LiveApiProgress.getInstance().observeAsState()

    val counts = taskCounts.value
    val hasTasks = counts?.isEmpty == false
    var expanded by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    var syncRequested by remember { mutableStateOf(false) }
    var wasSyncing by remember { mutableStateOf(false) }
    var baselineSet by remember { mutableStateOf(false) }
    var maxApiTasks by remember { mutableStateOf(0) }
    var maxAudioTasks by remember { mutableStateOf(0) }
    var maxPitchTasks by remember { mutableStateOf(0) }

    LaunchedEffect(counts) {
        if (counts == null) {
            return@LaunchedEffect
        }
        // Track the high-water mark of each task queue, so the remaining
        // counts can be shown as determinate progress bars.
        if (counts.isEmpty) {
            maxApiTasks = 0
            maxAudioTasks = 0
            maxPitchTasks = 0
        }
        else {
            if (counts.apiCount > maxApiTasks) {
                maxApiTasks = counts.apiCount
            }
            if (counts.audioCount > maxAudioTasks) {
                maxAudioTasks = counts.audioCount
            }
            if (counts.pitchInfoCount > maxPitchTasks) {
                maxPitchTasks = counts.pitchInfoCount
            }
        }
        val syncing = !counts.isEmpty
        if (syncing) {
            syncRequested = false
        }
        if (!baselineSet) {
            baselineSet = true
            wasSyncing = syncing
            return@LaunchedEffect
        }
        if (syncing && !wasSyncing) {
            showCompleted = false
            expanded = true
        }
        else if (!syncing && wasSyncing) {
            showCompleted = true
        }
        wasSyncing = syncing
    }

    LaunchedEffect(showCompleted) {
        if (showCompleted) {
            delay(COMPLETED_DISPLAY_MILLIS)
            expanded = false
            showCompleted = false
        }
    }

    val startSync: () -> Unit = {
        if (!hasTasks) {
            onSyncNow()
            syncRequested = true
        }
        expanded = true
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                if (expanded) {
                    expanded = false
                }
                else {
                    startSync()
                }
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Sync now",
                tint = if (hasTasks) {
                    MaterialTheme.colorScheme.primary
                }
                else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (expanded) {
            SyncProgressPopup(
                hasTasks = hasTasks,
                showCompleted = showCompleted,
                syncRequested = syncRequested,
                progressTrigger = progressTrigger.value,
                showStage = LiveApiProgress.getShow(),
                processed = LiveApiProgress.getNumProcessedEntities(),
                total = LiveApiProgress.getNumEntities(),
                entityName = LiveApiProgress.getEntityName(),
                apiCount = counts?.apiCount ?: 0,
                audioCount = counts?.audioCount ?: 0,
                pitchInfoCount = counts?.pitchInfoCount ?: 0,
                maxApiTasks = maxApiTasks,
                maxAudioTasks = maxAudioTasks,
                maxPitchTasks = maxPitchTasks,
                onDismiss = { expanded = false },
                onSyncNow = startSync,
            )
        }
    }
}

/**
 * The contents of the sync progress popover.
 *
 * <p>
 *     Rendered in a non-focusable [Popup], so taps outside reach the app and
 *     do not dismiss the popover; it is closed explicitly instead.
 * </p>
 *
 * @param progressTrigger a change marker from [LiveApiProgress]; it is not
 * used directly, but reading it keeps this content updating while sync
 * progress advances
 */
@Composable
@Suppress("UNUSED_PARAMETER")
private fun SyncProgressPopup(
    hasTasks: Boolean,
    showCompleted: Boolean,
    syncRequested: Boolean,
    progressTrigger: Any?,
    showStage: Boolean,
    processed: Int,
    total: Int,
    entityName: String,
    apiCount: Int,
    audioCount: Int,
    pitchInfoCount: Int,
    maxApiTasks: Int,
    maxAudioTasks: Int,
    maxPitchTasks: Int,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, with(LocalDensity.current) { POPOVER_GAP.roundToPx() }),
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.width(320.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (hasTasks) {
                    PopupHeader(
                        title = "Syncing…",
                        icon = { SpinningSyncIcon() },
                        onDismiss = onDismiss,
                    )
                    if (showStage) {
                        StageProgressRow(
                            processed = processed,
                            total = total,
                            entityName = entityName,
                        )
                    }
                    else {
                        Text(
                            text = "Preparing sync…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        RoundedProgressBar(progress = null)
                    }
                    TaskQueueRow(label = "Data tasks", remaining = apiCount, maxSeen = maxApiTasks)
                    TaskQueueRow(label = "Audio files", remaining = audioCount, maxSeen = maxAudioTasks)
                    TaskQueueRow(label = "Pitch accents", remaining = pitchInfoCount, maxSeen = maxPitchTasks)
                }
                else if (showCompleted) {
                    PopupHeader(
                        title = "Sync complete",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        onDismiss = onDismiss,
                    )
                }
                else if (syncRequested) {
                    PopupHeader(
                        title = "Starting sync…",
                        icon = { SpinningSyncIcon() },
                        onDismiss = onDismiss,
                    )
                    RoundedProgressBar(progress = null)
                }
                else {
                    PopupHeader(
                        title = "No sync in progress",
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onDismiss = onDismiss,
                    )
                    FilledTonalButton(
                        onClick = onSyncNow,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Sync now")
                    }
                }
            }
        }
    }
}

/**
 * The header row of the popover: icon, title and a close button.
 */
@Composable
private fun PopupHeader(
    title: String,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A sync icon that spins while a sync is in progress.
 */
@Composable
private fun SpinningSyncIcon(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "syncIcon")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing)),
        label = "angle",
    )
    Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = null,
        modifier = modifier
            .size(22.dp)
            .rotate(angle),
        tint = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Progress bar for the entity currently being synced, e.g. "subjects".
 * Indeterminate when the total is not known yet.
 */
@Composable
private fun StageProgressRow(
    processed: Int,
    total: Int,
    entityName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entityName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (total > 0) {
                Text(
                    text = "$processed / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RoundedProgressBar(
            progress = if (total > 0) (processed.toFloat() / total).coerceIn(0f, 1f) else null,
        )
    }
}

/**
 * Progress bar for one of the task queues, shown only while that queue has
 * remaining tasks. Determinate against the high-water mark seen this run,
 * indeterminate when no maximum is known yet.
 */
@Composable
private fun TaskQueueRow(
    label: String,
    remaining: Int,
    maxSeen: Int,
    modifier: Modifier = Modifier,
) {
    if (remaining <= 0) {
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$remaining remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RoundedProgressBar(
            progress = if (maxSeen > 0) ((maxSeen - remaining).toFloat() / maxSeen).coerceIn(0f, 1f) else null,
        )
    }
}

/**
 * A thin rounded progress bar with animated transitions between values.
 * A null progress renders the indeterminate variant.
 */
@Composable
private fun RoundedProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
    else {
        val animated by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 300),
            label = "syncProgress",
        )
        LinearProgressIndicator(
            progress = { animated },
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
