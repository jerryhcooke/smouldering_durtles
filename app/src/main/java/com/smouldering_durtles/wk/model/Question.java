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

package com.smouldering_durtles.wk.model;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.CloseEnoughAction;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.util.InflectionUtil;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.enums.SessionItemState.ACTIVE;
import static com.smouldering_durtles.wk.enums.SessionItemState.PENDING;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A model class representing one question (meaning, reading, on'yomi, kun'yomi) in a session.
 * One SessionItem has one or more question which are answered independently.
 */
public final class Question {
    private final SessionItem item;
    private final QuestionType type;
    
    /**
     * If this is a verb or adjective and the user requested questions to be shown in
     * forms with inflection, this field contains the name of the inflection for this question.
     */
    private @Nullable String inflection;

    /**
     * The constructor.
     *
     * @param item the item this question belongs to
     * @param type the type of question
     */
    public Question(final SessionItem item, final QuestionType type) {
        this.item = item;
        this.type = type;
    }

    /**
     * The item this question belongs to.
     * @return the value
     */
    public SessionItem getItem() {
        return item;
    }

    /**
     * The type of question.
     * @return the value
     */
    public QuestionType getType() {
        return type;
    }

    @Override
    public @Nonnull String toString() {
        return String.format(Locale.ROOT, "%s:%d", type, item.getId());
    }

    /**
     * Is this question finished?.
     *
     * @return true if finished
     */
    public boolean isFinished() {
        return !item.isActive() || item.isQuestionDone(type.getSlot());
    }

    /**
     * Can the workflow allow an undo on this question?.
     *
     * @return true if it can
     */
    public boolean canUndo() {
        return !item.isReported() && !item.isAbandoned();
    }

    /**
     * The title for this question, shown just above the input field for the answer.
     *
     * @return the title
     */
    public String getTitle() {
        return type.getTitle(GlobalSettings.AdvancedOther.getIndicateKanjiReadingType(), item.getKanjiAcceptedReadingType());
    }

    /**
     * Get the input field hint for this question.
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
    public String getHint(final boolean landscape) {
        return type.getHint(landscape);
    }

    /**
     * Check a given answer to this question.
     *
     * @param matchingKanji if this subject is a vocab item that consists of only one kanji character, this is that kanji.
     * @param answer the user's answer
     * @param closeEnoughAction action for an answer that is close enough for typo lenience.
     * @return the verdict for this answer
     */
    public AnswerVerdict checkAnswer(final @Nullable Subject matchingKanji, final String answer, final CloseEnoughAction closeEnoughAction) {
        if (isEmpty(answer)) {
            return AnswerVerdict.NOK_WITH_RETRY;
        }

        final Subject subject = requireNonNull(item.getSubject());

        return type.checkAnswer(subject, matchingKanji, answer, closeEnoughAction);
    }

    /**
     * Mark this question as having been answered correctly.
     */
    public void markCorrect() {
        item.setQuestionDone(type.getSlot(), true);
        item.setNumAnswers(item.getNumAnswers() + 1);
        item.setLastAnswer(System.currentTimeMillis());

        if (item.isFinished()) {
            if (Session.getInstance().isDelayed()) {
                item.setState(PENDING);
                item.update();
            }
            else {
                item.report();
            }
        }
        else {
            item.update();
        }
    }

    /**
     * Mark this question as having been answered incorrectly.
     */
    public void markIncorrect() {
        final int slot = type.getSlot();
        item.setQuestionIncorrect(slot, item.getQuestionIncorrect(slot) + 1);
        item.setNumAnswers(item.getNumAnswers() + 1);
        item.update();
    }

    /**
     * Undo the last processed answer to this question.
     */
    public void undo() {
        if (!canUndo()) {
            return;
        }

        final int slot = type.getSlot();
        if (item.isQuestionDone(slot)) {
            item.setQuestionDone(slot, false);
        }
        else {
            item.setQuestionIncorrect(slot, item.getQuestionIncorrect(slot) - 1);
        }
        item.setNumAnswers(item.getNumAnswers() - 1);
        if (item.isPending()) {
            item.setState(ACTIVE);
        }

        item.update();
    }

    /**
     * Get this question's accepted answers as a formatted string for Anki mode.
     *
     * @param subject the subject for this item
     * @return the text
     */
    public CharSequence getAnkiAnswerRichText(final Subject subject) {
        String inflection = getInflection(subject);
        return type.getAnkiAnswerRichText(subject) + (inflection != null ? " (" + inflection + ")" : "");
    }

    private boolean isVerb(final Subject subject) {
        return getInflectionForm(subject).contains("verb");
    }

    private String getInflectionForm(final Subject subject) {
        for (String partOfSpeech : subject.getPartsOfSpeech()) {
            if (partOfSpeech.equals("godan verb") ||
                partOfSpeech.equals("ichidan verb") || 
                partOfSpeech.equals("する verb") ||
                partOfSpeech.equals("い adjective") || 
                partOfSpeech.equals("な adjective")) return partOfSpeech;
        }
        return null;
    }

    private String getInflection(final Subject subject) {
        if (inflection != null) {
            return inflection;
        }
        if (getInflectionForm(subject) == null) {
            return null;
        }

        inflection = isVerb(subject) ? InflectionUtil.getRandomVerbConjugation() : InflectionUtil.getRandomAdjectiveDeclension();
        return inflection;
    }

    public @Nullable String getCharacters(final Subject subject) {
        String characters = subject.getCharacters();
        if (characters == null) {
            return null;
        }

        String inflection = getInflectionForm(subject);
        if (inflection == null)
            return characters;
        else if (inflection.equals("godan verb"))
            return InflectionUtil.getConjugatedVerb(characters, InflectionUtil.VerbType.GODAN, getInflection(subject));
        else if (inflection.equals("ichidan verb"))
            return InflectionUtil.getConjugatedVerb(characters, InflectionUtil.VerbType.ICHIDAN, getInflection(subject));
        else if (inflection.equals("する verb"))
            return InflectionUtil.getConjugatedVerb(characters, InflectionUtil.VerbType.SURU, getInflection(subject));
        else if (inflection.equals("い adjective"))
            return InflectionUtil.getDeclinedAdjective(characters, InflectionUtil.AdjectiveType.I, getInflection(subject));
        else if (inflection.equals("な adjective"))
            return InflectionUtil.getDeclinedAdjective(characters, InflectionUtil.AdjectiveType.NA_PLAIN, getInflection(subject));
         else
            return characters;
    }
}
