/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lazy_durtles.wk.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.lazy_durtles.wk.Constants.MINUTE
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.Identification
import com.lazy_durtles.wk.enums.ActiveTheme
import com.lazy_durtles.wk.jobs.ActivityResumedJob
import com.lazy_durtles.wk.jobs.AutoSyncNowJob
import com.lazy_durtles.wk.jobs.RetryApiErrorJob
import com.lazy_durtles.wk.jobs.SettingChangedJob
import com.lazy_durtles.wk.jobs.SyncNowJob
import com.lazy_durtles.wk.jobs.TickJob
import com.lazy_durtles.wk.livedata.LiveAlertContext
import com.lazy_durtles.wk.livedata.LiveBurnedItems
import com.lazy_durtles.wk.livedata.LiveCriticalCondition
import com.lazy_durtles.wk.livedata.LiveJlptProgress
import com.lazy_durtles.wk.livedata.LiveJoyoProgress
import com.lazy_durtles.wk.livedata.LiveLevelDuration
import com.lazy_durtles.wk.livedata.LiveLevelProgress
import com.lazy_durtles.wk.livedata.LiveRecentUnlocks
import com.lazy_durtles.wk.livedata.LiveReviewTotals
import com.lazy_durtles.wk.livedata.LiveSrsBreakDown
import com.lazy_durtles.wk.livedata.LiveTimeLine
import com.lazy_durtles.wk.model.Session
import com.lazy_durtles.wk.services.BackgroundAlarmReceiver
import com.lazy_durtles.wk.services.BackgroundSyncWorker
import com.lazy_durtles.wk.services.JobRunnerService
import com.lazy_durtles.wk.ui.MainScreen
import com.lazy_durtles.wk.ui.SessionScreen
import com.lazy_durtles.wk.ui.SettingsScreen
import com.lazy_durtles.wk.ui.screens.browse.SearchResultsScreen
import com.lazy_durtles.wk.ui.screens.browse.StudyMaterialsScreen
import com.lazy_durtles.wk.ui.screens.browse.SubjectDetailsScreen
import com.lazy_durtles.wk.ui.screens.browse.srsBucketSearchParameters
import com.lazy_durtles.wk.ui.screens.selfstudy.SelfStudyStartScreen
import com.lazy_durtles.wk.ui.screens.session.SessionLogScreen
import com.lazy_durtles.wk.ui.screens.settings.KeyboardHelpScreen
import com.lazy_durtles.wk.ui.screens.settings.NoApiKeyScreen
import com.lazy_durtles.wk.ui.screens.settings.ThemeCustomizationScreen
import com.lazy_durtles.wk.ui.theme.LazyDurtlesTheme
import com.lazy_durtles.wk.util.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main (and only) activity. Hosts the entire Compose UI as a stack of
 * screens, and takes care of the common lifecycle functionality that used to
 * live in the legacy AbstractActivity: theme application, sync-on-open,
 * resume/pause jobs, the tick timer, the volume stream and the settings
 * change listener.
 */
class MainActivity : ComponentActivity() {
    private val logger = Logger.get(MainActivity::class.java)

    private var tickTimer: Timer? = null
    private var createdTheme: Resources.Theme? = null
    private var creationTheme: ActiveTheme? = null
    private var lastResume = 0L
    private var lastPause = 0L

    /**
     * True while an async interaction (e.g. starting a session) is in flight,
     * to prevent double taps. Replaces the legacy interactionEnabled flag.
     */
    private val busy = AtomicBoolean(false)

    /**
     * Subject ID from a lazy-durtles://subject-info/{id} deep link, to
     * be pushed onto the screen stack by the Compose content.
     */
    private val pendingDeepLinkSubjectId = mutableStateOf<Long?>(null)

