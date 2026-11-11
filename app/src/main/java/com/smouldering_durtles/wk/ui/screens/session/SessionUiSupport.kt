package com.smouldering_durtles.wk.ui.screens.session

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation
import com.smouldering_durtles.wk.enums.SubjectInfoDump
import com.smouldering_durtles.wk.model.FloatingUiState
import com.smouldering_durtles.wk.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun showToast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

/**
 * Haptic feedback matching the legacy behaviour: CONFIRM/REJECT constants on
 * API 30+, with a Vibrator fallback (100 ms success, 300 ms failure).
 */
internal class SessionHaptics(private val view: View, private val context: Context) {
    fun success() {
        if (!GlobalSettings.Review.enable_haptic_feedback_success()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            vibrate(100)
        }
    }

    fun failure() {
        if (!GlobalSettings.Review.enable_haptic_feedback_failure()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            vibrate(300)
        }
    }

    /**
     * Shorter feedback for the shake-and-retry case, matching the legacy
     * HapticUtil.onRetry behaviour.
     */
    fun retry() {
        if (!GlobalSettings.Review.enable_haptic_feedback_failure()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            vibrate(200)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(durationMs)
        }
    }
}

@Composable
internal fun rememberSessionHaptics(): SessionHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember { SessionHaptics(view, context) }
}

/**
 * Swipe gestures matching the legacy SwipingScrollView: a threshold of 100 dp
 * with horizontal dominance. onSwipeRight fires when the finger moves right,
 * onSwipeLeft when it moves left.
 */
internal fun Modifier.sessionSwipes(
    onSwipeRight: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
): Modifier = composed {
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val currentRight by rememberUpdatedState(onSwipeRight)
    val currentLeft by rememberUpdatedState(onSwipeLeft)
    pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = {
                val threshold = 100.dp.toPx()
                if (totalDrag > threshold) {
                    currentRight?.invoke()
                } else if (totalDrag < -threshold) {
                    currentLeft?.invoke()
                }
            },
        ) { _, dragAmount ->
            totalDrag += dragAmount
        }
    }
}

/**
 * The progress text and bar shown at the top of every session screen.
 */
