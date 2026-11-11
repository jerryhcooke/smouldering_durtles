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
import com.lazy_durtles.wk.util.AudioUtil;

/**
 * Task to batch-updata audio download status. This is not a network task, the data is loaded
 * locally.
 */
public final class ScanAudioDownloadStatusTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 1;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public ScanAudioDownloadStatusTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return true;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        LiveApiProgress.reset(true, "scanning audio");
        LiveApiProgress.addEntities(0);

        final int maxLevel = db.subjectAggregatesDao().getMaxLevel();

        for (int i=0; i<maxLevel; i++) {
            AudioUtil.updateDownloadStatus(i);
        }

        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
    }
}
