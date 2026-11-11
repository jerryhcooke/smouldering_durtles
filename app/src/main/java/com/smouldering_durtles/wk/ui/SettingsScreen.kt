package com.smouldering_durtles.wk.ui
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.smouldering_durtles.wk.BuildConfig
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.Identification
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.api.ApiState
import com.smouldering_durtles.wk.jobs.FlushTasksJob
import com.smouldering_durtles.wk.jobs.ResetDatabaseJob
import com.smouldering_durtles.wk.livedata.LiveApiState
import com.smouldering_durtles.wk.services.JobRunnerService
import com.smouldering_durtles.wk.ui.screens.settings.DataImportExportScreen
import com.smouldering_durtles.wk.ui.screens.settings.DownloadAudioScreen
import com.smouldering_durtles.wk.ui.screens.settings.FontImportScreen
import com.smouldering_durtles.wk.ui.screens.settings.FontSelectionScreen
import com.smouldering_durtles.wk.ui.screens.settings.HtmlNoticeText
import com.smouldering_durtles.wk.ui.screens.settings.KeyboardHelpScreen
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.util.Locale

/**
 * Settings screen. Shows a list of setting categories; each category opens a
 * detail screen with the settings for that group. All values are persisted
 * immediately via GlobalSettings / SharedPreferences.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenThemeCustomization: () -> Unit = {},
    onGoHome: () -> Unit = {},
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    when (selectedCategory) {
        null -> {
            CategoryListScreen(
                onBack = onBack,
                onCategoryClick = { selectedCategory = it },
                modifier = modifier,
            )
        }
        "backup" -> {
            BackupRestoreScreen(
                onBack = { selectedCategory = null },
            )
        }
        "advanced" -> {
            AdvancedSettings(
                onBack = { selectedCategory = null },
            )
        }
        "data_import_export" -> {
            DataImportExportScreen(
                onBack = { selectedCategory = null },
            )
        }
        "font_selection" -> {
            FontSelectionScreen(
                onBack = { selectedCategory = null },
            )
        }
        "font_import" -> {
            FontImportScreen(
                onBack = { selectedCategory = null },
            )
        }
        "download_audio" -> {
            DownloadAudioScreen(
                onBack = { selectedCategory = null },
            )
        }
        "support" -> {
            SupportScreen(
                onBack = { selectedCategory = null },
            )
        }
        "keyboard_help" -> {
            KeyboardHelpScreen(
                onBack = { selectedCategory = null },
                onOpenKeyboardSettings = { selectedCategory = "keyboard" },
            )
        }
        else -> {
            CategoryDetailScreen(
                categoryId = selectedCategory!!,
                onBack = { selectedCategory = null },
                onOpenCategory = { selectedCategory = it },
                onOpenThemeCustomization = onOpenThemeCustomization,
                onGoHome = onGoHome,
            )
        }
    }
}

/**
 * A category of settings shown in the top-level list.
 */
private data class SettingsCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: Any? = null, // ImageVector or drawable resource ID
)

private val CATEGORIES = listOf(
    SettingsCategory("api", "API and audio download", "API token and network rules", Icons.Filled.Settings),
    SettingsCategory("display", "Display", "Theme and question screen appearance", Icons.Filled.Star),
    SettingsCategory("dashboard", "Dashboard", "What to show on the dashboard", Icons.Filled.Home),
    SettingsCategory("subject_info", "Subject Info", "Presentation of subject info details", Icons.Filled.Info),
    SettingsCategory("audio", "Audio", "Autoplay and voice settings", Icons.Filled.PlayArrow),
    SettingsCategory("download_audio", "Download audio", "Download audio files for offline use", Icons.Filled.KeyboardArrowDown),
    SettingsCategory("lesson_review", "Lesson / review", "Session size and question behavior", Icons.AutoMirrored.Filled.List),
    SettingsCategory("keyboard", "Keyboard input", "Input method related settings", Icons.Filled.Edit),
    SettingsCategory("font", "Font", "Font selection and sizes", Icons.Filled.Create),
    SettingsCategory("other", "Notifications and other settings", "Search engines, notifications and miscellaneous", Icons.Filled.Notifications),
    SettingsCategory("experimental", "Experimental (temporary) settings", "Short-term experiments, these will go away", Icons.Filled.Warning),
    SettingsCategory("advanced", "Advanced settings", "For experienced users", Icons.Filled.Build),
    SettingsCategory("other_actions", "Other actions", "Data import/export, resets", Icons.Filled.Delete),
    SettingsCategory("backup", "Backup and restore", "Export settings to a file, or import a backup", R.drawable.ic_backup_24dp),
    SettingsCategory("support", "Support / feedback", "Get help or send feedback", Icons.Filled.Email),
    SettingsCategory("about", "About", "About this app", Icons.Filled.Info),
)

/**
 * The top-level list of setting categories.
 */
@Composable
private fun CategoryListScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SettingsTopBar(title = "Settings", onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                CATEGORIES.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (val icon = category.icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                            is Int -> Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * A detail screen for one category of settings.
 */
@Composable
private fun CategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenThemeCustomization: () -> Unit,
    onGoHome: () -> Unit,
) {
    val title = when (categoryId) {
        "api" -> "API and audio download"
        "display" -> "Display"
        "dashboard" -> "Dashboard"
        "subject_info" -> "Subject Info"
        "audio" -> "Audio"
        "lesson_review" -> "Lesson / review"
        "keyboard" -> "Keyboard input"
        "font" -> "Font"
        "other" -> "Notifications and other settings"
        "experimental" -> "Experimental (temporary) settings"
        "other_actions" -> "Other actions"
        "about" -> "About"
        else -> "Settings"
    }

    Scaffold(
        topBar = {
            SettingsTopBar(title = title, onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            when (categoryId) {
                "api" -> ApiSettings()
                "display" -> DisplaySettings(onOpenThemeCustomization = onOpenThemeCustomization)
                "dashboard" -> DashboardSettings()
                "subject_info" -> SubjectInfoSettings()
                "audio" -> AudioSettings()
                "lesson_review" -> LessonReviewSettings()
                "keyboard" -> KeyboardSettings(onOpenHelp = { onOpenCategory("keyboard_help") })
                "font" -> FontSettings(onOpenCategory = onOpenCategory)
                "other" -> OtherSettings()
                "experimental" -> ExperimentalSettings()
                "other_actions" -> OtherActionsSettings(
                    onOpenDataImportExport = { onOpenCategory("data_import_export") },
                    onOpenDownloadAudio = { onOpenCategory("download_audio") },
                    onGoHome = onGoHome,
                )
                "about" -> AboutSettings()
            }
        }
    }
}

//
// Reusable setting rows.
//

/**
 * A boolean setting rendered as a switch.
 */
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String = "",
) {
    var currentValue by remember(checked) { mutableStateOf(checked) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { currentValue = !currentValue; onCheckedChange(currentValue) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = currentValue,
            onCheckedChange = { currentValue = it; onCheckedChange(it) },
        )
    }
}

/**
 * A numeric setting rendered as a slider.
 */
@Composable
private fun SliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    summary: String = "",
) {
    var sliderValue by remember(value) { mutableStateOf(value.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = "$title: ${sliderValue.toInt()}", style = MaterialTheme.typography.bodyLarge)
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.toInt()) },
            valueRange = valueRange,
        )
    }
}

/**
 * A text setting. Opens a dialog with a text field when tapped.
 */
@Composable
private fun TextRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    summary: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var editing by remember { mutableStateOf(false) }
    var currentValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (summary.isNotEmpty()) summary else currentValue.ifEmpty { "(not set)" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editing) {
        var text by remember { mutableStateOf(currentValue) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentValue = text
                        onValueChange(text)
                        editing = false
                    },
                ) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(text = "Cancel") }
            },
        )
    }
}

/**
 * A setting with a fixed set of options. Opens a dialog with radio buttons when tapped.
 */
@Composable
private fun DropdownRow(
    title: String,
    options: List<Pair<String, String>>,
    value: String,
    onValueChange: (String) -> Unit,
    summarySuffix: String = "",
) {
    var open by remember { mutableStateOf(false) }
    var currentValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        val label = options.firstOrNull { it.first == currentValue }?.second ?: currentValue
        Text(
            text = label + summarySuffix,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(text = title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { (label, optionValue) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentValue = optionValue; onValueChange(optionValue); open = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = optionValue == currentValue,
                                onClick = { currentValue = optionValue; onValueChange(optionValue); open = false },
                            )
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(text = "Cancel") }
            },
        )
    }
}

/**
 * A non-clickable row that shows a block of (HTML) info text, like the legacy
 * InfoPreference.
 */
@Composable
private fun InfoNoticeRow(
    title: String,
    html: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (title.isNotEmpty()) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
        HtmlNoticeText(html = html)
    }
}

/**
 * A clickable row that performs an action, like a plain legacy Preference.
 */
