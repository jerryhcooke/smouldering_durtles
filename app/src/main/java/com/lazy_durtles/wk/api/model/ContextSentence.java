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
 * Model class for a context sentence belonging to a vocab subject.
 */
@SuppressWarnings("unused")
public final class ContextSentence {
    @JsonProperty("en") private @Nullable String english = null;
    @JsonProperty("ja") private @Nullable String japanese = null;

    /**
     * English translation of the sentence.
     * @return the value
     */
    public @Nullable String getEnglish() {
        return english;
    }

    /**
     * English translation of the sentence.
     * @param english the value
     */
    public void setEnglish(final @Nullable String english) {
        this.english = english;
    }

    /**
     * The original Japanese sentence.
     * @return the value
     */
    public @Nullable String getJapanese() {
        return japanese;
    }

    /**
     * The original Japanese sentence.
     * @param japanese the value
     */
    public void setJapanese(final @Nullable String japanese) {
        this.japanese = japanese;
    }
}
