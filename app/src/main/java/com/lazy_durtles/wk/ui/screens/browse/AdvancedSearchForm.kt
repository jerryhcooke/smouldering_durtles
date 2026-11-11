package com.lazy_durtles.wk.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.db.Converters
import com.lazy_durtles.wk.enums.SearchSortOrder
import com.lazy_durtles.wk.enums.SubjectType
import com.lazy_durtles.wk.model.AdvancedSearchParameters
import com.lazy_durtles.wk.model.SrsSystemRepository
import com.lazy_durtles.wk.util.TextUtil

/**
 * State holder for the advanced search form, ported from the legacy
 * AdvancedSearchFormView. Empty text fields mean "not set", like the legacy
 * form where only non-empty fields are parsed into the parameters.
 */
class AdvancedSearchFormState {
    var minLevel by mutableStateOf("")
    var maxLevel by mutableStateOf("")
    var minFrequency by mutableStateOf("")
    var maxFrequency by mutableStateOf("")
    var leechesOnly by mutableStateOf(false)
    var reviewLessHours by mutableStateOf("")
    var reviewMoreHours by mutableStateOf("")
    var incorrectHours by mutableStateOf("")
    var burnedLessDays by mutableStateOf("")
    var burnedMoreDays by mutableStateOf("")
    var sortOrder by mutableStateOf(SearchSortOrder.TYPE)
    val starRatings = mutableStateListOf<Int>()
    val srsStages = mutableStateListOf<String>()
    val itemTypes = mutableStateListOf<SubjectType>()
    val jlptLevels = mutableStateListOf<Int>()
    val joyoGrades = mutableStateListOf<Int>()

    /**
     * Extract the current contents of the form as parameters, ignoring
     * non-numeric input like the legacy extractParameters().
     */
    fun extractParameters(): AdvancedSearchParameters {
        val parameters = AdvancedSearchParameters()
        parameters.minLevel = minLevel.toIntOrNull()
        parameters.maxLevel = maxLevel.toIntOrNull()
        parameters.minFrequency = minFrequency.toIntOrNull()
        parameters.maxFrequency = maxFrequency.toIntOrNull()
        parameters.upcomingReviewLessThan = reviewLessHours.toIntOrNull()
        parameters.upcomingReviewMoreThan = reviewMoreHours.toIntOrNull()
        parameters.incorrectAnswerWithin = incorrectHours.toIntOrNull()
        parameters.burnedLessThan = burnedLessDays.toIntOrNull()
        parameters.burnedMoreThan = burnedMoreDays.toIntOrNull()
        parameters.leechesOnly = leechesOnly
        parameters.sortOrder = sortOrder
        parameters.starRatings.addAll(starRatings)
        parameters.srsStages.addAll(srsStages)
        parameters.itemTypes.addAll(itemTypes)
        parameters.jlptLevels.addAll(jlptLevels)
        parameters.joyoGrades.addAll(joyoGrades)
        return parameters
    }

    /**
     * Inject the supplied parameters into the form.
     */
    fun injectParameters(parameters: AdvancedSearchParameters) {
        minLevel = parameters.minLevel?.toString() ?: ""
        maxLevel = parameters.maxLevel?.toString() ?: ""
        minFrequency = parameters.minFrequency?.toString() ?: ""
        maxFrequency = parameters.maxFrequency?.toString() ?: ""
        reviewLessHours = parameters.upcomingReviewLessThan?.toString() ?: ""
        reviewMoreHours = parameters.upcomingReviewMoreThan?.toString() ?: ""
        incorrectHours = parameters.incorrectAnswerWithin?.toString() ?: ""
        burnedLessDays = parameters.burnedLessThan?.toString() ?: ""
        burnedMoreDays = parameters.burnedMoreThan?.toString() ?: ""
        leechesOnly = parameters.leechesOnly
        sortOrder = parameters.sortOrder
        starRatings.clear()
        starRatings.addAll(parameters.starRatings)
        srsStages.clear()
        srsStages.addAll(parameters.srsStages)
        itemTypes.clear()
        itemTypes.addAll(parameters.itemTypes)
        jlptLevels.clear()
        jlptLevels.addAll(parameters.jlptLevels)
        joyoGrades.clear()
        joyoGrades.addAll(parameters.joyoGrades)
    }
}

