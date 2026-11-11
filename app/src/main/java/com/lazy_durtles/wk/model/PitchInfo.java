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

import android.annotation.SuppressLint;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lazy_durtles.wk.util.PitchInfoDeserializer;
import com.lazy_durtles.wk.util.PitchInfoSerializer;
import com.lazy_durtles.wk.util.PseudoIme;

import java.util.Comparator;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.hash;
import static com.lazy_durtles.wk.util.ObjectSupport.isEqual;
import static java.util.Objects.requireNonNull;

/**
 * Model class to represent one item of pitch information.
 */
@JsonSerialize(using = PitchInfoSerializer.class)
@JsonDeserialize(using = PitchInfoDeserializer.class)
public final class PitchInfo implements Comparable<PitchInfo> {
    @SuppressLint("NewApi")
    private static final Comparator<PitchInfo> COMPARATOR = Comparator.nullsFirst(
            Comparator.comparing(PitchInfo::getReading, Comparator.nullsFirst(Comparator.naturalOrder())))
            .thenComparing(PitchInfo::getPartOfSpeech, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparingInt(PitchInfo::getPitchNumber);

    private final @Nullable String reading;
    private final @Nullable String partOfSpeech;
    private final int pitchNumber;

    /**
     * The constructor.
     *
     * @param reading The reading this item applies to.
     * @param partOfSpeech The part of speech this entry applies to, null if not differentiated.
     * @param pitchNumber The number for the applicable pitch pattern.
     */
    public PitchInfo(final @Nullable CharSequence reading, final @Nullable String partOfSpeech, final int pitchNumber) {
        this.reading = reading == null ? null : requireNonNull(PseudoIme.toKatakana(reading)).intern();
        this.partOfSpeech = partOfSpeech == null ? null : partOfSpeech.intern();
        this.pitchNumber = pitchNumber;
    }

    /**
     * The reading this item applies to.
     * @return the value
     */
    public @Nullable String getReading() {
        return reading;
    }

    /**
     * The part of speech this entry applies to, null if not differentiated.
     * @return the value
     */
    public @Nullable String getPartOfSpeech() {
        return partOfSpeech;
    }

    /**
     * The number for the applicable pitch pattern.
     * @return the value
     */
    public int getPitchNumber() {
        return pitchNumber;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PitchInfo other = (PitchInfo) obj;
        return pitchNumber == other.pitchNumber
                && isEqual(reading, other.reading)
                && isEqual(partOfSpeech, other.partOfSpeech);
    }

    @Override
    public int hashCode() {
        return hash(reading, partOfSpeech, pitchNumber);
    }

    @Override
    public int compareTo(final @Nullable PitchInfo o) {
        return COMPARATOR.compare(this, o);
    }
}
