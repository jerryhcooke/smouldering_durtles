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

import android.annotation.SuppressLint;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.SubjectEntity;
import com.lazy_durtles.wk.enums.SubjectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO for subjects.
 */
@Dao
public abstract class SubjectCollectionsDao {
    @SuppressLint("NewApi")
    private static List<Subject> buildList(final Collection<SubjectEntity> list) {
        return list.stream().map(Subject::new).collect(Collectors.toList());
    }

    /**
     * Room-generated method: get a list of all subjects unlocked after a cutoff date.
     *
     * @param cutoff the cutoff date
     * @return the list
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND object IS NOT NULL"
            + " AND unlockedAt != 0 AND unlockedAt >= :cutoff"
            + " ORDER BY unlockedAt DESC, level DESC, lessonPosition DESC, id DESC LIMIT 10")
    public abstract List<SubjectEntity> getRecentUnlocksHelper(final long cutoff);

    /**
     * Get a list of all subjects unlocked after a cutoff date.
     *
     * @param cutoff the cutoff date
     * @return the list
     */
    public final List<Subject> getRecentUnlocks(final long cutoff) {
        return buildList(getRecentUnlocksHelper(cutoff));
    }

    /**
     * Room-generated method: get a list of all subjects in the current session.
     *
     * @return the list
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND object IS NOT NULL"
            + " AND id IN (SELECT id FROM session_item)")
    public abstract List<SubjectEntity> getSessionSubjectsHelper();

    /**
     * Get a list of all subjects in the current session.
     *
     * @return the list
     */
    public final Map<Long, Subject> getSessionSubjects() {
        final Map<Long, Subject> result = new HashMap<>();
        for (final SubjectEntity entity: getSessionSubjectsHelper()) {
            result.put(entity.id, new Subject(entity));
        }
        return result;
    }

    /**
     * Get a list of non-passed subjects with a total score of less than 75%.
     *
     * @param filter the SRS stage filter
     * @return the list
     */
    public final List<Subject> getCriticalCondition(final String filter) {
        final String sql = String.format(Locale.ROOT, "SELECT * FROM subject"
                + " WHERE hiddenAt = 0 AND object IS NOT NULL AND %s"
                + " AND percentageCorrect < 75 AND unlockedAt != 0 AND reviewStatisticId != 0"
                + " ORDER BY percentageCorrect, lessonPosition DESC, id DESC LIMIT 10", filter);

        return getSubjectsWithRawQuery(new SimpleSQLiteQuery(sql));
    }

    /**
     * Get a list of all subjects burned after a cutoff date.
     *
     * @param filter the SRS stage filter
     * @param cutoff the cutoff date
     * @return the list
     */
    public final List<Subject> getBurnedItems(final String filter, final long cutoff) {
        final String sql = String.format(Locale.ROOT, "SELECT * FROM subject"
                + " WHERE hiddenAt = 0 AND %s AND object IS NOT NULL"
                + " AND burnedAt != 0 AND burnedAt >= ?"
                + " ORDER BY burnedAt DESC, level DESC, lessonPosition DESC, id DESC LIMIT 10", filter);

        return getSubjectsWithRawQuery(new SimpleSQLiteQuery(sql, new Object[] {cutoff}));
    }

    /**
     * Room-generated method: get a list of all subjects available for lesson.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @return the list
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND object IS NOT NULL"
            + " AND level <= :maxLevel"
            + " AND (resurrectedAt != 0 OR burnedAt = 0)"
            + " AND unlockedAt != 0 AND startedAt = 0"
            + " ORDER BY level, lessonPosition, id")
    protected abstract List<SubjectEntity> getAvailableLessonItemsHelper(final int maxLevel);

    /**
     * Get a list of all subjects available for lesson.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @return the list
     */
    public final List<Subject> getAvailableLessonItems(final int maxLevel) {
        return buildList(getAvailableLessonItemsHelper(maxLevel));
    }

    /**
     * Room-generated method: get a list of all subjects available for review, where the review
     * becomes/became available before the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the list
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND object IS NOT NULL"
            + " AND level <= :maxLevel"
            + " AND availableAt != 0 AND availableAt < :cutoff")
    protected abstract List<SubjectEntity> getUpcomingReviewItemsHelper(final int maxLevel, final long cutoff);

    /**
     * Get a list of all subjects available for review, where the review
     * becomes/became available before the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the list
     */
    public final List<Subject> getUpcomingReviewItems(final int maxLevel, final long cutoff) {
        return buildList(getUpcomingReviewItemsHelper(maxLevel, cutoff));
    }

    /**
     * Room-generated method: get all kanji for a given level.
     *
     * @param level the level
     * @return the kanji subjects for this level
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND object = 'kanji'"
            + " AND level = :level"
            + " ORDER BY lessonPosition, id")
    protected abstract List<SubjectEntity> getKanjiForLevelHelper(final int level);

    /**
     * Get all kanji for a given level.
     *
     * @param level the level
     * @return the kanji subjects for this level
     */
    private List<Subject> getKanjiForLevel(final int level) {
        return buildList(getKanjiForLevelHelper(level));
    }

