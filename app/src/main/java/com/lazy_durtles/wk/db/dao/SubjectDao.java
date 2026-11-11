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

import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.SubjectEntity;
import com.lazy_durtles.wk.livedata.SubjectChangeWatcher;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * DAO for subjects.
 */
@Dao
public abstract class SubjectDao {
    /**
     * Room-generated method: delete all subjects.
     */
    @Query("DELETE FROM Subject")
    public abstract void deleteAll();

    /**
     * Room-generated method: get a single kanji subject by the characters column.
     *
     * @param characters the character to look for
     * @return the subject or null if not found
     */
    @Query("SELECT * FROM subject WHERE object = 'kanji' AND hiddenAt = 0 AND characters = :characters LIMIT 1")
    protected abstract @Nullable SubjectEntity getKanjiByCharactersHelper(String characters);

    /**
     * Get a single kanji subject by the characters column.
     *
     * @param characters the character to look for
     * @return the subject or null if not found
     */
    public final @Nullable Subject getKanjiByCharacters(final String characters) {
        final @Nullable SubjectEntity entity = getKanjiByCharactersHelper(characters);
        return entity == null ? null : new Subject(entity);
    }

    /**
     * Room-generated method: get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    @Query("SELECT * FROM subject WHERE id = :id")
    protected abstract @Nullable SubjectEntity getByIdHelper(long id);

    /**
     * Get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    public final @Nullable Subject getById(final long id) {
        final @Nullable SubjectEntity entity = getByIdHelper(id);
        return entity == null ? null : new Subject(entity);
    }

    /**
     * Room-generated methiod: update the reference data for a subject.
     *
     * @param id the subject ID
     * @param frequency the frequency
     * @param joyoGrade the Joyo grade
     * @param jlptLevel the JLPT level
     * @param pitchInfo the pitch info, encoded as a JSON string
     * @param strokeData the stroke data, encoded as a JSON string
     */
    @Query("UPDATE subject SET frequency = :frequency, joyoGrade = :joyoGrade, jlptLevel = :jlptLevel, pitchInfo = :pitchInfo,"
            + " strokeData = :strokeData WHERE id = :id")
    public abstract void updateReferenceData(final long id, final int frequency, final int joyoGrade, final int jlptLevel,
                                             @androidx.annotation.Nullable final String pitchInfo, @androidx.annotation.Nullable final String strokeData);

    /**
     * Room-generated method: update the last incorrect answer timestamp.
     * Only update if the new value is later than the previous value.
     *
     * @param id the subject ID
     * @param lastIncorrectAnswer the new timestamp
     */
    @Query("UPDATE subject SET lastIncorrectAnswer = :lastIncorrectAnswer "
            + "WHERE id = :id AND lastIncorrectAnswer < :lastIncorrectAnswer")
    public abstract void updateLastIncorrectAnswer(final long id, final long lastIncorrectAnswer);

    /**
     * Room-generated method: update the star rating.
     *
     * @param id the subject ID
     * @param numStars the new rating
     */
    @Query("UPDATE subject SET typeCode = :numStars WHERE id = :id")
    protected abstract void updateStarsHelper(final long id, final int numStars);

    /**
     * Update the star rating.
     *
     * @param id the subject ID
     * @param numStars the new rating
     */
    public final void updateStars(final long id, final int numStars) {
        updateStarsHelper(id, numStars);
        SubjectChangeWatcher.getInstance().reportChange(id);
    }

    /**
     * Room-generated method: clear the statisticPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET statisticPatched = 0 WHERE statisticPatched AND id in (:subjectIds)")
    public abstract void resolvePatchedReviewStatistics(Collection<Long> subjectIds);

    /**
     * Room-generated method: clear the assignmentPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET assignmentPatched = 0 WHERE assignmentPatched AND id in (:subjectIds)")
    public abstract void resolvePatchedAssignments(Collection<Long> subjectIds);

    /**
     * Room-generated method: clear the studyMaterialPatched flag from a collection of subjects.
     *
     * @param subjectIds the subject IDs
     */
    @Query("UPDATE subject SET studyMaterialPatched = 0 WHERE studyMaterialPatched AND id in (:subjectIds)")
    public abstract void resolvePatchedStudyMaterials(Collection<Long> subjectIds);
}
