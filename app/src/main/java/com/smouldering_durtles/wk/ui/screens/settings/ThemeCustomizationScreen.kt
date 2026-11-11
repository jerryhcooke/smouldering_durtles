package com.smouldering_durtles.wk.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.madrapps.pikolo.listeners.OnColorSelectionListener
import com.smouldering_durtles.wk.Constants.NUM_THEME_CUSTOMIZATION_OPTIONS
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.enums.ActiveTheme
import com.smouldering_durtles.wk.ui.common.SelfishColorPicker
import java.util.Locale

private const val NUM_SUBJECT_TYPE_SLOTS = 4

private const val EXTRA1 = "\nUsed on the timeline chart, the SRS breakdown, and the Post-60 progression bar"
private const val EXTRA2 = "\nUsed on the Post-60 progression bar with subsections enabled"
private const val EXTRA3 = "\nUsed on the Level progression chart"

private val SLOT_DESCRIPTIONS = arrayOf(
    "The identifying colour for Radical subjects",
    "The identifying colour for Kanji subjects",
    "The identifying colour for Vocabulary subjects",
    "The identifying colour for Kana Vocabulary subjects",
    "The segment colour for Locked items$EXTRA1",
    "The segment colour for Initiate items (not started yet)$EXTRA1",
    "The segment colour for Apprentice items$EXTRA1",
    "The segment colour for Guru items$EXTRA1",
    "The segment colour for Master items$EXTRA1",
    "The segment colour for Enlightened items$EXTRA1",
    "The segment colour for Burned items$EXTRA1",
    "The segment colour for Apprentice I items$EXTRA2",
    "The segment colour for Apprentice II items$EXTRA2",
    "The segment colour for Apprentice III items$EXTRA2",
    "The segment colour for Apprentice IV items$EXTRA2",
    "The segment colour for Guru I items$EXTRA2",
    "The segment colour for Guru II items$EXTRA2",
    "The segment colour for Passed items$EXTRA3",
    "The segment colour for stage 7 (Apprentice IV)$EXTRA3",
    "The segment colour for stage 6$EXTRA3",
    "The segment colour for stage 5 (Apprentice III)$EXTRA3",
    "The segment colour for stage 4$EXTRA3",
    "The segment colour for stage 3 (Apprentice II)$EXTRA3",
    "The segment colour for stage 2$EXTRA3",
    "The segment colour for stage 1 (Apprentice I)$EXTRA3",
    "The segment colour for Initiate items$EXTRA3",
    "The segment colour for Locked items$EXTRA3",
    "The background colour for the Anki mode \"Show Answer\" button",
    "The background colour for the Anki mode \"Next\" button",
    "The background colour for the Anki mode \"Correct\" button",
    "The background colour for the Anki mode \"Incorrect\" button",
    "The text colour for the Anki mode buttons and answer text",
    "The background colour for the Anki mode answer text",
)

private data class SlotGroup(
    val title: String,
    val firstIndex: Int,
    val count: Int,
)

private val SLOT_GROUPS = listOf(
    SlotGroup("Subject Types", 0, 4),
    SlotGroup("SRS stages:", 4, 7),
    SlotGroup("Apprentice and Guru subsections:", 11, 6),
    SlotGroup("Level progression subsections:", 17, 10),
    SlotGroup("Anki mode colours:", 27, 6),
)

/**
 * The default (uncustomized) color for a slot, mirroring the legacy
 * ThemeCustomizationActivity.getBaseColor.
 */
private fun baseColorFor(index: Int): Int = when {
    index < 4 -> ActiveTheme.getBaseSubjectTypeBucketColors()[index]
    index < 11 -> ActiveTheme.getBaseShallowStageBucketColors()[index - 4]
    index < 15 -> ActiveTheme.getBasePrePassedBucketColors()[index - 11]
    index < 17 -> ActiveTheme.getBasePassedBucketColors()[index - 15]
    index < 27 -> ActiveTheme.getBaseLevelProgressionBucketColors()[index - 17]
    else -> ActiveTheme.getBaseAnkiColors()[index - 27]
}

/**
 * The color the picker shows for a slot: the chosen color, or the theme's
 * base color when the slot is uncustomized (0 is the "default" sentinel).
 * Alpha is forced opaque, as the legacy picker does.
 */
private fun pickerColorFor(chosenColors: IntArray, index: Int): Int {
    val chosen = chosenColors[index]
    val actual = if (chosen == 0) baseColorFor(index) else chosen
    return actual or 0xFF000000.toInt()
}

