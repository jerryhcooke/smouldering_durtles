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

import java.util.ArrayList;
import java.util.Collection;

import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

/**
 * Task to fetch review statistics for a specific set of subjects that have had theirs
 * patched locally, but which haven't been updated from the API yet. This task is only
 * scheduled if the normal sync didn't resolve this already.
 */
public final class GetPatchedReviewStatisticsTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 22;

    private final String idList;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetPatchedReviewStatisticsTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
        idList = orElse(taskDefinition.getData(), "0");
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        LiveApiProgress.reset(true, "statistics");

        final String uri = "/v2/review_statistics?subject_ids=" + idList;
        if (!collectionApiCall(uri, ApiReviewStatistic.class, t -> db.subjectSyncDao().insertOrUpdateReviewStatistic(t))) {
            return;
        }

        final Collection<Long> subjectIds = new ArrayList<>();
        for (final String s: idList.split(",")) {
            subjectIds.add(Long.parseLong(s, 10));
        }
        db.subjectDao().resolvePatchedReviewStatistics(subjectIds);

        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
        if (LiveApiProgress.getNumProcessedEntities() > 0) {
            LiveCriticalCondition.getInstance().update();
        }
    }
}
