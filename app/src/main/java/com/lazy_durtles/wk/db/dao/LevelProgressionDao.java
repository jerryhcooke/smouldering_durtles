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
import androidx.room.Query;

import com.lazy_durtles.wk.api.model.ApiLevelProgression;
import com.lazy_durtles.wk.db.model.LevelProgression;

import java.util.List;

import javax.annotation.Nullable;

/**
 * DAO for level progression records.
 */
@Dao
public abstract class LevelProgressionDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM level_progression")
    public abstract void deleteAll();

    /**
     * Room-generated method: get all records for a specific level.
     *
     * @param userLevel the level to fetch for
     * @return the list of records in no particular order
     */
    @Query("SELECT * FROM level_progression WHERE level = :userLevel")
    protected abstract List<LevelProgression> getForLevel(int userLevel);

    /**
     * Room-generated method: get a specific record.
     *
     * @param id the ID to fetch
     * @return the record or null if it doesn't exist
     */
    @Query("SELECT * FROM level_progression WHERE id = :id")
    public abstract @Nullable LevelProgression getById(long id);

    /**
     * Get the time when the user most recently started this level.
     *
     * @param userLevel the level to look for
     * @return the timestamp or null if not started or unknown.
     */
    public final long getLevelReachedDate(final int userLevel) {
        long since = 0;
        for (final LevelProgression lp: getForLevel(userLevel)) {
            final long date = lp.getSince();
            if (date == 0) {
                continue;
            }
            if (since == 0 || date > since) {
                since = date;
            }
        }
        return since;
    }

    /**
     * Update a record in the database if it exists, or create a new record.
     *
     * @param apiLevelProgression the API entity to take the data from
     */
    public final void insertOrUpdate(final ApiLevelProgression apiLevelProgression) {
        boolean exists = true;

        @Nullable LevelProgression lp = getById(apiLevelProgression.getId());
        if (lp == null) {
            lp = new LevelProgression();
            lp.setId(apiLevelProgression.getId());
            exists = false;
        }

        lp.setAbandonedAt(apiLevelProgression.getAbandonedAt());
        lp.setCompletedAt(apiLevelProgression.getCompletedAt());
        lp.setCreatedAt(apiLevelProgression.getCreatedAt());
        lp.setPassedAt(apiLevelProgression.getPassedAt());
        lp.setStartedAt(apiLevelProgression.getStartedAt());
        lp.setUnlockedAt(apiLevelProgression.getUnlockedAt());
        lp.setLevel(apiLevelProgression.getLevel());

        if (exists) {
            update(lp);
        }
        else {
            insert(lp);
        }
    }

    /**
     * Room-generated method: insert a row.
     *
     * @param id entity field
     * @param abandonedAt entity field
     * @param completedAt entity field
     * @param createdAt entity field
     * @param passedAt entity field
     * @param startedAt entity field
     * @param unlockedAt entity field
     * @param level entity field
     */
    @Query("INSERT INTO level_progression (id, abandonedAt, completedAt, createdAt, passedAt, startedAt, unlockedAt, level) VALUES"
            + " (:id, :abandonedAt, :completedAt, :createdAt, :passedAt, :startedAt, :unlockedAt, :level)")
    protected abstract void insertHelper(final long id, final long abandonedAt, final long completedAt, final long createdAt,
                                         final long passedAt, final long startedAt, final long unlockedAt, final int level);

    private void insert(final LevelProgression levelProgression) {
        insertHelper(levelProgression.getId(), levelProgression.getAbandonedAt(), levelProgression.getCompletedAt(),
                levelProgression.getCreatedAt(), levelProgression.getPassedAt(), levelProgression.getStartedAt(),
                levelProgression.getUnlockedAt(), levelProgression.getLevel());
    }

    /**
     * Room-generated method: update a row.
     *
     * @param id ID of the row to update
     * @param abandonedAt entity field
     * @param completedAt entity field
     * @param createdAt entity field
     * @param passedAt entity field
     * @param startedAt entity field
     * @param unlockedAt entity field
     * @param level entity field
     */
    @Query("UPDATE level_progression SET abandonedAt=:abandonedAt, completedAt=:completedAt, createdAt=:createdAt, passedAt=:passedAt, "
            + "startedAt=:startedAt, unlockedAt=:unlockedAt, level=:level WHERE id = :id")
    protected abstract void updateHelper(final long id, final long abandonedAt, final long completedAt, final long createdAt,
                                         final long passedAt, final long startedAt, final long unlockedAt, final int level);

    private void update(final LevelProgression levelProgression) {
        updateHelper(levelProgression.getId(), levelProgression.getAbandonedAt(), levelProgression.getCompletedAt(),
                levelProgression.getCreatedAt(), levelProgression.getPassedAt(), levelProgression.getStartedAt(),
                levelProgression.getUnlockedAt(), levelProgression.getLevel());
    }
}
