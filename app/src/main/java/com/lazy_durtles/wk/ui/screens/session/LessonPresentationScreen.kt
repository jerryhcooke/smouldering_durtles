package com.lazy_durtles.wk.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.model.FloatingUiState
import com.lazy_durtles.wk.model.Session
import com.lazy_durtles.wk.util.AudioUtil

/**
 * Lesson presentation phase, ported from LessonSessionFragment: the full
 * subject info with progressive reveal, Previous / Next / "Start quiz"
 * buttons and swipe navigation. Session state is driven only through the
 * Session API (moveToNextLessonItem / moveToPreviousLessonItem / startQuiz).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonPresentationScreen(
    subject: Subject?,
    onFinishSession: () -> Unit,
    onBackPressed: () -> Unit = { },
    onSessionTick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit = { _, _ -> },
) {
    val session = remember { Session.getInstance() }
    val item = session.getCurrentItem()

    // Autoplay audio once per presented item, per the legacy playAudio().
    LaunchedEffect(item?.getId()) {
        if (FloatingUiState.audioPlayed) {
            return@LaunchedEffect
        }
        if (GlobalSettings.Audio.getAutoplayLessonPresentation() && subject != null) {
            FloatingUiState.audioPlayed = true
            AudioUtil.playAudio(subject, null)
        }
    }

    fun goToPrevious() {
        if (!session.isOnFirstLessonItem()) {
            session.moveToPreviousLessonItem()
            onSessionTick()
        }
    }

    fun goToNext() {
        if (session.isOnLastLessonItem()) {
            session.startQuiz()
        } else {
            session.moveToNextLessonItem()
        }
        onSessionTick()
    }

    SessionScaffold(
        title = subject?.getCharacters().orEmpty(),
        backgroundColor = subject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
        onFinishSession = onFinishSession,
        onSessionTick = onSessionTick,
        onViewSubject = onViewSubject,
        modifier = modifier,
    ) { paddingValues ->
        if (subject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@SessionScaffold
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .sessionSwipes(
                    onSwipeRight = { goToPrevious() },
                    onSwipeLeft = { goToNext() },
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SessionProgressHeader(
                progressText = session.getProgressText(),
                subjectColor = Color(subject.getBackgroundColor()),
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Text(
                        text = "${subject.getCharacters().orEmpty()} · ${subject.getType().getInfoTitleLabel()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(subject.getBackgroundColor())),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subject.getCharacters() ?: subject.getSlug() ?: "",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color(subject.getTextColor()),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            SubjectInfoCard(
                subject = subject,
                container = SubjectInfoContainer.LESSON_PRESENTATION,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!session.isOnFirstLessonItem()) {
                    OutlinedButton(
                        onClick = { goToPrevious() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Previous")
                    }
                }
                Button(
                    onClick = { goToNext() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (session.isOnLastLessonItem()) "Start quiz" else "Next")
                }
            }
        }
    }
}
