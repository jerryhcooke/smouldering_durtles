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

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.model.Subject;

/**
 * A model for the rules for subject selection.
 */
public final class SubjectSelectionRules {
    private final int currentMin;
    private final int currentMax;
    private final int earlierMin;
    private final int earlierMax;
    private final int radicalMin;
    private final int radicalMax;
    private final int kanjiMin;
    private final int kanjiMax;
    private final int vocabularyMin;
    private final int vocabularyMax;
    private final int kanaVocabularyMin;
    private final int kanaVocabularyMax;
    private final int userLevel;
    private int currentDone = 0;
    private int earlierDone = 0;
    private int radicalDone = 0;
    private int kanjiDone = 0;
    private int vocabularyDone = 0;

    /**
     * The constructor.
     *
     * @param currentMin value from settings
     * @param currentMax value from settings
     * @param earlierMin value from settings
     * @param earlierMax value from settings
     * @param radicalMin value from settings
     * @param radicalMax value from settings
     * @param kanjiMin value from settings
     * @param kanjiMax value from settings
     * @param vocabularyMin value from settings
     * @param vocabularyMax  value from settings
     * @param kanaVocabularyMin value from settings
     * @param kanaVocabularyMax value from settings
     */
    public SubjectSelectionRules(final int currentMin, final int currentMax, final int earlierMin, final int earlierMax,
                                 final int radicalMin, final int radicalMax, final int kanjiMin, final int kanjiMax,
                                 final int vocabularyMin, final int vocabularyMax, int kanaVocabularyMin, int kanaVocabularyMax) {
        this.currentMin = Math.max(currentMin, 0);
        this.currentMax = currentMax == -1 ? Integer.MAX_VALUE : currentMax;
        this.earlierMin = Math.max(earlierMin, 0);
        this.earlierMax = earlierMax == -1 ? Integer.MAX_VALUE : earlierMax;
        this.radicalMin = Math.max(radicalMin, 0);
        this.radicalMax = radicalMax == -1 ? Integer.MAX_VALUE : radicalMax;
        this.kanjiMin = Math.max(kanjiMin, 0);
        this.kanjiMax = kanjiMax == -1 ? Integer.MAX_VALUE : kanjiMax;
        this.vocabularyMin = Math.max(vocabularyMin, 0);
        this.vocabularyMax = vocabularyMax == -1 ? Integer.MAX_VALUE : vocabularyMax;
        this.kanaVocabularyMin = Math.max(kanaVocabularyMin, 0);
        this.kanaVocabularyMax = kanaVocabularyMax == -1 ? Integer.MAX_VALUE : kanaVocabularyMax;
        userLevel = WkApplication.getDatabase().propertiesDao().getUserLevel();
    }

    /**
     * Does this instance have any effective rules?.
     *
     * @return true if it doesn't
     */
    public boolean isEmpty() {
        return currentMin == -1 && currentMax == -1 && earlierMin == -1 && earlierMax == -1
                && radicalMin == -1 && radicalMax == -1 && kanjiMin == -1 && kanjiMax == -1
                && vocabularyMin == -1 && vocabularyMax == -1&& kanaVocabularyMin == -1 && kanaVocabularyMax == -1;
    }

    /**
     * Is this subject wanted for subject selection in the specified stage?.
     * When this is called, a usable precondition is that the test for this
     * subject in earlier stages has already been done and has failed.
     *
     * @param subject the subject to test
     * @param stage the current stage
     * @return true if it is wanted to fulfill requirements
     */
    public boolean isWantedForStage(final Subject subject, final int stage) {
        boolean fulfillsAgeMin = false;
        boolean exceedsAgeMax = false;
        boolean fulfillsTypeMin = false;
        boolean exceedsTypeMax = false;

        if (subject.getLevel() == userLevel) {
            if (currentDone < currentMin) {
                fulfillsAgeMin = true;
            }
            if (currentDone >= currentMax) {
                exceedsAgeMax = true;
            }
        }
        if (subject.getLevel() < userLevel) {
            if (earlierDone < earlierMin) {
                fulfillsAgeMin = true;
            }
            if (earlierDone >= earlierMax) {
                exceedsAgeMax = true;
            }
        }
        if (subject.getType().isRadical()) {
            if (radicalDone < radicalMin) {
                fulfillsTypeMin = true;
            }
            if (radicalDone >= radicalMax) {
                exceedsTypeMax = true;
            }
        }
        if (subject.getType().isKanji()) {
            if (kanjiDone < kanjiMin) {
                fulfillsTypeMin = true;
            }
            if (kanjiDone >= kanjiMax) {
                exceedsTypeMax = true;
            }
        }
        if (subject.getType().isVocabulary()) {
            if (vocabularyDone < vocabularyMin) {
                fulfillsTypeMin = true;
            }
            if (vocabularyDone >= vocabularyMax) {
                exceedsTypeMax = true;
            }
        }
        if (subject.getType().isKanaVocabulary()) {
            if (vocabularyDone < kanaVocabularyMin) {
                fulfillsTypeMin = true;
            }
            if (vocabularyDone >= kanaVocabularyMax) {
                exceedsTypeMax = true;
            }
        }

        switch (stage) {
            case 0: {
                // Stage 0: subjects that help to fulfill a min-rule for both age (current/earlier level)
                // and type (radical/kanji/vocab).
                return fulfillsAgeMin && fulfillsTypeMin;
            }
            case 1: {
                // Stage 1: subjects that help to fulfill a min-rule, and do not exceed a max-rule.
                return (fulfillsAgeMin || fulfillsTypeMin) && !exceedsAgeMax && !exceedsTypeMax;
            }
            case 2: {
                // Stage 2: subjects that help to fulfill a min-rule, but may exceed a max-rule.
                return fulfillsAgeMin || fulfillsTypeMin;
            }
            case 3: {
                // Stage 3: subjects that don't exceed a max-rule.
                return !exceedsAgeMax && !exceedsTypeMax;
            }
            case 4:
                // Stage 4: everything else, i.e. subjects that may exceed max-rules, but the session is not full yet.
                return true;
            default:
                return false;
        }
    }

    /**
     * Adjust the current counts considering that the given subject has just been accepted into the session.
     *
     * @param subject the accepted subject
     */
    public void notifySelected(final Subject subject) {
        if (subject.getLevel() == userLevel) {
            currentDone++;
        }
        if (subject.getLevel() < userLevel) {
            earlierDone++;
        }
        if (subject.getType().isRadical()) {
            radicalDone++;
        }
        if (subject.getType().isKanji()) {
            kanjiDone++;
        }
        if (subject.getType().isVocabulary()) {
            vocabularyDone++;
        }
    }
}
