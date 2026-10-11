package com.smouldering_durtles.wk.util;

import com.smouldering_durtles.wk.db.model.JpdbFrequency;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public final class JpdbDictionaryParserTest {
    private static byte[] zip(final String... nameContentPairs) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (int i = 0; i < nameContentPairs.length; i += 2) {
                zos.putNextEntry(new ZipEntry(nameContentPairs[i]));
                zos.write(nameContentPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    private static List<JpdbFrequency> parse(final byte[] data, final int batchSize) throws IOException {
        final List<JpdbFrequency> all = new ArrayList<>();
        final int total = JpdbDictionaryParser.parseZip(new ByteArrayInputStream(data), batchSize, all::addAll);
        assertEquals(total, all.size());
        return all;
    }

    private static JpdbFrequency entry(final String term, final String reading, final int rank) {
        final JpdbFrequency e = new JpdbFrequency();
        e.term = term;
        e.reading = reading;
        e.freqRank = rank;
        return e;
    }

    @Test
    public void parsesAllEntryShapesAndAllBanks() throws IOException {
        final byte[] data = zip(
                "index.json", "{\"title\":\"JPDBv2\"}",
                "term_meta_bank_1.json", "[[\"の\",\"freq\",{\"value\":1,\"displayValue\":\"1\"}],"
                        + "[\"思う\",\"freq\",{\"reading\":\"おもう\",\"frequency\":{\"value\":20,\"displayValue\":\"20\"}}],"
                        + "[\"上手\",\"freq\",{\"reading\":\"じょうず\",\"frequency\":2499}],"
                        + "[\"bad\",\"pitch\",{\"value\":5}],"
                        + "[\"zero\",\"freq\",0]]",
                "term_meta_bank_2.json", "[[\"上手\",\"freq\",{\"reading\":\"うわて\",\"frequency\":{\"value\":17102}}],"
                        + "[\"plain\",\"freq\",42]]");
        final List<JpdbFrequency> all = parse(data, 2);
        assertEquals(5, all.size());
        assertEquals("の", all.get(0).term);
        assertEquals("", all.get(0).reading);
        assertEquals(1, all.get(0).freqRank);
        assertEquals("おもう", all.get(1).reading);
        assertEquals(20, all.get(1).freqRank);
        assertEquals(2499, all.get(2).freqRank);
        assertEquals("うわて", all.get(3).reading);
        assertEquals(17102, all.get(3).freqRank);
        assertEquals(42, all.get(4).freqRank);
    }

    @Test
    public void picksRankForTaughtReading() {
        final List<JpdbFrequency> jouzu = Arrays.asList(entry("上手", "じょうず", 2499), entry("上手", "うわて", 17102));
        assertEquals(17102, JpdbDictionaryParser.pickRank(jouzu, Collections.singletonList("うわて")));
        assertEquals(2499, JpdbDictionaryParser.pickRank(jouzu, Arrays.asList("じょうず", "うわて")));
        // Unknown reading: fall back to the most common entry
        assertEquals(2499, JpdbDictionaryParser.pickRank(jouzu, Collections.singletonList("かみて")));
        // Kana entries have no reading
        assertEquals(1, JpdbDictionaryParser.pickRank(Collections.singletonList(entry("の", "", 1)),
                Collections.singletonList("の")));
        assertEquals(-1, JpdbDictionaryParser.pickRank(Collections.emptyList(), Collections.singletonList("x")));
    }

    /**
     * Run against the real dictionary: JPDB_ZIP=/path/to/JPDB_v2.2_Frequency_2024-10-13.zip ./gradlew testDebugUnitTest
     */
    @Test
    public void parsesRealDictionaryIfAvailable() throws IOException {
        final String path = System.getenv("JPDB_ZIP");
        org.junit.Assume.assumeTrue(path != null && new java.io.File(path).exists());
        final List<JpdbFrequency> jouzu = new ArrayList<>();
        final int[] total = {0};
        try (final java.io.InputStream is = new java.io.FileInputStream(path)) {
            total[0] = JpdbDictionaryParser.parseZip(is, 2000, batch -> {
                for (final JpdbFrequency e: batch) {
                    if (e.term.equals("上手")) {
                        jouzu.add(entry(e.term, e.reading, e.freqRank));
                    }
                }
            });
        }
        assertEquals(278946, total[0]);
        assertEquals(2499, JpdbDictionaryParser.pickRank(jouzu, Collections.singletonList("じょうず")));
        assertEquals(17102, JpdbDictionaryParser.pickRank(jouzu, Collections.singletonList("うわて")));
    }
}
