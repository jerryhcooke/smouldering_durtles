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

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.util.AudioUtil;

/**
 * Job that schedules audio downloads for a bracket of 10 levels at a time.
 */
public final class StartAudioDownloadJob extends Job {
    /**
     * The constructor.
     *
     * @param data parameters
     */
    public StartAudioDownloadJob(final String data) {
        super(data);
    }

    @Override
    public void runLocal() {
        if (GlobalSettings.getFirstTimeSetup() != 0) {
            final AppDatabase db = WkApplication.getDatabase();

            final String[] args = data.split("\\|");
            final int firstLevel = Integer.parseInt(args[0]);
            final int lastLevel = Integer.parseInt(args[1]);

            final Iterable<String> locationValues = AudioUtil.getLocationValues();

            for (final Subject subject: db.subjectCollectionsDao().getByLevelRange(firstLevel, lastLevel)) {
                final int status = AudioUtil.findAudioDownloadStatus(subject.getLevel(), subject.getParsedPronunciationAudios(), locationValues);
                if (status == 1 || status == 2) {
                    db.assertDownloadAudioTask(subject);
                }
            }
        }

        houseKeeping();
    }
}
