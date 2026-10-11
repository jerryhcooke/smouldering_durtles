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

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smouldering_durtles.wk.BuildConfig;
import com.smouldering_durtles.wk.Constants;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.Identification;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.services.ApiTaskService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqual;

/**
 * Utility methods for the JPDB frequency rank and Nadeshiko sentence count features.
 *
 * <p>
 *     Caching strategy: both data sources are cached permanently in term-keyed tables
 *     (jpdb_frequency, nadeshiko_count) that are not touched by database resets or subject syncs.
 *     The values are then copied onto the subject table for display. The JPDB dictionary is
 *     downloaded once per revision, and a Nadeshiko count is only requested for a term that has
 *     no successful result in the cache. Failed requests are never cached as results.
 * </p>
 */
public final class FrequencyUtil {
    private static final Logger LOGGER = Logger.get(FrequencyUtil.class);

    /**
     * URL of the JPDB frequency dictionary in Yomitan format.
     */
    private static final String JPDB_URL =
            "https://github.com/Kuuuube/yomitan-dictionaries/raw/main/dictionaries/JPDB_v2.2_Frequency_2024-10-13.zip";

    /**
     * The revision of the JPDB dictionary at JPDB_URL. Changing this (together with the URL) is the only
     * thing that will cause the dictionary to be downloaded again.
     */
    public static final String JPDB_REVISION = "JPDB v2.2 Frequency 2024-10-13";

    private static final String NADESHIKO_URL = "https://api.nadeshiko.co/v1/search/words";

    /**
     * Max words per Nadeshiko request. The API allows up to 100.
     */
    public static final int NADESHIKO_BATCH_SIZE = 50;

    private static final int JPDB_INSERT_BATCH_SIZE = 2000;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private FrequencyUtil() {
        //
    }

    /**
     * Normalize a subject's characters to a lookup term: strip the 〜 markers WaniKani uses for suffixes/prefixes.
     *
     * @param characters the subject's characters
     * @return the term
     */
    public static String normalizeTerm(final String characters) {
        return characters.replaceAll("^[〜～~]+|[〜～~]+$", "").trim();
    }

