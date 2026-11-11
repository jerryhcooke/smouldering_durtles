package com.lazy_durtles.wk.ui.screens.settings

import android.graphics.Typeface
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.R
import com.lazy_durtles.wk.util.FontStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One choosable font: static built-in fonts plus fonts imported by the user.
 */
private data class FontChoice(
    val fontId: String,
    val name: String,
    val typeface: Typeface?,
)

/**
 * Font chooser for quiz questions: a list of built-in and imported fonts, each
 * with a sample and a switch to include it in the quiz question rendering.
 * A port of the legacy FontSelectionActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var importedNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        importedNames = withContext(Dispatchers.IO) { FontStorageUtil.getNames() }
    }

    val staticChoices = remember {
        listOf(
            FontChoice("1", "Android default", null),
            FontChoice("2", "Sawarabi Mincho", ResourcesCompat.getFont(context, R.font.sawarabi_mincho_medium)),
            FontChoice("3", "Sawarabi Gothic", ResourcesCompat.getFont(context, R.font.sawarabi_gothic_medium)),
            FontChoice("4", "M⁺ P Type-1", ResourcesCompat.getFont(context, R.font.mplus_1p_regular)),
            FontChoice("5", "Kosugi", ResourcesCompat.getFont(context, R.font.kosugi_regular)),
            FontChoice("6", "Kosugi Maru", ResourcesCompat.getFont(context, R.font.kosugi_maru_regular)),
            FontChoice("7", "Otsutome", ResourcesCompat.getFont(context, R.font.otsutomefont_ver3)),
            FontChoice("8", "851 Gochikakutto", ResourcesCompat.getFont(context, R.font.gochikakutto)),
        )
    }

    val dynamicChoices = importedNames.mapNotNull { name ->
        val configuration = FontStorageUtil.getTypefaceConfiguration(name) ?: return@mapNotNull null
        FontChoice(name, name, configuration.typeface)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Select fonts") },
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
                .padding(horizontal = 8.dp),
        ) {
            for (choice in staticChoices + dynamicChoices) {
                FontSelectionRow(choice)
                HorizontalDivider()
            }
        }
    }
}

/**
 * One row in the font selection list: name, sample text in the font, and a
 * switch. A port of the legacy FontSelectionRowView.
 */
@Composable
private fun FontSelectionRow(choice: FontChoice) {
    var selected by remember(choice.fontId) { mutableStateOf(GlobalSettings.Font.isFontSelected(choice.fontId)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                selected = !selected
                GlobalSettings.Font.setFontSelected(choice.fontId, selected)
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = choice.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "日本語の書体",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 28.sp),
                fontFamily = choice.typeface?.let { FontFamily(it) },
            )
        }
        Switch(
            checked = selected,
            onCheckedChange = {
                selected = it
                GlobalSettings.Font.setFontSelected(choice.fontId, it)
            },
        )
    }
}
