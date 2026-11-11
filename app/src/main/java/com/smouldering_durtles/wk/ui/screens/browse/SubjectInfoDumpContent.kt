package com.smouldering_durtles.wk.ui.screens.browse

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.SubjectInfoDump
import com.smouldering_durtles.wk.jobs.SaveStudyMaterialJob
import com.smouldering_durtles.wk.model.Question
import com.smouldering_durtles.wk.services.JobRunnerService
import com.smouldering_durtles.wk.ui.common.BulkOpDialog
import com.smouldering_durtles.wk.ui.common.BulkOpState
import com.smouldering_durtles.wk.ui.common.BulkOpType
import com.smouldering_durtles.wk.ui.common.runBulkOp
import com.smouldering_durtles.wk.util.AudioUtil
import com.smouldering_durtles.wk.util.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

/**
 * The initial subject info dump for the lesson presentation, respecting the
 * "hide readings in lessons" setting, as the legacy
 * SubjectInfoView.getSubjectInfoDump does for container type LESSON_PRESENTATION.
 */
fun initialInfoDumpForLessonPresentation(): SubjectInfoDump =
    if (GlobalSettings.SubjectInfo.getHideLessonReadings()) {
        SubjectInfoDump.HIDE_READING_RELATED
    }
    else {
        SubjectInfoDump.ALL
    }

/**
 * The initial subject info dump after an answered question, respecting the
 * four meaning_info_dump / reading_info_dump(_incorrect) settings, as the
 * legacy SubjectInfoView.getSubjectInfoDump does for container type
 * ANSWERED_QUESTION.
 */
fun initialInfoDumpForAnsweredQuestion(question: Question?, correct: Boolean): SubjectInfoDump {
    if (question == null) {
        return SubjectInfoDump.ALL
    }
    return GlobalSettings.SubjectInfo.getInfoDump(question.getType(), correct)
}

/**
 * The full subject info dump, ported from the legacy SubjectInfoView and its
 * subviews: headline with reveal stages, on/kun'yomi and nanori, star rating,
 * components, meaning section with synonyms, reading section, parts of speech,
 * stroke order diagram, context sentences with blur-reveal translations,
 * visually similar / used-in grids (order per the swap setting), details table
 * and resurrect/burn actions.
 *
 * The visibility of each section is driven by the [SubjectInfoDump] stage and
 * the current [question] (null outside sessions), like the legacy
 * layoutSubjectImpl. The reveal button advances the stage:
 * NOTHING -> ANSWERS_ONLY -> HIDE_UNQUIZZED -> ALL.
 */
