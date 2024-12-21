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

package com.smouldering_durtles.wk.activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.Identification;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.FragmentTransitionAnimation;
import com.smouldering_durtles.wk.enums.OnlineStatus;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.fragments.AbstractFragment;
import com.smouldering_durtles.wk.jobs.ActivityResumedJob;
import com.smouldering_durtles.wk.jobs.AutoSyncNowJob;
import com.smouldering_durtles.wk.jobs.FlushTasksJob;
import com.smouldering_durtles.wk.jobs.SettingChangedJob;
import com.smouldering_durtles.wk.jobs.SyncNowJob;
import com.smouldering_durtles.wk.jobs.TickJob;
import com.smouldering_durtles.wk.livedata.LiveLevelDuration;
import com.smouldering_durtles.wk.livedata.LiveSessionProgress;
import com.smouldering_durtles.wk.livedata.LiveSessionState;
import com.smouldering_durtles.wk.livedata.LiveTaskCounts;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.model.TaskCounts;
import com.smouldering_durtles.wk.services.JobRunnerService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.FLUSH_TASKS_WARNING;
import static com.smouldering_durtles.wk.Constants.MINUTE;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.join;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ObjectSupport.safeNullable;
import static com.smouldering_durtles.wk.util.TextUtil.renderHtml;

/**
 * Abstract superclass for all activities that takes care of a bunch of common functionality.
 */
public abstract class AbstractActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Actment {
    private static long lastResume = 0;
    private static long lastPause = 0;
    private final int layoutId;
    private final int optionsMenuId;
    private final boolean mainActivity;
    private @Nullable Timer tickTimer = null;
    private @Nullable ActiveTheme creationTheme = null;
    private @Nullable Resources.Theme createdTheme = null;

    /**
     * True if the activity is 'active', i.e. the user can interact with the buttons on it.
     */
    protected boolean interactionEnabled = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    /**
     * The constructor.
     *
     * @param layoutId id for this activity's layout
     * @param optionsMenuId id for this activity's menu
     */
    protected AbstractActivity(final int layoutId, final int optionsMenuId) {
        this.layoutId = layoutId;
        this.optionsMenuId = optionsMenuId;
        mainActivity = this instanceof MainActivity;
    }

    private void onCreateBaseLiveTaskCounts(final ActionBar actionBar, final @Nullable TaskCounts t) {
        final OnlineStatus onlineStatus = WkApplication.getInstance().getOnlineStatus();
        final boolean hasApi = t != null && t.getApiCount() > 0 && onlineStatus.canCallApi();
        final boolean hasAudio = t != null && t.getAudioCount() > 0 && onlineStatus.canDownloadAudio();
        final boolean hasPitchInfo = t != null && t.getPitchInfoCount() > 0 && onlineStatus.canDownloadAudio();

        final Collection<String> parts = new ArrayList<>();
        if (hasApi) {
            parts.add(String.format(Locale.ROOT, "%d background tasks", t.getApiCount()));
        }
        if (hasAudio) {
            parts.add(String.format(Locale.ROOT, "%d audio download tasks", t.getAudioCount()));
        }
        if (hasPitchInfo) {
            parts.add(String.format(Locale.ROOT, "%d pitch info download tasks", t.getPitchInfoCount()));
        }

        if (parts.isEmpty()) {
            actionBar.setSubtitle(null);
        }
        else {
            actionBar.setSubtitle(join(", ", "", "", parts) + "... ⌛");
        }

        final @Nullable Menu menu = getMenu();
        if (menu != null) {
            final @Nullable MenuItem flushTasksItem = menu.findItem(R.id.action_flush_tasks);
            if (t != null && flushTasksItem != null) {
                flushTasksItem.setVisible(!t.isEmpty());
            }
        }
    }

    private void liveSessionStateOnChangeHelper() {
        final @Nullable Menu menu = getMenu();
        final Session session = Session.getInstance();

        if (menu != null) {
            final @Nullable MenuItem sessionLogItem = menu.findItem(R.id.action_session_log);
            if (sessionLogItem != null) {
                sessionLogItem.setVisible(!session.isInactive());
            }

            final @Nullable MenuItem abandonSessionItem = menu.findItem(R.id.action_abandon_session);
            if (abandonSessionItem != null) {
                abandonSessionItem.setVisible(session.canBeAbandoned());
            }

            final @Nullable MenuItem wrapupSessionItem = menu.findItem(R.id.action_wrapup_session);
            if (wrapupSessionItem != null) {
                wrapupSessionItem.setVisible(session.canBeWrappedUp());
            }

            final @Nullable MenuItem selfStudyItem = menu.findItem(R.id.action_self_study);
            if (selfStudyItem != null) {
                selfStudyItem.setVisible(session.isInactive());
            }
        }
    }

