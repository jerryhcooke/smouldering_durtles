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

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.model.ApiSubject;
import com.smouldering_durtles.wk.api.model.AuxiliaryMeaning;
import com.smouldering_durtles.wk.api.model.ContextSentence;
import com.smouldering_durtles.wk.api.model.Meaning;
import com.smouldering_durtles.wk.api.model.Reading;
import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.model.AdvancedSearchParameters;
import com.smouldering_durtles.wk.model.SrsSystemRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.DAY;
import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.Constants.MAX_SEARCH_HITS;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.join;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Utility methods for searching.
 */
public final class SearchUtil {
    private static final Logger LOGGER = Logger.get(SearchUtil.class);
    private static final Pattern TERM_PATTERN = Pattern.compile("[\\p{Z}\\s]");
    private static final Pattern PERC_PATTERN = Pattern.compile("%");
    private static final Pattern US_PATTERN = Pattern.compile("_");

    private SearchUtil() {
        //
    }

    /**
     * Split a query into words suitable for searching in Java code.
     *
     * @param query the search query
     * @return the list of words
     */
    private static List<String> splitTermsForJava(final CharSequence query) {
        final String[] terms = TERM_PATTERN.split(query);
        final List<String> result = new ArrayList<>();
        for (final String term: terms) {
            if (!isEmpty(term)) {
                result.add(term.toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    /**
     * Split a query into words suitable for searching in an SQL query.
     *
     * @param query the search query
     * @return the list of words
     */
    private static List<String> splitTermsForSql(final CharSequence query) {
        final String[] terms = TERM_PATTERN.split(query);
        final List<String> result = new ArrayList<>();
        for (final String term: terms) {
            if (!isEmpty(term)) {
                result.add(
                        US_PATTERN.matcher(PERC_PATTERN.matcher(term).replaceAll("\\%")).replaceAll("\\_"));
            }
        }
        return result;
    }

    /**
     * Get a search ranking for a search term in a haystack of body text.
     * This ranking reflects how well the term matches: number of times,
     * whole word or partial, etc.
     *
     * @param haystack the body to search in
     * @param term the term to search for
     * @param weight the weight for this search, higher weight means hits in this haystack are more important
     * @return the calculated score, higher means a better match
     */
    private static int getRanking(final @Nullable String haystack, final String term, final int weight) {
        if (isEmpty(haystack)) {
            return 0;
        }

        final String uHaystack = haystack.toUpperCase(Locale.ROOT);

        int base = 0;
        int count = 0;
        while (true) {
            final int p = uHaystack.indexOf(term, base);
            if (p < 0) {
                return count * weight;
            }
            count++;
            if (p == 0 || !Character.isLetterOrDigit(uHaystack.charAt(p-1))) {
                count++;
            }
            if (p+term.length() >= uHaystack.length() || !Character.isLetterOrDigit(uHaystack.charAt(p+term.length()))) {
                count++;
            }
            base = p + term.length();
        }
    }

    /**
     * Get a search ranking: how many terms are hits in this subject,
     * and how important are those hits?. The result is summarized as an int.
     *
     * @param subject the subject
     * @param terms the search query, split into words
     * @return the ranking
     */
    private static int getSearchRanking(final Subject subject, final Iterable<String> terms) {
        int ranking = 0;

        for (final String term: terms) {
            ranking += getRanking(subject.getCharacters(), term, 100);
            ranking += getRanking(subject.getSlug(), term, 10);
            ranking += getRanking(subject.getMeaningMnemonic(), term, 1);
            ranking += getRanking(subject.getMeaningHint(), term, 1);
            ranking += getRanking(subject.getMeaningNote(), term, 1);
            ranking += getRanking(subject.getReadingMnemonic(), term, 1);
            ranking += getRanking(subject.getReadingHint(), term, 1);
            ranking += getRanking(subject.getReadingNote(), term, 1);

            ranking += getRanking(subject.getOneMeaning(), term, 100);

            for (final Meaning meaning: subject.getMeanings()) {
                ranking += getRanking(meaning.getMeaning(), term, 30);
            }

            for (final AuxiliaryMeaning meaning: subject.getAuxiliaryMeanings()) {
                ranking += getRanking(meaning.getMeaning(), term, 30);
            }

            for (final Reading reading: subject.getReadings()) {
                ranking += getRanking(reading.getValue(false), term, 30);
            }

            for (final String s: subject.getPartsOfSpeech()) {
                ranking += getRanking(s, term, 1);
            }

            for (final ContextSentence sentence: subject.getContextSentences()) {
                ranking += getRanking(sentence.getEnglish(), term, 1);
                ranking += getRanking(sentence.getJapanese(), term, 1);
            }

            for (final String s: subject.getMeaningSynonyms()) {
                ranking += getRanking(s, term, 3);
            }
        }

        return ranking;
    }

    private static List<Subject> runQuery(final String sql, final String[] args) {
        final SupportSQLiteQuery query = new SimpleSQLiteQuery(sql, args);
        return WkApplication.getDatabase().subjectCollectionsDao().getSubjectsWithRawQuery(query);
    }

    /**
     * Run a search query for subjects, for the search auto-complete.
     *
     * @param query the query text
     * @return the list of found subjects
     */
    public static List<Subject> searchSubjectSuggestions(final String query) {
        if (query.trim().length() < 3) {
            return Collections.emptyList();
        }

        final List<String> terms = splitTermsForSql(query);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sb = new StringBuilder();
        final String[] args = new String[terms.size()];
        sb.append("SELECT * FROM subject WHERE hiddenAt=0 AND object IS NOT NULL");
        int i = 0;
        for (final String term: terms) {
            sb.append(" AND smallSearchTarget LIKE ?");
            args[i++] = "%" + term + "%";
        }
        sb.append(" LIMIT ").append(MAX_SEARCH_HITS);
        List<Subject> subjects = runQuery(sb.toString(), args);

        if (subjects.size() < MAX_SEARCH_HITS) {
            final StringBuilder sb2 = new StringBuilder();
            sb2.append("SELECT * FROM subject WHERE hiddenAt=0 AND object IS NOT NULL");
            for (int j=0; j<terms.size(); j++) {
                sb2.append(" AND searchTarget LIKE ?");
            }
            sb2.append(" LIMIT ").append(MAX_SEARCH_HITS);
            subjects = runQuery(sb2.toString(), args);
        }

        sortSearchResult(query, subjects);

        return subjects;
    }

    /**
     * Run a search query for subjects.
     *
     * @param query the query text
     * @return the list of found subjects
     */
    private static List<Subject> searchSubjects(final CharSequence query) {
        final List<String> terms = splitTermsForSql(query);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder sb = new StringBuilder();
        final String[] args = new String[terms.size()];
        sb.append("SELECT * FROM subject WHERE hiddenAt=0 AND object IS NOT NULL");
        int i = 0;
        for (final String term: terms) {
            sb.append(" AND searchTarget LIKE ?");
            args[i++] = "%" + term + "%";
        }
        final List<Subject> subjects = runQuery(sb.toString(), args);

        sortSearchResult(query, subjects);

        return subjects;
    }

    /**
     * Rank a search result and sort it, best match first. A subject whose primary meaning is
     * exactly the query sorts above everything else, since that is almost always what was
     * being looked for; the rest fall back to the computed ranking.
     *
     * @param query the search query
     * @param subjects the results to rank and sort, sorted in place
     */
    private static void sortSearchResult(final CharSequence query, final List<Subject> subjects) {
        final List<String> javaTerms = splitTermsForJava(query);
        for (final Subject subject: subjects) {
            subject.setRanking(getSearchRanking(subject, javaTerms));
        }

        final String queryUpperCase = query.toString().toUpperCase(Locale.ROOT);

        Collections.sort(subjects, (o1, o2) -> {
            // Both flags have to be computed before comparing them. Returning early on the first
            // exact match alone would report o1 < o2 and o2 < o1 when both match, which breaks the
            // Comparator contract and makes TimSort throw once a result set is large enough.
            final boolean exact1 = o1.getOneMeaning().toUpperCase(Locale.ROOT).contentEquals(queryUpperCase);
            final boolean exact2 = o2.getOneMeaning().toUpperCase(Locale.ROOT).contentEquals(queryUpperCase);
            if (exact1 != exact2) {
                return exact1 ? -1 : 1;
            }
            return Integer.compare(o2.getRanking(), o1.getRanking());
        });
    }

    /**
     * Run an advanced search query for subjects.
     *
     * @param parameters the query parameters
     * @return the list of found subjects
     */
    private static List<Subject> searchSubjects(final AdvancedSearchParameters parameters) {
        final StringBuilder sb = new StringBuilder();

        sb.append("SELECT * FROM subject WHERE hiddenAt = 0 AND object IS NOT NULL");

        if (parameters.minLevel != null) {
            sb.append(" AND level >= ");
            sb.append(parameters.minLevel);
        }

        if (parameters.maxLevel != null) {
            sb.append(" AND level <= ");
            sb.append(parameters.maxLevel);
        }

        if (parameters.minFrequency != null) {
            sb.append(" AND frequency >= ");
            sb.append(parameters.minFrequency);
        }

        if (parameters.maxFrequency != null) {
            sb.append(" AND frequency <= ");
            sb.append(parameters.maxFrequency);
        }

        if (parameters.leechesOnly) {
            sb.append(" AND ");
            sb.append(SrsSystemRepository.getLeechFilter());
            sb.append(" AND leechScore > 1000");
        }

        if (parameters.upcomingReviewLessThan != null || parameters.upcomingReviewMoreThan != null) {
            sb.append(" AND availableAt != 0");
        }

        if (parameters.upcomingReviewLessThan != null) {
            sb.append(" AND availableAt <= ");
            sb.append(System.currentTimeMillis() + parameters.upcomingReviewLessThan * HOUR);
        }

        if (parameters.upcomingReviewMoreThan != null) {
            sb.append(" AND availableAt >= ");
            sb.append(System.currentTimeMillis() + parameters.upcomingReviewMoreThan * HOUR);
        }

        if (parameters.burnedLessThan != null || parameters.burnedMoreThan != null) {
            sb.append(" AND burnedAt != 0");
        }

        if (parameters.burnedLessThan != null) {
            sb.append(" AND burnedAt >= ");
            sb.append(System.currentTimeMillis() - parameters.burnedLessThan * DAY);
        }

        if (parameters.burnedMoreThan != null) {
            sb.append(" AND burnedAt <= ");
            sb.append(System.currentTimeMillis() - parameters.burnedMoreThan * DAY);
        }

        if (parameters.incorrectAnswerWithin != null) {
            sb.append(" AND lastIncorrectAnswer != 0 AND lastIncorrectAnswer >= ");
            sb.append(System.currentTimeMillis() - parameters.incorrectAnswerWithin * HOUR);
        }

        if (GlobalSettings.Other.getEnableStarRatings()) {
            final Collection<String> starRatingFragments = new ArrayList<>();
            for (final Integer rating: parameters.starRatings) {
                starRatingFragments.add(rating.toString());
            }
            if (!starRatingFragments.isEmpty()) {
                sb.append(" AND typeCode IN ");
                sb.append(join(", ", "(", ")", starRatingFragments));
            }
        }

        final Collection<String> srsStageFragments = new ArrayList<>();
        for (final String srsStageTag: parameters.srsStages) {
            SrsSystemRepository.addSrsStageFragments(srsStageFragments, srsStageTag);
        }
        if (!srsStageFragments.isEmpty()) {
            sb.append(" AND ");
            sb.append(join(" OR ", "(", ")", srsStageFragments));
        }

        final Collection<String> itemTypeFragments = new ArrayList<>();
        for (final SubjectType itemType: parameters.itemTypes) {
            itemTypeFragments.add("'" + itemType.getDbTypeName() + "'");
        }
        if (!itemTypeFragments.isEmpty()) {
            sb.append(" AND object IN ");
            sb.append(join(", ", "(", ")", itemTypeFragments));
        }

        final Collection<String> jlptLevelFragments = new ArrayList<>();
        for (final Integer level: parameters.jlptLevels) {
            jlptLevelFragments.add(level.toString());
        }
        if (!jlptLevelFragments.isEmpty()) {
            sb.append(" AND jlptLevel IN ");
            sb.append(join(", ", "(", ")", jlptLevelFragments));
        }

        final Collection<String> joyoGradeFragments = new ArrayList<>();
        for (final Integer grade: parameters.joyoGrades) {
            joyoGradeFragments.add(grade.toString());
        }
        if (!joyoGradeFragments.isEmpty()) {
            sb.append(" AND joyoGrade IN ");
            sb.append(join(", ", "(", ")", joyoGradeFragments));
        }

        sb.append(" ORDER BY level, lessonPosition, id");

        final String sql = sb.toString();
        LOGGER.info("Advanced search query: %s", sql);
        final SupportSQLiteQuery query = new SimpleSQLiteQuery(sql);
        return WkApplication.getDatabase().subjectCollectionsDao().getSubjectsWithRawQuery(query);
    }

    /**
     * Run a search query for subjects.
     *
     * @param searchType the type of search
     * @param searchParameters the parameters for the search
     * @return the list of found subjects
     */
    public static List<Subject> searchSubjects(final int searchType, final String searchParameters) {
        if (searchType == 0) {
            final int level = Integer.parseInt(searchParameters, 10);
            return WkApplication.getDatabase().subjectCollectionsDao().getByLevelRange(level, level);
        }

        if (searchType == 1) {
            return searchSubjects(searchParameters);
        }

        if (searchType == 2) {
            return safe(Collections::emptyList, () -> {
                final AdvancedSearchParameters parameters = Converters.getObjectMapper().readValue(searchParameters, AdvancedSearchParameters.class);
                return searchSubjects(parameters);
            });
        }

        return Collections.emptyList();
    }

    /**
     * Build a string field for the database that contains the searchable text for
     * a subject.
     *
     * @param apiSubject the subject to examine
     * @return a string with all searchable text for the subject
     */
    public static String findSearchTarget(final ApiSubject apiSubject) {
        final Collection<String> result = new ArrayList<>();

        if (!isEmpty(apiSubject.getCharacters())) {
            result.add(apiSubject.getCharacters());
        }
        if (!isEmpty(apiSubject.getSlug())) {
            result.add(apiSubject.getSlug());
        }
        if (!isEmpty(apiSubject.getMeaningMnemonic())) {
            result.add(apiSubject.getMeaningMnemonic());
        }
        if (!isEmpty(apiSubject.getMeaningHint())) {
            result.add(apiSubject.getMeaningHint());
        }
        if (!isEmpty(apiSubject.getReadingMnemonic())) {
            result.add(apiSubject.getReadingMnemonic());
        }
        if (!isEmpty(apiSubject.getReadingHint())) {
            result.add(apiSubject.getReadingHint());
        }

        for (final Meaning meaning: apiSubject.getMeanings()) {
            result.add(meaning.getMeaning());
        }

        for (final AuxiliaryMeaning meaning: apiSubject.getAuxiliaryMeanings()) {
            result.add(meaning.getMeaning());
        }

        for (final Reading reading: apiSubject.getReadings()) {
            result.add(reading.getValue(false));
        }

        result.addAll(apiSubject.getPartsOfSpeech());

        for (final ContextSentence sentence: apiSubject.getContextSentences()) {
            result.add(sentence.getEnglish());
            result.add(sentence.getJapanese());
        }

        return join(" ", "", "", result);
    }

    /**
     * Build a string field for the database that contains the searchable text for
     * a subject. This variant only includes the high-priority fields.
     *
     * @param apiSubject the subject to examine
     * @return a string with all searchable text for the subject
     */
    public static String findSmallSearchTarget(final ApiSubject apiSubject) {
        final Collection<String> result = new ArrayList<>();

        if (!isEmpty(apiSubject.getCharacters())) {
            result.add(apiSubject.getCharacters());
        }
        if (!isEmpty(apiSubject.getSlug())) {
            result.add(apiSubject.getSlug());
        }

        for (final Meaning meaning: apiSubject.getMeanings()) {
            result.add(meaning.getMeaning());
        }

        for (final AuxiliaryMeaning meaning: apiSubject.getAuxiliaryMeanings()) {
            result.add(meaning.getMeaning());
        }

        for (final Reading reading: apiSubject.getReadings()) {
            result.add(reading.getValue(false));
        }

        return join(" ", "", "", result);
    }
}
