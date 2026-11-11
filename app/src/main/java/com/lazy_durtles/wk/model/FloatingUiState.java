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

import com.lazy_durtles.wk.enums.SubjectInfoDump;
import com.lazy_durtles.wk.util.Logger;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

/**
 * A class encapsulating some UI-related state that needs to survive across activity/fragment navigation
 * and suspend/resume. Does not use the activity's saved state, since it needs to move between
 * e.g. the quiz fragments and the session summary fragment, and it also needs to be available to
 * the session singleton.
 */
@SuppressWarnings("StaticNonFinalField")
public final class FloatingUiState {
    private static final Logger LOGGER = Logger.get(FloatingUiState.class);

    /**
     * Is there an SRS stage change toast to be shown?.
     */
    public static boolean showSrsStageChangedToast = false;
    /**
     * Is there an alternatives toast to be shown?.
     */
    public static boolean showAlternativesToast = false;
    /**
     * For the toast, the old stage.
     */
    public static SrsSystem.Stage toastOldSrsStage = SrsSystemRepository.getSrsSystem(1).getLockedStage();

    /**
     * For the toast, the new stage.
     */
    public static SrsSystem.Stage toastNewSrsStage = SrsSystemRepository.getSrsSystem(1).getLockedStage();

    /**
     * The verdict for the last given answer.
     */
    public static @Nullable AnswerVerdict lastVerdict = null;
    /**
     * The alternatives for the last correct answer
     */
    public static @Nullable String alternativesForLastCorrectAnswer = null;
    /**
     * The subject info dump reveal stage the subject info is in.
     */
    public static @Nullable SubjectInfoDump showDumpStage = null;

    /**
     * True if the audio for the current question has been played already.
     */
    public static boolean audioPlayed = false;

    /**
     * True if the correct/incorrect toast for the just-answered question has been played already.
     */
    public static boolean toastPlayed = false;

    /**
     * Does the session need to linger on the answer after a correct answer,
     * i.e. is lightning mode disabled for the current question?.
     */
    public static boolean lingerOnAnswer = false;

    /**
     * Should a 'not quite but close enough' toast be shown?.
     */
    public static boolean showCloseToast = false;

    private static String currentAnswer = "";

    private FloatingUiState() {
        //
    }

    /**
     * The current answer for the current question. This value may not have been
     * submitted yet. Empty if no answer has been entered yet, or a new question is
     * being presented, or this is Anki mode.
     * @return the value
     */
    public static String getCurrentAnswer() {
        return currentAnswer;
    }

    /**
     * The current answer for the current question. This value may not have been
     * submitted yet. Empty if no answer has been entered yet, or a new question is
     * being presented, or this is Anki mode.
     * @param currentAnswer the value
     */
    public static void setCurrentAnswer(final @Nullable String currentAnswer) {
        FloatingUiState.currentAnswer = orElse(currentAnswer, "");
        LOGGER.info("Current answer set to: '%s'", currentAnswer);
    }

    /**
     * Get the last answer that matched a correct answer, if known.
     *
     * @return the last answer
     */
    public static @Nullable String getLastMatchedAnswer() {
        if (lastVerdict == null) {
            return null;
        }
        return lastVerdict.getMatchedAnswer();
    }
}
