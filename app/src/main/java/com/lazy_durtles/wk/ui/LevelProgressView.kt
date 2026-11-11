package com.lazy_durtles.wk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.R
import com.lazy_durtles.wk.enums.ActiveTheme
import com.lazy_durtles.wk.enums.SubjectType
import com.lazy_durtles.wk.model.LevelProgress
import com.lazy_durtles.wk.ui.common.DashboardCard
import com.lazy_durtles.wk.util.ThemeUtil

/**
 * The level progress chart: one row per level/subject-type combination.
 */
@Composable
fun LevelProgressView(
    levelProgress: LevelProgress,
    userLevel: Int,
    onEntryClick: (level: Int, type: SubjectType) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashboardCard(modifier, contentPadding = PaddingValues(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Level progression",
                modifier = Modifier.align(Alignment.Start),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendBucket(index = 0)
                LegendLabel("Passed")
                LegendBucket(index = 1)
                LegendBucket(index = 2)
                LegendBucket(index = 3)
                LegendBucket(index = 4)
                LegendBucket(index = 5)
                LegendBucket(index = 6)
                LegendLabel("Apprentice")
                LegendBucket(index = 7)
                LegendLabel("Initiate")
                LegendBucket(index = 8)
                LegendLabel("Locked")
                LegendBucket(index = 9)
            }
            levelProgress.getEntries().forEach { entry ->
                LevelProgressRow(
                    entry = entry,
                    showTarget = entry.getLevel() == userLevel,
                    onEntryClick = onEntryClick,
                )
            }
        }
    }
}

@Composable
private fun LevelProgressRow(
    entry: LevelProgress.BarEntry,
    showTarget: Boolean,
    onEntryClick: (level: Int, type: SubjectType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onEntryClick(entry.getLevel(), entry.getType()) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .drawWithContent {
                    drawContent()
                    if (showTarget) {
                        val total = entry.getBuckets().sum()
                        val threshold = (total * 9 + 9) / 10
                        drawRect(
                            color = Color(ThemeUtil.getColor(R.attr.colorLevelProgressTarget)),
                            topLeft = Offset(x = size.width * threshold / total, y = 0f),
                            size = Size(width = 2.dp.toPx(), height = size.height),
                        )
                    }
                },
        ) {
            repeat(10) { index ->
                val value = entry.getBuckets()[index]
                if (value > 0) {
                    Box(
                        modifier = Modifier
                            .weight(value.toFloat())
                            .fillMaxHeight()
                            .background(Color(ActiveTheme.getLevelProgressionBucketColors()[index])),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendBucket(
    index: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(8.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(ActiveTheme.getLevelProgressionBucketColors()[index])),
    )
}

@Composable
private fun LegendLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
    )
}
