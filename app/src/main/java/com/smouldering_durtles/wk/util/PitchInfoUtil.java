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

import com.smouldering_durtles.wk.Constants;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.model.PitchInfo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;

import static com.smouldering_durtles.wk.Constants.MINUTE;
import static com.smouldering_durtles.wk.Constants.SECOND;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Utility class for managing data for pitch info support.
 */
public final class PitchInfoUtil {
    private static final Logger LOGGER = Logger.get(PitchInfoUtil.class);

    private static final Pattern HEAD_PATTERN_PITCH_NUMBER = Pattern.compile("［(\\d+)］");
    private static final Pattern HEAD_PATTERN_END_OF_READING = Pattern.compile("[【】［］〔〕]");
    private static final Pattern BODY_PATTERN_PITCH_NUMBER = Pattern.compile("［(\\d+)］");
    private static final Pattern BODY_PATTERN_PART_OF_SPEECH = Pattern.compile("（([^）]+)）");
    private static final Pattern TILDE_PATTERN = Pattern.compile("〜");

    private PitchInfoUtil() {
        //
    }

    /**
     * Get the location for the weblio file for a vocab's characters, may or may not exist.
     *
     * @param characters the characters to identify
     * @return the file
     * @throws IOException if an error occurs
     */
    private static File getWeblioFile(final CharSequence characters) throws IOException {
        final File location = requireNonNull(WkApplication.getInstance().getExternalFilesDir(null));
        final File dir = new File(location, "weblio");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        final String urlEncoded = URLEncoder.encode(TILDE_PATTERN.matcher(characters).replaceAll(""), "UTF-8");
        final String fileName = String.format(Locale.ROOT, "%s.html", urlEncoded);
        return new File(dir, fileName);
    }

    /**
     * Get a temporary file for downloading.
     *
     * @return the file
     */
    private static File getTempFile() {
        final File location = requireNonNull(WkApplication.getInstance().getExternalFilesDir(null));
        final File dir = new File(location, "weblio");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return new File(dir, "download.tmp");
    }

    /**
     * Get the file for the generated pitch info map.
     *
     * @return the file
     */
    private static File getMapFile() {
        final File location = requireNonNull(WkApplication.getInstance().getExternalFilesDir(null));
        return new File(location, "weblio_pitch_info.json");
    }

    /**
     * Get the total number of present weblio download files.
     *
     * @return the number
     */
    public static int getNumWeblioFiles() {
        final File location = requireNonNull(WkApplication.getInstance().getExternalFilesDir(null));
        final File dir = new File(location, "weblio");
        if (!dir.exists()) {
            return 0;
        }
        @androidx.annotation.Nullable
        final @Nullable String[] files = dir.list();
        if (files == null) {
            return 0;
        }
        return files.length;
    }

    /**
     * Check if a weblio download file for a vocab exists.
     *
     * @param characters the characters to look for
     * @return true if the file exists
     * @throws IOException if an error occurs
     */
    public static boolean existsWeblioFile(final CharSequence characters) throws IOException {
        return getWeblioFile(characters).exists();
    }

    /**
     * Try do download a weblio dictionary entry. Fail silently if unable.
     *
     * @param characters the characters of the vocab
     */
    public static void downloadWeblioFile(final String characters) {
        try {
            final String urlString = "https://www.weblio.jp/content?query="
                    + URLEncoder.encode(TILDE_PATTERN.matcher(characters).replaceAll(""), "UTF-8");
            final File file = getWeblioFile(characters);
            final File tempFile = getTempFile();

            final URL url = new URL(urlString);
            LOGGER.info("Weblio fetch for %s: %s", characters, url);
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setAllowUserInteraction(false);
            connection.setConnectTimeout((int) (10 * SECOND));
            connection.setReadTimeout((int) MINUTE);
            connection.setInstanceFollowRedirects(true);
            connection.getHeaderFields();
            try (final InputStream is = connection.getInputStream(); final OutputStream os = new FileOutputStream(tempFile)) {
                StreamUtil.pump(is, os);
            }
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            tempFile.renameTo(file);
            LOGGER.info("Weblio fetch done");
        } catch (final Exception e) {
            LOGGER.error(e, "Exception downloading weblio file");
        }
    }

