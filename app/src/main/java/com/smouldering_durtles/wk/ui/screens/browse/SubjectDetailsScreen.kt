package com.smouldering_durtles.wk.ui.screens.browse

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.WkApplication
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.enums.SubjectInfoDump
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher
import com.smouldering_durtles.wk.livedata.SubjectChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The subject details screen, styled after the legacy SubjectInfoFragment /
 * SubjectInfoView with container type BROWSE: all info sections are shown,
 * with swipe navigation between the subjects in [subjectIds], live updates
 * when the subject changes in the database (e.g. after saving study
 * materials), and a "My notes..." action to edit the study materials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailsScreen(
    subjectId: Long,
    modifier: Modifier = Modifier,
    subjectIds: List<Long> = emptyList(),
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    onEditStudyMaterials: () -> Unit = {},
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    val subjectState = remember { mutableStateOf<Subject?>(null) }
    var relatedById by remember { mutableStateOf<Map<Long, Subject>>(emptyMap()) }
    var reloadTick by remember { mutableIntStateOf(0) }

    // Live updates: observe subject changes (star ratings, study material saves,
    // syncs) like the legacy SubjectChangeWatcher-based views, and re-layout.
    val changeListener = remember(subjectId) {
        object : SubjectChangeListener {
            override fun onSubjectChange(subject: Subject) {
                subjectState.value = subject
                reloadTick++
            }

            override fun isInterestedInSubject(id: Long): Boolean = id == subjectId
        }
    }
    DisposableEffect(changeListener) {
        SubjectChangeWatcher.getInstance().addListener(changeListener)
        onDispose { }
    }

    LaunchedEffect(subjectId, reloadTick) {
        loading = subjectState.value?.getId() != subjectId
        if (subjectState.value?.getId() != subjectId) {
            subjectState.value = null
            relatedById = emptyMap()
        }
        val loaded = withContext(Dispatchers.IO) {
            WkApplication.getDatabase().subjectDao().getById(subjectId)
        }
        subjectState.value = loaded
        loading = false
        // Load component / amalgamation / visually similar subjects for the
        // tappable grids.
        if (loaded != null) {
            val ids = buildList {
                addAll(loaded.getComponentSubjectIds())
                addAll(loaded.getAmalgamationSubjectIds())
                addAll(loaded.getVisuallySimilarSubjectIds())
            }.distinct()
            relatedById = if (ids.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    WkApplication.getDatabase().subjectCollectionsDao().getByIds(ids)
                }.associateBy { it.getId() }
            }
            else {
                emptyMap()
            }
        }
    }

    val s = subjectState.value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = s?.let { it.getInfoTitle("", "").toString() } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (s != null && s.getType().canHaveStudyMaterials()) {
                        SubjectDetailsOverflowMenu(onEditStudyMaterials = onEditStudyMaterials)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = s?.let { Color(it.getBackgroundColor()) } ?: MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            s == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Subject not found")
            }
            else -> SubjectDetailsContent(
                subject = s,
                subjectIds = subjectIds,
                relatedById = relatedById,
                onSubjectClick = onSubjectClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * The top bar overflow menu with the legacy generic options menu's "My notes..."
 * action for the current subject.
 */
@Composable
private fun SubjectDetailsOverflowMenu(onEditStudyMaterials: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("My notes...") },
            onClick = {
                expanded = false
                onEditStudyMaterials()
            },
        )
    }
}

@Composable
private fun SubjectDetailsContent(
    subject: Subject,
    subjectIds: List<Long>,
    relatedById: Map<Long, Subject>,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 100.dp.toPx() }

    fun goToNeighbour(delta: Int) {
        val index = subjectIds.indexOf(subject.getId())
        if (index == -1) {
            return
        }
        val nextIndex = index + delta
        if (nextIndex in subjectIds.indices) {
            onSubjectClick(subjectIds[nextIndex], subjectIds)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Swipe navigation between subjects in the passed list, like the
            // legacy SwipingScrollView: swipe left for the next subject, swipe
            // right for the previous one, with a 100dp threshold.
            .pointerInput(subject.getId(), subjectIds) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            totalDrag < -swipeThresholdPx -> goToNeighbour(+1)
                            totalDrag > swipeThresholdPx -> goToNeighbour(-1)
                        }
                    },
                    onDragCancel = { totalDrag = 0f },
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // BROWSE container type: the full dump is shown, like the legacy
        // SubjectInfoView with container type BROWSE.
        SubjectInfoDumpContent(
            subject = subject,
            relatedById = relatedById,
            initialDump = SubjectInfoDump.ALL,
            question = null,
            onSubjectClick = onSubjectClick,
        )
    }
}