/**
 * Serialize parameters to the JSON consumed by SearchResultsScreen and
 * SearchUtil.searchSubjects(2, ...).
 */
fun advancedSearchParametersJson(parameters: AdvancedSearchParameters): String? = runCatching {
    Converters.getObjectMapper().writeValueAsString(parameters)
}.getOrNull()

/**
 * Parse parameters JSON produced by this form or by the legacy app.
 */
fun parseAdvancedSearchParameters(json: String): AdvancedSearchParameters? = runCatching {
    Converters.getObjectMapper().readValue(json, AdvancedSearchParameters::class.java)
}.getOrNull()

/**
 * Build the parameters JSON for a level browse, mirroring the legacy
 * SearchResultFragment searchType 0 handling (minLevel = maxLevel = level).
 */
fun levelSearchParametersJson(level: Int): String? {
    val parameters = AdvancedSearchParameters()
    parameters.minLevel = level
    parameters.maxLevel = level
    return advancedSearchParametersJson(parameters)
}

/**
 * The SRS stage filter options, in the order and with the tags the legacy
 * AdvancedSearchFormView assigns: locked, initiate, apprentice I..N, guru
 * I..N, master, enlightened, burned.
 */
private fun srsStageOptions(): List<Pair<String, String>> {
    val options = mutableListOf("Locked" to "locked", "Initiate" to "initial")
    for (i in 0 until SrsSystemRepository.getMaxNumApprenticeStages()) {
        options.add("Apprentice " + TextUtil.getRomanNumeral(i + 1) to "prepass:$i")
    }
    for (i in 0 until SrsSystemRepository.getMaxNumGuruStages()) {
        options.add("Guru " + TextUtil.getRomanNumeral(i + 1) to "pass:$i")
    }
    options.add("Master" to "master")
    options.add("Enlightened" to "enlightened")
    options.add("Burned" to "burned")
    return options
}

private fun starRatingLabel(rating: Int): String {
    val filled = "★".repeat(rating)
    val empty = "☆".repeat(5 - rating)
    return if (filled.isEmpty()) "$empty:" else "$filled $empty:"
}

/**
 * The advanced search filter form, ported from the legacy
 * AdvancedSearchFormView + advanced_search.xml. Shows Search buttons at the
 * top and bottom of the form.
 */
