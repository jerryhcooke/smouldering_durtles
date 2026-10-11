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

package com.smouldering_durtles.wk.tasks;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.util.FrequencyUtil;

/**
 * Task to download the JPDB frequency dictionary (only if it isn't in the local cache yet),
 * and then resolve the JPDB rank for all vocabulary subjects that don't have one yet.
 */
public final class DownloadJpdbFrequencyTask extends ApiTask {
    /**
     * Task priority. Lower priority than all API tasks and the Nadeshiko fetch task.
     */
    public static final int PRIORITY = 103;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public DownloadJpdbFrequencyTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        // Resolving from the local cache needs no network. The one-time 5 MB download follows the audio download policy.
        return !FrequencyUtil.needsJpdbDownload() || WkApplication.getInstance().getOnlineStatus().canDownloadAudio();
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        try {
            if (FrequencyUtil.needsJpdbDownload()) {
                if (!FrequencyUtil.downloadAndImportJpdb()) {
                    db.propertiesDao().setLastJpdbDownloadFailure(System.currentTimeMillis());
                    return;
                }
            }
            FrequencyUtil.resolveAllJpdbRanks();
        }
        finally {
            // Always remove the task, even on failure, so the task queue can never get stuck on it.
            db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        }
    }
}
