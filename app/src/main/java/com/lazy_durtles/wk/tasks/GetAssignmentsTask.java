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

package com.lazy_durtles.wk.tasks;

import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.util.TextUtil.formatTimestampForApi;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.api.model.ApiAssignment;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.livedata.LiveAlertContext;
import com.lazy_durtles.wk.livedata.LiveApiProgress;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveBurnedItems;
import com.lazy_durtles.wk.livedata.LiveCriticalCondition;
import com.lazy_durtles.wk.livedata.LiveJlptProgress;
import com.lazy_durtles.wk.livedata.LiveJoyoProgress;
import com.lazy_durtles.wk.livedata.LiveLevelDuration;
import com.lazy_durtles.wk.livedata.LiveLevelProgress;
import com.lazy_durtles.wk.livedata.LiveRecentUnlocks;
import com.lazy_durtles.wk.livedata.LiveSrsBreakDown;
import com.lazy_durtles.wk.livedata.LiveTimeLine;

/**
 * Task to fetch any assignments that have been updated since the last time this task was run.
 */
public final class GetAssignmentsTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 21;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetAssignmentsTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final long lastGetAssignmentsSuccess = db.propertiesDao().getLastAssignmentSyncSuccessDate(HOUR);

        LiveApiProgress.reset(true, "assignments");

        String uri = "/v2/assignments";
        if (lastGetAssignmentsSuccess != 0) {
            uri += "?updated_after=" + formatTimestampForApi(lastGetAssignmentsSuccess);
        }

        if (!collectionApiCall(uri, ApiAssignment.class, t -> db.subjectSyncDao().insertOrUpdateAssignment(t))) {
            return;
        }

        db.propertiesDao().setSyncReminder(false);
        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.propertiesDao().setLastAssignmentSyncSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
        if (LiveApiProgress.getNumProcessedEntities() > 0) {
            LiveTimeLine.getInstance().update();
            LiveSrsBreakDown.getInstance().update();
            LiveLevelProgress.getInstance().update();
            LiveJoyoProgress.getInstance().update();
            LiveJlptProgress.getInstance().update();
            LiveRecentUnlocks.getInstance().update();
            LiveCriticalCondition.getInstance().update();
            LiveBurnedItems.getInstance().update();
            LiveLevelDuration.getInstance().forceUpdate();
            LiveAlertContext.getInstance().update();
        }
    }
}
