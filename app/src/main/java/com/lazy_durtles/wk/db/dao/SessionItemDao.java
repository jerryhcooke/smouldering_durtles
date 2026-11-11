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
import androidx.room.Update;
import com.lazy_durtles.wk.db.model.SessionItem;

import java.util.List;

import javax.annotation.Nullable;

/**
 * DAO for session items. These are the persisten record of a session in progress.
 */
@Dao
public abstract class SessionItemDao {
    /**
     * Room-generated method: delete all items.
     */
    @Query("DELETE FROM session_item")
    public abstract void deleteAll();

    /**
     * Room-generated method: get all items.
     *
     * @return the list of items in order
     */
    @Query("SELECT * FROM session_item ORDER BY `order`")
    public abstract List<SessionItem> getAll();

    /**
     * Room-generated method: get a specific item by subject ID.
     *
     * @param id the subject ID
     * @return the item or null if not found
     */
    @Query("SELECT * FROM session_item WHERE id = :id LIMIT 1")
    public abstract @Nullable SessionItem getById(long id);

    /**
     * Room-generated method: insert a new item.
     *
     * @param sessionItem the item to insert
     */
    @Insert
    public abstract void insert(SessionItem sessionItem);

    /**
     * Room-generated method: update an item.
     *
     * @param sessionItem the item to update
     */
    @Update
    public abstract void update(SessionItem sessionItem);
}
