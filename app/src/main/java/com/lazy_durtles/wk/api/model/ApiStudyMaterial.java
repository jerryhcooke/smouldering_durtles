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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Model class representing a user's study materials for a subject, as defined by the API.
 */
public final class ApiStudyMaterial implements WaniKaniEntity {
    @JsonIgnore private long id = 0L;
    @JsonProperty("meaning_note") private @Nullable String meaningNote = null;
    @JsonProperty("meaning_synonyms") private List<String> meaningSynonyms = Collections.emptyList();
    @JsonProperty("reading_note") private @Nullable String readingNote = null;
    @JsonProperty("subject_id") private long subjectId = 0;

    /**
     * The unique ID for this instance.
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
     * The user's meaning note.
     * @return the value
     */
    public @Nullable String getMeaningNote() {
        return meaningNote;
    }

    /**
     * The user's meaning note.
     * @param meaningNote the value
     */
    public void setMeaningNote(final @Nullable String meaningNote) {
        this.meaningNote = meaningNote;
    }

    /**
     * The user's meaning synonyms.
     * @return the value
     */
    public List<String> getMeaningSynonyms() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return meaningSynonyms;
    }

    /**
     * The user's meaning synonyms.
     * @param meaningSynonyms the value
     */
    public void setMeaningSynonyms(final List<String> meaningSynonyms) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.meaningSynonyms = meaningSynonyms;
    }

    /**
     * The user's reading note.
     * @return the value
     */
    public @Nullable String getReadingNote() {
        return readingNote;
    }

    /**
     * The user's reading note.
     * @param readingNote the value
     */
    public void setReadingNote(final @Nullable String readingNote) {
        this.readingNote = readingNote;
    }

    /**
     * The subject ID this instance is associated with.
     * @return the value
     */
    public long getSubjectId() {
        return subjectId;
    }

    /**
     * The subject ID this instance is associated with.
     * @param subjectId the value
     */
    public void setSubjectId(final long subjectId) {
        this.subjectId = subjectId;
    }
}