private fun slotBackgroundColor(theme: ActiveTheme, index: Int, chosen: Int): Int {
    if (index >= NUM_SUBJECT_TYPE_SLOTS) {
        return if (chosen == 0) baseColorFor(index) else chosen
    }
    return if (!theme.hasIdentBackground() || chosen == 0) {
        ActiveTheme.getBaseSubjectTypeBackgroundColors()[index]
    }
    else {
        chosen
    }
}

private fun slotTextColor(theme: ActiveTheme, index: Int, chosen: Int): Int {
    if (index >= NUM_SUBJECT_TYPE_SLOTS) {
        return if (chosen == 0) baseColorFor(index) else chosen
    }
    return if (theme.hasIdentBackground() || chosen == 0) {
        ActiveTheme.getBaseSubjectTypeTextColors()[index]
    }
    else {
        chosen
    }
}

private fun Modifier.dashedHighlightBorder(): Modifier = drawBehind {
    drawRoundRect(
        color = Color(0xFFD0D0D0),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

/**
 * Theme customization screen: pick custom colors for the 33 color slots of
 * the current theme with an HSL color picker, reset slots to their defaults,
 * and persist via GlobalSettings like the legacy ThemeCustomizationActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val theme = remember { GlobalSettings.Display.getTheme() }
    var chosenColors by remember(theme) {
        val stored = GlobalSettings.Display.getThemeCustomizations(theme)
        mutableStateOf(IntArray(NUM_THEME_CUSTOMIZATION_OPTIONS) { index -> stored.getOrNull(index) ?: 0 })
    }
    var selection by rememberSaveable { mutableIntStateOf(0) }
    var pickerColor by remember { mutableIntStateOf(pickerColorFor(chosenColors, selection)) }

    fun selectSlot(index: Int) {
        selection = index
        pickerColor = pickerColorFor(chosenColors, index)
    }

    fun saveColors() {
        GlobalSettings.Display.setThemeCustomizations(theme, chosenColors)
    }

    fun resetSelectedColor() {
        val next = chosenColors.clone()
        next[selection] = 0
        chosenColors = next
        pickerColor = pickerColorFor(next, selection)
        saveColors()
    }

    val selectedChosen = chosenColors[selection]
    val previewColor = if (selectedChosen == 0) baseColorFor(selection) else selectedChosen
    val rgbText = String.format(Locale.US, "RGB values:\n#%06X", previewColor and 0xFFFFFF)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Theme customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF000000))
                        .padding(vertical = 8.dp),
                ) {
                    SLOT_GROUPS.forEach { group ->
                        Text(
                            text = group.title,
                            color = Color(0xFFD0D0D0),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            for (index in group.firstIndex until group.firstIndex + group.count) {
                                ColorSlotCell(
                                    index = index,
                                    theme = theme,
                                    chosenColor = chosenColors[index],
                                    selected = selection == index,
                                    onSelect = { selectSlot(it) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Text(
                        text = SLOT_DESCRIPTIONS[selection],
                        color = Color(0xFFD0D0D0),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { resetSelectedColor() },
                        enabled = selectedChosen != 0,
                    ) {
                        Text(text = "Reset")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = rgbText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            SelfishColorPicker(context).apply {
                                setColorSelectionListener(object : OnColorSelectionListener {
                                    override fun onColorSelected(color: Int) {
                                        val next = chosenColors.clone()
                                        next[selection] = color
                                        chosenColors = next
                                    }

                                    override fun onColorSelectionStart(color: Int) {
                                        // No behavior, matching the legacy activity
                                    }

                                    override fun onColorSelectionEnd(color: Int) {
                                        saveColors()
                                    }
                                })
                            }
                        },
                        update = { picker -> picker.setColor(pickerColor) },
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(previewColor or 0xFF000000.toInt()), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSlotCell(
    index: Int,
    theme: ActiveTheme,
    chosenColor: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .then(if (selected) Modifier.dashedHighlightBorder() else Modifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(slotBackgroundColor(theme, index, chosenColor)))
                .clickable { onSelect(index) }
                .semantics { contentDescription = SLOT_DESCRIPTIONS[index] },
            contentAlignment = Alignment.Center,
        ) {
            if (index < NUM_SUBJECT_TYPE_SLOTS) {
                Text(
                    text = "亀",
                    fontSize = 24.sp,
                    color = Color(slotTextColor(theme, index, chosenColor)),
                    modifier = Modifier.padding(2.dp),
                )
            }
            else {
                Spacer(modifier = Modifier.fillMaxWidth().height(24.dp))
            }
        }
    }
}
