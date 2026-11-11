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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.lazy_durtles.wk.model.PitchInfo;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Custom serializer for PitchInfo instances.
 */
public final class PitchInfoSerializer extends StdSerializer<PitchInfo> {
    /**
     * For serialization.
     */
    private static final long serialVersionUID = -1569741771743919908L;

    /**
     * The constructor.
     */
    public PitchInfoSerializer() {
        super(PitchInfo.class);
    }

    @Override
    public void serialize(final @Nullable PitchInfo value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartArray();
        if (value.getReading() == null) {
            gen.writeNull();
        }
        else {
            gen.writeString(value.getReading());
        }
        if (value.getPartOfSpeech() == null) {
            gen.writeNull();
        }
        else {
            gen.writeString(value.getPartOfSpeech());
        }
        gen.writeNumber(value.getPitchNumber());
        gen.writeEndArray();
    }
}
