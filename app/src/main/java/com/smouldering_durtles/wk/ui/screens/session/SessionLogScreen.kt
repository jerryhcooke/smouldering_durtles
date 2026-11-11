package com.smouldering_durtles.wk.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.db.model.SessionItem
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.model.EventItem
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.model.SessionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val LOG_URL_TAG = "URL"

private val LOG_CHARACTER_BOX_SIZE = 40.dp
private val LOG_CHARACTER_BOX_CORNER_SIZE = 8.dp
private val LOG_CHARACTER_BOX_MAX_FONT_SIZE = 18.sp
private val LOG_CHARACTER_BOX_MIN_FONT_SIZE = 8.sp

/**
 * A text segment of a session log line: plain text, or a link.
 */
private data class LogSegment(val text: String, val url: String? = null)

/**
 * The session log screen, styled after the legacy SessionLogFragment: items
 * grouped by status, plus the events section when the full session log is
 * enabled in Advanced -> Other. Reached from the Profile screen; shows the
 * current or most recent session, or an empty state when none exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionLogScreen(
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<SessionItem>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val sessionLog: SessionLog = withContext(Dispatchers.IO) {
            Session.getInstance().sessionLog
        }
        items = Session.getInstance().items
        events = sessionLog.events
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        when {
            loading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() && events.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No active session")
            }
            else -> SessionLogContent(
                items = items,
                events = events,
                onSubjectClick = onSubjectClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun SessionLogContent(
    items: List<SessionItem>,
    events: List<EventItem>,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullLog = GlobalSettings.AdvancedOther.getFullSessionLog()
    LazyColumn(modifier = modifier.fillMaxSize()) {
        for (group in sessionLogGroups(items)) {
            item(key = "header_${group.title}") {
                Text(
                    text = group.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            for (item in group.items) {
                item(key = "item_${item.id}") {
                    SessionLogRow(
                        item = item,
                        fullLog = fullLog,
                        onClick = if (fullLog || group.clickable) {
                            { onSubjectClick(item.id, listOf(item.id)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (fullLog && events.isNotEmpty()) {
            item(key = "header_events") {
                Text(
                    text = "Events",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(events.size, key = { i -> "event_${events.size - i}_${events[i].timestamp}" }) { i ->
                val event = events[i]
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(text = event.text.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * The item groups of the session log, mirroring the grouping in the legacy
 * SessionLogAdapter.initialize(): abandoned, completed, started, not started.
 */
private data class SessionLogGroup(val title: String, val items: List<SessionItem>, val clickable: Boolean)

private fun sessionLogGroups(items: List<SessionItem>): List<SessionLogGroup> {
    val groups = mutableListOf<SessionLogGroup>()
    fun addGroup(title: String, clickable: Boolean, predicate: (SessionItem) -> Boolean) {
        val groupItems = items.filter(predicate)
        if (groupItems.isNotEmpty()) {
            groups.add(SessionLogGroup(title, groupItems, clickable))
        }
    }
    addGroup("Abandoned items", clickable = true) { it.isAbandoned() }
    addGroup("Completed items", clickable = true) { it.isPending() || it.isReported() }
    addGroup("Started items", clickable = false) { it.isStarted() }
    addGroup("Items not started", clickable = false) { it.isActive() && !it.isStarted() }
    return groups
}

@Composable
private fun SessionLogRow(
    item: SessionItem,
    fullLog: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val subject = item.subject
        SessionLogCharacterBox(subject = subject)
        Column {
            Text(text = subject?.characters ?: "", style = MaterialTheme.typography.bodyLarge)
            val details = buildString {
                append(item.subject?.type?.shortDescription ?: "")
                append(" · level ")
                append(item.level)
            }
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (fullLog) {
                Text(
                    text = questionStatusSummary(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The subject character badge of a session log row: a fixed-size rounded box
 * with the characters on a single line, shrunk to fit like the legacy
 * SubjectInfoButtonView, so long vocabulary (止める) never wraps out of the
 * box. Image-only radicals have no characters and render an empty box.
 */
@Composable
private fun SessionLogCharacterBox(
    subject: Subject?,
    modifier: Modifier = Modifier,
) {
    val characters = subject?.characters ?: ""
    val textMeasurer = rememberTextMeasurer()
    val boxSizePx = with(LocalDensity.current) { LOG_CHARACTER_BOX_SIZE.toPx().roundToInt() }
    val fitted = remember(characters, textMeasurer, boxSizePx) {
        fitCharacterText(
            textMeasurer = textMeasurer,
            text = characters,
            boxSizePx = boxSizePx,
            minFontSizeSp = LOG_CHARACTER_BOX_MIN_FONT_SIZE.value,
            maxFontSizeSp = LOG_CHARACTER_BOX_MAX_FONT_SIZE.value,
        )
    }
    Box(
        modifier = modifier
            .size(LOG_CHARACTER_BOX_SIZE)
            .clip(RoundedCornerShape(LOG_CHARACTER_BOX_CORNER_SIZE))
            .background(Color(subject?.getButtonBackgroundColor() ?: 0))
            .drawBehind {
                fitted?.let { layout ->
                    drawText(
                        textLayoutResult = layout,
                        color = Color.White,
                        topLeft = Offset(
                            x = center.x - layout.size.width / 2f,
                            y = center.y - layout.size.height / 2f,
                        ),
                    )
                }
            },
    )
}

/**
 * Largest single-line layout of [text] that fits [boxSizePx] on both axes,
 * mirroring the binary search in the legacy SubjectInfoButtonView. Falls back
 * to the minimum size when even that overflows, so something is always drawn.
 */
private fun fitCharacterText(
    textMeasurer: TextMeasurer,
    text: String,
    boxSizePx: Int,
    minFontSizeSp: Float,
    maxFontSizeSp: Float,
): TextLayoutResult? {
    if (text.isEmpty()) {
        return null
    }
    // Width stays unbounded so the layout reports the natural single-line
    // width; height is capped at the box so overflow shows up in
    // hasVisualOverflow. A bare TextStyle keeps the font's own line metrics.
    val constraints = Constraints(maxHeight = boxSizePx)
    fun measureAt(fontSizeSp: Float): TextLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(fontSize = fontSizeSp.sp),
        softWrap = false,
        maxLines = 1,
        constraints = constraints,
    )
    var low = minFontSizeSp
    var high = maxFontSizeSp
    var best = measureAt(low)
    while (high - low >= 1f) {
        val mid = (low + high) / 2f
        val candidate = measureAt(mid)
        if (!candidate.hasVisualOverflow && candidate.size.width <= boxSizePx) {
            best = candidate
            low = mid
        } else {
            high = mid
        }
    }
    return best
}

/**
 * Question statuses as text, like the legacy session_log_item layout:
 "Q1: correct", "Q2: incorrect", or nothing for questions not yet done.
 */
private fun questionStatusSummary(item: SessionItem): String {
    val parts = mutableListOf<String>()
    for (i in 1..4) {
        val done = when (i) {
            1 -> item.isQuestion1Done()
            2 -> item.isQuestion2Done()
            3 -> item.isQuestion3Done()
            else -> item.isQuestion4Done()
        }
        if (!done) {
            continue
        }
        val incorrect = when (i) {
            1 -> item.question1Incorrect
            2 -> item.question2Incorrect
            3 -> item.question3Incorrect
            else -> item.question4Incorrect
        }
        parts.add("Q$i: " + if (incorrect > 0) "incorrect" else "correct")
    }
    return parts.joinToString(", ")
}
