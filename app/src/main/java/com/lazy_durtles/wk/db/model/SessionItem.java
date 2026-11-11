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
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.jobs.ReportSessionItemJob;
import com.lazy_durtles.wk.jobs.UpdateSessionItemJob;
import com.lazy_durtles.wk.enums.KanjiAcceptedReadingType;
import com.lazy_durtles.wk.model.Question;
import com.lazy_durtles.wk.model.Session;
import com.lazy_durtles.wk.enums.SessionItemState;
import com.lazy_durtles.wk.model.SrsSystem;
import com.lazy_durtles.wk.model.SrsSystemRepository;
import com.lazy_durtles.wk.model.TypefaceConfiguration;
import com.lazy_durtles.wk.services.JobRunnerService;
import com.lazy_durtles.wk.util.FontStorageUtil;
import com.lazy_durtles.wk.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.lazy_durtles.wk.enums.SessionItemState.ABANDONED;
import static com.lazy_durtles.wk.enums.SessionItemState.ACTIVE;
import static com.lazy_durtles.wk.enums.SessionItemState.PENDING;
import static com.lazy_durtles.wk.enums.SessionItemState.REPORTED;
import static com.lazy_durtles.wk.util.ObjectSupport.nextRandomInt;
import static com.lazy_durtles.wk.util.TextUtil.hasGlyphs;
import static com.lazy_durtles.wk.util.TextUtil.isTildeCapable;

/**
 * Room entity for the session_item table, which persists items in a session.
 */
@Entity(tableName = "session_item")
public final class SessionItem {
    private static final Logger LOGGER = Logger.get(SessionItem.class);
    private static final Pattern TILDE_PATTERN = Pattern.compile("〜");

    @PrimaryKey private long id = 0L;
    private long assignmentId = 0L;
    private SessionItemState state = ACTIVE;
    private long srsSystemId = 0L;
    @ColumnInfo(name = "srsStage") private long srsStageId = 0L;
    private int level = 0;
    @ColumnInfo(name = "typeCode") private int unused = 0;
    private int bucket = 0;
    private int order = 0;
    @ColumnInfo(name = "meaningDone") private boolean question1Done = false;
    @ColumnInfo(name = "meaningIncorrect") private int question1Incorrect = 0;
    @ColumnInfo(name = "readingDone") private boolean question2Done = false;
    @ColumnInfo(name = "readingIncorrect") private int question2Incorrect = 0;
    @ColumnInfo(name = "onyomiDone") private boolean question3Done = false;
    @ColumnInfo(name = "onyomiIncorrect") private int question3Incorrect = 0;
    @ColumnInfo(name = "kunyomiDone") private boolean question4Done = false;
    @ColumnInfo(name = "kunyomiIncorrect") private int question4Incorrect = 0;
    private int numAnswers = 0;
    private long lastAnswer = 0L;
    private KanjiAcceptedReadingType kanjiAcceptedReadingType = KanjiAcceptedReadingType.NEITHER;
    @Ignore private @Nullable TypefaceConfiguration typefaceConfiguration = null;
    @Ignore private @Nullable Subject subject = null;
    @Ignore private int choiceDelay = 0;
    @Ignore private final Collection<Question> questions = new ArrayList<>();

    /**
     * Is this item active?.
     *
     * @return true if it is
     */
    public boolean isActive() {
        return state == ACTIVE;
    }

    /**
     * Is this item pending to be reported at the end of the session?.
     *
     * @return true if it is
     */
    public boolean isPending() {
        return state == PENDING;
    }

    /**
     * Has this item been abandoned?.
     *
     * @return true if it has
     */
    public boolean isAbandoned() {
        return state == ABANDONED;
    }

    /**
     * Has this item been reported?.
     *
     * @return true if it has
     */
    public boolean isReported() {
        return state == REPORTED;
    }

    /**
     * Has this item been started, i.e. is it active and has it had at least one question answered correctly or incorrectly.
     *
     * @return true if it has
     */
    public boolean isStarted() {
        return isActive() && numAnswers > 0;
    }

    /**
     * Have all questions for this item been finished or is the item otherwise inactive?.
     *
     * @return true if they have
     */
    public boolean isFinished() {
        if (state != ACTIVE) {
            return true;
        }
        return question1Done && question2Done && question3Done && question4Done;
    }

    /**
     * The subject ID.
     * @return the value
     */
    public long getId() {
        return id;
    }

    /**
     * The subject ID.
     * @param id the value
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * The assignment ID for the subject, or 0 if not yet known.
     * @return the value
     */
    public long getAssignmentId() {
        return assignmentId;
    }

    /**
     * The assignment ID for the subject, or 0 if not yet known.
     * @param assignmentId the value
     */
    public void setAssignmentId(final long assignmentId) {
        this.assignmentId = assignmentId;
    }

