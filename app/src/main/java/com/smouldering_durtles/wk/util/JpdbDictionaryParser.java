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

package com.smouldering_durtles.wk.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smouldering_durtles.wk.db.model.JpdbFrequency;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

/**
 * Streaming parser for the JPDB frequency dictionary in Yomitan format (a zip with one or more
 * term_meta_bank_N.json files). Has no Android dependencies so it can be unit tested on the JVM.
 */
public final class JpdbDictionaryParser {
    /**
     * Callback for a batch of parsed entries.
     */
    public interface BatchConsumer {
        /**
         * Consume a batch. The list is reused after this returns.
         *
         * @param batch the entries
         */
        void accept(List<JpdbFrequency> batch);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JpdbDictionaryParser() {
        //
    }

    /**
     * Parse a dictionary zip, streaming. Neither the JSON nor the full dictionary is held in memory.
     *
     * @param inputStream the zip data
     * @param batchSize the batch size
     * @param consumer receives batches of entries
     * @return the total number of entries parsed
     * @throws IOException on read/parse errors
     */
    public static int parseZip(final InputStream inputStream, final int batchSize, final BatchConsumer consumer) throws IOException {
        final JsonFactory factory = MAPPER.getFactory();
        int total = 0;
        try (final ZipInputStream zis = new ZipInputStream(inputStream)) {
            @Nullable ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final String name = entry.getName();
                // Yomitan dictionaries can be split over several banks, so read all of them
                if (entry.isDirectory() || !name.contains("term_meta_bank") || !name.endsWith(".json")) {
                    continue;
                }
                final JsonParser parser = factory.createParser(zis);
                parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
                try {
                    total += parseBank(parser, batchSize, consumer);
                }
                finally {
                    parser.close();
                }
            }
        }
        return total;
    }

    private static int parseBank(final JsonParser parser, final int batchSize, final BatchConsumer consumer) throws IOException {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            return 0;
        }
        int total = 0;
        final List<JpdbFrequency> batch = new ArrayList<>(batchSize);
        while (parser.nextToken() == JsonToken.START_ARRAY) {
            final JsonNode node = parser.readValueAsTree();
            final @Nullable JpdbFrequency record = parseEntry(node);
            if (record != null) {
                batch.add(record);
                if (batch.size() >= batchSize) {
                    consumer.accept(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            consumer.accept(batch);
            total += batch.size();
        }
        return total;
    }

    /**
     * Parse one term_meta_bank entry: [term, "freq", data] where data is one of
     * N, {value: N}, {reading: R, frequency: N}, {reading: R, frequency: {value: N}}.
     *
     * @param node the entry
     * @return the record, or null if not a usable frequency entry
     */
    public static @Nullable JpdbFrequency parseEntry(final JsonNode node) {
        if (!(node instanceof ArrayNode) || node.size() < 3) {
            return null;
        }
        final String term = node.get(0).asText("");
        if (term.isEmpty() || !"freq".equals(node.get(1).asText(""))) {
            return null;
        }
        final JsonNode data = node.get(2);
        String reading = "";
        int rank = 0;
        if (data.isNumber()) {
            rank = data.asInt();
        }
        else if (data instanceof ObjectNode) {
            if (data.has("reading")) {
                reading = data.get("reading").asText("");
            }
            final JsonNode freq = data.has("frequency") ? data.get("frequency") : data;
            if (freq.isNumber()) {
                rank = freq.asInt();
            }
            else if (freq.has("value")) {
                rank = freq.get("value").asInt();
            }
        }
        if (rank <= 0) {
            return null;
        }
        final JpdbFrequency record = new JpdbFrequency();
        record.term = term;
        record.reading = reading;
        record.freqRank = rank;
        return record;
    }

    /**
     * Pick the rank for a subject from all dictionary entries for its term. Prefers the entry for one
     * of the subject's readings (in the given order), then a reading-less entry, then the most common one.
     *
     * @param entries the entries for the term, sorted by rank ascending
     * @param readings the subject's readings, primary first
     * @return the rank, or -1 if there are no entries
     */
    public static int pickRank(final List<JpdbFrequency> entries, final Iterable<String> readings) {
        if (entries.isEmpty()) {
            return -1;
        }
        for (final String reading: readings) {
            for (final JpdbFrequency entry: entries) {
                if (entry.reading.equals(reading)) {
                    return entry.freqRank;
                }
            }
        }
        for (final JpdbFrequency entry: entries) {
            if (entry.reading.isEmpty()) {
                return entry.freqRank;
            }
        }
        return entries.get(0).freqRank;
    }
}
