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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.api.model.ApiSrsSystem;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.Converters;
import com.lazy_durtles.wk.db.dao.SrsSystemDao;
import com.lazy_durtles.wk.db.model.SrsSystemDefinition;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.livedata.LiveApiProgress;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveSrsSystems;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Task to fetch the SRS stages (not an incremental update, this is a full fetch every time).
 */
public final class GetSrsSystemsTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 10;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetSrsSystemsTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final SrsSystemDao srsSystemDao = db.srsSystemDao();

        LiveApiProgress.reset(true, "SRS systems");

        srsSystemDao.deleteAll();
        final String uri = "/v2/spaced_repetition_systems";
        if (!collectionApiCall(uri, ApiSrsSystem.class, t -> {
            final SrsSystemDefinition definition = new SrsSystemDefinition();
            definition.id = t.id;
            definition.name = t.name;
            definition.description = t.description;
            definition.unlockingStagePosition = t.unlockingStagePosition;
            definition.startingStagePosition = t.startingStagePosition;
            definition.passingStagePosition = t.passingStagePosition;
            definition.burningStagePosition = t.burningStagePosition;
            definition.stages = serializeToJsonString(t.stages);
            srsSystemDao.insert(definition);
        })) {
            return;
        }

        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.propertiesDao().setLastSrsSystemSyncSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
        LiveSrsSystems.getInstance().forceUpdate();
    }

    private static String serializeToJsonString(final @Nullable Collection<?> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            return Converters.getObjectMapper().writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
