package com.lazy_durtles.wk.ui

import android.text.SpannableStringBuilder
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.Constants
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.R
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.enums.CloseEnoughAction
import com.lazy_durtles.wk.enums.SessionType
import com.lazy_durtles.wk.model.FloatingUiState
import com.lazy_durtles.wk.model.Question
import com.lazy_durtles.wk.model.Session
import com.lazy_durtles.wk.ui.common.BulkOpDialog
import com.lazy_durtles.wk.ui.common.BulkOpState
import com.lazy_durtles.wk.ui.common.BulkOpType
import com.lazy_durtles.wk.ui.common.runBulkOp
import com.lazy_durtles.wk.ui.screens.session.AnkiQuestionScreen
import com.lazy_durtles.wk.ui.screens.session.AutoDismissMessage
import com.lazy_durtles.wk.ui.screens.session.LessonPresentationScreen
import com.lazy_durtles.wk.ui.screens.session.SrsIndicatorChip
import com.lazy_durtles.wk.ui.screens.session.SrsStageChange
import com.lazy_durtles.wk.ui.screens.session.SessionProgressHeader
import com.lazy_durtles.wk.ui.screens.session.SessionScaffold
import com.lazy_durtles.wk.ui.screens.session.SessionSummaryScreen
import com.lazy_durtles.wk.ui.screens.session.StarRatingRow
import com.lazy_durtles.wk.ui.screens.session.SpecialButtonsRow
import com.lazy_durtles.wk.ui.screens.session.SlideTransitionBox
import com.lazy_durtles.wk.ui.screens.session.SubjectInfoCard
import com.lazy_durtles.wk.ui.screens.session.SubjectInfoContainer
import com.lazy_durtles.wk.ui.screens.session.captureSrsStageChange
import com.lazy_durtles.wk.ui.screens.session.rememberSessionHaptics
import com.lazy_durtles.wk.ui.screens.settings.HtmlNoticeText
import com.lazy_durtles.wk.ui.screens.session.sessionSwipes
import com.lazy_durtles.wk.util.AudioUtil
import com.lazy_durtles.wk.util.PseudoIme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val CorrectColor = Color(0xFF43A047)

/**
 * The session entry point. Dispatches to the lesson presentation, Anki mode,
 * the quiz (unanswered/answered) or the session summary based on the state of
 * the Session singleton, mirroring how the legacy SessionActivity picks
 * fragments via Session.getNewFragment().
 */
