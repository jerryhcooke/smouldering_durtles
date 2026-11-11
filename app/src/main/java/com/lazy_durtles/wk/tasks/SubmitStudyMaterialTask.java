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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.api.model.ApiStudyMaterial;
import com.lazy_durtles.wk.api.model.ApiUpdateStudyMaterial;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.Converters;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.util.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.Constants.API_RETRY_DELAY;
import static com.lazy_durtles.wk.Constants.NUM_API_TRIES;
import static com.lazy_durtles.wk.util.ObjectSupport.orElse;
import static com.lazy_durtles.wk.util.ObjectSupport.removeEmpty;

/**
 * Task to report updated study materials to the API. This does either a create or an update,
 * depending on whether there are already study materials stored for this subject.
 */
public final class SubmitStudyMaterialTask extends ApiTask {
    private static final Logger LOGGER = Logger.get(SubmitStudyMaterialTask.class);

    /**
     * Task priority.
     */
    public static final int PRIORITY = 16;

    private final List<String> values;
    private final long subjectId;
    private final String meaningNote;
    private final String readingNote;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public SubmitStudyMaterialTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
        try {
            final @Nullable String dataValue = orElse(taskDefinition.getData(), "[]");
            values = Converters.getObjectMapper().readValue(dataValue, new TypeReference<List<String>>() {});
        } catch (final IOException e) {
            // This can't realistically happen.
            throw new IllegalArgumentException(e);
        }
        subjectId = Long.parseLong(values.remove(0));
        meaningNote = values.remove(0);
        readingNote = values.remove(0);
        removeEmpty(values);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable Subject subject = db.subjectDao().getById(subjectId);
        if (subject == null) {
            db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
            return;
        }

        if (subject.getStudyMaterialId() == 0) {
            final ApiUpdateStudyMaterial requestBody = new ApiUpdateStudyMaterial();
            requestBody.getStudyMaterial().setMeaningNote(meaningNote);
            requestBody.getStudyMaterial().setReadingNote(readingNote);
            requestBody.getStudyMaterial().setMeaningSynonyms(values);
            requestBody.getStudyMaterial().setSubjectId(subjectId);

            final String url = "/v2/study_materials";
            final @Nullable JsonNode responseBody = postApiCallWithRetry(url, "POST", requestBody, NUM_API_TRIES, API_RETRY_DELAY);
            if (responseBody != null && responseBody.has("id")) {
                try {
                    final @Nullable ApiStudyMaterial studyMaterial = parseEntity(responseBody, ApiStudyMaterial.class);
                    if (studyMaterial != null) {
                        db.subjectSyncDao().insertOrUpdateStudyMaterial(studyMaterial, false);
                    }
                } catch (final Exception e) {
                    LOGGER.error(e, "Error parsing create-study-material response");
                }
            }
        }
        else {
            final ApiUpdateStudyMaterial requestBody = new ApiUpdateStudyMaterial();
            requestBody.getStudyMaterial().setMeaningNote(meaningNote);
            requestBody.getStudyMaterial().setReadingNote(readingNote);
            requestBody.getStudyMaterial().setMeaningSynonyms(values);
            requestBody.getStudyMaterial().setSubjectId(subjectId);

            final String url = String.format(Locale.ROOT, "/v2/study_materials/%d", subject.getStudyMaterialId());
            final @Nullable JsonNode responseBody = postApiCallWithRetry(url, "PUT", requestBody, NUM_API_TRIES, API_RETRY_DELAY);
            if (responseBody != null && responseBody.has("id")) {
                try {
                    final @Nullable ApiStudyMaterial studyMaterial = parseEntity(responseBody, ApiStudyMaterial.class);
                    if (studyMaterial != null) {
                        db.subjectSyncDao().insertOrUpdateStudyMaterial(studyMaterial, false);
                    }
                } catch (final Exception e) {
                    LOGGER.error(e, "Error parsing update-study-material response");
                }
            }
        }

        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
    }
}
