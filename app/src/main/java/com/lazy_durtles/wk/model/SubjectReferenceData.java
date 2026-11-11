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

package com.lazy_durtles.wk.model;

import com.lazy_durtles.wk.enums.SubjectType;

import javax.annotation.Nullable;

/**
 * Subset model of Subject that contains the characters and the reference data.
 * Used for bulk updates of reference data.
 */
public final class SubjectReferenceData {
    private final long id;
    private final @Nullable SubjectType type;
    private final @Nullable String characters;
    private final int frequency;
    private final int joyoGrade;
    private final int jlptLevel;
    private final @Nullable String pitchInfo;
    private final @Nullable String strokeData;

    /**
     * The constructor.
     *
     * @param id the subject ID
     * @param type subject field
     * @param characters subject field
     * @param frequency subject field
     * @param joyoGrade subject field
     * @param jlptLevel subject field
     * @param pitchInfo subject field
     * @param strokeData subject field
     */
    public SubjectReferenceData(final long id, final @Nullable SubjectType type, final @Nullable String characters,
                                final int frequency, final int joyoGrade, final int jlptLevel,
                                final @Nullable String pitchInfo, final @Nullable String strokeData) {
        this.id = id;
        this.type = type;
        this.characters = characters;
        this.frequency = frequency;
        this.joyoGrade = joyoGrade;
        this.jlptLevel = jlptLevel;
        this.pitchInfo = pitchInfo;
        this.strokeData = strokeData;
    }

    /**
     * The subject ID.
     * @return the value
     */
    public long getId() {
        return id;
    }

    /**
     * The subject's type.
     * @return the value
     */
    public SubjectType getType() {
        return type == null ? SubjectType.WANIKANI_RADICAL : type;
    }

    /**
     * The subject's characters, could be null for partially loaded subject and some radicals, but the default
     * reference data query explicitly excludes radicals.
     * @return the value
     */
    public @Nullable String getCharacters() {
        return characters;
    }

    /**
     * The frequency (1-2500) for the 2500 most commonly used kanji.
     * @return the value
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * The Joyo grade where this kanji is taught. 0 if not a Joyo kanji.
     * @return the value
     */
    public int getJoyoGrade() {
        return joyoGrade;
    }

    /**
     * The JLPT level this is expected to belong to. 0 if not in any level.
     * Since the JLPT scope per level is not published anymore, this is partly based on guesswork.
     * @return the value
     */
    public int getJlptLevel() {
        return jlptLevel;
    }

    /**
     * The pitch info for this subject. A null value means undetermined, an empty array means no
     * pitch info should be shown.
     * @return the value
     */
    public @Nullable String getPitchInfo() {
        return pitchInfo;
    }

    /**
     * The stroke data for this subject.
     * @return the value
     */
    public @Nullable String getStrokeData() {
        return strokeData;
    }
}
