package com.lazy_durtles.wk.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lazy_durtles.wk.BuildConfig
import com.lazy_durtles.wk.WkApplication
import com.lazy_durtles.wk.db.Converters
import com.lazy_durtles.wk.db.model.SearchPreset
import com.lazy_durtles.wk.livedata.LiveSearchPresets
import com.lazy_durtles.wk.model.SearchPresetExport
import com.lazy_durtles.wk.model.StarRatingsExport
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Import and export of data that is not stored on the WK servers and would be
 * lost during a database reset: search presets and star ratings. A port of the
 * legacy DataImportExportActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataImportExportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun shareJson(file: File) {
        val uri = FileProvider.getUriForFile(context, BuildConfig.FILEPROVIDER_AUTHORITY, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun exportSearchPresets() {
        scope.launch {
            busy = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val sharedDir = File(context.filesDir, "shared")
                    if (!sharedDir.exists() && !sharedDir.mkdir()) {
                        return@withContext null
                    }
                    val presets = LiveSearchPresets.getInstance()
                    val data = SearchPresetExport()
                    for (presetName: String in presets.names) {
                        val preset: SearchPreset? = presets.getByName(presetName)
                        data.namedPresets.add(preset)
                    }
                    data.selfStudyPreset = presets.getByName("\u0000SELF_STUDY_DEFAULT")
                    val exportFile = File(sharedDir, "search_presets.json")
                    FileOutputStream(exportFile).use { Converters.getObjectMapper().writeValue(it, data) }
                    exportFile
                }
                if (file != null) {
                    shareJson(file)
                }
            }
            catch (ignored: Exception) {
                // The legacy activity silently ignores export errors here.
            }
            finally {
                busy = false
            }
        }
    }

    fun importStarRatingsResult(uri: Uri) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val data = Converters.getObjectMapper().readValue(stream, StarRatingsExport::class.java)
                        val db = WkApplication.getDatabase()
                        for (id in data.stars1) db.subjectDao().updateStars(id, 1)
                        for (id in data.stars2) db.subjectDao().updateStars(id, 2)
                        for (id in data.stars3) db.subjectDao().updateStars(id, 3)
                        for (id in data.stars4) db.subjectDao().updateStars(id, 4)
                        for (id in data.stars5) db.subjectDao().updateStars(id, 5)
                        true
                    } ?: false
                }
                catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(context, if (ok) "Import finished" else "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun importSearchPresetsResult(uri: Uri) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val data = Converters.getObjectMapper().readValue(stream, SearchPresetExport::class.java)
                        val db = WkApplication.getDatabase()
                        for (preset in data.namedPresets) {
                            db.searchPresetDao().setPreset(preset.name, preset.type, preset.data)
                        }
                        data.selfStudyPreset?.let {
                            db.searchPresetDao().setPreset(it.name, it.type, it.data)
                        }
                        true
                    } ?: false
                }
                catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(context, if (ok) "Import finished" else "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportStarRatings() {
        scope.launch {
            busy = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val sharedDir = File(context.filesDir, "shared")
                    if (!sharedDir.exists() && !sharedDir.mkdir()) {
                        return@withContext null
                    }
                    val db = WkApplication.getDatabase()
                    val data = StarRatingsExport()
                    data.stars1 = db.subjectCollectionsDao().getStarredSubjectIds(1)
                    data.stars2 = db.subjectCollectionsDao().getStarredSubjectIds(2)
                    data.stars3 = db.subjectCollectionsDao().getStarredSubjectIds(3)
                    data.stars4 = db.subjectCollectionsDao().getStarredSubjectIds(4)
                    data.stars5 = db.subjectCollectionsDao().getStarredSubjectIds(5)
                    val exportFile = File(sharedDir, "star_ratings.json")
                    FileOutputStream(exportFile).use { Converters.getObjectMapper().writeValue(it, data) }
                    exportFile
                }
                if (file != null) {
                    shareJson(file)
                }
            }
            catch (ignored: Exception) {
                // The legacy activity silently ignores export errors here.
            }
            finally {
                busy = false
            }
        }
    }

    val importSearchPresetsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importSearchPresetsResult(uri)
        }
    }
    val importStarRatingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importStarRatingsResult(uri)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Data import / export") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Here you can import or export some data that is not stored on the WK servers.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Note that imports will add to or overwrite existing data, but won't remove existing data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = ::exportSearchPresets, enabled = !busy) {
                Text(text = "Export search presets")
            }
            Button(
                onClick = { importSearchPresetsLauncher.launch(arrayOf("application/json")) },
                enabled = !busy,
            ) {
                Text(text = "Import search presets")
            }
            Button(onClick = ::exportStarRatings, enabled = !busy) {
                Text(text = "Export star ratings")
            }
            Button(
                onClick = { importStarRatingsLauncher.launch(arrayOf("application/json")) },
                enabled = !busy,
            ) {
                Text(text = "Import star ratings")
            }
        }
    }
}
