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

package com.smouldering_durtles.wk.db.model;

import android.annotation.SuppressLint;
import android.os.Build;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.LegacyRadicals;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.api.model.AuxiliaryMeaning;
import com.smouldering_durtles.wk.api.model.ContextSentence;
import com.smouldering_durtles.wk.api.model.Meaning;
import com.smouldering_durtles.wk.api.model.PronunciationAudio;
import com.smouldering_durtles.wk.api.model.Reading;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.enums.KanjiAcceptedReadingType;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.enums.SubjectSource;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.model.PitchInfo;
import com.smouldering_durtles.wk.model.SrsSystem;
import com.smouldering_durtles.wk.model.SrsSystemRepository;
import com.smouldering_durtles.wk.util.PseudoIme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.enums.SubjectSource.WANIKANI;
import static com.smouldering_durtles.wk.util.ObjectSupport.getShortWaitTimeAsInformalString;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqual;
import static com.smouldering_durtles.wk.util.ObjectSupport.join;
import static com.smouldering_durtles.wk.util.ObjectSupport.removeDuplicates;
import static com.smouldering_durtles.wk.util.TextUtil.escapeHtml;
import static com.smouldering_durtles.wk.util.TextUtil.formatTimestampForDisplay;
import static com.smouldering_durtles.wk.util.TextUtil.renderHtml;
import static java.util.Objects.requireNonNull;

/**
 * Data class for the subject table that encapsulates the entity data more cleanly than the raw entity does.
 */
public final class Subject implements PronunciationAudioOwner {
    private static final Pattern NL_PATTERN = Pattern.compile("\n");

    /**
     * The backing entity for this subject.
     */
    private final SubjectEntity entity;
    private int ranking = 0;

    /**
     * The constructor.
     *
     * @param entity the backing entity
     */
    public Subject(final SubjectEntity entity) {
        this.entity = entity;

        entity.characters = internalize(entity.characters);
        entity.slug = internalize(entity.slug);
    }

    private static @Nullable String internalize(final @Nullable String value) {
        return value == null ? null : value.intern();
    }

    /**
     * Temporary storage for search result ranking.
     *
     * @return the ranking
     */
    public int getRanking() {
        return ranking;
    }

    /**
     * Temporary storage for search result ranking.
     *
     * @param ranking the ranking
     */
    public void setRanking(final int ranking) {
        this.ranking = ranking;
    }
    /*
     *************************************************************************************************************************************************
     * Identification
     *************************************************************************************************************************************************
     */

    /**
     * The subject's source, i.e. where does its definition come from?.
     *
     * @return the source
     */
    @SuppressWarnings({"SameReturnValue", "MethodMayBeStatic", "unused"})
    public SubjectSource getSource() {
        return WANIKANI;
    }

    /**
     * The subject's type (radical, kanji, ...).
     *
     * @return the type
     */
    public SubjectType getType() {
        final @Nullable SubjectType type = entity.type;
        if (type == null) {
            return SubjectType.WANIKANI_RADICAL;
        }
        return type;
    }

    /**
     * The subject's type order.
     *
     * @return the order
     */
    public int getTypeOrder() {
        return getType().getOrder();
    }

    @Override
    public long getId() {
        return entity.id;
    }

    @Override
    public int getLevel() {
        return entity.level;
    }

    /**
     * Does this subject need a question in slot 1?.
     *
     * @return true if it does
     */
    public boolean needsQuestion1() {
        return getType().supportsQuestion1() && hasAcceptedMeanings();
    }

    /**
     * Does this subject need a question in slot 2?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public boolean needsQuestion2(final boolean onkun) {
        return getType().supportsQuestion2(onkun) && hasAcceptedReadings();
    }

    /**
     * Does this subject need a question in slot 3?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public boolean needsQuestion3(final boolean onkun) {
        return getType().supportsQuestion3(onkun) && hasOnYomi();
    }

    /**
     * Does this subject need a question in slot 4?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public boolean needsQuestion4(final boolean onkun) {
        return getType().supportsQuestion4(onkun) && hasKunYomi();
    }

    /*
     *************************************************************************************************************************************************
     * The core of the subject's definition. Meanings, readings, audio, etc.
     *************************************************************************************************************************************************
     */

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     * @return the value
     */
    public int getLessonPosition() {
        return entity.lessonPosition;
    }

    /**
     * The ID of the SRS system that applies to this subject.
     * @return the value
     */
    public SrsSystem getSrsSystem() {
        return SrsSystemRepository.getSrsSystem(entity.srsSystemId);
    }

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     * @return the value
     */
    public @Nullable String getCharacters() {
        return entity.characters;
    }

    /**
     * Get the characters for this subject as HTML, tagged as Japanese or containing an image link if needed.
     *
     * @return the characters
     */
    public String getCharactersHtml() {
        if (needsTitleImage()) {
            final int imageId = getTitleImageId();
            return String.format(Locale.ROOT, "\u200C<title-image-%d>.</title-image-%d>", imageId, imageId);
        }
        if (entity.characters == null) {
            return Long.toString(entity.id);
        }
        return "<ja>" + entity.characters + "</ja>";
    }

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     * @return the value
     */
    public @Nullable String getSlug() {
        return entity.slug;
    }

    /**
     * The URL for the web site document for this subject.
     * @return the URL
     */
    public @Nullable String getDocumentUrl() {
        return entity.documentUrl;
    }

    /**
     * The meaning mnemonic for this subject.
     * @return the value
     */
    public @Nullable String getMeaningMnemonic() {
        return entity.meaningMnemonic;
    }

    /**
     * The meaning hint for this subject.
     * @return the value
     */
    public @Nullable String getMeaningHint() {
        return entity.meaningHint;
    }

