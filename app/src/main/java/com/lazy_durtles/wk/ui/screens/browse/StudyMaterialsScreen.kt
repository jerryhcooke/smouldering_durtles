package com.lazy_durtles.wk.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.Converters
import com.lazy_durtles.wk.db.model.Subject
import com.lazy_durtles.wk.jobs.SaveStudyMaterialJob
import com.lazy_durtles.wk.services.JobRunnerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The study materials editor ("My notes"), ported from the legacy
 * StudyMaterialsActivity: the user's meaning note, reading note and up to ten
 * meaning synonyms with reorder arrows. Saving pushes the material through
 * SaveStudyMaterialJob, like the legacy activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialsScreen(
    subjectId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var subject by remember { mutableStateOf<Subject?>(null) }
    // rememberSaveable keeps the user's edits across configuration changes,
    // like the legacy stateSaved flag.
    var populated by rememberSaveable { mutableStateOf(false) }
    var meaningNote by rememberSaveable { mutableStateOf("") }
    var readingNote by rememberSaveable { mutableStateOf("") }
    var synonyms by rememberSaveable { mutableStateOf(List(10) { "" }) }

    LaunchedEffect(subjectId) {
        val loaded = withContext(Dispatchers.IO) {
            WkApplication.getDatabase().subjectDao().getById(subjectId)
        }
        if (loaded == null || !loaded.getType().canHaveStudyMaterials()) {
            onBack()
            return@LaunchedEffect
        }
        subject = loaded
        if (!populated) {
            populated = true
            meaningNote = loaded.getMeaningNote() ?: ""
            readingNote = loaded.getReadingNote() ?: ""
            val loadedSynonyms = loaded.getMeaningSynonyms().toMutableList()
            while (loadedSynonyms.size < 10) {
                loadedSynonyms.add("")
            }
            synonyms = loadedSynonyms.take(10)
        }
        loading = false
    }

    val scope = rememberCoroutineScope()
    fun saveStudyMaterials() {
        val currentSubject = subject ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                val data = ArrayList<String>()
                data.add(currentSubject.getId().toString())
                data.add(meaningNote)
                data.add(readingNote)
                data.addAll(synonyms)
                val dataString = Converters.getObjectMapper().writeValueAsString(data)
                JobRunnerService.schedule(SaveStudyMaterialJob::class.java, dataString)
            }
            onBack()
        }
    }

    val s = subject
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = s?.let { it.getInfoTitle("My notes for ", ":").toString() } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = s.getInfoTitle("My notes for ", ":").toString(),
                    fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
                )
                Button(
                    onClick = { saveStudyMaterials() },
                    modifier = Modifier.padding(start = 40.dp),
                ) {
                    Text("Save")
                }
                Text(
                    text = "Meaning note:",
                    fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
                )
                OutlinedTextField(
                    value = meaningNote,
                    onValueChange = { meaningNote = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("My meaning note") },
                )
                Text(
                    text = "Reading note:",
                    fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
                )
                OutlinedTextField(
                    value = readingNote,
                    onValueChange = { readingNote = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("My reading note") },
                )
                Text(
                    text = "Synonyms:",
                    fontSize = GlobalSettings.Font.getFontSizeSubjectInfo().sp,
                )
                synonyms.forEachIndexed { index, value ->
                    SynonymRow(
                        index = index,
                        value = value,
                        onValueChange = { newValue ->
                            synonyms = synonyms.toMutableList().also { it[index] = newValue }
                        },
                        onSwap = { otherIndex ->
                            val other = synonyms[otherIndex]
                            synonyms = synonyms.toMutableList().also {
                                it[otherIndex] = synonyms[index]
                                it[index] = other
                            }
                        },
                    )
                }
                Button(
                    onClick = { saveStudyMaterials() },
                    modifier = Modifier.padding(start = 40.dp, top = 16.dp),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * One synonym row, ported from the legacy SynonymRowView: reorder arrows
 * (down, up) followed by the text field. The up arrow is invisible on the
 * first row and the down arrow on the last, keeping their space like the
 * legacy INVISIBLE flag.
 */
@Composable
private fun SynonymRow(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onSwap: (otherIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onSwap(index + 1) },
            enabled = index < 9,
            modifier = Modifier.then(if (index == 9) Modifier.alpha(0f) else Modifier),
        ) {
            Text(text = "⇩")
        }
        IconButton(
            onClick = { onSwap(index - 1) },
            enabled = index > 0,
            modifier = Modifier.then(if (index == 0) Modifier.alpha(0f) else Modifier),
        ) {
            Text(text = "⇧")
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text("Synonym") },
            modifier = Modifier.weight(1f),
        )
    }
}
