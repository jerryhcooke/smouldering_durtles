package com.smouldering_durtles.wk.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.enums.TimeLineBarChartGridStyle
import com.smouldering_durtles.wk.enums.TimeLineBarChartStyle
import com.smouldering_durtles.wk.model.TimeLine
import com.smouldering_durtles.wk.ui.common.DashboardCard
import com.smouldering_durtles.wk.util.ObjectSupport.getWaitTimeAsInformalString
import com.smouldering_durtles.wk.util.TextUtil.formatShortTimeForDisplay
import com.smouldering_durtles.wk.util.ThemeUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max

/**
 * Build the summary text shown above the chart, ported from UpcomingReviewsView.
 * Returns null if there is nothing to report.
 */
fun upcomingReviewsSummary(timeLine: TimeLine, vacationMode: Boolean, nowMillis: Long): String? {
    if (vacationMode) {
        return "Vacation mode is active"
    }
    val sizeDesc = if (timeLine.size <= 48) "${timeLine.size}h" else "${timeLine.size / 24}d"
    val hasAvailable = timeLine.hasAvailableReviews()
    val hasUpcoming = timeLine.hasUpcomingReviews()
    val hasLongTerm = timeLine.hasLongTermUpcomingReviews()
    return when {
        hasAvailable && hasUpcoming ->
            String.format(
                Locale.ROOT, "%d reviews available now · %d in next %s",
                timeLine.numAvailableReviews, timeLine.numUpcomingReviews, sizeDesc,
            )
        hasAvailable && hasLongTerm -> {
            val waitDays = (timeLine.longTermUpcomingReviewDate - nowMillis) * 1.0f / Constants.DAY
            String.format(
                Locale.ROOT, "%d reviews available now · %d in %1.1f days",
                timeLine.numAvailableReviews, timeLine.numLongTermUpcomingReviews, waitDays,
            )
        }
        hasAvailable ->
            String.format(Locale.ROOT, "%d reviews available now", timeLine.numAvailableReviews)
        hasUpcoming -> {
            val waitTime = timeLine.upcomingReviewDate - nowMillis
            String.format(
                Locale.ROOT, "%d reviews %s, %d upcoming in next %s",
                timeLine.numSingleSlotUpcomingReviews, getWaitTimeAsInformalString(waitTime),
                timeLine.numUpcomingReviews, sizeDesc,
            )
        }
        hasLongTerm -> {
            val waitDays = (timeLine.longTermUpcomingReviewDate - nowMillis) * 1.0f / Constants.DAY
            String.format(Locale.ROOT, "%d reviews available in %1.1f days", timeLine.numLongTermUpcomingReviews, waitDays)
        }
        else -> null
    }
}

/**
 * The summary text shown above the chart. Hidden when there is nothing to report.
 */
