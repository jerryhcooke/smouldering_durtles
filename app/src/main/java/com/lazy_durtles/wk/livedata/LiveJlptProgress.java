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
import com.lazy_durtles.wk.model.JlptProgress;

/**
 * LiveData that records the JLPT progress.
 */
public final class LiveJlptProgress extends ConservativeLiveData<JlptProgress> {
    /**
     * The singleton instance.
     */
    private static final LiveJlptProgress instance = new LiveJlptProgress();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveJlptProgress getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveJlptProgress() {
        //
    }

    @SuppressLint("NewApi")
    @Override
    protected void updateLocal() {
        if (GlobalSettings.Dashboard.getShowJlptProgress() || hasNullValue()) {
            final AppDatabase db = WkApplication.getDatabase();
            final JlptProgress progress = new JlptProgress();
            db.subjectAggregatesDao().getJlptProgress().forEach(progress::addItem);
            instance.postValue(progress);
        }
        else {
            ping();
        }
    }

    @Override
    public JlptProgress getDefaultValue() {
        return new JlptProgress();
    }
}