@Composable
fun AdvancedSearchForm(
    state: AdvancedSearchFormState,
    onSearch: (searchParametersJson: String) -> Unit,
    modifier: Modifier = Modifier,
    searchButtonLabel: String = "Search",
    showSortOrder: Boolean = true,
) {
    fun submit() {
        advancedSearchParametersJson(state.extractParameters())?.let(onSearch)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { submit() }, modifier = Modifier.padding(top = 8.dp)) {
            Text(searchButtonLabel)
        }

        RangeFieldRow(
            label = "Level range:",
            minValue = state.minLevel,
            maxValue = state.maxLevel,
            onMinChange = { state.minLevel = it },
            onMaxChange = { state.maxLevel = it },
        )

        RangeFieldRow(
            label = "Frequency range:",
            minValue = state.minFrequency,
            maxValue = state.maxFrequency,
            onMinChange = { state.minFrequency = it },
            onMaxChange = { state.maxFrequency = it },
        )

        SwitchRow(label = "Only leeches:", checked = state.leechesOnly, onCheckedChange = { state.leechesOnly = it })

        NumberUnitFieldRow(
            label = "Next review in less than:",
            value = state.reviewLessHours,
            onValueChange = { state.reviewLessHours = it },
            unit = "hours",
        )

        NumberUnitFieldRow(
            label = "Next review in more than:",
            value = state.reviewMoreHours,
            onValueChange = { state.reviewMoreHours = it },
            unit = "hours",
        )

        NumberUnitFieldRow(
            label = "Incorrect answer less than:",
            value = state.incorrectHours,
            onValueChange = { state.incorrectHours = it },
            unit = "hours ago",
        )

        NumberUnitFieldRow(
            label = "Burned less than:",
            value = state.burnedLessDays,
            onValueChange = { state.burnedLessDays = it },
            unit = "days ago",
        )

        NumberUnitFieldRow(
            label = "Burned more than:",
            value = state.burnedMoreDays,
            onValueChange = { state.burnedMoreDays = it },
            unit = "days ago",
        )

        if (showSortOrder) {
            SortOrderRow(selected = state.sortOrder, onSelect = { state.sortOrder = it })
        }

        if (GlobalSettings.Other.getEnableStarRatings()) {
            SectionHeader("Star rating:")
            for (rating in 0..5) {
                SwitchRow(
                    label = starRatingLabel(rating),
                    checked = rating in state.starRatings,
                    onCheckedChange = { checked ->
                        if (checked) state.starRatings.add(rating) else state.starRatings.remove(rating)
                    },
                )
            }
        }

        SectionHeader("SRS stages:")
        for ((label, tag) in srsStageOptions()) {
            SwitchRow(
                label = "$label:",
                checked = tag in state.srsStages,
                onCheckedChange = { checked ->
                    if (checked) state.srsStages.add(tag) else state.srsStages.remove(tag)
                },
            )
        }

        SectionHeader("Subject types:")
        for ((label, type) in listOf(
            "Radical:" to SubjectType.WANIKANI_RADICAL,
            "Kanji:" to SubjectType.WANIKANI_KANJI,
            "Vocabulary:" to SubjectType.WANIKANI_VOCAB,
            "Kana Vocabulary:" to SubjectType.WANIKANI_KANA_VOCAB,
        )) {
            SwitchRow(
                label = label,
                checked = type in state.itemTypes,
                onCheckedChange = { checked ->
                    if (checked) state.itemTypes.add(type) else state.itemTypes.remove(type)
                },
            )
        }

        SectionHeader("JLPT levels:")
        for ((label, value) in listOf("None:" to 0, "N5:" to 5, "N4:" to 4, "N3:" to 3, "N2:" to 2, "N1:" to 1)) {
            SwitchRow(
                label = label,
                checked = value in state.jlptLevels,
                onCheckedChange = { checked ->
                    if (checked) state.jlptLevels.add(value) else state.jlptLevels.remove(value)
                },
            )
        }

        SectionHeader("Jōyō kanji grades:")
        for ((label, value) in listOf(
            "None:" to 0, "1:" to 1, "2:" to 2, "3:" to 3,
            "4:" to 4, "5:" to 5, "6:" to 6, "Middle school:" to 7,
        )) {
            SwitchRow(
                label = label,
                checked = value in state.joyoGrades,
                onCheckedChange = { checked ->
                    if (checked) state.joyoGrades.add(value) else state.joyoGrades.remove(value)
                },
            )
        }

        Button(onClick = { submit() }, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            Text(searchButtonLabel)
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RangeFieldRow(
    label: String,
    minValue: String,
    maxValue: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        NumberField(value = minValue, onValueChange = onMinChange, hint = "Min", modifier = Modifier.weight(1f))
        Text(text = "-", style = MaterialTheme.typography.bodyMedium)
        NumberField(value = maxValue, onValueChange = onMaxChange, hint = "Max", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NumberUnitFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        NumberField(value = value, onValueChange = onValueChange, hint = null, modifier = Modifier.weight(1f))
        Text(text = unit, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = if (hint != null) {
            { Text(hint) }
        }
        else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOrderRow(
    selected: SearchSortOrder,
    onSelect: (SearchSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember { SearchSortOrder.descriptionValues() }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Sort order:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = options[selected.ordinal],
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (option in options) {
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(SearchSortOrder.forDescription(option, SearchSortOrder.TYPE))
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
