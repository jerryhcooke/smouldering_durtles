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
import com.lazy_durtles.wk.api.model.ApiSummary;
import com.lazy_durtles.wk.api.model.ApiSummarySession;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveTimeLine;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;

/**
 * Task to fetch the summary of available and upcoming lessons and reviews.
 * This is not directly used to schedule anything in the app, but it is used to
 * fix up situations where the app and API lose sync for some reason.
 */
public final class GetSummaryTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 25;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetSummaryTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        if (db.propertiesDao().getVacationMode()) {
            db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
            return;
        }

        final @Nullable ApiSummary summary = singleEntityApiCall("/v2/summary", ApiSummary.class);
        if (summary == null) {
            return;
        }

        final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();

        final Collection<Long> subjectIds = new HashSet<>();
        for (final ApiSummarySession session: summary.getLessons()) {
            if (session.getAvailableAt() == 0) {
                continue;
            }
            for (final long id: session.getSubjectIds()) {
                db.subjectSyncDao().forceLessonAvailable(id, session.getAvailableAt(), maxLevel);
                subjectIds.add(id);
            }
        }
        db.subjectSyncDao().forceLessonUnavailableExcept(maxLevel, subjectIds);

        subjectIds.clear();
        for (final ApiSummarySession session: summary.getReviews()) {
            if (session.getAvailableAt() == 0) {
                continue;
            }
            for (final long id: session.getSubjectIds()) {
                db.subjectSyncDao().forceReviewAvailable(id, session.getAvailableAt(), maxLevel);
                subjectIds.add(id);
            }
        }
        db.subjectSyncDao().forceUpcomingReviewUnavailableExcept(maxLevel, subjectIds);

        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.propertiesDao().setLastSummarySyncSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
        LiveTimeLine.getInstance().update();
    }
}