    /**
     * The workflow state of this item.
     * @return the value
     */
    public SessionItemState getState() {
        return state;
    }

    /**
     * The workflow state of this item.
     * @param state the value
     */
    public void setState(final SessionItemState state) {
        this.state = state;
    }

    /**
     * The SRS system for this item.
     * @return the value
     */
    public long getSrsSystemId() {
        return srsSystemId;
    }

    /**
     * The SRS system for this item.
     * @param srsSystemId the value
     */
    public void setSrsSystemId(final long srsSystemId) {
        this.srsSystemId = srsSystemId;
    }

    /**
     * The starting SRS stage for this item.
     * @return the value
     */
    public long getSrsStageId() {
        return srsStageId;
    }

    /**
     * The starting SRS stage for this item.
     * @param srsStageId the value
     */
    public void setSrsStageId(final long srsStageId) {
        this.srsStageId = srsStageId;
    }

    /**
     * The level of this item's subject.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The level of this item's subject.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * The type of this item's subject.
     * @return the value
     */
    public int getUnused() {
        return unused;
    }

    /**
     * The type of this item's subject.
     * @param unused the value
     */
    public void setUnused(final int unused) {
        this.unused = unused;
    }

    /**
     * The sorting bucket for this item.
     * @return the value
     */
    public int getBucket() {
        return bucket;
    }

    /**
     * The sorting bucket for this item.
     * @param bucket the value
     */
    public void setBucket(final int bucket) {
        this.bucket = bucket;
    }

    /**
     * The index for this item, determined after sorting.
     * @return the value
     */
    public int getOrder() {
        return order;
    }

    /**
     * The index for this item, determined after sorting.
     * @param order the value
     */
    public void setOrder(final int order) {
        this.order = order;
    }

    /**
     * The number of questions to wait before this item can come back as a choice.
     * @return the value
     */
    public int getChoiceDelay() {
        return choiceDelay;
    }

    /**
     * The number of questions to wait before this item can come back as a choice.
     * @param choiceDelay the value
     */
    public void setChoiceDelay(final int choiceDelay) {
        this.choiceDelay = choiceDelay;
    }

    /**
     * The first question has been finished for this item.
     * @return the value
     */
    public boolean isQuestion1Done() {
        return question1Done;
    }

    /**
     * The first question has been finished for this item.
     * @param question1Done the value
     */
    public void setQuestion1Done(final boolean question1Done) {
        this.question1Done = question1Done;
    }

    /**
     * Number of incorrect first question answers for this item.
     * @return the value
     */
    public int getQuestion1Incorrect() {
        return question1Incorrect;
    }

    /**
     * Number of incorrect first question answers for this item.
     * @param question1Incorrect the value
     */
    public void setQuestion1Incorrect(final int question1Incorrect) {
        this.question1Incorrect = question1Incorrect;
    }

    /**
     * The second question has been finished for this item, or doesn't exist.
     * @return the value
     */
    public boolean isQuestion2Done() {
        return question2Done;
    }

    /**
     * The second question has been finished for this item, or doesn't exist.
     * @param question2Done the value
     */
    public void setQuestion2Done(final boolean question2Done) {
        this.question2Done = question2Done;
    }

    /**
     * Number of incorrect second question answers for this item.
     * @return the value
     */
    public int getQuestion2Incorrect() {
        return question2Incorrect;
    }

    /**
     * Number of incorrect second question answers for this item.
     * @param question2Incorrect the value
     */
    public void setQuestion2Incorrect(final int question2Incorrect) {
        this.question2Incorrect = question2Incorrect;
    }

    /**
     * The third question has been finished for this item, or doesn't exist.
     * @return the value
     */
    public boolean isQuestion3Done() {
        return question3Done;
    }

    /**
     * The third question has been finished for this item, or doesn't exist.
     * @param question3Done the value
     */
    public void setQuestion3Done(final boolean question3Done) {
        this.question3Done = question3Done;
    }

    /**
     * Number of incorrect third question answers for this item.
     * @return the value
     */
    public int getQuestion3Incorrect() {
        return question3Incorrect;
    }

    /**
     * Number of incorrect third question answers for this item.
     * @param question3Incorrect the value
     */
    public void setQuestion3Incorrect(final int question3Incorrect) {
        this.question3Incorrect = question3Incorrect;
    }

    /**
     * The fourth question has been finished for this item, or doesn't exist.
     * @return the value
     */
    public boolean isQuestion4Done() {
        return question4Done;
    }

    /**
     * The fourth question has been finished for this item, or doesn't exist.
     * @param question4Done the value
     */
    public void setQuestion4Done(final boolean question4Done) {
        this.question4Done = question4Done;
    }