    /**
     * The legacy name for this subject.
     * @return the value
     */
    private @Nullable String getLegacyName() {
        return LegacyRadicals.getLegacyName(getId());
    }

    /**
     * The legacy mnemonic for this subject.
     * @return the value
     */
    private @Nullable String getLegacyMnemonic() {
        return LegacyRadicals.getLegacyMnemonic(getId());
    }

    /**
     * Is this a radical with legacy name and mnemonic?.
     * @return true if it is
     */
    public boolean hasLegacy() {
        return LegacyRadicals.isLegacyRadical(getId());
    }

    /**
     * The reading mnemonic for this subject.
     * @return the value
     */
    public @Nullable String getReadingMnemonic() {
        return entity.readingMnemonic;
    }

    /**
     * The reading hint for this subject.
     * @return the value
     */
    public @Nullable String getReadingHint() {
        return entity.readingHint;
    }

    /**
     * The meanings for this subject, lazily parsed from JSON.
     * @return the value
     */
    public List<Meaning> getMeanings() {
        if (entity.parsedMeanings == null) {
            if (isEmpty(entity.meanings)) {
                entity.parsedMeanings = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedMeanings = Converters.getObjectMapper().readValue(
                            entity.meanings, new TypeReference<List<Meaning>>() {});
                } catch (final IOException e) {
                    entity.parsedMeanings = Collections.emptyList();
                }
            }
        }
        if (entity.parsedMeanings == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedMeanings;
    }

    /**
     * The auxiliary meanings for this subject, lazily parsed from JSON.
     * @return the value
     */
    public List<AuxiliaryMeaning> getAuxiliaryMeanings() {
        if (entity.parsedAuxiliaryMeanings == null) {
            if (isEmpty(entity.auxiliaryMeanings)) {
                entity.parsedAuxiliaryMeanings = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedAuxiliaryMeanings = Converters.getObjectMapper().readValue(
                            entity.auxiliaryMeanings, new TypeReference<List<AuxiliaryMeaning>>() {});
                } catch (final IOException e) {
                    entity.parsedAuxiliaryMeanings = Collections.emptyList();
                }
            }
        }
        if (entity.parsedAuxiliaryMeanings == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedAuxiliaryMeanings;
    }

    /**
     * The readings for this subject, lazily parsed from JSON.
     * @return the value
     */
    public List<Reading> getReadings() {
        if (entity.parsedReadings == null) {
            if (isEmpty(entity.readings)) {
                entity.parsedReadings = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedReadings = Converters.getObjectMapper().readValue(
                            entity.readings, new TypeReference<List<Reading>>() {});
                } catch (final IOException e) {
                    entity.parsedReadings = Collections.emptyList();
                }
            }
        }
        if (entity.parsedReadings == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedReadings;
    }

    /**
     /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji. Lazily parsed from JSON.
     * @return the value
     */
    public List<Long> getComponentSubjectIds() {
        if (entity.parsedComponentSubjectIds == null) {
            if (isEmpty(entity.componentSubjectIds)) {
                entity.parsedComponentSubjectIds = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedComponentSubjectIds = Converters.getObjectMapper().readValue(
                            entity.componentSubjectIds, new TypeReference<List<Long>>() {});
                    removeDuplicates(entity.parsedComponentSubjectIds);
                } catch (final IOException e) {
                    entity.parsedComponentSubjectIds = Collections.emptyList();
                }
            }
        }
        if (entity.parsedComponentSubjectIds == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedComponentSubjectIds;
    }

    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in. Lazily parsed from JSON.
     * @return the value
     */
    public List<Long> getAmalgamationSubjectIds() {
        if (entity.parsedAmalgamationSubjectIds == null) {
            if (isEmpty(entity.amalgamationSubjectIds)) {
                entity.parsedAmalgamationSubjectIds = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedAmalgamationSubjectIds = Converters.getObjectMapper().readValue(
                            entity.amalgamationSubjectIds, new TypeReference<List<Long>>() {});
                    removeDuplicates(entity.parsedAmalgamationSubjectIds);
                } catch (final IOException e) {
                    entity.parsedAmalgamationSubjectIds = Collections.emptyList();
                }
            }
        }
        if (entity.parsedAmalgamationSubjectIds == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedAmalgamationSubjectIds;
    }

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab. Lazily parsed from JSON.
     * @return the value
     */
    public List<Long> getVisuallySimilarSubjectIds() {
        if (entity.parsedVisuallySimilarSubjectIds == null) {
            if (isEmpty(entity.visuallySimilarSubjectIds)) {
                entity.parsedVisuallySimilarSubjectIds = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedVisuallySimilarSubjectIds = Converters.getObjectMapper().readValue(
                            entity.visuallySimilarSubjectIds, new TypeReference<List<Long>>() {});
                    removeDuplicates(entity.parsedVisuallySimilarSubjectIds);
                } catch (final IOException e) {
                    entity.parsedVisuallySimilarSubjectIds = Collections.emptyList();
                }
            }
        }
        if (entity.parsedVisuallySimilarSubjectIds == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedVisuallySimilarSubjectIds;
    }

    /**
     * This subject's parts of speech. Lazily parsed from JSON.
     * @return the value
     */
    public List<String> getPartsOfSpeech() {
        if (entity.parsedPartsOfSpeech == null) {
            if (isEmpty(entity.partsOfSpeech)) {
                entity.parsedPartsOfSpeech = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedPartsOfSpeech = Converters.getObjectMapper().readValue(
                            entity.partsOfSpeech, new TypeReference<List<String>>() {});
                } catch (final IOException e) {
                    entity.parsedPartsOfSpeech = Collections.emptyList();
                }
            }
        }
        if (entity.parsedPartsOfSpeech == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedPartsOfSpeech;
    }