@Composable
private fun ActionRow(
    title: String,
    summary: String = "",
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A subject-selection range: a minimum and maximum value, each of which may be
 * empty. -1 means "not constrained". A port of the legacy NumberRangePreference.
 */
@Composable
private fun NumberRangeRow(
    title: String,
    key: String,
    prefs: SharedPreferences,
    minKey: String = "${key}_min",
) {
    var editing by remember { mutableStateOf(false) }
    val min = prefs.getInt(minKey, -1)
    val max = prefs.getInt("${key}_max", -1)
    val summary = when {
        min == -1 && max == -1 -> "Unlimited"
        min == -1 -> "Max $max"
        max == -1 -> "Min $min"
        else -> String.format(Locale.ROOT, "%d - %d", min, max)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editing) {
        var minText by remember { mutableStateOf(if (min == -1) "" else min.toString()) }
        var maxText by remember { mutableStateOf(if (max == -1) "" else max.toString()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(text = title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose a minimum and maximum value for this category. " +
                            "Leave a field empty to not constrain that value.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(text = "Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(text = "-")
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { maxText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(text = "Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Empty means unconstrained; unparseable input keeps the
                    // old value, like the legacy safe() wrapper does.
                    if (minText.isEmpty()) {
                        prefs.edit().putInt(minKey, -1).apply()
                    }
                    else {
                        minText.toIntOrNull()?.let { prefs.edit().putInt(minKey, it).apply() }
                    }
                    if (maxText.isEmpty()) {
                        prefs.edit().putInt("${key}_max", -1).apply()
                    }
                    else {
                        maxText.toIntOrNull()?.let { prefs.edit().putInt("${key}_max", it).apply() }
                    }
                    editing = false
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(text = "Cancel") }
            },
        )
    }
}

/**
 * A search engine: a name and a URL in which "%s" is replaced by the search
 * query. A port of the legacy TaggedUrlPreference.
 */
@Composable
private fun SearchEngineRow(
    title: String,
    key: String,
    prefs: SharedPreferences,
    defaultUrl: String = "",
    defaultTag: String = "",
) {
    var editing by remember { mutableStateOf(false) }
    val tag = prefs.getString("${key}_tag", defaultTag) ?: defaultTag
    val url = prefs.getString(key, defaultUrl) ?: defaultUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editing) {
        var tagText by remember { mutableStateOf(tag) }
        var urlText by remember { mutableStateOf(url) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(text = title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Search engines are shown when long-pressing on a subject title. " +
                            "In the URL, \"%s\" will be replaced with the search query.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text(text = "Name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text(text = "URL") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("${key}_tag", tagText).apply()
                    prefs.edit().putString(key, urlText).apply()
                    editing = false
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(text = "Cancel") }
            },
        )
    }
}

//
// Category screens.
//

/**
 * API and audio download settings.
 */
@Composable
private fun ApiSettings() {
    val prefs = prefs()
    val apiState by LiveApiState.getInstance().observeAsState(initial = ApiState.UNKNOWN)

    if (apiState != ApiState.OK) {
        InfoNoticeRow(title = "Caution", html = Constants.API_KEY_PERMISSION_NOTICE)
    }
    TextRow(
        title = "V2 API token",
        value = GlobalSettings.Api.getApiKey() ?: "",
        onValueChange = { GlobalSettings.Api.setApiKey(it) },
    )
    DropdownRow(
        title = "Network access rule",
        options = listOf(
            Pair("Always allow sync and downloads", "ALWAYS"),
            Pair("Always allow sync, downloads only on unmetered connections (WiFi)", "DOWNLOAD_WIFI_ONLY"),
            Pair("Allow sync and downloads only on unmetered connections (WiFi)", "WIFI_ONLY"),
        ),
        value = prefs.getString("network_rule", "DOWNLOAD_WIFI_ONLY") ?: "DOWNLOAD_WIFI_ONLY",
        onValueChange = { setPrefString(it, "network_rule", prefs) },
    )
    DropdownRow(
        title = "Audio files location",
        options = remember { audioLocationOptions() },
        value = prefs.getString("audio_location", "Internal") ?: "Internal",
        onValueChange = { setPrefString(it, "audio_location", prefs) },
    )
    SwitchRow(
        title = "Auto-download audio for upcoming reviews",
        checked = prefs.getBoolean("auto_download_audio", true),
        onCheckedChange = { setPrefBoolean(it, "auto_download_audio", prefs) },
    )
    SwitchRow(
        title = "Allow background sync",
        summary = "Caution - this may drain your battery faster than normal",
        checked = prefs.getBoolean("enable_background_sync", false),
        onCheckedChange = { setPrefBoolean(it, "enable_background_sync", prefs) },
    )
    SwitchRow(
        title = "Sync when the app is opened",
        summary = "Triggers sync if the app has been closed for at least 3 min",
        checked = prefs.getBoolean("sync_on_open", false),
        onCheckedChange = { setPrefBoolean(it, "sync_on_open", prefs) },
    )
    TextRow(
        title = "Web site password",
        value = prefs.getString("web_password", "") ?: "",
        summary = "This is only needed for resurrect functionality. Leave blank if you don't need it.",
        onValueChange = { setPrefString(it, "web_password", prefs) },
    )
}

/**
 * The available audio storage locations: internal storage plus the external
 * files dirs, with the package-specific part stripped for display.
 */
private fun audioLocationOptions(): List<Pair<String, String>> {
    val values = com.smouldering_durtles.wk.util.AudioUtil.getLocationValues()
    val labels = com.smouldering_durtles.wk.util.AudioUtil.getLocations(values)
    return values.mapIndexed { index, value -> labels[index] to value }
}

/**
 * Display settings.
 */
@Composable
private fun DisplaySettings(onOpenThemeCustomization: () -> Unit) {
    val prefs = prefs()

    DropdownRow(
        title = "Light Theme",
        options = THEME_OPTIONS,
        value = prefs.getString("theme", "LIGHT") ?: "LIGHT",
        onValueChange = { setPrefString(it, "theme", prefs) },
        summarySuffix = " theme is currently selected",
    )
    DropdownRow(
        title = "Dark Theme",
        options = THEME_OPTIONS,
        value = prefs.getString("nightTheme", "DARK") ?: "DARK",
        onValueChange = { setPrefString(it, "nightTheme", prefs) },
        summarySuffix = " is used in system wide dark mode",
    )
    ActionRow(
        title = "Theme Customisation",
        summary = "Customise colors in the current theme",
        onClick = onOpenThemeCustomization,
    )
    SwitchRow(
        title = "Center caret in quiz answer box",
        checked = prefs.getBoolean("center_caret", false),
        onCheckedChange = { setPrefBoolean(it, "center_caret", prefs) },
    )
    SliderRow(
        title = "Vertical size of the Anki mode buttons",
        value = prefs.getInt("anki_buttons_height", 50),
        valueRange = 10f..250f,
        onValueChange = { setPrefInt(it, "anki_buttons_height", prefs) },
    )
    SliderRow(
        title = "Fix height of main quiz question view",
        value = prefs.getInt("quiz_question_view_height", 0),
        valueRange = 0f..500f,
        onValueChange = { setPrefInt(it, "quiz_question_view_height", prefs) },
        summary = "Set to 0 to not force a fixed size",
    )
    SwitchRow(
        title = "Swap the correct/incorrect Anki mode buttons",
        checked = prefs.getBoolean("swap_anki_buttons", false),
        onCheckedChange = { setPrefBoolean(it, "swap_anki_buttons", prefs) },
    )
    SwitchRow(
        title = "Use slide animations to transition between quiz questions",
        checked = prefs.getBoolean("slide_animations", true),
        onCheckedChange = { setPrefBoolean(it, "slide_animations", prefs) },
    )
    SwitchRow(
        title = "Stretch the question view to fill the screen when keyboard input is active",
        checked = prefs.getBoolean("stretch_quiz_question_view", false),
        onCheckedChange = { setPrefBoolean(it, "stretch_quiz_question_view", prefs) },
    )
}

/**
 * Dashboard settings.
 */
@Composable
private fun DashboardSettings() {
    val prefs = prefs()

    SwitchRow(
        title = "Show lesson/review breakdown",
        checked = prefs.getBoolean("show_lesson_review_breakdown", false),
        onCheckedChange = { setPrefBoolean(it, "show_lesson_review_breakdown", prefs) },
    )
    SwitchRow(
        title = "Show 24h review timeline",
        checked = prefs.getBoolean("show_timeline", true),
        onCheckedChange = { setPrefBoolean(it, "show_timeline", prefs) },
    )
    DropdownRow(
        title = "Timeline chart style",
        options = listOf(
            Pair("SRS Stage", "SRS_STAGE"),
            Pair("Next SRS Stage", "NEXT_SRS_STAGE"),
            Pair("Item type", "ITEM_TYPE"),
        ),
        value = prefs.getString("timeline_chart_style", "SRS_STAGE") ?: "SRS_STAGE",
        onValueChange = { setPrefString(it, "timeline_chart_style", prefs) },
    )
    SwitchRow(
        title = "Show timeline waterfall line",
        summary = "Show cumulative amount of reviews over time in the timeline chart",
        checked = prefs.getBoolean("show_waterfall_line", false),
        onCheckedChange = { setPrefBoolean(it, "show_waterfall_line", prefs) },
    )
    DropdownRow(
        title = "Timeline chart Y-axis grid style",
        options = listOf(
            Pair("Off", "OFF"),
            Pair("Scaled for bars", "FOR_BARS"),
            Pair("Scaled for cumulative review count", "FOR_WATERFALL"),
        ),
        value = prefs.getString("timeline_chart_grid_style", "FOR_BARS") ?: "FOR_BARS",
        onValueChange = { setPrefString(it, "timeline_chart_grid_style", prefs) },
    )
    DropdownRow(
        title = "Timeline chart size",
        options = CHART_SIZE_OPTIONS,
        value = prefs.getString("timeline_chart_size", "24") ?: "24",
        onValueChange = { setPrefString(it, "timeline_chart_size", prefs) },
    )
    DropdownRow(
        title = "Part of timeline chart shown at once",
        options = CHART_SIZE_OPTIONS,
        value = prefs.getString("timeline_chart_size_shown", "24") ?: "24",
        onValueChange = { setPrefString(it, "timeline_chart_size_shown", prefs) },
    )
    SwitchRow(
        title = "Show SRS breakdown",
        checked = prefs.getBoolean("show_srs_breakdown", true),
        onCheckedChange = { setPrefBoolean(it, "show_srs_breakdown", prefs) },
    )
    SwitchRow(
        title = "Show level progression bars",
        checked = prefs.getBoolean("show_level_progression", true),
        onCheckedChange = { setPrefBoolean(it, "show_level_progression", prefs) },
    )
    SwitchRow(
        title = "Show level progression bars for levels higher than your own",
        summary = "This can happen if you unlocked subjects in the past that have since been moved above your level.",
        checked = prefs.getBoolean("show_over_level_progression", false),
        onCheckedChange = { setPrefBoolean(it, "show_over_level_progression", prefs) },
    )
    val limitPreviousLevels = prefs.getBoolean("previous_level_progression_limit", false)
    SwitchRow(
        title = "Limit progression bars for the completed levels to only the most recent levels",
        summary = "This can help if many new items were added to previous levels and you now have all those levels on your dashboard.",
        checked = limitPreviousLevels,
        onCheckedChange = { setPrefBoolean(it, "previous_level_progression_limit", prefs) },
    )
    if (limitPreviousLevels) {
        SliderRow(
            title = "Number of previous levels to show progression bars for",
            value = prefs.getString("previous_level_progression_limit_amount", "1")?.toIntOrNull() ?: 1,
            valueRange = 0f..60f,
            onValueChange = { setPrefString(it.toString(), "previous_level_progression_limit_amount", prefs) },
        )
    }
    SwitchRow(
        title = "Show post-60 progression bar",
        summary = "Can be used at any level, but intended for level 60! Does NOT include a legend, " +
            "so only enable this if you're cool with that.",
        checked = prefs.getBoolean("show_post60_progression", false),
        onCheckedChange = { setPrefBoolean(it, "show_post60_progression", prefs) },
    )
    SwitchRow(
        title = "Show subsections in post-60 progression bar",
        summary = "Show separate bar segments for the different Apprentice and Guru levels",
        checked = prefs.getBoolean("show_post60_subsections", false),
        onCheckedChange = { setPrefBoolean(it, "show_post60_subsections", prefs) },
    )
    SwitchRow(
        title = "Reverse post-60 progression bar",
        checked = prefs.getBoolean("show_post60_reverse", false),
        onCheckedChange = { setPrefBoolean(it, "show_post60_reverse", prefs) },
    )
    SwitchRow(
        title = "Show Jōyō progress",
        checked = prefs.getBoolean("show_joyo_progress", false),
        onCheckedChange = { setPrefBoolean(it, "show_joyo_progress", prefs) },
    )
    SwitchRow(
        title = "Show JLPT progress",
        checked = prefs.getBoolean("show_jlpt_progress", false),
        onCheckedChange = { setPrefBoolean(it, "show_jlpt_progress", prefs) },
    )
    SwitchRow(
        title = "Show recently unlocked items",
        checked = prefs.getBoolean("show_recent_unlocks", true),
        onCheckedChange = { setPrefBoolean(it, "show_recent_unlocks", prefs) },
    )
    SwitchRow(
        title = "Show critical condition items",
        checked = prefs.getBoolean("show_critical_condition", true),
        onCheckedChange = { setPrefBoolean(it, "show_critical_condition", prefs) },
    )
    SwitchRow(
        title = "Show recently burned items",
        checked = prefs.getBoolean("show_burned_items", true),
        onCheckedChange = { setPrefBoolean(it, "show_burned_items", prefs) },
    )
}

/**
 * Subject info settings.
 */
@Composable
private fun SubjectInfoSettings() {
    val prefs = prefs()

    SwitchRow(
        title = "Hide readings in lessons until tapped",
        checked = prefs.getBoolean("hide_lesson_readings", false),
        onCheckedChange = { setPrefBoolean(it, "hide_lesson_readings", prefs) },
    )
    SwitchRow(
        title = "Hide context sentence translations until tapped",
        checked = prefs.getBoolean("hide_sentence_translations", true),
        onCheckedChange = { setPrefBoolean(it, "hide_sentence_translations", prefs) },
    )
    SwitchRow(
        title = "Show pitch information on vocabulary",
        checked = prefs.getBoolean("show_pitch_info", false),
        onCheckedChange = { setPrefBoolean(it, "show_pitch_info", prefs) },
    )
    SwitchRow(
        title = "Swap 'visually similar' and 'used in' in subject info dump",
        checked = prefs.getBoolean("swap_similar_amalgamations", false),
        onCheckedChange = { setPrefBoolean(it, "swap_similar_amalgamations", prefs) },
    )
    SwitchRow(
        title = "Highlight subject tags in mnemonic texts with colours",
        summary = "This replaces tags such as <kanji> with appropriate colouring instead of just making the text bold",
        checked = prefs.getBoolean("highlight_subject_tags", true),
        onCheckedChange = { setPrefBoolean(it, "highlight_subject_tags", prefs) },
    )
    val alwaysHideWkMnemonic = prefs.getBoolean("always_hide_wk_mnemonic", false)
    SwitchRow(
        title = "Always hide WaniKani's own mnemonics",
        checked = alwaysHideWkMnemonic,
        onCheckedChange = { setPrefBoolean(it, "always_hide_wk_mnemonic", prefs) },
    )
    if (!alwaysHideWkMnemonic) {
        SwitchRow(
            title = "Hide WaniKani's meaning mnemonic if a user note is present",
            checked = prefs.getBoolean("hide_meaning_mnemonic", false),
            onCheckedChange = { setPrefBoolean(it, "hide_meaning_mnemonic", prefs) },
        )
        SwitchRow(
            title = "Hide WaniKani's reading mnemonic if a user note is present",
            checked = prefs.getBoolean("hide_reading_mnemonic", false),
            onCheckedChange = { setPrefBoolean(it, "hide_reading_mnemonic", prefs) },
        )
    }
    SwitchRow(
        title = "Show old names and mnemonics for radicals from before the December 2018 content update",
        checked = prefs.getBoolean("show_legacy", false),
        onCheckedChange = { setPrefBoolean(it, "show_legacy", prefs) },
    )
    DropdownRow(
        title = "Show info dump after a correct meaning answer",
        options = INFO_DUMP_OPTIONS,
        value = prefs.getString("meaning_info_dump", "ANSWERS_ONLY") ?: "ANSWERS_ONLY",
        onValueChange = { setPrefString(it, "meaning_info_dump", prefs) },
    )
    DropdownRow(
        title = "Show info dump after a correct reading answer",
        options = INFO_DUMP_OPTIONS,
        value = prefs.getString("reading_info_dump", "ANSWERS_ONLY") ?: "ANSWERS_ONLY",
        onValueChange = { setPrefString(it, "reading_info_dump", prefs) },
    )
    DropdownRow(
        title = "Show info dump after an incorrect meaning answer",
        options = INFO_DUMP_OPTIONS,
        value = prefs.getString("meaning_info_dump_incorrect", "ANSWERS_ONLY") ?: "ANSWERS_ONLY",
        onValueChange = { setPrefString(it, "meaning_info_dump_incorrect", prefs) },
    )
    DropdownRow(
        title = "Show info dump after an incorrect reading answer",
        options = INFO_DUMP_OPTIONS,
        value = prefs.getString("reading_info_dump_incorrect", "ANSWERS_ONLY") ?: "ANSWERS_ONLY",
        onValueChange = { setPrefString(it, "reading_info_dump_incorrect", prefs) },
    )
    SwitchRow(
        title = "Show stroke order diagrams on radicals and kanji",
        checked = prefs.getBoolean("show_stroke_order", false),
        onCheckedChange = { setPrefBoolean(it, "show_stroke_order", prefs) },
    )
    SwitchRow(
        title = "Animate the stroke order diagrams",
        summary = "Tap the diagram to toggle animation",
        checked = prefs.getBoolean("stroke_order_animated", true),
        onCheckedChange = { setPrefBoolean(it, "stroke_order_animated", prefs) },
    )
    SliderRow(
        title = "Size of the stroke order diagrams",
        value = prefs.getInt("stroke_order_size", 250),
        valueRange = 20f..500f,
        onValueChange = { setPrefInt(it, "stroke_order_size", prefs) },
    )
}

/**
 * Audio settings.
 */
@Composable
private fun AudioSettings() {
    val prefs = prefs()

    SwitchRow(
        title = "Autoplay audio during lesson presentation",
        checked = prefs.getBoolean("autoplay_lesson_presentation", true),
        onCheckedChange = { setPrefBoolean(it, "autoplay_lesson_presentation", prefs) },
    )
    SwitchRow(
        title = "Autoplay audio during lessons",
        summary = "When answering a question correctly in a lesson session",
        checked = prefs.getBoolean("autoplay_lessons", true),
        onCheckedChange = { setPrefBoolean(it, "autoplay_lessons", prefs) },
    )
    SwitchRow(
        title = "Autoplay audio during reviews",
        summary = "When answering a question correctly in a review/self-study session",
        checked = prefs.getBoolean("autoplay_reviews", true),
        onCheckedChange = { setPrefBoolean(it, "autoplay_reviews", prefs) },
    )
    SwitchRow(
        title = "Autoplay audio when revealing in Anki mode",
        summary = "When clicking the 'Show answer' button in Anki mode",
        checked = prefs.getBoolean("autoplay_anki_reveal", true),
        onCheckedChange = { setPrefBoolean(it, "autoplay_anki_reveal", prefs) },
    )
    DropdownRow(
        title = "Voice preference",
        options = listOf(
            Pair("Random", "RANDOM"),
            Pair("Masculine", "MALE"),
            Pair("Femminine", "FEMALE"),
            Pair("Alternate between voices", "ALTERNATE"),
        ),
        value = prefs.getString("voice_preference", "RANDOM") ?: "RANDOM",
        onValueChange = { setPrefString(it, "voice_preference", prefs) },
    )
    SwitchRow(
        title = "Request audio focus when playing vocab audio",
        summary = "Asks other player apps to pause or lower their volume when playing vocab audio",
        checked = prefs.getBoolean("use_audio_focus", true),
        onCheckedChange = { setPrefBoolean(it, "use_audio_focus", prefs) },
    )
}

/**
 * Lesson/review settings.
 */
@Composable
private fun LessonReviewSettings() {
    val prefs = prefs()

    TextRow(
        title = "Max. # of lessons per session",
        value = prefs.getString("max_lesson_session_size", "5") ?: "5",
        onValueChange = { setPrefString(it, "max_lesson_session_size", prefs) },
        keyboardType = KeyboardType.Number,
    )
    TextRow(
        title = "Max. # of reviews per session",
        value = prefs.getString("max_review_session_size", "100") ?: "100",
        onValueChange = { setPrefString(it, "max_review_session_size", prefs) },
        keyboardType = KeyboardType.Number,
    )
    TextRow(
        title = "Max. # of self-study items per session",
        value = prefs.getString("max_self_study_session_size", "100") ?: "100",
        onValueChange = { setPrefString(it, "max_self_study_session_size", prefs) },
        keyboardType = KeyboardType.Number,
    )
    SwitchRow(
        title = "Delay processing quiz results",
        summary = "Doesn't process results until the session is finished. Allows unlimited undo during sessions.",
        checked = prefs.getBoolean("delay_result_upload", false),
        onCheckedChange = { setPrefBoolean(it, "delay_result_upload", prefs) },
    )
    SwitchRow(
        title = "Enable lightning mode",
        summary = "Auto-advance to next question on a correct answer",
        checked = prefs.getBoolean("enable_lightning_mode", true),
        onCheckedChange = { setPrefBoolean(it, "enable_lightning_mode", prefs) },
    )
    SwitchRow(
        title = "Enable a check mark / X animation for a correct / incorrect answer",
        checked = prefs.getBoolean("show_answer_toast", true),
        onCheckedChange = { setPrefBoolean(it, "show_answer_toast", prefs) },
    )
    SwitchRow(
        title = "Enable SRS indicator",
        summary = "Show current SRS stage during reviews",
        checked = prefs.getBoolean("enable_srs_indicator", false),
        onCheckedChange = { setPrefBoolean(it, "enable_srs_indicator", prefs) },
    )
    SwitchRow(
        title = "Enable SRS up/downgrade toast",
        summary = "Show a brief notification when an item goes up or down in SRS stage during reviews",
        checked = prefs.getBoolean("enable_srs_toast", true),
        onCheckedChange = { setPrefBoolean(it, "enable_srs_toast", prefs) },
    )
    SwitchRow(
        title = "Enable alternative readings/meanings toast",
        summary = "Show a toast with alternative readings or meanings upon correct answer",
        checked = prefs.getBoolean("enable_alternatives_toast", false),
        onCheckedChange = { setPrefBoolean(it, "enable_alternatives_toast", prefs) },
    )
    SwitchRow(
        title = "Enable haptic feedback on correct answer",
        summary = "The device will give a short buzz to indicate a correct answer",
        checked = prefs.getBoolean("enable_haptic_feedback_success", false),
        onCheckedChange = { setPrefBoolean(it, "enable_haptic_feedback_success", prefs) },
    )
    SwitchRow(
        title = "Enable haptic feedback on incorrect answer",
        summary = "The device will give a long buzz to indicate an incorrect answer",
        checked = prefs.getBoolean("enable_haptic_feedback_failure", false),
        onCheckedChange = { setPrefBoolean(it, "enable_haptic_feedback_failure", prefs) },
    )
    DropdownRow(
        title = "Action for \"close enough\" answer",
        options = listOf(
            Pair("Accept silently", "SILENTLY_ACCEPT"),
            Pair("Accept and show a toast", "ACCEPT_WITH_TOAST"),
            Pair("Accept, show a toast, disable lightning mode for this question", "ACCEPT_WITH_TOAST_NO_LM"),
            Pair("Shake and retry", "SHAKE_AND_RETRY"),
            Pair("Reject", "REJECT"),
        ),
        value = prefs.getString("close_enough_action", "SILENTLY_ACCEPT") ?: "SILENTLY_ACCEPT",
        onValueChange = { setPrefString(it, "close_enough_action", prefs) },
    )
}

/**
 * Keyboard input settings.
 */
@Composable
private fun KeyboardSettings(onOpenHelp: () -> Unit) {
    val prefs = prefs()

    ActionRow(
        title = "Tap here to read some hints for keyboard settings",
        onClick = onOpenHelp,
    )
    SwitchRow(
        title = "Allow keyboard auto-correct for meanings",
        checked = prefs.getBoolean("enable_auto_correct_meaning", false),
        onCheckedChange = { setPrefBoolean(it, "enable_auto_correct_meaning", prefs) },
    )
    SwitchRow(
        title = "Allow keyboard auto-correct for readings",
        checked = prefs.getBoolean("enable_auto_correct_reading", false),
        onCheckedChange = { setPrefBoolean(it, "enable_auto_correct_reading", prefs) },
    )
    SwitchRow(
        title = "Force ASCII keyboard for meanings",
        checked = prefs.getBoolean("force_ascii_meaning", true),
        onCheckedChange = { setPrefBoolean(it, "force_ascii_meaning", prefs) },
    )
    SwitchRow(
        title = "Force ASCII keyboard for readings",
        checked = prefs.getBoolean("force_ascii_reading", false),
        onCheckedChange = { setPrefBoolean(it, "force_ascii_reading", prefs) },
    )
    SwitchRow(
        title = "Force 'visible password' setting for meanings",
        summary = "This may be necessary to prevent auto-suggest on some keyboards",
        checked = prefs.getBoolean("force_visible_password_meaning", false),
        onCheckedChange = { setPrefBoolean(it, "force_visible_password_meaning", prefs) },
    )
    SwitchRow(
        title = "Force 'visible password' setting for readings",
        summary = "This may be necessary to prevent auto-suggest on some keyboards",
        checked = prefs.getBoolean("force_visible_password_reading", false),
        onCheckedChange = { setPrefBoolean(it, "force_visible_password_reading", prefs) },
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SwitchRow(
            title = "Set keyboard language hint to English for meanings",
            summary = "Enable this to suggest a keyboard language for meaning questions",
            checked = prefs.getBoolean("ime_hint_meaning", false),
            onCheckedChange = { setPrefBoolean(it, "ime_hint_meaning", prefs) },
        )
        SwitchRow(
            title = "Set keyboard language hint to Japanese for readings",
            summary = "Enable this to suggest a keyboard language for reading questions",
            checked = prefs.getBoolean("ime_hint_reading", false),
            onCheckedChange = { setPrefBoolean(it, "ime_hint_reading", prefs) },
        )
    }
    SwitchRow(
        title = "Enable 'no personalized learning' option",
        summary = "Disable this to get rid of the annoying incognito icon in Swiftkey",
        checked = prefs.getBoolean("enable_no_learning", true),
        onCheckedChange = { setPrefBoolean(it, "enable_no_learning", prefs) },
    )
    TextRow(
        title = "Delay in seconds to disable the next/submit button after typing an incorrect answer",
        value = prefs.getString("next_button_delay", "0") ?: "0",
        onValueChange = { setPrefString(it, "next_button_delay", prefs) },
        keyboardType = KeyboardType.Decimal,
    )
}

/**
 * Font settings.
 */
@Composable
private fun FontSettings(onOpenCategory: (String) -> Unit) {
    val prefs = prefs()

    ActionRow(
        title = "Select fonts for quiz questions",
        onClick = { onOpenCategory("font_selection") },
    )
    ActionRow(
        title = "Import fonts for quiz questions",
        onClick = { onOpenCategory("font_import") },
    )
    SliderRow(
        title = "Max font size of subject title in lesson presentation",
        value = prefs.getInt("max_font_size_lesson", 128),
        valueRange = 0f..128f,
        onValueChange = { setPrefInt(it, "max_font_size_lesson", prefs) },
        summary = "Set to 0 to hide the title",
    )
    SliderRow(
        title = "Max font size of subject title on the quiz screen",
        value = prefs.getInt("max_font_size_quiz", 28),
        valueRange = 0f..128f,
        onValueChange = { setPrefInt(it, "max_font_size_quiz", prefs) },
        summary = "Set to 0 to hide the title",
    )
    SliderRow(
        title = "Max font size of subject title on the browse screen",
        value = prefs.getInt("max_font_size_browse", 64),
        valueRange = 0f..128f,
        onValueChange = { setPrefInt(it, "max_font_size_browse", prefs) },
        summary = "Set to 0 to hide the title",
    )
    SliderRow(
        title = "Max font size of main quiz question text",
        value = prefs.getInt("max_font_size_quiz_text", 100),
        valueRange = 0f..500f,
        onValueChange = { setPrefInt(it, "max_font_size_quiz_text", prefs) },
        summary = "This is the large question text in the center of the quiz screen",
    )
    SliderRow(
        title = "Font size of answer input text box",
        value = prefs.getInt("font_size_question_edit", 14),
        valueRange = 10f..32f,
        onValueChange = { setPrefInt(it, "font_size_question_edit", prefs) },
    )
    SliderRow(
        title = "Font size of subject info dump",
        value = prefs.getInt("font_size_subject_info", 14),
        valueRange = 10f..32f,
        onValueChange = { setPrefInt(it, "font_size_subject_info", prefs) },
    )
    SliderRow(
        title = "Font size of dashboard subject tables",
        value = prefs.getInt("font_size_live_subject_table", 14),
        valueRange = 10f..32f,
        onValueChange = { setPrefInt(it, "font_size_live_subject_table", prefs) },
        summary = "Critical condition items, recent unlocks, and recently burned",
    )
    SliderRow(
        title = "Font size of Anki mode answers",
        value = prefs.getInt("font_size_anki_answer", 18),
        valueRange = 10f..48f,
        onValueChange = { setPrefInt(it, "font_size_anki_answer", prefs) },
    )
}

/**
 * Other settings: search engines, notifications and miscellaneous.
 */
@Composable
private fun OtherSettings() {
    val prefs = prefs()
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        ActionRow(
            title = "Grant notification permission",
            summary = "Open system settings to allow this app to send notifications",
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent)
            },
        )
    }

    SwitchRow(
        title = "Enable notifications for new reviews",
        checked = prefs.getBoolean("enable_notifications", true),
        onCheckedChange = { setPrefBoolean(it, "enable_notifications", prefs) },
    )
    DropdownRow(
        title = "Notification update frequency",
        options = listOf(
            Pair("Only for new reviews", "ONLY_NEW_REVIEWS"),
            Pair("Once per hour", "ONCE_PER_HOUR"),
            Pair("Continuously", "CONTINUOUSLY"),
        ),
        value = prefs.getString("notification_update_frequency", "ONLY_NEW_REVIEWS") ?: "ONLY_NEW_REVIEWS",
        onValueChange = { setPrefString(it, "notification_update_frequency", prefs) },
    )
    DropdownRow(
        title = "Notification priority",
        options = listOf(
            Pair("Minimum", "MIN"),
            Pair("Low", "LOW"),
            Pair("Default", "DEFAULT"),
            Pair("High", "HIGH"),
            Pair("Maximum", "MAX"),
        ),
        value = prefs.getString("notification_priority", "DEFAULT") ?: "DEFAULT",
        onValueChange = { setPrefString(it, "notification_priority", prefs) },
    )
    DropdownRow(
        title = "Notification category",
        options = listOf(
            Pair("Incoming call", "CALL"),
            Pair("Navigation", "NAVIGATION"),
            Pair("Incoming message", "MESSAGE"),
            Pair("Email", "EMAIL"),
            Pair("Calendar event", "EVENT"),
            Pair("Promotion / Ad", "PROMO"),
            Pair("Alarm", "ALARM"),
            Pair("Progress", "PROGRESS"),
            Pair("Social media", "SOCIAL"),
            Pair("Error", "ERROR"),
            Pair("Media transport", "TRANSPORT"),
            Pair("System", "SYSTEM"),
            Pair("Service", "SERVICE"),
            Pair("Reminder", "REMINDER"),
            Pair("Recommendation (default)", "RECOMMENDATION"),
            Pair("Status", "STATUS"),
        ),
        value = prefs.getString("notification_category", "RECOMMENDATION") ?: "RECOMMENDATION",
        onValueChange = { setPrefString(it, "notification_category", prefs) },
    )
    SearchEngineRow(
        title = "Search engine 1",
        key = "search_engine_1",
        prefs = prefs,
        defaultUrl = "https://ja.wiktionary.org/w/index.php?search=%s",
        defaultTag = "Wiktionary",
    )
    SearchEngineRow(
        title = "Search engine 2",
        key = "search_engine_2",
        prefs = prefs,
        defaultUrl = "https://jisho.org/search/%s",
        defaultTag = "Jisho",
    )
    SearchEngineRow(title = "Search engine 3", key = "search_engine_3", prefs = prefs)
    SearchEngineRow(title = "Search engine 4", key = "search_engine_4", prefs = prefs)
    SearchEngineRow(title = "Search engine 5", key = "search_engine_5", prefs = prefs)
    SwitchRow(
        title = "Enable star ratings for subjects",
        summary = "If enabled you can give star ratings to subjects and use those ratings as search filters",
        checked = prefs.getBoolean("enable_star_ratings", false),
        onCheckedChange = { setPrefBoolean(it, "enable_star_ratings", prefs) },
    )
    SwitchRow(
        title = "Show on'yomi readings in Katakana",
        checked = prefs.getBoolean("show_onyomi_in_katakana", false),
        onCheckedChange = { setPrefBoolean(it, "show_onyomi_in_katakana", prefs) },
    )
    SwitchRow(
        title = "Require on'yomi answers in Katakana",
        checked = prefs.getBoolean("require_onyomi_in_katakana", false),
        onCheckedChange = { setPrefBoolean(it, "require_onyomi_in_katakana", prefs) },
    )
}