    /**
     * Number of incorrect fourth question answers for this item.
     * @return the value
     */
    public int getQuestion4Incorrect() {
        return question4Incorrect;
    }

    /**
     * Number of incorrect fourth question answers for this item.
     * @param question4Incorrect the value
     */
    public void setQuestion4Incorrect(final int question4Incorrect) {
        this.question4Incorrect = question4Incorrect;
    }

    /**
     * The nth question has been finished for this item.
     * @param slot the slot, 1..4
     * @return the value
     */
    public boolean isQuestionDone(final int slot) {
        switch (slot) {
            case 1:
                return question1Done;
            case 2:
                return question2Done;
            case 3:
                return question3Done;
            case 4:
            default:
                return question4Done;
        }
    }

    /**
     * The nth question has been finished for this item.
     * @param slot the slot, 1..4
     * @param questionDone the value
     */
    public void setQuestionDone(final int slot, final boolean questionDone) {
        switch (slot) {
            case 1:
                question1Done = questionDone;
                break;
            case 2:
                question2Done = questionDone;
                break;
            case 3:
                question3Done = questionDone;
                break;
            case 4:
            default:
                question4Done = questionDone;
        }
    }

    /**
     * Number of incorrect nth question answers for this item.
     * @param slot the slot, 1..4
     * @return the value
     */
    public int getQuestionIncorrect(final int slot) {
        switch (slot) {
            case 1:
                return question1Incorrect;
            case 2:
                return question2Incorrect;
            case 3:
                return question3Incorrect;
            case 4:
            default:
                return question4Incorrect;
        }
    }

    /**
     * Number of incorrect nth question answers for this item.
     * @param slot the slot, 1..4
     * @param questionIncorrect the value
     */
    public void setQuestionIncorrect(final int slot, final int questionIncorrect) {
        switch (slot) {
            case 1:
                question1Incorrect = questionIncorrect;
                break;
            case 2:
                question2Incorrect = questionIncorrect;
                break;
            case 3:
                question3Incorrect = questionIncorrect;
                break;
            case 4:
            default:
                question4Incorrect = questionIncorrect;
        }
    }

    /**
     * The number of answers that have been processed for this item in total.
     * @return the value
     */
    public int getNumAnswers() {
        return numAnswers;
    }

    /**
     * The number of answers that have been processed for this item in total.
     * @param numAnswers the value
     */
    public void setNumAnswers(final int numAnswers) {
        this.numAnswers = numAnswers;
    }

    /**
     * The Unix timestamp in ms of the last time a question was answered for this item.
     * @return the value
     */
    public long getLastAnswer() {
        return lastAnswer;
    }

    /**
     * The Unix timestamp in ms of the last time a question was answered for this item.
     * @param lastAnswer the value
     */
    public void setLastAnswer(final long lastAnswer) {
        this.lastAnswer = lastAnswer;
    }

    /**
     * The answer type needed for a kanji reading question.
     * @return the value
     */
    public KanjiAcceptedReadingType getKanjiAcceptedReadingType() {
        return kanjiAcceptedReadingType;
    }

    /**
     * The answer type needed for a kanji reading question.
     * @param kanjiAcceptedReadingType the value
     */
    public void setKanjiAcceptedReadingType(final KanjiAcceptedReadingType kanjiAcceptedReadingType) {
        this.kanjiAcceptedReadingType = kanjiAcceptedReadingType;
    }

    /**
     * Not stored in the database; the subject for this item.
     * @return the value
     */
    public @Nullable Subject getSubject() {
        return subject;
    }

    /**
     * Not stored in the database; the subject for this item.
     * @param subject the value
     */
    public void setSubject(final @Nullable Subject subject) {
        this.subject = subject;
    }

    /**
     * Get the starting SRS stage for this item, i.e. the stage it had before the session started.
     *
     * @return the stage
     */
    public SrsSystem.Stage getSrsStage() {
        return SrsSystemRepository.getSrsSystem(srsSystemId).getStage(srsStageId);
    }

    /**
     * Set the starting SRS stage for this item, i.e. the stage it had before the session started.
     *
     * @param srsStage the stage
     */
    public void setSrsStage(final SrsSystem.Stage srsStage) {
        srsSystemId = srsStage.getSystem().getId();
        srsStageId = srsStage.getId();
    }

    @Override
    public @Nonnull String toString() {
        return Long.toString(id);
    }

    /**
     * Is this item 'alive' in the session, meaning it's either active or pending a report.
     *
     * @return true if it is.
     */
    public boolean isAlive() {
        return state == ACTIVE || state == PENDING;
    }

    /**
     * Does this item has both meaning and reading-related questions to be answered?.
     *
     * @return true if it does
     */
    public boolean hasPendingReadingAndMeaning() {
        return !question1Done && !(question2Done && question3Done && question4Done);
    }