    /**
     * The context sentences for this subject. Lazily parsed from JSON.
     * @return the value
     */
    public List<ContextSentence> getContextSentences() {
        if (entity.parsedContextSentences == null) {
            if (isEmpty(entity.contextSentences)) {
                entity.parsedContextSentences = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedContextSentences = Converters.getObjectMapper().readValue(
                            entity.contextSentences, new TypeReference<List<ContextSentence>>() {});
                } catch (final IOException e) {
                    entity.parsedContextSentences = Collections.emptyList();
                }
            }
        }
        if (entity.parsedContextSentences == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedContextSentences;
    }

    /**
     * The audio for this vocab, empty for radicals and kanji. Lazily parsed from JSON.
     * @return the value
     */
    @Override
    public List<PronunciationAudio> getParsedPronunciationAudios() {
        if (entity.parsedPronunciationAudios == null) {
            if (isEmpty(entity.pronunciationAudios)) {
                entity.parsedPronunciationAudios = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedPronunciationAudios = Converters.getObjectMapper().readValue(
                            entity.pronunciationAudios, new TypeReference<List<PronunciationAudio>>() {});
                } catch (final IOException e) {
                    entity.parsedPronunciationAudios = Collections.emptyList();
                }
            }
        }
        if (entity.parsedPronunciationAudios == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedPronunciationAudios;
    }

    /**
     * Is this a vocab that forms a prefix?.
     *
     * @return true if it is
     */
    public boolean isPrefix() {
        return entity.characters != null && entity.characters.endsWith("〜");
    }

    /**
     * Is this a vocab that forms a suffix?.
     *
     * @return true if it is
     */
    public boolean isSuffix() {
        return entity.characters != null && entity.characters.charAt(0) == '〜';
    }

    /**
     * Does this subject have a non-empty meaning mnemonic?.
     *
     * @return true if it does
     */
    public boolean hasMeaningMnemonic() {
        return !isEmpty(entity.meaningMnemonic);
    }

    /**
     * Does this subject have a non-empty meaning hint?.
     *
     * @return true if it does
     */
    public boolean hasMeaningHint() {
        return !isEmpty(entity.meaningHint);
    }

    /**
     * Get the accepted meanings for this subject (excluding auxiliary and user synonyms).
     *
     * @return the list
     */
    @SuppressLint("NewApi")
    private List<Meaning> getAcceptedMeanings() {
        return getMeanings().stream()
                .filter(Meaning::isAcceptedAnswer)
                .collect(Collectors.toList());
    }

    /**
     * Does this subject have any meanings?.
     *
     * @return true if it does
     */
    public boolean hasMeanings() {
        return !getMeanings().isEmpty();
    }

    /**
     * Does this subject have any meanings that are accepted as meaning answers?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    private boolean hasAcceptedMeanings() {
        return getMeanings().stream()
                .anyMatch(Meaning::isAcceptedAnswer);
    }

    /**
     * Get the number of accepted meaning answers for this subject.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    private int getNumAcceptedMeanings() {
        return (int) getMeanings().stream()
                .filter(Meaning::isAcceptedAnswer)
                .count();
    }

    /**
     * Get the 'best' meaning for this subject. Basically, this is the first primary meaning listed.
     *
     * @return the meaning
     */
    @SuppressLint("NewApi")
    public String getOneMeaning() {
        return getMeanings().stream().reduce((t, u) -> {
            if (t.isPrimary()) {
                return t;
            }
            if (u.isPrimary()) {
                return u;
            }
            if (t.isAcceptedAnswer()) {
                return t;
            }
            return u;
        }).map(Meaning::getMeaning).orElse("");
    }

    /**
     * Get the accepted readings for this subject.
     *
     * @return the list
     */
    @SuppressLint("NewApi")
    public List<Reading> getAcceptedReadings() {
        return getReadings().stream()
                .filter(Reading::isAcceptedAnswer)
                .collect(Collectors.toList());
    }

    /**
     * Does this subject have any readings?.
     *
     * @return true if it does
     */
    public boolean hasReadings() {
        return !getReadings().isEmpty();
    }

    /**
     * Does this subject have any readings that are accepted as reading answers?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    private boolean hasAcceptedReadings() {
        return getReadings().stream()
                .anyMatch(Reading::isAcceptedAnswer);
    }

    /**
     * Get the number of accepted reading answers for this subject.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    private int getNumAcceptedReadings() {
        return (int) getReadings().stream()
                .filter(Reading::isAcceptedAnswer)
                .count();
    }

    /**
     * Does this subject have a non-empty reading mnemonic?.
     *
     * @return true if it does
     */
    public boolean hasReadingMnemonic() {
        return !isEmpty(entity.readingMnemonic);
    }

    /**
     * Get the 'best' reading for this subject. Basically, this is the first primary reading listed.
     *
     * @return the meaning
     */
    @SuppressLint("NewApi")
    public String getOneReading() {
        return getReadings().stream().reduce((t, u) -> {
            if (t.isPrimary()) {
                return t;
            }
            if (u.isPrimary()) {
                return u;
            }
            if (t.isAcceptedAnswer()) {
                return t;
            }
            return u;
        }).map(reading -> reading.getValue(GlobalSettings.Other.getShowOnInKatakana())).orElse("");
    }

    /**
     * Does this subject have a non-empty reading hint?.
     *
     * @return true if it does
     */
    public boolean hasReadingHint() {
        return !isEmpty(entity.readingHint);
    }

    /**
     * Get the on'yomi readings for this subject.
     *
     * @return the list
     */
    @SuppressLint("NewApi")
    public List<Reading> getOnYomiReadings() {
        return getReadings().stream()
                .filter(Reading::isOnYomi)
                .collect(Collectors.toList());
    }

