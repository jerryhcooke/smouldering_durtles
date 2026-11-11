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

package com.lazy_durtles.wk.enums;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.R;
import com.lazy_durtles.wk.api.model.AuxiliaryMeaning;
import com.lazy_durtles.wk.api.model.Meaning;
import com.lazy_durtles.wk.api.model.Reading;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.model.AnswerVerdict;
import com.lazy_durtles.wk.model.DigraphMatch;
import com.lazy_durtles.wk.util.FuzzyMatching;
import com.lazy_durtles.wk.util.PseudoIme;
import com.lazy_durtles.wk.util.ThemeUtil;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;

/**
 * The type of a question in a session. Determines which answer is required.
 */
public enum QuestionType {
    /**
     * Ask for the name of the radical.
     */
    WANIKANI_RADICAL_NAME(true, false, false, true, 1, "Name") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColor);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Radical Name";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Name]" : "Your response";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final Collection<String> accepted = new HashSet<>();
            final Collection<String> rejected = new HashSet<>();
            for (final Meaning m: subject.getMeanings()) {
                (m.isAcceptedAnswer() ? accepted : rejected).add(m.getMeaning());
            }
            for (final AuxiliaryMeaning m: subject.getAuxiliaryMeanings()) {
                if (m.isWhiteList()) {
                    accepted.add(m.getMeaning());
                }
                else if (m.isBlackList()) {
                    rejected.add(m.getMeaning());
                }
            }
            accepted.addAll(subject.getMeaningSynonyms());

            final AnswerVerdict verdict = FuzzyMatching.matches(answer, accepted, rejected, closeEnoughAction);
            if (!verdict.isOk() && !verdict.isRetry()) {
                final String kanaAnswer = PseudoIme.simulateInput(answer);
                for (final Reading r: subject.getReadings()) {
                    if (r.matches(kanaAnswer, false)) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                }
            }

            return verdict;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getMeaningRichText("Answer: ");
        }
    },

    /**
     * Ask for the meaning of the kanji.
     */
    WANIKANI_KANJI_MEANING(true, false, false, true, 1, "Meaning") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColor);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Kanji Meaning";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Meaning]" : "Your response";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final Collection<String> accepted = new HashSet<>();
            final Collection<String> rejected = new HashSet<>();
            for (final Meaning m: subject.getMeanings()) {
                (m.isAcceptedAnswer() ? accepted : rejected).add(m.getMeaning());
            }
            for (final AuxiliaryMeaning m: subject.getAuxiliaryMeanings()) {
                if (m.isWhiteList()) {
                    accepted.add(m.getMeaning());
                }
                else if (m.isBlackList()) {
                    rejected.add(m.getMeaning());
                }
            }
            accepted.addAll(subject.getMeaningSynonyms());

            final AnswerVerdict verdict = FuzzyMatching.matches(answer, accepted, rejected, closeEnoughAction);
            if (!verdict.isOk() && !verdict.isRetry()) {
                final String kanaAnswer = PseudoIme.simulateInput(answer);
                for (final Reading r: subject.getReadings()) {
                    if (r.matches(kanaAnswer, false)) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                }
            }

            return verdict;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getMeaningRichText("Answer: ");
        }
    },

    /**
     * Ask for the meaning of the vocabulary.
     */
    WANIKANI_VOCAB_MEANING(true, false, false, true, 1, "Meaning") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColor);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Vocabulary Meaning";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Meaning]" : "Your response";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final Collection<String> accepted = new HashSet<>();
            final Collection<String> rejected = new HashSet<>();
            for (final Meaning m: subject.getMeanings()) {
                (m.isAcceptedAnswer() ? accepted : rejected).add(m.getMeaning());
            }
            for (final AuxiliaryMeaning m: subject.getAuxiliaryMeanings()) {
                if (m.isWhiteList()) {
                    accepted.add(m.getMeaning());
                }
                else if (m.isBlackList()) {
                    rejected.add(m.getMeaning());
                }
            }
            accepted.addAll(subject.getMeaningSynonyms());

            final AnswerVerdict verdict = FuzzyMatching.matches(answer, accepted, rejected, closeEnoughAction);
            if (!verdict.isOk() && !verdict.isRetry()) {
                final String kanaAnswer = PseudoIme.simulateInput(answer);
                for (final Reading r: subject.getReadings()) {
                    if (r.matches(kanaAnswer, false)) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                }
            }

            return verdict;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getMeaningRichText("Answer: ");
        }
    },

    /**
     * Ask for the meaning of the kana vocabulary.
     */
    WANIKANI_KANA_VOCAB_MEANING(true, false, false, true, 1, "Meaning") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColor);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Kana Vocabulary Meaning";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Meaning]" : "Your response";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final Collection<String> accepted = new HashSet<>();
            final Collection<String> rejected = new HashSet<>();
            for (final Meaning m: subject.getMeanings()) {
                (m.isAcceptedAnswer() ? accepted : rejected).add(m.getMeaning());
            }
            for (final AuxiliaryMeaning m: subject.getAuxiliaryMeanings()) {
                if (m.isWhiteList()) {
                    accepted.add(m.getMeaning());
                }
                else if (m.isBlackList()) {
                    rejected.add(m.getMeaning());
                }
            }
            accepted.addAll(subject.getMeaningSynonyms());

            final AnswerVerdict verdict = FuzzyMatching.matches(answer, accepted, rejected, closeEnoughAction);
            if (!verdict.isOk() && !verdict.isRetry()) {
                final String kanaAnswer = PseudoIme.simulateInput(answer);
                for (final Reading r: subject.getReadings()) {
                    if (r.matches(kanaAnswer, false)) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                }
            }

            return verdict;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getMeaningRichText("Answer: ");
        }
    },
    /**
     * Ask for the reading of the kanji.
     */
    WANIKANI_KANJI_READING(false, true, true, false, 2, "Reading") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColorInverse);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            if (indicateKanjiAcceptedReadingType && kanjiAcceptedReadingType == KanjiAcceptedReadingType.ONYOMI) {
                return "Kanji On'yomi";
            }
            if (indicateKanjiAcceptedReadingType && kanjiAcceptedReadingType == KanjiAcceptedReadingType.KUNYOMI) {
                return "Kanji Kun'yomi";
            }
            return "Kanji Reading";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Reading]" : "答え";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final boolean requireOnInKatakana = GlobalSettings.Other.getRequireOnInKatakana();

            for (final Reading r: subject.getReadings()) {
                if (r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                }
            }

            for (final Reading r: subject.getReadings()) {
                if (!r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(false, true, false, answer, r.getReading(), null);
                }
            }

            for (final Reading r: subject.getAcceptedReadings()) {
                final @Nullable DigraphMatch match = r.matchesForDigraph(answer);
                if (match != null) {
                    return new AnswerVerdict(false, false, false, answer, r.getReading(), match);
                }
            }

            return AnswerVerdict.NOK_WITHOUT_RETRY;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getRegularReadingRichText("Answer: ");
        }
    },

    /**
     * Ask for the reading of the vocabulary.
     */
    WANIKANI_VOCAB_READING(false, true, true, false, 2, "Reading") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColorInverse);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Vocabulary Reading";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Reading]" : "答え";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final boolean requireOnInKatakana = GlobalSettings.Other.getRequireOnInKatakana();

            for (final Reading r: subject.getReadings()) {
                if (r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                }
            }

            for (final Reading r: subject.getReadings()) {
                if (!r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(false, true, false, answer, r.getReading(), null);
                }
            }

            if (matchingKanji != null) {
                for (final Reading r: matchingKanji.getReadings()) {
                    if (r.matches(answer, requireOnInKatakana)) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                }
            }

            for (final Reading r: subject.getReadings()) {
                final @Nullable DigraphMatch match = r.matchesForDigraph(answer);
                if (match != null) {
                    return new AnswerVerdict(false, false, false, answer, r.getReading(), match);
                }
            }

            return AnswerVerdict.NOK_WITHOUT_RETRY;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getRegularReadingRichText("Answer: ");
        }
    },

    /**
     * Ask for an on'yomi reading of the kanji.
     */
    WANIKANI_KANJI_ONYOMI(false, true, true, false, 3, "On'yomi") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColorInverse);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Kanji On'yomi";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[On'yomi]" : "答え";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final boolean requireOnInKatakana = GlobalSettings.Other.getRequireOnInKatakana();

            for (final Reading r: subject.getOnYomiReadings()) {
                if (r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                }
            }
            if (!subject.hasAcceptedOnYomi()) {
                for (final Reading r: subject.getOnYomiReadings()) {
                    if (r.matches(answer, requireOnInKatakana)) {
                        return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                    }
                }
            }
            for (final Reading r: subject.getReadings()) {
                if (r.matches(answer, requireOnInKatakana)) {
                    return AnswerVerdict.NOK_WITH_RETRY;
                }
            }

            for (final Reading r: subject.getReadings()) {
                final @Nullable DigraphMatch match = r.matchesForDigraph(answer);
                if (match != null) {
                    return new AnswerVerdict(false, false, false, answer, r.getReading(), match);
                }
            }

            return AnswerVerdict.NOK_WITHOUT_RETRY;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getAcceptedOnYomiRichText("Answer: ");
        }
    },

    /**
     * Ask for a kun'yomi reading of the kanji.
     */
    WANIKANI_KANJI_KUNYOMI(false, true, true, false, 4, "Kun'yomi") {
        @Override
        public int getHintColor() {
            return ThemeUtil.getColor(R.attr.readingHintColor);
        }

        @Override
        public int getHintContrastColor() {
            return ThemeUtil.getColor(R.attr.meaningHintColor);
        }

        @Override
        public int getEditTextHintColor() {
            return ThemeUtil.getColor(R.attr.editTextHintColorInverse);
        }

        @Override
        public String getTitle(final boolean indicateKanjiAcceptedReadingType, final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
            return "Kanji Kun'yomi";
        }

        @Override
        public String getHint(final boolean landscape) {
            return landscape ? "[Kun'yomi]" : "答え";
        }

        @Override
        public AnswerVerdict checkAnswer(final Subject subject, final @Nullable Subject matchingKanji,
                                         final String answer, final CloseEnoughAction closeEnoughAction) {
            final boolean requireOnInKatakana = GlobalSettings.Other.getRequireOnInKatakana();

            for (final Reading r: subject.getKunYomiReadings()) {
                if (r.isAcceptedAnswer() && r.matches(answer, requireOnInKatakana)) {
                    return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                }
            }
            if (!subject.hasAcceptedKunYomi()) {
                for (final Reading r: subject.getKunYomiReadings()) {
                    if (r.matches(answer, requireOnInKatakana)) {
                        return new AnswerVerdict(true, false, false, answer, r.getReading(), null);
                    }
                }
            }
            for (final Reading r: subject.getReadings()) {
                if (r.matches(answer, requireOnInKatakana)) {
                    return AnswerVerdict.NOK_WITH_RETRY;
                }
            }

            for (final Reading r: subject.getAcceptedReadings()) {
                final @Nullable DigraphMatch match = r.matchesForDigraph(answer);
                if (match != null) {
                    return new AnswerVerdict(false, false, false, answer, r.getReading(), match);
                }
            }

            return AnswerVerdict.NOK_WITHOUT_RETRY;
        }

        @Override
        public CharSequence getAnkiAnswerRichText(final Subject subject) {
            return subject.getAcceptedKunYomiRichText("Answer: ");
        }
    };

    private final boolean meaning;
    private final boolean reading;
    private final boolean kana;
    private final boolean ascii;
    private final int slot;
    private final String shortTitle;

    QuestionType(final boolean meaning, final boolean reading, final boolean kana, final boolean ascii, final int slot,
                 final String shortTitle) {
        this.meaning = meaning;
        this.reading = reading;
        this.kana = kana;
        this.ascii = ascii;
        this.slot = slot;
        this.shortTitle = shortTitle;
    }

    /**
     * Is this question type a meaning question?
     *
     * @return true if it is
     */
    public final boolean isMeaning() {
        return meaning;
    }

    /**
     * Is this question type a reading question?
     *
     * @return true if it is
     */
    public final boolean isReading() {
        return reading;
    }

    /**
     * Is this question type one that expects a kana answer?
     *
     * @return true if it is
     */
    public final boolean isKana() {
        return kana;
    }

    /**
     * Is this question type one that expects an ASCII answer?
     *
     * @return true if it is
     */
    public final boolean isAscii() {
        return ascii;
    }

    /**
     * Get the question slot in a session item that is occupied by this question type.
     * @return the slot number 1..4
     */
    public final int getSlot() {
        return slot;
    }

    /**
     * Get the short title for a question of this type.
     *
     * @return the title
     */
    public final String getShortTitle() {
        return shortTitle;
    }

    /**
     * The hint color that indicates the question type (meaning, reading, ...)
     *
     * @return the ARGB values
     */
    public abstract int getHintColor();

    /**
     * The contrast hint color that indicates the question type (meaning, reading, ...)
     *
     * @return the ARGB values
     */
    public abstract int getHintContrastColor();

    /**
     * The contrast hint color that indicates the question type (meaning, reading, ...)
     *
     * @return the ARGB values
     */
    public abstract int getEditTextHintColor();

    /**
     * The title for this question type, shown just above the input field for the answer.
     *
     * @param indicateKanjiAcceptedReadingType true if that setting is enabled
     * @param kanjiAcceptedReadingType which kanji reading type is accepted here
     * @return the title
     */
    public abstract String getTitle(boolean indicateKanjiAcceptedReadingType, KanjiAcceptedReadingType kanjiAcceptedReadingType);

    /**
     * Get the input field hint for this question type.
     *
     * <p>
     *     In landscape mode, this includes the question type as part of
     *     making the view more compact. In portrait mode, it contains the Japanese
     *     word for "answer" for reading questions.
     * </p>
     *
     * @param landscape true if the device is in landscape mode
     * @return the hint
     */
    public abstract String getHint(final boolean landscape);

    /**
     * Check a given answer to this question type.
     *
     * @param subject the subject this question is for
     * @param matchingKanji if this subject is a vocab item that consists of only one kanji character, this is that kanji.
     * @param answer the user's answer
     * @param closeEnoughAction action for an answer that is close enough for typo lenience.
     * @return the verdict for this answer
     */
    public abstract AnswerVerdict checkAnswer(Subject subject, @Nullable Subject matchingKanji,
                                              String answer, CloseEnoughAction closeEnoughAction);

    /**
     * Get this question type's accepted answers as a formatted string for Anki mode.
     *
     * @param subject the subject for this item
     * @return the text
     */
    public abstract CharSequence getAnkiAnswerRichText(Subject subject);
}