/**
 * Experimental (temporary) settings.
 */
@Composable
private fun ExperimentalSettings() {
    val prefs = prefs()

    InfoNoticeRow(title = "Caution", html = Constants.EXPERIMENTAL_PREFERENCE_STATUS_NOTICE)
    DropdownRow(
        title = "Subject card layout in search results",
        options = SUBJECT_CARD_LAYOUT_OPTIONS,
        value = prefs.getString("exp_subject_card_layout_search", "NORMAL") ?: "NORMAL",
        onValueChange = { setPrefString(it, "exp_subject_card_layout_search", prefs) },
    )
    DropdownRow(
        title = "Subject card layout in other places",
        options = SUBJECT_CARD_LAYOUT_OPTIONS,
        value = prefs.getString("exp_subject_card_layout_grid", "NORMAL") ?: "NORMAL",
        onValueChange = { setPrefString(it, "exp_subject_card_layout_grid", prefs) },
    )
}

/**
 * Other actions: data import/export and destructive reset actions.
 */
@Composable
private fun OtherActionsSettings(
    onOpenDataImportExport: () -> Unit,
    onOpenDownloadAudio: () -> Unit,
    onGoHome: () -> Unit,
) {
    val context = LocalContext.current
    var resetTutorialsOpen by remember { mutableStateOf(false) }
    var resetDatabaseOpen by remember { mutableStateOf(false) }
    var flushTasksOpen by remember { mutableStateOf(false) }

    ActionRow(
        title = "Data import / export",
        onClick = onOpenDataImportExport,
    )
    ActionRow(
        title = "Download audio",
        summary = "Download audio files for offline use",
        onClick = onOpenDownloadAudio,
    )
    ActionRow(
        title = "Flush background tasks",
        summary = "Clear stuck background tasks",
        onClick = { flushTasksOpen = true },
    )
    ActionRow(
        title = "Reset UI confirmations and tutorials",
        onClick = { resetTutorialsOpen = true },
    )
    ActionRow(
        title = "Reset database",
        summary = "Clear all data in the app",
        onClick = { resetDatabaseOpen = true },
    )

    if (flushTasksOpen) {
        AlertDialog(
            onDismissRequest = { flushTasksOpen = false },
            title = { Text(text = "Flush background tasks?") },
            text = { Text(text = Constants.FLUSH_TASKS_WARNING.replace("<b>", "").replace("</b>", "")) },
            confirmButton = {
                TextButton(onClick = {
                    flushTasksOpen = false
                    JobRunnerService.schedule(FlushTasksJob::class.java, "")
                    Toast.makeText(context, "Background tasks flushed!", Toast.LENGTH_LONG).show()
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { flushTasksOpen = false }) {
                    Text(text = "No")
                }
            },
        )
    }

    if (resetTutorialsOpen) {
        AlertDialog(
            onDismissRequest = { resetTutorialsOpen = false },
            title = { Text(text = "Reset confirmations and tutorials?") },
            text = { Text(text = Constants.RESET_TUTORIALS_WARNING) },
            confirmButton = {
                TextButton(onClick = {
                    resetTutorialsOpen = false
                    GlobalSettings.resetConfirmationsAndTutorials()
                    Toast.makeText(context, "Confirmations and tutorials reset", Toast.LENGTH_LONG).show()
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTutorialsOpen = false }) {
                    Text(text = "No")
                }
            },
        )
    }

    if (resetDatabaseOpen) {
        AlertDialog(
            onDismissRequest = { resetDatabaseOpen = false },
            title = { Text(text = "Reset database?") },
            text = { HtmlNoticeText(html = Constants.RESET_DATABASE_WARNING) },
            confirmButton = {
                TextButton(onClick = {
                    resetDatabaseOpen = false
                    JobRunnerService.schedule(ResetDatabaseJob::class.java, "")
                    onGoHome()
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetDatabaseOpen = false }) {
                    Text(text = "No")
                }
            },
        )
    }
}

//
// Advanced settings.
//

/**
 * Advanced settings: a global unlock switch with a first-enable warning
 * dialog, and four sub-sections that are only visible when unlocked.
 */
@Composable
private fun AdvancedSettings(onBack: () -> Unit) {
    var subCategory by remember { mutableStateOf<String?>(null) }

    when (subCategory) {
        "advanced_lesson_settings" -> AdvancedLessonSettings(onBack = { subCategory = null })
        "advanced_review_settings" -> AdvancedReviewSettings(onBack = { subCategory = null })
        "advanced_self_study_settings" -> AdvancedSelfStudySettings(onBack = { subCategory = null })
        "advanced_other_settings" -> AdvancedOtherSettings(onBack = { subCategory = null })
        else -> AdvancedMainScreen(onBack = onBack, onOpenSub = { subCategory = it })
    }
}

/**
 * The main advanced settings screen: the unlock switch plus links to the
 * four advanced sub-sections.
 */
@Composable
private fun AdvancedMainScreen(
    onBack: () -> Unit,
    onOpenSub: (String) -> Unit,
) {
    var advancedEnabled by remember { mutableStateOf(GlobalSettings.getAdvancedEnabled()) }
    var warningOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsTopBar(title = "Advanced settings", onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SwitchRow(
                title = "Enable advanced settings",
                summary = "Only for experienced users!",
                checked = advancedEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !advancedEnabled) {
                        // First enable shows a confirmation dialog; the switch
                        // only flips once the user confirms.
                        warningOpen = true
                    }
                    else {
                        advancedEnabled = enabled
                        GlobalSettings.setAdvancedEnabled(enabled)
                    }
                },
            )

            if (advancedEnabled) {
                ActionRow(title = "Lesson advanced settings", onClick = { onOpenSub("advanced_lesson_settings") })
                ActionRow(title = "Review advanced settings", onClick = { onOpenSub("advanced_review_settings") })
                ActionRow(title = "Self-study advanced settings", onClick = { onOpenSub("advanced_self_study_settings") })
                ActionRow(title = "Other advanced settings", onClick = { onOpenSub("advanced_other_settings") })
            }
        }
    }

    if (warningOpen) {
        AlertDialog(
            onDismissRequest = { warningOpen = false },
            title = { Text(text = "Enable advanced settings?") },
            text = { HtmlNoticeText(html = Constants.ENABLE_ADVANCED_WARNING) },
            confirmButton = {
                TextButton(onClick = {
                    warningOpen = false
                    advancedEnabled = true
                    GlobalSettings.setAdvancedEnabled(true)
                }) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    warningOpen = false
                    advancedEnabled = false
                    GlobalSettings.setAdvancedEnabled(false)
                }) {
                    Text(text = "No")
                }
            },
        )
    }
}

