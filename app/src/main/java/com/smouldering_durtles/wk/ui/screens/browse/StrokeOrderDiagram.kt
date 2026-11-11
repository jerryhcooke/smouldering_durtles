package com.smouldering_durtles.wk.ui.screens.browse

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.util.ThemeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Compose port of the legacy StrokeDiagramView: a diagram of the stroke order
 * of a character, animated stroke by stroke. The stroke data is SVG-style path
 * data in a 109x109 coordinate space with a custom "T" instruction carrying the
 * stroke number and its label position. Tapping the diagram toggles the
 * animation on or off, as in the legacy view.
 */
@Composable
internal fun StrokeOrderDiagram(
    strokeData: List<String>,
    initialAnimated: Boolean,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    var animated by remember(initialAnimated) { mutableStateOf(initialAnimated) }
    val strokes = remember(strokeData) { strokeData.map(::parseStroke) }
    val measures = remember(strokes) { strokes.map { PathMeasure().apply { setPath(it.path, false) } } }
    val ghostColor = Color(ThemeUtil.getColor(R.attr.strokeDiagramGhostColor))
    val primaryColor = Color(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary))

    var finishedStrokes by remember(strokes, animated) { mutableIntStateOf(if (animated) 0 else strokes.size) }
    var animatingStroke by remember(strokes, animated) { mutableIntStateOf(-1) }
    var fraction by remember(strokes, animated) { mutableFloatStateOf(0f) }

    LaunchedEffect(strokes, animated) {
        if (!animated || strokes.isEmpty()) {
            return@LaunchedEffect
        }
        // The legacy view animates stroke by stroke, lingers for 1.5 s when
        // finished, then starts over.
        while (isActive) {
            for (i in strokes.indices) {
                animatingStroke = i
                fraction = 0f
                delay(200)
                val duration = (measures[i].length * 5).toInt().coerceIn(50, 10_000)
                animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(durationMillis = duration)) { value, _ ->
                    fraction = value
                }
                finishedStrokes = i + 1
                animatingStroke = -1
            }
            delay(1500)
            finishedStrokes = 0
        }
    }

    val labelPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }

    Canvas(
        modifier = modifier
            .size(sizeDp.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { animated = !animated }
            .semantics { contentDescription = "Stroke order diagram, tap to toggle animation" },
    ) {
        val scale = size.width / 109f
        val strokeWidth = size.width / 50f
        labelPaint.textSize = 10.dp.toPx()

        fun drawPaths(list: List<ParsedStroke>, color: Color) {
            withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                for (stroke in list) {
                    drawPath(
                        path = stroke.path,
                        color = color,
                        style = Stroke(width = strokeWidth / scale, cap = StrokeCap.Round),
                    )
                }
            }
        }

        fun drawLabels(list: List<ParsedStroke>, color: Color) {
            for (stroke in list) {
                labelPaint.color = color.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    stroke.strokeNumber.toString(),
                    stroke.labelX * scale,
                    stroke.labelY * scale,
                    labelPaint,
                )
            }
        }

        drawPaths(strokes.drop(finishedStrokes), ghostColor)
        drawPaths(strokes.take(finishedStrokes), primaryColor)
        drawLabels(strokes.drop(finishedStrokes + 1), ghostColor)
        drawLabels(strokes.take(finishedStrokes + 1), primaryColor)

        if (animatingStroke in strokes.indices) {
            val measure = measures[animatingStroke]
            val length = measure.length
            val partial = Path()
            measure.getSegment(0f, length * fraction, partial, true)
            withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                drawPath(
                    path = partial,
                    color = primaryColor,
                    style = Stroke(width = strokeWidth / scale, cap = StrokeCap.Round),
                )
            }
            val pos = measure.getPosition(length * fraction)
            if (!pos.isUnspecified) {
                drawCircle(color = primaryColor, radius = strokeWidth / 2, center = Offset(pos.x * scale, pos.y * scale))
            }
        }
    }
}

private val svgInstructionPattern = Regex("([a-zA-Z])([^a-zA-Z]+)")
private val svgCoordinatesPattern = Regex("-?\\d*\\.?\\d*")

/**
 * One parsed stroke: the drawn path in the 109x109 input coordinate space,
 * plus the position and number of its order label.
 */
private class ParsedStroke {
    val path = Path()
    var labelX = 0f
    var labelY = 0f
    var strokeNumber = 0
}

