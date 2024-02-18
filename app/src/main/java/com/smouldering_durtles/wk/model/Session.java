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

import android.annotation.SuppressLint;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.adapter.sessionlog.SessionLogAdapter;
import com.smouldering_durtles.wk.api.model.Meaning;
import com.smouldering_durtles.wk.api.model.Reading;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.LessonOrder;
import com.smouldering_durtles.wk.enums.QuestionChoiceReason;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.enums.ReviewOrder;
import com.smouldering_durtles.wk.enums.SessionItemState;
import com.smouldering_durtles.wk.enums.SessionState;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.fragments.AbstractSessionFragment;
import com.smouldering_durtles.wk.fragments.AnkiSessionFragment;
import com.smouldering_durtles.wk.fragments.AnsweredSessionFragment;
import com.smouldering_durtles.wk.fragments.LessonSessionFragment;
import com.smouldering_durtles.wk.fragments.SummarySessionFragment;
import com.smouldering_durtles.wk.fragments.UnansweredSessionFragment;
import com.smouldering_durtles.wk.jobs.AbandonSessionItemJob;
import com.smouldering_durtles.wk.jobs.FinishSessionJob;
import com.smouldering_durtles.wk.livedata.LiveSessionProgress;
import com.smouldering_durtles.wk.livedata.LiveSessionState;
import com.smouldering_durtles.wk.livedata.SubjectChangeListener;
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher;
import com.smouldering_durtles.wk.services.JobRunnerService;
import com.smouldering_durtles.wk.util.AudioUtil;
import com.smouldering_durtles.wk.util.KanaUtil;
import com.smouldering_durtles.wk.util.Logger;
import com.smouldering_durtles.wk.util.PitchInfoUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.enums.SessionItemState.ABANDONED;
import static com.smouldering_durtles.wk.enums.SessionState.ACTIVE;
import static com.smouldering_durtles.wk.enums.SessionState.FINISHING;
import static com.smouldering_durtles.wk.enums.SessionState.INACTIVE;
import static com.smouldering_durtles.wk.enums.SessionState.IN_LESSON_PRESENTATION;
import static com.smouldering_durtles.wk.enums.SessionType.LESSON;
import static com.smouldering_durtles.wk.enums.SessionType.NONE;
import static com.smouldering_durtles.wk.enums.SessionType.REVIEW;
import static com.smouldering_durtles.wk.enums.SessionType.SELF_STUDY;
import static com.smouldering_durtles.wk.util.ObjectSupport.nextRandomInt;
import static java.util.Objects.requireNonNull;

/**
 * The singleton object that represents the user's lesson/review/self-study session.
 *
 * <p>
 *     The state of the session is persisted, and if the app process is killed, it
 *     can be reloaded from the database when the app restarts.
 * </p>
 */
public final class Session implements SubjectChangeListener {
    private static final Logger LOGGER = Logger.get(Session.class);
    private static final Session instance = new Session();

    private final SessionLogAdapter adapter = new SessionLogAdapter();
    private boolean loaded = false;
    private SessionType type = NONE;
    private boolean onkun = false;
    private boolean backToBack = false;
    private boolean readingFirst = false;
    private boolean meaningFirst = false;
    private boolean delayed = false;
    private SessionState state = INACTIVE;
    private @Nullable SessionItem currentItem = null;
    private @Nullable Question currentQuestion = null;
    private QuestionChoiceReason questionChoiceReason = QuestionChoiceReason.STARTING_REVIEW_SESSION;
    private long lastFinishedSubjectId = -1;
    private boolean answered = false;
    private boolean correct = false;
    private List<SessionItem> items = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();
    private Comparator<Subject> comparator = ReviewOrder.SHUFFLE.getComparator();
    private final Deque<Question> history = new ArrayDeque<>();
    private long lastTypedIncorrectAnswer = 0;
    private boolean forceNewFragment = false;


    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static Session getInstance() {
        return instance;
    }

    private Session() {
        LiveSessionState.getInstance().post(state);
        //noinspection ThisEscapedInObjectConstruction
        SubjectChangeWatcher.getInstance().addListener(this);
    }

    /**
     * The full list of items in this session, including items that haven been
     * finished or abandoned. This list is in the order determined by the applicable
     * ordering settings.
     * @return the value
     */
    public List<SessionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Get the recycler view adapter for the session.
     *
     * @return the adapter
     */
    public SessionLogAdapter getAdapter() {
        return adapter;
    }

    /**
     * The type of this session, or NONE if no session is active.
     * @return the value
     */
    public SessionType getType() {
        return type;
    }

    /**
     * True if the initial load on process startup has been done.
     *
     * @return true or false
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Is the session eligible for being abandoned?.
     *
     * @return true if it is
     */
    public boolean canBeAbandoned() {
        return state != INACTIVE;
    }

    /**
     * Is the session eligible for being wrapped up?.
     *
     * @return true if it is
     */
    public boolean canBeWrappedUp() {
        return state == ACTIVE && getNumStartedItems() < getNumActiveItems();
    }

    /**
     * Is the session eligible for an undo?.
     *
     * @return true if it is
     */
    public boolean canUndo() {
        if (currentQuestion != null && answered && currentQuestion.canUndo()) {
            return true;
        }
        if (history.isEmpty()) {
            return false;
        }
        final @Nullable Question peek = history.peek();
        return isDelayed() || peek != null && peek.canUndo();
    }

    /**
     * Is this is a lession session that is in the lesson presentation stage?.
     *
     * @return true if it is
     */
    private boolean isInLessonPresentation() {
        return state == IN_LESSON_PRESENTATION;
    }

    /**
     * Is this session active?.
     *
     * @return true if it is
     */
    public boolean isActive() {
        return state == ACTIVE;
    }

    /**
     * Is this session inactive?.
     *
     * @return true if it is
     */
    public boolean isInactive() {
        return state == INACTIVE;
    }

    /**
     * Is this session finishing up?.
     *
     * @return true if it is
     */
    private boolean isFinishing() {
        return state == FINISHING;
    }

    /**
     * Has the current question been answered already?.
     * A shake-and-retry doesn't count as answered.
     * @return the value
     */
    public boolean isAnswered() {
        return answered;
    }

    /**
     * If answered, was the current question answered correctly?.
     * @return the value
     */
    public boolean isCorrect() {
        return correct;
    }

    /**
     * True if delayed reporting is active for this session.
     * @return the value
     */
    public boolean isDelayed() {
        return delayed || type == SELF_STUDY;
    }