    private val sharedPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            JobRunnerService.schedule(SettingChangedJob::class.java, key ?: "")
        }

    /**
     * Launcher for the POST_NOTIFICATIONS permission request (Android 13+).
     * The result itself is irrelevant; asking once is what matters.
     */
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null

    /**
     * Shared text from an ACTION_SEND intent, to be searched once the
     * Compose content is ready.
     */
    private val pendingSharedText = mutableStateOf<String?>(null)

    override fun getTheme(): Resources.Theme {
        // Apply the user's theme style on top of the manifest theme so the
        // window background and theme attributes match the selected theme.
        if (createdTheme == null) {
            createdTheme = super.getTheme()
            createdTheme?.applyStyle(ActiveTheme.getCurrentTheme().getStyleId(), true)
        }
        return requireNotNull(createdTheme)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creationTheme = ActiveTheme.getCurrentTheme()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { GlobalSettings.Tutorials.setNotificationPermissionAsked(true) }
        }
        handleSendIntent(intent)
        pendingDeepLinkSubjectId.value = extractDeepLinkSubjectId(intent)
        setContent {
            LazyDurtlesTheme {
                MainApplicationContent(
                    activity = this,
                    pendingDeepLinkSubjectId = pendingDeepLinkSubjectId,
                    pendingSharedText = pendingSharedText,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendIntent(intent)
        extractDeepLinkSubjectId(intent)?.let { pendingDeepLinkSubjectId.value = it }
    }

    /**
     * A text/plain ACTION_SEND intent opens the keyword search for the shared
     * text, like the legacy BrowseActivity did.
     */
    private fun handleSendIntent(intent: Intent?) {
        if (intent == null || Intent.ACTION_SEND != intent.action || "text/plain" != intent.type) {
            return
        }
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            pendingSharedText.value = sharedText
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            notificationPermissionLauncher != null &&
            !GlobalSettings.Tutorials.getNotificationPermissionAsked() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            GlobalSettings.Tutorials.setNotificationPermissionAsked(true)
            notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (creationTheme != null && creationTheme != ActiveTheme.getCurrentTheme()) {
            recreate()
            return
        }

        if (GlobalSettings.Api.getSyncOnOpen() && (lastPause == 0L && lastResume == 0L
                    || lastResume < lastPause && System.currentTimeMillis() - lastPause > 3 * MINUTE)
        ) {
            JobRunnerService.schedule(AutoSyncNowJob::class.java, "")
        }
        lastResume = System.currentTimeMillis()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(sharedPreferenceListener)

        JobRunnerService.schedule(ActivityResumedJob::class.java, getClassSimpleName())

        tickTimer = Timer()
        tickTimer?.schedule(object : TimerTask() {
            override fun run() {
                JobRunnerService.schedule(TickJob::class.java, "")
            }
        }, MINUTE, MINUTE)

        volumeControlStream = AudioManager.STREAM_MUSIC

        BackgroundAlarmReceiver.scheduleOrCancelAlarm()
        BackgroundSyncWorker.scheduleOrCancelWork()

        runAsync {
            LiveBurnedItems.getInstance().forceUpdate()
            LiveCriticalCondition.getInstance().forceUpdate()
            LiveLevelDuration.getInstance().forceUpdate()
            LiveLevelProgress.getInstance().forceUpdate()
            LiveRecentUnlocks.getInstance().forceUpdate()
            LiveReviewTotals.getInstance().forceUpdate()
            LiveSrsBreakDown.getInstance().forceUpdate()
            LiveTimeLine.getInstance().forceUpdate()
            LiveJoyoProgress.getInstance().forceUpdate()
            LiveJlptProgress.getInstance().forceUpdate()
            LiveAlertContext.getInstance().forceUpdate()
        }
    }

    override fun onPause() {
        lastPause = System.currentTimeMillis()
        tickTimer?.cancel()
        tickTimer = null
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener)
        super.onPause()
    }

    private fun getClassSimpleName(): String = MainActivity::class.java.simpleName

    /**
     * Extract the subject ID from a lazy-durtles://subject-info/{id}
     * deep link, or null if this intent is not such a link.
     */
    private fun extractDeepLinkSubjectId(intent: Intent?): Long? {
        val uri = intent?.data ?: return null
        if (uri.scheme != Identification.APP_URI_SCHEME || uri.authority != "subject-info") {
            return null
        }
        val path = uri.path ?: return null
        val index = path.lastIndexOf('/')
        if (index < 0) {
            return null
        }
        return path.substring(index + 1).toLongOrNull()
    }

    /**
     * Run [action] on a background thread. Exceptions are logged.
     */
    private fun runAsync(action: () -> Unit) {
        Thread {
            try {
                action()
            }
            catch (e: Exception) {
                logger.error(e, "Async action failed")
            }
        }.start()
    }

    /**
     * Handler for the API error retry button.
     */
    internal fun retryApiError() {
        JobRunnerService.schedule(RetryApiErrorJob::class.java, "")
    }

    /**
     * Handler for the manual sync button.
     */
    internal fun syncNow() {
        JobRunnerService.schedule(SyncNowJob::class.java, "")
    }

    /**
     * Start a new lesson or review session on a background thread. The
     * session screen is opened by the caller once the session is ready.
     */
    internal fun startNewSession(type: String, onStarted: () -> Unit) {
        if (!busy.compareAndSet(false, true)) {
            return
        }
        Thread {
            try {
                val timeline = LiveTimeLine.getInstance().get()
                when (type) {
                    "LESSONS" -> if (timeline.hasAvailableLessons()) {
                        Session.getInstance().startNewLessonSession(timeline.availableLessons)
                    }
                    "REVIEWS" -> if (timeline.hasAvailableReviews()) {
                        Session.getInstance().startNewReviewSession(timeline.availableReviews)
                    }
                }
            }
            catch (e: Exception) {
                logger.error(e, "Failed to start session")
            }
            Handler(Looper.getMainLooper()).post {
                busy.set(false)
                onStarted()
            }
        }.start()
    }
}

