package com.lazy_durtles.wk.ui.screens.settings

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lazy_durtles.wk.util.FontStorageUtil
import com.lazy_durtles.wk.util.ObjectSupport.isEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Importer for TTF/OTF font files used for quiz questions: pick a font file,
 * import it, preview it, or delete it. A port of the legacy FontImportActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontImportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var importCandidate by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var sampleCandidate by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<String?>(null) }
    var sampleLoadFailed by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            names = withContext(Dispatchers.IO) { FontStorageUtil.getNames() }
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    fun importFile(uri: Uri, fileName: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        FontStorageUtil.importFontFile(stream, fileName)
                    }
                }
                catch (ignored: Exception) {
                    // The legacy activity ignores import IO errors and still
                    // reports the file as imported.
                }
                FontStorageUtil.flushCache(fileName)
            }
            refresh()
            Toast.makeText(context, "File imported", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = resolveFileName(context, uri)
            if (fileName != null) {
                if (FontStorageUtil.hasFontFile(fileName)) {
                    importCandidate = uri to fileName
                }
                else {
                    importFile(uri, fileName)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Import fonts") },
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
                text = "Click import to select a TTF font file to import.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                Text(text = "Import")
            }
            for (name in names) {
                FontImportRow(
                    name = name,
                    onShowSample = {
                        sampleCandidate = name
                        sampleLoadFailed = FontStorageUtil.getTypefaceConfiguration(name)?.typeface == null
                    },
                    onDelete = { deleteCandidate = name },
                )
            }
        }
    }

    importCandidate?.let { (uri, fileName) ->
        AlertDialog(
            onDismissRequest = { importCandidate = null },
            title = { Text(text = "Overwrite file?") },
            text = {
                Text(text = "A file named '$fileName' already exists. Do you want to overwrite it?")
            },
            confirmButton = {
                TextButton(onClick = {
                    importCandidate = null
                    importFile(uri, fileName)
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { importCandidate = null }) {
                    Text(text = "No")
                }
            },
        )
    }

    deleteCandidate?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(text = "Delete file?") },
            text = {
                Text(text = "Are you sure you want to delete '$name'")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            FontStorageUtil.deleteFont(name)
                            FontStorageUtil.flushCache(name)
                        }
                        refresh()
                    }
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(text = "No")
                }
            },
        )
    }

    if (sampleCandidate != null) {
        if (sampleLoadFailed) {
            AlertDialog(
                onDismissRequest = { sampleCandidate = null },
                title = { Text(text = "Unable to load font") },
                text = {
                    Text(text = "There was an error attempting to load this font. You will not be able to use it in quizzes.")
                },
                confirmButton = {
                    TextButton(onClick = { sampleCandidate = null }) {
                        Text(text = "OK")
                    }
                },
            )
        }
        else {
            val typeface = sampleCandidate?.let { FontStorageUtil.getTypefaceConfiguration(it)?.typeface }
            AlertDialog(
                onDismissRequest = { sampleCandidate = null },
                title = { Text(text = "Font sample") },
                text = {
                    Text(
                        text = "日本語の書体",
                        fontSize = 36.sp,
                        fontFamily = typeface?.let { FontFamily(it) },
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { sampleCandidate = null }) {
                        Text(text = "OK")
                    }
                },
            )
        }
    }
}

/**
 * One row in the imported fonts table: file name, sample preview, delete.
 * A port of the legacy FontImportRowView.
 */
@Composable
private fun FontImportRow(
    name: String,
    onShowSample: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onShowSample) {
            Text(text = "Sample")
        }
        OutlinedButton(onClick = onDelete) {
            Text(text = "Delete")
        }
    }
}

/**
 * Best-effort file name for the document behind [uri], matching the legacy
 * resolveFileName(): prefer the content resolver display name, strip any path.
 */
private fun resolveFileName(context: Context, uri: Uri): String? {
    return try {
        var fileName: String? = uri.path
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex("_display_name")
                val displayName = if (displayNameIndex == -1) null else cursor.getString(displayNameIndex)
                if (displayName != null) {
                    fileName = displayName
                }
            }
        }
        fileName?.let {
            val p = it.lastIndexOf('/')
            if (p >= 0) it.substring(p + 1) else it
        }?.takeIf { !isEmpty(it) }
    }
    catch (ignored: Exception) {
        null
    }
}
