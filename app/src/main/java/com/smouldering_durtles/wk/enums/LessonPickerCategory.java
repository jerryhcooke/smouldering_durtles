package com.smouldering_durtles.wk.enums;

import com.smouldering_durtles.wk.db.model.Subject;

/**
 * The subject groups offered by the lesson picker.
 */
public enum LessonPickerCategory {
    /**
     * Radical subjects.
     */
    RADICALS {
        @Override
        public boolean matches(final Subject subject) {
            return subject.getType().isRadical();
        }
    },

    /**
     * Kanji subjects.
     */
    KANJI {
        @Override
        public boolean matches(final Subject subject) {
            return subject.getType().isKanji();
        }
    },

    /**
     * Vocabulary and kana-vocabulary subjects.
     */
    VOCABULARY {
        @Override
        public boolean matches(final Subject subject) {
            return subject.getType().isVocabulary() || subject.getType().isKanaVocabulary();
        }
    };

    /**
     * Determine whether a subject belongs to this picker category.
     *
     * @param subject the subject to classify
     * @return true if the subject belongs in this category
     */
    public abstract boolean matches(Subject subject);
}
