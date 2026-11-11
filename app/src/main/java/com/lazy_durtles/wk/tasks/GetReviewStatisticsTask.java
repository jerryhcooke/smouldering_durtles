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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.api.model.ApiReviewStatistic;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.livedata.LiveApiProgress;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveCriticalCondition;

import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.util.TextUtil.formatTimestampForApi;

/**
 * Task to fetch any review statistics that have been updated since the last time this task was run.
 */
public final class GetReviewStatisticsTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 22;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetReviewStatisticsTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final long lastGetReviewStatisticsSuccess = db.propertiesDao().getLastReviewStatisticSyncSuccessDate(HOUR);

        LiveApiProgress.reset(true, "statistics");

        String uri = "/v2/review_statistics";
        if (lastGetReviewStatisticsSuccess != 0) {
            uri += "?updated_after=" + formatTimestampForApi(lastGetReviewStatisticsSuccess);
        }

        if (!collectionApiCall(uri, ApiReviewStatistic.class, t -> db.subjectSyncDao().insertOrUpdateReviewStatistic(t))) {
            return;
        }

        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.propertiesDao().setLastReviewStatisticSyncSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
        if (LiveApiProgress.getNumProcessedEntities() > 0) {
            LiveCriticalCondition.getInstance().update();
        }
    }
}
