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

package com.lazy_durtles.wk.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Model class representing a user's review statistics for a subject, as defined by the API.
 */
@SuppressWarnings("unused")
public final class ApiReviewStatistic implements WaniKaniEntity {
    private long id = 0;
    @JsonProperty("meaning_correct") private int meaningCorrect = 0;
    @JsonProperty("meaning_incorrect") private int meaningIncorrect = 0;
    @JsonProperty("meaning_max_streak") private int meaningMaxStreak = 0;
    @JsonProperty("meaning_current_streak") private int meaningCurrentStreak = 0;
    @JsonProperty("reading_correct") private int readingCorrect = 0;
    @JsonProperty("reading_incorrect") private int readingIncorrect = 0;
    @JsonProperty("reading_max_streak") private int readingMaxStreak = 0;
    @JsonProperty("reading_current_streak") private int readingCurrentStreak = 0;
    @JsonProperty("percentage_correct") private int percentageCorrect = 0;
    @JsonProperty("subject_id") private long subjectId = 0;

    /**
     * The unique ID for this record.
     * @return the value
     */
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public void setObject(final @Nullable String object) {
        //
    }

    /**
     * Number of times the meaning has been answered correctly.
     * @return the value
     */
    public int getMeaningCorrect() {
        return meaningCorrect;
    }

    /**
     * Number of times the meaning has been answered correctly.
     * @param meaningCorrect the value
     */
    public void setMeaningCorrect(final int meaningCorrect) {
        this.meaningCorrect = meaningCorrect;
    }

    /**
     * Number of times the meaning has been answered incorrectly.
     * @return the value
     */
    public int getMeaningIncorrect() {
        return meaningIncorrect;
    }

    /**
     * Number of times the meaning has been answered incorrectly.
     * @param meaningIncorrect the value
     */
    public void setMeaningIncorrect(final int meaningIncorrect) {
        this.meaningIncorrect = meaningIncorrect;
    }

    /**
     * The longest streak of correct meaning answers for this subject.
     * @return the value
     */
    public int getMeaningMaxStreak() {
        return meaningMaxStreak;
    }

    /**
     * The longest streak of correct meaning answers for this subject.
     * @param meaningMaxStreak the value
     */
    public void setMeaningMaxStreak(final int meaningMaxStreak) {
        this.meaningMaxStreak = meaningMaxStreak;
    }

    /**
     * The current streak of correct meaning answers for this subject.
     * @return the value
     */
    public int getMeaningCurrentStreak() {
        return meaningCurrentStreak;
    }

    /**
     * The current streak of correct meaning answers for this subject.
     * @param meaningCurrentStreak the value
     */
    public void setMeaningCurrentStreak(final int meaningCurrentStreak) {
        this.meaningCurrentStreak = meaningCurrentStreak;
    }

    /**
     * Number of times the reading has been answered correctly.
     * @return the value
     */
    public int getReadingCorrect() {
        return readingCorrect;
    }

    /**
     * Number of times the reading has been answered correctly.
     * @param readingCorrect the value
     */
    public void setReadingCorrect(final int readingCorrect) {
        this.readingCorrect = readingCorrect;
    }

    /**
     * Number of times the reading has been answered incorrectly.
     * @return the value
     */
    public int getReadingIncorrect() {
        return readingIncorrect;
    }

    /**
     * Number of times the reading has been answered incorrectly.
     * @param readingIncorrect the value
     */
    public void setReadingIncorrect(final int readingIncorrect) {
        this.readingIncorrect = readingIncorrect;
    }

    /**
     * The longest streak of correct reading answers for this subject.
     * @return the value
     */
    public int getReadingMaxStreak() {
        return readingMaxStreak;
    }

    /**
     * The longest streak of correct reading answers for this subject.
     * @param readingMaxStreak the value
     */
    public void setReadingMaxStreak(final int readingMaxStreak) {
        this.readingMaxStreak = readingMaxStreak;
    }

    /**
     * The current streak of correct reading answers for this subject.
     * @return the value
     */
    public int getReadingCurrentStreak() {
        return readingCurrentStreak;
    }

    /**
     * The current streak of correct reading answers for this subject.
     * @param readingCurrentStreak the value
     */
    public void setReadingCurrentStreak(final int readingCurrentStreak) {
        this.readingCurrentStreak = readingCurrentStreak;
    }

    /**
     * The overall percentage of correct answers for this subject.
     * @return the value
     */
    public int getPercentageCorrect() {
        return percentageCorrect;
    }

    /**
     * The overall percentage of correct answers for this subject.
     * @param percentageCorrect the value
     */
    public void setPercentageCorrect(final int percentageCorrect) {
        this.percentageCorrect = percentageCorrect;
    }

    /**
     * The ID of the subject this record applies to.
     * @return the value
     */
    public long getSubjectId() {
        return subjectId;
    }

    /**
     * The ID of the subject this record applies to.
     * @param subjectId the value
     */
    public void setSubjectId(final long subjectId) {
        this.subjectId = subjectId;
    }
}
