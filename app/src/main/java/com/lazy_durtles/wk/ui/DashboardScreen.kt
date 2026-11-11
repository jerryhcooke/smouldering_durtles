package com.lazy_durtles.wk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.GlobalSettings
import com.lazy_durtles.wk.api.ApiState
import com.lazy_durtles.wk.enums.SessionState
import com.lazy_durtles.wk.enums.SubjectType
import com.lazy_durtles.wk.livedata.LiveApiProgress
import com.lazy_durtles.wk.livedata.LiveBurnedItems
import com.lazy_durtles.wk.livedata.LiveCriticalCondition
import com.lazy_durtles.wk.livedata.LiveFirstTimeSetup
import com.lazy_durtles.wk.livedata.LiveJlptProgress
import com.lazy_durtles.wk.livedata.LiveJoyoProgress
import com.lazy_durtles.wk.livedata.LiveLevelDuration
import com.lazy_durtles.wk.livedata.LiveLevelProgress
import com.lazy_durtles.wk.livedata.LiveRecentUnlocks
import com.lazy_durtles.wk.livedata.LiveSrsBreakDown
import com.lazy_durtles.wk.livedata.LiveSessionState
import com.lazy_durtles.wk.livedata.LiveTaskCounts
import com.lazy_durtles.wk.livedata.LiveTimeLine
import com.lazy_durtles.wk.livedata.LiveVacationMode

/**
 * The dashboard screen, showing all the widgets of the legacy dashboard in
 * the same order: status banners, session counts, the upcoming-reviews
 * chart, the SRS breakdown, the level progression, and the subject tables.
 */