@Composable
internal fun SessionProgressHeader(
    progressText: String,
    subjectColor: Color,
    modifier: Modifier = Modifier,
) {
    val session = remember { Session.getInstance() }
    val items = session.getItems()
    val total = items.size
    val started = items.count { it.isStarted }
    val fraction = if (total > 0) started.toFloat() / total else 0f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = subjectColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

/**
 * The current item's SRS stage chip, per the legacy srsIndicator view.
 */
@Composable
internal fun SrsIndicatorChip(
    subject: Subject,
    modifier: Modifier = Modifier,
) {
    if (!GlobalSettings.Review.getEnableSrsIndicator()) {
        return
    }
    val stage = subject.getSrsStage()
    if (stage.isInitial()) {
        return
    }
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier,
    ) {
        Text(
            text = stage.getName(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * Star rating display with tap-to-rate, per the legacy StarRatingView:
 * tapping the current rating clears it.
 */
@Composable
internal fun StarRatingRow(
    subject: Subject,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var numStars by remember(subject.getId()) { mutableIntStateOf(subject.getNumStars()) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        val newNumStars = if (numStars == i) 0 else i
                        numStars = newNumStars
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                WkApplication.getDatabase().subjectDao().updateStars(subject.getId(), newNumStars)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (numStars >= i) "★" else "☆",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The special buttons 1-3. Visibility and labels follow the legacy
 * SpecialButtonBehavior configuration; onActionPerformed lets the caller
 * refresh the session state after the action.
 */
@Composable
internal fun SpecialButtonsRow(
    onActionPerformed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val behaviors = listOf(
        GlobalSettings.AdvancedOther.getSpecialButton1Behavior(),
        GlobalSettings.AdvancedOther.getSpecialButton2Behavior(),
        GlobalSettings.AdvancedOther.getSpecialButton3Behavior(),
    ).filter { it.canShow() && it.getLabel().isNotEmpty() }
    if (behaviors.isEmpty()) {
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        behaviors.forEach { behavior ->
            OutlinedButton(
                onClick = {
                    behavior.perform()
                    onActionPerformed()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(behavior.getLabel())
            }
        }
    }
}

/**
 * Toast-style overlay message that fades in, lingers briefly and fades out.
 */
@Composable
internal fun AutoDismissMessage(
    text: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(text) {
        alpha.animateTo(1f, tween(durationMillis = 300))
        delay(2000)
        alpha.animateTo(0f, tween(durationMillis = 300))
        currentOnFinished()
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/**
 * The SRS stage change toast ("↑ Guru" style), matching the legacy
 * SessionActivity.showSrsStageChangeToast. Returns null when there is no
 * toast to show.
 */
internal data class SrsStageChange(val stageName: String, val goingUp: Boolean)

internal fun captureSrsStageChange(): SrsStageChange? {
    if (!FloatingUiState.showSrsStageChangedToast) {
        return null
    }
    FloatingUiState.showSrsStageChangedToast = false
    val session = Session.getInstance()
    if (FloatingUiState.toastOldSrsStage == FloatingUiState.toastNewSrsStage
        || !session.getType().isSrsRelevant()
        || !GlobalSettings.Review.getEnableSrsToast()
    ) {
        return null
    }
    return SrsStageChange(
        stageName = FloatingUiState.toastNewSrsStage.getName(),
        goingUp = FloatingUiState.toastNewSrsStage.compareTo(FloatingUiState.toastOldSrsStage) > 0,
    )
}

/**
 * Which context the subject info dump is shown in, mirroring the legacy
 * SubjectInfoView.ContainerType cases that the session screens use.
 */
internal enum class SubjectInfoContainer {
    LESSON_PRESENTATION,
    ANSWERED_QUESTION,
}

private const val MAX_CONTEXT_SENTENCES = 10

/**
 * The subject info dump with progressive reveal stages, ported from
 * SubjectInfoView + SubjectInfoDump. The reveal stage is kept in
 * FloatingUiState.showDumpStage like the legacy code does.
 */
@Composable
internal fun SubjectInfoCard(
    subject: Subject,
    container: SubjectInfoContainer,
    modifier: Modifier = Modifier,
) {
    val session = remember { Session.getInstance() }
    val startingDump = remember(subject.getId(), container) {
        when (container) {
            SubjectInfoContainer.LESSON_PRESENTATION ->
                if (GlobalSettings.SubjectInfo.getHideLessonReadings()) {
                    SubjectInfoDump.HIDE_READING_RELATED
                } else {
                    SubjectInfoDump.ALL
                }
            SubjectInfoContainer.ANSWERED_QUESTION -> {
                val question = session.getCurrentQuestion()
                if (question == null) {
                    SubjectInfoDump.ALL
                } else {
                    GlobalSettings.SubjectInfo.getInfoDump(question.getType(), session.isCorrect())
                }
            }
        }
    }
    var dumpStage by remember(subject.getId(), container) {
        val stage = FloatingUiState.showDumpStage ?: startingDump
        FloatingUiState.showDumpStage = stage
        mutableStateOf(stage)
    }
    val question = session.getCurrentQuestion()
    val showMeaningAnswers = dumpStage.getShowMeaningAnswers(question)
    val showReadingAnswers = dumpStage.getShowReadingAnswers(question)
    val showMeaningRelated = dumpStage.getShowMeaningRelated(question)
    val showReadingRelated = dumpStage.getShowReadingRelated(question)
    val showMeaningMnemonic = !GlobalSettings.SubjectInfo.getHideMeaningMnemonic(subject.hasMeaningNote())
    val showReadingMnemonic = !GlobalSettings.SubjectInfo.getHideReadingMnemonic(subject.hasReadingNote())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (dumpStage.getShowRevealButton()) {
                OutlinedButton(
                    onClick = {
                        dumpStage = dumpStage.getNextStage()
                        FloatingUiState.showDumpStage = dumpStage
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(dumpStage.getRevealButtonLabel())
                }
            }

            if (showMeaningAnswers) {
                SubjectInfoText("Meaning", subject.getMeaningRichText("").toString())
                if (subject.hasMeaningSynonyms()) {
                    SubjectInfoText("Synonyms", subject.getMeaningSynonymsRichText().toString())
                }
            }
            if (showReadingAnswers) {
                if (subject.getType().isKanji()) {
                    SubjectInfoText("On'yomi", subject.getOnYomiRichText().toString())
                    SubjectInfoText("Kun'yomi", subject.getKunYomiRichText().toString())
                    SubjectInfoText("Nanori", subject.getNanoriRichText().toString())
                } else {
                    SubjectInfoText("Reading", subject.getRegularReadingRichText("").toString())
                }
            }
            if (showMeaningRelated) {
                if (subject.hasMeaningMnemonic() && showMeaningMnemonic) {
                    SubjectInfoText("Meaning mnemonic", subject.getMeaningMnemonicRichText().toString())
                }
                if (subject.hasMeaningHint() && showMeaningMnemonic) {
                    SubjectInfoText("Meaning hint", subject.getMeaningHintRichText().toString())
                }
                if (subject.hasPartsOfSpeech()) {
                    SubjectInfoText("Parts of speech", subject.getPartsOfSpeechRichText().toString())
                }
                if (subject.hasContextSentences()) {
                    ContextSentencesSection(subject)
                }
            }
            if (showReadingRelated) {
                if (subject.hasReadingMnemonic() && showReadingMnemonic) {
                    SubjectInfoText("Reading mnemonic", subject.getReadingMnemonicRichText().toString())
                }
                if (subject.hasReadingHint() && showReadingMnemonic) {
                    SubjectInfoText("Reading hint", subject.getReadingHintRichText().toString())
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            InfoRow("SRS Stage:", subject.getSrsStage().getName())
            if (subject.getFrequency() > 0) {
                InfoRow("Frequency:", subject.getFrequency().toString())
            }
            if (subject.getJlptLevel() > 0) {
                InfoRow("JLPT level:", subject.getJlptLevelAsString())
            }
            InfoRow("Unlocked:", subject.getFormattedUnlockedAt() ?: "Unknown")
            InfoRow("Started:", subject.getFormattedStartedAt() ?: "Unknown")
            InfoRow("Passed:", subject.getFormattedPassedAt() ?: "Unknown")
        }
    }
}

/**
 * Context sentences with tap-to-reveal translations, per the legacy
 * showContextSentence + hide_sentence_translations setting.
 */
@Composable
private fun ContextSentencesSection(subject: Subject, modifier: Modifier = Modifier) {
    val hideTranslations = GlobalSettings.SubjectInfo.getHideSentenceTranslations()
    val sentences = subject.getContextSentences().take(MAX_CONTEXT_SENTENCES)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Context sentences",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        sentences.forEach { sentence ->
            var translationShown by remember { mutableStateOf(!hideTranslations) }
            Column(
                modifier = Modifier.clickable { translationShown = !translationShown },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = sentence.getJapanese().orEmpty(), style = MaterialTheme.typography.bodyMedium)
                if (translationShown) {
                    Text(
                        text = sentence.getEnglish().orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SubjectInfoText(title: String, value: String, modifier: Modifier = Modifier) {
    if (value.isBlank()) {
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Wraps question content and slides it in when the trigger changes, per the
 * legacy FragmentTransitionAnimation directions and the slide_animations
 * setting. No slide on the first composition or for NONE.
 */
@Composable
internal fun SlideTransitionBox(
    trigger: Any?,
    direction: FragmentTransitionAnimation,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val fraction = remember { Animatable(0f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (firstComposition || !GlobalSettings.Display.getSlideAnimations()
            || direction == FragmentTransitionAnimation.NONE
        ) {
            firstComposition = false
            fraction.snapTo(0f)
            return@LaunchedEffect
        }
        fraction.snapTo(if (direction == FragmentTransitionAnimation.RTL) 1f else -1f)
        fraction.animateTo(0f, tween(durationMillis = 200))
    }
    Box(
        modifier = modifier.graphicsLayer { translationX = fraction.value * size.width },
        content = content,
    )
}
