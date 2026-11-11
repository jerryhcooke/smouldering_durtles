package com.lazy_durtles.wk.ui

import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.ui.common.DashboardCard
import com.lazy_durtles.wk.util.TextUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A card showing the subjects unlocked in the last 30 days, laid out as a
 * fixed 3-column grid of chips (at most 4 rows, 12 subjects shown; any
 * further subjects are summarized in a "+N more" line).
 *
 * @param subjects the subjects to show
 * @param onSubjectClick invoked with the clicked subject's id and the ids of all subjects
 * @param modifier the modifier
 */
@Composable
fun RecentUnlocksCard(
    subjects: List<Subject>,
    onSubjectClick: (subjectId: Long, allIds: LongArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) return
    DashboardCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Recent unlocks in the last 30 days",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val shown = subjects.take(12)
            val allIds = LongArray(subjects.size) { subjects[it].id }
            shown.chunked(3).forEach { rowSubjects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSubjects.forEach { subject ->
                        SubjectChip(
                            subject = subject,
                            allIds = allIds,
                            onSubjectClick = onSubjectClick,
                            secondaryText = null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowSubjects.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (subjects.size > 12) {
                Text(
                    text = "+${subjects.size - 12} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/**
 * A card showing the subjects in a critical condition, laid out as a fixed
 * 3-column grid of chips (at most 4 rows, 12 subjects shown; any further
 * subjects are summarized in a "+N more" line). Each chip shows the subject's
 * characters with the percentage of correct answers underneath.
 *
 * @param subjects the subjects to show
 * @param onSubjectClick invoked with the clicked subject's id and the ids of all subjects
 * @param modifier the modifier
 */
@Composable
fun CriticalConditionCard(
    subjects: List<Subject>,
    onSubjectClick: (subjectId: Long, allIds: LongArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) return
    DashboardCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Critical condition items",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val shown = subjects.take(12)
            val allIds = LongArray(subjects.size) { subjects[it].id }
            shown.chunked(3).forEach { rowSubjects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSubjects.forEach { subject ->
                        SubjectChip(
                            subject = subject,
                            allIds = allIds,
                            onSubjectClick = onSubjectClick,
                            secondaryText = String.format(Locale.ROOT, "%d%%", subject.percentageCorrect),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowSubjects.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (subjects.size > 12) {
                Text(
                    text = "+${subjects.size - 12} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/**
 * A card showing the subjects burned in the last 30 days, laid out as a fixed
 * 3-column grid of chips (at most 4 rows, 12 subjects shown; any further
 * subjects are summarized in a "+N more" line). Each chip shows the subject's
 * characters with the burned date underneath.
 *
 * @param subjects the subjects to show
 * @param onSubjectClick invoked with the clicked subject's id and the ids of all subjects
 * @param modifier the modifier
 */
@Composable
fun BurnedItemsCard(
    subjects: List<Subject>,
    onSubjectClick: (subjectId: Long, allIds: LongArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (subjects.isEmpty()) return
    DashboardCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Burned items in the last 30 days",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val shown = subjects.take(12)
            val allIds = LongArray(subjects.size) { subjects[it].id }
            shown.chunked(3).forEach { rowSubjects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSubjects.forEach { subject ->
                        SubjectChip(
                            subject = subject,
                            allIds = allIds,
                            onSubjectClick = onSubjectClick,
                            secondaryText = formatBurnedAt(subject.burnedAt).takeIf { it.isNotEmpty() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowSubjects.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (subjects.size > 12) {
                Text(
                    text = "+${subjects.size - 12} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/**
 * A single chip in a subject grid card, showing the subject's characters on
 * its type-colored background with an optional secondary line underneath.
 * Chips without a secondary line keep the unlock date in the semantics
 * description for accessibility.
 *
 * @param subject the subject to show
 * @param allIds the ids of all subjects in the card, for the pager
 * @param onSubjectClick invoked with the clicked subject's id and [allIds]
 * @param secondaryText optional secondary line shown under the characters
 * @param modifier the modifier
 */
@Composable
private fun SubjectChip(
    subject: Subject,
    allIds: LongArray,
    onSubjectClick: (subjectId: Long, allIds: LongArray) -> Unit,
    secondaryText: String?,
    modifier: Modifier = Modifier,
) {
    val description = remember(subject.id, secondaryText) {
        val extra = when {
            secondaryText == null -> if (subject.unlockedAt != 0L) "unlocked " + formatUnlockedAt(subject.unlockedAt) else null
            subject.burnedAt != 0L -> "burned $secondaryText"
            else -> "$secondaryText correct"
        }
        listOfNotNull(
            subject.characters,
            subject.oneMeaning.takeIf { it.isNotBlank() },
            extra,
        ).joinToString(", ")
    }
    Box(
        modifier = modifier
            .heightIn(min = if (secondaryText == null) 56.dp else 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(subject.backgroundColor))
            .clickable(role = Role.Button) { onSubjectClick(subject.id, allIds) }
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        key(subject.id) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            textSize = GlobalSettings.Font.getFontSizeLiveSubjectTable().toFloat()
                            setTextColor(subject.textColor)
                            gravity = Gravity.CENTER
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            text = TextUtil.renderHtml(subject.charactersHtml)
                        }
                    },
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(subject.textColor),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Format the unlockedAt timestamp for display, blank if 0.
 */
private fun formatUnlockedAt(unlockedAt: Long): String =
    if (unlockedAt == 0L) "" else SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(unlockedAt))

/**
 * Format the burnedAt timestamp for display, blank if 0.
 */
private fun formatBurnedAt(burnedAt: Long): String =
    if (burnedAt == 0L) "" else SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(burnedAt))