    /**
     * Get the kun'yomi readings for this subject.
     *
     * @return the list
     */
    @SuppressLint("NewApi")
    public List<Reading> getKunYomiReadings() {
        return getReadings().stream()
                .filter(Reading::isKunYomi)
                .collect(Collectors.toList());
    }

    /**
     * Does this subject have any on'yomi readings?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    public boolean hasOnYomi() {
        return getReadings().stream()
                .anyMatch(Reading::isOnYomi);
    }

    /**
     * Does this subject have any kun'yomi readings?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    public boolean hasKunYomi() {
        return getReadings().stream()
                .anyMatch(Reading::isKunYomi);
    }

    /**
     * Does this subject have any nanori readings?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    public boolean hasNanori() {
        return getReadings().stream()
                .anyMatch(Reading::isNanori);
    }

    /**
     * Does this subject have any accepted on'yomi readings?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    public boolean hasAcceptedOnYomi() {
        return getReadings().stream()
                .filter(Reading::isAcceptedAnswer)
                .anyMatch(Reading::isOnYomi);
    }

    /**
     * Does this subject have any accepted kun'yomi readings?.
     *
     * @return true if it does
     */
    @SuppressLint("NewApi")
    public boolean hasAcceptedKunYomi() {
        return getReadings().stream()
                .filter(Reading::isAcceptedAnswer)
                .anyMatch(Reading::isKunYomi);
    }

    /**
     * Is this string a primary accepted reading for this subject?.
     *
     * @param value the reading
     * @return true if it is
     */
    @SuppressLint("NewApi")
    public boolean isPrimaryReading(final @Nullable String value) {
        return getReadings().stream()
                .filter(Reading::isPrimary)
                .anyMatch(reading -> isEqual(reading.getReading(), value));
    }

    /**
     * Does this subject have any component subjects?.
     *
     * @return true if it does
     */
    public boolean hasComponents() {
        return !getComponentSubjectIds().isEmpty();
    }

    /**
     * Does this subject have any amalgamation subjects?.
     *
     * @return true if it does
     */
    public boolean hasAmalgamations() {
        return !getAmalgamationSubjectIds().isEmpty();
    }

    /**
     * Does this subject have any visually similar subjects?.
     *
     * @return true if it does
     */
    public boolean hasVisuallySimilar() {
        return !getVisuallySimilarSubjectIds().isEmpty();
    }

    /**
     * Does this subject have a non-empty list of parts of speech?.
     *
     * @return true if it does
     */
    public boolean hasPartsOfSpeech() {
        return !getPartsOfSpeech().isEmpty();
    }

    /**
     * Does this subject have any context sentences?.
     *
     * @return true if it does
     */
    public boolean hasContextSentences() {
        return !getContextSentences().isEmpty();
    }

    /**
     * Get the type of reading (on'yomi or kun'yomi) that is required to
     * answer a reading question for this subject.
     *
     * @return the type of reading
     */
    public KanjiAcceptedReadingType getKanjiAcceptedReadingType() {
        if (!getType().isKanji()) {
            return KanjiAcceptedReadingType.NEITHER;
        }

        final boolean onYomi = hasAcceptedOnYomi();
        final boolean kunYomi = hasAcceptedKunYomi();

        if (onYomi && kunYomi) {
            return KanjiAcceptedReadingType.BOTH;
        }

        if (onYomi) {
            return KanjiAcceptedReadingType.ONYOMI;
        }

        if (kunYomi) {
            return KanjiAcceptedReadingType.KUNYOMI;
        }

        return KanjiAcceptedReadingType.NEITHER;
    }

    /*
     *************************************************************************************************************************************************
     * Reference data for the subject that is not part of the core.
     *************************************************************************************************************************************************
     */

    /**
     * The frequency (1-2500) in everyday use of a kanji.
     * @return the value
     */
    public int getFrequency() {
        return entity.frequency;
    }

    /**
     * The Joyo grade where this kanji is first taught. 0 = not in Joyo, 7 = middle school.
     * @return the value
     */
    public int getJoyoGrade() {
        return entity.joyoGrade;
    }

    /**
     * The JLPT level where this kanji/vocab is thought to be required. 0 = not in JLPT, 1-5 = N1-N5.
     * @return the value
     */
    public int getJlptLevel() {
        return entity.jlptLevel;
    }

