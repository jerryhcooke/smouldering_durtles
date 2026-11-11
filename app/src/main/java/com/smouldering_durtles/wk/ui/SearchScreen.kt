package com.smouldering_durtles.wk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.ui.screens.browse.AdvancedSearchForm
import com.smouldering_durtles.wk.ui.screens.browse.AdvancedSearchFormState
import com.smouldering_durtles.wk.ui.screens.browse.parseAdvancedSearchParameters
import com.smouldering_durtles.wk.ui.screens.search.SearchPresetSection
import com.smouldering_durtles.wk.util.Logger
import com.smouldering_durtles.wk.util.SearchUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE_MS = 400L
private const val MIN_QUERY_LENGTH = 3

/** Marker type so the search screen can get its own [Logger] instance. */
private class SearchScreenLogMarker

/**
 * A single search result: the subject plus a pre-computed display label.
 */
private data class SearchResultItem(val subject: Subject, val label: String)

/**
 * The search screen (the main UI's Search tab). Hosts the keyword search, the
 * search presets, and the advanced filter form — the roles of the legacy
 * BrowseOverviewFragment and the advanced search screen. Submitting a keyword
 * or the advanced form, or running a preset, opens the search results screen
 * via [onSearchResults] (parameters JSON, search type, preset name).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    onSearchResults: (searchParametersJson: String, searchType: Int, presetName: String?) -> Unit = { _, _, _ -> },
    initialParametersJson: String? = null,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    val formState = remember { AdvancedSearchFormState() }

    // Preload the advanced form when coming back from "Refine search" on the
    // results screen. A non-JSON value is a keyword search; prefill the
    // query box with it, like the legacy search prefill behaviour.
    LaunchedEffect(initialParametersJson) {
        if (initialParametersJson != null) {
            val parameters = withContext(Dispatchers.IO) {
                parseAdvancedSearchParameters(initialParametersJson)
            }
            if (parameters != null) {
                formState.injectParameters(parameters)
                showAdvanced = true
            }
            else {
                query = initialParametersJson
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Search presets: pick, run and delete, like the legacy browse overview.
        SearchPresetSection(
            onRunPreset = { preset ->
                onSearchResults(preset.data, preset.type, preset.name)
            },
        )

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { submitted ->
                if (submitted.trim().length >= MIN_QUERY_LENGTH) {
                    onSearchResults(submitted.trim(), 1, null)
                }
            },
            active = false,
            onActiveChange = { },
            placeholder = { Text("Search") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
        }

        // Advanced filters, collapsed by default.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (showAdvanced) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (showAdvanced) "Collapse" else "Expand",
            )
            Text(
                text = "Advanced filters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (showAdvanced) {
            AdvancedSearchForm(
                state = formState,
                onSearch = { json -> onSearchResults(json, 2, null) },
            )
        }

        // Debounce the query, then run the search off the main thread.
        LaunchedEffect(query) {
            val trimmed = query.trim()
            errorMessage = null
            if (trimmed.length < MIN_QUERY_LENGTH) {
                results = emptyList()
                searching = false
                return@LaunchedEffect
            }
            searching = true
            delay(SEARCH_DEBOUNCE_MS)
            if (query.trim() != trimmed) {
                return@LaunchedEffect
            }
            try {
                val found = withContext(Dispatchers.IO) {
                    SearchUtil.searchSubjects(1, trimmed)
                }
                results = found
                errorMessage = null
            }
            catch (e: Exception) {
                Logger.get(SearchScreenLogMarker::class.java).error(e, "Search failed")
                errorMessage = "Something went wrong while searching. Please try again."
            }
            searching = false
        }

        when {
            errorMessage != null -> StatusText(errorMessage!!)
            !searching && query.trim().length >= MIN_QUERY_LENGTH && results.isEmpty() ->
                StatusText("No subjects found for \"${query.trim()}\".")
            !searching && query.trim().length < MIN_QUERY_LENGTH && results.isEmpty() ->
                SearchEmptyState()
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rendered eagerly inside the screen's vertical scroll: a
                // LazyColumn here would be measured with infinite height and
                // crash. The result set is capped at MAX_SEARCH_HITS.
                for (subject in results) {
                    SubjectResultCard(
                        subject = subject,
                        onClick = {
                            onSubjectClick(subject.getId(), results.map { it.getId() })
                        },
                    )
                }
            }
        }

        if (searching) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SearchEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = "Type to search your subjects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubjectResultCard(
    subject: Subject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subject.getCharacters() ?: subject.getSlug() ?: "",
                style = MaterialTheme.typography.headlineMedium,
            )
            Column {
                Text(
                    text = subject.getOneMeaning(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = subject.getType().getShortDescription()
                        + " · level " + subject.getLevel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
