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

package com.smouldering_durtles.wk;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

import androidx.core.net.ConnectivityManagerCompat;
import androidx.multidex.MultiDexApplication;

import com.smouldering_durtles.wk.components.EncryptedPreferenceDataStore;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.NotificationPriority;
import com.smouldering_durtles.wk.enums.OnlineStatus;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.enums.SubjectInfoDump;
import com.smouldering_durtles.wk.jobs.NetworkStateChangedJob;
import com.smouldering_durtles.wk.livedata.LiveAlertContext;
import com.smouldering_durtles.wk.livedata.LiveAudioDownloadStatus;
import com.smouldering_durtles.wk.livedata.LiveBurnedItems;
import com.smouldering_durtles.wk.livedata.LiveCriticalCondition;
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup;
import com.smouldering_durtles.wk.livedata.LiveJlptProgress;
import com.smouldering_durtles.wk.livedata.LiveJoyoProgress;
import com.smouldering_durtles.wk.livedata.LiveLevelDuration;
import com.smouldering_durtles.wk.livedata.LiveLevelProgress;
import com.smouldering_durtles.wk.livedata.LiveRecentUnlocks;
import com.smouldering_durtles.wk.livedata.LiveSearchPresets;
import com.smouldering_durtles.wk.livedata.LiveSessionProgress;
import com.smouldering_durtles.wk.livedata.LiveSessionState;
import com.smouldering_durtles.wk.livedata.LiveSrsBreakDown;
import com.smouldering_durtles.wk.livedata.LiveSrsSystems;
import com.smouldering_durtles.wk.livedata.LiveTaskCounts;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.livedata.LiveVacationMode;
import com.smouldering_durtles.wk.livedata.LiveWorkInfos;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.services.BackgroundAlarmReceiver;
import com.smouldering_durtles.wk.services.JobRunnerService;
import com.smouldering_durtles.wk.util.AsyncTask;
import com.smouldering_durtles.wk.util.DbLogger;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.enums.OnlineStatus.METERED;
import static com.smouldering_durtles.wk.enums.OnlineStatus.NO_CONNECTION;
import static com.smouldering_durtles.wk.enums.OnlineStatus.UNMETERED;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static java.util.Objects.requireNonNull;

/**
 * Global application object to handle some global concerns.
 */
public final class WkApplication extends MultiDexApplication {
    private static @Nullable WkApplication instance = null;
    private static @Nullable AppDatabase database = null;
    private static @Nullable EncryptedPreferenceDataStore encryptedPreferenceDataStore = null;

    private @Nullable ActiveTheme currentTheme = null;
    private @Nullable Resources.Theme createdTheme = null;
    private OnlineStatus onlineStatus = NO_CONNECTION;

    /**
     * Get the singleton application instance.
     *
     * @return the application instance
     */
    public static WkApplication getInstance() {
        return requireNonNull(instance);
    }

    /**
     * Get the singleton database instance.
     *
     * @return the database instance
     */
    public static AppDatabase getDatabase() {
        return requireNonNull(database);
    }

    /**
     * Get the singleton store for encrypted settings.
     *
     * @return the store instance
     */
    public static EncryptedPreferenceDataStore getEncryptedPreferenceDataStore() {
        return requireNonNull(encryptedPreferenceDataStore);
    }