    private List<PitchInfo> getPitchInfo() {
        if (entity.parsedPitchInfo == null) {
            if (isEmpty(entity.pitchInfo)) {
                entity.parsedPitchInfo = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedPitchInfo = Converters.getObjectMapper().readValue(
                            entity.pitchInfo, new TypeReference<List<PitchInfo>>() {});
                } catch (final IOException e) {
                    entity.parsedPitchInfo = Collections.emptyList();
                }
            }
        }
        if (entity.parsedPitchInfo == null) {
            return Collections.emptyList();
        }
        return entity.parsedPitchInfo;
    }

    @SuppressLint("NewApi")
    private boolean hasPitchInfoFor(final CharSequence reading) {
        final String kana = requireNonNull(PseudoIme.toKatakana(reading));
        return getPitchInfo().stream().anyMatch(info -> isEqual(kana, info.getReading()));
    }

    @SuppressLint("NewApi")
    private boolean hasFallbackPitchInfo() {
        return getPitchInfo().stream().anyMatch(info -> info.getReading() == null);
    }

    /**
     * Get the pitch info records that apply to a specific reading.
     *
     * @param reading the reading to check
     * @return the list
     */
    @SuppressLint("NewApi")
    public List<PitchInfo> getPitchInfoFor(final CharSequence reading) {
        final String kana = requireNonNull(PseudoIme.toKatakana(reading));

        final List<PitchInfo> normalMatches = getPitchInfo().stream()
                .filter(info -> isEqual(kana, info.getReading()))
                .collect(Collectors.toList());

        if (!normalMatches.isEmpty()) {
            return normalMatches;
        }

        return getPitchInfo().stream()
                .filter(info -> info.getReading() == null)
                .collect(Collectors.toList());
    }

    /**
     * Does this subject need an attempt to download pitch info data?.
     *
     * @param delta the min time in ms between attempts
     * @return true if it does
     */
    public boolean needsPitchInfoDownload(final long delta) {
        if (!getType().canHavePitchInfo() || isEmpty(entity.characters)
                || entity.characters.charAt(0) == '〜' || entity.characters.endsWith("〜")) {
            return false;
        }
        if (isEmpty(entity.pitchInfo)) {
            return true;
        }
        if (entity.pitchInfo.charAt(0) != '@') {
            return false;
        }
        final long ts = Long.parseLong(entity.pitchInfo.substring(1));
        return (System.currentTimeMillis() - ts) > delta;
    }

    /**
     * Get the raw pitch info string stored in the database for this subject. Only used
     * for managing the pitch info reference data.
     *
     * @return the raw data as a JSON encoded string
     */
    public @Nullable String getRawPitchInfo() {
        return entity.pitchInfo;
    }

    /**
     * Does this subject have any pitch info records?.
     *
     * @return true if it does
     */
    public boolean hasPitchInfo() {
        if (!getType().canHavePitchInfo()) {
            return false;
        }

        for (final Reading reading: getReadings()) {
            if (isEmpty(reading.getReading())) {
                continue;
            }
            if (hasPitchInfoFor(reading.getReading())) {
                return true;
            }
        }

        return hasFallbackPitchInfo();
    }

    /**
     * Does this subject have stroke data?.
     *
     * @return true if it does
     */
    public boolean hasStrokeData() {
        return getType().canHaveStrokeData() && !getParsedStrokeData().isEmpty();
    }

    /**
     * The stroke data for this radical/kanji, empty for vocab. Lazily parsed from JSON.
     * @return the value
     */
    public List<String> getParsedStrokeData() {
        if (entity.parsedStrokeData == null) {
            if (isEmpty(entity.strokeData)) {
                entity.parsedStrokeData = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedStrokeData = Converters.getObjectMapper().readValue(
                            entity.strokeData, new TypeReference<List<String>>() {});
                } catch (final IOException e) {
                    entity.parsedStrokeData = Collections.emptyList();
                }
            }
        }
        if (entity.parsedStrokeData == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedStrokeData;
    }

    /**
     * Get the raw stroke data, encoded as a JSON string.
     *
     * @return the stroke data or null if none is available
     */
    public @Nullable String getStrokeData() {
        return entity.strokeData;
    }

    /*
     *************************************************************************************************************************************************
     * The user's assignment data for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * The unique ID of this subject's assignment, or 0 if it doesn't exist.
     * @return the value
     */
    public long getAssignmentId() {
        return entity.assignmentId;
    }

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     * @return the value
     */
    public long getAvailableAt() {
        return entity.availableAt;
    }

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     * @param availableAt the value
     */
    public void setAvailableAt(final long availableAt) {
        entity.availableAt = availableAt;
    }

    /**
     * The timestamp when this subject was burned, or 0 if it hasn't been burned yet.
     * @return the value
     */
    public long getBurnedAt() {
        return entity.burnedAt;
    }

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field is not filled in, but the passed boolean is
     * always reliable.
     * @return the value
     */
    public long getPassedAt() {
        return entity.passedAt;
    }

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     * @return the value
     */
    public long getStartedAt() {
        return entity.startedAt;
    }

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     * @param startedAt the value
     */
    public void setStartedAt(final long startedAt) {
        entity.startedAt = startedAt;
    }

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @return the value
     */
    public long getUnlockedAt() {
        return entity.unlockedAt;
    }

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @param unlockedAt the value
     */
    public void setUnlockedAt(final long unlockedAt) {
        entity.unlockedAt = unlockedAt;
    }

    /**
     * The timestamp when this subject was resurrected from burned status, or 0 if it hasn't been resurrected.
     * @return the value
     */
    public long getResurrectedAt() {
        return entity.resurrectedAt;
    }

    /**
     * The SRS stage for this subject.
     * @return the value
     */
    public SrsSystem.Stage getSrsStage() {
        return SrsSystemRepository.getSrsSystem(entity.srsSystemId).getStage(entity.srsStageId);
    }

    /**
     * The SRS stage for this subject.
     * @param srsStage the value
     */
    public void setSrsStage(final SrsSystem.Stage srsStage) {
        entity.srsStageId = srsStage.getId();
    }

    /**
     * True if this subject has passed, i.e. has reached Guru I at some point.
     * @return the value
     */
    public boolean isPassed() {
        return entity.passedAt != 0;
    }

    /**
     * True if this subject's assignment has been patched locally but this hasn't been replaced with an API updated version yet.
     * @return the value
     */
    public boolean isAssignmentPatched() {
        return entity.assignmentPatched;
    }

    /**
     * Is this subject overdue, i.e. has at least [threshold] percent of the SRS interval
     * elapsed since the latest review became available?.
     *
     * @return true if it is
     */
    public boolean isOverdue() {
        if (entity.availableAt == 0 || isLocked()) {
            return false;
        }
        final SrsSystem.Stage stage = getSrsStage();
        if (stage.isInitial()) {
            return true;
        }
        if (stage.isCompleted()) {
            return false;
        }
        final double since = System.currentTimeMillis() - entity.availableAt;
        if (since <= 0) {
            return false;
        }
        final double interval = stage.getInterval();
        return (since / interval) >= GlobalSettings.AdvancedOther.getOverdueThreshold();
    }

    /**
     * Is this subject eligible to be resurrected?.
     *
     * @return true if it is
     */
    public boolean isResurrectable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        if (isEmpty(GlobalSettings.Api.getWebPassword())) {
            return false;
        }
        return getSrsStage().isCompleted();
    }

    /**
     * Is this subject eligible to be burned?.
     *
     * @return true if it is
     */
    public boolean isBurnable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        if (isEmpty(GlobalSettings.Api.getWebPassword())) {
            return false;
        }
        return !getSrsStage().isCompleted() && getResurrectedAt() != 0;
    }

    /**
     * Is this subject locked?.
     *
     * @return true if it is
     */
    public boolean isLocked() {
        return entity.unlockedAt == 0;
    }

    /**
     * Is this subject eligible for a session of the given type right now?. Ignores level restrictions.
     *
     * @param sessionType type of session to check
     * @return true if it is
     */
    public boolean isEligibleForSessionType(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return entity.unlockedAt != 0 && entity.startedAt == 0 && (entity.resurrectedAt != 0 || entity.burnedAt == 0);
            case REVIEW:
                return entity.availableAt != 0 && entity.availableAt <= System.currentTimeMillis();
            case NONE:
            case SELF_STUDY:
            default:
                return true;
        }
    }

    /*
     *************************************************************************************************************************************************
     * The user's review statistics for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * Number of times the meaning has been answered correctly.
     * @return the value
     */
    public int getMeaningCorrect() {
        return entity.meaningCorrect;
    }

    /**
     * Number of times the meaning has been answered incorrectly.
     * @return the value
     */
    public int getMeaningIncorrect() {
        return entity.meaningIncorrect;
    }

    /**
     * The current streak of correct meaning answers for this subject.
     * @return the value
     */
    public int getMeaningCurrentStreak() {
        return entity.meaningCurrentStreak;
    }

    /**
     * The longest streak of correct meaning answers for this subject.
     * @return the value
     */
    public int getMeaningMaxStreak() {
        return entity.meaningMaxStreak;
    }

    /**
     * Number of times the reading has been answered correctly.
     * @return the value
     */
    public int getReadingCorrect() {
        return entity.readingCorrect;
    }

    /**
     * Number of times the reading has been answered incorrectly.
     * @return the value
     */
    public int getReadingIncorrect() {
        return entity.readingIncorrect;
    }

    /**
     * The current streak of correct reading answers for this subject.
     * @return the value
     */
    public int getReadingCurrentStreak() {
        return entity.readingCurrentStreak;
    }

    /**
     * The longest streak of correct reading answers for this subject.
     * @return the value
     */
    public int getReadingMaxStreak() {
        return entity.readingMaxStreak;
    }

    /**
     * The overall percentage of correct answers for this subject.
     * @return the value
     */
    public int getPercentageCorrect() {
        return entity.percentageCorrect;
    }

    /*
     *************************************************************************************************************************************************
     * The user's study materials for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * The unique ID of this subject's study material, or 0 if it doesn't exist.
     * @return the value
     */
    public long getStudyMaterialId() {
        return entity.studyMaterialId;
    }

    /**
     * The user's meaning note.
     * @return the value
     */
    public @Nullable String getMeaningNote() {
        return entity.meaningNote;
    }

    /**
     * The user's reading note.
     * @return the value
     */
    public @Nullable String getReadingNote() {
        return entity.readingNote;
    }

    /**
     * The user's meaning synonyms, lazily parsed from JSON.
     * @return the value
     */
    public List<String> getMeaningSynonyms() {
        if (entity.parsedMeaningSynonyms == null) {
            if (isEmpty(entity.meaningSynonyms)) {
                entity.parsedMeaningSynonyms = Collections.emptyList();
            }
            else {
                try {
                    entity.parsedMeaningSynonyms = Converters.getObjectMapper().readValue(
                            entity.meaningSynonyms, new TypeReference<List<String>>() {});
                } catch (final IOException e) {
                    entity.parsedMeaningSynonyms = Collections.emptyList();
                }
            }
        }
        if (entity.parsedMeaningSynonyms == null) {
            return Collections.emptyList();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return entity.parsedMeaningSynonyms;
    }

    /**
     * The user's meaning synonyms, lazily parsed from JSON.
     * @param meaningSynonyms the value
     */
    public void setMeaningSynonyms(final List<String> meaningSynonyms) {
        try {
            entity.meaningSynonyms = Converters.getObjectMapper().writeValueAsString(meaningSynonyms);
            entity.parsedMeaningSynonyms = new ArrayList<>(meaningSynonyms);
        } catch (final JsonProcessingException e) {
            // This can't realistically happen.
        }
    }

    /**
     * Does this subject have a non-empty meaning note?.
     *
     * @return true if it does
     */
    public boolean hasMeaningNote() {
        return !isEmpty(entity.meaningNote);
    }

    /**
     * Does this subject have a non-empty list of user synonyms?.
     *
     * @return true if it does
     */
    public boolean hasMeaningSynonyms() {
        return !getMeaningSynonyms().isEmpty();
    }

    /**
     * Does this subject have a non-empty reading note?.
     *
     * @return true if it does
     */
    public boolean hasReadingNote() {
        return !isEmpty(entity.readingNote);
    }

    /**
     * Get the star rating the user gave to this item. 0 = no stars, the default. 1-5 are the non-default options.
     *
     * @return the number of start, 0-5
     */
    public int getNumStars() {
        return entity.numStars;
    }

    /*
     *************************************************************************************************************************************************
     * Some presentation-related methods for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * Get the type of this subject for the search suggestion box.
     *
     * @return the type
     */
    public String getSearchSuggestionType() {
        return getType().getShortDescription();
    }

    /**
     * Get the type-specific background color for this subject.
     *
     * @return the color
     */
    public int getBackgroundColor() {
        return getType().getBackgroundColor();
    }

    /**
     * Get the type-specific button background color for this subject.
     *
     * @return the color
     */
    public int getButtonBackgroundColor() {
        return getType().getButtonBackgroundColor();
    }

    /**
     * Get the type-specific text color for this subject.
     *
     * @return the color
     */
    public int getTextColor() {
        return getType().getTextColor();
    }

    /**
     * Get the drawable resource ID for this radical if this is a radical
     * and it should be shown with an image rather than text.
     *
     * @return the ID or 0 if not needed
     */
    public int getTitleImageId() {
        if (!getType().canHaveTitleImage()) {
            return 0;
        }
        switch ((int) (entity.id & 0x7FFFFFFF)) {
            case 56: return R.drawable.radical_56;
            case 114: return R.drawable.radical_114;
            case 241: return R.drawable.radical_241;
            case 8761: return R.drawable.radical_8761;
            case 8762: return R.drawable.radical_8762;
            case 8763: return R.drawable.radical_8763;
            case 8764: return R.drawable.radical_8764;
            case 8765: return R.drawable.radical_8765;
            case 8766: return R.drawable.radical_8766;
            case 8767: return R.drawable.radical_8767;
            case 8768: return R.drawable.radical_8768;
            case 8769: return R.drawable.radical_8769;
            case 8770: return R.drawable.radical_8770;
            case 8771: return R.drawable.radical_8771;
            case 8772: return R.drawable.radical_8772;
            case 8773: return R.drawable.radical_8773;
            case 8774: return R.drawable.radical_8774;
            case 8775: return R.drawable.radical_8775;
            case 8776: return R.drawable.radical_8776;
            case 8777: return R.drawable.radical_8777;
            case 8778: return R.drawable.radical_8778;
            case 8779: return R.drawable.radical_8779;
            case 8780: return R.drawable.radical_8780;
            case 8781: return R.drawable.radical_8781;
            case 8782: return R.drawable.radical_8782;
            case 8783: return R.drawable.radical_8783;
            case 8784: return R.drawable.radical_8784;
            case 8785: return R.drawable.radical_8785;
            case 8787: return R.drawable.radical_8787;
            case 8788: return R.drawable.radical_8788;
            case 8790: return R.drawable.radical_8790;
            case 8792: return R.drawable.radical_8792;
            case 8793: return R.drawable.radical_8793;
            case 8794: return R.drawable.radical_8794;
            case 8796: return R.drawable.radical_8796;
            case 8797: return R.drawable.radical_8797;
            case 8798: return R.drawable.radical_8798;
            case 8799: return R.drawable.radical_8799;
            case 8819: return R.drawable.radical_8819;
            default: break;
        }
        return 0;
    }

    /**
     * Is this a radical that needs an image instead of characters?.
     *
     * @return true if it is
     */
    public boolean needsTitleImage() {
        if (!getType().canHaveTitleImage()) {
            return false;
        }
        return getTitleImageId() != 0;
    }

    /**
     * Get the title for the subject info dump for this subject, with the subject title tagged as Japanese.
     *
     * @param prefix A prefix to add to the title
     * @param suffix A suffix to add to the title
     * @return the title
     */
    public CharSequence getInfoTitle(final String prefix, final String suffix) {
        final String charactersHtml = getCharactersHtml();
        final String html = String.format(Locale.ROOT, "%s%s (level %d %s)%s",
                prefix, charactersHtml, entity.level, getType().getInfoTitleLabel(), suffix);
        return renderHtml(html);
    }

    /**
     * Get a short subject info title for use in the info dump itself.
     *
     * @return the title
     */
    public String getSimpleInfoTitle() {
        return getType().getSimpleInfoTitle(entity.level);
    }

    /**
     * The availableAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedAvailableAt() {
        return formatTimestampForDisplay(getAvailableAt());
    }

    /**
     * The burnedAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedBurnedAt() {
        return formatTimestampForDisplay(getBurnedAt());
    }

    /**
     * The passedAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedPassedAt() {
        return formatTimestampForDisplay(getPassedAt());
    }

    /**
     * The resurrectedAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedResurrectedAt() {
        return formatTimestampForDisplay(getResurrectedAt());
    }

    /**
     * The startedAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedStartedAt() {
        return formatTimestampForDisplay(getStartedAt());
    }

    /**
     * The unlockedAt value, formatted for display.
     * @return the formatted value
     */
    public String getFormattedUnlockedAt() {
        return formatTimestampForDisplay(getUnlockedAt());
    }

    /**
     * Get the accepted meanings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getMeaningRichText(final CharSequence prefix) {
        final String html = getAcceptedMeanings().stream()
                .map(meaning -> meaning.isPrimary() && getNumAcceptedMeanings() > 1
                ? String.format(Locale.ROOT, "<b>%s</b>", meaning.getMeaning())
                : meaning.getMeaning())
                .collect(Collectors.joining(", ", prefix, ""));
        return renderHtml(html);
    }

    /**
     * Get the meaning mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getMeaningMnemonicRichText() {
        @Nullable String s = getMeaningMnemonic();
        if (s == null) {
            s = "";
        }
        s = "<b>Meaning mnemonic</b>: " + NL_PATTERN.matcher(s).replaceAll("<br>");
        return renderHtml(s);
    }

    /**
     * Get the meaning hint formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getMeaningHintRichText() {
        @Nullable String s = getMeaningHint();
        if (s == null) {
            s = "";
        }
        return renderHtml(s);
    }

    /**
     * Get the legacy name formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getLegacyNameRichText() {
        @Nullable String s = getLegacyName();
        if (s == null) {
            s = "";
        }
        s = "<b>Old name</b>: " + s;
        return renderHtml(s);
    }

    /**
     * Get the legacy mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getLegacyMnemonicRichText() {
        @Nullable String s = getLegacyMnemonic();
        if (s == null) {
            s = "";
        }
        s = "<b>Old mnemonic</b>: " + s;
        return renderHtml(s);
    }

    /**
     * Get the meaning note formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getMeaningNoteRichText() {
        @Nullable String s = getMeaningNote();
        if (s == null) {
            s = "";
        }
        s = "<b>My meaning note</b>: " + NL_PATTERN.matcher(escapeHtml(s)).replaceAll("<br/>");
        return renderHtml(s);
    }

    /**
     * Get the user synonyms formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getMeaningSynonymsRichText() {
        final String s = "<b>My synonyms</b>: " + escapeHtml(join(", ", "", "", getMeaningSynonyms()));
        return renderHtml(s);
    }

    /**
     * Get the accepted readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getRegularReadingRichText(final CharSequence prefix) {
        final boolean showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana();
        final String html = getAcceptedReadings().stream()
                .map(reading -> reading.isPrimary() && getNumAcceptedReadings() > 1
                        ? String.format(Locale.ROOT, "<b>%s</b>", reading.getValue(showOnInKatakana))
                        : reading.getValue(showOnInKatakana))
                .collect(Collectors.joining(", ", prefix, ""));
        return renderHtml(html);
    }

    /**
     * Get the reading mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getReadingMnemonicRichText() {
        @Nullable String s = getReadingMnemonic();
        if (s == null) {
            s = "";
        }
        s = "<b>Reading mnemonic</b>: " + NL_PATTERN.matcher(s).replaceAll("<br>");
        return renderHtml(s);
    }

    /**
     * Get the reading hint formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getReadingHintRichText() {
        @Nullable String s = getReadingHint();
        if (s == null) {
            s = "";
        }
        return renderHtml(s);
    }

    /**
     * Get the reading note formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getReadingNoteRichText() {
        @Nullable String s = getReadingNote();
        if (s == null) {
            s = "";
        }
        s = "<b>My reading note</b>: " + NL_PATTERN.matcher(escapeHtml(s)).replaceAll("<br/>");
        return renderHtml(s);
    }

    /**
     * Get the accepted on'yomi readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getAcceptedOnYomiRichText(final CharSequence prefix) {
        final boolean showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana();
        final String html = getReadings().stream()
                .filter(Reading::isOnYomi)
                .filter(reading -> !hasAcceptedOnYomi() || reading.isAcceptedAnswer())
                .map(reading -> reading.getValue(showOnInKatakana))
                .collect(Collectors.joining(", ", prefix, ""));
        return renderHtml(html);
    }

    /**
     * Get the accepted kun'yomi readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getAcceptedKunYomiRichText(final CharSequence prefix) {
        final String html = getReadings().stream()
                .filter(Reading::isKunYomi)
                .filter(reading -> !hasAcceptedKunYomi() || reading.isAcceptedAnswer())
                .map(reading -> reading.getValue(false))
                .collect(Collectors.joining(", ", prefix, ""));
        return renderHtml(html);
    }

    /**
     * Get the on'yomi readings formatted as a piece of rich text.
     *
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getOnYomiRichText() {
        final boolean showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana();
        final String html = getReadings().stream()
                .filter(Reading::isOnYomi)
                .map(reading -> reading.getValue(showOnInKatakana))
                .collect(Collectors.joining(", ", "<b>On'yomi:</b> ", ""));
        return renderHtml(html);
    }

    /**
     * Get the kun'yomi readings formatted as a piece of rich text.
     *
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getKunYomiRichText() {
        final String html = getReadings().stream()
                .filter(Reading::isKunYomi)
                .map(reading -> reading.getValue(false))
                .collect(Collectors.joining(", ", "<b>Kun'yomi:</b> ", ""));
        return renderHtml(html);
    }

    /**
     * Get the nanori readings formatted as a piece of rich text.
     *
     * @return the text
     */
    @SuppressLint("NewApi")
    public CharSequence getNanoriRichText() {
        final String html = getReadings().stream()
                .filter(Reading::isNanori)
                .map(reading -> reading.getValue(false))
                .collect(Collectors.joining(", ", "<b>Nanori:</b> ", ""));
        return renderHtml(html);
    }

    /**
     * Get the parts of speech formatted as a piece of rich text.
     *
     * @return the text
     */
    public CharSequence getPartsOfSpeechRichText() {
        final String s = join(", ", "<b>Part of speech</b>: ", "", getPartsOfSpeech());
        return renderHtml(s);
    }

    /**
     * Get the Joyo grade for this subject as a string.
     *
     * @return the grade
     */
    public String getJoyoGradeAsString() {
        switch (getJoyoGrade()) {
            case 1:
                return "1";
            case 2:
                return "2";
            case 3:
                return "3";
            case 4:
                return "4";
            case 5:
                return "5";
            case 6:
                return "6";
            case 7:
                return "Middle school";
            default:
                return "None";
        }
    }

    /**
     * Get the JLPT level for this subject as a string.
     *
     * @return the level
     */
    public String getJlptLevelAsString() {
        switch (getJlptLevel()) {
            case 1:
                return "N1";
            case 2:
                return "N2";
            case 3:
                return "N3";
            case 4:
                return "N4";
            case 5:
                return "N5";
            default:
                return "None";
        }
    }

    /**
     * Get a short informal string describing when the next review will become available.
     *
     * @return the wait time
     */
    public String getShortNextReviewWaitTime() {
        if (getAvailableAt() == 0) {
            return "locked";
        }
        final long now = System.currentTimeMillis();
        final long next = getAvailableAt();
        return getShortWaitTimeAsInformalString(next - now);
    }
}
