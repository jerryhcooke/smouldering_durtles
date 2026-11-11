package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.model.SrsBreakDown
import com.smouldering_durtles.wk.ui.common.DashboardCard

private val BUCKET_LABELS = listOf("Apprentice", "Guru", "Master", "Enlightened", "Burned")

/**
 * The SRS stage breakdown card.
 *
 * Bucket indices match [com.smouldering_durtles.wk.model.AdvancedSearchParameters.srsStages]:
 * 0 = apprentice stages ("prepass:0..max-1"), 1 = guru stages ("pass:0..max-1"),
 * 2 = master, 3 = enlightened, 4 = burned.
 *
 * A titled card: a "SRS stages" header row with the number of items in progress
 * (apprentice through enlightened), followed by tall 2-column tiles (label
 * top-left, count bottom-left) tinted with colors derived from the legacy
 * bucket colors. Clicking a tile opens the search for that bucket, as before.
 * Rows by width: 2 columns below 480dp with the last tile spanning the card,
 * 3 columns plus a spacer from 480dp, and a single row at 840dp or more.
 */
@Composable
fun SrsBreakDownView(
    srsBreakDown: SrsBreakDown,
    onBucketClick: (bucket: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Burned does not count as "in progress"; the mockup total covers apprentice through enlightened.
    val inProgress = (0..3).sumOf { srsBreakDown.getSrsBreakdownCount(it) }
    DashboardCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "SRS stages",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$inProgress in progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Explicit receiver: the UnusedBoxWithConstraintsScope lint check does not
                // resolve the implicit scope receiver (K2 mode, issuetracker 379269842).
                val availableWidth = this.maxWidth
                // 3 columns would be too narrow on a 360dp phone, so phones use 2 columns.
                val singleRow = availableWidth >= 840.dp
                val twoColumns = availableWidth < 480.dp
                val rows: List<List<Int>> = when {
                    singleRow -> listOf(listOf(0, 1, 2, 3, 4))
                    twoColumns -> listOf(listOf(0, 1), listOf(2, 3), listOf(4))
                    else -> listOf(listOf(0, 1, 2), listOf(3, 4))
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rows.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { bucket ->
                                val tileModifier = if (twoColumns && rowIndex == rows.lastIndex) {
                                    Modifier.fillMaxWidth()
                                }
                                else {
                                    Modifier.weight(1f)
                                }
                                BucketTile(
                                    bucket,
                                    BUCKET_LABELS[bucket],
                                    srsBreakDown.getSrsBreakdownCount(bucket),
                                    onBucketClick,
                                    tileModifier,
                                )
                            }
                            if (!singleRow && !twoColumns && rowIndex == rows.lastIndex) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One tall SRS bucket tile: label top-left, count bottom-left.
 */
@Composable
private fun BucketTile(
    bucket: Int,
    label: String,
    count: Int,
    onBucketClick: (bucket: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The mockup shows dark muted tiles with pale tinted text, which the legacy
    // vivid fills would not match; both tile colors are therefore derived from
    // the legacy bucket color (the one place not covered by Material roles).
    val base = Color(ActiveTheme.getShallowStageBucketColors5()[bucket])
    val isLight = ActiveTheme.getCurrentTheme() == ActiveTheme.LIGHT
    val containerColor = if (isLight) lerp(base, Color.White, 0.8f) else lerp(Color.Black, base, 0.45f)
    val contentColor = if (isLight) lerp(base, Color.Black, 0.65f) else lerp(base, Color.White, 0.75f)
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable { onBucketClick(bucket) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}