private fun splitCoordinates(str: String): List<Float> =
    svgCoordinatesPattern.findAll(str)
        .map { it.value }
        .filter { it.isNotEmpty() }
        .map { it.toFloat() }
        .toList()

private fun parseStroke(pathData: String): ParsedStroke {
    val stroke = ParsedStroke()
    try {
        parseStrokeHelper(stroke, pathData)
    } catch (e: Exception) {
        // Keep whatever was parsed before the error, like the legacy parser.
    }
    return stroke
}

private fun parseStrokeHelper(stroke: ParsedStroke, pathData: String) {
    var lastX = 0f
    var lastY = 0f
    var lastX1 = 0f
    var lastY1 = 0f
    var subPathStartX = 0f
    var subPathStartY = 0f
    var curve = false

    for (match in svgInstructionPattern.findAll(pathData)) {
        val command = match.groupValues[1][0]
        val coordinates = splitCoordinates(match.groupValues[2])

        when (command) {
            'T' -> {
                stroke.strokeNumber = coordinates[0].toInt()
                stroke.labelX = coordinates[1]
                stroke.labelY = coordinates[2]
            }
            'm', 'M' -> {
                val x = coordinates[0]
                val y = coordinates[1]
                if (command == 'M') {
                    subPathStartX = x
                    subPathStartY = y
                    stroke.path.moveTo(x, y)
                    lastX = x
                    lastY = y
                }
                else {
                    subPathStartX += x
                    subPathStartY += y
                    stroke.path.moveTo(lastX + x, lastY + y)
                    lastX += x
                    lastY += y
                }
            }
            'l', 'L' -> {
                val x = coordinates[0]
                val y = coordinates[1]
                if (command == 'L') {
                    stroke.path.lineTo(x, y)
                    lastX = x
                    lastY = y
                }
                else {
                    stroke.path.lineTo(lastX + x, lastY + y)
                    lastX += x
                    lastY += y
                }
            }
            'v', 'V' -> {
                for (y in coordinates) {
                    if (command == 'V') {
                        stroke.path.lineTo(lastX, y)
                        lastY = y
                    }
                    else {
                        stroke.path.lineTo(lastX, lastY + y)
                        lastY += y
                    }
                }
            }
            'h', 'H' -> {
                for (x in coordinates) {
                    if (command == 'H') {
                        stroke.path.lineTo(x, lastY)
                        lastX = x
                    }
                    else {
                        stroke.path.lineTo(lastX + x, lastY)
                        lastX += x
                    }
                }
            }
            'c', 'C' -> {
                curve = true
                var index = 0
                while (index + 6 <= coordinates.size) {
                    var x1 = coordinates[index]
                    var y1 = coordinates[index + 1]
                    var x2 = coordinates[index + 2]
                    var y2 = coordinates[index + 3]
                    var x = coordinates[index + 4]
                    var y = coordinates[index + 5]
                    if (command == 'c') {
                        x1 += lastX
                        x2 += lastX
                        x += lastX
                        y1 += lastY
                        y2 += lastY
                        y += lastY
                    }
                    stroke.path.cubicTo(x1, y1, x2, y2, x, y)
                    lastX1 = x2
                    lastY1 = y2
                    lastX = x
                    lastY = y
                    index += 6
                }
            }
            's', 'S' -> {
                curve = true
                var index = 0
                while (index + 4 <= coordinates.size) {
                    var x2 = coordinates[index]
                    var y2 = coordinates[index + 1]
                    var x = coordinates[index + 2]
                    var y = coordinates[index + 3]
                    if (command == 's') {
                        x2 += lastX
                        x += lastX
                        y2 += lastY
                        y += lastY
                    }
                    val x1 = 2 * lastX - lastX1
                    val y1 = 2 * lastY - lastY1
                    stroke.path.cubicTo(x1, y1, x2, y2, x, y)
                    lastX1 = x2
                    lastY1 = y2
                    lastX = x
                    lastY = y
                    index += 4
                }
            }
            'z', 'Z' -> {
                stroke.path.close()
                stroke.path.moveTo(subPathStartX, subPathStartY)
                lastX = subPathStartX
                lastY = subPathStartY
                lastX1 = subPathStartX
                lastY1 = subPathStartY
                curve = true
            }
        }

        if (!curve) {
            lastX1 = lastX
            lastY1 = lastY
        }
    }
}
