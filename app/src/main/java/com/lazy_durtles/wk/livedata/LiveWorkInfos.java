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

import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.services.BackgroundSyncWorker;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.safe;

/**
 * A special case LiveData for information about existing background workers in the database.
 *
 * <p>
 *     It must be possible for the main activity to observe this before the backing LiveData
 *     instance is created by Room. So this just delegates to the Room's LiveData as soon
 *     as that becomes available.
 * </p>
 */
public final class LiveWorkInfos extends LiveData<List<WorkInfo>> {
    /**
     * The singleton instance.
     */
    private static final LiveWorkInfos instance = new LiveWorkInfos();

    /**
     * The backing LiveData created by Room.
     */
    private @Nullable LiveData<List<WorkInfo>> backing = null;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveWorkInfos getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveWorkInfos() {
        //
    }

    /**
     * Initialize this instance with the available database.
     */
    public void initialize() {
        safe(() -> {
            if (backing == null) {
                backing = WorkManager.getInstance(WkApplication.getInstance()).getWorkInfosByTagLiveData(BackgroundSyncWorker.JOB_TAG_NEW);
                backing.observeForever(t -> safe(() -> {
                    if (t != null) {
                        postValue(t);
                    }
                }));
            }
        });
    }

    /**
     * Get the value, or a dummy instance if no value is available yet.
     *
     * @return the value
     */
    public List<WorkInfo> get() {
        if (getValue() == null) {
            return Collections.emptyList();
        }
        return getValue();
    }

    /**
     * Does this istance have a null value, i.e. has it not been initialized yet?.
     *
     * @return true if it does
     */
    public boolean hasNullValue() {
        return getValue() == null;
    }
}