    /**
     * Are on and kun quizzed separately for this session?.
     *
     * @return true if they are
     */
    public boolean isOnkun() {
        return onkun;
    }

    /**
     * Get the subject ID for the last item in the session that has been finished.
     *
     * @return the ID, or -1 if unavailable
     */
    public long getLastFinishedSubjectId() {
        if (state == INACTIVE) {
            return -1;
        }
        return lastFinishedSubjectId;
    }

    /**
     * The session item for the current question.
s     *
     * @return the item
     */
    public @Nullable SessionItem getCurrentItem() {
        return currentItem;
    }

    /**
     * The current question.
     *
     * @return the question
     */
    public @Nullable Question getCurrentQuestion() {
        return currentQuestion;
    }

    private void setCurrentQuestion(final @Nullable Question question, final QuestionChoiceReason reason) {
        if (question != null && currentQuestion != question) {
            FloatingUiState.lingerOnAnswer = false;
        }
        currentQuestion = question;
        questionChoiceReason = reason;
    }

    /**
     * The reason for the most recent question change.
     *
     * @return the reason
     */
    public QuestionChoiceReason getQuestionChoiceReason() {
        return questionChoiceReason;
    }

    /**
     * The number of items in the session that are still active.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    public int getNumActiveItems() {
        return (int) items.stream().filter(SessionItem::isActive).count();
    }

    /**
     * The number of items in the session that are still pending.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    public int getNumPendingItems() {
        return (int) items.stream().filter(SessionItem::isPending).count();
    }

    /**
     * The number of items in the session that have been finished.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    private int getNumFinishedItems() {
        return (int) items.stream().filter(item -> item.isReported() || item.isPending()).count();
    }

    /**
     * The total number of items in the session, excluding abandoned items.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    private int getNumLiveItems() {
        return (int) items.stream().filter(item -> !item.isAbandoned()).count();
    }

    /**
     * The total number of started items in the session.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    public int getNumStartedItems() {
        return (int) items.stream().filter(SessionItem::isStarted).count();
    }

    /**
     * The total number of reported items in the session.
     *
     * @return the number
     */
    @SuppressLint("NewApi")
    public int getNumReportedItems() {
        return (int) items.stream().filter(SessionItem::isReported).count();
    }

    /**
     * Is the session currently set to display the first lesson item?.
     *
     * @return true if it is
     */
    public boolean isOnFirstLessonItem() {
        final @Nullable SessionItem item = currentItem;
        if (item == null) {
            return false;
        }
        return items.indexOf(item) == 0;
    }

    /**
     * Is the session currently set to display the last lesson item?.
     *
     * @return true if it is
     */
    public boolean isOnLastLessonItem() {
        final @Nullable SessionItem item = currentItem;
        if (item == null) {
            return false;
        }
        return items.indexOf(item) == items.size() - 1;
    }

    /**
     * Should the next/submit button be frozen right now?.
     *
     * @return true if it is
     */
    public boolean isNextButtonFrozen() {
        return System.currentTimeMillis() - lastTypedIncorrectAnswer < GlobalSettings.Keyboard.getNextButtonDelay() * 1000;
    }

    /**
     * Get the session progress text shown in the top left corner of the question display.
     *
     * @return the text
     */
    public String getProgressText() {
        if (isInLessonPresentation()) {
            return String.format(Locale.ROOT, "%d/%d", items.indexOf(currentItem)+1, items.size());
        }
        final boolean wrappingUp = state == ACTIVE && getNumStartedItems() >= getNumActiveItems();
        final String prefix = wrappingUp ? "Wrapup: " : "";
        return String.format(Locale.ROOT, "%s%d/%d", prefix, getNumFinishedItems(), getNumLiveItems());
    }

    /**
     * Choose a question from the available questions, taking into account
     * order requirements and other relevant configuration.
     */
    @SuppressLint("NewApi")
    public void chooseQuestion() {
        if (currentQuestion != null || isFinishing() || isInactive()) {
            return;
        }

        answered = false;
        FloatingUiState.setCurrentAnswer("");
        if (state == IN_LESSON_PRESENTATION) {
            setCurrentQuestion(questions.get(0), QuestionChoiceReason.STARTING_LESSON_SESSION);
            currentItem = requireNonNull(items.get(0));
            WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
            WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(currentQuestion.getType());
            FloatingUiState.audioPlayed = false;
            FloatingUiState.showDumpStage = null;
            LOGGER.info("Choose question: %s in lesson presentation", currentItem);
            LiveSessionProgress.getInstance().ping();
            return;
        }

        if (questions.isEmpty()) {
            setCurrentQuestion(null, QuestionChoiceReason.FINISHING_SESSION);
            currentItem = null;
            if (state == ACTIVE) {
                state = FINISHING;
                LiveSessionState.getInstance().post(state);
            }
            LOGGER.info("Choose question: no more questions, setting session to FINISHING");
            LiveSessionProgress.getInstance().ping();
            return;
        }

        List<Question> candidateQuestions = questions;

        if (backToBack) {
            final List<Question> list = candidateQuestions.stream()
                    .filter(question -> question.getItem().isStarted())
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                candidateQuestions = list;
            }
        }

