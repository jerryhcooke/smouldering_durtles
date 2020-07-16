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

package com.the_tinkering.wk.jobs;

import com.the_tinkering.wk.WkApplication;
import com.the_tinkering.wk.db.AppDatabase;

/**
 * Job that schedules audio downloads for a bracket of 10 levels at a time.
 */
public final class ScanAudioDownloadStatusJob extends Job {
    /**
     * The constructor.
     *
     * @param data parameters
     */
    public ScanAudioDownloadStatusJob(final String data) {
        super(data);
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        db.assertScanAudioDownloadStatusTask();
        houseKeeping();
    }
}