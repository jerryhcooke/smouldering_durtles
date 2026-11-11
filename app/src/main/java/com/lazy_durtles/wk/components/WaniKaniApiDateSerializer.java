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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.TextUtil.formatTimestampForApi;

/**
 * Custom serializer for dates, hardcoded for the format used in the WaniKani API.
 * This the serializing counterpart to WaniKaniApiDateDeserializerNew.
 */
public final class WaniKaniApiDateSerializer extends JsonSerializer<Long> {
    /**
     * The constructor.
     */
    @SuppressWarnings({"unused", "RedundantNoArgConstructor"})
    public WaniKaniApiDateSerializer() {
        //
    }

    @Override
    public void serialize(final @Nullable Long value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (value == null || value == 0L) {
            gen.writeNull();
            return;
        }

        gen.writeString(formatTimestampForApi(value));
    }

    @Override
    public Class<Long> handledType() {
        return Long.class;
    }

    @Override
    public boolean isEmpty(final SerializerProvider provider, final @Nullable Long value) {
        return value == null || value == 0L;
    }
}
