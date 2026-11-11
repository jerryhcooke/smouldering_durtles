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

import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Dao;
import androidx.room.Query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lazy_durtles.wk.Constants;
import com.lazy_durtles.wk.api.model.ApiAssignment;
import com.lazy_durtles.wk.api.model.ApiReviewStatistic;
import com.lazy_durtles.wk.api.model.ApiStudyMaterial;
import com.lazy_durtles.wk.api.model.ApiSubject;
import com.lazy_durtles.wk.db.Converters;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.SubjectEntity;
import com.lazy_durtles.wk.enums.SubjectType;
import com.lazy_durtles.wk.livedata.SubjectChangeWatcher;
import com.lazy_durtles.wk.model.SrsSystem;
import com.lazy_durtles.wk.util.Logger;
import com.lazy_durtles.wk.util.ReferenceDataUtil;
import com.lazy_durtles.wk.util.SearchUtil;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

/**
 * DAO for subjects.
 */
@Dao
public abstract class SubjectSyncDao {
    private static final Logger LOGGER = Logger.get(SubjectSyncDao.class);

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
    private @Nullable Subject getById(final long id) {
        final @Nullable SubjectEntity entity = getByIdHelper(id);
        return entity == null ? null : new Subject(entity);
    }

    // Note: the following are a bunch of methods that offer a very convoluted way to insert and update
    // subjects in the database. It's ugly and could be a lot cleaner, but this approach makes the
    // first time setup (somewhat) acceptably fast.

    // Don't start on refactoring this until you're very sure you can do it, and you're ready for weird
    // and subtle bugs and have a plan for performance problems.

