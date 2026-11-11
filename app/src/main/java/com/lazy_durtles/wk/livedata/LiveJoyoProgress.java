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

import android.annotation.SuppressLint;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.model.JoyoProgress;

/**
 * LiveData that records the Joyo kanji progress.
 */
public final class LiveJoyoProgress extends ConservativeLiveData<JoyoProgress> {
    /**
     * The singleton instance.
     */
    private static final LiveJoyoProgress instance = new LiveJoyoProgress();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveJoyoProgress getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveJoyoProgress() {
        //
    }

    @SuppressLint("NewApi")
    @Override
    protected void updateLocal() {
        if (GlobalSettings.Dashboard.getShowJoyoProgress() || hasNullValue()) {
            final AppDatabase db = WkApplication.getDatabase();
            final JoyoProgress progress = new JoyoProgress();
            db.subjectAggregatesDao().getJoyoProgress().forEach(progress::addItem);
            instance.postValue(progress);
        }
        else {
            ping();
        }
    }

    @Override
    public JoyoProgress getDefaultValue() {
        return new JoyoProgress();
    }
}
