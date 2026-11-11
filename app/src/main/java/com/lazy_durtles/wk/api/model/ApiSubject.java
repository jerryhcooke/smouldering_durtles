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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lazy_durtles.wk.components.WaniKaniApiDateDeserializer;
import com.lazy_durtles.wk.components.WaniKaniApiDateSerializer;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Model class representing a subject in the API.
 */
@SuppressWarnings("unused")
public final class ApiSubject implements WaniKaniEntity {
    private long id = 0L;
    private @Nullable String object = null;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("created_at") private long createdAt = 0;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("hidden_at") private long hiddenAt = 0;
    @JsonProperty("document_url") private @Nullable String documentUrl = null;
    @JsonProperty("lesson_position") private int lessonPosition = 0;
    @JsonProperty("spaced_repetition_system_id") private long srsSystemId = 0;
    private int level = 0;
    private @Nullable String characters = null;
    private @Nullable String slug = null;
    private List<Meaning> meanings = Collections.emptyList();
    @JsonProperty("meaning_mnemonic") private @Nullable String meaningMnemonic = null;
    @JsonProperty("meaning_hint") private @Nullable String meaningHint = null;
    @JsonProperty("auxiliary_meanings") private List<AuxiliaryMeaning> auxiliaryMeanings = Collections.emptyList();
    private List<Reading> readings = Collections.emptyList();
    @JsonProperty("reading_mnemonic") private @Nullable String readingMnemonic = null;
    @JsonProperty("reading_hint") private @Nullable String readingHint = null;
    @JsonProperty("component_subject_ids") private List<Long> componentSubjectIds = Collections.emptyList();
    @JsonProperty("amalgamation_subject_ids") private List<Long> amalgamationSubjectIds = Collections.emptyList();
    @JsonProperty("visually_similar_subject_ids") private List<Long> visuallySimilarSubjectIds = Collections.emptyList();
    @JsonProperty("parts_of_speech") private List<String> partsOfSpeech = Collections.emptyList();
    @JsonProperty("context_sentences") private List<ContextSentence> contextSentences = Collections.emptyList();
    @JsonProperty("pronunciation_audios") private List<PronunciationAudio> pronunciationAudios = Collections.emptyList();

    /**
     * The unique ID for this subject.
     * @return the value
     */
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * The type of subject, one of "radical", "kanji", "vocabulary", "kana_vocabulary".
     * @return the value
     */
    public @Nullable String getObject() {
        return object;
    }

    @Override
    public void setObject(final @Nullable String object) {
        this.object = object;
    }

    /**
     * Timestamp when this subject was created.
     * @return the value
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Timestamp when this subject was created.
     * @param createdAt the value
     */
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Timestamp when this subject was hidden, or 0 if it isn't hidden.
     * A hidden subject is still maintained, but is treated as non-existent almost everywhere.
     * @return the value
     */
    public long getHiddenAt() {
        return hiddenAt;
    }

    /**
     * Timestamp when this subject was hidden, or 0 if it isn't hidden.
     * A hidden subject is still maintained, but is treated as non-existent almost everywhere.
     * @param hiddenAt the value
     */
    public void setHiddenAt(final long hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    /**
     * The URL of the web page for this subject.
     * @return the value
     */
    public @Nullable String getDocumentUrl() {
        return documentUrl;
    }

    /**
     * The URL of the web page for this subject.
     * @param documentUrl the value
     */
    public void setDocumentUrl(final @Nullable String documentUrl) {
        this.documentUrl = documentUrl;
    }

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     * @return the value
     */
    public int getLessonPosition() {
        return lessonPosition;
    }

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     * @param lessonPosition the value
     */
    public void setLessonPosition(final int lessonPosition) {
        this.lessonPosition = lessonPosition;
    }

    /**
     * The ID of the SRS system that applies to this subject.
     * @return the value
     */
    public long getSrsSystemId() {
        return srsSystemId;
    }

    /**
     * The ID of the SRS system that applies to this subject.
     * @param srsSystemId the value
     */
    public void setSrsSystemId(final long srsSystemId) {
        this.srsSystemId = srsSystemId;
    }

    /**
     * The level this subject belongs to.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The level this subject belongs to.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     * @return the value
     */
    public @Nullable String getCharacters() {
        return characters;
    }

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     * @param characters the value
     */
    public void setCharacters(final @Nullable String characters) {
        this.characters = characters;
    }

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     * @return the value
     */
    public @Nullable String getSlug() {
        return slug;
    }

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     * @param slug the value
     */
    public void setSlug(final @Nullable String slug) {
        this.slug = slug;
    }

    /**
     * The registered meanings for this subject.
     * @return the value
     */
    public List<Meaning> getMeanings() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return meanings;
    }