    /**
     * Helper method: serialize a generic collection to a JSON array expression. This is to more
     * efficiently store complex fields in the database: each complex field becomes just
     * a string and is de-serialized on demand.
     *
     * @param value the collection to serialize
     * @return the resulting String
     */
    private static String serializeToJsonString(final @Nullable Collection<?> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            return Converters.getObjectMapper().writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Room-generated method: try to update a subject record. This covers the core subject data
     * from the API, and the static reference data that is not user-specific.
     *
     * @param subjectId subject ID
     * @param object subject field
     * @param characters subject field
     * @param slug subject field
     * @param documentUrl subject field
     * @param meaningMnemonic subject field
     * @param meaningHint subject field
     * @param readingMnemonic subject field
     * @param readingHint subject field
     * @param searchTarget subject field
     * @param smallSearchTarget subject field
     * @param meanings subject field
     * @param auxiliaryMeanings subject field
     * @param readings subject field
     * @param componentSubjectIds subject field
     * @param amalgamationSubjectIds subject field
     * @param visuallySimilarSubjectIds subject field
     * @param partsOfSpeech subject field
     * @param contextSentences subject field
     * @param pronunciationAudios subject field
     * @param lessonPosition subject field
     * @param level subject field
     * @param hiddenAt subject field
     * @param frequency subject field
     * @param joyoGrade subject field
     * @param jlptLevel subject field
     * @param pitchInfo subject field
     * @param strokeData subject field
     * @param srsSystemId subject field
     * @return true if there was a record to update
     */
    @Query("UPDATE subject SET"
            + " object = :object,"
            + " characters = :characters,"
            + " slug = :slug,"
            + " documentUrl = :documentUrl,"
            + " meaningMnemonic = :meaningMnemonic,"
            + " meaningHint = :meaningHint,"
            + " readingMnemonic = :readingMnemonic,"
            + " readingHint = :readingHint,"
            + " searchTarget = :searchTarget,"
            + " smallSearchTarget = :smallSearchTarget,"
            + " meanings = :meanings,"
            + " auxiliaryMeanings = :auxiliaryMeanings,"
            + " readings = :readings,"
            + " componentSubjectIds = :componentSubjectIds,"
            + " amalgamationSubjectIds = :amalgamationSubjectIds,"
            + " visuallySimilarSubjectIds = :visuallySimilarSubjectIds,"
            + " partsOfSpeech = :partsOfSpeech,"
            + " contextSentences = :contextSentences,"
            + " pronunciationAudios = :pronunciationAudios,"
            + " lessonPosition = :lessonPosition,"
            + " level = :level,"
            + " hiddenAt = :hiddenAt,"
            + " frequency = :frequency,"
            + " joyoGrade = :joyoGrade,"
            + " jlptLevel = :jlptLevel,"
            + " pitchInfo = :pitchInfo,"
            + " strokeData = :strokeData,"
            + " srsSystemId = :srsSystemId"
            + " WHERE id = :subjectId")
    protected abstract int tryUpdateHelper(final long subjectId,
                                           final String object,
                                           @androidx.annotation.Nullable final String characters,
                                           @androidx.annotation.Nullable final String slug,
                                           @androidx.annotation.Nullable final String documentUrl,
                                           @androidx.annotation.Nullable final String meaningMnemonic,
                                           @androidx.annotation.Nullable final String meaningHint,
                                           @androidx.annotation.Nullable final String readingMnemonic,
                                           @androidx.annotation.Nullable final String readingHint,
                                           final String searchTarget,
                                           final String smallSearchTarget,
                                           final String meanings,
                                           final String auxiliaryMeanings,
                                           final String readings,
                                           final String componentSubjectIds,
                                           final String amalgamationSubjectIds,
                                           final String visuallySimilarSubjectIds,
                                           final String partsOfSpeech,
                                           final String contextSentences,
                                           final String pronunciationAudios,
                                           final int lessonPosition,
                                           final int level,
                                           final long hiddenAt,
                                           final int frequency,
                                           final int joyoGrade,
                                           final int jlptLevel,
                                           @androidx.annotation.Nullable final String pitchInfo,
                                           @androidx.annotation.Nullable final String strokeData,
                                           final long srsSystemId);

    /**
     * Try to update a subject record from an API subject instance.
     *
     * @param apiSubject the API subject to pull data from
     * @return true if there was a record to update
     */
    private boolean tryUpdate(final ApiSubject apiSubject) {
        final @Nullable SubjectType type = Converters.stringToSubjectType(apiSubject.getObject());
        final int count = tryUpdateHelper(
                apiSubject.getId(),
                apiSubject.getObject(),
                apiSubject.getCharacters(),
                apiSubject.getSlug(),
                apiSubject.getDocumentUrl(),
                apiSubject.getMeaningMnemonic(),
                apiSubject.getMeaningHint(),
                apiSubject.getReadingMnemonic(),
                apiSubject.getReadingHint(),
                SearchUtil.findSearchTarget(apiSubject),
                SearchUtil.findSmallSearchTarget(apiSubject),
                serializeToJsonString(apiSubject.getMeanings()),
                serializeToJsonString(apiSubject.getAuxiliaryMeanings()),
                serializeToJsonString(apiSubject.getReadings()),
                serializeToJsonString(apiSubject.getComponentSubjectIds()),
                serializeToJsonString(apiSubject.getAmalgamationSubjectIds()),
                serializeToJsonString(apiSubject.getVisuallySimilarSubjectIds()),
                serializeToJsonString(apiSubject.getPartsOfSpeech()),
                serializeToJsonString(apiSubject.getContextSentences()),
                serializeToJsonString(apiSubject.getPronunciationAudios()),
                apiSubject.getLessonPosition(),
                apiSubject.getLevel(),
                apiSubject.getHiddenAt(),
                ReferenceDataUtil.getFrequency(type, apiSubject.getCharacters()),
                ReferenceDataUtil.getJoyoGrade(type, apiSubject.getCharacters()),
                ReferenceDataUtil.getJlptLevel(type, apiSubject.getCharacters()),
                ReferenceDataUtil.getPitchInfo(type, apiSubject.getCharacters()),
                ReferenceDataUtil.getStrokeData(type, apiSubject.getId(), apiSubject.getCharacters()),
                apiSubject.getSrsSystemId()
        );
        return count > 0;
    }

    /**
     * Room-generated method: try to insert a new subject record. This covers the core subject data
     * from the API, and the static reference data that is not user-specific.
     *
     * @param subjectId the subject ID
     * @param object subject field
     * @param characters subject field
     * @param slug subject field
     * @param documentUrl subject field
     * @param meaningMnemonic subject field
     * @param meaningHint subject field
     * @param readingMnemonic subject field
     * @param readingHint subject field
     * @param searchTarget subject field
     * @param smallSearchTarget subject field
     * @param meanings subject field
     * @param auxiliaryMeanings subject field
     * @param readings subject field
     * @param componentSubjectIds subject field
     * @param amalgamationSubjectIds subject field
     * @param visuallySimilarSubjectIds subject field
     * @param partsOfSpeech subject field
     * @param contextSentences subject field
     * @param pronunciationAudios subject field
     * @param lessonPosition subject field
     * @param level subject field
     * @param hiddenAt subject field
     * @param frequency subject field
     * @param joyoGrade subject field
     * @param jlptLevel subject field
     * @param pitchInfo subject field
     * @param strokeData subject field
     * @param srsSystemId subject field
     */
    @Query("INSERT INTO subject"
            + " (id, object, characters, slug, documentUrl, meaningMnemonic, meaningHint, readingMnemonic, readingHint, searchTarget, smallSearchTarget,"
            + " meanings, auxiliaryMeanings, readings, componentSubjectIds, amalgamationSubjectIds, visuallySimilarSubjectIds,"
            + " partsOfSpeech, contextSentences, pronunciationAudios,"
            + " typeCode, lessonPosition, level, hiddenAt, frequency, joyoGrade, jlptLevel, pitchInfo, strokeData, srsSystemId,"
            + " assignmentId, passed, resurrected, srsStage, assignmentPatched, studyMaterialId, studyMaterialPatched,"
            + " reviewStatisticId, meaningCorrect, meaningIncorrect, meaningMaxStreak, meaningCurrentStreak,"
            + " readingCorrect, readingIncorrect, readingMaxStreak, readingCurrentStreak, percentageCorrect,"
            + " statisticPatched, leechScore, levelProgressScore, audioDownloadStatus,"
            + " resurrectedAt, burnedAt, unlockedAt, startedAt, passedAt, availableAt, lastIncorrectAnswer"
            + " )"
            + " VALUES (:subjectId, :object, :characters, :slug, :documentUrl, :meaningMnemonic, :meaningHint, :readingMnemonic, :readingHint,"
            + " :searchTarget, :smallSearchTarget,"
            + " :meanings, :auxiliaryMeanings, :readings, :componentSubjectIds, :amalgamationSubjectIds, :visuallySimilarSubjectIds,"
            + " :partsOfSpeech, :contextSentences, :pronunciationAudios,"
            + " 0, :lessonPosition, :level, :hiddenAt,"
            + " :frequency, :joyoGrade, :jlptLevel, :pitchInfo, :strokeData, :srsSystemId,"
            + " 0, 0, 0, -999, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,"
            + " 0, 0, 0, 0, 0, 0, 0"
            + ")")
    protected abstract void tryInsertHelper(final long subjectId,
                                            final String object,
                                            @androidx.annotation.Nullable final String characters,
                                            @androidx.annotation.Nullable final String slug,
                                            @androidx.annotation.Nullable final String documentUrl,
                                            @androidx.annotation.Nullable final String meaningMnemonic,
                                            @androidx.annotation.Nullable final String meaningHint,
                                            @androidx.annotation.Nullable final String readingMnemonic,
                                            @androidx.annotation.Nullable final String readingHint,
                                            final String searchTarget,
                                            final String smallSearchTarget,
                                            final String meanings,
                                            final String auxiliaryMeanings,
                                            final String readings,
                                            final String componentSubjectIds,
                                            final String amalgamationSubjectIds,
                                            final String visuallySimilarSubjectIds,
                                            final String partsOfSpeech,
                                            final String contextSentences,
                                            final String pronunciationAudios,
                                            final int lessonPosition,
                                            final int level,
                                            final long hiddenAt,
                                            final int frequency,
                                            final int joyoGrade,
                                            final int jlptLevel,
                                            @androidx.annotation.Nullable final String pitchInfo,
                                            @androidx.annotation.Nullable final String strokeData,
                                            final long srsSystemId);

    /**
     * Try to insert a subject record from an API subject instance.
     *
     * @param apiSubject the API subject to pull data from
     * @return true if the insert was successful (false if it already existed)
     */
    private boolean tryInsert(final ApiSubject apiSubject) {
        try {
            final @Nullable SubjectType type = Converters.stringToSubjectType(apiSubject.getObject());
            tryInsertHelper(
                    apiSubject.getId(),
                    apiSubject.getObject(),
                    apiSubject.getCharacters(),
                    apiSubject.getSlug(),
                    apiSubject.getDocumentUrl(),
                    apiSubject.getMeaningMnemonic(),
                    apiSubject.getMeaningHint(),
                    apiSubject.getReadingMnemonic(),
                    apiSubject.getReadingHint(),
                    SearchUtil.findSearchTarget(apiSubject),
                    SearchUtil.findSmallSearchTarget(apiSubject),
                    serializeToJsonString(apiSubject.getMeanings()),
                    serializeToJsonString(apiSubject.getAuxiliaryMeanings()),
                    serializeToJsonString(apiSubject.getReadings()),
                    serializeToJsonString(apiSubject.getComponentSubjectIds()),
                    serializeToJsonString(apiSubject.getAmalgamationSubjectIds()),
                    serializeToJsonString(apiSubject.getVisuallySimilarSubjectIds()),
                    serializeToJsonString(apiSubject.getPartsOfSpeech()),
                    serializeToJsonString(apiSubject.getContextSentences()),
                    serializeToJsonString(apiSubject.getPronunciationAudios()),
                    apiSubject.getLessonPosition(),
                    apiSubject.getLevel(),
                    apiSubject.getHiddenAt(),
                    ReferenceDataUtil.getFrequency(type, apiSubject.getCharacters()),
                    ReferenceDataUtil.getJoyoGrade(type, apiSubject.getCharacters()),
                    ReferenceDataUtil.getJlptLevel(type, apiSubject.getCharacters()),
                    ReferenceDataUtil.getPitchInfo(type, apiSubject.getCharacters()),
                    ReferenceDataUtil.getStrokeData(type, apiSubject.getId(), apiSubject.getCharacters()),
                    apiSubject.getSrsSystemId()
            );
        }
        catch (final SQLiteConstraintException e) {
            return false;
        }
        return true;
    }

    /**
     * Room-generated method: insert an empty subject into the database. This is used to prepare an update with
     * an assignment or something like that for which the subject doesn't exist yet. The subject will be
     * effectively useless until the core subject data is included as well.
     *
     * @param id the subject ID
     */
    @Query("INSERT INTO subject (id,"
            + " assignmentId, passed, resurrected, srsStage, assignmentPatched, studyMaterialId, studyMaterialPatched,"
            + " reviewStatisticId, meaningCorrect, meaningIncorrect, meaningMaxStreak, meaningCurrentStreak,"
            + " readingCorrect, readingIncorrect, readingMaxStreak, readingCurrentStreak, percentageCorrect,"
            + " statisticPatched, frequency, joyoGrade, jlptLevel, levelProgressScore, leechScore, srsSystemId,"
            + " resurrectedAt, burnedAt, unlockedAt, startedAt, passedAt, availableAt, hiddenAt, lastIncorrectAnswer"
            + ") VALUES (:id,"
            + " 0, 0, 0, -999, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,"
            + " 0, 0, 0, 0, 0, 0, 0, 0"
            + ")")
    protected abstract void tryInsertHelperIdOnly(final long id);

    /**
     * Try to insert an empty subject into the database, see tryInsertHelperIdOnly. Ignore the exception if the
     * subject already exists.
     *
     * @param id the subject ID
     */
    private void tryInsertIdOnly(final long id) {
        try {
            tryInsertHelperIdOnly(id);
        }
        catch (final SQLiteConstraintException e) {
            //
        }
    }

    /**
     * Insert or update an API subject depending on whether it exists in the database already.
     *
     * @param apiSubject the API subject
     * @param existingSubjectIds the set of existing subject IDs, to predict the likely (non-)existence of the subject
     */
    public final void insertOrUpdate(final ApiSubject apiSubject, final Collection<Long> existingSubjectIds) {
        if (existingSubjectIds.contains(apiSubject.getId())) {
            final boolean updated = tryUpdate(apiSubject);
            if (!updated) {
                tryInsert(apiSubject);
            }
        }
        else {
            final boolean inserted = tryInsert(apiSubject);
            if (!inserted) {
                tryUpdate(apiSubject);
            }
        }
        SubjectChangeWatcher.getInstance().reportChange(apiSubject.getId());
    }

    /**
     * Room-generated method: update a subject with data from an assignment.
     *
     * @param subjectId the subject ID
     * @param assignmentId the assignment ID
     * @param srsStageId the assignment field
     * @param availableAt the assignment field
     * @param burnedAt the assignment field
     * @param passedAt the assignment field
     * @param resurrectedAt the assignment field
     * @param startedAt the assignment field
     * @param unlockedAt the assignment field
     * @return 0 if the subject doesn't exist yet
     */
    @Query("UPDATE subject SET"
            + " assignmentId = :assignmentId,"
            + " srsStage = :srsStageId,"
            + " availableAt = :availableAt,"
            + " burnedAt = :burnedAt,"
            + " passedAt = :passedAt,"
            + " resurrectedAt = :resurrectedAt,"
            + " startedAt = :startedAt,"
            + " unlockedAt = :unlockedAt,"
            + " assignmentPatched = 0"
            + " WHERE id = :subjectId")
    protected abstract int tryUpdateHelperAssignment(final long subjectId,
                                                     final long assignmentId,
                                                     final long srsStageId,
                                                     final long availableAt,
                                                     final long burnedAt,
                                                     final long passedAt,
                                                     final long resurrectedAt,
                                                     final long startedAt,
                                                     final long unlockedAt);

    /**
     * Try to update a subject record from an API assignment instance.
     *
     * @param apiAssignment the API assignment to pull data from
     * @return true if there was a record to update
     */
    private boolean tryUpdateAssignment(final ApiAssignment apiAssignment) {
        final int count = tryUpdateHelperAssignment(
                apiAssignment.getSubjectId(),
                apiAssignment.getId(),
                apiAssignment.getUnlockedAt() == 0 ? -999 : apiAssignment.getSrsStageId(),
                apiAssignment.getAvailableAt(),
                apiAssignment.getBurnedAt(),
                apiAssignment.getPassedAt(),
                apiAssignment.getResurrectedAt(),
                apiAssignment.getStartedAt(),
                apiAssignment.getUnlockedAt()
        );
        return count > 0;
    }

    /**
     * Insert or update an API assignment depending on whether it exists in the database already.
     *
     * @param apiAssignment the API assignment
     */
    public final void insertOrUpdateAssignment(final ApiAssignment apiAssignment) {
        final boolean updated = tryUpdateAssignment(apiAssignment);
        if (!updated) {
            tryInsertIdOnly(apiAssignment.getSubjectId());
            tryUpdateAssignment(apiAssignment);
        }
        SubjectChangeWatcher.getInstance().reportChange(apiAssignment.getSubjectId());
    }

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param studyMaterialId the study material ID
     * @param meaningNote study material field
     * @param meaningSynonyms study material field
     * @param readingNote study material field
     * @return 0 if the subject doesn't exist yet
     */
    @Query("UPDATE subject SET"
            + " studyMaterialId = :studyMaterialId,"
            + " meaningNote = :meaningNote,"
            + " meaningSynonyms = :meaningSynonyms,"
            + " readingNote = :readingNote,"
            + " studyMaterialPatched = 0"
            + " WHERE id = :subjectId")
    protected abstract int tryUpdateHelperStudyMaterial(final long subjectId,
                                                        final long studyMaterialId,
                                                        @androidx.annotation.Nullable final String meaningNote,
                                                        final String meaningSynonyms,
                                                        @androidx.annotation.Nullable final String readingNote);

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param meaningNote study material field
     * @param meaningSynonyms study material field
     * @param readingNote study material field
     * @return 0 if the subject doesn't exist yet
     */
    @Query("UPDATE subject SET"
            + " meaningNote = :meaningNote,"
            + " meaningSynonyms = :meaningSynonyms,"
            + " readingNote = :readingNote,"
            + " studyMaterialPatched = 1"
            + " WHERE id = :subjectId")
    protected abstract int tryUpdateHelperStudyMaterial(final long subjectId,
                                                        @androidx.annotation.Nullable final String meaningNote,
                                                        final String meaningSynonyms,
                                                        @androidx.annotation.Nullable final String readingNote);

    /**
     * Try to update a subject record from an API study material instance.
     *
     * @param apiStudyMaterial the API study material to pull data from
     * @param patched if true, leave the study material ID in the subject alone
     * @return true if there was a record to update
     */
    private boolean tryUpdateStudyMaterial(final ApiStudyMaterial apiStudyMaterial, final boolean patched) {
        final int count = patched ? tryUpdateHelperStudyMaterial(
                apiStudyMaterial.getSubjectId(),
                apiStudyMaterial.getMeaningNote(),
                serializeToJsonString(apiStudyMaterial.getMeaningSynonyms()),
                apiStudyMaterial.getReadingNote()
        ) : tryUpdateHelperStudyMaterial(
                apiStudyMaterial.getSubjectId(),
                apiStudyMaterial.getId(),
                apiStudyMaterial.getMeaningNote(),
                serializeToJsonString(apiStudyMaterial.getMeaningSynonyms()),
                apiStudyMaterial.getReadingNote()
        );
        return count > 0;
    }

    /**
     * Insert or update an API study material depending on whether it exists in the database already.
     *
     * @param apiStudyMaterial the API study material
     * @param patched if true, leave the study material ID in the subject alone
     */
    public final void insertOrUpdateStudyMaterial(final ApiStudyMaterial apiStudyMaterial, final boolean patched) {
        final boolean updated = tryUpdateStudyMaterial(apiStudyMaterial, patched);
        if (!updated) {
            tryInsertIdOnly(apiStudyMaterial.getSubjectId());
            tryUpdateStudyMaterial(apiStudyMaterial, patched);
        }
        SubjectChangeWatcher.getInstance().reportChange(apiStudyMaterial.getSubjectId());
    }

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param reviewStatisticId the review statistic ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     * @param leechScore review statistic field
     * @return 0 if the subject doesn't exist yet
     */
    @Query("UPDATE subject SET"
            + " reviewStatisticId = :reviewStatisticId,"
            + " meaningCorrect = :meaningCorrect,"
            + " meaningIncorrect = :meaningIncorrect,"
            + " meaningCurrentStreak = :meaningCurrentStreak,"
            + " meaningMaxStreak = :meaningMaxStreak,"
            + " readingCorrect = :readingCorrect,"
            + " readingIncorrect = :readingIncorrect,"
            + " readingCurrentStreak = :readingCurrentStreak,"
            + " readingMaxStreak = :readingMaxStreak,"
            + " percentageCorrect = :percentageCorrect,"
            + " leechScore = :leechScore,"
            + " statisticPatched = 0"
            + " WHERE id = :subjectId")
    protected abstract int tryUpdateHelperReviewStatistic(final long subjectId,
                                           final long reviewStatisticId,
                                           final int meaningCorrect,
                                           final int meaningIncorrect,
                                           final int meaningCurrentStreak,
                                           final int meaningMaxStreak,
                                           final int readingCorrect,
                                           final int readingIncorrect,
                                           final int readingCurrentStreak,
                                           final int readingMaxStreak,
                                           final int percentageCorrect,
                                           final int leechScore);

    /**
     * Try to update a subject record from an API review statistic instance.
     *
     * @param apiReviewStatistic the API review statistic to pull data from
     * @return true if there was a record to update
     */
    private boolean tryUpdateReviewStatistic(final ApiReviewStatistic apiReviewStatistic) {
        double meaningStreak = Math.pow(apiReviewStatistic.getMeaningCurrentStreak(), 1.5);
        if (meaningStreak == 0) {
            meaningStreak = 0.5;
        }
        double readingStreak = Math.pow(apiReviewStatistic.getReadingCurrentStreak(), 1.5);
        if (readingStreak == 0) {
            readingStreak = 0.5;
        }
        final double meaningScore = apiReviewStatistic.getMeaningIncorrect() / meaningStreak;
        final double readingScore = apiReviewStatistic.getReadingIncorrect() / readingStreak;
        final int leechScore = (int) (1000 * Math.max(meaningScore, readingScore));
        final int count = tryUpdateHelperReviewStatistic(
                apiReviewStatistic.getSubjectId(),
                apiReviewStatistic.getId(),
                apiReviewStatistic.getMeaningCorrect(),
                apiReviewStatistic.getMeaningIncorrect(),
                apiReviewStatistic.getMeaningCurrentStreak(),
                apiReviewStatistic.getMeaningMaxStreak(),
                apiReviewStatistic.getReadingCorrect(),
                apiReviewStatistic.getReadingIncorrect(),
                apiReviewStatistic.getReadingCurrentStreak(),
                apiReviewStatistic.getReadingMaxStreak(),
                apiReviewStatistic.getPercentageCorrect(),
                leechScore
        );
        return count > 0;
    }

    /**
     * Insert or update an API review statistic depending on whether it exists in the database already.
     *
     * @param apiReviewStatistic the API review statistic
     */
    public final void insertOrUpdateReviewStatistic(final ApiReviewStatistic apiReviewStatistic) {
        final boolean updated = tryUpdateReviewStatistic(apiReviewStatistic);
        if (!updated) {
            tryInsertIdOnly(apiReviewStatistic.getSubjectId());
            tryUpdateReviewStatistic(apiReviewStatistic);
        }
        SubjectChangeWatcher.getInstance().reportChange(apiReviewStatistic.getSubjectId());
    }

    /**
     * Room-generated method: locally patch the assignment data for a record.
     *
     * @param subjectId the subject ID
     * @param srsStageId assignment field
     * @param unlockedAt assignment field
     * @param startedAt assignment field
     * @param availableAt assignment field
     * @param passedAt assignment field
     * @param burnedAt assignment field
     * @param resurrectedAt assignment field
     */
    @Query("UPDATE subject SET"
            + " srsStage = :srsStageId,"
            + " unlockedAt = :unlockedAt,"
            + " startedAt = :startedAt,"
            + " availableAt = :availableAt,"
            + " passedAt = :passedAt,"
            + " burnedAt = :burnedAt,"
            + " resurrectedAt = :resurrectedAt,"
            + " assignmentPatched = 1"
            + " WHERE id = :subjectId")
    protected abstract void patchAssignmentHelper(final long subjectId,
                                                  final long srsStageId,
                                                  final long unlockedAt,
                                                  final long startedAt,
                                                  final long availableAt,
                                                  final long passedAt,
                                                  final long burnedAt,
                                                  final long resurrectedAt);

    /**
     * Locally patch the assignment data for a record.
     *
     * @param subjectId the subject ID
     * @param srsStageId assignment field
     * @param unlockedAt assignment field
     * @param startedAt assignment field
     * @param availableAt assignment field
     * @param passedAt assignment field
     * @param burnedAt assignment field
     * @param resurrectedAt assignment field
     */
    public final void patchAssignment(final long subjectId,
                                      final long srsStageId,
                                      final long unlockedAt,
                                      final long startedAt,
                                      final long availableAt,
                                      final long passedAt,
                                      final long burnedAt,
                                      final long resurrectedAt) {
        LOGGER.info("Patch assignment: id:%d stage:%d unlockedAt:%s startedAt:%s availableAt:%s passedAt:%s burnedAt:%s resurrectedAt:%s",
                subjectId, srsStageId, unlockedAt, startedAt, availableAt, passedAt, burnedAt, resurrectedAt);
        patchAssignmentHelper(subjectId, unlockedAt == 0 ? -999 : srsStageId, unlockedAt, startedAt, availableAt, passedAt, burnedAt, resurrectedAt);
        SubjectChangeWatcher.getInstance().reportChange(subjectId);
    }

    /**
     * Room-generated method: locally patch the review statistic data for a record.
     *
     * @param subjectId the subject ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     * @param leechScore review statistic field
     */
    @Query("UPDATE subject SET"
            + " meaningCorrect = :meaningCorrect,"
            + " meaningIncorrect = :meaningIncorrect,"
            + " meaningCurrentStreak = :meaningCurrentStreak,"
            + " meaningMaxStreak = :meaningMaxStreak,"
            + " readingCorrect = :readingCorrect,"
            + " readingIncorrect = :readingIncorrect,"
            + " readingCurrentStreak = :readingCurrentStreak,"
            + " readingMaxStreak = :readingMaxStreak,"
            + " percentageCorrect = :percentageCorrect,"
            + " leechScore = :leechScore,"
            + " statisticPatched = 1"
            + " WHERE id = :subjectId")
    protected abstract void patchReviewStatisticHelper(final long subjectId,
                                                       final int meaningCorrect,
                                                       final int meaningIncorrect,
                                                       final int meaningCurrentStreak,
                                                       final int meaningMaxStreak,
                                                       final int readingCorrect,
                                                       final int readingIncorrect,
                                                       final int readingCurrentStreak,
                                                       final int readingMaxStreak,
                                                       final int percentageCorrect,
                                                       final int leechScore);

    /**
     * Locally patch the review statistic data for a record.
     *
     * @param subjectId the subject ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     */
    public final void patchReviewStatistic(final long subjectId,
                                           final int meaningCorrect,
                                           final int meaningIncorrect,
                                           final int meaningCurrentStreak,
                                           final int meaningMaxStreak,
                                           final int readingCorrect,
                                           final int readingIncorrect,
                                           final int readingCurrentStreak,
                                           final int readingMaxStreak,
                                           final int percentageCorrect) {
        double meaningStreak = Math.pow(meaningCurrentStreak, 1.5);
        if (meaningStreak == 0) {
            meaningStreak = 0.5;
        }
        double readingStreak = Math.pow(readingCurrentStreak, 1.5);
        if (readingStreak == 0) {
            readingStreak = 0.5;
        }
        final double meaningScore = meaningIncorrect / meaningStreak;
        final double readingScore = readingIncorrect / readingStreak;
        final int leechScore = (int) (1000 * Math.max(meaningScore, readingScore));
        patchReviewStatisticHelper(subjectId,
                meaningCorrect,
                meaningIncorrect,
                meaningCurrentStreak,
                meaningMaxStreak,
                readingCorrect,
                readingIncorrect,
                readingCurrentStreak,
                readingMaxStreak,
                percentageCorrect,
                leechScore);
        SubjectChangeWatcher.getInstance().reportChange(subjectId);
    }

    /**
     * Forcibly patch a subject to have an available lesson right now, if it doesn't have one.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param id the subject ID
     * @param unlockedAt the date to simulate for the unlock of this subject, if not set
     * @param maxLevel the max level granted by the user's subscription
     */
    public final void forceLessonAvailable(final long id, final long unlockedAt, final int maxLevel) {
        final @Nullable Subject subject = getById(id);
        if (subject == null || unlockedAt == 0 || subject.getLevel() > maxLevel) {
            return;
        }

        boolean changed = false;
        if (subject.getUnlockedAt() == 0) {
            subject.setUnlockedAt(unlockedAt);
            changed = true;
        }
        if (subject.getStartedAt() != 0) {
            subject.setStartedAt(0);
            changed = true;
        }
        SrsSystem.Stage stage = subject.getSrsStage();
        if (!stage.isInitial()) {
            stage = stage.getSystem().getInitialStage();
            subject.setSrsStage(stage);
            changed = true;
        }
        if (changed) {
            patchAssignment(id, stage.getId(), subject.getUnlockedAt(), subject.getStartedAt(),
                    subject.getAvailableAt(), subject.getPassedAt(), subject.getBurnedAt(), subject.getResurrectedAt());
        }
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
     * For selected subjects, forcibly patch them so no lesson is available for them.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param maxLevel the max level granted by the user's subscription
     * @param subjectIds the subject IDs to remove from the lesson pool
     */
    public final void forceLessonUnavailableExcept(final int maxLevel, final Collection<Long> subjectIds) {
        for (final SubjectEntity subject: getAvailableLessonItemsHelper(maxLevel)) {
            if (!subjectIds.contains(subject.id)) {
                patchAssignment(subject.id, subject.srsStageId, subject.unlockedAt, subject.unlockedAt,
                        subject.availableAt, subject.passedAt, subject.burnedAt, subject.resurrectedAt);
            }
        }
    }

    /**
     * Forcibly patch a subject to have an available review at the specified timestamp, if it doesn't have one.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param id the subject ID
     * @param availableAt the date to force for the availability of the review
     * @param maxLevel the max level granted by the user's subscription
     */
    public final void forceReviewAvailable(final long id, final long availableAt, final int maxLevel) {
        final @Nullable Subject subject = getById(id);
        if (subject == null || availableAt == 0 || subject.getLevel() > maxLevel) {
            return;
        }

        boolean changed = false;
        if (subject.getAvailableAt() == 0 || subject.getAvailableAt() > availableAt) {
            subject.setAvailableAt(availableAt);
            changed = true;
        }
        SrsSystem.Stage stage = subject.getSrsStage();
        if (subject.getUnlockedAt() == 0) {
            subject.setUnlockedAt(availableAt);
            stage = stage.getSystem().getFirstStartedStage();
            subject.setSrsStage(stage);
            changed = true;
        }
        if (subject.getStartedAt() == 0) {
            subject.setStartedAt(availableAt);
            changed = true;
        }
        if (stage.isCompleted()) {
            stage = stage.getSystem().getFirstStartedStage();
            subject.setSrsStage(stage);
            changed = true;
        }
        if (changed) {
            patchAssignment(id, stage.getId(), subject.getUnlockedAt(), subject.getStartedAt(),
                    subject.getAvailableAt(), subject.getPassedAt(), subject.getBurnedAt(), subject.getResurrectedAt());
        }
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
    protected abstract List<SubjectEntity> getPendingReviewItemsHelper(final int maxLevel, final long cutoff);

    /**
     * For selected subjects, forcibly patch them so no review is available for them in the next hour.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param maxLevel the max level granted by the user's subscription
     * @param subjectIds the subject IDs to remove from the review pool
     */
    public final void forceUpcomingReviewUnavailableExcept(final int maxLevel, final Collection<Long> subjectIds) {
        final long cutoff = System.currentTimeMillis() + Constants.HOUR;
        for (final SubjectEntity subject: getPendingReviewItemsHelper(maxLevel, cutoff)) {
            if (!subjectIds.contains(subject.id)) {
                patchAssignment(subject.id, subject.srsStageId, subject.unlockedAt, subject.startedAt,
                        0, subject.passedAt, subject.burnedAt, subject.resurrectedAt);
            }
        }
    }
}
