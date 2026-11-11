package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.model.JoyoProgress
import com.smouldering_durtles.wk.model.JlptProgress

/**
 * A card with a table showing the number of kanji in each SRS stage,
 * grouped by Jōyō grade.
 *
 * @param progress the progress data to display
 * @param modifier the modifier
 */
@Composable
fun JoyoProgressTable(
    progress: JoyoProgress,
    modifier: Modifier = Modifier,
) {
    ProgressTableCard(
        title = "Jōyō kanji progress",
        rows = (1..7).map { grade ->
            ProgressRow(
                label = gradeLabel(grade),
                locked = progress.getLocked(grade),
                inProgress = progress.getPrePassed(grade),
                passed = progress.getPassed(grade),
                burned = progress.getBurned(grade),
            )
        },
        modifier = modifier,
    )
}

/**
 * A card with a table showing the number of kanji in each JLPT level.
 *
 * @param progress the progress data to display
 * @param modifier the modifier
 */
@Composable
fun JlptProgressTable(
    progress: JlptProgress,
    modifier: Modifier = Modifier,
) {
    ProgressTableCard(
        title = "JLPT kanji progress",
        rows = (1..5).map { level ->
            ProgressRow(
                label = levelLabel(level),
                locked = progress.getLocked(level),
                inProgress = progress.getPrePassed(level),
                passed = progress.getPassed(level),
                burned = progress.getBurned(level),
            )
        },
        modifier = modifier,
    )
}

/**
 * The label for a Jōyō grade row: "Grade N" for grades 1-6, "Middle school" for grade 7.
 */
private fun gradeLabel(grade: Int): String =
    if (grade <= 6) "Grade $grade" else "Middle school"

/**
 * The label for a JLPT level row: "N5" for level 1, ..., "N1" for level 5.
 */
private fun levelLabel(level: Int): String = "N${6 - level}"

/**
 * One row of a progress table.
 */
private data class ProgressRow(
    val label: String,
    val locked: Int,
    val inProgress: Int,
    val passed: Int,
    val burned: Int,
)

/**
 * A card with a title and a table of progress rows.
 */
@Composable
private fun ProgressTableCard(
    title: String,
    rows: List<ProgressRow>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TableCell(text = "", modifier = Modifier.weight(2f))
                TableCell(text = "Locked", modifier = Modifier.weight(1f))
                TableCell(text = "In progress", modifier = Modifier.weight(1f))
                TableCell(text = "Passed", modifier = Modifier.weight(1f))
                TableCell(text = "Burned", modifier = Modifier.weight(1f))
            }
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    TableCell(text = row.label, modifier = Modifier.weight(2f))
                    TableCell(text = if (row.locked == 0) "" else row.locked.toString(), modifier = Modifier.weight(1f))
                    TableCell(text = if (row.inProgress == 0) "" else row.inProgress.toString(), modifier = Modifier.weight(1f))
                    TableCell(text = if (row.passed == 0) "" else row.passed.toString(), modifier = Modifier.weight(1f))
                    TableCell(text = if (row.burned == 0) "" else row.burned.toString(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier, fontSize = 14.sp)
}
