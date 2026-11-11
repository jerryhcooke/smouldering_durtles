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

package com.lazy_durtles.wk.db.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import com.lazy_durtles.wk.api.model.AuxiliaryMeaning;
import com.lazy_durtles.wk.api.model.ContextSentence;
import com.lazy_durtles.wk.api.model.Meaning;
import com.lazy_durtles.wk.api.model.PronunciationAudio;
import com.lazy_durtles.wk.api.model.Reading;
import com.lazy_durtles.wk.model.PitchInfo;
import com.lazy_durtles.wk.enums.SubjectType;

import java.util.List;

import javax.annotation.Nullable;

// Pending changes for a future large-scale database update:
// - Remove resurrected boolean from assignment data
// - Remove audioDownloadStatus int
// - Remove levelProgressScore int
// - Reuse typeCode int for numStars

/**
 * Room entity for the subject table. This class combines all information about a subject in
 * a single entity for convenience - subject, assignment, review statistics, study materials
 * and reference data.
 */
public final class SubjectEntity {
    // From base subject

    /**
     * The unique ID.
     */
    public long id = 0L;

    /**
     * The type of subject, one of "radical", "kanji", "vocabulary", "kana_vocabulary".
     */
    @ColumnInfo(name = "object")
    public @Nullable SubjectType type;

    /**
     * The star rating (0-5) given to the subject by the user. This reuses the typeCode database column that
     * was retired a while ago. On the next 'recreate and clone' style DB migration this will be straightened out.
     */
    @SuppressWarnings("unused")
    @ColumnInfo(name = "typeCode")
    public int numStars = 0;

    /**
     * Timestamp when this subject was hidden, or null if it isn't hidden.
     * A hidden subject is still maintained, but is treated as non-existent almost everywhere.
     */
    public long hiddenAt = 0L;

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     */
    public int lessonPosition = 0;

    /**
     * The ID of the SRS system that applies to this subject.
     */
    public long srsSystemId = 0L;

    /**
     * The level this subject belongs to.
     */
    @ColumnInfo(index = true) public int level = 0;

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     */
    @ColumnInfo(index = true) public @Nullable String characters;

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     */
    public @Nullable String slug;

    /**
     * The URL for the weg page for this subject.
     */
    public @Nullable String documentUrl;

    /**
     * The registered meanings for this subject. Encoded as a JSON string.
     */
    public @Nullable String meanings;

    /**
     * The meaning mnemonic for this subject.
     */
    public @Nullable String meaningMnemonic;

    /**
     * The meaning hint for this subject.
     */
    public @Nullable String meaningHint;

    /**
     * The registered auxiliary meanings for this subject. Encoded as a JSON string.
     */
    public @Nullable String auxiliaryMeanings;

    /**
     * The registered readings for this subject. Encoded as a JSON string.
     */
    public @Nullable String readings;

    /**
     * The reading mnemonic for this subject.
     */
    public @Nullable String readingMnemonic;

    /**
     * The reading hint for this subject.
     */
    public @Nullable String readingHint;

    /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji. Encoded as a JSON string.
     */
    public @Nullable String componentSubjectIds;

    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in. Encoded as a JSON string.
     */
    public @Nullable String amalgamationSubjectIds;

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab. Encoded as a JSON string.
     */
    public @Nullable String visuallySimilarSubjectIds;

    /**
     * This subject's parts of speech. Encoded as a JSON string.
     */
    public @Nullable String partsOfSpeech;

    /**
     * The context sentences for this subject. Encoded as a JSON string.
     */
    public @Nullable String contextSentences;

    /**
     * The audio for this vocab, empty for radicals and kanji. Encoded as a JSON string.
     */
    public @Nullable String pronunciationAudios;

    /**
     * Unused - to be removed in a future DB change.
     */
    @ColumnInfo(name = "audioDownloadStatus")
    public int unused3 = 0;

    /**
     * A concatenation of all searchable text in this subject. Used to speed up searches.
     */
    public @Nullable String searchTarget;

    /**
     * A concatenation of the most important searchable text in this subject. Used to speed up searches.
     */
    public @Nullable String smallSearchTarget;

    /**
     * The unique ID of this subject's assignment, or 0 if it doesn't exist.
     */
    public long assignmentId = 0L;

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     */
    @ColumnInfo(index = true) public long availableAt = 0L;

    /**
     * The timestamp when this subject was burned, or null if it hasn't been burned yet.
     */
    @ColumnInfo(index = true) public long burnedAt = 0L;

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field used to be empty. They have been backfilled since then.
     */
    public long passedAt = 0L;

    /**
     * The timestamp when this subject was resurrected from burned status, or null if it hasn't been resurrected.
     */
    public long resurrectedAt = 0L;

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     */
    public long startedAt = 0L;

    /**
     * The timestamp when this subject was unlocked, or null if it is still locked.
     */
    public long unlockedAt = 0L;

