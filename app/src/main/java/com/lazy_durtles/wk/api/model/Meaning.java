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

package com.lazy_durtles.wk.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Model class for a meaning registered for a subject.
 */
@SuppressWarnings("unused")
public final class Meaning {
    private @Nullable String meaning = null;
    private boolean primary = false;
    @JsonProperty("accepted_answer") private boolean acceptedAnswer = false;

    /**
     * The text for the meaning.
     * @return the value
     */
    public @Nullable String getMeaning() {
        return meaning;
    }

    /**
     * The text for the meaning.
     * @param meaning the value
     */
    public void setMeaning(final @Nullable String meaning) {
        this.meaning = meaning == null ? null : meaning.intern();
    }

    /**
     * True if this is the primary meaning for the subject.
     * @return the value
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * True if this is the primary meaning for the subject.
     * @param primary the value
     */
    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }

    /**
     * True if this meaning is an acceptable answer for a meaning question.
     * @return the value
     */
    public boolean isAcceptedAnswer() {
        return acceptedAnswer;
    }

    /**
     * True if this meaning is an acceptable answer for a meaning question.
     * @param acceptedAnswer the value
     */
    public void setAcceptedAnswer(final boolean acceptedAnswer) {
        this.acceptedAnswer = acceptedAnswer;
    }
}