    /**
     * Does this item have any incorrectly answered questions?.
     *
     * @return true if it does
     */
    public boolean hasIncorrectAnswers() {
        return question1Incorrect > 0 || question2Incorrect > 0 || question3Incorrect > 0 || question4Incorrect > 0;
    }

    /**
     * Assuming this is a review session, what would the new SRS stage for this item be if it were finished now?.
     *
     * @return the new stage
     */
    public SrsSystem.Stage getNewSrsStage() {
        return getSrsStage().getNewStage(question1Incorrect + question2Incorrect + question3Incorrect + question4Incorrect);
    }

    /**
     * Schedule a job to update this item in the database one last time and then
     * report it to the API (if applicable for this session type).
     */
    public void report() {
        final String data = String.format(Locale.ROOT, "%d %d %s %d %d %d",
                id,
                assignmentId,
                Session.getInstance().getType(),
                question1Incorrect,
                question2Incorrect + question3Incorrect + question4Incorrect,
                lastAnswer);
        JobRunnerService.schedule(ReportSessionItemJob.class, data);
        state = REPORTED;
    }

    /**
     * Schedule a job to update this item in the database.
     */
    public void update() {
        final String data = String.format(Locale.ROOT, "%d %s %s %d %s %d %s %d %s %d %d %d",
                id,
                state,
                question1Done,
                question1Incorrect,
                question2Done,
                question2Incorrect,
                question3Done,
                question3Incorrect,
                question4Done,
                question4Incorrect,
                numAnswers,
                lastAnswer);
        JobRunnerService.schedule(UpdateSessionItemJob.class, data);
    }

    /**
     * Test if a typeface config is usable for the given question text. If it is, add it to the supplied collection.
     *
     * @param typefaces the collection to update
     * @param typefaceConfiguration the typeface config to test for suitability
     * @param text the question text to test against
     */
    private static void tryAddTypefaceConfiguration(final Collection<? super TypefaceConfiguration> typefaces,
                                                    final @Nullable TypefaceConfiguration typefaceConfiguration, final String text) {
        if (typefaceConfiguration == null) {
            return;
        }
        if (hasGlyphs(typefaceConfiguration.getTypeface(), text)) {
            typefaces.add(typefaceConfiguration);
        }
        else if (text.contains("〜") && !isTildeCapable(typefaceConfiguration.getTypeface())
                && hasGlyphs(typefaceConfiguration.getTypeface(), TILDE_PATTERN.matcher(text).replaceAll("~"))) {
            typefaces.add(typefaceConfiguration);
        }
    }

    /**
     * Get the typeface configuration chosen for this item in question text display. Choose one at random
     * from the configured options if none has been chosen yet.
     *
     * @param text The question text to show, used to test compatibility of a typeface with the question text
     * @return a typeface config, which could be the Android default if no other suitable typeface is found,
     *         even if the Android default typeface is not one of the configured options
     */
    public TypefaceConfiguration getTypefaceConfiguration(final String text) {
        if (typefaceConfiguration != null) {
            return typefaceConfiguration;
        }

        try {
            final List<TypefaceConfiguration> typefaceConfigs = new ArrayList<>();
            for (final String name: GlobalSettings.Font.getSelectedFonts()) {
                tryAddTypefaceConfiguration(typefaceConfigs, FontStorageUtil.getTypefaceConfiguration(name), text);
            }

            if (typefaceConfigs.isEmpty()) {
                typefaceConfiguration = TypefaceConfiguration.DEFAULT;
            }
            else if (typefaceConfigs.size() == 1) {
                typefaceConfiguration = typefaceConfigs.get(0);
            }
            else {
                typefaceConfiguration = typefaceConfigs.get(nextRandomInt(typefaceConfigs.size()));
            }
        }
        catch (final Exception e) {
            LOGGER.error(e, "Error fetching a typeface");
            typefaceConfiguration = TypefaceConfiguration.DEFAULT;
        }

        return typefaceConfiguration;
    }

    /**
     * Add a question for this item.
     *
     * @param question the question
     */
    public void addQuestion(final Question question) {
        questions.add(question);
    }

    /**
     * Get a question for this item by its type's name() if it exists.
     *
     * @param questionType the type as a string
     * @return the question
     */
    public @Nullable Question getQuestionByTypeStr(final @Nullable String questionType) {
        if (questionType == null) {
            return null;
        }
        for (final Question question: questions) {
            if (question.getType().name().equals(questionType)) {
                return question;
            }
        }
        return null;
    }

    /**
     * Get a question for this item by its slot if it exists.
     *
     * @param slot the slot the question belongs in
     * @return the question
     */
    public @Nullable Question getQuestionBySlot(final int slot) {
        for (final Question question: questions) {
            if (question.getType().getSlot() == slot) {
                return question;
            }
        }
        return null;
    }
}
