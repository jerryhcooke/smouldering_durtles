package com.lazy_durtles.wk.ui.screens.browse

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.Converters
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.enums.SearchSortOrder
import com.lazy_durtles.wk.enums.SubjectType
import com.lazy_durtles.wk.model.AdvancedSearchParameters
import com.lazy_durtles.wk.model.SrsSystemRepository
import com.lazy_durtles.wk.ui.common.BulkOpDialog
import com.lazy_durtles.wk.ui.common.BulkOpState
import com.lazy_durtles.wk.ui.common.BulkOpType
import com.lazy_durtles.wk.ui.common.filterForBulkOp
import com.lazy_durtles.wk.ui.common.runBulkOp
import com.lazy_durtles.wk.ui.screens.search.SavePresetDialog
import com.lazy_durtles.wk.util.SearchUtil
import com.lazy_durtles.wk.util.TextUtil
import com.lazy_durtles.wk.util.WebClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A subject list filtered by a search, styled after the legacy
 * SearchResultFragment: subjects grouped under collapsible headers for the
 * active sort order, the "%s: %d subject(s) found" line, and the result
 * actions menu (refine, save preset, self-study, resurrect/burn, star
 * ratings). Opened from the browse overview, SRS breakdown buckets, level
 * progress entries or presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    searchParametersJson: String,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    searchType: Int = 2,
    presetName: String? = null,
    onRefineSearch: (searchParametersJson: String) -> Unit = {},
    onStartSelfStudy: (searchParametersJson: String) -> Unit = {},
) {
    var loading by remember { mutableStateOf(true) }
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf(SearchSortOrder.TYPE) }
    var searchTime by remember { mutableLongStateOf(0L) }
    var collapsedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var pendingStarRating by remember { mutableIntStateOf(-1) }
    var bulkOpType by remember { mutableStateOf<BulkOpType?>(null) }
    val bulkOpState = remember { BulkOpState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val searchDescription = when (searchType) {
        0 -> "Level " + searchParametersJson
        1 -> "'" + searchParametersJson + "'"
        else -> "Advanced search"
    }

    LaunchedEffect(searchParametersJson, searchType) {
        loading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                if (searchType == 2) {
                    parseAdvancedSearchParameters(searchParametersJson)?.let { sortOrder = it.sortOrder }
                }
                SearchUtil.searchSubjects(searchType, searchParametersJson)
            }
            subjects = result
            searchTime = System.currentTimeMillis()
        }
        catch (e: Exception) {
            errorMessage = e.message ?: "Search failed"
        }
        loading = false
    }

    val canResurrect = subjects.any { it.isResurrectable() }
    val canBurn = subjects.any { it.isBurnable() }

    // The advanced parameters JSON representing the current search, used for
    // refine and self-study. Keyword searches have no parameters equivalent.
    val currentAdvancedJson = when (searchType) {
        2 -> searchParametersJson
        0 -> searchParametersJson.toIntOrNull()?.let { levelSearchParametersJson(it) }
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(presetName ?: "Search results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Refine search") },
                            onClick = {
                                menuExpanded = false
                                // Keyword searches refine with the raw query;
                                // level and advanced searches with their JSON.
                                onRefineSearch(currentAdvancedJson ?: searchParametersJson)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save preset") },
                            onClick = {
                                menuExpanded = false
                                showSavePresetDialog = true
                            },
                        )
                        if (subjects.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Start self-study quiz") },
                                onClick = {
                                    menuExpanded = false
                                    onStartSelfStudy(currentAdvancedJson ?: "")
                                },
                            )
                        }
                        if (canResurrect) {
                            DropdownMenuItem(
                                text = { Text("Resurrect burned items") },
                                onClick = {
                                    menuExpanded = false
                                    bulkOpType = BulkOpType.RESURRECT
                                    bulkOpState.reset(subjects.filter { it.isResurrectable() }.size)
                                },
                            )
                        }
                        if (canBurn) {
                            DropdownMenuItem(
                                text = { Text("Burn resurrected items") },
                                onClick = {
                                    menuExpanded = false
                                    bulkOpType = BulkOpType.BURN
                                    bulkOpState.reset(subjects.filter { it.isBurnable() }.size)
                                },
                            )
                        }
                        if (GlobalSettings.Other.getEnableStarRatings()) {
                            for (rating in 0..5) {
                                DropdownMenuItem(
                                    text = { Text("Set to ${starRatingMenuLabel(rating)}") },
                                    onClick = {
                                        menuExpanded = false
                                        pendingStarRating = rating
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!GlobalSettings.Tutorials.getSearchResultDismissed()) {
                SearchResultsTutorialCard()
            }
            Text(
                text = String.format("%s: %d subject(s) found", searchDescription, subjects.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(errorMessage ?: "")
                }
                else -> {
                    val entries = buildResultEntries(subjects, sortOrder, searchTime, collapsedTags)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        for (entry in entries) {
                            when (entry) {
                                is ResultEntry.Header -> item(key = "h_${entry.tag}") {
                                    ResultHeaderRow(
                                        title = entry.title,
                                        details = entry.details,
                                        collapsed = entry.collapsed,
                                        isTopLevel = entry.isTopLevel,
                                        onClick = {
                                            collapsedTags = if (entry.collapsed) {
                                                collapsedTags - entry.tag
                                            }
                                            else {
                                                collapsedTags + entry.tag
                                            }
                                        },
                                    )
                                }
                                is ResultEntry.SubjectEntry -> item(key = "s_${entry.subject.getId()}") {
                                    SubjectResultRow(
                                        subject = entry.subject,
                                        onClick = {
                                            onSubjectClick(entry.subject.getId(), subjects.map { it.getId() })
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            initialName = presetName ?: "",
            onDismiss = { showSavePresetDialog = false },
            onSave = { name ->
                showSavePresetDialog = false
                if (name.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        WkApplication.getDatabase().searchPresetDao().setPreset(name, searchType, searchParametersJson)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Preset '$name' saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )
    }

    if (pendingStarRating >= 0) {
        val count = subjects.size
        AlertDialog(
            onDismissRequest = { pendingStarRating = -1 },
            title = { Text("Set star ratings?") },
            text = {
                Text(
                    "Do you want to set the star rating for $count subject${if (count == 1) "" else "s"}"
                        + " to $pendingStarRating star${if (pendingStarRating == 1) "" else "s"}?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val rating = pendingStarRating
                    pendingStarRating = -1
                    scope.launch(Dispatchers.IO) {
                        for (subject in subjects) {
                            WkApplication.getDatabase().subjectDao().updateStars(subject.getId(), rating)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Star ratings updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStarRating = -1 }) {
                    Text("No")
                }
            },
        )
    }

    val type = bulkOpType
    if (type != null && bulkOpState.visible) {
        BulkOpDialog(
            type = type,
            state = bulkOpState,
            onStart = {
                val ids = subjects.filterForBulkOp(type)
                scope.launch {
                    bulkOpState.running = true
                    runBulkOp(type, ids, bulkOpState)
                }
            },
            onDismiss = { bulkOpType = null },
        )
    }
}

/**
 * The first-run tutorial text from the legacy SearchResultFragment.
 */
@Composable
private fun SearchResultsTutorialCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(8.dp),
    ) {
        Text(
            text = "This screen shows the results from the search you chose before."
                + " You can click on the various headers in the list to collapse/expand groups"
                + " of subjects.\n"
                + "Other actions such as refining your search, saving a preset or "
                + "starting a self-study session can be accessed from the menu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        TextButton(onClick = { GlobalSettings.Tutorials.setSearchResultDismissed(true) }) {
            Text("Dismiss")
        }
    }
}

private fun starRatingMenuLabel(rating: Int): String {
    val filled = "★".repeat(rating)
    val empty = "☆".repeat(5 - rating)
    return if (filled.isEmpty()) empty else "$filled $empty"
}

/**
 * One entry of the flattened result list.
 */
private sealed interface ResultEntry {
    data class Header(
        val tag: String,
        val title: String,
        val details: String,
        val collapsed: Boolean,
        val isTopLevel: Boolean,
    ) : ResultEntry

    data class SubjectEntry(val subject: Subject) : ResultEntry
}

private class SubHeaderGroup(val tag: String, val type: SubjectType, val subjects: MutableList<Subject> = mutableListOf())

private class TopHeaderGroup(
    val tag: String,
    val title: String?,
    val type: SubjectType?,
    val subs: MutableList<SubHeaderGroup> = mutableListOf(),
    val directSubjects: MutableList<Subject> = mutableListOf(),
)

/**
 * Group the search result under headers for the given sort order, ported from
 * the legacy SearchResultAdapter.setResult(): runs of comparator-equal
 * subjects form sub groups by subject type, which are merged under top-level
 * headers by the order's top-level tag. Collapsed headers hide their content.
 */
private fun buildResultEntries(
    subjects: List<Subject>,
    sortOrder: SearchSortOrder,
    searchTime: Long,
    collapsedTags: Set<String>,
): List<ResultEntry> {
    if (subjects.isEmpty()) {
        return emptyList()
    }
    val comparator = sortOrder.getComparator(searchTime)
    val sorted = subjects.sortedWith(comparator)

    val topGroups = mutableListOf<TopHeaderGroup>()
    var start = 0
    while (start < sorted.size) {
        val startSubject = sorted[start]
        var end = start + 1
        while (end < sorted.size && comparator.compare(startSubject, sorted[end]) == 0) {
            end++
        }
        if (sortOrder.isSingleLevel()) {
            val tag = startSubject.getType().name
            val group = TopHeaderGroup(tag = tag, title = null, type = startSubject.getType())
            for (i in start until end) {
                group.directSubjects.add(sorted[i])
            }
            topGroups.add(group)
        }
        else {
            val topTag = sortOrder.getTopLevelTag(startSubject, searchTime)
            val group = topGroups.firstOrNull { it.tag == topTag }
                ?: TopHeaderGroup(
                    tag = topTag,
                    title = topLevelHeaderTitle(sortOrder, startSubject, searchTime),
                    type = null,
                ).also { topGroups.add(it) }
            val subTag = "$topTag ${startSubject.getType().name}"
            val sub = group.subs.firstOrNull { it.tag == subTag }
                ?: SubHeaderGroup(tag = subTag, type = startSubject.getType()).also { group.subs.add(it) }
            for (i in start until end) {
                sub.subjects.add(sorted[i])
            }
        }
        start = end
    }

    val entries = mutableListOf<ResultEntry>()
    for (top in topGroups) {
        if (top.title == null && top.type != null) {
            val topCollapsed = top.tag in collapsedTags
            entries.add(
                ResultEntry.Header(
                    tag = top.tag,
                    title = typeHeaderTitle(top.type, top.directSubjects.size),
                    details = headerDetails(top.directSubjects),
                    collapsed = topCollapsed,
                    isTopLevel = true,
                ),
            )
            if (!topCollapsed) {
                for (subject in top.directSubjects) {
                    entries.add(ResultEntry.SubjectEntry(subject))
                }
            }
        }
        else {
            val allSubjects = top.subs.flatMap { it.subjects }
            val topCollapsed = top.tag in collapsedTags
            entries.add(
                ResultEntry.Header(
                    tag = top.tag,
                    title = top.title ?: "",
                    details = headerDetails(allSubjects),
                    collapsed = topCollapsed,
                    isTopLevel = true,
                ),
            )
            if (!topCollapsed) {
                for (sub in top.subs) {
                    val subCollapsed = sub.tag in collapsedTags
                    entries.add(
                        ResultEntry.Header(
                            tag = sub.tag,
                            title = typeHeaderTitle(sub.type, sub.subjects.size),
                            details = headerDetails(sub.subjects),
                            collapsed = subCollapsed,
                            isTopLevel = false,
                        ),
                    )
                    if (!subCollapsed) {
                        for (subject in sub.subjects) {
                            entries.add(ResultEntry.SubjectEntry(subject))
                        }
                    }
                }
            }
        }
    }
    return entries
}

private fun typeHeaderTitle(type: SubjectType, count: Int): String {
    return if (count == 1) {
        "1 " + type.getDescription()
    }
    else {
        "$count " + type.getDescriptionPlural()
    }
}

/**
 * The title of a top-level header for the given sort order, mirroring the
 * legacy LevelHeaderItem / AvailableAtHeaderItem / SrsStageHeaderItem titles.
 */
private fun topLevelHeaderTitle(sortOrder: SearchSortOrder, subject: Subject, searchTime: Long): String {
    return when (sortOrder) {
        SearchSortOrder.LEVEL_TYPE -> "Level ${subject.getLevel()}"
        SearchSortOrder.AVAILABLE_AT_TYPE -> {
            val date = subject.getAvailableAt()
            when {
                date == 0L -> "No review scheduled"
                date <= searchTime -> "Review available now"
                else -> TextUtil.formatTimestampForDisplay(date)
            }
        }
        SearchSortOrder.STAGE_TYPE -> runCatching { subject.getSrsStage().getName() }.getOrDefault("")
        else -> ""
    }
}

/**
 * The header details line with SRS stage counts, mirroring the legacy
 * HeaderItemViewHolder.bindCommon().
 */
private fun headerDetails(subjects: List<Subject>): String {
    var locked = 0
    var initial = 0
    var prePassed = 0
    var passed = 0
    var burned = 0
    for (subject in subjects) {
        val stage = runCatching { subject.getSrsStage() }.getOrNull()
        when {
            stage == null -> prePassed++
            stage.isCompleted() -> burned++
            stage.isInitial() -> initial++
            stage.isLocked() -> locked++
            subject.isPassed() -> passed++
            else -> prePassed++
        }
    }
    val parts = mutableListOf<String>()
    if (locked > 0) {
        parts.add("$locked locked")
    }
    if (initial > 0) {
        parts.add("$initial not started")
    }
    if (prePassed > 0) {
        parts.add("$prePassed in progress")
    }
    if (passed > 0) {
        parts.add("$passed passed")
    }
    if (burned > 0) {
        parts.add("$burned burned")
    }
    return parts.joinToString(", ")
}

@Composable
private fun ResultHeaderRow(
    title: String,
    details: String,
    collapsed: Boolean,
    isTopLevel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isTopLevel) {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Build the advanced search parameters JSON for an SRS breakdown bucket click,
 * mirroring the legacy SrsBreakDownView click handlers.
 */
fun srsBucketSearchParameters(bucket: Int): String {
    val parameters = AdvancedSearchParameters()
    when (bucket) {
        0 -> for (i in 0 until SrsSystemRepository.getMaxNumApprenticeStages()) {
            parameters.srsStages.add("prepass:$i")
        }
        1 -> for (i in 0 until SrsSystemRepository.getMaxNumGuruStages()) {
            parameters.srsStages.add("pass:$i")
        }
        2 -> parameters.srsStages.add("master")
        3 -> parameters.srsStages.add("enlightened")
        4 -> parameters.srsStages.add("burned")
    }
    return Converters.getObjectMapper().writeValueAsString(parameters)
}

@Composable
private fun SubjectResultRow(
    subject: Subject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(subject.getButtonBackgroundColor())),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = subject.getCharacters() ?: subject.getSlug() ?: "",
                color = Color.White,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = subject.getOneMeaning(),
                style = MaterialTheme.typography.bodyLarge,
            )
            val reading = subject.getOneReading()
            if (!reading.isNullOrBlank()) {
                Text(
                    text = reading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val stageName = runCatching { subject.getSrsStage().getName() }.getOrNull()
            val subtitle = buildString {
                append("Level ")
                append(subject.getLevel())
                if (stageName != null) {
                    append(" · ")
                    append(stageName)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
