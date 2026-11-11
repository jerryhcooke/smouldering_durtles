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

package com.lazy_durtles.wk.livedata;

import com.lazy_durtles.wk.Constants;
import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.Subject;

import java.util.Collections;
import java.util.List;

/**
 * LiveData that records up to the last 10 items unlocked in the last 30 days.
 */
public final class LiveRecentUnlocks extends ConservativeLiveData<List<Subject>> {
    /**
     * The singleton instance.
     */
    private static final LiveRecentUnlocks instance = new LiveRecentUnlocks();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveRecentUnlocks getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveRecentUnlocks() {
        //
    }

    @Override
    protected void updateLocal() {
        if (GlobalSettings.Dashboard.getShowRecentUnlocks() || hasNullValue()) {
            final AppDatabase db = WkApplication.getDatabase();
            final long cutoff = System.currentTimeMillis() - Constants.MONTH;
            instance.postValue(db.subjectCollectionsDao().getRecentUnlocks(cutoff));
        }
        else {
            ping();
        }
    }

    @Override
    public List<Subject> getDefaultValue() {
        return Collections.emptyList();
    }
}