    private static OkHttpClient buildClient(final long readTimeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    private static String userAgent() {
        return Identification.APP_NAME_UA + "/" + BuildConfig.VERSION_NAME;
    }

    // ------------------------------------------------------------------------------------------------
    // Scheduling
    // ------------------------------------------------------------------------------------------------

    /**
     * Schedule background tasks to get frequency data for the subjects of a new session.
     * Only schedules work that is actually missing. Must be called on a background thread.
     *
     * @param subjects the session's subjects
     */
    public static void scheduleForSession(final Iterable<Subject> subjects) {
        ObjectSupport.safe(() -> scheduleForSessionImpl(subjects));
    }

    private static void scheduleForSessionImpl(final Iterable<Subject> subjects) {
        final boolean jpdb = GlobalSettings.SubjectInfo.getShowJpdbFrequency();
        final boolean nadeshiko = GlobalSettings.SubjectInfo.getShowNadeshikoCount() && canUseNadeshikoKey();
        if (!jpdb && !nadeshiko) {
            return;
        }

        final AppDatabase db = WkApplication.getDatabase();
        boolean needsJpdb = false;
        final List<Long> nadeshikoIds = new ArrayList<>();
        for (final Subject subject: subjects) {
            if (!subject.canHaveFrequencyData()) {
                continue;
            }
            if (jpdb && subject.getJpdbRank() == 0) {
                needsJpdb = true;
            }
            if (nadeshiko && subject.getNadeshikoCount() < 0) {
                nadeshikoIds.add(subject.getId());
            }
        }

        boolean scheduled = false;
        if (jpdb && !needsJpdb) {
            final @Nullable String revision = db.propertiesDao().getJpdbRevision();
            needsJpdb = revision != null && !revision.equals(JPDB_REVISION);
        }
        if (needsJpdb && !(needsJpdbDownload() && recentlyFailedJpdbDownload())) {
            db.assertDownloadJpdbFrequencyTask();
            scheduled = true;
        }

        for (int i = 0; i < nadeshikoIds.size(); i += NADESHIKO_BATCH_SIZE) {
            db.assertFetchNadeshikoCountsTask(nadeshikoIds.subList(i, Math.min(i + NADESHIKO_BATCH_SIZE, nadeshikoIds.size())));
            scheduled = true;
        }

        if (scheduled) {
            ApiTaskService.schedule();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // JPDB
    // ------------------------------------------------------------------------------------------------

    /**
     * Does the JPDB dictionary still need to be downloaded?.
     *
     * @return true if it does
     */
    public static boolean needsJpdbDownload() {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable String revision = db.propertiesDao().getJpdbRevision();
        if (revision != null && !revision.equals(JPDB_REVISION)) {
            return true;
        }
        return db.frequencyDao().getJpdbCount() == 0;
    }

    private static boolean recentlyFailedJpdbDownload() {
        final long lastFailure = WkApplication.getDatabase().propertiesDao().getLastJpdbDownloadFailure();
        return lastFailure != 0 && System.currentTimeMillis() - lastFailure < Constants.HOUR;
    }

    /**
     * Download the JPDB dictionary and import it into the jpdb_frequency table in a single transaction.
     * The zip is streamed and parsed incrementally, so neither the 25 MB JSON nor the full dictionary is
     * ever held in memory.
     *
     * @return true if successful
     */
    public static boolean downloadAndImportJpdb() {
        final AppDatabase db = WkApplication.getDatabase();
        LOGGER.info("Downloading JPDB frequency dictionary: %s", JPDB_URL);
        final Request request = new Request.Builder()
                .header("User-Agent", userAgent())
                .url(JPDB_URL)
                .build();
        try (final Response response = buildClient(120).newCall(request).execute()) {
            final @Nullable ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                LOGGER.info("JPDB download failed with HTTP %d", response.code());
                return false;
            }
            final int[] count = {0};
            db.runInTransaction(() -> {
                db.frequencyDao().deleteAllJpdb();
                try {
                    count[0] = JpdbDictionaryParser.parseZip(body.byteStream(), JPDB_INSERT_BATCH_SIZE,
                            batch -> db.frequencyDao().insertJpdb(batch));
                }
                catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                if (count[0] == 0) {
                    throw new RuntimeException("JPDB dictionary contained no usable entries");
                }
                // A new revision may change ranks, so re-resolve every subject from the new data
                db.frequencyDao().resetAllJpdbRanks();
            });
            db.propertiesDao().setJpdbRevision(JPDB_REVISION);
            db.propertiesDao().setLastJpdbDownloadFailure(0);
            LOGGER.info("Imported %d JPDB frequency entries", count[0]);
            return true;
        }
        catch (final Exception e) {
            LOGGER.error(e, "JPDB download/import failed");
            return false;
        }
    }

    /**
     * Look up the JPDB rank for a subject in the local cache, preferring the entry for the reading WaniKani teaches.
     *
     * @param subject the subject
     * @return the rank, or -1 if not in JPDB
     */
    public static int lookupJpdbRank(final Subject subject) {
        final String term = normalizeTerm(subject.getCharacters() == null ? "" : subject.getCharacters());
        if (term.isEmpty()) {
            return -1;
        }
        return JpdbDictionaryParser.pickRank(WkApplication.getDatabase().frequencyDao().getJpdbByTerm(term),
                subject.getFrequencyLookupReadings());
    }

    /**
     * Resolve the JPDB rank for all vocab subjects that don't have one yet, from the local cache. No network.
     */
    public static void resolveAllJpdbRanks() {
        final AppDatabase db = WkApplication.getDatabase();
        final List<Subject> subjects = db.frequencyDao().getUnresolvedJpdbSubjects();
        if (subjects.isEmpty()) {
            return;
        }
        LOGGER.info("Resolving JPDB ranks for %d subjects", subjects.size());
        db.runInTransaction(() -> {
            for (final Subject subject: subjects) {
                db.frequencyDao().updateJpdbRank(subject.getId(), lookupJpdbRank(subject));
            }
        });
    }

    // ------------------------------------------------------------------------------------------------
    // Nadeshiko
    // ------------------------------------------------------------------------------------------------

    private static String hashKey(final String key) {
        return ObjectSupport.<String>safe("", () -> {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder();
            for (final byte b: digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        });
    }

    /**
     * Is there a Nadeshiko API key that hasn't been rejected?.
     *
     * @return true if there is
     */
    public static boolean canUseNadeshikoKey() {
        final @Nullable String key = GlobalSettings.SubjectInfo.getNadeshikoApiKey();
        if (isEmpty(key)) {
            return false;
        }
        return !isEqual(hashKey(key), WkApplication.getDatabase().propertiesDao().getRejectedNadeshikoKey());
    }

    /**
     * Fetch Nadeshiko match counts for a batch of terms in a single request.
     *
     * @param terms the terms, at most NADESHIKO_BATCH_SIZE
     * @return the counts for the terms that were returned by the API; empty on any failure
     */
    public static Map<String, Integer> fetchNadeshikoCounts(final Collection<String> terms) {
        final Map<String, Integer> result = new HashMap<>();
        final @Nullable String key = GlobalSettings.SubjectInfo.getNadeshikoApiKey();
        if (isEmpty(key) || terms.isEmpty()) {
            return result;
        }
        try {
            final ObjectMapper mapper = Converters.getObjectMapper();
            final ObjectNode root = mapper.createObjectNode();
            final ArrayNode words = root.putObject("query").putArray("words");
            for (final String term: terms) {
                words.add(term);
            }
            final Request request = new Request.Builder()
                    .header("User-Agent", userAgent())
                    .header("Authorization", "Bearer " + key)
                    .url(NADESHIKO_URL)
                    .post(RequestBody.create(mapper.writeValueAsString(root), JSON_MEDIA_TYPE))
                    .build();
            LOGGER.info("Fetching Nadeshiko counts for %d terms", terms.size());
            try (final Response response = buildClient(30).newCall(request).execute()) {
                if (response.code() == 401) {
                    LOGGER.info("Nadeshiko API key rejected");
                    WkApplication.getDatabase().propertiesDao().setRejectedNadeshikoKey(hashKey(key));
                    return result;
                }
                final @Nullable ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    // Transient (429, 5xx, quota...): cache nothing, the terms are retried next session
                    LOGGER.info("Nadeshiko request failed with HTTP %d", response.code());
                    return result;
                }
                final JsonNode json = mapper.readTree(body.byteStream());
                final @Nullable JsonNode results = json.get("results");
                if (results == null || !results.isArray()) {
                    return result;
                }
                for (final JsonNode item: results) {
                    final @Nullable JsonNode word = item.get("word");
                    final @Nullable JsonNode count = item.get("matchCount");
                    if (word != null && count != null && count.isNumber() && terms.contains(word.asText())) {
                        result.put(word.asText(), count.asInt());
                    }
                }
            }
        }
        catch (final Exception e) {
            LOGGER.error(e, "Nadeshiko request failed");
            result.clear();
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------------------------------------

    private static final int COLOR_HIGH = Color.rgb(0x2e, 0x7d, 0x32);
    private static final int COLOR_MEDIUM = Color.rgb(0xb2, 0x6a, 0x00);
    private static final int COLOR_LOW = Color.rgb(0xc6, 0x28, 0x28);

    /**
     * Get the color for a JPDB rank, using the same thresholds as the userscript.
     *
     * @param rank the rank
     * @return the color
     */
    public static int getJpdbRankColor(final int rank) {
        if (rank < 1500) {
            return COLOR_HIGH;
        }
        if (rank < 5000) {
            return COLOR_MEDIUM;
        }
        return COLOR_LOW;
    }

    /**
     * Should a frequency badge be shown for this subject at all?.
     *
     * @param subject the subject
     * @return true if it should
     */
    public static boolean isBadgeEnabledFor(final Subject subject) {
        return subject.canHaveFrequencyData()
                && (GlobalSettings.SubjectInfo.getShowJpdbFrequency() || GlobalSettings.SubjectInfo.getShowNadeshikoCount());
    }

    /**
     * Format the JPDB rank for display.
     *
     * @param subject the subject
     * @return the text, or null if the feature is disabled
     */
    public static @Nullable String formatJpdb(final Subject subject) {
        if (!GlobalSettings.SubjectInfo.getShowJpdbFrequency() || !subject.canHaveFrequencyData()) {
            return null;
        }
        final int rank = subject.getJpdbRank();
        if (rank == 0) {
            return "…";
        }
        if (rank < 0) {
            return "not ranked";
        }
        return String.format(Locale.ROOT, "#%d", rank);
    }

    /**
     * Format the Nadeshiko count for display.
     *
     * @param subject the subject
     * @return the text, or null if the feature is disabled
     */
    public static @Nullable String formatNadeshiko(final Subject subject) {
        if (!GlobalSettings.SubjectInfo.getShowNadeshikoCount() || !subject.canHaveFrequencyData()) {
            return null;
        }
        final int count = subject.getNadeshikoCount();
        if (count < 0) {
            return isEmpty(GlobalSettings.SubjectInfo.getNadeshikoApiKey()) ? "no API key" : "…";
        }
        return String.format(Locale.ROOT, "%,d sentences", count);
    }

    /**
     * Build the compact badge text, e.g. "JPDB #1165 · N 342". The JPDB part is colour coded.
     *
     * @param subject the subject
     * @return the text, or null if nothing should be shown
     */
    public static @Nullable CharSequence buildBadgeText(final Subject subject) {
        if (!isBadgeEnabledFor(subject)) {
            return null;
        }
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        if (GlobalSettings.SubjectInfo.getShowJpdbFrequency()) {
            final int rank = subject.getJpdbRank();
            sb.append("JPDB ");
            final int start = sb.length();
            if (rank == 0) {
                sb.append("…");
            }
            else if (rank < 0) {
                sb.append("—");
            }
            else {
                sb.append(String.format(Locale.ROOT, "#%d", rank));
                sb.setSpan(new ForegroundColorSpan(getJpdbRankColor(rank)), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (GlobalSettings.SubjectInfo.getShowNadeshikoCount()) {
            if (sb.length() > 0) {
                sb.append("  ·  ");
            }
            final int count = subject.getNadeshikoCount();
            sb.append("N ");
            if (count < 0) {
                sb.append(isEmpty(GlobalSettings.SubjectInfo.getNadeshikoApiKey()) ? "no key" : "…");
            }
            else {
                sb.append(String.format(Locale.ROOT, "%,d", count));
            }
        }
        return sb;
    }

    /**
     * Create a rounded light background for the badge so it's readable on any subject colour.
     *
     * @param context Android context
     * @return the drawable
     */
    public static GradientDrawable createBadgeBackground(final Context context) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.argb(0xe6, 0xff, 0xff, 0xff));
        drawable.setCornerRadius(6 * context.getResources().getDisplayMetrics().density);
        return drawable;
    }

    /**
     * The Nadeshiko search URL for a subject, for opening in a browser.
     *
     * @param subject the subject
     * @return the URL
     */
    public static String getNadeshikoSearchUrl(final Subject subject) {
        final String term = normalizeTerm(subject.getCharacters() == null ? "" : subject.getCharacters());
        return "https://nadeshiko.co/search/" + android.net.Uri.encode(term);
    }

    private static final java.util.Set<Long> REQUESTED_IN_THIS_PROCESS =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * If a displayed subject is still missing frequency data (e.g. the setting was enabled in the middle
     * of a session), schedule the work for it in the background. At most once per subject per process;
     * the tasks themselves check the caches, so this never causes a duplicate fetch.
     *
     * @param subject the subject
     */
    public static void requestIfMissing(final Subject subject) {
        if (!subject.canHaveFrequencyData()) {
            return;
        }
        final boolean missingJpdb = GlobalSettings.SubjectInfo.getShowJpdbFrequency() && subject.getJpdbRank() == 0;
        final boolean missingNadeshiko = GlobalSettings.SubjectInfo.getShowNadeshikoCount() && subject.getNadeshikoCount() < 0
                && !isEmpty(GlobalSettings.SubjectInfo.getNadeshikoApiKey());
        if ((missingJpdb || missingNadeshiko) && REQUESTED_IN_THIS_PROCESS.add(subject.getId())) {
            ObjectSupport.runAsync(() -> scheduleForSession(java.util.Collections.singletonList(subject)));
        }
    }

    /**
     * Bind a badge TextView to a subject: set text, background and visibility. Tapping the badge
     * opens the Nadeshiko search page for the word.
     *
     * @param view the badge view, may be null
     * @param subject the subject
     * @param allowed false to force-hide the badge
     */
    public static void bindBadge(final @Nullable android.widget.TextView view, final Subject subject, final boolean allowed) {
        requestIfMissing(subject);
        if (view == null) {
            return;
        }
        final @Nullable CharSequence text = allowed ? buildBadgeText(subject) : null;
        if (text == null || text.length() == 0) {
            view.setVisibility(android.view.View.GONE);
            view.setOnClickListener(null);
            view.setClickable(false);
            return;
        }
        view.setText(text);
        view.setBackground(createBadgeBackground(view.getContext()));
        view.setVisibility(android.view.View.VISIBLE);
        if (GlobalSettings.SubjectInfo.getShowNadeshikoCount()) {
            final String url = getNadeshikoSearchUrl(subject);
            view.setOnClickListener(v -> ObjectSupport.safe(() -> v.getContext().startActivity(
                    new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))));
        }
        else {
            view.setOnClickListener(null);
            view.setClickable(false);
        }
    }
}