@Composable
public fun SubjectInfoDumpContent(
    subject: Subject,
    relatedById: Map<Long, Subject>,
    initialDump: SubjectInfoDump,
    question: Question?,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dumpStage by remember(initialDump, subject.getId()) { mutableStateOf(initialDump) }
    var bulkOpType by remember { mutableStateOf<BulkOpType?>(null) }
    val bulkOpState = remember { BulkOpState() }
    val coroutineScope = rememberCoroutineScope()

    val showMeaningAnswers = dumpStage.getShowMeaningAnswers(question)
    val showReadingAnswers = dumpStage.getShowReadingAnswers(question)
    val showMeaningRelated = dumpStage.getShowMeaningRelated(question)
    val showReadingRelated = dumpStage.getShowReadingRelated(question)
    val showMeaningMnemonic = !GlobalSettings.SubjectInfo.getHideMeaningMnemonic(subject.hasMeaningNote())
    val showReadingMnemonic = !GlobalSettings.SubjectInfo.getHideReadingMnemonic(subject.hasReadingNote())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubjectInfoHeadline(
            subject = subject,
            dumpStage = dumpStage,
            showMeaningAnswers = showMeaningAnswers,
            showReadingAnswers = showReadingAnswers,
            onReveal = { dumpStage = dumpStage.getNextStage() },
        )

        // Kanji on'yomi, kun'yomi, nanori
        if (showReadingAnswers && subject.hasOnYomi()) {
            InfoText(subject.getOnYomiRichText().toString())
        }
        if (showReadingAnswers && subject.hasKunYomi()) {
            InfoText(subject.getKunYomiRichText().toString())
        }
        if (showReadingAnswers && subject.hasNanori()) {
            InfoText(subject.getNanoriRichText().toString())
        }

        // Star rating
        if (GlobalSettings.Other.getEnableStarRatings()) {
            StarRatingRow(subject = subject, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // Components table
        val hasComponents = showMeaningRelated && subject.hasComponents()
        if (hasComponents) {
            SectionDivider()
            SectionHeader(subject.getType().getComponentsHeaderText())
            SubjectGrid(
                subjects = subject.getComponentSubjectIds().mapNotNull { relatedById[it] },
                onSubjectClick = onSubjectClick,
                showMeaning = true,
                showReading = showReadingRelated,
            )
        }

        // Meaning section
        val hasMeaningSection = subject.hasMeaningMnemonic() &&
                (showMeaningRelated || showMeaningAnswers && subject.hasMeaningSynonyms())
        if (hasMeaningSection) {
            SectionDivider()
            if (showMeaningRelated && subject.hasMeaningMnemonic() && showMeaningMnemonic) {
                InfoText(subject.getMeaningMnemonicRichText().toString())
            }
            if (showMeaningRelated && subject.hasMeaningHint() && showMeaningMnemonic) {
                InfoText(subject.getMeaningHintRichText().toString())
            }
            if (showMeaningRelated && GlobalSettings.SubjectInfo.getShowLegacy() && subject.hasLegacy() && showMeaningMnemonic) {
                InfoText(subject.getLegacyNameRichText().toString())
                InfoText(subject.getLegacyMnemonicRichText().toString())
            }
            if (showMeaningRelated && subject.hasMeaningNote()) {
                InfoText(subject.getMeaningNoteRichText().toString())
            }
            if (showMeaningAnswers) {
                SynonymsRow(subject = subject)
            }
        }

        // Reading section
        if (showReadingRelated && subject.hasReadingMnemonic()) {
            SectionDivider()
            if (showReadingMnemonic) {
                InfoText(subject.getReadingMnemonicRichText().toString())
            }
            if (subject.hasReadingHint() && showReadingMnemonic) {
                InfoText(subject.getReadingHintRichText().toString())
            }
            if (subject.hasReadingNote()) {
                InfoText(subject.getReadingNoteRichText().toString())
            }
        }

        // Parts of speech
        if (showMeaningRelated && subject.hasPartsOfSpeech()) {
            SectionDivider()
            InfoText(subject.getPartsOfSpeechRichText().toString())
        }

        // Stroke order diagram
        val showStrokeOrder = showMeaningRelated && GlobalSettings.SubjectInfo.getShowStrokeOrder() && subject.hasStrokeData()
        if (showStrokeOrder) {
            SectionDivider()
            StrokeOrderDiagram(
                strokeData = subject.getParsedStrokeData(),
                initialAnimated = GlobalSettings.SubjectInfo.getStrokeOrderAnimated(),
                sizeDp = GlobalSettings.SubjectInfo.getStrokeOrderSize(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // Context sentences
        val hasContextSentences = showMeaningRelated && subject.hasContextSentences()
        if (hasContextSentences) {
            SectionDivider()
            SectionHeader("Context sentences:")
            for (sentence in subject.getContextSentences()) {
                InfoText(sentence.japanese.orEmpty())
                BlurRevealSentenceText(text = "- " + sentence.english.orEmpty() + " ")
            }
        }

        // Visually similar / used in tables, in the order given by the swap setting
        val visuallySimilarIds = subject.getVisuallySimilarSubjectIds()
        val amalgamationIds = subject.getAmalgamationSubjectIds()
        val hasVisuallySimilars = showMeaningRelated && visuallySimilarIds.isNotEmpty()
        val hasAmalgamations = showMeaningRelated && amalgamationIds.isNotEmpty()
        val visuallySimilarSection: @Composable () -> Unit = {
            if (hasVisuallySimilars) {
                SectionDivider()
                SectionHeader("Visually similar:")
                SubjectGrid(
                    subjects = visuallySimilarIds.mapNotNull { relatedById[it] },
                    onSubjectClick = onSubjectClick,
                    showMeaning = true,
                    showReading = showReadingRelated,
                )
            }
        }
        val amalgamationsSection: @Composable () -> Unit = {
            if (hasAmalgamations) {
                SectionDivider()
                SectionHeader("Used in:")
                SubjectGrid(
                    subjects = amalgamationIds.mapNotNull { relatedById[it] },
                    onSubjectClick = onSubjectClick,
                    showMeaning = true,
                    showReading = showReadingRelated,
                )
            }
        }
        if (GlobalSettings.SubjectInfo.getSwapSimilarAndAmalgamations()) {
            amalgamationsSection()
            visuallySimilarSection()
        }
        else {
            visuallySimilarSection()
            amalgamationsSection()
        }

        // Details table
        if (showMeaningAnswers || showReadingAnswers) {
            SectionDivider()
            DetailsTable(subject = subject)
        }

        // Resurrect / burn buttons
        if (subject.isResurrectable() || subject.isBurnable()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subject.isResurrectable()) {
                    Button(onClick = {
                        bulkOpType = BulkOpType.RESURRECT
                        bulkOpState.reset(1)
                    }) {
                        Text("Resurrect")
                    }
                }
                if (subject.isBurnable()) {
                    Button(onClick = {
                        bulkOpType = BulkOpType.BURN
                        bulkOpState.reset(1)
                    }) {
                        Text("Burn")
                    }
                }
            }
        }
    }

    val type = bulkOpType
    if (type != null && bulkOpState.visible) {
        BulkOpDialog(
            type = type,
            state = bulkOpState,
            onStart = {
                coroutineScope.launch {
                    bulkOpState.running = true
                    runBulkOp(type, listOf(subject.getId()), bulkOpState)
                }
            },
            onDismiss = { bulkOpType = null },
        )
    }
}

/**
 * The headline of the info dump, ported from the legacy SubjectInfoHeadlineView:
 * subject button (long-press for copy/search), title, meaning, reading or pitch
 * info, and a column with the reveal button and audio buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectInfoHeadline(
    subject: Subject,
    dumpStage: SubjectInfoDump,
    showMeaningAnswers: Boolean,
    showReadingAnswers: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textSize = GlobalSettings.Font.getFontSizeSubjectInfo()
    val showPitchInfo = GlobalSettings.SubjectInfo.getShowPitchInfo() && subject.hasPitchInfo() && showReadingAnswers

    Row(modifier = modifier.fillMaxWidth()) {
        SubjectTitleButton(subject = subject)

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(text = subject.getSimpleInfoTitle(), fontSize = textSize.sp)
            if (showMeaningAnswers && subject.hasMeanings()) {
                Text(
                    text = subject.getMeaningRichText("").toString(),
                    fontSize = (textSize + 4).sp,
                )
            }
            if (showPitchInfo) {
                for (reading in subject.getReadings()) {
                    val kana = reading.getReading()
                    if (kana.isNullOrEmpty()) {
                        continue
                    }
                    val pitchInfos = subject.getPitchInfoFor(kana)
                    if (pitchInfos.isNotEmpty()) {
                        PitchInfoGroup(kana = kana, pitchInfos = pitchInfos, textSizeSp = textSize)
                    }
                }
            }
            else if (showReadingAnswers && subject.hasReadings()) {
                Text(
                    text = subject.getRegularReadingRichText("").toString(),
                    fontSize = (textSize + 4).sp,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (dumpStage.getShowRevealButton()) {
                Button(onClick = onReveal) {
                    Text(dumpStage.getRevealButtonLabel())
                }
            }
            val type = subject.getType()
            if ((type.isVocabulary() || type.isKanaVocabulary()) && !type.isRadical() && !type.isKanji()) {
                if (type.isKanaVocabulary() || subject.getReadings().isNotEmpty()) {
                    AudioButton(
                        label = subject.getCharacters() ?: "",
                        onClick = { AudioUtil.playAudio(subject, null) },
                    )
                }
                else {
                    for (reading in subject.getReadings()) {
                        AudioButton(
                            label = reading.getValue(GlobalSettings.Other.getShowOnInKatakana()).orEmpty(),
                            onClick = { AudioUtil.playAudio(subject, reading.getReading()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_volume_up_24px),
            contentDescription = "Play audio",
        )
        Text(text = label)
    }
}

/**
 * The subject title button: the character (or radical image) on the type's
 * button background. Long-pressing offers "Copy title" and the configured
 * custom search engines 1-5, ported from the legacy SubjectInfoButtonView.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectTitleButton(subject: Subject, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(subject.getButtonBackgroundColor()))
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (subject.needsTitleImage()) {
                            Toast.makeText(
                                context,
                                "This radical has no text character. Can't copy or search for an image-only radical.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        else {
                            menuExpanded = true
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (subject.needsTitleImage()) {
                Image(
                    painter = painterResource(subject.getTitleImageId()),
                    contentDescription = subject.getSlug() ?: "",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(subject.getTextColor())),
                    modifier = Modifier.size(72.dp),
                )
            }
            else {
                Text(
                    text = subject.getCharacters() ?: subject.getSlug() ?: "",
                    color = Color(subject.getTextColor()),
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Copy title") },
                onClick = {
                    menuExpanded = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("title", subject.getCharacters() ?: ""))
                        Toast.makeText(context, "Subject title copied", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            for (i in 1..5) {
                if (GlobalSettings.Other.hasSearchEngine(i)) {
                    DropdownMenuItem(
                        text = { Text(GlobalSettings.Other.getSearchEngineName(i)) },
                        onClick = {
                            menuExpanded = false
                            val query = URLEncoder.encode(subject.getCharacters() ?: "", "UTF-8")
                            val url = GlobalSettings.Other.getSearchEngineUrl(i).replace("%s", query)
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Star rating display and tap-to-rate, ported from the legacy StarRatingView:
 * tapping the same star as the current rating clears it.
 */
@Composable
private fun StarRatingRow(subject: Subject, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            IconButton(
                onClick = {
                    val newNumStars = if (subject.getNumStars() == i) 0 else i
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            WkApplication.getDatabase().subjectDao().updateStars(subject.getId(), newNumStars)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Rate $i stars" },
            ) {
                Text(
                    text = if (subject.getNumStars() >= i) "★" else "☆",
                    fontSize = 20.sp,
                )
            }
        }
    }
}

/**
 * The user synonyms line with the "+" button to add a synonym, ported from the
 * legacy synonyms TextView + addSynonymButton.
 */
@Composable
private fun SynonymsRow(subject: Subject, modifier: Modifier = Modifier) {
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = subject.getMeaningSynonymsRichText().toString(),
            fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
        )
        TextButton(
            onClick = { showAddDialog = true },
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(text = "+")
        }
    }

    if (showAddDialog) {
        AddSynonymDialog(
            onDismiss = { showAddDialog = false },
            onSave = { value ->
                showAddDialog = false
                scope.launch { addSynonym(subject, value) }
            },
        )
    }
}

