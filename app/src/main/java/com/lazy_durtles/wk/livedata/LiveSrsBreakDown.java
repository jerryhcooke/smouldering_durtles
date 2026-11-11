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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.model.SrsBreakDown;
import com.lazy_durtles.wk.model.SrsSystem;
import com.lazy_durtles.wk.model.SrsSystemRepository;

/**
 * LiveData that tracks the SRS breakdown data for the dashboard.
 */
public final class LiveSrsBreakDown extends ConservativeLiveData<SrsBreakDown> {
    /**
     * The singleton instance.
     */
    private static final LiveSrsBreakDown instance = new LiveSrsBreakDown();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveSrsBreakDown getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveSrsBreakDown() {
        //
    }

    @SuppressLint("NewApi")
    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final SrsBreakDown breakDown = new SrsBreakDown();
        db.subjectViewsDao().getSrsBreakDownItems().forEach(item -> {
            final SrsSystem.Stage stage = SrsSystemRepository.getSrsSystem(item.getSystemId()).getStage(item.getStageId());
            breakDown.addCount(stage, item.getCount());
        });
        instance.postValue(breakDown);
    }

    @Override
    public SrsBreakDown getDefaultValue() {
        return new SrsBreakDown();
    }
}
