package com.lazy_durtles.wk.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.Constants
import com.lazy_durtles.wk.db.model.AudioDownloadStatus
import com.lazy_durtles.wk.jobs.AbortAudioDownloadJob
import com.lazy_durtles.wk.jobs.DeleteAllAudioJob
import com.lazy_durtles.wk.jobs.ScanAudioDownloadStatusJob
import com.lazy_durtles.wk.jobs.StartAudioDownloadJob
import com.lazy_durtles.wk.livedata.LiveAudioDownloadStatus
import com.lazy_durtles.wk.livedata.LiveAudioMoveStatus
import com.lazy_durtles.wk.livedata.LiveTaskCounts
import com.lazy_durtles.wk.services.JobRunnerService
import com.lazy_durtles.wk.util.AudioUtil
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Overview of vocabulary audio downloads per level bracket, with controls to
 * start / abort downloads, delete all downloaded audio, and move misplaced
 * audio files to the preferred location. A port of the legacy
 * DownloadAudioActivity and DownloadAudioBracketView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadAudioScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val overview by LiveAudioDownloadStatus.getInstance().observeAsState(initial = emptyList())
    val moveStatusUpdate by LiveAudioMoveStatus.getInstance().observeAsState()
    val taskCounts by LiveTaskCounts.getInstance().observeAsState()
    val audioCount = taskCounts?.audioCount ?: 0

    var deleteConfirmOpen by remember { mutableStateOf(false) }
    var hasMisplacedFiles by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        JobRunnerService.schedule(ScanAudioDownloadStatusJob::class.java, "")
    }

    // The legacy activity deactivates any running move task when it pauses.
    DisposableEffect(Unit) {
        onDispose {
            LiveAudioMoveStatus.getInstance().isActive = false
            LiveAudioMoveStatus.getInstance().forceUpdate()
        }
    }

    LaunchedEffect(overview, moveStatusUpdate) {
        hasMisplacedFiles = withContext(Dispatchers.IO) { AudioUtil.hasAnyMisplacedAudioFiles() }
    }

    val moveActive = LiveAudioMoveStatus.getInstance().isActive
    val moveDone = LiveAudioMoveStatus.getInstance().numDone
    val moveTotal = LiveAudioMoveStatus.getInstance().numTotal
    val moveFraction = if (moveTotal > 0) {
        (moveDone.toFloat() / moveTotal.toFloat()).coerceIn(0f, 1f)
    }
    else {
        0f
    }

    fun startMove() {
        val status = LiveAudioMoveStatus.getInstance()
        status.isActive = true
        status.numDone = 0
        status.numTotal = 100
        status.forceUpdate()
        scope.launch {
            withContext(Dispatchers.IO) {
                val numTotal = AudioUtil.getNumMisplacedAudioFiles()
                if (numTotal > 0) {
                    status.numTotal = numTotal
                    status.forceUpdate()
                    AudioUtil.iterateMisplacedAudioFiles { input ->
                        AudioUtil.moveToPreferredLocation(input)
                        status.numDone = status.numDone + 1
                        status.forceUpdate()
                        !status.isActive
                    }
                }
            }
            status.isActive = false
            status.forceUpdate()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Download audio") },
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "You can download vocabulary audio here, which will be played during lessons and reviews." +
                    " Once started, downloads will continue in the background.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Active downloads: $audioCount",
                style = MaterialTheme.typography.titleMedium,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Move audio files from other locations to the preferred location",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = ::startMove, enabled = !moveActive && hasMisplacedFiles) {
                        Text(text = "Move")
                    }
                    OutlinedButton(onClick = {
                        LiveAudioMoveStatus.getInstance().isActive = false
                        LiveAudioMoveStatus.getInstance().forceUpdate()
                    }, enabled = moveActive) {
                        Text(text = "Abort")
                    }
                }
            }

            if (moveActive) {
                LinearProgressIndicator(
                    progress = { moveFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (audioCount > 0) {
                Button(onClick = {
                    JobRunnerService.schedule(AbortAudioDownloadJob::class.java, "")
                }) {
                    Text(text = "Abort")
                }
            }
            else {
                Button(onClick = { deleteConfirmOpen = true }) {
                    Text(text = "Delete all audio")
                }
            }

            val maxLevel = maxOf(overview.size, 60)
            var minLevel = 1
            while (minLevel <= maxLevel) {
                DownloadAudioBracket(
                    overview = overview,
                    minLevel = minLevel,
                    maxLevel = minLevel + 9,
                    audioCount = audioCount,
                )
                minLevel += 10
            }
        }
    }

    if (deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmOpen = false },
            title = { Text(text = "Delete all audio?") },
            text = { HtmlNoticeText(html = Constants.DELETE_AUDIO_WARNING) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmOpen = false
                    JobRunnerService.schedule(DeleteAllAudioJob::class.java, "")
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmOpen = false }) {
                    Text(text = "No")
                }
            },
        )
    }
}

/**
 * One level bracket in the audio download overview: a download button and a
 * summary of how much audio is available for these levels. A port of the
 * legacy DownloadAudioBracketView.
 */
@Composable
private fun DownloadAudioBracket(
    overview: List<AudioDownloadStatus>,
    minLevel: Int,
    maxLevel: Int,
    audioCount: Int,
) {
    var numTotal = 0
    var numNoAudio = 0
    var numMissingAudio = 0
    var numPartialAudio = 0
    var numFullAudio = 0
    for (status in overview) {
        if (status.level < minLevel || status.level > maxLevel) {
            continue
        }
        numTotal += status.numTotal
        numNoAudio += status.numNoAudio
        numMissingAudio += status.numMissingAudio
        numPartialAudio += status.numPartialAudio
        numFullAudio += status.numFullAudio
    }

    val downloadEnabled = audioCount == 0 && (numMissingAudio > 0 || numPartialAudio > 0)

    Column {
        Text(
            text = "Levels %d-%d".format(Locale.ROOT, minLevel, maxLevel),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    JobRunnerService.schedule(
                        StartAudioDownloadJob::class.java,
                        String.format(Locale.ROOT, "%d|%d", minLevel, maxLevel),
                    )
                },
                enabled = downloadEnabled,
            ) {
                Text(text = "Download")
            }
            Text(
                text = if (numMissingAudio == 0 && numPartialAudio == 0) {
                    "Download finished"
                }
                else {
                    "%d/%d available, %d partially done".format(
                        Locale.ROOT,
                        numPartialAudio + numFullAudio,
                        numTotal - numNoAudio,
                        numPartialAudio,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