/**
 * The screens of the app, managed as a back stack by the Compose content.
 */
private sealed class Screen {
    data object Main : Screen()
    data class Session(val type: String) : Screen()
    data object SessionLog : Screen()
    data object Settings : Screen()
    data class SearchResults(
        val searchParametersJson: String,
        val searchType: Int = 2,
        val presetName: String? = null,
    ) : Screen()
    data class SubjectDetails(val subjectId: Long, val subjectIds: List<Long>) : Screen()
    data class SelfStudyStart(val initialSearchParametersJson: String? = null) : Screen()
    data object ThemeCustomization : Screen()
    data class StudyMaterials(val subjectId: Long) : Screen()
    data object KeyboardHelp : Screen()
}

private fun Screen.toJson(): JSONObject {
    val o = JSONObject()
    when (this) {
        is Screen.Main -> o.put("type", "main")
        is Screen.Session -> {
            o.put("type", "session")
            o.put("sessionType", type)
        }
        is Screen.SessionLog -> o.put("type", "sessionLog")
        is Screen.Settings -> o.put("type", "settings")
        is Screen.SearchResults -> {
            o.put("type", "searchResults")
            o.put("json", searchParametersJson)
            o.put("searchType", searchType)
            if (presetName != null) {
                o.put("presetName", presetName)
            }
        }
        is Screen.SubjectDetails -> {
            o.put("type", "subjectDetails")
            o.put("subjectId", subjectId)
            o.put("ids", JSONArray(subjectIds))
        }
        is Screen.SelfStudyStart -> {
            o.put("type", "selfStudyStart")
            if (initialSearchParametersJson != null) {
                o.put("json", initialSearchParametersJson)
            }
        }
        is Screen.ThemeCustomization -> o.put("type", "themeCustomization")
        is Screen.StudyMaterials -> {
            o.put("type", "studyMaterials")
            o.put("subjectId", subjectId)
        }
        is Screen.KeyboardHelp -> o.put("type", "keyboardHelp")
    }
    return o
}

private fun screenFromJson(o: JSONObject): Screen = when (o.getString("type")) {
    "session" -> Screen.Session(o.getString("sessionType"))
    "sessionLog" -> Screen.SessionLog
    "settings" -> Screen.Settings
    "searchResults" -> Screen.SearchResults(
        o.getString("json"),
        o.optInt("searchType", 2),
        o.optString("presetName", "").ifEmpty { null },
    )
    "subjectDetails" -> Screen.SubjectDetails(
        o.getLong("subjectId"),
        o.optJSONArray("ids")?.let { array -> (0 until array.length()).map { array.getLong(it) } } ?: emptyList(),
    )
    "selfStudyStart" -> Screen.SelfStudyStart(o.optString("json", "").ifEmpty { null })
    "themeCustomization" -> Screen.ThemeCustomization
    "studyMaterials" -> Screen.StudyMaterials(o.getLong("subjectId"))
    "keyboardHelp" -> Screen.KeyboardHelp
    else -> Screen.Main
}

/**
 * Saves the screen stack across activity recreation (theme change,
 * configuration change) as a list of JSON strings.
 */
private val ScreenStackSaver = Saver<SnapshotStateList<Screen>, List<String>>(
    save = { stack -> stack.map { it.toJson().toString() } },
    restore = { saved ->
        val stack = mutableStateListOf<Screen>()
        saved.forEach { json -> stack.add(screenFromJson(JSONObject(json))) }
        if (stack.isEmpty()) {
            stack.add(Screen.Main)
        }
        stack
    },
)

/**
 * The Compose content of [MainActivity]: the screen stack and navigation.
 */
