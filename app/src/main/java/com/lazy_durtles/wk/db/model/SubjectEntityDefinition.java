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
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lazy_durtles.wk.enums.SubjectType;

import javax.annotation.Nullable;

/**
 * Room entity for the subject table. See LogRecordEntityDefinition for an explanation of why this class exists.
 */
@SuppressWarnings("unused")
@Entity(tableName = "subject")
public final class SubjectEntityDefinition {
    @PrimaryKey public long id = 0L;
    @ColumnInfo(name = "object")
    public @Nullable SubjectType type;
    @ColumnInfo(name = "typeCode")
    public int numStars = 0;
    public @Nullable Long hiddenAt;
    public int lessonPosition = 0;
    public long srsSystemId = 0L;
    @ColumnInfo(index = true) public int level = 0;
    @ColumnInfo(index = true) public @Nullable String characters;
    public @Nullable String slug;
    public @Nullable String documentUrl;
    public @Nullable String meanings;
    public @Nullable String meaningMnemonic;
    public @Nullable String meaningHint;
    public @Nullable String auxiliaryMeanings;
    public @Nullable String readings;
    public @Nullable String readingMnemonic;
    public @Nullable String readingHint;
    public @Nullable String componentSubjectIds;
    public @Nullable String amalgamationSubjectIds;
    public @Nullable String visuallySimilarSubjectIds;
    public @Nullable String partsOfSpeech;
    public @Nullable String contextSentences;
    public @Nullable String pronunciationAudios;
    @ColumnInfo(name = "audioDownloadStatus")
    public int unused3 = 0;
    public @Nullable String searchTarget;
    public @Nullable String smallSearchTarget;
    public long assignmentId = 0L;
    @ColumnInfo(index = true) public @Nullable Long availableAt;
    @ColumnInfo(index = true) public @Nullable Long burnedAt;
    public @Nullable Long passedAt;
    public @Nullable Long resurrectedAt;
    @ColumnInfo(index = true) public @Nullable Long startedAt;
    @ColumnInfo(index = true) public @Nullable Long unlockedAt;
    @ColumnInfo(name = "passed")
    public boolean unused5 = false;
    @ColumnInfo(name = "resurrected")
    public boolean unused2 = false;
    @ColumnInfo(index = true, name = "srsStage") public long srsStageId = 0L;
    @ColumnInfo(name = "levelProgressScore")
    public int unused4 = 0;
    public @Nullable Long lastIncorrectAnswer;
    public boolean assignmentPatched = false;
    public long studyMaterialId = 0L;
    public @Nullable String meaningNote;
    public @Nullable String meaningSynonyms;
    public @Nullable String readingNote;
    public boolean studyMaterialPatched = false;
    public long reviewStatisticId = 0L;
    public int meaningCorrect = 0;
    public int meaningIncorrect = 0;
    public int meaningMaxStreak = 0;
    public int meaningCurrentStreak = 0;
    public int readingCorrect = 0;
    public int readingIncorrect = 0;
    public int readingMaxStreak = 0;
    public int readingCurrentStreak = 0;
    public int percentageCorrect = 0;
    public int leechScore = 0;
    public boolean statisticPatched = false;
    public int frequency = 0;
    public int joyoGrade = 0;
    public int jlptLevel = 0;
    public @Nullable String pitchInfo;
    public @Nullable String strokeData;
}
