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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;

import static com.lazy_durtles.wk.enums.SessionType.NONE;

/**
 * Job to finish the current session, deleting all items from the database.
 */
public final class FinishSessionJob extends Job {
    /**
     * The constructor.
     *
     * @param data parameters
     */
    public FinishSessionJob(final String data) {
        super(data);
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        db.sessionItemDao().deleteAll();
        db.propertiesDao().setSessionType(NONE);
        db.propertiesDao().setSessionOnkun(false);
        db.propertiesDao().setCurrentItemId(0);
        houseKeeping();
    }
}
