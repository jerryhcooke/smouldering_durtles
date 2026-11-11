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

package com.lazy_durtles.wk.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.lazy_durtles.wk.db.model.SearchPreset;

import java.util.List;

/**
 * DAO for search presets.
 */
@Dao
public abstract class SearchPresetDao {
    /**
     * Room-generated method: set a preset.
     *
     * @param name the name of the preset
     * @param type the type of the preset
     * @param data the value of the preset
     */
    @Query("INSERT OR REPLACE INTO search_preset (name, type, data) VALUES (:name, :type, :data)")
    public abstract void setPreset(final String name, final int type, final String data);

    /**
     * Room-generated method: delete a preset.
     *
     * @param name the name of the preset
     */
    @Query("DELETE FROM search_preset WHERE name = :name")
    public abstract void deletePreset(final String name);

    /**
     * Room-generated method: get a LiveData instance containing the presets.
     *
     * @return the LiveData instance
     */
    @Query("SELECT * FROM search_preset ORDER BY name")
    public abstract LiveData<List<SearchPreset>> getLivePresets();
}