        if (readingFirst) {
            final List<Question> list = candidateQuestions.stream()
                    .filter(question -> !question.getItem().hasPendingReadingAndMeaning() || question.getType().isReading())
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                candidateQuestions = list;
            }
        }

        if (meaningFirst) {
            final List<Question> list = candidateQuestions.stream()
                    .filter(question -> !question.getItem().hasPendingReadingAndMeaning() || question.getType().isMeaning())
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                candidateQuestions = list;
            }
        }

        if (getNumStartedItems() >= 10) {
            final List<Question> list = candidateQuestions.stream()
                    .filter(question -> question.getItem().isStarted())
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                candidateQuestions = list;
            }
        }

        boolean hasDelayed = false;
        boolean hasUndelayed = false;
        final int currentBucket = candidateQuestions.get(0).getItem().getBucket();
        for (final SessionItem item: items) {
            if (item.getBucket() == currentBucket) {
                if (item.getChoiceDelay() == 0) {
                    hasUndelayed = true;
                }
                else {
                    hasDelayed = true;
                }
            }
        }
        if (hasDelayed && hasUndelayed) {
            final List<Question> list = candidateQuestions.stream()
                    .filter(question -> question.getItem().getBucket() == currentBucket)
                    .filter(question -> question.getItem().getChoiceDelay() == 0)
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                candidateQuestions = list;
            }
        }

        final int bucket = candidateQuestions.get(0).getItem().getBucket();
        int i = 1;
        while (i < candidateQuestions.size() && candidateQuestions.get(i).getItem().getBucket() == bucket) {
            i++;
        }
        final int index = nextRandomInt(i);
        setCurrentQuestion(candidateQuestions.get(index), questionChoiceReason);
        currentItem = currentQuestion.getItem();
        WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
        WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(currentQuestion.getType());
        FloatingUiState.audioPlayed = false;
        FloatingUiState.showDumpStage = null;
        for (final SessionItem item: items) {
            if (item.getChoiceDelay() > 0) {
                item.setChoiceDelay(item.getChoiceDelay() - 1);
            }
        }
        LOGGER.info("Choose question: %s", currentQuestion);
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Advance the lession session to the next lesson item.
     */
    public void moveToNextLessonItem() {
        answered = false;
        FloatingUiState.setCurrentAnswer("");
        if (state != IN_LESSON_PRESENTATION) {
            LOGGER.info("Move to next lesson item: not in lesson presentation");
            return;
        }
        if (currentItem == null) {
            LOGGER.info("Move to next lesson item: no current item");
            return;
        }
        final int index = items.indexOf(currentItem) + 1;
        if (index >= items.size()) {
            LOGGER.info("Move to next lesson item: no next item");
            return;
        }
        setCurrentQuestion(questions.get(0), QuestionChoiceReason.MOVE_TO_NEXT_LESSON_ITEM);
        currentItem = requireNonNull(items.get(index));
        WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
        WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(questions.get(0).getType());
        FloatingUiState.audioPlayed = false;
        FloatingUiState.showDumpStage = null;
        LOGGER.info("Move to next lesson item: %s", currentItem);
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Advance the lession session to the previous lesson item.
     */
    public void moveToPreviousLessonItem() {
        answered = false;
        FloatingUiState.setCurrentAnswer("");
        if (state != IN_LESSON_PRESENTATION) {
            LOGGER.info("Move to previous lesson item: not in lesson presentation");
            return;
        }
        if (currentItem == null) {
            LOGGER.info("Move to previous lesson item: no current item");
            return;
        }
        final int index = items.indexOf(currentItem) - 1;
        if (index < 0 || index >= items.size()) {
            LOGGER.info("Move to previous lesson item: no next item");
            return;
        }
        setCurrentQuestion(questions.get(0), QuestionChoiceReason.MOVE_TO_PREVIOUS_LESSON_ITEM);
        currentItem = requireNonNull(items.get(index));
        WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
        WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(questions.get(0).getType());
        FloatingUiState.audioPlayed = false;
        FloatingUiState.showDumpStage = null;
        LOGGER.info("Move to previous lesson item: %s", currentItem);
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Advance the session to the next question.
     */
    public void advance() {
        if (!answered) {
            LOGGER.info("Advance: current question not answered yet");
            return;
        }
        if (currentItem != null && currentItem.isFinished()) {
            lastFinishedSubjectId = currentItem.getId();
        }
        if (currentQuestion != null) {
            if (!correct) {
                currentQuestion.getItem().setChoiceDelay(3);
            }
            history.push(currentQuestion);
            if (currentQuestion.isFinished()) {
                questions.remove(currentQuestion);
                if (questions.isEmpty()) {
                    state = FINISHING;
                    LiveSessionState.getInstance().post(state);
                }
            }
        }
        answered = false;
        FloatingUiState.setCurrentAnswer("");
        setCurrentQuestion(null, QuestionChoiceReason.NEXT_NATURAL);
        currentItem = null;
        LOGGER.info("Advance: current question and item cleared");
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Advance the session to the next question, but without triggering any outside actions.
     */
    public void advanceQuietly() {
        if (!answered) {
            LOGGER.info("Advance: current question not answered yet");
            return;
        }
        answered = false;
        FloatingUiState.setCurrentAnswer("");
        setCurrentQuestion(null, QuestionChoiceReason.NEXT_FORCED);
        currentItem = null;
        LOGGER.info("Advance quietly: current question and item cleared");
    }

    /**
     * After the lesson preparation, start the quiz.
     */
    public void startQuiz() {
        LOGGER.info("Start quiz");
        state = ACTIVE;
        LiveSessionState.getInstance().post(state);
        currentItem = null;
        setCurrentQuestion(null, QuestionChoiceReason.STARTING_QUIZ);
        lastFinishedSubjectId = -1;
        LiveSessionProgress.getInstance().ping();
        adapter.addEventStartQuiz();
    }

    /**
     * Go back to lesson presentation.
     */
    public void goBackToPresentation() {
        LOGGER.info("Go back to lesson presentation");
        if (!isActive() || type != LESSON || items.isEmpty() || questions.isEmpty()) {
            LOGGER.info("Go back to lesson presentation: not in an active lesson session");
        }

        state = IN_LESSON_PRESENTATION;
        adapter.addEventBackToLessonPresentation();
        currentItem = items.get(items.size()-1);
        setCurrentQuestion(questions.get(0), QuestionChoiceReason.BACK_TO_PRESENTATION);
        LiveSessionState.getInstance().post(state);
        LiveSessionProgress.getInstance().ping();
    }

    private AnswerVerdict submitHelper(final @Nullable Subject matchingKanji) {
        final @Nullable Subject subject = currentQuestion == null ? null : currentQuestion.getItem().getSubject();
        String currentAnswer = FloatingUiState.getCurrentAnswer();

        LOGGER.info("Start submit: %s '%s' for subject '%s'",
                currentQuestion, currentAnswer, subject == null ? null : subject.getCharacters());

        if (currentQuestion == null || subject == null) {
            LOGGER.info("End submit: %s current question is null", AnswerVerdict.NOK_WITHOUT_RETRY);
            return AnswerVerdict.NOK_WITHOUT_RETRY;
        }

        if (answered) {
            final AnswerVerdict verdict = new AnswerVerdict(correct, false, false, null, null, null);
            LOGGER.info("End submit: %s current question already answered", verdict);
            return verdict;
        }

        if (currentAnswer.isEmpty()) {
            LOGGER.info("End submit: %s current answer is empty", AnswerVerdict.NOK_WITH_RETRY);
            return AnswerVerdict.NOK_WITH_RETRY;
        }

        if (currentQuestion.getType().isKana()) {
            if (currentAnswer.endsWith("n")) {
                currentAnswer = currentAnswer.substring(0, currentAnswer.length() - 1) + "ん";
                FloatingUiState.setCurrentAnswer(currentAnswer);
                LOGGER.info("Submit: Reading ends in 'n', replacing with 'ん': '%s'", currentAnswer);
            }
            for (int i=0; i<currentAnswer.length(); i++) {
                final char c = currentAnswer.charAt(i);
                if (c < 0x3040 || c > 0x30FF) {
                    LOGGER.info("End submit: %s non-kana in answer", AnswerVerdict.NOK_WITH_RETRY);
                    return AnswerVerdict.NOK_WITH_RETRY;
                }
            }

            // Because the user input and subject can contain both Hiragana and Katakana in addition to Kanji, we convert Katakana to Hiragana for the next check
            String normalizedSubjectCharacters = KanaUtil.convertKatakanaToHiragana(subject.getCharacters());
            String normalizedAnswer = KanaUtil.convertKatakanaToHiragana(currentAnswer);

            // Check if the answer contains the subject's Kana characters in the expected order
            String expectedKana = normalizedSubjectCharacters.replaceAll("\\p{Script=Han}+", ".+");
            boolean foundNecessaryKana = normalizedAnswer.matches(expectedKana);
            if (!foundNecessaryKana) {
                LOGGER.info("End submit: %s didn't find all expected kana", AnswerVerdict.NOK_WITH_RETRY);
                return AnswerVerdict.NOK_WITH_RETRY;
            }
        }

        final AnswerVerdict verdict = currentQuestion.checkAnswer(matchingKanji, currentAnswer.trim(),
                GlobalSettings.Review.getCloseEnoughAction());

        if (verdict.isOk()) {
            currentQuestion.markCorrect();
            LOGGER.info("Marked correct: %s finished=%s", currentQuestion, currentQuestion.isFinished());
            if (currentQuestion.getItem().isFinished()) {
                FloatingUiState.toastOldSrsStage = currentQuestion.getItem().getSrsStage();
                FloatingUiState.toastNewSrsStage = currentQuestion.getItem().getNewSrsStage();
                FloatingUiState.showSrsStageChangedToast = true;

            }

            answered = true;
            correct = true;

            FloatingUiState.lastVerdict = verdict;
            List<String> alternatives = null;
            if (currentQuestion.getType().isMeaning() && subject.getMeanings().size() > 1) {
                alternatives = subject.getMeanings().stream().map(Meaning::getMeaning).collect(Collectors.toList());
            } else if (currentQuestion.getType().isReading() && subject.getAcceptedReadings().size() > 1) {
                alternatives = subject.getAcceptedReadings().stream().map(Reading::getReading).collect(Collectors.toList());
            }

            if (alternatives != null) {
                List<String> filteredAlternatives = new ArrayList<>();
                Set<String> uniqueAlternativesWithoutThe = new HashSet<>();
                String lowerCaseCurrentAnswer = currentAnswer.toLowerCase().replaceFirst("^the ", "");
                String closeEnoughAnswer = FloatingUiState.lastVerdict != null ? FloatingUiState.lastVerdict.getMatchedAnswer().toLowerCase().replaceFirst("^the ", "") : null;

                for (String alternative : alternatives) {
                    String lowerCaseAlternative = alternative.toLowerCase();
                    String alternativeWithoutThe = lowerCaseAlternative.replaceFirst("^the ", "");

                    if (alternativeWithoutThe.equals(lowerCaseCurrentAnswer) || alternativeWithoutThe.equals(closeEnoughAnswer)) {
                        continue;
                    }

                    if (uniqueAlternativesWithoutThe.contains(alternativeWithoutThe)) {
                        continue;
                    }

                    uniqueAlternativesWithoutThe.add(alternativeWithoutThe);
                    filteredAlternatives.add(alternative);
                }

                FloatingUiState.alternativesForLastCorrectAnswer = String.join(", ", filteredAlternatives);
                FloatingUiState.showAlternativesToast = !filteredAlternatives.isEmpty();
            } else {
                FloatingUiState.alternativesForLastCorrectAnswer = null;
            }

            FloatingUiState.showCloseToast = verdict.isNearMatch();
            FloatingUiState.toastPlayed = false;
            LiveSessionProgress.getInstance().ping();
        }
        else if (!verdict.isRetry()) {
            currentQuestion.markIncorrect();
            LOGGER.info("Marked incorrect: %s finished=%s", currentQuestion, currentQuestion.isFinished());
            answered = true;
            correct = false;
            FloatingUiState.lastVerdict = verdict;
            FloatingUiState.showCloseToast = false;
            lastTypedIncorrectAnswer = System.currentTimeMillis();
            FloatingUiState.toastPlayed = false;
            LiveSessionProgress.getInstance().ping();
        }

        LOGGER.info("End submit: %s judged normally", verdict);
        return verdict;
    }



    /**
     * Submit the current answer as answer for the current question. The session doesn't advance yet,
     * it just records the status for the current question, and processes the result in the database
     * and, if needed, the API.
     *
     * @param matchingKanji if this subject is a vocab item that consists of only one kanji character, this is that kanji.
     * @return the verdict indicating if the answer was correct and if a retry is permitted.
     */
    public AnswerVerdict submit(final @Nullable Subject matchingKanji) {
        final AnswerVerdict verdict = submitHelper(matchingKanji);
        adapter.addEventSubmitTypedAnswer(currentQuestion, FloatingUiState.getCurrentAnswer(), verdict);
        return verdict;
    }

    /*
     * Process a correct answer for Anki mode.
     */
    public void submitAnkiCorrect() {
        if (answered || currentQuestion == null) {
            LOGGER.info("Submit Anki correct: alreayd answered or current question is null");
            return;
        }

        currentQuestion.markCorrect();
        if (currentQuestion.getItem().isFinished()) {
            FloatingUiState.toastOldSrsStage = currentQuestion.getItem().getSrsStage();
            FloatingUiState.toastNewSrsStage = currentQuestion.getItem().getNewSrsStage();
            FloatingUiState.showSrsStageChangedToast = true;
        }
        answered = true;
        correct = true;
        FloatingUiState.lastVerdict = null;
        FloatingUiState.showCloseToast = false;
        LOGGER.info("Submit Anki correct: %s processed", currentQuestion);
        LiveSessionProgress.getInstance().ping();
        adapter.addEventSubmitAnkiCorrect(currentQuestion);
    }

    /**
     * Process an incorrect answer for Anki mode.
     */
    public void submitAnkiIncorrect() {
        if (answered || currentQuestion == null) {
            LOGGER.info("Submit Anki incorrect: alreayd answered or current question is null");
            return;
        }

        currentQuestion.markIncorrect();
        answered = true;
        correct = false;
        FloatingUiState.lastVerdict = null;
        FloatingUiState.showCloseToast = false;
        LOGGER.info("Submit Anki incorrect: %s processed", currentQuestion);
        LiveSessionProgress.getInstance().ping();
        adapter.addEventSubmitAnkiIncorrect(currentQuestion);
    }

    /**
     * Process a "don't know" answer, as if an incorrect answer had been given.
     */
    public void submitDontKnow() {
        if (answered || currentQuestion == null) {
            LOGGER.info("Submit Don't Know: alreayd answered or current question is null");
            return;
        }

        FloatingUiState.setCurrentAnswer("");
        currentQuestion.markIncorrect();
        answered = true;
        correct = false;
        FloatingUiState.lastVerdict = null;
        FloatingUiState.showCloseToast = false;
        FloatingUiState.toastPlayed = false;
        LOGGER.info("Submit Don't Know: %s processed", currentQuestion);
        LiveSessionProgress.getInstance().ping();
        adapter.addEventSubmitDontKnow(currentQuestion);
    }

    /**
     * Put a question back into the list of pending questions, if it's not already in there.
     *
     * @param question the question to put back
     */
    private void putBack(final Question question) {
        if (questions.contains(question)) {
            return;
        }
        int index = 0;
        while (index < questions.size() && question.getItem().getBucket() < questions.get(index).getItem().getBucket()) {
            index++;
        }
        questions.add(index, question);
        question.getItem().setChoiceDelay(3);
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Undo an incorrect answer, letting the user answer it again.
     */
    public void undoAndRetry() {
        if (!canUndo()) {
            LOGGER.info("Undo and retry: can't undo now");
            return;
        }

        forceNewFragment = true;
        if (currentQuestion != null && answered && currentQuestion.canUndo()) {
            LOGGER.info("Undo and retry: %s undid current question in-place", currentQuestion);
            setCurrentQuestion(currentQuestion, QuestionChoiceReason.UNDO_AND_RETRY);
            currentQuestion.undo();
        }
        else {
            setCurrentQuestion(history.pop(), QuestionChoiceReason.UNDO_AND_RETRY);
            currentQuestion.undo();
            currentItem = currentQuestion.getItem();
            WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
            WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(currentQuestion.getType());
            putBack(currentQuestion);
            LOGGER.info("Undo and retry: %s popped question from history stack", currentQuestion);
        }
        answered = false;
        correct = false;
        FloatingUiState.showDumpStage = null;
        FloatingUiState.setCurrentAnswer("");

        if (state == FINISHING) {
            state = ACTIVE;
            LiveSessionState.getInstance().post(state);
        }
        LiveSessionProgress.getInstance().ping();
        adapter.addEventUndoAndRetry(currentQuestion);
    }

    /**
     * Undo an incorrect answer, putting the question back into the queue.
     */
    public void undoAndPutBack() {
        if (!canUndo()) {
            LOGGER.info("Undo and put back: can't undo now");
            return;
        }

        forceNewFragment = true;
        if (currentQuestion != null && answered && currentQuestion.canUndo()) {
            currentQuestion.undo();
            putBack(currentQuestion);
            LOGGER.info("Undo and put back: %s undid current question in-place", currentQuestion);
            adapter.addEventUndoAndPutBack(currentQuestion);
        }
        else {
            final Question question = history.pop();
            question.undo();
            putBack(question);
            LOGGER.info("Undo and put back: %s popped question from history stack", question);
            adapter.addEventUndoAndPutBack(question);
        }
        answered = false;
        correct = false;
        setCurrentQuestion(null, QuestionChoiceReason.UNDO_AND_PUT_BACK);
        currentItem = null;
        FloatingUiState.showDumpStage = null;
        FloatingUiState.setCurrentAnswer("");

        if (state == FINISHING) {
            state = ACTIVE;
            LiveSessionState.getInstance().post(state);
        }
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Skip a question, putting it back into the queue.
     */
    public void skip() {
        if (answered) {
            LOGGER.info("Skip: can't skip now");
            return;
        }

        if (currentItem != null) {
            currentItem.setChoiceDelay(3);
        }

        adapter.addEventSkip(currentQuestion);

        forceNewFragment = true;
        setCurrentQuestion(null, QuestionChoiceReason.SKIP);
        currentItem = null;
        FloatingUiState.setCurrentAnswer("");

        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Ignore an incorrect answer and mark as correct instead.
     */
    public void ignore() {
        if (!canUndo()) {
            LOGGER.info("Ignore: can't undo now");
            return;
        }

        forceNewFragment = true;
        if (currentQuestion != null && answered && currentQuestion.canUndo()) {
            LOGGER.info("Ignore: %s undid current question in-place", currentQuestion);
            setCurrentQuestion(currentQuestion, QuestionChoiceReason.UNDO_AND_RETRY);
            currentQuestion.undo();
        }
        else {
            setCurrentQuestion(history.pop(), QuestionChoiceReason.UNDO_AND_RETRY);
            currentQuestion.undo();
            currentItem = currentQuestion.getItem();
            WkApplication.getDatabase().propertiesDao().setCurrentItemId(currentItem.getId());
            WkApplication.getDatabase().propertiesDao().setCurrentQuestionType(currentQuestion.getType());
            putBack(currentQuestion);
            LOGGER.info("Ignore: %s popped question from history stack", currentQuestion);
        }
        FloatingUiState.showDumpStage = null;
        FloatingUiState.setCurrentAnswer("");

        if (state == FINISHING) {
            state = ACTIVE;
            LiveSessionState.getInstance().post(state);
        }

        currentQuestion.markCorrect();
        if (currentQuestion.getItem().isFinished()) {
            FloatingUiState.toastOldSrsStage = currentQuestion.getItem().getSrsStage();
            FloatingUiState.toastNewSrsStage = currentQuestion.getItem().getNewSrsStage();
            FloatingUiState.showSrsStageChangedToast = true;
        }
        answered = true;
        correct = true;
        FloatingUiState.lastVerdict = null;
        FloatingUiState.showCloseToast = false;
        LOGGER.info("Ignore: %s processed", currentQuestion);
        LiveSessionProgress.getInstance().ping();
        adapter.addEventIgnore(currentQuestion);
    }

    /**
     * Get the typeface configuration used to display the current question's text.
     *
     * @param text the question text to show, for testing compatibility
     * @return the typeface config, never null, can be TypefaceConfiguration.DEFAULT as a fallback
     */
    public TypefaceConfiguration getCurrentTypefaceConfiguration(final String text) {
        if (currentItem == null) {
            return TypefaceConfiguration.DEFAULT;
        }

        return currentItem.getTypefaceConfiguration(text);
    }

    /**
     * Determine the fragment to show for the current session state.
     * The session is known to be not inactive, that case is handled earlier.
     *
     * @param currentFragment The current fragment, or null if the activity is starting fresh
     * @param ankiMode True if Anki mode is enabled for this question
     * @return the fragment, possible the same instance as currentFragment
     */
    @SuppressWarnings("IfMayBeConditional")
    public AbstractSessionFragment getNewFragment(final @Nullable AbstractSessionFragment currentFragment,
                                                  final boolean ankiMode) {
        final @Nullable Question question = currentQuestion;
        final @Nullable SessionItem item = currentItem;

        final boolean oldForceNewFragment = forceNewFragment;
        forceNewFragment = false;

        if (isInLessonPresentation()) {
            requireNonNull(item);
            if (!oldForceNewFragment && currentFragment instanceof LessonSessionFragment && currentFragment.getItem() == item) {
                return currentFragment;
            }
            else {
                return LessonSessionFragment.newInstance(item.getId());
            }
        }

        if (isFinishing()) {
            if (!oldForceNewFragment && currentFragment instanceof SummarySessionFragment) {
                return currentFragment;
            }
            else {
                return new SummarySessionFragment();
            }
        }

        if (ankiMode) {
            requireNonNull(item);
            requireNonNull(question);
            if (!oldForceNewFragment && currentFragment instanceof AnkiSessionFragment && currentFragment.getQuestion() == question) {
                return currentFragment;
            }
            else {
                return AnkiSessionFragment.newInstance(item.getId(), question.getType());
            }
        }

        if (answered) {
            requireNonNull(item);
            requireNonNull(question);
            if (!oldForceNewFragment && currentFragment instanceof AnsweredSessionFragment && currentFragment.getQuestion() == question) {
                return currentFragment;
            }
            else {
                return AnsweredSessionFragment.newInstance(item.getId(), question.getType());
            }
        }

        if (!oldForceNewFragment && currentFragment instanceof UnansweredSessionFragment && currentFragment.getQuestion() == question) {
            return currentFragment;
        }

        requireNonNull(item);
        requireNonNull(question);
        return UnansweredSessionFragment.newInstance(item.getId(), question.getType());
    }

    /**
     * Check the list of questions for the session, and remove finished ones.
     */
    private void checkQuestions() {
        if (currentQuestion != null && currentQuestion.isFinished()) {
            currentItem = null;
            setCurrentQuestion(null, QuestionChoiceReason.CLEANUP);
        }

        int i=0;
        while (i < questions.size()) {
            final Question question = questions.get(i);
            if (question.isFinished()) {
                questions.remove(i);
            }
            else {
                i++;
            }
        }

        for (final Question question: new ArrayList<>(history)) {
            if (question.getItem().isAbandoned()) {
                history.remove(question);
            }
        }

        if (state == ACTIVE && questions.isEmpty()) {
            state = FINISHING;
            LiveSessionState.getInstance().post(state);
        }

        LOGGER.info("Questions after checking: %d", questions.size());
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Create question instances for the session's items.
     */
    private void createQuestions() {
        questions = new ArrayList<>();
        for (final SessionItem item: items) {
            final Subject subject = requireNonNull(item.getSubject());
            for (final QuestionType questionType: subject.getType().getPossibleQuestionTypes(onkun)) {
                final Question question = new Question(item, questionType);
                item.addQuestion(question);
                questions.add(question);
            }
        }

        LOGGER.info("Questions created: %d", questions.size());
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * If the app process has been restarted with an active session,
     * repopulate it from the database.
     */
    @SuppressLint("NewApi")
    public void load() {
        if (loaded) {
            return;
        }
        try {
            final AppDatabase db = WkApplication.getDatabase();
            type = db.propertiesDao().getSessionType();
            if (type == NONE) {
                return;
            }
            onkun = db.propertiesDao().getSessionOnkun();
            delayed = GlobalSettings.Review.getDelayResultUpload();
            backToBack = GlobalSettings.getBackToBack(type);
            readingFirst = GlobalSettings.getReadingFirst(type);
            meaningFirst = GlobalSettings.getMeaningFirst(type);
            comparator = GlobalSettings.getSubjectComparator(type);
            if (GlobalSettings.getOrderReversed(type)) {
                comparator = comparator.reversed();
            }
            if (GlobalSettings.getOrderOverdueFirst(type)) {
                final Comparator<Subject> c = (o1, o2) -> Boolean.compare(o2.isOverdue(), o1.isOverdue());
                comparator = c.thenComparing(comparator);
            }
            final int userLevel = db.propertiesDao().getUserLevel();
            final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();
            comparator = GlobalSettings.getOrderPriority(type).getComparator(
                    comparator,
                    db.subjectCollectionsDao().getLevelUpIds(userLevel, maxLevel),
                    userLevel,
                    maxLevel);
            items = new ArrayList<>();
            history.clear();
            final Map<Long, Subject> subjects = db.subjectCollectionsDao().getSessionSubjects();
            for (final SessionItem item: db.sessionItemDao().getAll()) {
                final @Nullable Subject subject = subjects.get(item.getId());
                if (subject != null) {
                    if (!subject.isEligibleForSessionType(type)) {
                        continue;
                    }
                    item.setSubject(subject);
                    items.add(item);
                }
            }
            if (!items.isEmpty()) {
                createQuestions();
                state = type == LESSON && getNumStartedItems() == 0 ? IN_LESSON_PRESENTATION : ACTIVE;
                adapter.clear();
                adapter.addEventLoadSession(type);
                LiveSessionState.getInstance().post(state);
                checkQuestions();
                lastFinishedSubjectId = -1;
                final long currentItemId = db.propertiesDao().getCurrentItemId();
                final QuestionType currentQuestionType = db.propertiesDao().getCurrentQuestionType();
                if (state == IN_LESSON_PRESENTATION) {
                    for (final SessionItem item : items) {
                        if (!item.isActive()) {
                            continue;
                        }
                        if (item.getId() == currentItemId) {
                            currentItem = item;
                            setCurrentQuestion(questions.get(0), QuestionChoiceReason.LOAD);
                            break;
                        }
                    }
                }
                else {
                    for (final Question question : questions) {
                        if (!question.getItem().isActive()) {
                            continue;
                        }
                        if (question.isFinished()) {
                            continue;
                        }
                        if (question.getItem().getId() == currentItemId && question.getType() == currentQuestionType) {
                            currentItem = question.getItem();
                            setCurrentQuestion(question, QuestionChoiceReason.LOAD);
                            break;
                        }
                    }
                }
            }
        } finally {
            loaded = true;
            LiveSessionState.getInstance().post(state);
        }
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Finish or abandon the session, removing all items and questions.
     */
    public void finish() {
        LOGGER.info("Finishing session");
        items.clear();
        questions.clear();
        history.clear();
        adapter.clear();
        state = INACTIVE;
        FloatingUiState.setCurrentAnswer("");
        setCurrentQuestion(null, QuestionChoiceReason.FINISHED);
        currentItem = null;
        LiveSessionState.getInstance().post(state);
        JobRunnerService.schedule(FinishSessionJob.class, type.toString());
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Force reset the state in the database, as part of resetting the app database.
     */
    public void reset() {
        items.clear();
        questions.clear();
        history.clear();
        adapter.clear();
        state = INACTIVE;
        LiveSessionState.getInstance().post(state);
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Wrap up the session. Any items that have not been started yet are removed from the session,
     * so that when the started items are all finished, the session is done.
     */
    public void wrapup() {
        LOGGER.info("Wrapping up session");
        if (state == IN_LESSON_PRESENTATION) {
            finish();
            return;
        }

        for (final SessionItem item: items) {
            if (item.isActive() && !item.isStarted()) {
                item.setState(ABANDONED);
                JobRunnerService.schedule(AbandonSessionItemJob.class, Long.toString(item.getId()));
            }
        }
        if (currentQuestion != null && currentQuestion.getItem().isAbandoned()) {
            currentItem = null;
            setCurrentQuestion(null, QuestionChoiceReason.WRAPUP);
        }
        checkQuestions();
        LiveSessionProgress.getInstance().ping();
    }

    private List<Subject> trimSelection(final List<Subject> candidates, final int maxSize) {
        final SubjectSelectionRules rules = GlobalSettings.getSubjectSelectionRules(type);
        if (rules.isEmpty()) {
            return new ArrayList<>(candidates.subList(0, maxSize));
        }

        final List<Subject> result = new ArrayList<>();

        for (int stage=0; stage<5; stage++) {
            int i = 0;
            while (i < candidates.size()) {
                if (result.size() >= maxSize) {
                    break;
                }
                final Subject subject = candidates.get(i);
                if (rules.isWantedForStage(subject, stage)) {
                    result.add(subject);
                    candidates.remove(i);
                    rules.notifySelected(subject);
                }
                else {
                    i++;
                }
            }
        }

        Collections.sort(result, comparator);
        return result;
    }

    /**
     * Given a list of subjects, populate the session with items for all subjects.
     *
     * @param subjects the list of subjects
     * @param maxSize the maximum size of the session
     * @param shuffle should the subject list be shuffled before applying the ordering rules
     */
    @SuppressLint("NewApi")
    private void populateItems(final List<Subject> subjects, final int maxSize, final boolean shuffle) {
        LOGGER.info("Starting %s session", type);

        List<Subject> list = new ArrayList<>(subjects);
        if (shuffle) {
            Collections.shuffle(list);
        }

        Collections.sort(list, comparator);

        if (list.size() > maxSize) {
            list = trimSelection(list, maxSize);
        }

        LOGGER.info("Subject IDs for session: %s", list.stream().map(Subject::getId).map(Object::toString).collect(Collectors.joining(",")));

        if (GlobalSettings.getShuffleAfterSelection(type)) {
            comparator = LessonOrder.SHUFFLE.getComparator();
        }

        if (GlobalSettings.Api.getAutoDownloadAudio()) {
            AudioUtil.scheduleDownloadTasks(list, 100);
        }

        if (GlobalSettings.SubjectInfo.getShowPitchInfo()) {
            PitchInfoUtil.scheduleDownloadTasks(list, 100);
        }

        final AppDatabase db = WkApplication.getDatabase();
        db.sessionItemDao().deleteAll();

        items = new ArrayList<>();
        int index = 0;
        int currentBucket = 0;
        @Nullable Subject prev = null;
        for (final Subject subject: list) {
            if (prev != null && comparator.compare(prev, subject) != 0) {
                currentBucket++;
            }

            final SessionItem item = new SessionItem();
            item.setId(subject.getId());
            item.setAssignmentId(subject.getAssignmentId());
            item.setState(SessionItemState.ACTIVE);
            item.setSrsStage(subject.getSrsStage());
            item.setLevel(subject.getLevel());
            item.setBucket(currentBucket);
            item.setOrder(index++);
            item.setNumAnswers(0);
            item.setKanjiAcceptedReadingType(subject.getKanjiAcceptedReadingType());
            item.setSubject(subject);

            item.setQuestion1Done(!subject.needsQuestion1());
            item.setQuestion2Done(!subject.needsQuestion2(onkun));
            item.setQuestion3Done(!subject.needsQuestion3(onkun));
            item.setQuestion4Done(!subject.needsQuestion4(onkun));

            item.setQuestion1Incorrect(0);
            item.setQuestion2Incorrect(0);
            item.setQuestion3Incorrect(0);
            item.setQuestion4Incorrect(0);

            db.sessionItemDao().insert(item);
            items.add(item);

            prev = subject;
        }

        LOGGER.info("Items created: %d", items.size());
    }

    /**
     * Start a new lesson session for a given list of subjects.
     *
     * @param subjects list of subjects
     */
    @SuppressLint("NewApi")
    public void startNewLessonSession(final List<Subject> subjects) {
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException();
        }
        type = LESSON;
        onkun = GlobalSettings.AdvancedOther.getKanjiModeOnKun();
        delayed = GlobalSettings.Review.getDelayResultUpload();
        backToBack = GlobalSettings.AdvancedLesson.getBackToBack();
        readingFirst = GlobalSettings.AdvancedLesson.getReadingFirst();
        meaningFirst = GlobalSettings.AdvancedLesson.getMeaningFirst();
        final AppDatabase db = WkApplication.getDatabase();
        db.propertiesDao().setSessionType(type);
        db.propertiesDao().setSessionOnkun(onkun);

        final int maxSize = GlobalSettings.Review.gexMaxLessonSessionSize();
        comparator = GlobalSettings.AdvancedLesson.getOrder().getComparator();
        if (GlobalSettings.AdvancedLesson.getOrderReversed()) {
            comparator = comparator.reversed();
        }
        final int userLevel = db.propertiesDao().getUserLevel();
        final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();
        comparator = GlobalSettings.AdvancedLesson.getOrderPriority().getComparator(
                comparator,
                db.subjectCollectionsDao().getLevelUpIds(userLevel, maxLevel),
                userLevel,
                maxLevel);

        history.clear();
        populateItems(subjects, maxSize, GlobalSettings.AdvancedLesson.getOrder().isShuffle());
        createQuestions();
        state = IN_LESSON_PRESENTATION;
        adapter.clear();
        adapter.addEventStartSession(type);
        currentItem = null;
        setCurrentQuestion(null, QuestionChoiceReason.STARTING_LESSON_SESSION);
        lastFinishedSubjectId = -1;
        LiveSessionState.getInstance().post(state);
        checkQuestions();
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Start a new review session for a given list of subjects.
     *
     * @param subjects list of subjects
     */
    @SuppressLint("NewApi")
    public void startNewReviewSession(final List<Subject> subjects) {
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException();
        }
        type = REVIEW;
        onkun = GlobalSettings.AdvancedOther.getKanjiModeOnKun();
        delayed = GlobalSettings.Review.getDelayResultUpload();
        backToBack = GlobalSettings.AdvancedReview.getBackToBack();
        readingFirst = GlobalSettings.AdvancedReview.getReadingFirst();
        meaningFirst = GlobalSettings.AdvancedReview.getMeaningFirst();
        final AppDatabase db = WkApplication.getDatabase();
        db.propertiesDao().setSessionType(type);
        db.propertiesDao().setSessionOnkun(onkun);

        final int maxSize = GlobalSettings.Review.gexMaxReviewSessionSize();
        comparator = GlobalSettings.AdvancedReview.getOrder().getComparator();
        if (GlobalSettings.AdvancedReview.getOrderReversed()) {
            comparator = comparator.reversed();
        }
        if (GlobalSettings.AdvancedReview.getOrderOverdueFirst()) {
            final Comparator<Subject> c = (o1, o2) -> Boolean.compare(o2.isOverdue(), o1.isOverdue());
            comparator = c.thenComparing(comparator);
        }
        final int userLevel = db.propertiesDao().getUserLevel();
        final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();
        comparator = GlobalSettings.AdvancedReview.getOrderPriority().getComparator(
                comparator,
                db.subjectCollectionsDao().getLevelUpIds(userLevel, maxLevel),
                userLevel,
                maxLevel);

        history.clear();
        populateItems(subjects, maxSize, true);
        createQuestions();
        state = ACTIVE;
        adapter.clear();
        adapter.addEventStartSession(type);
        currentItem = null;
        setCurrentQuestion(null, QuestionChoiceReason.STARTING_REVIEW_SESSION);
        lastFinishedSubjectId = -1;
        LiveSessionState.getInstance().post(state);
        checkQuestions();
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Start a new self-study session for a given list of subjects.
     *
     * @param subjects list of subjects
     */
    @SuppressLint("NewApi")
    public void startNewSelfStudySession(final List<Subject> subjects) {
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException();
        }
        type = SELF_STUDY;
        onkun = GlobalSettings.AdvancedOther.getKanjiModeOnKun();
        delayed = GlobalSettings.Review.getDelayResultUpload();
        backToBack = GlobalSettings.AdvancedSelfStudy.getBackToBack();
        readingFirst = GlobalSettings.AdvancedSelfStudy.getReadingFirst();
        meaningFirst = GlobalSettings.AdvancedSelfStudy.getMeaningFirst();
        final AppDatabase db = WkApplication.getDatabase();
        db.propertiesDao().setSessionType(type);
        db.propertiesDao().setSessionOnkun(onkun);

        final int maxSize = GlobalSettings.Review.getMaxSelfStudySessionSize();
        comparator = GlobalSettings.AdvancedSelfStudy.getOrder().getComparator();
        if (GlobalSettings.AdvancedSelfStudy.getOrderReversed()) {
            comparator = comparator.reversed();
        }
        if (GlobalSettings.AdvancedSelfStudy.getOrderOverdueFirst()) {
            final Comparator<Subject> c = (o1, o2) -> Boolean.compare(o2.isOverdue(), o1.isOverdue());
            comparator = c.thenComparing(comparator);
        }
        final int userLevel = db.propertiesDao().getUserLevel();
        final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();
        comparator = GlobalSettings.AdvancedSelfStudy.getOrderPriority().getComparator(
                comparator,
                db.subjectCollectionsDao().getLevelUpIds(userLevel, maxLevel),
                userLevel,
                maxLevel);

        history.clear();
        populateItems(subjects, maxSize, true);
        createQuestions();
        state = ACTIVE;
        adapter.clear();
        adapter.addEventStartSession(type);
        currentItem = null;
        setCurrentQuestion(null, QuestionChoiceReason.STARTING_SELF_STUDY_SESSION);
        lastFinishedSubjectId = -1;
        LiveSessionState.getInstance().post(state);
        checkQuestions();
        LiveSessionProgress.getInstance().ping();
    }

    /**
     * Find an item in the current session that is for the given subject ID.
     *
     * @param id the subject ID
     * @return the item or null if not found
     */
    @SuppressLint("NewApi")
    public @Nullable SessionItem findItemBySubjectId(final long id) {
        return items.stream().filter(item -> item.getId() == id).findAny().orElse(null);
    }

    @Override
    public void onSubjectChange(final Subject subject) {
        final @Nullable SessionItem item = findItemBySubjectId(subject.getId());
        if (item != null) {
            item.setSubject(subject);
            if (item.isAlive() && isActive() && !subject.isAssignmentPatched() && !subject.isEligibleForSessionType(type)) {
                LOGGER.info("Subject %d %s is no longer eligible for this session - removing it", subject.getId(), subject.getCharacters());
                item.setState(ABANDONED);
                checkQuestions();
                LiveSessionProgress.getInstance().ping();
            }
        }
    }

    @Override
    public boolean isInterestedInSubject(final long subjectId) {
        return findItemBySubjectId(subjectId) != null;
    }
}
