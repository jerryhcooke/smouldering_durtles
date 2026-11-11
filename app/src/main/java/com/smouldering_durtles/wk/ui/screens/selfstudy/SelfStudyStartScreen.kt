package com.smouldering_durtles.wk.ui.screens.selfstudy

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.db.model.SearchPreset
import com.smouldering_durtles.wk.enums.SearchSortOrder
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.livedata.LiveSearchPresets
import com.smouldering_durtles.wk.model.AdvancedSearchParameters
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.model.SrsSystemRepository
import com.smouldering_durtles.wk.util.SearchUtil
import com.smouldering_durtles.wk.util.TextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The name of the hidden preset that stores the last used self-study parameters.
 */
private const val SELF_STUDY_DEFAULT_PRESET = "\u0000SELF_STUDY_DEFAULT"

/**
 * Screen for starting a self-study session, ported from the legacy
 * SelfStudyStartActivity. Shows the search presets to run directly, and the
 * advanced search form to compose a new self-study quiz. Results from a
 * self-study quiz are not reported to WaniKani.
 *
 * @param onBack invoked to leave this screen
 * @param onSessionStarted invoked after a self-study session was started successfully
 * @param initialSearchParametersJson optional advanced search parameters JSON to
 *     preload into the form (e.g. when starting from search results); when absent,
 *     the last used self-study parameters are restored instead
 * @param modifier the modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfStudyStartScreen(
    onBack: () -> Unit,
    onSessionStarted: () -> Unit,
    modifier: Modifier = Modifier,
    initialSearchParametersJson: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The legacy activity finishes itself when a session is already in progress.
    LaunchedEffect(Unit) {
        if (!Session.getInstance().isInactive()) {
            onBack()
        }
    }

    val presets by LiveSearchPresets.getInstance().observeAsState()
    val presetNames = remember(presets) {
        presets.orEmpty().filter { !it.name.startsWith("\u0000") }.map { it.name }
    }
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    var presetDropdownOpen by remember { mutableStateOf(false) }
    var deletePresetName by remember { mutableStateOf<String?>(null) }

    var minLevel by remember { mutableStateOf("") }
    var maxLevel by remember { mutableStateOf("") }
    var minFrequency by remember { mutableStateOf("") }
    var maxFrequency by remember { mutableStateOf("") }
    var leechesOnly by remember { mutableStateOf(false) }
    var reviewLessHours by remember { mutableStateOf("") }
    var reviewMoreHours by remember { mutableStateOf("") }
    var incorrectHours by remember { mutableStateOf("") }
    var burnedLessDays by remember { mutableStateOf("") }
    var burnedMoreDays by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SearchSortOrder.TYPE) }
    var starRatings by remember { mutableStateOf(setOf<Int>()) }
    var srsStages by remember { mutableStateOf(setOf<String>()) }
    var itemTypes by remember { mutableStateOf(setOf<SubjectType>()) }
    var jlptLevels by remember { mutableStateOf(setOf<Int>()) }
    var joyoGrades by remember { mutableStateOf(setOf<Int>()) }

    var tutorialDismissed by remember {
        mutableStateOf(GlobalSettings.Tutorials.getStartSelfStudyDismissed())
    }
    var starting by remember { mutableStateOf(false) }

    fun injectParameters(parameters: AdvancedSearchParameters) {
        minLevel = parameters.minLevel?.toString() ?: ""
        maxLevel = parameters.maxLevel?.toString() ?: ""
        minFrequency = parameters.minFrequency?.toString() ?: ""
        maxFrequency = parameters.maxFrequency?.toString() ?: ""
        leechesOnly = parameters.leechesOnly
        reviewLessHours = parameters.upcomingReviewLessThan?.toString() ?: ""
        reviewMoreHours = parameters.upcomingReviewMoreThan?.toString() ?: ""
        incorrectHours = parameters.incorrectAnswerWithin?.toString() ?: ""
        burnedLessDays = parameters.burnedLessThan?.toString() ?: ""
        burnedMoreDays = parameters.burnedMoreThan?.toString() ?: ""
        sortOrder = parameters.sortOrder
        starRatings = parameters.starRatings.toSet()
        srsStages = parameters.srsStages.toSet()
        itemTypes = parameters.itemTypes.toSet()
        jlptLevels = parameters.jlptLevels.toSet()
        joyoGrades = parameters.joyoGrades.toSet()
    }

    fun extractParameters(): AdvancedSearchParameters {
        val parameters = AdvancedSearchParameters()
        minLevel.toIntOrNull()?.let { parameters.minLevel = it }
        maxLevel.toIntOrNull()?.let { parameters.maxLevel = it }
        minFrequency.toIntOrNull()?.let { parameters.minFrequency = it }
        maxFrequency.toIntOrNull()?.let { parameters.maxFrequency = it }
        reviewLessHours.toIntOrNull()?.let { parameters.upcomingReviewLessThan = it }
        reviewMoreHours.toIntOrNull()?.let { parameters.upcomingReviewMoreThan = it }
        incorrectHours.toIntOrNull()?.let { parameters.incorrectAnswerWithin = it }
        burnedLessDays.toIntOrNull()?.let { parameters.burnedLessThan = it }
        burnedMoreDays.toIntOrNull()?.let { parameters.burnedMoreThan = it }
        parameters.leechesOnly = leechesOnly
        parameters.sortOrder = sortOrder
        parameters.starRatings.addAll(starRatings)
        parameters.srsStages.addAll(srsStages)
        parameters.itemTypes.addAll(itemTypes)
        parameters.jlptLevels.addAll(jlptLevels)
        parameters.joyoGrades.addAll(joyoGrades)
        return parameters
    }

    // Restore the form from the preloaded parameters, or from the hidden preset
    // with the last used self-study parameters, like the legacy activity does on
    // create.
    LaunchedEffect(Unit) {
        val json = initialSearchParametersJson
            ?: LiveSearchPresets.getInstance().getByName(SELF_STUDY_DEFAULT_PRESET)?.data
        if (json != null) {
            try {
                val parameters = withContext(Dispatchers.IO) {
                    Converters.getObjectMapper().readValue(json, AdvancedSearchParameters::class.java)
                }
                injectParameters(parameters)
            } catch (_: Exception) {
                // Malformed parameters are ignored, like the legacy safe() wrapper.
            }
        }
    }

    fun startSession(searchType: Int, searchParameters: String, saveSelfStudyPreset: Boolean) {
        if (starting) {
            return
        }
        starting = true
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    if (saveSelfStudyPreset && searchType == 2) {
                        WkApplication.getDatabase().searchPresetDao()
                            .setPreset(SELF_STUDY_DEFAULT_PRESET, 2, searchParameters)
                    }
                    val subjects = SearchUtil.searchSubjects(searchType, searchParameters)
                    if (subjects.isNotEmpty() && Session.getInstance().isInactive()) {
                        Session.getInstance().startNewSelfStudySession(subjects)
                    }
                    subjects.isNotEmpty() && !Session.getInstance().isInactive()
                } catch (_: Exception) {
                    false
                }
            }
            starting = false
            if (success) {
                onSessionStarted()
            }
            else {
                Toast.makeText(context, "No subjects found for self-study!", Toast.LENGTH_SHORT).show()
                onBack()
            }
        }
    }

    fun startFromForm() {
        try {
            val json = Converters.getObjectMapper().writeValueAsString(extractParameters())
            startSession(2, json, true)
        } catch (_: Exception) {
            // Parameter serialization failed; ignore like the legacy safe() wrapper.
        }
    }

    fun startFromPreset(preset: SearchPreset) {
        startSession(preset.type, preset.data, false)
    }

    deletePresetName?.let { name ->
        AlertDialog(
            onDismissRequest = { deletePresetName = null },
            title = { Text("Delete preset?") },
            text = { Text("Are you sure you want to delete the preset named '$name'?") },
            dismissButton = {
                TextButton(onClick = { deletePresetName = null }) {
                    Text("No")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deletePresetName = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            WkApplication.getDatabase().searchPresetDao().deletePreset(name)
                        }
                        Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Yes")
                }
            },
        )
    }

    val srsStageOptions = remember {
        buildList {
            add("locked" to "Locked")
            add("initial" to "Initiate")
            repeat(SrsSystemRepository.getMaxNumApprenticeStages()) { i ->
                add("prepass:$i" to "Apprentice ${TextUtil.getRomanNumeral(i + 1)}")
            }
            repeat(SrsSystemRepository.getMaxNumGuruStages()) { i ->
                add("pass:$i" to "Guru ${TextUtil.getRomanNumeral(i + 1)}")
            }
            add("master" to "Master")
            add("enlightened" to "Enlightened")
            add("burned" to "Burned")
        }
    }
    val starRatingOptions = remember {
        (0..5).map { rating ->
            rating to (0 until 5).joinToString(" ") { index -> if (index < rating) "★" else "☆" } + ":"
        }
    }
    val itemTypeOptions = remember {
        listOf(
            SubjectType.WANIKANI_RADICAL to "Radical:",
            SubjectType.WANIKANI_KANJI to "Kanji:",
            SubjectType.WANIKANI_VOCAB to "Vocabulary:",
            SubjectType.WANIKANI_KANA_VOCAB to "Kana Vocabulary:",
        )
    }
    val jlptLevelOptions = remember {
        listOf(0 to "None:", 5 to "N5:", 4 to "N4:", 3 to "N3:", 2 to "N2:", 1 to "N1:")
    }
    val joyoGradeOptions = remember {
        listOf(
            0 to "None:",
            1 to "1:",
            2 to "2:",
            3 to "3:",
            4 to "4:",
            5 to "5:",
            6 to "6:",
            7 to "Middle school:",
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Self-study quiz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!tutorialDismissed) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "From this screen you can start a self-study quiz. The subjects for this quiz can be selected"
                                + " by filling out the search form below. If you have any search presets defined, you can also start a self-study"
                                + " based on those directly. Go to 'Browse/search...' from the menu to define presets.\n\n"
                                + "Results from a self-study quiz are not reported to WaniKani. They don't affect your progress"
                                + " and are only for your own self-study purposes.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            GlobalSettings.Tutorials.setStartSelfStudyDismissed(true)
                            tutorialDismissed = true
                        }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (presetNames.isNotEmpty()) {
                val selected = selectedPresetName?.takeIf { it in presetNames } ?: presetNames.first()
                Text(
                    text = "Search presets:",
                    style = MaterialTheme.typography.titleLarge,
                )
                Box {
                    OutlinedButton(
                        onClick = { presetDropdownOpen = true },
                        enabled = !starting,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(selected)
                    }
                    DropdownMenu(
                        expanded = presetDropdownOpen,
                        onDismissRequest = { presetDropdownOpen = false },
                    ) {
                        presetNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedPresetName = name
                                    presetDropdownOpen = false
                                },
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Button(
                        onClick = {
                            LiveSearchPresets.getInstance().getByName(selected)?.let { startFromPreset(it) }
                        },
                        enabled = !starting,
                    ) {
                        Text("Start")
                    }
                    Button(
                        onClick = { deletePresetName = selected },
                        enabled = !starting,
                    ) {
                        Text("Delete")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            Text(
                text = "Selection criteria for self-study:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = { startFromForm() }, enabled = !starting) {
                Text("Start")
            }

            NumberFieldRow(
                label = "Level range:",
                minValue = minLevel,
                maxValue = maxLevel,
                onMinChange = { minLevel = it },
                onMaxChange = { maxLevel = it },
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Frequency range:",
                minValue = minFrequency,
                maxValue = maxFrequency,
                onMinChange = { minFrequency = it },
                onMaxChange = { maxFrequency = it },
                enabled = !starting,
            )
            SwitchRow(
                label = "Only leeches:",
                checked = leechesOnly,
                onCheckedChange = { leechesOnly = it },
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Next review in less than:",
                minValue = reviewLessHours,
                onMinChange = { reviewLessHours = it },
                suffix = "hours",
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Next review in more than:",
                minValue = reviewMoreHours,
                onMinChange = { reviewMoreHours = it },
                suffix = "hours",
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Incorrect answer less than:",
                minValue = incorrectHours,
                onMinChange = { incorrectHours = it },
                suffix = "hours ago",
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Burned less than:",
                minValue = burnedLessDays,
                onMinChange = { burnedLessDays = it },
                suffix = "days ago",
                enabled = !starting,
            )
            NumberFieldRow(
                label = "Burned more than:",
                minValue = burnedMoreDays,
                onMinChange = { burnedMoreDays = it },
                suffix = "days ago",
                enabled = !starting,
            )

            if (GlobalSettings.Other.getEnableStarRatings()) {
                Text(
                    text = "Star rating:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                starRatingOptions.forEach { (rating, label) ->
                    SwitchRow(
                        label = label,
                        checked = rating in starRatings,
                        onCheckedChange = { checked ->
                            starRatings = if (checked) starRatings + rating else starRatings - rating
                        },
                        enabled = !starting,
                    )
                }
            }

            Text(
                text = "SRS stages:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            srsStageOptions.forEach { (stage, label) ->
                SwitchRow(
                    label = "$label:",
                    checked = stage in srsStages,
                    onCheckedChange = { checked ->
                        srsStages = if (checked) srsStages + stage else srsStages - stage
                    },
                    enabled = !starting,
                )
            }

            Text(
                text = "Subject types:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            itemTypeOptions.forEach { (type, label) ->
                SwitchRow(
                    label = label,
                    checked = type in itemTypes,
                    onCheckedChange = { checked ->
                        itemTypes = if (checked) itemTypes + type else itemTypes - type
                    },
                    enabled = !starting,
                )
            }

            Text(
                text = "JLPT levels:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            jlptLevelOptions.forEach { (level, label) ->
                SwitchRow(
                    label = label,
                    checked = level in jlptLevels,
                    onCheckedChange = { checked ->
                        jlptLevels = if (checked) jlptLevels + level else jlptLevels - level
                    },
                    enabled = !starting,
                )
            }

            Text(
                text = "Jōyō kanji grades:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            joyoGradeOptions.forEach { (grade, label) ->
                SwitchRow(
                    label = label,
                    checked = grade in joyoGrades,
                    onCheckedChange = { checked ->
                        joyoGrades = if (checked) joyoGrades + grade else joyoGrades - grade
                    },
                    enabled = !starting,
                )
            }

            Button(
                onClick = { startFromForm() },
                enabled = !starting,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Start")
            }
        }
    }
}

/**
 * A form row with a label and one or two numeric input fields, with an
 * optional unit suffix after the (single) field.
 */
@Composable
private fun NumberFieldRow(
    label: String,
    minValue: String,
    onMinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxValue: String? = null,
    onMaxChange: ((String) -> Unit)? = null,
    suffix: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.2f),
        )
        OutlinedTextField(
            value = minValue,
            onValueChange = onMinChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        if (maxValue != null && onMaxChange != null) {
            Text(text = "-", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = maxValue,
                onValueChange = onMaxChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        else if (suffix != null) {
            Text(text = suffix, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * A form row with a label and a switch, toggled by tapping the whole row.
 */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
