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

import com.lazy_durtles.wk.model.Question;

import javax.annotation.Nullable;

/**
 * What to show in subject info dump after a question has been answered.
 */
public enum SubjectInfoDump {
    /**
     * Show nothing.
     */
    @SuppressWarnings("unused")
    NOTHING() {
        @Override
        public boolean getShowMeaningAnswers(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowReadingAnswers(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowMeaningRelated(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowReadingRelated(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowRevealButton() {
            return true;
        }

        @Override
        public String getRevealButtonLabel() {
            return "Show answer";
        }

        @SuppressWarnings("SuspiciousGetterSetter")
        @Override
        public SubjectInfoDump getNextStage() {
            return ANSWERS_ONLY;
        }
    },

    /**
     * Only show answers for the current question.
     */
    ANSWERS_ONLY() {
        @Override
        public boolean getShowMeaningAnswers(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isMeaning() || question.getItem().isQuestion1Done();
        }

        @Override
        public boolean getShowReadingAnswers(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isReading()
                    || question.getItem().isQuestion2Done() && question.getItem().isQuestion3Done() && question.getItem().isQuestion4Done();
        }

        @Override
        public boolean getShowMeaningRelated(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowReadingRelated(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowRevealButton() {
            return true;
        }

        @Override
        public String getRevealButtonLabel() {
            return "Show more";
        }

        @SuppressWarnings("SuspiciousGetterSetter")
        @Override
        public SubjectInfoDump getNextStage() {
            return HIDE_UNQUIZZED;
        }
    },

    /**
     * Hide unquizzed info, e.g. hide readings for meaning question if
     * reading hasn't been answered correctly yet.
     */
    HIDE_UNQUIZZED() {
        @Override
        public boolean getShowMeaningAnswers(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isMeaning() || question.getItem().isQuestion1Done();
        }

        @Override
        public boolean getShowReadingAnswers(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isReading()
                    || question.getItem().isQuestion2Done() && question.getItem().isQuestion3Done() && question.getItem().isQuestion4Done();
        }

        @Override
        public boolean getShowMeaningRelated(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isMeaning() || question.getItem().isQuestion1Done();
        }

        @Override
        public boolean getShowReadingRelated(final @Nullable Question question) {
            if (question == null) {
                return false;
            }
            return question.getType().isReading()
                    || question.getItem().isQuestion2Done() && question.getItem().isQuestion3Done() && question.getItem().isQuestion4Done();
        }

        @Override
        public boolean getShowRevealButton() {
            return true;
        }

        @Override
        public String getRevealButtonLabel() {
            return "Show all";
        }

        @SuppressWarnings("SuspiciousGetterSetter")
        @Override
        public SubjectInfoDump getNextStage() {
            return ALL;
        }
    },

    /**
     * Hide the reading and reading-related info.
      */
    HIDE_READING_RELATED() {
        @Override
        public boolean getShowMeaningAnswers(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowReadingAnswers(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowMeaningRelated(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowReadingRelated(final @Nullable Question question) {
            return false;
        }

        @Override
        public boolean getShowRevealButton() {
            return true;
        }

        @Override
        public String getRevealButtonLabel() {
            return "Show all";
        }

        @SuppressWarnings("SuspiciousGetterSetter")
        @Override
        public SubjectInfoDump getNextStage() {
            return ALL;
        }
    },

    /**
     * Show everything.
     */
    ALL() {
        @Override
        public boolean getShowMeaningAnswers(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowReadingAnswers(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowMeaningRelated(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowReadingRelated(final @Nullable Question question) {
            return true;
        }

        @Override
        public boolean getShowRevealButton() {
            return false;
        }

        @Override
        public String getRevealButtonLabel() {
            return "";
        }

        @SuppressWarnings("SuspiciousGetterSetter")
        @Override
        public SubjectInfoDump getNextStage() {
            return ALL;
        }
    };

    /**
     * Should the subject info view show meaning answers in this state?.
     *
     * @param question the current question
     * @return true if it should
     */
    public abstract boolean getShowMeaningAnswers(@Nullable Question question);

    /**
     * Should the subject info view show reading answers in this state?.
     *
     * @param question the current question
     * @return true if it should
     */
    public abstract boolean getShowReadingAnswers(@Nullable Question question);

    /**
     * Should the subject info view show meaning related info in this state?.
     *
     * @param question the current question
     * @return true if it should
     */
    public abstract boolean getShowMeaningRelated(@Nullable Question question);

    /**
     * Should the subject info view show reading related info in this state?.
     *
     * @param question the current question
     * @return true if it should
     */
    public abstract boolean getShowReadingRelated(@Nullable Question question);

    /**
     * Show the subject headline view show a reveal button?.
     *
     * @return true if it should
     */
    public abstract boolean getShowRevealButton();

    /**
     * Get the applicable label for the reveal button.
     *
     * @return the label
     */
    public abstract String getRevealButtonLabel();

    /**
     * Get the next stage to move to when the reveal button is pressed.
     *
     * @return the next stage
     */
    public abstract SubjectInfoDump getNextStage();
}