/**
 * Add a synonym to the subject's study material and schedule the save job, as
 * the legacy SubjectInfoView.addSynonym does: trim, ignore empty values and
 * duplicates, then push the whole material through SaveStudyMaterialJob.
 */
private suspend fun addSynonym(subject: Subject, value: String) {
    if (value.isEmpty()) {
        return
    }
    withContext(Dispatchers.IO) {
        val list = ArrayList(subject.getMeaningSynonyms())
        if (!list.contains(value)) {
            list.add(value)
        }
        subject.setMeaningSynonyms(list)

        val data = ArrayList<String>()
        data.add(subject.getId().toString())
        data.add(subject.getMeaningNote() ?: "")
        data.add(subject.getReadingNote() ?: "")
        data.addAll(list)
        val dataString = Converters.getObjectMapper().writeValueAsString(data)
        JobRunnerService.schedule(SaveStudyMaterialJob::class.java, dataString)
    }
}

/**
 * The "Add synonym" dialog, ported from the legacy addSynonymButton click
 * handler: an "Add synonym" dialog with a single text field, Cancel and Save
 * buttons, and a save-on-IME-done action.
 */
@Composable
private fun AddSynonymDialog(onDismiss: () -> Unit, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    fun save() {
        if (!saved) {
            saved = true
            onSave(text.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Add synonym") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
        },
        confirmButton = {
            TextButton(onClick = { save() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * The details table, ported from the legacy detailsTable rows including their
 * visibility rules and text formats.
 */
@Composable
private fun DetailsTable(subject: Subject, modifier: Modifier = Modifier) {
    val meaningTotal = subject.getMeaningCorrect() + subject.getMeaningIncorrect()
    val readingTotal = subject.getReadingCorrect() + subject.getReadingIncorrect()
    val total = meaningTotal + readingTotal

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DetailRow("SRS System:", subject.getSrsSystem().getName())
        DetailRow("SRS Stage:", subject.getSrsStage().getName())
        if (subject.getUnlockedAt() != 0L) {
            DetailRow("Unlocked:", subject.getFormattedUnlockedAt())
        }
        if (subject.getStartedAt() != 0L) {
            DetailRow("Started:", subject.getFormattedStartedAt())
        }
        if (subject.getPassedAt() != 0L) {
            DetailRow("Passed:", subject.getFormattedPassedAt())
        }
        if (subject.getBurnedAt() != 0L) {
            DetailRow("Burned:", subject.getFormattedBurnedAt())
        }
        if (subject.getResurrectedAt() != 0L) {
            DetailRow("Resurrected:", subject.getFormattedResurrectedAt())
        }
        if (subject.getAvailableAt() != 0L) {
            DetailRow("Next review:", subject.getFormattedAvailableAt())
        }
        if (total > 0) {
            DetailRow("Overall correct:", String.format(Locale.ROOT, "%d%%", subject.getPercentageCorrect()))
        }
        if (meaningTotal > 0) {
            DetailRow(
                "Meaning correct:",
                String.format(
                    Locale.ROOT, "%d%%, max streak %d, current streak %s",
                    (100 * subject.getMeaningCorrect()) / meaningTotal,
                    subject.getMeaningMaxStreak(),
                    subject.getMeaningCurrentStreak(),
                ),
            )
        }
        if (readingTotal > 0) {
            DetailRow(
                "Reading correct:",
                String.format(
                    Locale.ROOT, "%d%%, max streak %d, current streak %s",
                    (100 * subject.getReadingCorrect()) / readingTotal,
                    subject.getReadingMaxStreak(),
                    subject.getReadingCurrentStreak(),
                ),
            )
        }
        if (subject.getFrequency() > 0) {
            DetailRow("Frequency:", subject.getFrequency().toString())
        }
        if (subject.getJlptLevel() > 0) {
            DetailRow("JLPT level:", subject.getJlptLevelAsString())
        }
        if (subject.getJoyoGrade() > 0) {
            DetailRow("Jōyō kanji grade:", subject.getJoyoGradeAsString())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
        )
    }
}

@Composable
private fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
    )
}

@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(top = 4.dp),
        thickness = 1.dp,
        color = Color(ThemeUtil.getColor(R.attr.colorDivider)),
    )
}
