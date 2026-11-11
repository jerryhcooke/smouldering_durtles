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
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.livedata.LiveApiProgress;
import com.lazy_durtles.wk.model.SubjectReferenceData;
import com.lazy_durtles.wk.util.ReferenceDataUtil;

import java.util.List;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.Constants.REFERENCE_DATA_VERSION;
import static com.lazy_durtles.wk.util.ObjectSupport.isEqual;

/**
 * Task to batch-update reference data. This is not a network task, the data is loaded
 * locally.
 */
public final class LoadReferenceDataTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 1;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public LoadReferenceDataTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return true;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        LiveApiProgress.reset(true, "reference data");

        LiveApiProgress.addEntities(0);
        final List<SubjectReferenceData> referenceData = db.subjectViewsDao().getReferenceData();
        LiveApiProgress.addEntities(referenceData.size());

        for (final SubjectReferenceData data: referenceData) {
            final int frequency = ReferenceDataUtil.getFrequency(data.getType(), data.getCharacters());
            final int joyoGrade = ReferenceDataUtil.getJoyoGrade(data.getType(), data.getCharacters());
            final int jlptLevel = ReferenceDataUtil.getJlptLevel(data.getType(), data.getCharacters());
            final @Nullable String pitchInfo = ReferenceDataUtil.getPitchInfo(data.getType(), data.getCharacters());
            final @Nullable String strokeData = ReferenceDataUtil.getStrokeData(data.getType(), data.getId(), data.getCharacters());
            if (frequency != data.getFrequency() || joyoGrade != data.getJoyoGrade() || jlptLevel != data.getJlptLevel()
                    || !isEqual(pitchInfo, data.getPitchInfo())|| !isEqual(strokeData, data.getStrokeData())) {
                db.subjectDao().updateReferenceData(data.getId(), frequency, joyoGrade, jlptLevel, pitchInfo, strokeData);
            }
            LiveApiProgress.addProcessedEntity();
        }

        db.propertiesDao().setReferenceDataVersion(REFERENCE_DATA_VERSION);

        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
    }
}