    /**
     * The registered meanings for this subject.
     * @param meanings the value
     */
    public void setMeanings(final List<Meaning> meanings) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.meanings = meanings;
    }

    /**
     * The meaning mnemonic for this subject.
     * @return the value
     */
    public @Nullable String getMeaningMnemonic() {
        return meaningMnemonic;
    }

    /**
     * The meaning mnemonic for this subject.
     * @param meaningMnemonic the value
     */
    public void setMeaningMnemonic(final @Nullable String meaningMnemonic) {
        this.meaningMnemonic = meaningMnemonic;
    }

    /**
     * The meaning hint for this subject.
     * @return the value
     */
    public @Nullable String getMeaningHint() {
        return meaningHint;
    }

    /**
     * The meaning hint for this subject.
     * @param meaningHint the value
     */
    public void setMeaningHint(final @Nullable String meaningHint) {
        this.meaningHint = meaningHint;
    }

    /**
     * The registered auxiliary meanings for this subject.
     * @return the value
     */
    public List<AuxiliaryMeaning> getAuxiliaryMeanings() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return auxiliaryMeanings;
    }

    /**
     * The registered auxiliary meanings for this subject.
     * @param auxiliaryMeanings the value
     */
    public void setAuxiliaryMeanings(final List<AuxiliaryMeaning> auxiliaryMeanings) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.auxiliaryMeanings = auxiliaryMeanings;
    }

    /**
     * The registered readings for this subject.
     * @return the value
     */
    public List<Reading> getReadings() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return readings;
    }

    /**
     * The registered readings for this subject.
     * @param readings the value
     */
    public void setReadings(final List<Reading> readings) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.readings = readings;
    }

    /**
     * The reading mnemonic for this subject.
     * @return the value
     */
    public @Nullable String getReadingMnemonic() {
        return readingMnemonic;
    }

    /**
     * The reading mnemonic for this subject.
     * @param readingMnemonic the value
     */
    public void setReadingMnemonic(final @Nullable String readingMnemonic) {
        this.readingMnemonic = readingMnemonic;
    }

    /**
     * The reading hint for this subject.
     * @return the value
     */
    public @Nullable String getReadingHint() {
        return readingHint;
    }

    /**
     * The reading hint for this subject.
     * @param readingHint the value
     */
    public void setReadingHint(final @Nullable String readingHint) {
        this.readingHint = readingHint;
    }

    /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji.
     * @return the value
     */
    public List<Long> getComponentSubjectIds() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return componentSubjectIds;
    }

    /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji.
     * @param componentSubjectIds the value
     */
    public void setComponentSubjectIds(final List<Long> componentSubjectIds) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.componentSubjectIds = componentSubjectIds;
    }


    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in.
     * @return the value
     */
    public List<Long> getAmalgamationSubjectIds() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return amalgamationSubjectIds;
    }


    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in.
     * @param amalgamationSubjectIds the value
     */
    public void setAmalgamationSubjectIds(final List<Long> amalgamationSubjectIds) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.amalgamationSubjectIds = amalgamationSubjectIds;
    }

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab.
     * @return the value
     */
    public List<Long> getVisuallySimilarSubjectIds() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return visuallySimilarSubjectIds;
    }

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab.
     * @param visuallySimilarSubjectIds the value
     */
    public void setVisuallySimilarSubjectIds(final List<Long> visuallySimilarSubjectIds) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.visuallySimilarSubjectIds = visuallySimilarSubjectIds;
    }

    /**
     * This subject's parts of speech.
     * @return the value
     */
    public List<String> getPartsOfSpeech() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return partsOfSpeech;
    }

    /**
     * This subject's parts of speech.
     * @param partsOfSpeech the value
     */
    public void setPartsOfSpeech(final List<String> partsOfSpeech) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.partsOfSpeech = partsOfSpeech;
    }

    /**
     * The context sentences for this subject.
     * @return the value
     */
    public List<ContextSentence> getContextSentences() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return contextSentences;
    }

    /**
     * The context sentences for this subject.
     * @param contextSentences the value
     */
    public void setContextSentences(final List<ContextSentence> contextSentences) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.contextSentences = contextSentences;
    }

    /**
     * The audio for this vocab, empty for radicals and kanji.
     * @return the value
     */
    public List<PronunciationAudio> getPronunciationAudios() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return pronunciationAudios;
    }

    /**
     * The audio for this vocab, empty for radicals and kanji.
     * @param pronunciationAudios the value
     */
    public void setPronunciationAudios(final List<PronunciationAudio> pronunciationAudios) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.pronunciationAudios = pronunciationAudios;
    }
}
