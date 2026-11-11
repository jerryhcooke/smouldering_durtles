package com.lazy_durtles.wk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.enums.ActiveTheme
import com.lazy_durtles.wk.model.SrsBreakDown
import com.lazy_durtles.wk.util.ThemeUtil
import java.util.Locale

/**
 * The post-60 progress bar.
 */
@Composable
fun Post60ProgressView(
    srsBreakDown: SrsBreakDown,
    showSubsections: Boolean,
    reversed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Post-60 progression",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 16.sp,
        )
        val values = if (showSubsections) {
            (0..10).map { srsBreakDown.getPost60DeepCount(it) }
        } else {
            (0..6).map { srsBreakDown.getPost60ShallowCount(it) }
        }
        val colors = if (showSubsections) {
            ActiveTheme.getStageDeepBucketColors().toList()
        } else {
            ActiveTheme.getShallowStageBucketColors7().toList()
        }
        StackedBar(
            values = if (reversed) values.asReversed() else values,
            colors = if (reversed) colors.asReversed() else colors,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StackedBar(
    values: List<Int>,
    colors: List<Int>,
    modifier: Modifier = Modifier,
) {
    val total = values.sum()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val paddingPx = with(density) { 2.dp.toPx() }
        val maxWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            values.forEachIndexed { index, value ->
                if (value == 0) return@forEachIndexed
                val segmentWidthPx = maxWidthPx * value / total
                val longText = String.format(Locale.ROOT, "%.1f%%", value * 100.0f / total)
                val shortText = String.format(Locale.ROOT, "%d%%", value * 100 / total)
                val label = when {
                    textMeasurer.measure(longText, TextStyle(fontSize = 10.sp)).size.width + paddingPx <= segmentWidthPx -> longText
                    textMeasurer.measure(shortText, TextStyle(fontSize = 10.sp)).size.width + paddingPx <= segmentWidthPx -> shortText
                    else -> null
                }
                Box(
                    modifier = Modifier
                        .weight(value.toFloat())
                        .fillMaxHeight()
                        .background(Color(colors[index])),
                    contentAlignment = Alignment.Center,
                ) {
                    if (label != null) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = if (ThemeUtil.isLightColor(colors[index])) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}
