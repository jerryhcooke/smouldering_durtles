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

package com.lazy_durtles.wk.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.lazy_durtles.wk.model.PitchInfo;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Custom serializer for PitchInfo instances.
 */
public final class PitchInfoDeserializer extends StdDeserializer<PitchInfo> {
    /**
     * For serialization.
     */
    private static final long serialVersionUID = 5515932274345600748L;

    /**
     * The constructor.
     */
    public PitchInfoDeserializer() {
        super(PitchInfo.class);
    }

    @Override
    public @Nullable PitchInfo deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        if (!p.hasToken(JsonToken.START_ARRAY)) {
            return null;
        }
        p.nextToken();
        if (!p.hasToken(JsonToken.VALUE_STRING) && !p.hasToken(JsonToken.VALUE_NULL)) {
            return null;
        }
        final @Nullable String reading = p.getValueAsString();
        p.nextToken();
        if (!p.hasToken(JsonToken.VALUE_STRING) && !p.hasToken(JsonToken.VALUE_NULL)) {
            return null;
        }
        final @Nullable String partOfSpeech = p.getValueAsString();
        p.nextToken();
        if (!p.hasToken(JsonToken.VALUE_NUMBER_INT) && !p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
            return null;
        }
        final int pitchNumber = p.getValueAsInt(-1);
        p.nextToken();
        if (!p.hasToken(JsonToken.END_ARRAY)) {
            return null;
        }
        return new PitchInfo(reading, partOfSpeech, pitchNumber);
    }
}
