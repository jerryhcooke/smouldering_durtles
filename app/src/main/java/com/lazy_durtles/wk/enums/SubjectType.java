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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Enum for types of subjects, encapsulating as much knowledge as possible about the subjects of this type.
 */
public enum SubjectType {
    /**
     * A WaniKani radical.
     */
    WANIKANI_RADICAL("radical",
            false, true, true, 10, true, false, false, false, false,
            "Radical", "Radicals", "Radical", "", "radicals", "radical", 0) {
        @Override
        public int getTextColor() {
            return ActiveTheme.getSubjectTypeTextColors()[0];
        }

        @Override
        public int getBackgroundColor() {
            return ActiveTheme.getSubjectTypeBackgroundColors()[0];
        }

        @Override
        public int getButtonBackgroundColor() {
            return ActiveTheme.getSubjectTypeButtonBackgroundColors()[0];
        }

        @Override
        public String getSimpleInfoTitle(final int level) {
            return String.format(Locale.ROOT, "Level %d radical", level);
        }

        @Override
        public Collection<QuestionType> getPossibleQuestionTypes(final boolean onkun) {
            if (possibleQuestionTypes.isEmpty()) {
                possibleQuestionTypes.add(QuestionType.WANIKANI_RADICAL_NAME);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_RADICAL_NAME);
            }
            return Collections.unmodifiableCollection(onkun ? possibleQuestionTypesOnKun : possibleQuestionTypes);
        }

        @Override
        public boolean supportsQuestion1() {
            return true;
        }

        @Override
        public boolean supportsQuestion2(final boolean onkun) {
            return false;
        }

        @Override
        public boolean supportsQuestion3(final boolean onkun) {
            return false;
        }

