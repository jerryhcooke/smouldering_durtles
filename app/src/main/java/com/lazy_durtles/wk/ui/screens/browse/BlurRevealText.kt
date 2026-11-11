package com.lazy_durtles.wk.ui.screens.browse

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.GlobalSettings

/**
 * One context sentence's English translation, hidden behind a blur until
 * tapped when the "hide sentence translations" setting is enabled. Ported from
 * the legacy showContextSentence() blur-reveal behavior: the blur radius is a
 * third of the text size.
 *
 * Modifier.blur() is a no-op below API 31, so older versions fall back to
 * rendering the text at very low alpha, which keeps the hidden-until-tapped
 * behavior.
 */
@Composable
internal fun BlurRevealSentenceText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val hide = GlobalSettings.SubjectInfo.getHideSentenceTranslations()
    var revealed by remember(text) { mutableStateOf(!hide) }
    val density = LocalDensity.current
    val blurRadius = with(density) {
        (MaterialTheme.typography.bodyMedium.fontSize.toPx() / 3f).toDp()
    }

    Box(modifier = modifier.clickable(enabled = hide && !revealed) {
        revealed = true
    }) {
        val hidden = hide && !revealed
        val textModifier = when {
            hidden && Build.VERSION.SDK_INT >= 31 -> Modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            hidden -> Modifier.alpha(0.15f)
            else -> Modifier
        }
        Text(
            text = text,
            modifier = textModifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
