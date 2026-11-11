package com.smouldering_durtles.wk.ui.screens.settings

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.smouldering_durtles.wk.util.TextUtil

/**
 * Renders a legacy HTML notice (bold text, links, lists) in a TextView, the
 * same way the legacy preference screens displayed their info notices and
 * confirmation warnings. Links are clickable.
 */
@Composable
fun HtmlNoticeText(
    html: String,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod()
                textSize = 14f
                setLinkTextColor(linkColor)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.text = TextUtil.renderHtml(html)
        },
    )
}
