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

package com.lazy_durtles.wk.services;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.enums.OnlineStatus;
import com.lazy_durtles.wk.jobs.Job;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveWorkInfos;
import com.lazy_durtles.wk.util.Logger;
import com.lazy_durtles.wk.util.ObjectSupport;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.lazy_durtles.wk.Constants.MINUTE;
import static com.lazy_durtles.wk.Constants.SECOND;
import static com.lazy_durtles.wk.util.ObjectSupport.getTopOfHour;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;

import javax.annotation.Nonnull;

/**
 * The background worker that implements background sync.
 */
public final class BackgroundSyncWorker extends Worker {
    private static final Logger LOGGER = Logger.get(BackgroundSyncWorker.class);

    /**
     * A tag used to identify work in the work manager.
     */
    private static final String JOB_TAG_OLD = "bgsync";

    /**
     * A tag used to identify work in the work manager.
     */
    public static final String JOB_TAG_NEW = "bgsync2";

    /**
     * The constructor.
     *
     * @param context Android context
     * @param workerParams parameters for this instance, managed by the work manager
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public BackgroundSyncWorker(final Context context, final WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Do the work. If background sync is eanbled, clear any pending API error state,
     * check for tasks that need to run, and do a run of all tasks that can run.
     *
     * @return the result of the work, always Success for this work.
     */
    @Override
    public @Nonnull Result doWork() {
        safe(() -> {
            if (GlobalSettings.Api.getEnableBackgroundSync()) {
                final long topOfHour1 = getTopOfHour(System.currentTimeMillis());
                final long topOfHour2 = WkApplication.getDatabase().propertiesDao().getLastBackgroundSync();
                if (topOfHour1 != topOfHour2) {
                    LOGGER.info("Background sync starts: %s %s", ApiState.getCurrentApiState(), WkApplication.getInstance().getOnlineStatus());
                    final AppDatabase db = WkApplication.getDatabase();
                    WkApplication.getDatabase().propertiesDao().setLastBackgroundSync(topOfHour1);
                    if (WkApplication.getInstance().getOnlineStatus() == OnlineStatus.NO_CONNECTION) {
                        LOGGER.info("Online status is NO_CONNECTION - wait for the network status callback to settle");
                        Thread.sleep(5 * SECOND);
                    }
                    if (LiveApiState.getInstance().get() == ApiState.ERROR) {
                        db.propertiesDao().setApiInError(false);
                        LiveApiState.getInstance().forceUpdate();
                    }
                    Job.assertDueTasks(5 * MINUTE);
                    ApiTaskService.runTasks();
                    LOGGER.info("Background sync ends");
                }
            }
            else {
                cancelWork();
            }
        });

        return Result.success();
    }

    /**
     * Prepare the work request for the background sync, and schedule it with the work manager.
     */
    private static void scheduleWork() {
        final Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        final PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(BackgroundSyncWorker.class, 1, TimeUnit.HOURS, 5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(JOB_TAG_NEW)
                .setInitialDelay(ObjectSupport.nextRandomInt(3600), TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(WkApplication.getInstance()).enqueueUniquePeriodicWork(
                JOB_TAG_NEW, ExistingPeriodicWorkPolicy.KEEP, request);
        WorkManager.getInstance(WkApplication.getInstance()).cancelAllWorkByTag(JOB_TAG_OLD);
    }

    /**
     * Cancel all work for the background sync.
     */
    private static void cancelWork() {
        WorkManager.getInstance(WkApplication.getInstance()).cancelAllWorkByTag(JOB_TAG_OLD);
        WorkManager.getInstance(WkApplication.getInstance()).cancelAllWorkByTag(JOB_TAG_NEW);
    }

    /**
     * Cancel one specific job for the background sync.
     *
     * @param id the ID for the work to cancel
     */
    private static void cancelWork(final UUID id) {
        WorkManager.getInstance(WkApplication.getInstance()).cancelWorkById(id);
    }

    /**
     * Depending on user settings, either schedule or abandon work requests,
     * to make sure what is scheduled matches what the user wants.
     */
    public static void scheduleOrCancelWork() {
        safe(() -> {
            if (LiveWorkInfos.getInstance().hasNullValue()) {
                return;
            }
            final List<WorkInfo> infos = LiveWorkInfos.getInstance().get();

            if (GlobalSettings.Api.getEnableBackgroundSync()) {
                if (infos.isEmpty()) {
                    scheduleWork();
                }
                else if (infos.size() > 1) {
                    cancelWork(infos.get(0).getId());
                }
            }
            else {
                if (!infos.isEmpty()) {
                    cancelWork();
                }
            }
        });
    }
}