    private static void initialize(final WkApplication application) {
        instance = application;
        database = AppDatabase.getInstance();
        DbLogger.initializeInstance(database);
        encryptedPreferenceDataStore = new EncryptedPreferenceDataStore();
        encryptedPreferenceDataStore.getString("api_key", null);
        encryptedPreferenceDataStore.getString("web_password", null);
        GlobalSettings.setApplication(application);

        safe(() -> LiveTaskCounts.getInstance().initialize());
        safe(() -> LiveSearchPresets.getInstance().initialize());
        safe(() -> LiveWorkInfos.getInstance().initialize());

        LiveFirstTimeSetup.getInstance().observeForever(t -> safe(() -> {
            LiveTimeLine.getInstance().ping();
            LiveAlertContext.getInstance().ping();
            LiveSrsBreakDown.getInstance().ping();
            LiveRecentUnlocks.getInstance().ping();
            LiveCriticalCondition.getInstance().ping();
            LiveBurnedItems.getInstance().ping();
            LiveLevelProgress.getInstance().ping();
            LiveLevelDuration.getInstance().ping();
            LiveJoyoProgress.getInstance().ping();
            LiveJlptProgress.getInstance().ping();
        }));
        LiveVacationMode.getInstance().observeForever(t -> safe(() -> LiveTimeLine.getInstance().ping()));
        LiveSessionState.getInstance().observeForever(t -> safe(() -> LiveTimeLine.getInstance().ping()));
        LiveTaskCounts.getInstance().observeForever(t -> safe(() -> LiveAudioDownloadStatus.getInstance().ping()));
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void registerNetworkStateChangeListenerPre24() {
        final @Nullable ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        final IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                safe(() -> {
                    final @Nullable android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        onlineStatus = ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager) ? METERED : UNMETERED;
                    }
                    else {
                        onlineStatus = NO_CONNECTION;
                    }
                    JobRunnerService.schedule(NetworkStateChangedJob.class, onlineStatus.name());
                });
            }
        }, filter);
    }

    @TargetApi(24)
    private void registerNetworkStateChangeListenerPost24() {
        final @Nullable ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(final Network network) {
                safe(() -> {
                    onlineStatus = connectivityManager.isActiveNetworkMetered() ? METERED : UNMETERED;
                    JobRunnerService.schedule(NetworkStateChangedJob.class, onlineStatus.name());
                });
            }

            @Override
            public void onLost(final Network network) {
                safe(() -> {
                    onlineStatus = NO_CONNECTION;
                    JobRunnerService.schedule(NetworkStateChangedJob.class, onlineStatus.name());
                });
            }
        });
    }

    private void onCreateLocal() {
        initialize(this);
        new Task().execute();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkStateChangeListenerPost24();
        }
        else {
            registerNetworkStateChangeListenerPre24();
        }
    }

    @Override
    public void onCreate() {
        safe(() -> {
            super.onCreate();
            onCreateLocal();
        });
    }

    /**
     * Get the current theme based on user settings.
     *
     * @return the theme
     */
    @Override
    public @Nullable Resources.Theme getTheme() {
        if (currentTheme != GlobalSettings.Display.getTheme()) {
            createdTheme = null;
            currentTheme = GlobalSettings.Display.getTheme();
        }
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

    /**
     * After a theme setting change, forget the current one so it gets recreated.
     */
    public void resetTheme() {
        createdTheme = null;
    }

    /**
     * Get the current online status. The valus is automatically updated with a listener/receiver registered by this class.
     * Changes in value are also reported via a scheduled job.
     *
     * @return the status
     */
    public OnlineStatus getOnlineStatus() {
        return onlineStatus;
    }

    private static final class Task extends AsyncTask<Void> {
        @Override
        public @Nullable Void doInBackground() {
            final AppDatabase db = requireNonNull(database);

            db.propertiesDao().preload();

            safe(() -> {
                db.propertiesDao().deleteProperty("migration_done_audio1");
                db.propertiesDao().deleteProperty("self_study_configuration");
            });

            safe(() -> {
                if (!db.propertiesDao().getMigrationDoneAnkiSplit()) {
                    db.propertiesDao().setMigrationDoneAnkiSplit(true);
                    final boolean ankiLesson = GlobalSettings.AdvancedLesson.getAnkiMode();
                    GlobalSettings.AdvancedLesson.setAnkiModeMeaning(ankiLesson);
                    GlobalSettings.AdvancedLesson.setAnkiModeReading(ankiLesson);
                    final boolean ankiReview = GlobalSettings.AdvancedReview.getAnkiMode();
                    GlobalSettings.AdvancedReview.setAnkiModeMeaning(ankiReview);
                    GlobalSettings.AdvancedReview.setAnkiModeReading(ankiReview);
                    final boolean ankiSelfStudy = GlobalSettings.AdvancedSelfStudy.getAnkiMode();
                    GlobalSettings.AdvancedSelfStudy.setAnkiModeMeaning(ankiSelfStudy);
                    GlobalSettings.AdvancedSelfStudy.setAnkiModeReading(ankiSelfStudy);
                }
            });

            safe(() -> {
                if (!db.propertiesDao().getMigrationDoneAudio2()) {
                    db.propertiesDao().setMigrationDoneAudio2(true);
                    final boolean audioLessonPresentation = GlobalSettings.getAutoPlay(SessionType.LESSON);
                    GlobalSettings.Audio.setAutoplayLessonPresentation(audioLessonPresentation);
                    final boolean audioAnkiReveal = GlobalSettings.getAutoPlay(SessionType.REVIEW);
                    GlobalSettings.Audio.setAutoplayAnkiReveal(audioAnkiReveal);
                    final int maxSize = GlobalSettings.Font.getMaxFontSizeQuizText();
                    if (maxSize == 250) {
                        GlobalSettings.Font.setMaxFontSizeQuizText(100);
                    }
                }
            });

            safe(() -> {
                if (!db.propertiesDao().getMigrationDoneNotif()) {
                    db.propertiesDao().setMigrationDoneNotif(true);
                    final boolean low = GlobalSettings.Other.getNotificationLowPriority();
                    GlobalSettings.Other.setNotificationPriority(low ? NotificationPriority.LOW : NotificationPriority.DEFAULT);
                }
            });

            safe(() -> {
                if (!db.propertiesDao().getMigrationDoneDump()) {
                    db.propertiesDao().setMigrationDoneDump(true);
                    final SubjectInfoDump dump1 = GlobalSettings.SubjectInfo.getMeaningInfoDump();
                    GlobalSettings.SubjectInfo.setMeaningInfoDumpIncorrect(dump1);
                    final SubjectInfoDump dump2 = GlobalSettings.SubjectInfo.getReadingInfoDump();
                    GlobalSettings.SubjectInfo.setReadingInfoDumpIncorrect(dump2);
                }
            });

            safe(() -> {
                if (LiveSrsSystems.getInstance().hasNullValue()) {
                    LiveSrsSystems.getInstance().update();
                }
                if (LiveVacationMode.getInstance().hasNullValue()) {
                    LiveVacationMode.getInstance().update();
                }
                if (LiveSrsBreakDown.getInstance().hasNullValue()) {
                    LiveSrsBreakDown.getInstance().update();
                }
                if (LiveLevelDuration.getInstance().hasNullValue()) {
                    LiveLevelDuration.getInstance().update();
                }
                if (LiveLevelProgress.getInstance().hasNullValue()) {
                    LiveLevelProgress.getInstance().update();
                }
                if (LiveJoyoProgress.getInstance().hasNullValue()) {
                    LiveJoyoProgress.getInstance().update();
                }
                if (LiveJlptProgress.getInstance().hasNullValue()) {
                    LiveJlptProgress.getInstance().update();
                }
                if (LiveRecentUnlocks.getInstance().hasNullValue()) {
                    LiveRecentUnlocks.getInstance().update();
                }
                if (LiveCriticalCondition.getInstance().hasNullValue()) {
                    LiveCriticalCondition.getInstance().update();
                }
                if (LiveBurnedItems.getInstance().hasNullValue()) {
                    LiveBurnedItems.getInstance().update();
                }
                if (LiveFirstTimeSetup.getInstance().hasNullValue()) {
                    LiveFirstTimeSetup.getInstance().update();
                }
                if (LiveSessionProgress.getInstance().hasNullValue()) {
                    LiveSessionProgress.getInstance().update();
                }
                if (LiveAlertContext.getInstance().hasNullValue()) {
                    LiveAlertContext.getInstance().update();
                }

                Session.getInstance().load();
            });

            return null;
        }

        @Override
        public void onPostExecute(final @Nullable Void result) {
            LiveAlertContext.getInstance().observeForever(alertContext -> {
                BackgroundAlarmReceiver.processAlarm(alertContext);
                com.smouldering_durtles.wk.widgets.WidgetUpdater.updateAll(instance);
            });
        }

        @Override
        public void onProgressUpdate(final Object[] values) {
            //
        }
    }
}