    private void onCreateBase() {
        creationTheme = ActiveTheme.getCurrentTheme();
        setContentView(layoutId);

        final @Nullable Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            final @Nullable ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (!mainActivity) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    actionBar.setDisplayShowHomeEnabled(true);
                }

                LiveTaskCounts.getInstance().observe(this, t -> safe(() -> onCreateBaseLiveTaskCounts(actionBar, t)));
            }
        }

        LiveSessionState.getInstance().observe(this, t -> safe(this::liveSessionStateOnChangeHelper));

        LiveSessionProgress.getInstance().observe(this, t -> safe(() -> {
            final @Nullable Menu menu = getMenu();
            final Session session = Session.getInstance();

            if (menu != null) {
                final @Nullable MenuItem wrapupSessionItem = menu.findItem(R.id.action_wrapup_session);
                if (wrapupSessionItem != null) {
                    wrapupSessionItem.setVisible(session.canBeWrappedUp());
                }
            }
        }));

        LiveLevelDuration.getInstance().observe(this, t -> safe(() -> {
            final @Nullable Menu menu = getMenu();
            if (menu != null) {
                final @Nullable MenuItem testItem = menu.findItem(R.id.action_test);
                if (testItem != null) {
                    final @Nullable String username = t.getUsername();
                    testItem.setVisible(!isEmpty(username) && username.equals(Identification.AUTHOR_USERNAME));
                }
            }
        }));
    }

    @Override
    protected final void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        safe(() -> {
            onCreateBase();
            onCreateLocal(savedInstanceState);
        });
    }

    @Override
    protected final void onResume() {
        safe(() -> {
            super.onResume();

            if (creationTheme != null && creationTheme != ActiveTheme.getCurrentTheme()) {
                recreate();
                return;
            }

            if (GlobalSettings.Api.getSyncOnOpen() && (lastPause == 0 && lastResume == 0
                    || lastResume < lastPause && System.currentTimeMillis() - lastPause > 3 * MINUTE)) {
                JobRunnerService.schedule(AutoSyncNowJob.class, "");
            }
            lastResume = System.currentTimeMillis();

            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

            if (!showWithoutApiKey()) {
                final @Nullable String apiKey = GlobalSettings.Api.getApiKey();
                if (isEmpty(apiKey)) {
                    goToActivity(NoApiKeyHelpActivity.class);
                }
            }

            JobRunnerService.schedule(ActivityResumedJob.class, getClass().getSimpleName());

            tickTimer = new Timer();
            tickTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    safe(() -> JobRunnerService.schedule(TickJob.class, ""));
                }
            }, MINUTE, MINUTE);

            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            onResumeLocal();
            enableInteraction();
        });
    }

    @Override
    protected final void onPause() {
        safe(() -> {
            super.onPause();

            lastPause = System.currentTimeMillis();

            if (tickTimer != null) {
                tickTimer.cancel();
                tickTimer = null;
            }

            PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

            onPauseLocal();
        });
    }

    @SuppressWarnings("SameReturnValue")
    private boolean onCreateOptionsMenuHelper(final Menu menu) {
        getMenuInflater().inflate(optionsMenuId, menu);

        final @Nullable MenuItem sessionLogItem = menu.findItem(R.id.action_session_log);
        if (sessionLogItem != null) {
            final Session session = Session.getInstance();
            sessionLogItem.setVisible(!session.isInactive());
        }

        final @Nullable MenuItem abandonSessionItem = menu.findItem(R.id.action_abandon_session);
        if (abandonSessionItem != null) {
            final Session session = Session.getInstance();
            abandonSessionItem.setVisible(session.canBeAbandoned());
        }

        final @Nullable MenuItem wrapupSessionItem = menu.findItem(R.id.action_wrapup_session);
        if (wrapupSessionItem != null) {
            final Session session = Session.getInstance();
            wrapupSessionItem.setVisible(session.canBeWrappedUp());
        }

        final @Nullable MenuItem viewLastFinishedItem = menu.findItem(R.id.action_view_last_finished);
        if (viewLastFinishedItem != null) {
            final Session session = Session.getInstance();
            viewLastFinishedItem.setVisible(session.getLastFinishedSubjectId() != -1);
        }

        final @Nullable MenuItem backToPresentationItem = menu.findItem(R.id.action_back_to_presentation);
        if (backToPresentationItem != null) {
            final Session session = Session.getInstance();
            backToPresentationItem.setVisible(session.getType() == SessionType.LESSON && session.isActive());
        }

        final @Nullable MenuItem studyMaterialsItem = menu.findItem(R.id.action_study_materials);
        if (studyMaterialsItem != null) {
            studyMaterialsItem.setVisible(getCurrentSubject() != null && getCurrentSubject().getType().canHaveStudyMaterials());
        }

        final @Nullable MenuItem selfStudyItem = menu.findItem(R.id.action_self_study);
        if (selfStudyItem != null) {
            final Session session = Session.getInstance();
            selfStudyItem.setVisible(session.isInactive());
        }

        final @Nullable MenuItem flushTasksItem = menu.findItem(R.id.action_flush_tasks);
        if (flushTasksItem != null) {
            flushTasksItem.setVisible(!LiveTaskCounts.getInstance().get().isEmpty());
        }

        final @Nullable SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final @Nullable MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null && searchManager != null) {
            final SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, BrowseActivity.class)));
        }

        final @Nullable MenuItem testItem = menu.findItem(R.id.action_test);
        if (testItem != null) {
            final @Nullable String username = LiveLevelDuration.getInstance().get().getUsername();
            testItem.setVisible(!isEmpty(username) && username.equals(Identification.AUTHOR_USERNAME));
        }

        final @Nullable MenuItem muteItem = menu.findItem(R.id.action_mute);
        if (muteItem != null) {
            final boolean muted = WkApplication.getDatabase().propertiesDao().getIsMuted();
            final int imageId = muted ? R.drawable.ic_volume_off_24dp : R.drawable.ic_volume_up_24dp;
            final String title = muted ? "Unmute" : "Mute";
            muteItem.setIcon(ContextCompat.getDrawable(this, imageId));
            muteItem.setTitle(title);
        }

        return true;
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        return safe(true, () -> onCreateOptionsMenuHelper(menu));
    }

    private boolean onOptionsItemSelectedHelper(final MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            goToPreferencesActivity(null);
            return true;
        }
        if (itemId == R.id.action_search) {
            startSearch(null, false, null, false);
            return true;
        }
        if (itemId == R.id.action_mute) {
            final boolean muted = !WkApplication.getDatabase().propertiesDao().getIsMuted();
            WkApplication.getDatabase().propertiesDao().setIsMuted(muted);
            final int imageId = muted
                    ? R.drawable.ic_volume_off_24dp
                    : R.drawable.ic_volume_up_24dp;
            final String title = muted ? "Unmute" : "Mute";
            item.setIcon(ContextCompat.getDrawable(this, imageId));
            item.setTitle(title);
            return true;
        }
        if (itemId == R.id.action_dashboard) {
            goToMainActivity();
            return true;
        }
        if (itemId == R.id.action_download_audio) {
            goToActivity(DownloadAudioActivity.class);
            return true;
        }
        if (itemId == R.id.action_browse) {
            goToActivity(BrowseActivity.class);
            return true;
        }
        if (itemId == R.id.action_view_last_finished) {
            final long subjectId = Session.getInstance().getLastFinishedSubjectId();
            if (subjectId != -1) {
                goToSubjectInfo(subjectId, Collections.emptyList(), FragmentTransitionAnimation.RTL);
            }
            return true;
        }
        if (itemId == R.id.action_back_to_presentation) {
            Session.getInstance().goBackToPresentation();
            if (!(this instanceof SessionActivity)) {
                goToActivity(SessionActivity.class);
            }
            return true;
        }
        if (itemId == R.id.action_session_log) {
            goToSessionLog();
            return true;
        }
        if (itemId == R.id.action_abandon_session) {
            final Session session = Session.getInstance();
            if (GlobalSettings.UiConfirmations.getUiConfirmAbandonSession()) {
                final long numActive = session.getNumActiveItems();
                final long numPending = session.getNumPendingItems();
                final long numReported = session.getNumReportedItems();
                final long numStarted = session.getNumStartedItems();
                final long numNotStarted = numActive - numStarted;
                String message = "Are you sure you want to abandon this session? If you do:";
                if (numPending > 0) {
                    message += String.format(Locale.ROOT, "\n- %d finished items will not be reported", numPending);
                }
                if (numReported > 0) {
                    message += String.format(Locale.ROOT, "\n- %d already finished items will still be reported", numReported);
                }
                if (numStarted > 0) {
                    message += String.format(Locale.ROOT, "\n- %d partially quizzed items will not be reported", numStarted);
                }
                if (numNotStarted > 0) {
                    message += String.format(Locale.ROOT, "\n- %d unquizzed items will not be reported", numNotStarted);
                }
                new AlertDialog.Builder(this)
                        .setTitle("Abandon session?")
                        .setMessage(message)
                        .setIcon(R.drawable.ic_baseline_warning_24px)
                        .setNegativeButton("No", (dialog, which) -> {
                        })
                        .setNeutralButton("Yes and don't ask again", (dialog, which) -> safe(() -> {
                            session.finish();
                            Toast.makeText(this, "Session abandoned", Toast.LENGTH_SHORT).show();
                            GlobalSettings.UiConfirmations.setUiConfirmAbandonSession(false);
                        }))
                        .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                            session.finish();
                            Toast.makeText(this, "Session abandoned", Toast.LENGTH_SHORT).show();
                        })).create().show();
            } else {
                session.finish();
                Toast.makeText(this, "Session abandoned", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (itemId == R.id.action_wrapup_session) {
            final Session session = Session.getInstance();
            if (GlobalSettings.UiConfirmations.getUiConfirmWrapupSession()) {
                final long numActive = session.getNumActiveItems();
                final long numStarted = session.getNumStartedItems();
                final long numNotStarted = numActive - numStarted;
                String message = "Are you sure you want to wrap up this session? If you do:";
                if (numStarted > 0) {
                    message += String.format(Locale.ROOT, "\n- %d partially quizzed items will remain in the session", numStarted);
                }
                if (numNotStarted > 0) {
                    message += String.format(Locale.ROOT, "\n- %d unquizzed items will be removed from the session", numNotStarted);
                }
                new AlertDialog.Builder(this)
                        .setTitle("Wrap up session?")
                        .setMessage(message)
                        .setIcon(R.drawable.ic_baseline_warning_24px)
                        .setNegativeButton("No", (dialog, which) -> {
                        })
                        .setNeutralButton("Yes and don't ask again", (dialog, which) -> safe(() -> {
                            session.wrapup();
                            Toast.makeText(this, "Session wrapping up...", Toast.LENGTH_SHORT).show();
                            GlobalSettings.UiConfirmations.setUiConfirmWrapupSession(false);
                        }))
                        .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                            session.wrapup();
                            Toast.makeText(this, "Session wrapping up...", Toast.LENGTH_SHORT).show();
                        })).create().show();
            } else {
                session.wrapup();
                Toast.makeText(this, "Session wrapping up...", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (itemId == R.id.action_study_materials) {
            final @Nullable Subject subject = getCurrentSubject();
            if (subject != null && subject.getType().canHaveStudyMaterials()) {
                goToStudyMaterialsActivity(subject.getId());
            }
            return true;
        }
        if (itemId == R.id.action_self_study) {
            goToActivity(SelfStudyStartActivity.class);
            return true;
        }
        if (itemId == R.id.action_sync_now) {
            JobRunnerService.schedule(SyncNowJob.class, "");
            return true;
        }
        if (itemId == R.id.action_flush_tasks) {
            new AlertDialog.Builder(this)
                    .setTitle("Flush background tasks?")
                    .setMessage(renderHtml(FLUSH_TASKS_WARNING))
                    .setIcon(R.drawable.ic_baseline_warning_24px)
                    .setNegativeButton("No", (dialog, which) -> {
                    })
                    .setPositiveButton("Yes", (dialog, which) -> safe(() -> {
                        JobRunnerService.schedule(FlushTasksJob.class, "");
                        Toast.makeText(this, "Background tasks flushed!", Toast.LENGTH_SHORT).show();
                    })).create().show();
            return true;
        }
        if (itemId == R.id.action_about) {
            goToActivity(AboutActivity.class);
            return true;
        }
        if (itemId == R.id.action_support) {
            goToActivity(SupportActivity.class);
            return true;
        }
        if (itemId == R.id.action_test) {
            Toast.makeText(this, "Test!", Toast.LENGTH_SHORT).show();
            goToActivity(TestActivity.class);
            return true;
        }
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        return safe(true, () -> onOptionsItemSelectedHelper(item));
    }

    @Override
    public final void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        safe(() -> {
            JobRunnerService.schedule(SettingChangedJob.class, key);
            if ("theme".equals(key)) {
                recreate();
                WkApplication.getInstance().resetTheme();
            }
        });
    }

    /**
     * When changing content within an activity, collapse the search box.
     */
    protected final void collapseSearchBox() {
        safe(() -> {
            final @Nullable Menu menu = getMenu();
            if (menu != null) {
                final @Nullable MenuItem searchItem = menu.findItem(R.id.action_search);
                if (searchItem != null) {
                    searchItem.collapseActionView();
                }
            }
        });
    }

    /**
     * Get the current theme based on user settings.
     *
     * @return the theme
     */
    @Override
    public final @Nullable Resources.Theme getTheme() {
        if (createdTheme == null) {
            safe(() -> {
                createdTheme = super.getTheme();
                if (createdTheme != null) {
                    createdTheme.applyStyle(ActiveTheme.getCurrentTheme().getStyleId(), true);
                }
            });
        }
        return createdTheme;
    }

    @Override
    public final @Nullable Toolbar getToolbar() {
        return safeNullable(() -> findViewById(R.id.toolbar));
    }

    /**
     * Get the menu for this activity.
     *
     * @return the menu or null if it doesn't exist (yet).
     */
    public final @Nullable Menu getMenu() {
        return safeNullable(() -> {
            final @Nullable Toolbar toolbar = getToolbar();
            if (toolbar == null) {
                return null;
            }
            return toolbar.getMenu();
        });
    }

    @Override
    public final void goToActivity(final Class<? extends AbstractActivity> clas) {
        safe(() -> startActivity(new Intent(this, clas)));
    }

    /**
     * Jump to the preferences activity immediately.
     *
     * @param rootKey the key of the screen to jump to, or null for the root screen
     */
    protected final void goToPreferencesActivity(final @Nullable String rootKey) {
        safe(() -> {
            final Intent intent = new Intent(this, PreferencesActivity.class);
            if (rootKey != null) {
                intent.putExtra("rootKey", rootKey);
            }
            startActivity(intent);
        });
    }

    public final void goToMainActivity() {
        safe(() -> {
            final Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    @Override
    public final void goToSubjectInfo(final long id, final List<Long> ids, final FragmentTransitionAnimation animation) {
        safe(() -> {
            final long[] a = new long[ids.size()];
            for (int i=0; i<a.length; i++) {
                a[i] = ids.get(i);
            }
            goToSubjectInfo(id, a, animation);
        });
    }

    @Override
    public final void goToSubjectInfo(final long id, final long[] ids, final FragmentTransitionAnimation animation) {
        safe(() -> {
            if (this instanceof BrowseActivity) {
                ((BrowseActivity) this).loadSubjectInfoFragment(id, ids, animation);
                return;
            }
            final Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("ids", ids);
            startActivity(intent);
        });
    }

    /**
     * Go to the session log fragment.
     */
    private void goToSessionLog() {
        safe(() -> {
            if (this instanceof BrowseActivity) {
                ((BrowseActivity) this).loadSessionLogFragment();
                return;
            }
            final Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra("sessionLog", true);
            startActivity(intent);
        });
    }

    @Override
    public final void goToSearchResult(final int searchType, final String searchParameters, final @Nullable String presetName) {
        safe(() -> {
            if (this instanceof BrowseActivity) {
                ((BrowseActivity) this).loadSearchResultFragment(presetName, searchType, searchParameters);
                return;
            }
            final Intent intent = new Intent(this, BrowseActivity.class);
            intent.putExtra("searchType", searchType);
            intent.putExtra("searchParameters", searchParameters);
            if (presetName != null) {
                intent.putExtra("presetName", presetName);
            }
            startActivity(intent);
        });
    }

    /**
     * Go to the resurrect activity with the supplied list of subject IDs to resurrect.
     *
     * @param ids the subject IDs
     */
    public final void goToResurrectActivity(final long[] ids) {
        safe(() -> {
            final Intent intent = new Intent(this, ResurrectActivity.class);
            intent.putExtra("ids", ids);
            startActivity(intent);
        });
    }

    /**
     * Go to the burn activity with the supplied list of subject IDs to burn.
     *
     * @param ids the subject IDs
     */
    public final void goToBurnActivity(final long[] ids) {
        safe(() -> {
            final Intent intent = new Intent(this, BurnActivity.class);
            intent.putExtra("ids", ids);
            startActivity(intent);
        });
    }

    private void goToStudyMaterialsActivity(final long id) {
        safe(() -> {
            final Intent intent = new Intent(this, StudyMaterialsActivity.class);
            intent.putExtra("id", id);
            startActivity(intent);
        });
    }

    /**
     * Get the subject that this activity is currently dealing with,
     * or null if there is no specific subject.
     *
     * @return the subject
     */
    private @Nullable Subject getCurrentSubject() {
        final @Nullable AbstractFragment fragment = getCurrentFragment();
        if (fragment != null) {
            return fragment.getCurrentSubject();
        }
        return null;
    }

    /**
     * Update the current subject ID for this activity. This provides the
     * context to enable the menu option for study materials.
     */
    public final void updateCurrentSubject() {
        safe(() -> {
            final @Nullable Menu menu = getMenu();
            if (menu != null) {
                final @Nullable MenuItem studyMaterialsItem = menu.findItem(R.id.action_study_materials);
                if (studyMaterialsItem != null) {
                    studyMaterialsItem.setVisible(getCurrentSubject() != null && getCurrentSubject().getType().canHaveStudyMaterials());
                }
            }
        });
    }

    /**
     * The current fragment attached to the default fragment container view.
     *
     * @return the fragment or null if not found
     */
    protected final @Nullable AbstractFragment getCurrentFragment() {
        final FragmentManager manager = getSupportFragmentManager();
        final @Nullable Fragment fragment = manager.findFragmentById(R.id.fragment);
        if (fragment instanceof AbstractFragment) {
            return (AbstractFragment) fragment;
        }
        return null;
    }

    /**
     * Enable interactivity on the current activity. This makes buttons clickable, etc.
     *
     * <p>
     *     The idea is that if an interaction can take time to resolve, further
     *     interaction is disabled until it has been resolved. Allowing parallel actions
     *     sometimes causes problems.
     * </p>
     */
    protected final void enableInteraction() {
        safe(() -> {
            enableInteractionLocal();
            interactionEnabled = true;
        });
    }

    /**
     * Disable interactivity on the current activity. This makes buttons non-clickable, etc.
     *
     * <p>
     *     The idea is that if an interaction can take time to resolve, further
     *     interaction is disabled until it has been resolved. Allowing parallel actions
     *     sometimes causes problems.
     * </p>
     */
    protected final void disableInteraction() {
        safe(() -> {
            interactionEnabled = false;
            disableInteractionLocal();
        });
    }

    /**
     * Translate DIPs to pixels.
     *
     * @param dp the dimension in DIPs
     * @return the corresponding number of pixels
     */
    protected final int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Handle the onCreate event. This is called after the generic handling has been done.
     *
     * @param savedInstanceState the saved instance state from onCreate()
     */
    protected abstract void onCreateLocal(@Nullable Bundle savedInstanceState);

    /**
     * Handle the onResume event. This is called after the generic handling has been done.
     */
    protected abstract void onResumeLocal();

    /**
     * Handle the onPause event. This is called after the generic handling has been done.
     */
    protected abstract void onPauseLocal();

    /**
     * See enableInteraction(), this is the part that must be implemented by each subclass.
     */
    protected abstract void enableInteractionLocal();

    /**
     * See disableInteraction(), this is the part that must be implemented by each subclass.
     */
    protected abstract void disableInteractionLocal();

    /**
     * Can this activity be activated without entering an API key?
     *
     * @return False if showing this activity should bounde to the NoApiKeyHelpActivity.
     */
    protected abstract boolean showWithoutApiKey();
}