    /**
     * Try do download a weblio dictionary entry. Fail silently if unable.
     *
     * @param characters the characters of the vocab
     * @return the body of the page or null in case of errors
     */
    public static @Nullable String downloadWeblioPage(final String characters) {
        try {
            final String urlString = "https://www.weblio.jp/content?query="
                    + URLEncoder.encode(TILDE_PATTERN.matcher(characters).replaceAll(""), "UTF-8");

            final URL url = new URL(urlString);
            LOGGER.info("Weblio fetch for %s: %s", characters, url);
            final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setAllowUserInteraction(false);
            connection.setConnectTimeout((int) (10 * SECOND));
            connection.setReadTimeout((int) MINUTE);
            connection.setInstanceFollowRedirects(true);
            connection.getHeaderFields();
            try (final InputStream is = connection.getInputStream()) {
                final byte[] body = StreamUtil.slurp(is);
                LOGGER.info("Weblio fetch done");
                return new String(body, StandardCharsets.UTF_8);
            }
        } catch (final Exception e) {
            LOGGER.error(e, "Exception downloading weblio page");
            return null;
        }
    }

    /**
     * Parse a weblio download file to find the pitch info.
     *
     * @param characters the vocab's characters
     * @return the set of pitch info items
     * @throws IOException if an error occurs
     */
    public static Set<PitchInfo> parseWeblioFile(final String characters) throws IOException {
        final File file = getWeblioFile(characters);
        final Document doc = Jsoup.parse(file, "UTF-8");
        final Set<PitchInfo> result = new HashSet<>();

        // Get the list of entry headings
        final Elements heads = doc.getElementsByClass("NetDicHead");
        if (heads.isEmpty()) {
            LOGGER.info("No NetDicHead entries found for: %s", characters);
            return result;
        }

        Matcher matcher;
        // Go through each heading in turn
        for (final Element head: heads) {
            // Remove small-print text
            for (final Element span: head.getElementsByTag("span")) {
                if (span.parent() != null && "font-size:75%;".equals(span.attr("style"))) {
                    final String spanText = span.text().trim();
                    if (!HEAD_PATTERN_PITCH_NUMBER.matcher(spanText).matches()) {
                        span.remove();
                    }
                }
            }

            // The text of the heading without any markup
            final String headText = head.text();

            // Extract any pitch accent numbers
            final Collection<Integer> pitchNumbers = new ArrayList<>();
            matcher = HEAD_PATTERN_PITCH_NUMBER.matcher(headText);
            while (matcher.find()) {
                pitchNumbers.add(Integer.parseInt(requireNonNull(matcher.group(1)), 10));
            }

            // Trim off excess to leave only the reading
            matcher = HEAD_PATTERN_END_OF_READING.matcher(headText);
            String reading = matcher.find() ? headText.substring(0, matcher.start()).trim() : headText.trim();

            // Strip stray non-kana characters from the reading
            int i = 0;
            while (i < reading.length()) {
                final char c = reading.charAt(i);
                if (c >= 0x3041 && c <= 0x3096 || c >= 0x30A1 && c <= 0x30FA || c == 0x30FC) {
                    i++;
                    continue;
                }
                reading = reading.substring(0, i) + reading.substring(i+1);
            }

            if (isEmpty(reading)) {
                continue;
            }

            for (final int pitchNumber: pitchNumbers) {
                result.add(new PitchInfo(reading, null, pitchNumber));
            }

            // Look through the body after the heading, scanning for part-of-speech variants
            final @Nullable Element body = head.nextElementSibling();
            if (pitchNumbers.isEmpty() && body != null && "NetDicBody".equals(body.className())) {
                // Look for spans that indicate the variants: an inverted span
                final Elements spans = body.getElementsByAttribute("data-txt-len");
                for (final Element span: spans) {
                    if (!span.attr("style").contains("background-color:black")) {
                        continue;
                    }
                    final @Nullable Element parent = span.parent();
                    if (parent == null) {
                        continue;
                    }
                    final @Nullable Element span2 = parent.nextElementSibling();
                    if (span2 == null) {
                        continue;
                    }
                    // Found it
                    // Remove stray stuff to keep just the variant info
                    for (final Element span3: span2.getElementsByTag("span")) {
                        if (span3.parent() != null && "font-size:75%;".equals(span3.attr("style"))) {
                            span3.remove();
                        }
                    }
                    for (final Element div: span2.getElementsByTag("div")) {
                        if (div.parent() != null) {
                            div.remove();
                        }
                    }
                    final String variantString = span2.text().trim();
                    if (!variantString.isEmpty()) {
                        // Extract any pitch accent numbers
                        final Collection<Integer> pitchNumbersVariant = new ArrayList<>();
                        matcher = BODY_PATTERN_PITCH_NUMBER.matcher(variantString);
                        while (matcher.find()) {
                            pitchNumbersVariant.add(Integer.parseInt(requireNonNull(matcher.group(1)), 10));
                        }

                        @Nullable String partOfSpeech = null;
                        matcher = BODY_PATTERN_PART_OF_SPEECH.matcher(variantString);
                        if (matcher.find()) {
                            partOfSpeech = requireNonNull(matcher.group(1)).trim();
                        }

                        for (final int pitchNumber: pitchNumbersVariant) {
                            result.add(new PitchInfo(reading, partOfSpeech, pitchNumber));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Parse a weblio download page to find the pitch info.
     *
     * @param page the body of the page to parse
     * @return the set of pitch info items
     */
    public static @Nullable Set<PitchInfo> parseWeblioPage(final String page) {
        try {
            final Document doc = Jsoup.parse(page);
            final Set<PitchInfo> result = new HashSet<>();

            // Get the list of entry headings
            final Elements heads = doc.getElementsByClass("NetDicHead");
            if (heads.isEmpty()) {
                return result;
            }

            Matcher matcher;
            // Go through each heading in turn
            for (final Element head: heads) {
                // Remove small-print text
                for (final Element span: head.getElementsByTag("span")) {
                    if (span.parent() != null && "font-size:75%;".equals(span.attr("style"))) {
                        span.remove();
                    }
                }

                // The text of the heading without any markup
                final String headText = head.text();

                // Extract any pitch accent numbers
                final Collection<Integer> pitchNumbers = new ArrayList<>();
                matcher = HEAD_PATTERN_PITCH_NUMBER.matcher(headText);
                while (matcher.find()) {
                    pitchNumbers.add(Integer.parseInt(requireNonNull(matcher.group(1)), 10));
                }

                // Trim off excess to leave only the reading
                matcher = HEAD_PATTERN_END_OF_READING.matcher(headText);
                String reading = matcher.find() ? headText.substring(0, matcher.start()).trim() : headText.trim();

                // Strip stray non-kana characters from the reading
                int i = 0;
                while (i < reading.length()) {
                    final char c = reading.charAt(i);
                    if (c >= 0x3041 && c <= 0x3096 || c >= 0x30A1 && c <= 0x30FA || c == 0x30FC) {
                        i++;
                        continue;
                    }
                    reading = reading.substring(0, i) + reading.substring(i+1);
                }

                if (isEmpty(reading)) {
                    continue;
                }

                for (final int pitchNumber: pitchNumbers) {
                    result.add(new PitchInfo(reading, null, pitchNumber));
                }

                // Look through the body after the heading, scanning for part-of-speech variants
                final @Nullable Element body = head.nextElementSibling();
                if (pitchNumbers.isEmpty() && body != null && "NetDicBody".equals(body.className())) {
                    // Look for spans that indicate the variants: an inverted span
                    final Elements spans = body.getElementsByAttribute("data-txt-len");
                    for (final Element span: spans) {
                        if (!span.attr("style").contains("background-color:black")) {
                            continue;
                        }
                        final @Nullable Element parent = span.parent();
                        if (parent == null) {
                            continue;
                        }
                        final @Nullable Element span2 = parent.nextElementSibling();
                        if (span2 == null) {
                            continue;
                        }
                        // Found it
                        // Remove stray stuff to keep just the variant info
                        for (final Element span3: span2.getElementsByTag("span")) {
                            if (span3.parent() != null && "font-size:75%;".equals(span3.attr("style"))) {
                                span3.remove();
                            }
                        }
                        for (final Element div: span2.getElementsByTag("div")) {
                            if (div.parent() != null) {
                                div.remove();
                            }
                        }
                        final String variantString = span2.text().trim();
                        if (!variantString.isEmpty()) {
                            // Extract any pitch accent numbers
                            final Collection<Integer> pitchNumbersVariant = new ArrayList<>();
                            matcher = BODY_PATTERN_PITCH_NUMBER.matcher(variantString);
                            while (matcher.find()) {
                                pitchNumbersVariant.add(Integer.parseInt(requireNonNull(matcher.group(1)), 10));
                            }

                            @Nullable String partOfSpeech = null;
                            matcher = BODY_PATTERN_PART_OF_SPEECH.matcher(variantString);
                            if (matcher.find()) {
                                partOfSpeech = requireNonNull(matcher.group(1)).trim();
                            }

                            for (final int pitchNumber: pitchNumbersVariant) {
                                result.add(new PitchInfo(reading, partOfSpeech, pitchNumber));
                            }
                        }
                    }
                }
            }
            return result;
        }
        catch (final Exception e) {
            LOGGER.error(e, "Exception parsing weblio page");
            return null;
        }
    }

    /**
     * Save the pitch information gathered from weblio responses.
     *
     * @param map the map to save
     * @throws IOException if an error occurs
     */
    public static void saveMap(final Map<String, List<PitchInfo>> map) throws IOException {
        final File file = getMapFile();
        try (final OutputStream os = new FileOutputStream(file)) {
            Converters.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(os, map);
        }
    }

    /**
     * Schedule tasks to download pitch info for a list of subjects.
     *
     * @param list the list of subjects to download for
     */
    private static void scheduleDownloadTasksFor(final Iterable<Subject> list) {
        final AppDatabase db = WkApplication.getDatabase();
        for (final Subject subject: list) {
            db.assertDownloadPitchInfoTask(subject.getId());
        }
    }

    /**
     * Schedule tasks to download pitch info for a list of subjects.
     *
     * @param maxSize the max number of tasks to schedule
     */
    public static void scheduleDownloadTasks(final int maxSize) {
        final AppDatabase db = WkApplication.getDatabase();
        final Collection<Subject> candidates = db.subjectCollectionsDao().getPitchInfoDownloadCandidates();
        final Collection<Subject> list = new ArrayList<>();
        for (final Subject subject: candidates) {
            if (subject.needsPitchInfoDownload(Constants.WEEK)) {
                list.add(subject);
            }
            if (list.size() >= maxSize) {
                break;
            }
        }
        scheduleDownloadTasksFor(list);
    }

    /**
     * Schedule tasks to download pitch info for a list of subjects.
     *
     * @param candidates the candidates to select from
     * @param maxSize the max number of tasks to schedule
     */
    public static void scheduleDownloadTasks(final Iterable<Subject> candidates, final int maxSize) {
        final Collection<Subject> list = new ArrayList<>();
        for (final Subject subject: candidates) {
            if (subject.needsPitchInfoDownload(Constants.DAY)) {
                list.add(subject);
            }
            if (list.size() >= maxSize) {
                break;
            }
        }
        scheduleDownloadTasksFor(list);
    }
}
