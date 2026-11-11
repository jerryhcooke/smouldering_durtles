package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A view that describes the number of available lessons and reviews. The two
 * cards are intentionally asymmetric per the mockup: active lessons use the
 * dark purple primary container while active reviews use the stronger primary
 * color, and only active cards are clickable and show the arrow.
 */
@Composable
fun AvailableSessionsView(
    lessons: Int,
    reviews: Int,
    onStartLessons: () -> Unit,
    onStartReviews: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionCountCard(
            label = "Lessons",
            count = lessons,
            icon = Icons.Outlined.Edit,
            caption = "Ready to learn",
            onClick = onStartLessons,
            container = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        SessionCountCard(
            label = "Reviews",
            count = reviews,
            icon = Icons.Outlined.Refresh,
            caption = "Available now",
            onClick = onStartReviews,
            container = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

/**
 * Count card for a single session type. Active cards (count > 0) are
 * clickable and use the given container colors; inactive cards are plain,
 * outlined, and not clickable.
 */
@Composable
private fun SessionCountCard(
    label: String,
    count: Int,
    icon: ImageVector,
    caption: String,
    onClick: (() -> Unit)?,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val active = count > 0
    if (active) {
        Surface(
            onClick = { onClick?.invoke() },
            enabled = active,
            shape = RoundedCornerShape(28.dp),
            color = container,
            contentColor = contentColor,
            modifier = modifier
                .heightIn(min = 150.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$count ${label.lowercase()} available"
                },
        ) {
            SessionCountCardContent(
                label = label,
                count = count,
                icon = icon,
                caption = caption,
                active = active,
                contentColor = contentColor,
            )
        }
    }
    else {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier,
        ) {
            SessionCountCardContent(
                label = label,
                count = count,
                icon = icon,
                caption = caption,
                active = active,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Shared content of a count card: icon bubble and label on top, the large
 * count pushed to the bottom, and the caption below it.
 */
@Composable
private fun SessionCountCardContent(
    label: String,
    count: Int,
    icon: ImageVector,
    caption: String,
    active: Boolean,
    contentColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = contentColor.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Spacer(Modifier.weight(1f))
            if (active) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.85f),
        )
    }
}
