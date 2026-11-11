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

import com.fasterxml.jackson.core.type.TypeReference;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.model.ApiStudyMaterial;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.Converters;

import java.io.IOException;
import java.util.List;

import static com.lazy_durtles.wk.util.ObjectSupport.removeEmpty;

/**
 * Job to save an edit to the subject's study material to the database.
 * Also schedules a task to push the change to the API.
 */
public final class SaveStudyMaterialJob extends Job {
    private final List<String> values;
    private final long subjectId;
    private final String meaningNote;
    private final String readingNote;

    /**
     * The constructor.
     *
     * @param data parameters
     */
    public SaveStudyMaterialJob(final String data) {
        super(data);
        try {
            values = Converters.getObjectMapper().readValue(data, new TypeReference<List<String>>() {});
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
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        final ApiStudyMaterial material = new ApiStudyMaterial();
        material.setMeaningNote(meaningNote);
        material.setReadingNote(readingNote);
        material.setMeaningSynonyms(values);
        material.setSubjectId(subjectId);
        db.subjectSyncDao().insertOrUpdateStudyMaterial(material, true);

        db.assertSubmitStudyMaterialTask(data);

        houseKeeping();
    }
}
