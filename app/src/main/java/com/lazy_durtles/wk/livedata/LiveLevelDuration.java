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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.model.LevelDuration;

import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

/**
 * LiveData that tracks how long the user has been on their current level.
 */
public final class LiveLevelDuration extends ConservativeLiveData<LevelDuration> {
    /**
     * The singleton instance.
     */
    private static final LiveLevelDuration instance = new LiveLevelDuration();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveLevelDuration getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveLevelDuration() {
        //
    }

    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final int level = db.propertiesDao().getUserLevel();

        long since = db.levelProgressionDao().getLevelReachedDate(level);
        if (since == 0) {
            since = db.subjectAggregatesDao().getLevelReachedDate(level);
        }
        if (since == 0) {
            since = System.currentTimeMillis();
        }

        final String username = orElse(db.propertiesDao().getUsername(), "");

        final LevelDuration levelDuration = new LevelDuration(level, since, username);
        postValue(levelDuration);
    }

    @Override
    public LevelDuration getDefaultValue() {
        return new LevelDuration(0, System.currentTimeMillis(), "");
    }
}