    /**
     * True if this subject has passed, i.e. has reached Guru I at some point.
     */
    @ColumnInfo(name = "passed")
    public boolean unused5 = false;

    /**
     * Unused - to be removed in a future DB change.
     */
    @ColumnInfo(name = "resurrected")
    public boolean unused2 = false;

    /**
     * The current SRS stage for this subject.
     */
    @ColumnInfo(index = true, name = "srsStage") public long srsStageId = 0L;

    /**
     * The precomputed score for the level progression bars. Not used anymore.
     */
    @ColumnInfo(name = "levelProgressScore")
    public int unused4 = 0;

    /**
     * The timestamp when the last incorrect answer was given for this subject.
     */
    public long lastIncorrectAnswer = 0L;

    /**
     * True if this subject's assignment has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    public boolean assignmentPatched = false;

    // From study material

    /**
     * The unique ID of this subject's study material, or 0 if it doesn't exist.
     */
    public long studyMaterialId = 0L;

    /**
     * The user's meaning note.
     */
    public @Nullable String meaningNote;

    /**
     * The user's meaning synonyms. Encoded as a JSON string.
     */
    public @Nullable String meaningSynonyms;

    /**
     * The user's reading note.
     */
    public @Nullable String readingNote;

    /**
     * True if this subject's study material has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    public boolean studyMaterialPatched = false;

    // From review statistics

    /**
     * The unique ID of this subject's review statistics, or 0 if it doesn't exist.
     */
    public long reviewStatisticId = 0L;

    /**
     * Number of times the meaning has been answered correctly.
     */
    public int meaningCorrect = 0;

    /**
     * Number of times the meaning has been answered incorrectly.
     */
    public int meaningIncorrect = 0;

    /**
     * The longest streak of correct meaning answers for this subject.
     */
    public int meaningMaxStreak = 0;

    /**
     * The current streak of correct meaning answers for this subject.
     */
    public int meaningCurrentStreak = 0;

    /**
     * Number of times the reading has been answered correctly.
     */
    public int readingCorrect = 0;

    /**
     * Number of times the reading has been answered incorrectly.
     */
    public int readingIncorrect = 0;

    /**
     * The longest streak of correct reading answers for this subject.
     */
    public int readingMaxStreak = 0;

    /**
     * The current streak of correct reading answers for this subject.
     */
    public int readingCurrentStreak = 0;

    /**
     * The overall percentage of correct answers for this subject.
     */
    public int percentageCorrect = 0;

    /**
     * The leech score for this subject, precomputed for the self-study quiz filters.
     */
    public int leechScore = 0;

    /**
     * True if this subject's review statistic has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    public boolean statisticPatched = false;

    // From reference data

    /**
     * The frequency (1-2500) in everyday use of a kanji.
     */
    public int frequency = 0;

    /**
     * The Joyo grade where this kanji is first taught. 0 = not in Joyo, 7 = middle school.
     */
    public int joyoGrade = 0;

    /**
     * The JLPT level where this kanji/vocab is thought to be required. 0 = not in JLPT, 1-5 = N1-N5.
     */
    public int jlptLevel = 0;

    /**
     * The pitch info for this subject.
     */
    public @Nullable String pitchInfo;

    /**
     * The stroke order data for this subject.
     */
    public @Nullable String strokeData;

    /**
     * Parsed version of meanings, inflated on demand.
     */
    @Ignore public @Nullable List<Meaning> parsedMeanings;

    /**
     * Parsed version of auxiliaryMeanings, inflated on demand.
     */
    @Ignore public @Nullable List<AuxiliaryMeaning> parsedAuxiliaryMeanings;

    /**
     * Parsed version of readings, inflated on demand.
     */
    @Ignore public @Nullable List<Reading> parsedReadings;

    /**
     * Parsed version of componentSubjectIds, inflated on demand.
     */
    @Ignore public @Nullable List<Long> parsedComponentSubjectIds;

    /**
     * Parsed version of amalgamationSubjectIds, inflated on demand.
     */
    @Ignore public @Nullable List<Long> parsedAmalgamationSubjectIds;

    /**
     * Parsed version of visuallySimilarSubjectIds, inflated on demand.
     */
    @Ignore public @Nullable List<Long> parsedVisuallySimilarSubjectIds;

    /**
     * Parsed version of partsOfSpeech, inflated on demand.
     */
    @Ignore public @Nullable List<String> parsedPartsOfSpeech;

    /**
     * Parsed version of contextSentences, inflated on demand.
     */
    @Ignore public @Nullable List<ContextSentence> parsedContextSentences;

    /**
     * Parsed version of pronunciationAudios, inflated on demand.
     */
    @Ignore public @Nullable List<PronunciationAudio> parsedPronunciationAudios;

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @Ignore public @Nullable List<String> parsedMeaningSynonyms;

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @Ignore public @Nullable List<PitchInfo> parsedPitchInfo;

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @Ignore public @Nullable List<String> parsedStrokeData;
}
