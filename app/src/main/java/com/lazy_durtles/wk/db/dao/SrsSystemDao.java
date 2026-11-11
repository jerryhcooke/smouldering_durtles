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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lazy_durtles.wk.db.model.SrsSystemDefinition;

import java.util.List;

/**
 * DAO for SRS systems.
 */
@Dao
public abstract class SrsSystemDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM srs_system")
    public abstract void deleteAll();

    /**
     * Room-generated method: get a list of all records.
     *
     * @return the list
     */
    @Query("SELECT * FROM srs_system ORDER BY id")
    public abstract List<SrsSystemDefinition> getAll();

    /**
     * Room-generated method: insert a new record.
     * @param system the record to insert
     */
    @Insert
    public abstract void insert(SrsSystemDefinition system);
}
