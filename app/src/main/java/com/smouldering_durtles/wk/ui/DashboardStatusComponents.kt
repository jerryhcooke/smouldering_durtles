package com.smouldering_durtles.wk.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.api.ApiState
import java.util.Locale

/**
 * Banner shown when the API is in an error state or when the API token was rejected.
 */
@Composable
fun ApiStatusBanner(
    apiState: ApiState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apiState == ApiState.ERROR) {
        Row(
            modifier = modifier.padding(8.dp),
        ) {
            Text(
                text = "There was an error talking to the WaniKani servers",
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
            ) {
                Text("Retry")
            }
        }
    }
    else if (apiState == ApiState.API_KEY_REJECTED) {
        Row(
            modifier = modifier.padding(8.dp),
        ) {
            Text(
                text = "WaniKani is not accepting your API token. Did you give your API token the correct permissions?",
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
            ) {
                Text("Retry")
            }
            TextButton(
                onClick = onOpenSettings,
            ) {
                Text("Settings")
            }
        }
    }
}

/**
 * Banner shown when the keyboard help has not been dismissed yet.
 */
@Composable
fun KeyboardHelpBanner(
    onView: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
    ) {
        Text(
            text = "Please take a moment to review some hints for your keyboard settings",
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onView,
        ) {
            Text("View")
        }
        TextButton(
            onClick = onDismiss,
        ) {
            Text("Dismiss")
        }
    }
}

/**
 * Text shown while the app performs its first-time setup.
 */
@Composable
fun FirstTimeSetupText(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Please wait while we prepare the app for your account. This may take a minute or more...",
        modifier = modifier,
    )
}

/**
 * Dashboard header: a large "Welcome back" headline, a "★ Level N" pill and
 * the number of days spent on the current level. The username is kept for
 * TalkBack only, matching the mockup's plain headline.
 */
@Composable
fun DashboardHeader(
    level: Int,
    daysAtCurrentLevel: Float,
    username: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Welcome back$username. Level $level. " +
                    String.format(Locale.ROOT, "%.1f days on this level", daysAtCurrentLevel)
            },
    ) {
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelPill(level = level)
            Text(
                text = String.format(Locale.ROOT, "%.1f days on this level", daysAtCurrentLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelPill(level: Int, modifier: Modifier = Modifier) {
    // Subtle fade-in tied to level changes; end state is fully opaque.
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(level) {
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(200))
    }
    Surface(
        modifier = modifier.graphicsLayer { this.alpha = alpha.value },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Level $level", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Text that shows the current sync progress, or null if nothing needs to be shown.
 */
fun syncStatusText(
    hasTasks: Boolean,
    show: Boolean,
    processed: Int,
    total: Int,
    entityName: String,
    syncReminder: Boolean,
): String? {
    if (!hasTasks) {
        return null
    }
    if (show) {
        return "Sync: $processed/$total $entityName"
    }
    if (syncReminder) {
        return "You may have pending unlocks - don't forget to sync"
    }
    return null
}

/**
 * Text that shows the current sync progress; hidden when the text is null.
 */
@Composable
fun SyncStatusText(
    text: String?,
    modifier: Modifier = Modifier,
) {
    if (text != null) {
        Text(
            text = text,
            modifier = modifier,
        )
    }
}
