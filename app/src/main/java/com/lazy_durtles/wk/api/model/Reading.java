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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lazy_durtles.wk.model.DigraphMatch;
import com.lazy_durtles.wk.util.PseudoIme;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.isEmpty;
import static com.lazy_durtles.wk.util.ObjectSupport.isEqual;

/**
 * Model class for a reading registered for a subject.
 */
@SuppressWarnings("unused")
public final class Reading {
    private static final String smallKana = "ぁぃぅぇぉっゃゅょゎゕゖァィゥェォッャュョヮヵヶ";
    private static final String regularKana = "あいうえおつやゆよわかけアイウエオツヤユヨワカケ";

    private @Nullable String reading = null;
    private boolean primary = false;
    @JsonProperty("accepted_answer") private boolean acceptedAnswer = false;
    private @Nullable String type = null;

    /**
     * Is this an on'yomi reading?.
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isOnYomi() {
        return isEqual(type, "onyomi");
    }

    /**
     * Is this an kun'yomi reading?.
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isKunYomi() {
        return isEqual(type, "kunyomi");
    }

    /**
     * Is this an nanori reading?.
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isNanori() {
        return isEqual(type, "nanori");
    }

    /**
     * Get the reading text, possibly converted to katakana.
     *
     * @param showOnInKatakana true if on'yomi should be rendered in katakana.
     * @return the text
     */
    @JsonIgnore
    public @Nullable String getValue(final boolean showOnInKatakana) {
        if (showOnInKatakana && isOnYomi()) {
            return PseudoIme.toKatakana(reading);
        }
        return reading;
    }

    /**
     * Check if an answer matches this reading, taking into account the setting to require on'yomi
     * in katakana.
     *
     * @param answer the answer to check
     * @param requireOnInKatakana true if the answer must be in katakana if the reading is on'yomi
     * @return true if it is a match
     */
    @JsonIgnore
    public boolean matches(final String answer, final boolean requireOnInKatakana) {
        if (isOnYomi()) {
            if (requireOnInKatakana) {
                return isEqual(PseudoIme.toKatakana(reading), answer);
            }
            return isEqual(reading, answer) || isEqual(PseudoIme.toKatakana(reading), answer);
        }
        else {
            return isEqual(reading, answer);
        }
    }

    @JsonIgnore
    private static @Nullable DigraphMatch matchesForDigraph(final CharSequence answer, final @Nullable CharSequence baseLine) {
        if (baseLine == null || answer.length() != baseLine.length()) {
            return null;
        }
        char regular = 0;
        char small = 0;
        for (int i=0; i<answer.length(); i++) {
            final char c1 = answer.charAt(i);
            final char c2 = baseLine.charAt(i);
            if (c1 == c2) {
                continue;
            }
            final int p1 = regularKana.indexOf(c1);
            if (p1 >= 0 && smallKana.charAt(p1) == c2) {
                regular = c1;
                small = c2;
                continue;
            }
            final int p2 = smallKana.indexOf(c1);
            if (p2 >= 0 && regularKana.charAt(p2) == c2) {
                regular = c2;
                small = c1;
                continue;
            }
            return null;
        }
        if (regular != 0) {
            return new DigraphMatch(regular, small);
        }
        return null;
    }

    /**
     * Check if an answer matches this reading except for a digraph mismatch.
     *
     * @param answer the answer to check
     * @return the digraph match details if the answer is correct except for the digraph mismatch
     */
    @JsonIgnore
    public @Nullable DigraphMatch matchesForDigraph(final CharSequence answer) {
        if (isOnYomi()) {
            final @Nullable DigraphMatch regularMatch = matchesForDigraph(answer, reading);
            if (regularMatch != null) {
                return regularMatch;
            }
            return matchesForDigraph(answer, PseudoIme.toKatakana(reading));
        }
        else {
            return matchesForDigraph(answer, reading);
        }
    }

    /**
     * Is the meaning empty or it it's value "None".
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isEmptyOrNone() {
        return isEmpty(reading) || reading.equals("None");
    }

    /**
     * True if this reading is an acceptable answer for a reading question.
     * @return the value
     */
    public boolean isAcceptedAnswer() {
        return acceptedAnswer;
    }

    /**
     * True if this reading is an acceptable answer for a reading question.
     * @param acceptedAnswer the value
     */
    public void setAcceptedAnswer(final boolean acceptedAnswer) {
        this.acceptedAnswer = acceptedAnswer;
    }

    /**
     * True if this is the primary reading for the subject.
     * @return the value
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * True if this is the primary reading for the subject.
     * @param primary the value
     */
    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }

    /**
     * The type of this reading; one of "onyomi", "kunyomi" or "nanori".
     * @return the value
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * The type of this reading; one of "onyomi", "kunyomi" or "nanori".
     * @param type the value
     */
    public void setType(final @Nullable String type) {
        this.type = type == null ? null : type.intern();
    }

    /**
     * The text for the reading.
     * @return the value
     */
    public @Nullable String getReading() {
        return reading;
    }

    /**
     * The text for the reading.
     * @param reading the value
     */
    public void setReading(final @Nullable String reading) {
        this.reading = reading == null ? null : reading.intern();
    }
}
