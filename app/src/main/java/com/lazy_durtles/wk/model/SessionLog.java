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

import com.lazy_durtles.wk.db.model.SessionItem;
import com.lazy_durtles.wk.enums.SessionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The event log of a session: a list of events, most recent first, shown in
 * the session log. Formerly part of the legacy SessionLogAdapter.
 */
public final class SessionLog {
    private final List<EventItem> events = new ArrayList<>();

    /**
     * Clear the state for a new session.
     */
    public void clear() {
        events.clear();
    }

    /**
     * The events recorded so far, most recent first.
     *
     * @return an unmodifiable view of the events
     */
    public List<EventItem> getEvents() {
        return Collections.unmodifiableList(events);
    }

    private void add(final @Nullable SessionItem sessionItem, final String text) {
        events.add(0, new EventItem(sessionItem, text));
    }

    /**
     * Record the start of a session.
     *
     * @param type the session type
     */
    public void addEventStartSession(final SessionType type) {
        add(null, String.format(Locale.ROOT, "%s session started", type.getDescription()));
    }

    /**
     * Record the reload of a session on app startup.
     *
     * @param type the session type
     */
    public void addEventLoadSession(final SessionType type) {
        add(null, String.format(Locale.ROOT, "%s session re-loaded on app startup", type.getDescription()));
    }

    /**
     * Record the start of a lesson quiz.
     */
    public void addEventStartQuiz() {
        add(null, "Started lesson quiz");
    }

    /**
     * Record jumping back to the lesson presentation.
     */
    public void addEventBackToLessonPresentation() {
        add(null, "Jumped back to lesson presentation");
    }

    /**
     * Record a submitted typed answer.
     *
     * @param question the question or null
     * @param answer the submitted answer
     * @param verdict the verdict for the answer
     */
    public void addEventSubmitTypedAnswer(final @Nullable Question question, final String answer, final AnswerVerdict verdict) {
        if (question == null || verdict.isRetry()) {
            return;
        }
        add(question.getItem(), String.format(Locale.ROOT, "Submit %s: %s\n%s",
                question.getType().getShortTitle(),
                answer,
                verdict.isOk() ? "Correct" : "Incorrect"));
    }

    /**
     * Record an Anki-mode correct answer.
     *
     * @param question the question
     */
    public void addEventSubmitAnkiCorrect(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Submit: %s\nAnki correct", question.getType().getShortTitle()));
    }

    /**
     * Record an Anki-mode incorrect answer.
     *
     * @param question the question
     */
    public void addEventSubmitAnkiIncorrect(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Submit: %s\nAnki incorrect", question.getType().getShortTitle()));
    }

    /**
     * Record a "don't know" answer.
     *
     * @param question the question
     */
    public void addEventSubmitDontKnow(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Submit: %s\n'Don't know' incorrect", question.getType().getShortTitle()));
    }

    /**
     * Record an undo-and-retry.
     *
     * @param question the question
     */
    public void addEventUndoAndRetry(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Undo and retry immediately: %s", question.getType().getShortTitle()));
    }

    /**
     * Record an undo-and-put-back.
     *
     * @param question the question
     */
    public void addEventUndoAndPutBack(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Undo and put back: %s", question.getType().getShortTitle()));
    }

    /**
     * Record a skip.
     *
     * @param question the question or null
     */
    public void addEventSkip(final @Nullable Question question) {
        if (question == null) {
            return;
        }
        add(question.getItem(), String.format(Locale.ROOT, "Skip: %s", question.getType().getShortTitle()));
    }

    /**
     * Record an ignore-and-mark-correct.
     *
     * @param question the question
     */
    public void addEventIgnore(final Question question) {
        add(question.getItem(), String.format(Locale.ROOT, "Ignore and mark correct: %s", question.getType().getShortTitle()));
    }
}