@Composable
fun DashboardScreen(
    apiState: ApiState,
    onStartLessons: () -> Unit,
    onStartReviews: () -> Unit,
    onResumeSession: () -> Unit,
    onGoToSettings: () -> Unit,
    onRetryApiError: () -> Unit,
    onViewKeyboardHelp: () -> Unit,
    onSrsBucketClick: (bucket: Int) -> Unit,
    onLevelProgressEntryClick: (level: Int, type: SubjectType) -> Unit,
    onSubjectClick: (subjectId: Long, allIds: LongArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Observe the LiveData instances that drive the widgets.
    // The firstTimeSetup subscription keeps that LiveData active so the
    // dashboard refreshes after first-time setup completes.
    LiveFirstTimeSetup.getInstance().observeAsState()
    val levelDuration = LiveLevelDuration.getInstance().observeAsState()
    val timeLine = LiveTimeLine.getInstance().observeAsState()
    val vacationMode = LiveVacationMode.getInstance().observeAsState(initial = false)
    val sessionState = LiveSessionState.getInstance().observeAsState()
    val srsBreakDown = LiveSrsBreakDown.getInstance().observeAsState(
        initial = LiveSrsBreakDown.getInstance().getDefaultValue())
    val levelProgress = LiveLevelProgress.getInstance().observeAsState(
        initial = LiveLevelProgress.getInstance().getDefaultValue())
    val joyoProgress = LiveJoyoProgress.getInstance().observeAsState(
        initial = LiveJoyoProgress.getInstance().getDefaultValue())
    val jlptProgress = LiveJlptProgress.getInstance().observeAsState(
        initial = LiveJlptProgress.getInstance().getDefaultValue())
    val recentUnlocks = LiveRecentUnlocks.getInstance().observeAsState()
    val criticalCondition = LiveCriticalCondition.getInstance().observeAsState()
    val burnedItems = LiveBurnedItems.getInstance().observeAsState()
    val taskCounts = LiveTaskCounts.getInstance().observeAsState()
    // Observing this triggers recomposition when sync progress changes;
    // the actual values live in static fields on LiveApiProgress.
    LiveApiProgress.getInstance().observeAsState()

    val firstTimeSetup = GlobalSettings.getFirstTimeSetup()
    val userLevel = levelDuration.value?.level ?: 0
    val keyboardHelpDismissed = remember {
        mutableStateOf(GlobalSettings.Tutorials.getKeyboardHelpDismissed())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ApiStatusBanner(
                apiState = apiState,
                onRetry = onRetryApiError,
                onOpenSettings = onGoToSettings,
            )

            if (!keyboardHelpDismissed.value) {
                KeyboardHelpBanner(
                    onView = onViewKeyboardHelp,
                    onDismiss = {
                        GlobalSettings.Tutorials.setKeyboardHelpDismissed(true)
                        keyboardHelpDismissed.value = true
                    },
                )
            }

            if (firstTimeSetup == 0) {
                FirstTimeSetupText()
            }
            else {
                levelDuration.value?.let { duration ->
                    DashboardHeader(
                        level = duration.level,
                        daysAtCurrentLevel = duration.daysAtCurrentLevel,
                        username = duration.username ?: "",
                    )
                }

                timeLine.value?.let { timeline ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AvailableSessionsView(
                            lessons = timeline.numAvailableLessons,
                            reviews = timeline.numAvailableReviews,
                            onStartLessons = onStartLessons,
                            onStartReviews = onStartReviews,
                        )
                        if (GlobalSettings.Dashboard.getShowTimeLine()) {
                            UpcomingReviewsSummaryText(
                                text = upcomingReviewsSummary(
                                    timeLine = timeline,
                                    vacationMode = vacationMode.value,
                                    nowMillis = System.currentTimeMillis(),
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }

                    if (GlobalSettings.Dashboard.getShowTimeLine()) {
                        UpcomingReviewsChart(
                            timeLine = timeline,
                            style = GlobalSettings.Dashboard.getTimeLineChartStyle(),
                            gridStyle = GlobalSettings.Dashboard.getTimeLineChartGridStyle(),
                            showWaterfall = GlobalSettings.Dashboard.getShowWaterfallLine(),
                            numShownBars = GlobalSettings.Dashboard.getTimeLineChartSizeShown(),
                        )
                    }
                }

                SyncStatusText(
                    text = syncStatusText(
                        hasTasks = taskCounts.value?.isEmpty == false,
                        show = LiveApiProgress.getShow(),
                        processed = LiveApiProgress.getNumProcessedEntities(),
                        total = LiveApiProgress.getNumEntities(),
                        entityName = LiveApiProgress.getEntityName(),
                        syncReminder = LiveApiProgress.getInstance().syncReminder,
                    ),
                )

                if (GlobalSettings.Dashboard.getShowLessonReviewBreakdown()) {
                    val lessons = timeLine.value?.availableLessons ?: emptyList()
                    val reviews = timeLine.value?.availableReviews ?: emptyList()
                    if (lessons.isNotEmpty() || reviews.isNotEmpty()) {
                        LessonReviewBreakdownView(
                            lessons = lessons,
                            reviews = reviews,
                            userLevel = userLevel,
                        )
                    }
                }

                if (GlobalSettings.Dashboard.getShowSrsBreakdown()) {
                    SrsBreakDownView(
                        srsBreakDown = srsBreakDown.value,
                        onBucketClick = onSrsBucketClick,
                    )
                }

                if (GlobalSettings.Dashboard.getShowLevelProgression()) {
                    LevelProgressView(
                        levelProgress = levelProgress.value,
                        userLevel = userLevel,
                        onEntryClick = onLevelProgressEntryClick,
                    )
                }

                if (GlobalSettings.Dashboard.getShowPost60Progression()) {
                    Post60ProgressView(
                        srsBreakDown = srsBreakDown.value,
                        showSubsections = GlobalSettings.Dashboard.getShowPost60Subsections(),
                        reversed = GlobalSettings.Dashboard.getShowPost60Reverse(),
                    )
                }

                if (GlobalSettings.Dashboard.getShowJoyoProgress()) {
                    JoyoProgressTable(progress = joyoProgress.value)
                }

                if (GlobalSettings.Dashboard.getShowJlptProgress()) {
                    JlptProgressTable(progress = jlptProgress.value)
                }

                if (GlobalSettings.Dashboard.getShowRecentUnlocks()) {
                    RecentUnlocksCard(
                        subjects = recentUnlocks.value ?: emptyList(),
                        onSubjectClick = onSubjectClick,
                    )
                }

                if (GlobalSettings.Dashboard.getShowCriticalCondition()) {
                    CriticalConditionCard(
                        subjects = criticalCondition.value ?: emptyList(),
                        onSubjectClick = onSubjectClick,
                    )
                }

                if (GlobalSettings.Dashboard.getShowBurnedItems()) {
                    BurnedItemsCard(
                        subjects = burnedItems.value ?: emptyList(),
                        onSubjectClick = onSubjectClick,
                    )
                }
            }
        }

        // Pinned bottom bar with the session buttons; renders nothing when
        // there is nothing to show.
        SessionButtonsRow(
            availableLessons = timeLine.value?.numAvailableLessons ?: 0,
            availableReviews = timeLine.value?.numAvailableReviews ?: 0,
            sessionActive = when (sessionState.value) {
                null, SessionState.INACTIVE -> false
                else -> true
            },
            onStartLessons = onStartLessons,
            onStartReviews = onStartReviews,
            onResumeSession = onResumeSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
