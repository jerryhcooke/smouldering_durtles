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

package com.lazy_durtles.wk.components;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.util.AccessPattern;

import java.io.IOException;

import static com.lazy_durtles.wk.util.TextUtil.parseTimestampFromApi;

/**
 * Custom deserializer for dates, hardcoded for the format used in the WaniKani API.
 * This is used instead of a simple dateformat since date parsing is done incorrectly
 * on some old Android versions. Specifically, Android 4.1 in the emulator.
 *
 * <p>
 *     Since I couldn't figure out why the parsing was incorrect, I decided to just
 *     sidestep the problem by making a simple custom parser.
 * </p>
 */
public final class WaniKaniApiDateDeserializer extends JsonDeserializer<Long> {
    /**
     * The constructor.
     */
    @SuppressWarnings({"unused", "RedundantNoArgConstructor"})
    public WaniKaniApiDateDeserializer() {
        //
    }

    @Override
    public Long deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return parseTimestampFromApi(p.getText().trim());
        }
        return 0L;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Class<?> handledType() {
        return Long.class;
    }

    @Override
    public Long getNullValue(final DeserializationContext ctxt) {
        return 0L;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.CONSTANT;
    }

    @Override
    public AccessPattern getEmptyAccessPattern() {
        return AccessPattern.CONSTANT;
    }

    @Override
    public Object getEmptyValue(final DeserializationContext ctxt) {
        return 0L;
    }
}
