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

package com.smouldering_durtles.wk.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smouldering_durtles.wk.db.model.JpdbFrequency;
import com.smouldering_durtles.wk.db.model.NadeshikoCount;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.SubjectEntity;
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * DAO for the JPDB frequency and Nadeshiko count caches, and the denormalized copies of
 * those values on the subject table.
 */
@Dao
public abstract class FrequencyDao {
    /**
     * Room-generated method: insert a batch of JPDB entries. The dictionary is sorted by rank,
     * so on a duplicate (term, reading) the first (= most common) one is kept.
     *
     * @param entries the entries
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insertJpdb(List<JpdbFrequency> entries);

    /**
     * Room-generated method: delete all JPDB entries.
     */
    @Query("DELETE FROM jpdb_frequency")
    public abstract void deleteAllJpdb();

    /**
     * Room-generated method: count JPDB entries.
     *
     * @return the count
     */
    @Query("SELECT COUNT(*) FROM jpdb_frequency")
    public abstract int getJpdbCount();

    /**
     * Room-generated method: mark all subjects' JPDB ranks as unresolved, after a new dictionary revision was imported.
     */
    @Query("UPDATE subject SET jpdbRank = 0 WHERE jpdbRank != 0")
    public abstract void resetAllJpdbRanks();

    /**
     * Room-generated method: get all JPDB entries for a term.
     *
     * @param term the term
     * @return the entries
     */
    @Query("SELECT * FROM jpdb_frequency WHERE term = :term ORDER BY freqRank")
    public abstract List<JpdbFrequency> getJpdbByTerm(String term);

    /**
     * Room-generated method: get the cached Nadeshiko count for a term.
     *
     * @param term the term
     * @return the record or null if never fetched successfully
     */
    @Query("SELECT * FROM nadeshiko_count WHERE term = :term")
    public abstract @Nullable NadeshikoCount getNadeshikoCount(String term);

    /**
     * Room-generated method: store a Nadeshiko count.
     *
     * @param record the record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertNadeshikoCount(NadeshikoCount record);

    /**
     * Room-generated method: vocab subjects whose JPDB rank hasn't been resolved yet.
     *
     * @return the list
     */
    @Query("SELECT * FROM subject WHERE hiddenAt = 0 AND (object = 'vocabulary' OR object = 'kana_vocabulary')"
            + " AND characters IS NOT NULL AND jpdbRank = 0")
    protected abstract List<SubjectEntity> getUnresolvedJpdbSubjectsHelper();

    /**
     * Vocab subjects whose JPDB rank hasn't been resolved yet.
     *
     * @return the list
     */
    public final List<Subject> getUnresolvedJpdbSubjects() {
        return getUnresolvedJpdbSubjectsHelper().stream().map(Subject::new).collect(Collectors.toList());
    }

    /**
     * Room-generated method: set the JPDB rank for a subject.
     *
     * @param id the subject ID
     * @param rank the rank
     */
    @Query("UPDATE subject SET jpdbRank = :rank WHERE id = :id")
    protected abstract void updateJpdbRankHelper(long id, int rank);

    /**
     * Set the JPDB rank for a subject and notify listeners.
     *
     * @param id the subject ID
     * @param rank the rank
     */
    public final void updateJpdbRank(final long id, final int rank) {
        updateJpdbRankHelper(id, rank);
        SubjectChangeWatcher.getInstance().reportChange(id);
    }

    /**
     * Room-generated method: set the Nadeshiko count for a subject.
     *
     * @param id the subject ID
     * @param count the count
     */
    @Query("UPDATE subject SET nadeshikoCount = :count WHERE id = :id")
    protected abstract void updateNadeshikoCountHelper(long id, int count);

    /**
     * Set the Nadeshiko count for a subject and notify listeners.
     *
     * @param id the subject ID
     * @param count the count
     */
    public final void updateNadeshikoCount(final long id, final int count) {
        updateNadeshikoCountHelper(id, count);
        SubjectChangeWatcher.getInstance().reportChange(id);
    }
}
