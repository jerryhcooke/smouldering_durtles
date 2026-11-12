package com.lazy_durtles.wk.ui.screens.session

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.jobs.ReportSessionItemJob
import com.lazy_durtles.wk.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Session summary, ported from SummarySessionFragment: overall and
 * per-question-type correct/incorrect percentages, "Show items" tables,
 * "Resurrect items with incorrect answers", batch star rating controls and a
 * Finish button with a progress bar while delayed reports are uploaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionSummaryScreen(
    onFinishSession: () -> Unit,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    onResurrectSubjects: (LongArray) -> Unit,
    modifier: Modifier = Modifier,
    onSessionTick: () -> Unit = {},
    onBackPressed: () -> Unit = { },
) {
    val session = remember { Session.getInstance() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showItems by remember { mutableStateOf(false) }
    var finishProgress by remember { mutableFloatStateOf(-1f) }
    var starDialog by remember { mutableStateOf<StarDialogState?>(null) }

    val stats = remember { computeSummaryStats(session) }
    val total = stats.total
    val correctPercentage = if (total == 0) 0 else Math.round(stats.correctTotal * 100f / total)

    val incorrectSubjects = remember {
        session.getItems().asSequence()
            .filter { !it.isAbandoned() && it.hasIncorrectAnswers() }
            .mapNotNull { it.getSubject() }
            .sortedWith(
                compareBy<Subject> { it.getTypeOrder() }
                    .thenComparing(
                        if (session.getType().isSrsRelevant()) {
                            compareBy { session.findItemBySubjectId(it.getId())?.getNewSrsStage() }
                        } else {
                            compareBy { it.getSrsStage() }
                        },
                    )
                    .thenComparingInt { it.getLevel() }
                    .thenComparingInt { it.getLessonPosition() }
                    .thenComparingLong { it.getId() },
            )
            .toList()
    }
    val correctSubjects = remember {
        session.getItems().asSequence()
            .filter { !it.isAbandoned() && !it.hasIncorrectAnswers() }
            .mapNotNull { it.getSubject() }
            .sortedWith(
                compareBy<Subject> { it.getTypeOrder() }
                    .thenComparingInt { it.getLevel() }
                    .thenComparingInt { it.getLessonPosition() }
                    .thenComparingLong { it.getId() },
            )
            .toList()
    }

    val hasResurrectable = incorrectSubjects.any { it.isResurrectable() }
    val starRatingsEnabled = GlobalSettings.Other.getEnableStarRatings()

    fun finishSession() {
        if (!session.isDelayed()) {
            session.finish()
            onFinishSession()
            return
        }
        val pending = session.getNumPendingItems()
        finishProgress = 0f
        coroutineScope.launch {
            var count = 0
            withContext(Dispatchers.IO) {
                for (item in WkApplication.getDatabase().sessionItemDao().getAll()) {
                    if (item.isPending()) {
                        ReportSessionItemJob(
                            item.getId(),
                            item.getAssignmentId(),
                            session.getType(),
                            item.getQuestion1Incorrect(),
                            item.getQuestion2Incorrect() + item.getQuestion3Incorrect() + item.getQuestion4Incorrect(),
                            item.getLastAnswer(),
                            false,
                        ).run()
                        count++
                        finishProgress = if (pending > 0) count.toFloat() / pending else 1f
                    }
                }
            }
            session.finish()
            onFinishSession()
        }
    }

    SessionScaffold(
        title = "Session summary",
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        onFinishSession = onFinishSession,
        onSessionTick = onSessionTick,
        onViewSubject = onViewSubject,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .sessionSwipes(onSwipeRight = { finishSession() })
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryStatsCard(
                header = "Overall percentage correct:",
                percentage = correctPercentage,
                stats = stats,
                correct = true,
            )
            SummaryStatsCard(
                header = "Overall percentage incorrect:",
                percentage = 100 - correctPercentage,
                stats = stats,
                correct = false,
            )

            if (!showItems) {
                Button(
                    onClick = { showItems = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Show items")
                }
            } else {
                SubjectListCard("Items with incorrect answers:", incorrectSubjects, onViewSubject)
                SubjectListCard("Items with only correct answers:", correctSubjects, onViewSubject)

                if (starRatingsEnabled) {
                    StarBatchControls(
                        label = "Bulk set star rating",
                        subjects = incorrectSubjects,
                        onConfirmed = { state -> starDialog = state },
                    )
                    StarBatchControls(
                        label = "Bulk set star rating",
                        subjects = correctSubjects,
                        onConfirmed = { state -> starDialog = state },
                    )
                }
            }

            if (hasResurrectable) {
                Button(
                    onClick = {
                        val ids = session.getItems()
                            .filter { it.hasIncorrectAnswers() }
                            .map { it.getId() }
                            .toLongArray()
                        onResurrectSubjects(ids)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Resurrect items with incorrect answers")
                }
            }

            SpecialButtonsRow(onActionPerformed = onSessionTick)

            if (finishProgress >= 0f) {
                LinearProgressIndicator(
                    progress = { finishProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            } else {
                Button(
                    onClick = { finishSession() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Finish")
                }
            }
        }
    }

    starDialog?.let { state ->
        val message = String.format(
            Locale.ROOT,
            "Do you want to set the star rating for %d subject%s to %d star%s?",
            state.subjects.size,
            if (state.subjects.size == 1) "" else "s",
            state.numStars,
            if (state.numStars == 1) "" else "s",
        )
        AlertDialog(
            onDismissRequest = { starDialog = null },
            title = { Text("Set star ratings?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        starDialog = null
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                state.subjects.forEach { subject ->
                                    WkApplication.getDatabase().subjectDao().updateStars(subject.getId(), state.numStars)
                                }
                            }
                            showToast(context, "Star ratings updated")
                        }
                    },
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { starDialog = null }) { Text("No") }
            },
        )
    }
}

private data class StarDialogState(val subjects: List<Subject>, val numStars: Int)

private data class SummaryStats(
    val total: Int,
    val correctTotal: Int,
    val totalRadical: Int,
    val correctRadical: Int,
    val totalKanji: Int,
    val correctKanji: Int,
    val totalVocabulary: Int,
    val correctVocabulary: Int,
    val totalKanaVocabulary: Int,
    val correctKanaVocabulary: Int,
)

private fun computeSummaryStats(session: Session): SummaryStats {
    var correctTotal = 0
    var total = 0
    var totalRadical = 0
    var correctRadical = 0
    var totalKanji = 0
    var correctKanji = 0
    var totalVocabulary = 0
    var correctVocabulary = 0
    var totalKanaVocabulary = 0
    var correctKanaVocabulary = 0
    for (item in session.getItems()) {
        if (item.isAbandoned()) {
            continue
        }
        val incorrect = item.getQuestion1Incorrect() + item.getQuestion2Incorrect() +
            item.getQuestion3Incorrect() + item.getQuestion4Incorrect()
        val subject = item.getSubject() ?: continue
        val type = subject.getType()
        if (type.isRadical()) {
            total++
            totalRadical++
            if (incorrect == 0) {
                correctRadical++
                correctTotal++
            }
        }
        if (type.isKanji()) {
            total++
            totalKanji++
            if (incorrect == 0) {
                correctKanji++
                correctTotal++
            }
        }
        if (type.isVocabulary()) {
            total++
            totalVocabulary++
            if (incorrect == 0) {
                correctVocabulary++
                correctTotal++
            }
        }
        if (type.isKanaVocabulary()) {
            total++
            totalKanaVocabulary++
            if (incorrect == 0) {
                correctKanaVocabulary++
                correctTotal++
            }
        }
    }
    return SummaryStats(
        total, correctTotal,
        totalRadical, correctRadical,
        totalKanji, correctKanji,
        totalVocabulary, correctVocabulary,
        totalKanaVocabulary, correctKanaVocabulary,
    )
}

@Composable
private fun SummaryStatsCard(
    header: String,
    percentage: Int,
    stats: SummaryStats,
    correct: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (correct) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(header, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            InfoRow("Radicals correct:", stats.correctRadical.let { if (correct) it else stats.totalRadical - it }.toString())
            InfoRow("Kanji correct:", stats.correctKanji.let { if (correct) it else stats.totalKanji - it }.toString())
            InfoRow("Vocabulary correct:", stats.correctVocabulary.let { if (correct) it else stats.totalVocabulary - it }.toString())
            InfoRow("Kana Vocabulary correct:", stats.correctKanaVocabulary.let { if (correct) it else stats.totalKanaVocabulary - it }.toString())
        }
    }
}

@Composable
private fun SubjectListCard(
    header: String,
    subjects: List<Subject>,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(header, style = MaterialTheme.typography.titleMedium)
            if (subjects.isEmpty()) {
                Text(
                    text = "None",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            subjects.forEach { subject ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewSubject(subject.getId(), listOf(subject.getId())) }
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(subject.getBackgroundColor()), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = subject.getCharacters() ?: subject.getSlug().orEmpty(),
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = subject.getMeaningRichText("").toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun StarBatchControls(
    label: String,
    subjects: List<Subject>,
    onConfirmed: (StarDialogState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedStars by remember { mutableIntStateOf(0) }
    val starNames = remember {
        (0..5).map { n -> List(5) { i -> if (i < n) "★" else "☆" }.joinToString(" ") }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(starNames[selectedStars])
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                starNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedStars = index
                            expanded = false
                        },
                    )
                }
            }
        }
        Button(
            onClick = { onConfirmed(StarDialogState(subjects, selectedStars)) },
            enabled = subjects.isNotEmpty(),
        ) {
            Text(label)
        }
    }
}
