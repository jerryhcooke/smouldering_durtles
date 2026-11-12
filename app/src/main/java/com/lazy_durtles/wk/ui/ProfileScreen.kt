package com.lazy_durtles.wk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.lazy_durtles.wk.livedata.LiveLevelDuration
import com.lazy_durtles.wk.livedata.LiveReviewTotals
import com.lazy_durtles.wk.livedata.LiveVacationMode
import com.lazy_durtles.wk.model.LevelDuration
import com.lazy_durtles.wk.model.ReviewTotals
import java.util.Locale

private const val DEFAULT_AVATAR_URL =
    "https://assets.wanikani.com/assets/user/avatars/default-963f016d.svg"

/**
 * The profile screen. The legacy app has no profile screen of its own; this
 * shows the user info the app has available: the default avatar, the username,
 * the level, and lifetime lesson/answer statistics from the subject database.
 */
@Composable
fun ProfileScreen(
    onGoToSettings: () -> Unit,
    onViewSessionLog: () -> Unit = {},
    onBackPressed: () -> Unit = { },
    modifier: Modifier = Modifier,
) {
    val levelDuration = LiveLevelDuration.getInstance().observeAsState()
    val vacationMode = LiveVacationMode.getInstance().observeAsState(initial = false)
    val reviewTotals = LiveReviewTotals.getInstance().observeAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileHeader(
                username = levelDuration.value?.username ?: "",
                level = levelDuration.value?.level,
            )
            ProfileAccountCard(
                levelDuration = levelDuration.value,
                vacationMode = vacationMode.value,
            )
            ProfileTotalsCard(totals = reviewTotals.value)
            ProfileAccuracyCard(totals = reviewTotals.value)
            ProfileSessionCard(onViewSessionLog = onViewSessionLog)
        }
        IconButton(
            onClick = onGoToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    level: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileAvatar()
        Text(
            text = username.ifEmpty { "—" },
            style = MaterialTheme.typography.headlineSmall,
        )
        if (level != null && level > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(modifier: Modifier = Modifier) {
    // The default avatar is an SVG; Coil needs the SvgDecoder to render it.
    // Without it the image silently stays blank.
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val fallback = rememberVectorPainter(Icons.Default.Person)
    AsyncImage(
        model = DEFAULT_AVATAR_URL,
        contentDescription = "Profile picture",
        imageLoader = imageLoader,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun ProfileCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun ProfileAccountCard(
    levelDuration: LevelDuration?,
    vacationMode: Boolean,
    modifier: Modifier = Modifier,
) {
    ProfileCard(title = "Account", modifier = modifier) {
        ProfileStatRow(
            label = "Time on this level",
            value = levelDuration?.let {
                String.format(Locale.ROOT, "%.1f days", it.daysAtCurrentLevel)
            },
        )
        ProfileStatRow(label = "Vacation mode", value = if (vacationMode) "On" else "Off")
    }
}

@Composable
private fun ProfileSessionCard(
    onViewSessionLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileCard(title = "Session", modifier = modifier) {
        ProfileLinkRow(label = "Session log", onClick = onViewSessionLog)
    }
}

@Composable
private fun ProfileTotalsCard(
    totals: ReviewTotals?,
    modifier: Modifier = Modifier,
) {
    ProfileCard(title = "Lessons and answers", modifier = modifier) {
        ProfileStatRow(label = "Lessons completed", value = totals?.lessonsCompleted)
        ProfileStatRow(
            label = "Answers given",
            value = totals?.let {
                it.meaningCorrect + it.meaningIncorrect + it.readingCorrect + it.readingIncorrect
            },
        )
        ProfileStatRow(
            label = "Correct answers",
            value = totals?.let { it.meaningCorrect + it.readingCorrect },
        )
    }
}

@Composable
private fun ProfileAccuracyCard(
    totals: ReviewTotals?,
    modifier: Modifier = Modifier,
) {
    ProfileCard(title = "Accuracy", modifier = modifier) {
        ProfileAccuracyRow(
            label = "Overall",
            correct = totals?.let { it.meaningCorrect + it.readingCorrect },
            incorrect = totals?.let { it.meaningIncorrect + it.readingIncorrect },
        )
        ProfileAccuracyRow(
            label = "Meaning",
            correct = totals?.meaningCorrect,
            incorrect = totals?.meaningIncorrect,
        )
        ProfileAccuracyRow(
            label = "Reading",
            correct = totals?.readingCorrect,
            incorrect = totals?.readingIncorrect,
        )
    }
}

private fun accuracyPercent(correct: Int?, incorrect: Int?): Int? {
    if (correct == null || incorrect == null) {
        return null
    }
    val total = correct + incorrect
    if (total == 0) {
        return null
    }
    return correct * 100 / total
}

@Composable
private fun ProfileAccuracyRow(
    label: String,
    correct: Int?,
    incorrect: Int?,
    modifier: Modifier = Modifier,
) {
    val percent = accuracyPercent(correct, incorrect)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProfileStatRow(label = label, value = percent?.let { "$it%" })
        LinearProgressIndicator(
            progress = { (percent ?: 0) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileStatRow(
    label: String,
    value: Int?,
    modifier: Modifier = Modifier,
) {
    ProfileStatRow(label = label, value = value?.toString(), modifier = modifier)
}

@Composable
private fun ProfileStatRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileLinkRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
