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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The "verdict" after judging a typed answer to a quiz question. Encodes all of the
 * information that may be needed for later actions based on the verdict.
 */
public final class AnswerVerdict {
    /**
     * Predefined constant for common case: answer was incorrect, but user can retry.
     */
    public static final AnswerVerdict NOK_WITH_RETRY = new AnswerVerdict(false, true, false, null, null, null);

    /**
     * Predefined constant for common case: answer was incorrect, and user can not retry.
     */
    public static final AnswerVerdict NOK_WITHOUT_RETRY = new AnswerVerdict(false, false, false, null, null, null);

    private final boolean ok;
    private final boolean retry;
    private final boolean nearMatch;
    private final @Nullable String givenAnswer;
    private final @Nullable String matchedAnswer;
    private final @Nullable DigraphMatch digraphMatch;

    /**
     * The constructor.
     *
     * @param ok value for ok field
     * @param retry value for retry field
     * @param nearMatch value for nearMatch field
     * @param givenAnswer value for givenAnswer field
     * @param matchedAnswer value for matchedAnswer field
     * @param digraphMatch vaule for digraphMatch field
     */
    public AnswerVerdict(final boolean ok, final boolean retry, final boolean nearMatch,
                         final @Nullable String givenAnswer, final @Nullable String matchedAnswer,
                         final @Nullable DigraphMatch digraphMatch) {
        this.ok = ok;
        this.retry = retry;
        this.nearMatch = nearMatch;
        this.givenAnswer = givenAnswer;
        this.matchedAnswer = matchedAnswer;
        this.digraphMatch = digraphMatch;
    }

    /**
     * Is the answer considered correct?.
     * @return the value
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * If ok is false, can the user retry without recording the incorrect answer yet?.
     * @return the value
     */
    public boolean isRetry() {
        return retry;
    }

    /**
     * Is the answer a "close enough" meaning that is not exactly correct but is accepted by the typo lenience?.
     * @return the value
     */
    public boolean isNearMatch() {
        return nearMatch;
    }

    /**
     * If nearMatch is true, this is the given answer.
     * @return the value
     */
    public @Nullable String getGivenAnswer() {
        return givenAnswer;
    }

    /**
     * If ok is true, this is the answer that made the verdict ok. Specifically,
     * if nearMatch is true, this is the answer that was the closest match.
     *
     * <p>
     *     This may be null if the verdict was cast without checking user input, e.g. Anki mode.
     * </p>
     *
     * <p>
     *     If any cleanup or translation was done, this is the original answer that
     *     came straight from the Subject before any modifications.
     * </p>
     * @return the value
     */
    public @Nullable String getMatchedAnswer() {
        return matchedAnswer;
    }

    /**
     * Details of a near-match that differs from an OK because of a difference in regular/small kana.
     *
     * @return the match data, or null if not applicable
     */
    public @Nullable DigraphMatch getDigraphMatch() {
        return digraphMatch;
    }

    @Override
    public @Nonnull String toString() {
        return String.format(Locale.ROOT, "Verdict:%s,%s,%s,%s,%s,%s", ok, retry, nearMatch, givenAnswer, matchedAnswer, digraphMatch);
    }
}
