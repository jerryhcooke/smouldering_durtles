package com.smouldering_durtles.wk.ui.screens.browse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.model.PitchInfo
import com.smouldering_durtles.wk.util.ThemeUtil

private const val KANA_DIGRAPHS = "ぁぃぅぇぉゃゅょゎゕゖァィゥェォャュョヮヵヶ"

/**
 * Precomputed mora layout of a reading string, ported from the legacy
 * PitchInfoDiagramView. The X-offsets of each mora in the reading string. The
 * mora are numbered 0 .. n-1; the width of mora i is xpos[i+1] - xpos[i]. The
 * placeholder for the following particle is directly after the last mora. Text
 * bounds measurement is done with an extra kana character at the start and end
 * of the reading text, so xpos[0] must be subtracted to get absolute offsets.
 */
internal class PitchMoraLayout(
    val xpos: IntArray,
    val numMora: Int,
    val heightPx: Int,
    val circleRadiusPx: Float,
    val lineThicknessPx: Float,
)

internal fun computePitchMoraLayout(text: String, textSizePx: Float, density: Float): PitchMoraLayout {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.textSize = textSizePx
    val rect = android.graphics.Rect()
    fun getWidth(value: String): Int {
        paint.getTextBounds("あ" + value + "あ", 0, value.length + 2, rect)
        return rect.width()
    }

    val circleRadius = 3 * density
    val lineThickness = 2 * density
    val metrics = paint.fontMetrics
    val height = (Math.abs(metrics.ascent) + Math.abs(metrics.descent) + metrics.leading).toInt()

    val xpos = IntArray(text.length + 2)
    xpos[0] = getWidth("")
    var numMora = 0
    var i = 0
    while (i < text.length - 1) {
        i += if (KANA_DIGRAPHS.indexOf(text[i + 1]) >= 0) 2 else 1
        numMora++
        xpos[numMora] = getWidth(text.substring(0, i))
    }
    if (i < text.length) {
        numMora++
        xpos[numMora] = getWidth(text)
    }
    xpos[numMora + 1] = getWidth(text + "あ")

    return PitchMoraLayout(xpos, numMora, height, circleRadius, lineThickness)
}

private fun isDotHigh(pitchNumber: Int, mora: Int): Boolean =
    when {
        pitchNumber == 0 -> mora != 0
        pitchNumber == 1 -> mora == 0
        else -> mora > 0 && mora < pitchNumber
    }

private fun pitchInfoColor(pitchNumber: Int, numMora: Int): Color = when {
    pitchNumber == 0 -> Color(ThemeUtil.getColor(R.attr.pitchInfoHeibanColor))
    pitchNumber == 1 -> Color(ThemeUtil.getColor(R.attr.pitchInfoAtamadakaColor))
    pitchNumber == numMora -> Color(ThemeUtil.getColor(R.attr.pitchInfoOdakaColor))
    else -> Color(ThemeUtil.getColor(R.attr.pitchInfoNakadakaColor))
}

/**
 * The pitch accent diagram for one reading: a line of mora dots drawn over an
 * invisible copy of the reading text, ported from the legacy
 * PitchInfoDiagramView. The dot for the following particle is drawn hollow.
 */
@Composable
internal fun PitchInfoDiagram(
    layout: PitchMoraLayout,
    pitchNumber: Int,
    modifier: Modifier = Modifier,
) {
    val color = pitchInfoColor(pitchNumber, layout.numMora)
    val backgroundColor = Color(ThemeUtil.getColor(R.attr.colorBackground))
    val widthPx = layout.xpos[layout.numMora + 1] - layout.xpos[0] + layout.circleRadiusPx * 2

    Canvas(
        modifier = modifier
            .width(with(LocalDensity.current) { widthPx.toDp() })
            .height(with(LocalDensity.current) { layout.heightPx.toDp() }),
    ) {
        val dotYHigh = layout.circleRadiusPx
        val dotYLow = layout.heightPx - layout.circleRadiusPx

        var prevX = -1f
        var prevY = -1f
        for (i in 0..layout.numMora) {
            val x = (layout.xpos[i] + layout.xpos[i + 1]) / 2f - layout.xpos[0]
            val y = if (isDotHigh(pitchNumber, i)) dotYHigh else dotYLow
            if (prevX != -1f) {
                drawLine(color = color, start = Offset(prevX, prevY), end = Offset(x, y), strokeWidth = layout.lineThicknessPx)
            }
            prevX = x
            prevY = y
        }

        for (i in 0..layout.numMora) {
            val x = (layout.xpos[i] + layout.xpos[i + 1]) / 2f - layout.xpos[0]
            val y = if (isDotHigh(pitchNumber, i)) dotYHigh else dotYLow
            if (i == layout.numMora) {
                drawCircle(color = backgroundColor, radius = layout.circleRadiusPx, center = Offset(x, y))
                drawCircle(color = color, radius = layout.circleRadiusPx, center = Offset(x, y), style = Stroke(width = 1f))
            }
            else {
                drawCircle(color = color, radius = layout.circleRadiusPx, center = Offset(x, y))
            }
        }
    }
}

/**
 * The pitch info for one reading: a diagram plus "[n] 頭高"-style label for each
 * applicable pitch info record, followed by the reading itself. Ported from the
 * legacy PitchInfoView.
 */
@Composable
internal fun PitchInfoGroup(
    kana: String,
    pitchInfos: List<PitchInfo>,
    textSizeSp: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val layout = remember(kana, textSizeSp, density) {
        computePitchMoraLayout(kana, with(density) { (textSizeSp + 4).sp.toPx() }, density.density)
    }

    Column(modifier = modifier) {
        for (info in pitchInfos) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PitchInfoDiagram(layout = layout, pitchNumber = info.getPitchNumber())
                Text(
                    text = pitchInfoLabel(info, layout.numMora),
                    fontSize = (textSizeSp + 4).sp,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(text = kana, fontSize = (textSizeSp + 4).sp, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun pitchInfoLabel(info: PitchInfo, numMora: Int): String {
    val sb = StringBuilder()
    sb.append('[').append(info.getPitchNumber()).append("] ")
    when (info.getPitchNumber()) {
        0 -> sb.append("平板")
        1 -> sb.append("頭高")
        else -> {
            if (info.getPitchNumber() == numMora) {
                sb.append("尾高")
            }
            else {
                sb.append("中高")
            }
        }
    }
    if (!info.getPartOfSpeech().isNullOrEmpty()) {
        sb.append(" (").append(info.getPartOfSpeech()).append(')')
    }
    return sb.toString()
}
