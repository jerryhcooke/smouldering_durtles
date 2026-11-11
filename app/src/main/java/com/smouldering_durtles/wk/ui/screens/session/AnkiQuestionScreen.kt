package com.smouldering_durtles.wk.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.model.FloatingUiState
import com.smouldering_durtles.wk.model.Question
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.util.AudioUtil

/**
 * Anki mode question, ported from AnkiSessionFragment: Show answer / Correct /
 * Incorrect / Next buttons with theme-configurable colors and swappable
 * correct/incorrect order, the answer revealed with synonyms while revealing,
 * and autoplay on reveal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnkiQuestionScreen(
    question: Question,
    subject: Subject?,
    onFinishSession: () -> Unit,
    onSessionTick: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit = { _, _ -> },
) {
    val session = remember { Session.getInstance() }
    var showingAnswer by remember(question) { mutableStateOf(false) }
    val answered = session.isAnswered()
    val ankiColors = remember { ActiveTheme.getAnkiColors() }
    fun ankiColor(index: Int) = Color(ankiColors[index])
    val textColor = ankiColor(4)
    val answerBackground = ankiColor(5)

    // Autoplay on reveal, per the legacy AnkiSessionFragment.playAudio().
    LaunchedEffect(showingAnswer, answered, question) {
        if (showingAnswer && !answered && !FloatingUiState.audioPlayed
            && question.getType().isReading() && GlobalSettings.Audio.getAutoplayAnkiReveal() && subject != null
        ) {
            FloatingUiState.audioPlayed = true
            AudioUtil.playAudio(subject, null)
        }
    }

    // An undo (special button) re-opens the same question unanswered.
    LaunchedEffect(answered) {
        if (!answered) {
            showingAnswer = false
        }
    }

    fun showAnswer() {
        showingAnswer = true
    }

    fun markCorrect() {
        showingAnswer = false
        if (GlobalSettings.getTestMode() && !question.getItem().hasIncorrectAnswers()) {
            session.submitAnkiIncorrect()
        } else {
            session.submitAnkiCorrect()
        }
        onSessionTick()
    }

    fun markIncorrect() {
        showingAnswer = false
        session.submitAnkiIncorrect()
        onSessionTick()
    }

    fun nextQuestion() {
        showingAnswer = false
        session.advance()
        session.chooseQuestion()
        onSessionTick()
    }

    SessionScaffold(
        title = question.getTitle(),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SessionProgressHeader(
                progressText = session.getProgressText(),
                subjectColor = Color(subject.getBackgroundColor()),
            )

            SrsIndicatorChip(subject = subject)
            StarRatingRow(subject = subject)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (GlobalSettings.Display.getQuizQuestionViewHeight() > 0) {
                            Modifier.height(GlobalSettings.Display.getQuizQuestionViewHeight().dp)
                        } else {
                            Modifier
                        },
                    ),
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

            if (showingAnswer || answered) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = answerBackground),
                ) {
                    Text(
                        text = question.getAnkiAnswerRichText(subject, session.getType()).toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = GlobalSettings.Font.getFontSizeAnkiAnswer().sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    )
                }
            }

            if (!answered && showingAnswer && question.getType().isMeaning() && subject.hasMeaningSynonyms()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = answerBackground),
                ) {
                    Text(
                        text = subject.getMeaningSynonymsRichText().toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = GlobalSettings.Font.getFontSizeAnkiAnswer().sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    )
                }
            }

            if (answered) {
                SubjectInfoCard(
                    subject = subject,
                    container = SubjectInfoContainer.ANSWERED_QUESTION,
                )
            }

            SpecialButtonsRow(onActionPerformed = onSessionTick)

            val buttonHeight = GlobalSettings.Display.getAnkiButtonsHeight().dp
            if (!answered && !showingAnswer) {
                Button(
                    onClick = { showAnswer() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ankiColor(0),
                        contentColor = textColor,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                ) {
                    Text("Show answer")
                }
            } else if (!answered && showingAnswer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val buttons: List<Pair<String, () -> Unit>> = if (GlobalSettings.Display.getSwapAnkiButtons()) {
                        listOf("Correct" to { markCorrect() }, "Incorrect" to { markIncorrect() })
                    } else {
                        listOf("Incorrect" to { markIncorrect() }, "Correct" to { markCorrect() })
                    }
                    buttons.forEach { (label, action) ->
                        Button(
                            onClick = action,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (label == "Correct") ankiColor(2) else ankiColor(3),
                                contentColor = textColor,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(buttonHeight),
                        ) {
                            Text(label)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { nextQuestion() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ankiColor(1),
                        contentColor = textColor,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                ) {
                    Text("Next")
                }
            }
        }
    }
}