@Composable
fun UpcomingReviewsSummaryText(text: String?, modifier: Modifier = Modifier) {
    if (text == null) {
        return
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * One data point of the chart: the per-bucket subject counts for one hourly slot,
 * plus the number of level-up items and the cumulative total up to this slot.
 */
private class ChartEntry(
    val index: Int,
    val values: IntArray,
    val numLevelUpItems: Int,
    val cumulativeCount: Int,
) {
    val barCount: Int = values.sum()
}

private fun buildEntries(timeLine: TimeLine, style: TimeLineBarChartStyle): List<ChartEntry> {
    val numSegments = when (style) {
        TimeLineBarChartStyle.SRS_STAGE -> 4
        TimeLineBarChartStyle.NEXT_SRS_STAGE -> 5
        TimeLineBarChartStyle.ITEM_TYPE -> 3
    }
    val slots = timeLine.timeLine
    val entries = mutableListOf<ChartEntry>()
    var cumulative = 0
    for (i in slots.indices) {
        val values = IntArray(numSegments)
        for (subject in slots[i]) {
            val bucket = when (style) {
                TimeLineBarChartStyle.SRS_STAGE -> {
                    val bucket = subject.srsStage.timeLineBarChartBucket
                    if (bucket == 4) 3 else bucket
                }
                TimeLineBarChartStyle.NEXT_SRS_STAGE -> subject.srsStage.getNewStage(0).timeLineBarChartBucket
                TimeLineBarChartStyle.ITEM_TYPE -> subject.type.timeLineBarChartBucket
            }
            if (bucket in values.indices) {
                values[bucket]++
            }
        }
        cumulative += values.sum()
        entries.add(ChartEntry(i, values, timeLine.numRequiredForLevelUp[i], cumulative))
    }
    return entries
}

private fun segmentColorsFor(style: TimeLineBarChartStyle) = when (style) {
    TimeLineBarChartStyle.SRS_STAGE -> ActiveTheme.getShallowStageBucketColors4()
    TimeLineBarChartStyle.NEXT_SRS_STAGE -> ActiveTheme.getShallowStageBucketColors5()
    TimeLineBarChartStyle.ITEM_TYPE -> ActiveTheme.getSubjectTypeBucketColors()
}

private fun legendLabelsFor(style: TimeLineBarChartStyle): List<String> = when (style) {
    TimeLineBarChartStyle.SRS_STAGE -> listOf("Apprentice", "Guru", "Master", "Enlightened")
    TimeLineBarChartStyle.NEXT_SRS_STAGE -> listOf("Apprentice", "Guru", "Master", "Enlightened", "Burned")
    TimeLineBarChartStyle.ITEM_TYPE -> listOf("Radical", "Kanji", "Vocabulary")
}

/**
 * Chart geometry, ported from TimeLineBarChart.onDrawImpl(). All values in pixels.
 */
private class ChartGeometry(
    val originX: Float,
    val originY: Float,
    val axisSizeX: Float,
    val axisSizeY: Float,
    val barAdvance: Float,
    val pixelsPerUnit: Float,
    val pixelsPerCumulative: Float,
    val pixelsPerGrid: Float,
    val axisIntervalY: Int,
)

private fun roundUpInterval(value: Int): Int = when {
    value <= 0 -> 1
    value < 5 -> value
    value < 10 -> 5
    else -> roundUpInterval((value + 9) / 10) * 10
}

private fun Density.computeGeometry(
    width: Float,
    height: Float,
    gridStyle: TimeLineBarChartGridStyle,
    numShownBars: Int,
    maxBarCount: Int,
    totalCount: Int,
): ChartGeometry? {
    if (maxBarCount <= 0 || numShownBars <= 0) {
        return null
    }
    val measurePaint = Paint()
    measurePaint.textSize = 10.dp.toPx()

    val originY = height - 30.dp.toPx()
    val axisSizeY = height - 45.dp.toPx()
    if (axisSizeY <= 0) {
        return null
    }
    // Reserve room above the tallest bar for its value label, so the label
    // cannot collide with the level-up markers drawn at the top of the chart.
    val labelHeadroom = 20.dp.toPx()
    val pixelsPerUnit = (axisSizeY - labelHeadroom) / maxBarCount
    val pixelsPerCumulative = (axisSizeY - labelHeadroom) / max(totalCount, 1)
    val lineSpacing = maxOf(abs(measurePaint.fontSpacing), 10.dp.toPx()) * 1.8f

    val axisIntervalY: Int
    val pixelsPerGrid: Float
    val topLabel: String
    when (gridStyle) {
        TimeLineBarChartGridStyle.FOR_WATERFALL -> {
            pixelsPerGrid = pixelsPerCumulative
            axisIntervalY = roundUpInterval(ceil(lineSpacing / pixelsPerGrid).toInt())
            topLabel = (((totalCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY).toString()
        }
        TimeLineBarChartGridStyle.OFF -> {
            pixelsPerGrid = pixelsPerUnit
            axisIntervalY = 0
            topLabel = ""
        }
        else -> {
            pixelsPerGrid = pixelsPerUnit
            axisIntervalY = roundUpInterval(ceil(lineSpacing / pixelsPerGrid).toInt())
            topLabel = (((maxBarCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY).toString()
        }
    }

    val topLabelWidth = measurePaint.measureText(topLabel)
    val originX = topLabelWidth + 12.dp.toPx()
    val axisSizeX = width - topLabelWidth - 20.dp.toPx()
    if (axisSizeX <= 0) {
        return null
    }
    return ChartGeometry(
        originX = originX,
        originY = originY,
        axisSizeX = axisSizeX,
        axisSizeY = axisSizeY,
        barAdvance = axisSizeX / numShownBars,
        pixelsPerUnit = pixelsPerUnit,
        pixelsPerCumulative = pixelsPerCumulative,
        pixelsPerGrid = pixelsPerGrid,
        axisIntervalY = axisIntervalY,
    )
}

private suspend fun flingScroll(
    initialOffset: Float,
    initialVelocity: Float,
    maxOffset: Float,
    onOffsetChanged: (Float) -> Unit,
) {
    var offset = initialOffset
    var velocity = initialVelocity
    var lastFrameTime = withFrameNanos { it }
    while (abs(velocity) > 50f) {
        val frameTime = withFrameNanos { it }
        val dt = (frameTime - lastFrameTime) / 1e9f
        lastFrameTime = frameTime
        offset = (offset + velocity * dt).coerceIn(0f, maxOffset)
        onOffsetChanged(offset)
        velocity *= exp(-6f * dt)
        if (offset <= 0f || offset >= maxOffset) {
            break
        }
    }
}

/**
 * The stacked bar chart of upcoming reviews, a Compose port of the legacy TimeLineBarChart view.
 *
 * @param timeLine the timeline to display, or null to render nothing
 * @param style how the bars are broken down
 * @param gridStyle style of the horizontal grid lines and Y-axis labels
 * @param showWaterfall whether to draw the cumulative waterfall line
 * @param numShownBars the number of bars visible at once; more bars than this scroll horizontally
 * @param modifier the modifier
 */
@Composable
fun UpcomingReviewsChart(
    timeLine: TimeLine?,
    style: TimeLineBarChartStyle,
    gridStyle: TimeLineBarChartGridStyle,
    showWaterfall: Boolean,
    numShownBars: Int,
    modifier: Modifier = Modifier,
) {
    if (timeLine == null) {
        return
    }
    val entries = remember(timeLine, style) { buildEntries(timeLine, style) }
    val segmentColors = remember(style) { segmentColorsFor(style) }
    val legendLabels = remember(style) { legendLabelsFor(style) }
    val maxBarCount = entries.maxOfOrNull { it.barCount } ?: 0
    val totalCount = entries.sumOf { it.barCount }
    val nextLabel = if (timeLine.size <= 48) "Next ${timeLine.size}h" else "Next ${timeLine.size / 24}d"

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val velocityTracker = remember { VelocityTracker() }

    DashboardCard(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
    ) {
        Column {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reviews forecast",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    nextLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (totalCount == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (timeLine.size <= 48) {
                            "No reviews in the next ${timeLine.size} hours"
                        } else {
                            "No reviews in the next ${timeLine.size / 24} days"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .then(
                            if (entries.size > numShownBars) {
                                Modifier.pointerInput(entries, numShownBars, gridStyle) {
                                    val geometry = computeGeometry(
                                        size.width.toFloat(), size.height.toFloat(),
                                        gridStyle, numShownBars, maxBarCount, totalCount,
                                    )
                                    val maxScroll = if (geometry == null) {
                                        0f
                                    } else {
                                        max(0f, entries.size * geometry.barAdvance - geometry.axisSizeX)
                                    }
                                    detectDragGestures(
                                        onDragStart = {
                                            flingJob?.cancel()
                                            velocityTracker.resetTracking()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            scrollOffset = (scrollOffset - dragAmount.x).coerceIn(0f, maxScroll)
                                        },
                                        onDragEnd = {
                                            val velocity = runCatching { velocityTracker.calculateVelocity().x }
                                                .getOrDefault(0f)
                                            velocityTracker.resetTracking()
                                            if (abs(velocity) > 100f) {
                                                flingJob?.cancel()
                                                flingJob = scope.launch {
                                                    flingScroll(scrollOffset, -velocity, maxScroll) { scrollOffset = it }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            velocityTracker.resetTracking()
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val primary = ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary)
                        val primaryTonedDown = ThemeUtil.getColor(R.attr.colorPrimaryTonedDown)
                        val waterfallColor = ThemeUtil.getColor(R.attr.colorWaterfallLine)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
                        if (totalCount == 0) {
                            return@Canvas
                        }
    
                        val geometry = computeGeometry(
                            size.width, size.height, gridStyle, numShownBars, maxBarCount, totalCount,
                        ) ?: return@Canvas
                        val maxScroll = max(0f, entries.size * geometry.barAdvance - geometry.axisSizeX)
                        val scroll = scrollOffset.coerceIn(0f, maxScroll)
    
                        // Background grid: Y axis, then horizontal grid lines with labels, or just the
                        // X baseline when the grid is off.
                        paint.strokeWidth = 0f
                        paint.color = primary
                        nativeCanvas.drawLine(
                            geometry.originX, geometry.originY, geometry.originX, geometry.originY - geometry.axisSizeY, paint,
                        )
                        paint.textSize = 10.dp.toPx()
                        paint.textAlign = Paint.Align.RIGHT
                        if (geometry.axisIntervalY == 0) {
                            nativeCanvas.drawLine(
                                geometry.originX, geometry.originY, geometry.originX + geometry.axisSizeX, geometry.originY, paint,
                            )
                        } else {
                            var level = 0
                            while (true) {
                                val offset = level * geometry.pixelsPerGrid
                                if (offset >= geometry.axisSizeY) {
                                    break
                                }
                                nativeCanvas.drawLine(
                                    geometry.originX, geometry.originY - offset,
                                    geometry.originX + geometry.axisSizeX, geometry.originY - offset, paint,
                                )
                                if (level > 0) {
                                    nativeCanvas.drawText(
                                        level.toString(),
                                        geometry.originX - 4.dp.toPx(),
                                        geometry.originY - offset - (paint.ascent() + paint.descent()) / 2f,
                                        paint,
                                    )
                                }
                                level += geometry.axisIntervalY
                            }
                        }
    
                        // The stacked bars.
                        paint.textAlign = Paint.Align.CENTER
                        paint.style = Paint.Style.FILL
                        for (entry in entries) {
                            val i = entry.index
                            val left = maxOf(
                                geometry.originX,
                                geometry.originX + geometry.barAdvance * i + geometry.barAdvance * 0.075f - scroll,
                            )
                            val right = minOf(
                                geometry.originX + geometry.axisSizeX,
                                geometry.originX + geometry.barAdvance * i + geometry.barAdvance * 0.925f - scroll,
                            )
                            if (right < geometry.originX || left > geometry.originX + geometry.axisSizeX || left > right) {
                                continue
                            }
                            var bottom = geometry.originY
                            for (j in entry.values.indices) {
                                paint.color = segmentColors[j]
                                val top = bottom - entry.values[j] * geometry.pixelsPerUnit
                                nativeCanvas.drawRect(left, top, right, bottom, paint)
                                bottom = top
                            }
                        }
    
                        // The count above each bar, skipping labels that would overlap.
                        paint.color = primary
                        val placedLabels = mutableListOf<RectF>()
                        for (entry in entries.sortedWith(compareBy({ it.barCount }, { it.index }))) {
                            val x = geometry.originX + geometry.barAdvance * entry.index + geometry.barAdvance / 2 - scroll
                            val y = geometry.originY - entry.barCount * geometry.pixelsPerUnit - 1.dp.toPx() - paint.descent()
                            if (entry.barCount == 0 || x < geometry.originX || x > geometry.originX + geometry.axisSizeX) {
                                continue
                            }
                            val text = entry.barCount.toString()
                            val textWidth = paint.measureText(text)
                            val rect = RectF(
                                x - textWidth / 2 - 1.dp.toPx(),
                                y + paint.ascent() - 1.dp.toPx(),
                                x + textWidth / 2 + 1.dp.toPx(),
                                y + paint.descent() + 1.dp.toPx(),
                            )
                            if (rect.top < geometry.originY - geometry.axisSizeY) {
                                continue
                            }
                            while (placedLabels.isNotEmpty() && placedLabels.first().bottom <= rect.top) {
                                placedLabels.removeAt(0)
                            }
                            if (placedLabels.any { RectF.intersects(it, rect) }) {
                                continue
                            }
                            nativeCanvas.drawText(text, x, y, paint)
                            placedLabels.add(rect)
                        }
    
                        // Level-up markers at the top of the chart: a small arrow plus the count.
                        paint.textSize = 9.dp.toPx()
                        paint.color = primaryTonedDown
                        for (entry in entries) {
                            val x = geometry.originX + geometry.barAdvance * entry.index + geometry.barAdvance / 2 - scroll
                            if (entry.numLevelUpItems == 0 || x < geometry.originX || x > geometry.originX + geometry.axisSizeX) {
                                continue
                            }
                            val arrow = android.graphics.Path().apply {
                                moveTo(x, 0f)
                                lineTo(x - 5.dp.toPx(), 6.dp.toPx())
                                lineTo(x - 2.dp.toPx(), 6.dp.toPx())
                                lineTo(x - 2.dp.toPx(), 9.dp.toPx())
                                lineTo(x + 2.dp.toPx(), 9.dp.toPx())
                                lineTo(x + 2.dp.toPx(), 6.dp.toPx())
                                lineTo(x + 5.dp.toPx(), 6.dp.toPx())
                                close()
                            }
                            nativeCanvas.drawPath(arrow, paint)
                        }
                        var lastMarkerX = -1f
                        for (entry in entries) {
                            val x = geometry.originX + geometry.barAdvance * entry.index + geometry.barAdvance / 2 - scroll
                            if (entry.numLevelUpItems == 0 || x < geometry.originX || x > geometry.originX + geometry.axisSizeX) {
                                continue
                            }
                            val text = entry.numLevelUpItems.toString()
                            val textWidth = paint.measureText(text)
                            if (x - textWidth / 2 - 1.dp.toPx() < lastMarkerX) {
                                continue
                            }
                            lastMarkerX = x + textWidth / 2 + 1.dp.toPx()
                            nativeCanvas.drawText(text, x, 9.dp.toPx() - paint.ascent(), paint)
                        }
    
                        // Time labels on the X axis.
                        paint.color = primary
                        paint.textSize = 10.dp.toPx()
                        paint.textAlign = Paint.Align.CENTER
                        val labelStep = max(1, numShownBars / 6)
                        var lastLabelRight = -1f
                        for (i in entries.indices step labelStep) {
                            val label = formatShortTimeForDisplay(timeLine.firstSlot + i * Constants.HOUR, i >= 24)
                            val x = geometry.originX + geometry.barAdvance * i + geometry.barAdvance / 2 - scroll
                            if (x < geometry.originX || x > geometry.originX + geometry.axisSizeX) {
                                continue
                            }
                            val textWidth = paint.measureText(label)
                            if (x - textWidth / 2 < lastLabelRight) {
                                continue
                            }
                            lastLabelRight = x + textWidth / 2
                            nativeCanvas.drawText(label, x, geometry.originY + 4.dp.toPx() - paint.ascent(), paint)
                        }
    
                        // The waterfall line through the cumulative counts.
                        if (showWaterfall) {
                            paint.color = waterfallColor
                            paint.style = Paint.Style.FILL_AND_STROKE
                            paint.strokeWidth = density
                            var prevX = -1f
                            var prevY = -1f
                            for (entry in entries) {
                                val x = geometry.originX + geometry.barAdvance * entry.index + geometry.barAdvance / 2 - scroll
                                val y = geometry.originY - entry.cumulativeCount * geometry.pixelsPerCumulative
                                if (prevX >= geometry.originX && x <= geometry.originX + geometry.axisSizeX) {
                                    nativeCanvas.drawLine(prevX, prevY, x, y, paint)
                                }
                                prevX = x
                                prevY = y
                            }
                        }
    
                        // The legend below the X axis.
                        paint.style = Paint.Style.FILL
                        paint.strokeWidth = 0f
                        paint.textSize = 10.dp.toPx()
                        paint.textAlign = Paint.Align.LEFT
                        val boxSize = 8.dp.toPx()
                        val gapSize = 5.dp.toPx()
                        var legendX = 8.dp.toPx()
                        val boxY = geometry.originY + 18.dp.toPx()
                        val textY = geometry.originY + 26.dp.toPx()
                        for (i in segmentColors.indices) {
                            paint.color = segmentColors[i]
                            nativeCanvas.drawRect(legendX, boxY, legendX + boxSize, boxY + boxSize, paint)
                            legendX += boxSize + gapSize
                            val label = legendLabels[i]
                            paint.color = primary
                            nativeCanvas.drawText(label, legendX, textY, paint)
                            legendX += paint.measureText(label) + gapSize
                        }
                    }
                }
                }
        }
    }
}