@Composable
fun SessionScreen(
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier,
    onViewSubject: (subjectId: Long, subjectIds: List<Long>) -> Unit = { _, _ -> },
    onBackPressed: () -> Unit = { },
) {
    val session = remember { Session.getInstance() }
    val haptics = rememberSessionHaptics()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var uiTick by remember { mutableIntStateOf(0) }
    var currentSubject by remember { mutableStateOf<Subject?>(null) }
    var matchingKanji by remember { mutableStateOf<Subject?>(null) }
    var canReplayAudio by remember { mutableStateOf(false) }
    var answerField by remember { mutableStateOf(TextFieldValue("")) }
    var closeMessage by remember { mutableStateOf<String?>(null) }
    var alternativesMessage by remember { mutableStateOf<String?>(null) }
    var digraphMessage by remember { mutableStateOf<String?>(null) }
    var showDigraphHelp by remember { mutableStateOf(false) }
    var lightningToastMessage by remember { mutableStateOf<String?>(null) }
    var srsToast by remember { mutableStateOf<SrsStageChange?>(null) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    var failTrigger by remember { mutableIntStateOf(0) }
    var nextFrozen by remember { mutableStateOf(false) }
    var resurrectIds by remember { mutableStateOf<LongArray?>(null) }
    val bulkOpState = remember { BulkOpState() }
    val shakeOffset = remember { Animatable(0f) }

    val question = session.getCurrentQuestion()
    val item = session.getCurrentItem()
    val answered = session.isAnswered()
    val correct = session.isCorrect()
    val ankiMode = question != null && GlobalSettings.getAnkiMode(session.getType(), question.getType())

    LaunchedEffect(Unit) {
        session.chooseQuestion()
        uiTick++
    }

    // Central tick handler used by all sub-screens: like the legacy
    // SessionActivity.updateFragment(), it picks a new question when the
    // session is active but has none (e.g. right after startQuiz()).
    fun handleTick() {
        if (session.getCurrentQuestion() == null && session.isActive()) {
            session.chooseQuestion()
        }
        uiTick++
    }

    LaunchedEffect(uiTick) {
        if (session.isInactive()) {
            onFinishSession()
        }
    }

    // Load the subject (and the matching kanji for single-kanji vocab) off the main thread.
    LaunchedEffect(item?.getId(), uiTick) {
        val itemId = item?.getId() ?: return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            val db = WkApplication.getDatabase()
            val subject = db.subjectCollectionsDao().getSessionSubjects()[itemId]
            val characters = subject?.getCharacters().orEmpty()
            val matching = if (subject != null && subject.getType().isVocabulary() && characters.length == 1
                && GlobalSettings.AdvancedOther.getShakeOnMatchingKanji()
            ) {
                db.subjectDao().getKanjiByCharacters(characters)
            } else {
                null
            }
            Triple(subject, matching, subject != null && AudioUtil.hasAudio(subject))
        }
        item.setSubject(loaded.first)
        currentSubject = loaded.first
        matchingKanji = loaded.second
        canReplayAudio = loaded.third
    }

    // Freeze the Next button after an incorrect answer, per getNextButtonDelay().
    LaunchedEffect(answered, correct, uiTick) {
        if (answered && !correct && session.isNextButtonFrozen()) {
            nextFrozen = true
            while (session.isNextButtonFrozen()) {
                delay(200)
            }
            nextFrozen = false
        } else {
            nextFrozen = false
        }
    }

    LaunchedEffect(uiTick) {
        captureSrsStageChange()?.let { srsToast = it }
    }

    fun shakeInput() {
        coroutineScope.launch {
            repeat(3) {
                shakeOffset.animateTo(12f, tween(durationMillis = 50))
                shakeOffset.animateTo(-12f, tween(durationMillis = 50))
            }
            shakeOffset.animateTo(0f, tween(durationMillis = 50))
        }
    }

    fun onAnswerChanged(value: TextFieldValue) {
        if (value.text.length > 50) {
            return
        }
        val kana = question?.getType()?.isKana() == true
        answerField = if (kana && value.composition == null && value.text != answerField.text) {
            val editable = SpannableStringBuilder(value.text)
            PseudoIme.fixup(editable, 0, editable.length)
            val converted = editable.toString()
            TextFieldValue(converted, TextRange(converted.length))
        } else {
            value
        }
    }

    fun clearMessages() {
        closeMessage = null
        alternativesMessage = null
        digraphMessage = null
    }

    fun playAudioForCorrectAnswer() {
        val subject = currentSubject
        if (subject != null && question?.getType()?.isReading() == true
            && !FloatingUiState.audioPlayed && GlobalSettings.getAutoPlay(session.getType())
        ) {
            FloatingUiState.audioPlayed = true
            AudioUtil.playAudio(subject, FloatingUiState.getLastMatchedAnswer())
        }
    }

    fun advanceToNextQuestion() {
        session.advance()
        session.chooseQuestion()
        answerField = TextFieldValue("")
        clearMessages()
        FloatingUiState.audioPlayed = false
        uiTick++
    }

    fun submitAnswer() {
        if (answered || ankiMode || question == null) {
            return
        }
        FloatingUiState.setCurrentAnswer(answerField.text)
        val verdict = try {
            session.submit(matchingKanji)
        } catch (e: Exception) {
            Log.e("SessionScreen", "Error checking answer", e)
            return
        }
        if (verdict.isRetry()) {
            shakeInput()
            haptics.retry()
            return
        }

        val lastVerdict = FloatingUiState.lastVerdict
        closeMessage = if (FloatingUiState.showCloseToast && lastVerdict != null
            && GlobalSettings.Review.getCloseEnoughAction().isShowToast()
        ) {
            "Your answer ('${lastVerdict.getGivenAnswer()}') was a bit off, the closest correct answer was " +
                "'${lastVerdict.getMatchedAnswer()}'. Check the meaning to make sure you are correct."
        } else {
            null
        }
        alternativesMessage = if (FloatingUiState.showAlternativesToast && GlobalSettings.Review.getEnableAlternativesToast()) {
            FloatingUiState.alternativesForLastCorrectAnswer?.let { "Alt: $it" }
        } else {
            null
        }
        digraphMessage = lastVerdict?.getDigraphMatch()?.let { match ->
            String.format(
                "Your answer was incorrect because you mixed up the regular kana %c and the small kana %c. " +
                    "Tap this message for more information about the difference between small and regular kana.",
                match.getRegularKana(),
                match.getSmallKana(),
            )
        }

        if (verdict.isOk()) {
            haptics.success()
            if (GlobalSettings.Review.getShowAnswerToast()) {
                burstTrigger++
            }
            if (session.isLightningModeActive()
                && lastVerdict != null && lastVerdict.isNearMatch()
                && GlobalSettings.Review.getCloseEnoughAction() == CloseEnoughAction.ACCEPT_WITH_TOAST_NO_LM
            ) {
                FloatingUiState.lingerOnAnswer = true
            }
            playAudioForCorrectAnswer()
            if (session.isLightningModeActive() && !FloatingUiState.lingerOnAnswer) {
                val toastParts = listOfNotNull(closeMessage, alternativesMessage).filter { it.isNotBlank() }
                if (toastParts.isNotEmpty()) {
                    lightningToastMessage = toastParts.joinToString("\n")
                }
                advanceToNextQuestion()
                return
            }
        } else {
            haptics.failure()
            if (GlobalSettings.Review.getShowAnswerToast()) {
                failTrigger++
            }
        }
        uiTick++
    }

    fun dontKnow() {
        if (answered || ankiMode || question == null) {
            return
        }
        session.submitDontKnow()
        answerField = TextFieldValue("")
        clearMessages()
        uiTick++
    }

    LaunchedEffect(question, currentSubject, answered) {
        if (!answered && currentSubject != null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val subjectColor = currentSubject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize()) {
        when {
            session.isInactive() -> Unit
            question == null && !session.isActive() -> {
                SessionSummaryScreen(
                    onFinishSession = onFinishSession,
                    onViewSubject = onViewSubject,
                    onResurrectSubjects = { ids ->
                        resurrectIds = ids
                        bulkOpState.reset(ids.size)
                    },
                    onSessionTick = { handleTick() },
                )
            }
            session.getType() == SessionType.LESSON && question != null && !session.isActive() -> {
                LessonPresentationScreen(
                    subject = currentSubject,
                    onFinishSession = onFinishSession,
                    onSessionTick = { handleTick() },
                    onViewSubject = onViewSubject,
                )
            }
            ankiMode -> {
                AnkiQuestionScreen(
                    question = question,
                    subject = currentSubject,
                    onFinishSession = onFinishSession,
                    onSessionTick = { handleTick() },
                    onViewSubject = onViewSubject,
                )
            }
            question != null && answered -> {
                SessionScaffold(
                    title = question.getTitle(),
                    backgroundColor = subjectColor,
                    onFinishSession = onFinishSession,
                    onSessionTick = { handleTick() },
                    onViewSubject = onViewSubject,
                    bottomBar = {
                        Surface(
                            tonalElevation = 3.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.navigationBarsPadding(),
                        ) {
                            Button(
                                onClick = { if (!nextFrozen) advanceToNextQuestion() },
                                enabled = !nextFrozen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Text("Next")
                            }
                        }
                    },
                ) { paddingValues ->
                    SlideTransitionBox(
                        trigger = question,
                        direction = session.getQuestionChoiceReason().getAnimation(),
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                    ) {
                        AnsweredQuestionContent(
                            session = session,
                            question = question,
                            subject = currentSubject,
                            isCorrect = correct,
                            closeMessage = closeMessage,
                            alternativesMessage = alternativesMessage,
                            digraphMessage = digraphMessage,
                            canReplayAudio = canReplayAudio,
                            onNext = { if (!nextFrozen) advanceToNextQuestion() },
                            onSpecialAction = { handleTick() },
                            onShowDigraphHelp = { showDigraphHelp = true },
                        )
                    }
                }
            }
            question != null -> {
                SessionScaffold(
                    title = question.getTitle(),
                    backgroundColor = subjectColor,
                    onFinishSession = onFinishSession,
                    onSessionTick = { handleTick() },
                    onViewSubject = onViewSubject,
                ) { paddingValues ->
                    SlideTransitionBox(
                        trigger = question,
                        direction = session.getQuestionChoiceReason().getAnimation(),
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                    ) {
                        UnansweredQuestionContent(
                            session = session,
                            question = question,
                            subject = currentSubject,
                            answerField = answerField,
                            onAnswerChanged = { onAnswerChanged(it) },
                            onSubmit = { submitAnswer() },
                            onDontKnow = { dontKnow() },
                            onSpecialAction = { handleTick() },
                            focusRequester = focusRequester,
                            shakeOffset = shakeOffset,
                        )
                    }
                }
            }
        }

        CorrectOverlay(trigger = burstTrigger, modifier = Modifier.fillMaxSize())
        FailOverlay(trigger = failTrigger, modifier = Modifier.fillMaxSize())
        lightningToastMessage?.let { message ->
            AutoDismissMessage(
                text = message,
                modifier = Modifier.fillMaxSize(),
                onFinished = { lightningToastMessage = null },
            )
        }
        srsToast?.let { toast ->
            AutoDismissMessage(
                text = "${if (toast.goingUp) "↑" else "↓"} ${toast.stageName}",
                modifier = Modifier.fillMaxSize(),
                onFinished = { srsToast = null },
            )
        }
    }

    if (showDigraphHelp) {
        DigraphHelpDialog(onDismiss = { showDigraphHelp = false })
    }

    if (resurrectIds != null && bulkOpState.visible) {
        BulkOpDialog(
            type = BulkOpType.RESURRECT,
            state = bulkOpState,
            onStart = {
                val ids = requireNotNull(resurrectIds).toList()
                resurrectIds = null
                coroutineScope.launch {
                    bulkOpState.running = true
                    runBulkOp(BulkOpType.RESURRECT, ids, bulkOpState)
                }
            },
            onDismiss = { resurrectIds = null },
        )
    }
}

/**
 * The digraph help dialog, a port of the legacy DigraphHelpActivity, shown
 * when the digraph hint on an answered question is tapped.
 */
@Composable
private fun DigraphHelpDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digraph information") },
        text = {
            Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HtmlNoticeText(html = Constants.DIGRAPH_HELP_DOCUMENT_1)
                HtmlNoticeText(html = Constants.DIGRAPH_HELP_DOCUMENT_2)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

/**
 * The unanswered quiz screen, ported from UnansweredSessionFragment.
 */
@Composable
private fun UnansweredQuestionContent(
    session: Session,
    question: Question,
    subject: Subject?,
    answerField: TextFieldValue,
    onAnswerChanged: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onDontKnow: () -> Unit,
    onSpecialAction: () -> Unit,
    focusRequester: FocusRequester,
    shakeOffset: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val stretch = GlobalSettings.Display.getStretchQuestionView()
    val heightDp = GlobalSettings.Display.getQuizQuestionViewHeight()

    Column(
        modifier = modifier
            .imePadding()
            .then(if (stretch) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SessionProgressHeader(
            progressText = session.getProgressText(),
            subjectColor = subject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
        )

        if (subject != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SrsIndicatorChip(subject = subject)
                if (GlobalSettings.Other.getEnableStarRatings()) {
                    StarRatingRow(subject = subject)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = Color(question.getType().getHintColor()),
                ),
            ) {
                Text(
                    text = question.getTitle(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(question.getType().getHintContrastColor()),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        stretch -> Modifier.weight(1f, fill = true)
                        heightDp > 0 -> Modifier.height(heightDp.dp)
                        else -> Modifier
                    },
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = subject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject?.getCharacters() ?: subject?.getSlug() ?: "",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = GlobalSettings.Font.getMaxFontSizeQuizText().coerceAtMost(100).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = subject?.let { Color(it.getTextColor()) } ?: MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val type = question.getType()
        val forceAscii = (type.isAscii() && GlobalSettings.Keyboard.getForceAsciiMeaning()) ||
            (type.isKana() && GlobalSettings.Keyboard.getForceAsciiReading())
        val autoCorrect = (type.isAscii() && GlobalSettings.Keyboard.getEnableAutoCorrectMeaning()) ||
            (type.isKana() && GlobalSettings.Keyboard.getEnableAutoCorrectReading())
        val centerCaret = GlobalSettings.Display.getCenterCaret()

        OutlinedTextField(
            value = answerField,
            onValueChange = onAnswerChanged,
            label = { Text("Your Answer") },
            placeholder = { Text(question.getHint(false)) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = GlobalSettings.Font.getFontSizeQuestionEdit().sp,
                textAlign = if (centerCaret) TextAlign.Center else TextAlign.Start,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (forceAscii) KeyboardType.Ascii else KeyboardType.Text,
                autoCorrect = autoCorrect,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .focusRequester(focusRequester),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (GlobalSettings.AdvancedOther.getDontKnowButtonBehavior().canShow()) {
                OutlinedButton(
                    onClick = onDontKnow,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Don't know")
                }
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
            ) {
                Text("Submit")
            }
        }

        SpecialButtonsRow(onActionPerformed = onSpecialAction)
    }
}

/**
 * The answered quiz screen, ported from AnsweredSessionFragment.
 */
@Composable
private fun AnsweredQuestionContent(
    session: Session,
    question: Question,
    subject: Subject?,
    isCorrect: Boolean,
    closeMessage: String?,
    alternativesMessage: String?,
    digraphMessage: String?,
    canReplayAudio: Boolean,
    onNext: () -> Unit,
    onSpecialAction: () -> Unit,
    onShowDigraphHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heightDp = GlobalSettings.Display.getQuizQuestionViewHeight()
    val isReadingQuestion = question.getType().isReading()

    Column(
        modifier = modifier
            .fillMaxSize()
            .sessionSwipes(onSwipeRight = onNext)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SessionProgressHeader(
            progressText = session.getProgressText(),
            subjectColor = subject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
        )

        if (subject != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SrsIndicatorChip(subject = subject)
                if (GlobalSettings.Other.getEnableStarRatings()) {
                    StarRatingRow(subject = subject)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = Color(question.getType().getHintColor()),
                ),
            ) {
                Text(
                    text = question.getTitle(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(question.getType().getHintContrastColor()),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (heightDp > 0) Modifier.height(heightDp.dp) else Modifier),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = subject?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject?.getCharacters() ?: subject?.getSlug() ?: "",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = GlobalSettings.Font.getMaxFontSizeQuizText().coerceAtMost(100).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = subject?.let { Color(it.getTextColor()) } ?: MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCorrect) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isCorrect) "Correct" else "Incorrect",
                        tint = if (isCorrect) CorrectColor else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = if (isCorrect) "Correct!" else "Incorrect",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isCorrect) CorrectColor else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (FloatingUiState.getCurrentAnswer().isNotEmpty()) {
                    Text(
                        text = FloatingUiState.getCurrentAnswer(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                closeMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                alternativesMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                digraphMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onShowDigraphHelp),
                    )
                }
            }
        }

        if (subject != null) {
            SubjectInfoCard(
                subject = subject,
                container = SubjectInfoContainer.ANSWERED_QUESTION,
            )
        }

        if (isReadingQuestion && canReplayAudio && subject != null) {
            OutlinedButton(
                onClick = {
                    FloatingUiState.audioPlayed = true
                    AudioUtil.playAudio(subject, FloatingUiState.getLastMatchedAnswer())
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume_up_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play audio")
            }
        }

        SpecialButtonsRow(onActionPerformed = onSpecialAction)
    }
}

private class Speck(val angle: Float, val distanceFraction: Float, val radius: Float, val color: Color)

@Composable
private fun CorrectOverlay(trigger: Int, modifier: Modifier = Modifier) {
    FeedbackOverlay(
        trigger = trigger,
        speckColors = listOf(Color(0xFF66BB6A), Color(0xFF81C784), Color(0xFFA5D6A7), Color(0xFF43A047)),
        circleColor = CorrectColor,
        iconVector = Icons.Default.Check,
        iconDescription = "Correct",
        modifier = modifier,
    )
}

@Composable
private fun FailOverlay(trigger: Int, modifier: Modifier = Modifier) {
    FeedbackOverlay(
        trigger = trigger,
        speckColors = listOf(Color(0xFFE57373), Color(0xFFEF9A9A), Color(0xFFE53935)),
        circleColor = MaterialTheme.colorScheme.error,
        iconVector = Icons.Default.Close,
        iconDescription = "Incorrect",
        modifier = modifier,
    )
}

@Composable
private fun FeedbackOverlay(
    trigger: Int,
    speckColors: List<Color>,
    circleColor: Color,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var active by remember { mutableStateOf(false) }
    val specks = remember {
        List(28) { i ->
            Speck(
                angle = (2 * PI * i / 28 + Random.nextDouble(-0.1, 0.1)).toFloat(),
                distanceFraction = 0.2f + Random.nextFloat() * 0.5f,
                radius = 2f + Random.nextFloat() * 2f,
                color = speckColors[i % speckColors.size],
            )
        }
    }

    LaunchedEffect(trigger) {
        if (trigger == 0) {
            return@LaunchedEffect
        }
        active = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 600, easing = LinearOutSlowInEasing))
        active = false
    }

    if (!active) {
        return
    }

    val p = progress.value
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxDistance = maxOf(size.width, size.height)
            specks.forEach { speck ->
                val d = speck.distanceFraction * maxDistance * p
                drawCircle(
                    color = speck.color,
                    radius = speck.radius,
                    center = Offset(center.x + cos(speck.angle) * d, center.y + sin(speck.angle) * d),
                    alpha = (1f - p).coerceIn(0f, 1f),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale((p * 4f).coerceAtMost(1f))
                .alpha(1f - ((p - 0.6f) / 0.4f).coerceIn(0f, 1f))
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = iconDescription,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}
