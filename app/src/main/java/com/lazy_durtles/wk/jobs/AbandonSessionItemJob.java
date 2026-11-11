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
import com.lazy_durtles.wk.db.model.SessionItem;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.enums.SessionItemState.ABANDONED;

/**
 * Job to abandon one session item in the active session. Sets the item state in the database
 * to ABANDONED, but doesn't remove it. That is left to the overall session cleanup.
 */
public final class AbandonSessionItemJob extends Job {
    private final long subjectId;

    /**
     * The constructor.
     *
     * @param data parameters
     */
    public AbandonSessionItemJob(final String data) {
        super(data);
        subjectId = Long.parseLong(data);
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable SessionItem item = db.sessionItemDao().getById(subjectId);
        if (item != null) {
            item.setState(ABANDONED);
            db.sessionItemDao().update(item);
        }
        houseKeeping();
    }
}