@Composable
private fun MainApplicationContent(
    activity: MainActivity,
    pendingDeepLinkSubjectId: MutableState<Long?>,
    pendingSharedText: MutableState<String?>,
) {
    var hasApiKey by remember { mutableStateOf(GlobalSettings.Api.getApiKey() != null) }
    var pendingSearchParameters by remember { mutableStateOf<String?>(null) }
    if (!hasApiKey) {
        NoApiKeyScreen(onApiKeySaved = { hasApiKey = true })
        return
    }

    val stack = rememberSaveable(saver = ScreenStackSaver) { mutableStateListOf(Screen.Main) }
    val currentScreen = stack.last()
    val push: (Screen) -> Unit = { stack.add(it) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val goHome: () -> Unit = {
        stack.clear()
        stack.add(Screen.Main)
    }

    val onBackPressed: () -> Unit = {
        if (stack.size > 1) {
            pop()
        } else {
            goHome()
        }
    }

    LaunchedEffect(pendingDeepLinkSubjectId.value) {
        pendingDeepLinkSubjectId.value?.let { subjectId ->
            stack.add(Screen.SubjectDetails(subjectId, emptyList()))
            pendingDeepLinkSubjectId.value = null
        }
    }

    LaunchedEffect(pendingSharedText.value) {
        pendingSharedText.value?.let { text ->
            stack.add(Screen.SearchResults(searchParametersJson = text, searchType = 1))
            pendingSharedText.value = null
        }
    }

    when (val screen = currentScreen) {
        Screen.Main -> {
            MainScreen(
                onStartLessons = {
                    activity.startNewSession("LESSONS") { push(Screen.Session("LESSONS")) }
                },
                onStartReviews = {
                    activity.startNewSession("REVIEWS") { push(Screen.Session("REVIEWS")) }
                },
                onResumeSession = { push(Screen.Session("RESUME")) },
                onGoToSettings = { push(Screen.Settings) },
                onViewSessionLog = { push(Screen.SessionLog) },
                onRetryApiError = activity::retryApiError,
                onViewKeyboardHelp = { push(Screen.KeyboardHelp) },
                onSyncNow = activity::syncNow,
                onSrsBucketClick = { bucket ->
                    push(
                        Screen.SearchResults(
                            srsBucketSearchParameters(bucket),
                        ),
                    )
                },
                onSubjectClick = { subjectId, subjectIds ->
                    push(Screen.SubjectDetails(subjectId, subjectIds))
                },
                onSearchResults = { json, searchType, presetName ->
                    push(Screen.SearchResults(json, searchType, presetName))
                },
                initialSearchParametersJson = pendingSearchParameters,
            )
        }
        is Screen.Session -> {
            SessionScreen(
                onFinishSession = goHome,
                onViewSubject = { subjectId, subjectIds ->
                    push(Screen.SubjectDetails(subjectId, subjectIds))
                },
            )
        }
        Screen.SessionLog -> {
            SessionLogScreen(
                onSubjectClick = { subjectId, subjectIds ->
                    push(Screen.SubjectDetails(subjectId, subjectIds))
                },
                onBack = pop,
            )
        }
        Screen.Settings -> {
            SettingsScreen(
                onBack = pop,
                onOpenThemeCustomization = { push(Screen.ThemeCustomization) },
                onGoHome = goHome,
            )
        }
        is Screen.SearchResults -> {
            SearchResultsScreen(
                searchParametersJson = screen.searchParametersJson,
                searchType = screen.searchType,
                presetName = screen.presetName,
                onSubjectClick = { subjectId, subjectIds ->
                    push(Screen.SubjectDetails(subjectId, subjectIds))
                },
                onBack = pop,
                onRefineSearch = { json ->
                    pendingSearchParameters = json
                    pop()
                },
                onStartSelfStudy = { json -> push(Screen.SelfStudyStart(json)) },
            )
        }
        is Screen.SubjectDetails -> {
            SubjectDetailsScreen(
                subjectId = screen.subjectId,
                subjectIds = screen.subjectIds,
                onSubjectClick = { subjectId, subjectIds ->
                    push(Screen.SubjectDetails(subjectId, subjectIds))
                },
                onBack = pop,
                onEditStudyMaterials = { push(Screen.StudyMaterials(screen.subjectId)) },
            )
        }
        is Screen.SelfStudyStart -> {
            SelfStudyStartScreen(
                onBack = pop,
                onSessionStarted = { push(Screen.Session("SELF_STUDY")) },
                initialSearchParametersJson = screen.initialSearchParametersJson,
            )
        }
        Screen.ThemeCustomization -> {
            ThemeCustomizationScreen(onBack = pop)
        }
        is Screen.StudyMaterials -> {
            StudyMaterialsScreen(
                subjectId = screen.subjectId,
                onBack = pop,
            )
        }
        Screen.KeyboardHelp -> {
            KeyboardHelpScreen(
                onBack = pop,
                onOpenKeyboardSettings = { push(Screen.Settings) },
            )
        }
    }
}
