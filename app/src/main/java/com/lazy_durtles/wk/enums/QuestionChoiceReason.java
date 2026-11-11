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

import static com.lazy_durtles.wk.enums.FragmentTransitionAnimation.LTR;
import static com.lazy_durtles.wk.enums.FragmentTransitionAnimation.NONE;
import static com.lazy_durtles.wk.enums.FragmentTransitionAnimation.RTL;

/**
 * An enum that encodes the reason the current session question was chosen, or if Session.currentQuestion is null,
 * the reason it was set to null in preparation for the next chooseQuestion() call.
 */
public enum QuestionChoiceReason {
    /**
     * The current session is being reloaded from the database after a crash/force quit.
     */
    LOAD(NONE),

    /**
     * A new lesson session is starting.
     */
    STARTING_LESSON_SESSION(NONE),

    /**
     * A new review session is starting.
     */
    STARTING_REVIEW_SESSION(NONE),

    /**
     * A new self-study session is starting.
     */
    STARTING_SELF_STUDY_SESSION(NONE),

    /**
     * Move to the next lesson item in lesson presentation.
     */
    MOVE_TO_NEXT_LESSON_ITEM(RTL),

    /**
     * Move to the previous lesson item in lesson presentation.
     */
    MOVE_TO_PREVIOUS_LESSON_ITEM(LTR),

    /**
     * The quiz part of a lesson session has been started.
     */
    STARTING_QUIZ(RTL),

    /**
     * The quiz part of a lesson session has been started.
     */
    BACK_TO_PRESENTATION(LTR),

    /**
     * The user is progressing normally to the next question. Either they answered a question and then clicked Next,
     * or the user has lightning mode enabled and answered a question correctly.
     */
    NEXT_NATURAL(RTL),

    /**
     * The current question was removed from the session because of a background sync.
     */
    NEXT_FORCED(RTL),

    /**
     * The special button Undo and retry was used.
     */
    UNDO_AND_RETRY(LTR),

    /**
     * The special button Undo and put back/Ignore was used.
     */
    UNDO_AND_PUT_BACK(LTR),

    /**
     * The special button Skip was used.
     */
    SKIP(RTL),

    /**
     * The question pool for this session was cleaned up, and the current question disappeared as a result.
     * Cleanup happens when a subject is forced out the session during a sync, after wrapup, on load, and when starting a new session.
     */
    CLEANUP(RTL),

    /**
     * The user chose to wrap up the session, and the current question was one of the questions abandoned in the wrapup.
     */
    WRAPUP(RTL),

    /**
     * All questions have been completed, the session goes into the finishing stage for the summary.
     */
    FINISHING_SESSION(RTL),

    /**
     * The session is finished (state INACTIVE).
     */
    FINISHED(NONE);

    private final FragmentTransitionAnimation animation;

    QuestionChoiceReason(final FragmentTransitionAnimation animation) {
        this.animation = animation;
    }

    /**
     * The animation type associated with this reason.
     *
     * @return the animation
     */
    public FragmentTransitionAnimation getAnimation() {
        return animation;
    }
}