        @Override
        public boolean supportsQuestion4(final boolean onkun) {
            return false;
        }
    },

    /**
     * A WaniKani kanji.
     */
    WANIKANI_KANJI("kanji",
            false, true, false, 20, false, true, false, false, true,
            "Kanji", "Kanji", "Kanji", "Used radicals:", "kanji", "kanji", 1) {
        @Override
        public int getTextColor() {
            return ActiveTheme.getSubjectTypeTextColors()[1];
        }

        @Override
        public int getBackgroundColor() {
            return ActiveTheme.getSubjectTypeBackgroundColors()[1];
        }

        @Override
        public int getButtonBackgroundColor() {
            return ActiveTheme.getSubjectTypeButtonBackgroundColors()[1];
        }

        @Override
        public String getSimpleInfoTitle(final int level) {
            return String.format(Locale.ROOT, "Level %d kanji", level);
        }

        @Override
        public Collection<QuestionType> getPossibleQuestionTypes(final boolean onkun) {
            if (possibleQuestionTypes.isEmpty()) {
                possibleQuestionTypes.add(QuestionType.WANIKANI_KANJI_MEANING);
                possibleQuestionTypes.add(QuestionType.WANIKANI_KANJI_READING);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_KANJI_MEANING);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_KANJI_ONYOMI);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_KANJI_KUNYOMI);
            }
            return Collections.unmodifiableCollection(onkun ? possibleQuestionTypesOnKun : possibleQuestionTypes);
        }

        @Override
        public boolean supportsQuestion1() {
            return true;
        }

        @Override
        public boolean supportsQuestion2(final boolean onkun) {
            return !onkun;
        }

        @Override
        public boolean supportsQuestion3(final boolean onkun) {
            return onkun;
        }

        @Override
        public boolean supportsQuestion4(final boolean onkun) {
            return onkun;
        }
    },

    /**
     * A WaniKani vocab.
     */
    WANIKANI_VOCAB("vocabulary",
            true, false, false,30, false, false, true, false, false,
            "Vocabulary", "Vocabulary", "Vocab", "Used kanji:", "vocabulary", "vocabulary", 2) {
        @Override
        public int getTextColor() {
            return ActiveTheme.getSubjectTypeTextColors()[2];
        }

        @Override
        public int getBackgroundColor() {
            return ActiveTheme.getSubjectTypeBackgroundColors()[2];
        }

        @Override
        public int getButtonBackgroundColor() {
            return ActiveTheme.getSubjectTypeButtonBackgroundColors()[2];
        }

        @Override
        public String getSimpleInfoTitle(final int level) {
            return String.format(Locale.ROOT, "Level %d vocabulary", level);
        }

        @Override
        public Collection<QuestionType> getPossibleQuestionTypes(final boolean onkun) {
            if (possibleQuestionTypes.isEmpty()) {
                possibleQuestionTypes.add(QuestionType.WANIKANI_VOCAB_MEANING);
                possibleQuestionTypes.add(QuestionType.WANIKANI_VOCAB_READING);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_VOCAB_MEANING);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_VOCAB_READING);
            }
            return Collections.unmodifiableCollection(onkun ? possibleQuestionTypesOnKun : possibleQuestionTypes);
        }

        @Override
        public boolean supportsQuestion1() {
            return true;
        }

        @Override
        public boolean supportsQuestion2(final boolean onkun) {
            return true;
        }

        @Override
        public boolean supportsQuestion3(final boolean onkun) {
            return false;
        }

        @Override
        public boolean supportsQuestion4(final boolean onkun) {
            return false;
        }
    },
    /**
     * A WaniKani kana vocab.
     */
    WANIKANI_KANA_VOCAB("kana_vocabulary",
            true, false, false,30, false, false, false, true, false,
            "Kana-only Vocabulary", "Vocabulary", "Kana-only Vocab", "Used kanji:", "vocabulary", "vocabulary", 2) {
        @Override
        public int getTextColor() {
            return ActiveTheme.getSubjectTypeTextColors()[3];
        }

        @Override
        public int getBackgroundColor() {
            return ActiveTheme.getSubjectTypeBackgroundColors()[3];
        }

        @Override
        public int getButtonBackgroundColor() {
            return ActiveTheme.getSubjectTypeButtonBackgroundColors()[3];
        }
        @Override
        public String getInfoTitleLabel() {
            return "Kana-only Vocab"; // Change this to the desired label
        }
        @Override
        public String getSimpleInfoTitle(final int level) {
            return String.format(Locale.ROOT, "Level %d kana-only vocabulary", level);
        }

        @Override
        public Collection<QuestionType> getPossibleQuestionTypes(final boolean onkun) {
            if (possibleQuestionTypes.isEmpty()) {
                possibleQuestionTypes.add(QuestionType.WANIKANI_KANA_VOCAB_MEANING);
                possibleQuestionTypesOnKun.add(QuestionType.WANIKANI_KANA_VOCAB_MEANING);
            }
            return Collections.unmodifiableCollection(onkun ? possibleQuestionTypesOnKun : possibleQuestionTypes);
        }

        @Override
        public boolean supportsQuestion1() {
            return true;
        }

        @Override
        public boolean supportsQuestion2(final boolean onkun) {
            return true;
        }

        @Override
        public boolean supportsQuestion3(final boolean onkun) {
            return false;
        }

        @Override
        public boolean supportsQuestion4(final boolean onkun) {
            return false;
        }
    };

    private final String dbTypeName;
    private final SubjectSource source;
    private final boolean canHavePitchInfo;
    private final boolean canHaveStrokeData;
    private final boolean canHaveTitleImage;
    private final int order;
    private final boolean radical;
    private final boolean kanji;
    private final boolean vocabulary;
    private final boolean kanaVocabulary;
    private final boolean hasLevelUpTarget;
    private final String description;
    private final String descriptionPlural;
    private final String shortDescription;
    private final String componentsHeaderText;
    private final String levelProgressLabel;
    private final String infoTitleLabel;
    private final int timeLineBarChartBucket;

    /**
     * The collection of question types that can possibly exist for this subject type.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    protected final Collection<QuestionType> possibleQuestionTypes = EnumSet.noneOf(QuestionType.class);

    /**
     * The collection of question types that can possibly exist for this subject type, when the option to quiz kanji
     * on'yomi and kun'yomi separately is tuned on.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    protected final Collection<QuestionType> possibleQuestionTypesOnKun = EnumSet.noneOf(QuestionType.class);

    /**
     * Find a SubjectType instance based on type code as stored in the database.
     *
     * @param dbTypeName the name of the type in the database, i.e. the object column.
     * @return the SubjectType instance that corresponds to this.
     */
    public static SubjectType from(final String dbTypeName) {
        for (final SubjectType type: values()) {
            if (type.dbTypeName.equals(dbTypeName)) {
                return type;
            }
        }
        return WANIKANI_RADICAL;
    }

    /**
     * The constructor.
     * @param dbTypeName instance field
     * @param canHavePitchInfo instance field
     * @param canHaveStrokeData instance field
     * @param canHaveTitleImage instance field
     * @param order instance field
     * @param radical instance field
     * @param kanji instance field
     * @param vocabulary instance field
     * @param kanaVocabulary instance field
     * @param hasLevelUpTarget instance field
     * @param description instance field
     * @param shortDescription instance field
     * @param componentsHeaderText instance field
     * @param infoTitleLabel instance field
     * @param timeLineBarChartBucket instance field
     */
    SubjectType(final String dbTypeName, final boolean canHavePitchInfo, final boolean canHaveStrokeData, final boolean canHaveTitleImage,
                final int order, final boolean radical, final boolean kanji, final boolean vocabulary, final boolean kanaVocabulary, final boolean hasLevelUpTarget,
                final String description, final String descriptionPlural, final String shortDescription,
                final String componentsHeaderText, final String levelProgressLabel, final String infoTitleLabel,
                final int timeLineBarChartBucket) {
        this.dbTypeName = dbTypeName;
        source = SubjectSource.WANIKANI;
        this.canHavePitchInfo = canHavePitchInfo;
        this.canHaveStrokeData = canHaveStrokeData;
        this.canHaveTitleImage = canHaveTitleImage;
        this.order = order;
        this.radical = radical;
        this.kanji = kanji;
        this.vocabulary = vocabulary;
        this.kanaVocabulary = kanaVocabulary;
        this.hasLevelUpTarget = hasLevelUpTarget;
        this.description = description;
        this.descriptionPlural = descriptionPlural;
        this.shortDescription = shortDescription;
        this.componentsHeaderText = componentsHeaderText;
        this.levelProgressLabel = levelProgressLabel;
        this.infoTitleLabel = infoTitleLabel;
        this.timeLineBarChartBucket = timeLineBarChartBucket;
    }

    /**
     * The type code for this type, for database mapping.
     * @return the value
     */
    public final String getDbTypeName() {
        return dbTypeName;
    }

    /**
     * The source for this subject type.
     * @return the value
     */
    @SuppressWarnings("unused")
    public SubjectSource getSource() {
        return source;
    }

    /**
     * Can a subject of this type have pitch info?.
     * @return the value
     */
    public final boolean canHavePitchInfo() {
        return canHavePitchInfo;
    }

    /**
     * Can a subject of this type have stroke order data?.
     * @return the value
     */
    public final boolean canHaveStrokeData() {
        return canHaveStrokeData;
    }

    /**
     * Can a subject of this type have an image instead of text for a title?.
     * @return the value
     */
    public final boolean canHaveTitleImage() {
        return canHaveTitleImage;
    }

    /**
     * A numeric code for this subject type to determine an ordering.
     * @return the value
     */
    public final int getOrder() {
        return order;
    }

    /**
     * Is this a radical type?.
     * @return the value
     */
    public final boolean isRadical() {
        return radical;
    }

    /**
     * Is this a kanji type?.
     * @return the value
     */
    public final boolean isKanji() {
        return kanji;
    }

    /**
     * Is this a vocabulary type?.
     * @return the value
     */
    public final boolean isVocabulary() {
        return vocabulary;
    }

    /**
     * Is this a kana vocabulary type?.
     * @return the value
     */
    public boolean isKanaVocabulary() {
        return kanaVocabulary;
    }

    /**
     * Does this type offer a level-up target?.
     * @return the value
     */
    public final boolean hasLevelUpTarget() {
        return hasLevelUpTarget;
    }

    /**
     * A textual description of this type.
     * @return the value
     */
    public final String getDescription() {
        return description;
    }

    /**
     * A textual description of this type, plural form.
     * @return the value
     */
    public final String getDescriptionPlural() {
        return descriptionPlural;
    }

    /**
     * A short textual description of this type.
     * @return the value
     */
    public final String getShortDescription() {
        return shortDescription;
    }

    /**
     * A textual description of the component subjects of this subject type, used as a header for a table of those subjects.
     * @return the value
     */
    public final String getComponentsHeaderText() {
        return componentsHeaderText;
    }

    /**
     * Label for this type in the level progress chart.
     * @return the value
     */
    public final String getLevelProgressLabel() {
        return levelProgressLabel;
    }

    /**
     * Label for this type in the subject info title.
     * @return the value
     */
    public String getInfoTitleLabel() {
        return infoTitleLabel;
    }

    /**
     * For a timeline bar chart set to style 'Item type', which bucket does this type belong in?.
     * @return the value
     */
    public final int getTimeLineBarChartBucket() {
        return timeLineBarChartBucket;
    }

    /**
     * True if subjects of this type can store study materials.
     *
     * @return true if they can
     */
    @SuppressWarnings({"MethodMayBeStatic", "SameReturnValue"})
    public final boolean canHaveStudyMaterials() {
        return true;
    }

    /**
     * Get the regular text color for this subject type.
     *
     * @return the color ARGB values
     */
    public abstract int getTextColor();

    /**
     * Get the regular background color for this subject type.
     *
     * @return the color ARGB values
     */
    public abstract int getBackgroundColor();

    /**
     * Get the background color for subject info buttons of this subject type.
     *
     * @return the color ARGB values
     */
    public abstract int getButtonBackgroundColor();

    /**
     * Get the short title for this subject type, given its level.
     *
     * @param level the level of the subject
     * @return the title string
     */
    public abstract String getSimpleInfoTitle(int level);

    /**
     * Get a collection of question types that can potentially be used for this subject type.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public abstract Collection<QuestionType> getPossibleQuestionTypes(boolean onkun);

    /**
     * Does this subject type support a question in slot 1?.
     *
     * @return true if it does
     */
    @SuppressWarnings("SameReturnValue")
    public abstract boolean supportsQuestion1();

    /**
     * Does this subject type support a question in slot 2?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public abstract boolean supportsQuestion2(boolean onkun);

    /**
     * Does this subject type support a question in slot 3?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public abstract boolean supportsQuestion3(boolean onkun);

    /**
     * Does this subject type support a question in slot 4?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    public abstract boolean supportsQuestion4(boolean onkun);
}
