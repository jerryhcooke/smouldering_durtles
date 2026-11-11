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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.model.TaskCounts;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.safe;

/**
 * A special case LiveData for information about existing task records in the database.
 *
 * <p>
 *     It must be possible for the main activity to observe this before the backing LiveData
 *     instance is created by Room. So this just delegates to the Room's LiveData as soon
 *     as that becomes available.
 * </p>
 */
public final class LiveTaskCounts extends LiveData<TaskCounts> {
    /**
     * The singleton instance.
     */
    private static final LiveTaskCounts instance = new LiveTaskCounts();

    /**
     * The backing LiveData created by Room.
     */
    private @Nullable LiveData<TaskCounts> counts = null;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveTaskCounts getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveTaskCounts() {
        //
    }

    /**
     * Initialize this instance with the available database.
     */
    public void initialize() {
        safe(() -> {
            if (counts == null) {
                final AppDatabase db = WkApplication.getDatabase();
                counts = db.taskDefinitionDao().getLiveCounts();
                counts.observeForever(t -> safe(() -> {
                    if (t != null) {
                        postValue(t);
                    }
                }));
                LiveApiProgress.getInstance().setSyncReminder(WkApplication.getDatabase().propertiesDao().getSyncReminder());
            }
        });
    }

    /**
     * Get the value, or a dummy instance if no value is available yet.
     *
     * @return the value
     */
    public TaskCounts get() {
        if (getValue() == null) {
            return new TaskCounts(0, 0, 0);
        }
        return getValue();
    }
}
