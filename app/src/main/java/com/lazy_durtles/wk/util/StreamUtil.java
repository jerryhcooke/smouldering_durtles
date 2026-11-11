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

import com.fasterxml.jackson.core.util.ByteArrayBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for dealing with streams.
 */
public final class StreamUtil {
    private StreamUtil() {
        //
    }

    /**
     * Read all data from a stream and return it as a byte array.
     *
     * @param is the stream to read from
     * @return the data from the stream as a byte array
     * @throws IOException if the stream could not be read
     */
    public static byte[] slurp(final InputStream is) throws IOException {
        final byte[] buffer = new byte[256];
        try (final ByteArrayBuilder builder = new ByteArrayBuilder()) {
            while (true) {
                final int n = is.read(buffer);
                if (n < 0) {
                    return builder.toByteArray();
                }
                builder.write(buffer, 0, n);
            }
        }
    }

    /**
     * Read all data from one stream and write it to another.
     *
     * @param is the stream to read from
     * @param os the stream to write to
     * @throws IOException if either reading or writing fails
     */
    public static void pump(final InputStream is, final OutputStream os) throws IOException {
        final byte[] buffer = new byte[256];
        while (true) {
            final int n = is.read(buffer);
            if (n < 0) {
                return;
            }
            os.write(buffer, 0, n);
        }
    }
}