/**
 * A generic sub-screen of the advanced settings.
 */
@Composable
private fun AdvancedSubScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            SettingsTopBar(title = title, onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

/**
 * Lesson advanced settings.
 */
@Composable
private fun AdvancedLessonSettings(onBack: () -> Unit) {
    val prefs = prefs()

    AdvancedSubScreen(title = "Lesson advanced settings", onBack = onBack) {
        DropdownRow(
            title = "Order",
            options = LESSON_ORDER_OPTIONS,
            value = prefs.getString("lesson_order", "LEVEL_THEN_TYPE") ?: "LEVEL_THEN_TYPE",
            onValueChange = { setPrefString(it, "lesson_order", prefs) },
        )
        SwitchRow(
            title = "Reverse order",
            checked = prefs.getBoolean("lesson_order_reversed", false),
            onCheckedChange = { setPrefBoolean(it, "lesson_order_reversed", prefs) },
        )
        DropdownRow(
            title = "Priority",
            options = SESSION_PRIORITY_OPTIONS,
            value = prefs.getString("lesson_order_priority", "NONE") ?: "NONE",
            onValueChange = { setPrefString(it, "lesson_order_priority", prefs) },
        )
        SwitchRow(
            title = "Shuffle after selecting items",
            summary = "Items are selected for the session according to ordering settings, " +
                "but after that they are shuffled randomly.",
            checked = prefs.getBoolean("lesson_shuffle_after_selection", false),
            onCheckedChange = { setPrefBoolean(it, "lesson_shuffle_after_selection", prefs) },
        )
        SwitchRow(
            title = "Reading/meaning back-to-back",
            checked = prefs.getBoolean("lesson_back_to_back", false),
            onCheckedChange = { setPrefBoolean(it, "lesson_back_to_back", prefs) },
        )
        SwitchRow(
            title = "Reading before meaning",
            checked = prefs.getBoolean("lesson_reading_first", false),
            onCheckedChange = { setPrefBoolean(it, "lesson_reading_first", prefs) },
        )
        SwitchRow(
            title = "Meaning before reading",
            checked = prefs.getBoolean("lesson_meaning_first", false),
            onCheckedChange = { setPrefBoolean(it, "lesson_meaning_first", prefs) },
        )
        SwitchRow(
            title = "Anki mode for meanings",
            checked = prefs.getBoolean("anki_mode_lesson_meaning", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_lesson_meaning", prefs) },
        )
        SwitchRow(
            title = "Anki mode for readings",
            checked = prefs.getBoolean("anki_mode_lesson_reading", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_lesson_reading", prefs) },
        )
        InfoNoticeRow(title = "", html = Constants.SUBJECT_SELECTION_NOTICE)
        SubjectRangeRows(prefix = "lesson_subjects", prefs = prefs)
    }
}

/**
 * Review advanced settings.
 */
@Composable
private fun AdvancedReviewSettings(onBack: () -> Unit) {
    val prefs = prefs()

    AdvancedSubScreen(title = "Review advanced settings", onBack = onBack) {
        DropdownRow(
            title = "Order",
            options = REVIEW_ORDER_OPTIONS,
            value = prefs.getString("review_order", "SHUFFLE") ?: "SHUFFLE",
            onValueChange = { setPrefString(it, "review_order", prefs) },
        )
        SwitchRow(
            title = "Reverse order",
            checked = prefs.getBoolean("review_order_reversed", false),
            onCheckedChange = { setPrefBoolean(it, "review_order_reversed", prefs) },
        )
        SwitchRow(
            title = "Overdue items first",
            checked = prefs.getBoolean("review_order_overdue_first", false),
            onCheckedChange = { setPrefBoolean(it, "review_order_overdue_first", prefs) },
        )
        DropdownRow(
            title = "Priority",
            options = SESSION_PRIORITY_OPTIONS,
            value = prefs.getString("review_order_priority", "NONE") ?: "NONE",
            onValueChange = { setPrefString(it, "review_order_priority", prefs) },
        )
        SwitchRow(
            title = "Shuffle after selecting items",
            summary = "Items are selected for the session according to ordering settings, " +
                "but after that they are shuffled randomly.",
            checked = prefs.getBoolean("review_shuffle_after_selection", false),
            onCheckedChange = { setPrefBoolean(it, "review_shuffle_after_selection", prefs) },
        )
        SwitchRow(
            title = "Reading/meaning back-to-back",
            checked = prefs.getBoolean("review_back_to_back", false),
            onCheckedChange = { setPrefBoolean(it, "review_back_to_back", prefs) },
        )
        SwitchRow(
            title = "Reading before meaning",
            checked = prefs.getBoolean("review_reading_first", false),
            onCheckedChange = { setPrefBoolean(it, "review_reading_first", prefs) },
        )
        SwitchRow(
            title = "Meaning before reading",
            checked = prefs.getBoolean("review_meaning_first", false),
            onCheckedChange = { setPrefBoolean(it, "review_meaning_first", prefs) },
        )
        SwitchRow(
            title = "Anki mode for meanings",
            checked = prefs.getBoolean("anki_mode_review_meaning", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_review_meaning", prefs) },
        )
        SwitchRow(
            title = "Anki mode for readings",
            checked = prefs.getBoolean("anki_mode_review_reading", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_review_reading", prefs) },
        )
        SwitchRow(
            title = "Randomise inflections in review",
            summary = "Works best with Anki mode. Random inflections will be added to adjectives and verbs. " +
                "Note that answers and audio will use only dictionary forms.",
            checked = prefs.getBoolean("review_randomize_inflections", false),
            onCheckedChange = { setPrefBoolean(it, "review_randomize_inflections", prefs) },
        )
        InfoNoticeRow(title = "", html = Constants.SUBJECT_SELECTION_NOTICE)
        SubjectRangeRows(prefix = "review_subjects", prefs = prefs)
    }
}

/**
 * Self-study advanced settings.
 */
@Composable
private fun AdvancedSelfStudySettings(onBack: () -> Unit) {
    val prefs = prefs()

    AdvancedSubScreen(title = "Self-study advanced settings", onBack = onBack) {
        DropdownRow(
            title = "Order",
            options = REVIEW_ORDER_OPTIONS,
            value = prefs.getString("self_study_order", "SHUFFLE") ?: "SHUFFLE",
            onValueChange = { setPrefString(it, "self_study_order", prefs) },
        )
        SwitchRow(
            title = "Reverse order",
            checked = prefs.getBoolean("self_study_order_reversed", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_order_reversed", prefs) },
        )
        SwitchRow(
            title = "Overdue items first",
            checked = prefs.getBoolean("self_study_order_overdue_first", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_order_overdue_first", prefs) },
        )
        DropdownRow(
            title = "Priority",
            options = SESSION_PRIORITY_OPTIONS,
            value = prefs.getString("self_study_order_priority", "NONE") ?: "NONE",
            onValueChange = { setPrefString(it, "self_study_order_priority", prefs) },
        )
        SwitchRow(
            title = "Shuffle after selecting items",
            summary = "Items are selected for the session according to ordering settings, " +
                "but after that they are shuffled randomly.",
            checked = prefs.getBoolean("self_study_shuffle_after_selection", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_shuffle_after_selection", prefs) },
        )
        SwitchRow(
            title = "Reading/meaning back-to-back",
            checked = prefs.getBoolean("self_study_back_to_back", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_back_to_back", prefs) },
        )
        SwitchRow(
            title = "Reading before meaning",
            checked = prefs.getBoolean("self_study_reading_first", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_reading_first", prefs) },
        )
        SwitchRow(
            title = "Meaning before reading",
            checked = prefs.getBoolean("self_study_meaning_first", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_meaning_first", prefs) },
        )
        SwitchRow(
            title = "Anki mode for meanings",
            checked = prefs.getBoolean("anki_mode_self_study_meaning", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_self_study_meaning", prefs) },
        )
        SwitchRow(
            title = "Anki mode for readings",
            checked = prefs.getBoolean("anki_mode_self_study_reading", false),
            onCheckedChange = { setPrefBoolean(it, "anki_mode_self_study_reading", prefs) },
        )
        SwitchRow(
            title = "Randomize inflections in self-study",
            summary = "Works best with Anki mode. Random inflections will be added to adjectives and verbs. " +
                "Note that answers and audio will use only dictionary forms.",
            checked = prefs.getBoolean("self_study_randomize_inflections", false),
            onCheckedChange = { setPrefBoolean(it, "self_study_randomize_inflections", prefs) },
        )
        InfoNoticeRow(title = "", html = Constants.SUBJECT_SELECTION_NOTICE)
        SubjectRangeRows(prefix = "self_study_subjects", prefs = prefs)
    }
}

/**
 * Other advanced settings.
 */
@Composable
private fun AdvancedOtherSettings(onBack: () -> Unit) {
    val prefs = prefs()

    AdvancedSubScreen(title = "Other advanced settings", onBack = onBack) {
        TextRow(
            title = "Overdue threshold percentage",
            value = prefs.getString("overdue_threshold", "20") ?: "20",
            onValueChange = { setPrefString(it, "overdue_threshold", prefs) },
            keyboardType = KeyboardType.Number,
        )
        SwitchRow(
            title = "Quiz on/kun'yomi separately",
            checked = prefs.getBoolean("kanji_mode_onkun", false),
            onCheckedChange = { setPrefBoolean(it, "kanji_mode_onkun", prefs) },
        )
        DropdownRow(
            title = "Special button 1 behavior (top right)",
            options = SPECIAL_BUTTON_OPTIONS,
            value = prefs.getString("undo_button", "DISABLE") ?: "DISABLE",
            onValueChange = { setPrefString(it, "undo_button", prefs) },
        )
        DropdownRow(
            title = "Special button 2 behavior (bottom left)",
            options = SPECIAL_BUTTON_OPTIONS,
            value = prefs.getString("ignore_button", "DISABLE") ?: "DISABLE",
            onValueChange = { setPrefString(it, "ignore_button", prefs) },
        )
        DropdownRow(
            title = "Special button 3 behavior (top left)",
            options = SPECIAL_BUTTON_OPTIONS,
            value = prefs.getString("special_button_3", "DISABLE") ?: "DISABLE",
            onValueChange = { setPrefString(it, "special_button_3", prefs) },
        )
        DropdownRow(
            title = "Don't know button behavior",
            options = listOf(
                Pair("Disable", "DISABLE"),
                Pair("Enable", "ENABLE"),
                Pair("Hide if Special button 2 is visible", "ONLY_IF_IGNORE_HIDDEN"),
            ),
            value = prefs.getString("dont_know_button", "ONLY_IF_IGNORE_HIDDEN") ?: "ONLY_IF_IGNORE_HIDDEN",
            onValueChange = { setPrefString(it, "dont_know_button", prefs) },
        )
        SwitchRow(
            title = "Soft-reject on single-kanji vocabulary",
            summary = "If an incorrect reading is an alternative reading for the kanji, do a shake-and-retry instead of failing",
            checked = prefs.getBoolean("shake_on_matching_kanji", true),
            onCheckedChange = { setPrefBoolean(it, "shake_on_matching_kanji", prefs) },
        )
        SwitchRow(
            title = "Show full session log",
            summary = "This may reveal inappropriate hints and answers",
            checked = prefs.getBoolean("full_session_log", false),
            onCheckedChange = { setPrefBoolean(it, "full_session_log", prefs) },
        )
        SwitchRow(
            title = "Indicate type of reading for kanji quiz",
            summary = "For a kanji reading question, indicate if the question requires an on'yomi or kun'yomi answer",
            checked = prefs.getBoolean("indicate_kanji_reading_type", false),
            onCheckedChange = { setPrefBoolean(it, "indicate_kanji_reading_type", prefs) },
        )
    }
}

/**
 * The six subject-selection range rows for one session type.
 */
@Composable
private fun SubjectRangeRows(prefix: String, prefs: SharedPreferences) {
    NumberRangeRow(title = "Current-level items", key = "${prefix}_current", prefs = prefs)
    NumberRangeRow(title = "Earlier-level items", key = "${prefix}_earlier", prefs = prefs)
    NumberRangeRow(title = "Radicals", key = "${prefix}_radical", prefs = prefs)
    NumberRangeRow(title = "Kanji", key = "${prefix}_kanji", prefs = prefs)
    NumberRangeRow(title = "Vocabulary", key = "${prefix}_vocabulary", prefs = prefs)
    if (prefix == "lesson_subjects") {
        // Legacy quirk: GlobalSettings reads the lesson kana-vocabulary min
        // from a key with a capital K, so write that key.
        NumberRangeRow(
            title = "Kana Vocabulary",
            key = "${prefix}_kana_vocabulary",
            prefs = prefs,
            minKey = "lesson_subjects_Kana_vocabulary_min",
        )
    }
    else {
        NumberRangeRow(title = "Kana Vocabulary", key = "${prefix}_kana_vocabulary", prefs = prefs)
    }
}

//
// Helpers for reading and writing settings.
//

/**
 * Get the default shared preferences, where all settings are stored.
 */
@Composable
private fun prefs(): SharedPreferences {
    val context = LocalContext.current
    return remember { PreferenceManager.getDefaultSharedPreferences(context) }
}

private fun setPrefBoolean(value: Boolean, key: String, prefs: SharedPreferences) {
    prefs.edit().putBoolean(key, value).apply()
}

private fun setPrefString(value: String, key: String, prefs: SharedPreferences) {
    prefs.edit().putString(key, value).apply()
}

private fun setPrefInt(value: Int, key: String, prefs: SharedPreferences) {
    prefs.edit().putInt(key, value).apply()
}

//
// Shared option lists.
//

/**
 * The top app bar used by the settings screens, with a back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}

private val THEME_OPTIONS = listOf(
    Pair("Wanikani", "LIGHT"),
    Pair("Wanikani Dark", "DARK_WANIKANI"),
    Pair("Wanikani Black", "BLACK_WANIKANI"),
    Pair("Breeze", "DARK"),
    Pair("Black", "BLACK_BREEZE"),
    Pair("Nord", "NORD_POLAR_NIGHT"),
)

private val INFO_DUMP_OPTIONS = listOf(
    Pair("Show nothing", "NOTHING"),
    Pair("Show only answers", "ANSWERS_ONLY"),
    Pair("Hide information not quizzed yet", "HIDE_UNQUIZZED"),
    Pair("Show all information", "ALL"),
)

private val CHART_SIZE_OPTIONS = listOf(
    Pair("12 hours", "12"),
    Pair("24 hours", "24"),
    Pair("36 hours", "36"),
    Pair("48 hours", "48"),
    Pair("3 days", "72"),
    Pair("5 days", "120"),
    Pair("7 days", "168"),
    Pair("10 days", "240"),
    Pair("14 days", "336"),
)

private val REVIEW_ORDER_OPTIONS = listOf(
    Pair("Shuffle", "SHUFFLE"),
    Pair("Level", "LEVEL"),
    Pair("Type", "TYPE"),
    Pair("SRS stage", "SRS"),
    Pair("Level, Type", "LEVEL_THEN_TYPE"),
    Pair("Level, SRS stage", "LEVEL_THEN_SRS"),
    Pair("Type, Level", "TYPE_THEN_LEVEL"),
    Pair("Type, SRS stage", "TYPE_THEN_SRS"),
    Pair("SRS stage, Type", "SRS_THEN_TYPE"),
    Pair("SRS stage, Level", "SRS_THEN_LEVEL"),
    Pair("Level, Type, SRS stage", "LEVEL_THEN_TYPE_THEN_SRS"),
    Pair("Level, SRS stage, Type", "LEVEL_THEN_SRS_THEN_TYPE"),
    Pair("Type, Level, SRS stage", "TYPE_THEN_LEVEL_THEN_SRS"),
    Pair("Type, SRS stage, Level", "TYPE_THEN_SRS_THEN_LEVEL"),
    Pair("SRS stage, Type, Level", "SRS_THEN_TYPE_THEN_LEVEL"),
    Pair("SRS stage, Level, Type", "SRS_THEN_LEVEL_THEN_TYPE"),
)

private val LESSON_ORDER_OPTIONS = listOf(
    Pair("Shuffle", "SHUFFLE"),
    Pair("Level, Type", "LEVEL_THEN_TYPE"),
    Pair("Radicals, Level, Type", "RADICALS_THEN_LEVEL_THEN_TYPE"),
    Pair("Type, Level", "TYPE_THEN_LEVEL"),
    Pair("Level, then shuffle", "LEVEL_THEN_SHUFFLE"),
    Pair("Type, then shuffle", "TYPE_THEN_SHUFFLE"),
)

private val SESSION_PRIORITY_OPTIONS = listOf(
    Pair("None", "NONE"),
    Pair("Radicals first", "RADICALS_FIRST"),
    Pair("Level-up items first", "LEVEL_UP_FIRST"),
    Pair("Current-level radicals and kanji first", "CURRENT_LEVEL_RADICAL_KANJI_FIRST"),
    Pair("Current-level items first", "CURRENT_LEVEL_FIRST"),
)

private val SPECIAL_BUTTON_OPTIONS = listOf(
    Pair("Disable", "DISABLE"),
    Pair("Ignore (undo and mark correct instead)", "UNDO_AND_MARK_CORRECT"),
    Pair("Undo and retry immediately", "UNDO_AND_RETRY_NOW"),
    Pair("Undo and put back into queue", "UNDO_AND_PUT_BACK"),
    Pair("Skip (put back into queue)", "SKIP"),
    Pair("Toggle lightning mode", "TOGGLE_LIGHTNING_MODE"),
)

private val SUBJECT_CARD_LAYOUT_OPTIONS = listOf(
    Pair("Normal", "NORMAL"),
    Pair("Compact with progression info", "COMPACT"),
    Pair("Compact without progression info", "COMPACT_NO_PROGRESSION"),
)

//
// About.
//

private const val ABOUT_URL_TAG = "URL"

/**
 * The About page, a port of the legacy AboutActivity content from
 * Constants.ABOUT_DOCUMENT.
 */
@Composable
private fun AboutSettings() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The welcome banner is an animated GIF; painterResource() cannot load
        // it (it casts the drawable to BitmapDrawable and crashes on API 28+),
        // so render it through Glide like the legacy AboutActivity does.
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                ImageView(context).apply {
                    adjustViewBounds = true
                    Glide.with(context)
                        .asGif()
                        .load(R.drawable.welcome)
                        .apply(RequestOptions.bitmapTransform(RoundedCornersTransformation(30, 0)))
                        .into(this)
                }
            },
            onRelease = { view ->
                try {
                    Glide.with(view).clear(view)
                }
                catch (ignored: IllegalArgumentException) {
                    // Glide refuses destroyed activities; it cleans up its own
                    // requests in that case anyway.
                }
            },
        )

        Text(
            text = "Welcome to ${Identification.APP_NAME} ${BuildConfig.VERSION_NAME}!",
            style = MaterialTheme.typography.titleMedium,
        )

        aboutParagraph(
            listOf(
                AboutSegment("${Identification.APP_NAME} is an app for "),
                AboutSegment("WaniKani", "https://www.wanikani.com/"),
                AboutSegment(", the kanji learning service created by "),
                AboutSegment("Tofugu", "https://www.tofugu.com/"),
                AboutSegment("."),
            ),
        )

        aboutParagraph(
            listOf(
                AboutSegment("Are you new to WaniKani? Check out their "),
                AboutSegment("knowledge base", "https://knowledge.wanikani.com/"),
                AboutSegment(" for more information."),
            ),
        )

        aboutParagraph(
            listOf(
                AboutSegment("${Identification.APP_NAME} was re-coded by ${Identification.AUTHOR_NAME} (known as Joeni) <"),
                AboutSegment(Identification.AUTHOR_EMAIL, "mailto:${Identification.AUTHOR_EMAIL}"),
                AboutSegment("> based on the original work of Ernst Jan Plugge who developed the original code."),
            ),
        )

        aboutParagraph(
            listOf(
                AboutSegment(
                    "This app will always be free and open source, but if you'd like to support the development, " +
                        "you can do so by ",
                ),
                AboutSegment("donating", "https://ko-fi.com/jerrycooke"),
                AboutSegment("."),
            ),
        )

        aboutParagraph(
            listOf(
                AboutSegment("It has been independently developed, and is not supported by, affiliated with, or endorsed by Tofugu."),
            ),
        )

        aboutParagraph(
            listOf(
                AboutSegment("${Identification.APP_NAME} has been independently developed, and is not supported by, affiliated with, or endorsed by Tofugu."),
            ),
        )

        aboutList(
            "This app uses these Open Source components:",
            listOf(
                listOf(AboutSegment("The Jackson Project", "https://github.com/FasterXML/jackson")),
                listOf(AboutSegment("Lottie", "https://airbnb.io/lottie/")),
                listOf(AboutSegment("JSoup", "https://jsoup.org/")),
                listOf(AboutSegment("OkHttp", "https://square.github.io/okhttp/")),
                listOf(AboutSegment("Pikolo color picker", "https://github.com/Madrapps/Pikolo")),
                listOf(AboutSegment("KanjiVG", "https://github.com/KanjiVG/kanjivg")),
            ),
        )

        aboutList(
            "It was also heavily influenced by:",
            listOf(
                listOf(
                    AboutSegment(
                        "Tsurukame",
                        "https://community.wanikani.com/t/ios-tsurukame-native-app-with-offline-lessons-and-reviews/30635",
                    ),
                ),
                listOf(
                    AboutSegment(
                        "The WaniKani Developer Community",
                        "https://community.wanikani.com/c/wanikani/api-and-third-party-apps",
                    ),
                ),
            ),
        )

        aboutList(
            "The following freely available fonts are included in unmodified form:",
            listOf(
                listOf(
                    AboutSegment(
                        "Sawarabi Mincho and Gothic, (C) mshio at the sawarabi-fonts project on OSDN",
                        "https://sawarabi-fonts.osdn.jp/en/",
                    ),
                    AboutSegment(", covered by the "),
                    AboutSegment("CC Attribution V3.0", "https://creativecommons.org/licenses/by/3.0/"),
                    AboutSegment(" license."),
                ),
                listOf(
                    AboutSegment("M+ P Type-1, (C) the M+ fonts project on OSDN", "https://mplus-fonts.osdn.jp/about-en.html"),
                ),
                listOf(AboutSegment("Kosugi, created by MOTOYA", "https://fonts.google.com/specimen/Kosugi")),
                listOf(AboutSegment("Kosugi Maru, created by MOTOYA", "https://fonts.google.com/specimen/Kosugi+Maru")),
                listOf(AboutSegment("Otsutome", "http://rooms.webcrow.jp/font/index.html")),
                listOf(AboutSegment("851 Gochikakutto", "http://pm85122.onamae.jp/851Gkktt.html")),
            ),
        )
    }
}

/**
 * A text segment of an About paragraph: plain text, or a link.
 */
private data class AboutSegment(val text: String, val url: String? = null)

/**
 * Render a paragraph made of segments; link segments are tappable.
 */
@Composable
private fun aboutParagraph(segments: List<AboutSegment>) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        for (segment in segments) {
            if (segment.url == null) {
                append(segment.text)
            }
            else {
                pushStringAnnotation(ABOUT_URL_TAG, segment.url)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(segment.text)
                }
                pop()
            }
        }
    }
    ClickableText(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = { offset ->
            text.getStringAnnotations(ABOUT_URL_TAG, offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        },
    )
}

/**
 * A bulleted list of linked items with an intro line.
 */
@Composable
private fun aboutList(intro: String, items: List<List<AboutSegment>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = intro, style = MaterialTheme.typography.bodyMedium)
        Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (item in items) {
                aboutParagraph(listOf(AboutSegment("• ")) + item)
            }
        }
    }
}