    /**
     * Room-generated method: get the candidates for downloading pitch info data.
     *
     * @return the candidates
     */
    @Query("SELECT * FROM subject"
            + " WHERE hiddenAt = 0 AND (object = 'vocabulary' OR object = 'kana_vocabulary')"
            + " AND (pitchInfo IS NULL OR pitchInfo = '' OR pitchInfo LIKE '@%')")
    protected abstract List<SubjectEntity> getPitchInfoDownloadCandidatesHelper();

    /**
     * Get the candidates for downloading pitch info data.
     *
     * @return the candidates
     */
    public final List<Subject> getPitchInfoDownloadCandidates() {
        return buildList(getPitchInfoDownloadCandidatesHelper());
    }

    /**
     * Room-generated method: get the subjects for the given collection of IDs.
     *
     * @param ids the subject IDs to fetch
     * @return the list of subjects
     */
    @Query("SELECT * FROM subject"
            + " WHERE id in (:ids) AND hiddenAt = 0 AND object IS NOT NULL")
    protected abstract List<SubjectEntity> getByIdsHelper(Collection<Long> ids);

    /**
     * Get the subjects for the given collection of IDs. Fetch in batches of 100 to avoid SQL queries that are too long.
     *
     * @param ids the subject IDs to fetch
     * @return the list of subjects
     */
    public final List<Subject> getByIds(final Collection<Long> ids) {
        final List<Long> worklist = new ArrayList<>(ids);
        final List<Subject> result = new ArrayList<>();
        while (!worklist.isEmpty()) {
            final int num = Math.min(worklist.size(), 100);
            result.addAll(buildList(getByIdsHelper(worklist.subList(0, num))));
            worklist.subList(0, num).clear();
        }
        return result;
    }

    /**
     * Room-generated method: get a list of all subjects in a given range of levels.
     *
     * @param firstLevel the lowest level for requested subjects
     * @param lastLevel the lowest level for requested subjects
     * @return the list of subjects
     */
    @Query("SELECT * FROM subject"
            + " WHERE level >= :firstLevel AND level <= :lastLevel AND hiddenAt = 0 ORDER BY level, lessonPosition, id")
    protected abstract List<SubjectEntity> getByLevelRangeHelper(int firstLevel, int lastLevel);

    /**
     * Get a list of all subjects in a given range of levels.
     *
     * @param firstLevel the lowest level for requested subjects
     * @param lastLevel the lowest level for requested subjects
     * @return the list of subjects
     */
    public final List<Subject> getByLevelRange(final int firstLevel, final int lastLevel) {
        return buildList(getByLevelRangeHelper(firstLevel, lastLevel));
    }

    /**
     * Room-generated method: get a list of all subjects for a given level/type pair.
     *
     * @param level the level for the subjects
     * @param type the type for the subjects
     * @return the list of subjects
     */
    @Query("SELECT * FROM subject"
            + " WHERE level = :level AND object = :type AND hiddenAt = 0")
    protected abstract List<SubjectEntity> getLevelProgressSubjectsHelper(int level, SubjectType type);

    /**
     * Get a list of all subjects for a given level/type pair.
     *
     * @param level the level for the subjects
     * @param type the type for the subjects
     * @return the list of subjects
     */
    public final List<Subject> getLevelProgressSubjects(final int level, final SubjectType type) {
        return buildList(getLevelProgressSubjectsHelper(level, type));
    }

    /**
     * Room-generated method: get a list of subjects from a dynamically generated SQL query string.
     *
     * @param query the query to run
     * @return the list of subjects
     */
    @RawQuery
    protected abstract List<SubjectEntity> getSubjectsWithRawQueryHelper(final SupportSQLiteQuery query);

    /**
     * Room-generated method: get a list of subjects from a dynamically generated SQL query string.
     *
     * @param query the query to run
     * @return the list of subjects
     */
    public final List<Subject> getSubjectsWithRawQuery(final SupportSQLiteQuery query) {
        return buildList(getSubjectsWithRawQueryHelper(query));
    }

    /**
     * Get a collection of subject IDs that are on the level-up track: current-level kanji
     * and radicals that are locking away current-level kanji. Empty list if the user is
     * at max level.
     *
     * @param userLevel the user's level
     * @param maxLevel the max level allowed by the user's subscription
     * @return the collection of IDs
     */
    public final Collection<Long> getLevelUpIds(final int userLevel, final int maxLevel) {
        final Collection<Long> result = new HashSet<>();
        if (userLevel < maxLevel) {
            for (final Subject subject: getKanjiForLevel(userLevel)) {
                result.add(subject.getId());
                result.addAll(subject.getComponentSubjectIds());
            }
        }
        return result;
    }

    /**
     * Get the subject IDs for the subjects that have a specific number of stars.
     *
     * @param numStars the number of stars
     * @return the list of IDs
     */
    @Query("SELECT id FROM subject WHERE typeCode = :numStars ORDER BY id")
    public abstract List<Long> getStarredSubjectIds(final int numStars);
}
