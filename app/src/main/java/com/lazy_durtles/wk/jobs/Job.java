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

package com.lazy_durtles.wk.jobs;

import static com.lazy_durtles.wk.Constants.DAY;
import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.Constants.MINUTE;
import static com.lazy_durtles.wk.Constants.REFERENCE_DATA_VERSION;
import static com.lazy_durtles.wk.Constants.WEEK;
import static com.lazy_durtles.wk.enums.OnlineStatus.NO_CONNECTION;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.enums.OnlineStatus;
import com.lazy_durtles.wk.livedata.LiveAlertContext;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveFirstTimeSetup;
import com.lazy_durtles.wk.livedata.LiveTimeLine;
import com.lazy_durtles.wk.model.TimeLine;
import com.lazy_durtles.wk.services.ApiTaskService;
import com.lazy_durtles.wk.util.DbLogger;
import com.lazy_durtles.wk.util.Logger;

/**
 * Abstract base class for background jobs. These are actions that need to
 * be pushed to a background thread, but don't do network communication and
 * don't need to be retried or persisted across restarts.
 */
public abstract class Job {
    private static final Logger LOGGER = Logger.get(Job.class);

    private static long currentHour = -1;
    private static long currentMinute = -1;

    /**
     * The parameters for this job, encoded in a string in a class-specific format.
     */
    protected final String data;

    /**
     * The constructor.
     *
     * @param data parameters for this job, encoded in a string in a class-specific format
     */
    protected Job(final String data) {
        this.data = data;
    }

    /**
     * Make sure that any tasks that need to be executed are scheduled in the database.
     *
     * @param hourlySkew how much earlier than normal can hourly tasks be triggered?
     */
    public static void assertDueTasks(final long hourlySkew) {
        final AppDatabase db = WkApplication.getDatabase();

        final OnlineStatus onlineStatus = WkApplication.getInstance().getOnlineStatus();
        boolean canTriggerApiTasks = onlineStatus != NO_CONNECTION;
        final ApiState currentApiState = ApiState.getCurrentApiState();

        switch (currentApiState) {
            case API_KEY_MISSING:
            case API_KEY_REJECTED:
            case ERROR:
                db.assertGetUserTask();
                canTriggerApiTasks = false;
                break;
            case UNKNOWN:
            case EXPIRED:
            case REFRESH_USER_DATA:
                db.assertGetUserTask();
                break;
            case OK:
                break;
        }

        final long lastGetSrsSystemsSuccess = db.propertiesDao().getLastSrsSystemSyncSuccessDate();
        if (lastGetSrsSystemsSuccess == 0
                || System.currentTimeMillis() - lastGetSrsSystemsSuccess > WEEK) {
            db.assertGetSrsSystemsTask();
        }

        final long lastGetLevelProgressionSuccess = db.propertiesDao().getLastLevelProgressionSyncSuccessDate(0);
        if (lastGetLevelProgressionSuccess == 0
                || System.currentTimeMillis() - lastGetLevelProgressionSuccess > WEEK) {
            db.assertGetLevelProgressionTask();
        }

        final long lastGetSubjectsSuccess = db.propertiesDao().getLastSubjectSyncSuccessDate(0);
        if (lastGetSubjectsSuccess == 0
                || System.currentTimeMillis() - lastGetSubjectsSuccess > DAY) {
            db.assertGetSubjectsTask();
        }

        final long lastGetAssignmentsSuccess = db.propertiesDao().getLastAssignmentSyncSuccessDate(0);
        if (lastGetAssignmentsSuccess == 0
                || System.currentTimeMillis() - lastGetAssignmentsSuccess > HOUR - hourlySkew) {
            db.assertGetAssignmentsTask();
        }

        final long lastGetReviewStatisticsSuccess = db.propertiesDao().getLastReviewStatisticSyncSuccessDate(0);
        if (lastGetReviewStatisticsSuccess == 0
                || System.currentTimeMillis() - lastGetReviewStatisticsSuccess > HOUR - hourlySkew) {
            db.assertGetReviewStatisticsTask();
        }

        final long lastGetStudyMaterialsSuccess = db.propertiesDao().getLastStudyMaterialSyncSuccessDate(0);
        if (lastGetStudyMaterialsSuccess == 0
                || System.currentTimeMillis() - lastGetStudyMaterialsSuccess > HOUR - hourlySkew) {
            db.assertGetStudyMaterialsTask();
        }

        final long lastGetSummarySuccess = db.propertiesDao().getLastSummarySyncSuccessDate();
        if (lastGetSummarySuccess == 0
                || System.currentTimeMillis() - lastGetSummarySuccess > HOUR - hourlySkew) {
            db.assertGetSummaryTask();
        }

        if (db.propertiesDao().getReferenceDataVersion() != REFERENCE_DATA_VERSION) {
            db.loadReferenceData();
        }

        LiveApiState.getInstance().post(currentApiState);

        if (canTriggerApiTasks) {
            ApiTaskService.schedule();
        }
    }

    /**
     * Regular housekeeping tasks that should be run on every or nearly every background job.
     */
    protected static void houseKeeping() {
        final AppDatabase db = WkApplication.getDatabase();

        assertDueTasks(0);

        if (GlobalSettings.getFirstTimeSetup() == 0 && db.taskDefinitionDao().getApiCount() == 0) {
            GlobalSettings.setFirstTimeSetup(1);
            LiveFirstTimeSetup.getInstance().forceUpdate();
        }

        boolean timeLineNeedsUpdate = LiveTimeLine.getInstance().hasNullValue();
        final TimeLine timeLine = LiveTimeLine.getInstance().get();
        final long nowMinute = System.currentTimeMillis() / MINUTE;
        final long nowHour = nowMinute / 60;
        if (currentHour != nowHour) {
            timeLineNeedsUpdate = true;
        }
        if (currentMinute != nowMinute && !timeLine.hasAvailableReviews() && timeLine.hasUpcomingReviews()) {
            timeLineNeedsUpdate = true;
        }
        if (timeLineNeedsUpdate) {
            LiveTimeLine.getInstance().update();
            LiveAlertContext.getInstance().update();
            currentHour = nowHour;
            currentMinute = nowMinute;
        }

        DbLogger.trim();
    }

    /**
     * Run this job. This handles start/end logging and captures any exceptions thrown,
     * then delegates to the implementing subclass.
     */
    public final void run() {
        LOGGER.info("%s started with data: %s", DbLogger.getSimpleClassName(getClass()), data);
        safe(this::runLocal);
        LOGGER.info("%s finished", DbLogger.getSimpleClassName(getClass()));
    }

    /**
     * The actual action to be performed by this job. This is implemented by every concrete job class.
     */
    protected abstract void runLocal();
}
