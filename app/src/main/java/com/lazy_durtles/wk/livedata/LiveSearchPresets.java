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
import com.lazy_durtles.wk.db.model.SearchPreset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public final class LiveSearchPresets extends LiveData<List<SearchPreset>> {
    /**
     * The singleton instance.
     */
    private static final LiveSearchPresets instance = new LiveSearchPresets();

    /**
     * The backing LiveData created by Room.
     */
    private @Nullable LiveData<List<SearchPreset>> presets = null;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveSearchPresets getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveSearchPresets() {
        //
    }

    /**
     * Initialize this instance with the available database.
     */
    public void initialize() {
        safe(() -> {
            if (presets == null) {
                final AppDatabase db = WkApplication.getDatabase();
                presets = db.searchPresetDao().getLivePresets();
                presets.observeForever(t -> safe(() -> {
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
    private List<SearchPreset> get() {
        if (getValue() == null) {
            return Collections.emptyList();
        }
        return getValue();
    }

    /**
     * Get an ordered list of preset names, excluding hidden ones.
     *
     * @return the list of names
     */
    public List<String> getNames() {
        final List<String> result = new ArrayList<>();
        for (final SearchPreset preset: get()) {
            if (preset.name.startsWith("\u0000")) {
                continue;
            }
            result.add(preset.name);
        }
        return result;
    }

    /**
     * Get a preset by name.
     *
     * @param name the name
     * @return the preset or null if not found
     */
    public @Nullable SearchPreset getByName(final String name) {
        for (final SearchPreset preset: get()) {
            if (preset.name.equals(name)) {
                return preset;
            }
        }
        return null;
    }
}
