package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.ui.common.DashboardCard

private val CountColumnWidth = 52.dp

/**
 * A table that shows a breakdown of available lessons and reviews per subject type.
 */
@Composable
fun LessonReviewBreakdownView(
    lessons: List<Subject>,
    reviews: List<Subject>,
    userLevel: Int,
    modifier: Modifier = Modifier,
) {
    if (lessons.isEmpty() && reviews.isEmpty()) {
        return
    }

    val lessonCurrentRad = lessons.count { it.level == userLevel && it.type.isRadical() }
    val lessonCurrentKan = lessons.count { it.level == userLevel && it.type.isKanji() }
    val lessonCurrentVoc = lessons.count { it.level == userLevel && (it.type.isVocabulary() || it.type.isKanaVocabulary()) }
    val lessonPastRad = lessons.count { it.level < userLevel && it.type.isRadical() }
    val lessonPastKan = lessons.count { it.level < userLevel && it.type.isKanji() }
    val lessonPastVoc = lessons.count { it.level < userLevel && (it.type.isVocabulary() || it.type.isKanaVocabulary()) }

    val reviewCurrentRad = reviews.count { it.level == userLevel && it.type.isRadical() }
    val reviewCurrentKan = reviews.count { it.level == userLevel && it.type.isKanji() }
    val reviewCurrentVoc = reviews.count { it.level == userLevel && (it.type.isVocabulary() || it.type.isKanaVocabulary()) }
    val reviewPastRad = reviews.count { it.level < userLevel && it.type.isRadical() }
    val reviewPastKan = reviews.count { it.level < userLevel && it.type.isKanji() }
    val reviewPastVoc = reviews.count { it.level < userLevel && (it.type.isVocabulary() || it.type.isKanaVocabulary()) }

    DashboardCard(modifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Column {
            BreakdownHeaderRow()
            BreakdownCountsRow(
                label = "Current-level lessons",
                counts = listOf(lessonCurrentRad, lessonCurrentKan, lessonCurrentVoc),
            )
            BreakdownCountsRow(
                label = "Earlier-level lessons",
                counts = listOf(lessonPastRad, lessonPastKan, lessonPastVoc),
            )
            BreakdownCountsRow(
                label = "Current-level reviews",
                counts = listOf(reviewCurrentRad, reviewCurrentKan, reviewCurrentVoc),
            )
            BreakdownCountsRow(
                label = "Earlier-level reviews",
                counts = listOf(reviewPastRad, reviewPastKan, reviewPastVoc),
            )
        }
    }
}

/**
 * The header row with the column titles.
 */
@Composable
private fun BreakdownHeaderRow(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 4.dp),
    ) {
        Box(Modifier.weight(1f))
        Box(Modifier.width(CountColumnWidth), contentAlignment = Alignment.Center) {
            Text(
                text = "Radical",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(Modifier.width(CountColumnWidth), contentAlignment = Alignment.Center) {
            Text(
                text = "Kanji",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(Modifier.width(CountColumnWidth), contentAlignment = Alignment.Center) {
            Text(
                text = "Vocab",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * A row with a label and the counts for each subject type. Hidden when all counts are zero.
 */
@Composable
private fun BreakdownCountsRow(
    label: String,
    counts: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (counts.all { it == 0 }) {
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
        )
        counts.forEachIndexed { index, count ->
            Box(Modifier.width(CountColumnWidth), contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    modifier = Modifier
                        .widthIn(min = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(ActiveTheme.getSubjectTypeBackgroundColors()[index]))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color(ActiveTheme.getSubjectTypeTextColors()[index]),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
