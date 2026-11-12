package com.lazy_durtles.wk.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lazy_durtles.wk.R
import com.lazy_durtles.wk.api.ApiState
import com.lazy_durtles.wk.db.Converters
import com.lazy_durtles.wk.enums.SearchSortOrder
import com.lazy_durtles.wk.enums.SubjectType
import com.lazy_durtles.wk.Identification
import com.lazy_durtles.wk.jobs.RetryApiErrorJob
import com.lazy_durtles.wk.jobs.SyncNowJob
import com.lazy_durtles.wk.livedata.LiveApiState
import com.lazy_durtles.wk.model.AdvancedSearchParameters
import com.lazy_durtles.wk.services.JobRunnerService
import com.lazy_durtles.wk.ui.theme.LazyDurtlesTheme
import com.lazy_durtles.wk.WkApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartLessons: () -> Unit,
    onStartReviews: () -> Unit,
    onResumeSession: () -> Unit,
    onGoToSettings: () -> Unit,
    onViewSessionLog: () -> Unit,
    onRetryApiError: () -> Unit,
    onViewKeyboardHelp: () -> Unit,
    onSyncNow: () -> Unit,
    onSrsBucketClick: (bucket: Int) -> Unit,
    onSubjectClick: (subjectId: Long, subjectIds: List<Long>) -> Unit,
    onSearchResults: (searchParametersJson: String, searchType: Int, presetName: String?) -> Unit,
    onBackPressed: () -> Unit = { },
    modifier: Modifier = Modifier,
    initialSearchParametersJson: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val apiState by LiveApiState.getInstance().observeAsState(initial = ApiState.UNKNOWN)
    var isAudioMuted by remember { mutableStateOf(WkApplication.getDatabase().propertiesDao().getIsMuted()) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(apiState) {
        when (apiState) {
            ApiState.ERROR -> {
                snackbarHostState.showSnackbar("API Error: Retry or check settings")
            }
            ApiState.API_KEY_REJECTED -> {
                snackbarHostState.showSnackbar("API Key Rejected: Update in settings")
            }
            else -> {}
        }
    }

    LazyDurtlesTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(Identification.APP_NAME) },
                    actions = {
                        SyncButtonWithPopover(onSyncNow = onSyncNow)
                        IconButton(onClick = {
                            isAudioMuted = !isAudioMuted
                            WkApplication.getDatabase().propertiesDao().setIsMuted(isAudioMuted)
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (isAudioMuted) R.drawable.ic_volume_off_24dp else R.drawable.ic_volume_up_24dp,
                                ),
                                contentDescription = if (isAudioMuted) "Unmute Audio" else "Mute Audio",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                // Neutral tile color instead of the default surface + surfaceTint
                // elevation overlay, which tinted the bar with the accent color.
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Dashboard",
                            )
                        },
                        label = { Text("Dashboard") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                            )
                        },
                        label = { Text("Search") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profile",
                            )
                        },
                        label = { Text("Profile") },
                    )
                }
            },
            modifier = modifier.fillMaxSize(),
        ) { paddingValues ->
            when (selectedTab) {
                0 -> DashboardScreen(
                    apiState = apiState,
                    onStartLessons = onStartLessons,
                    onStartReviews = onStartReviews,
                    onResumeSession = onResumeSession,
                    onGoToSettings = onGoToSettings,
                    onRetryApiError = onRetryApiError,
                    onViewKeyboardHelp = onViewKeyboardHelp,
                    onSrsBucketClick = onSrsBucketClick,
                    onLevelProgressEntryClick = { level, type ->
                        onSearchResults(levelProgressSearchParameters(level, type), 0, null)
                    },
                    onSubjectClick = { subjectId, allIds ->
                        onSubjectClick(subjectId, allIds.toList())
                    },
                    onBackPressed = onBackPressed,
                    modifier = Modifier.padding(paddingValues),
                )
                1 -> SearchScreen(
                    onSubjectClick = onSubjectClick,
                    onSearchResults = onSearchResults,
                    initialParametersJson = initialSearchParametersJson,
                    onBackPressed = onBackPressed,
                    modifier = Modifier.padding(paddingValues),
                )
                2 -> ProfileScreen(
                    onGoToSettings = onGoToSettings,
                    onViewSessionLog = onViewSessionLog,
                    onBackPressed = onBackPressed,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

/**
 * Build the advanced search parameters JSON for a level progression row click,
 * mirroring the legacy LevelProgressRowView click handler.
 */
private fun levelProgressSearchParameters(level: Int, type: SubjectType): String {
    val parameters = AdvancedSearchParameters()
    parameters.minLevel = level
    parameters.maxLevel = level
    parameters.itemTypes.add(type)
    parameters.sortOrder = SearchSortOrder.STAGE_TYPE
    return Converters.getObjectMapper().writeValueAsString(parameters)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        onStartLessons = {},
        onStartReviews = {},
        onResumeSession = {},
        onGoToSettings = {},
        onViewSessionLog = {},
        onRetryApiError = {},
        onViewKeyboardHelp = {},
        onSyncNow = {},
        onSrsBucketClick = {},
        onSubjectClick = { _, _ -> },
        onSearchResults = { _, _, _ -> },
    )
}
