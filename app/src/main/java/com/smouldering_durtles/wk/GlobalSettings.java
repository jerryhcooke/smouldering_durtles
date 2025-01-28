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

package com.smouldering_durtles.wk;


import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;


import androidx.preference.PreferenceManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.CloseEnoughAction;
import com.smouldering_durtles.wk.enums.DontKnowButtonBehavior;
import com.smouldering_durtles.wk.enums.LessonOrder;
import com.smouldering_durtles.wk.enums.NetworkRule;
import com.smouldering_durtles.wk.enums.NotificationCategory;
import com.smouldering_durtles.wk.enums.NotificationPriority;
import com.smouldering_durtles.wk.enums.NotificationUpdateFrequency;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.enums.ReviewOrder;
import com.smouldering_durtles.wk.enums.SessionPriority;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.enums.SpecialButtonBehavior;
import com.smouldering_durtles.wk.enums.SubjectCardLayout;
import com.smouldering_durtles.wk.enums.SubjectInfoDump;
import com.smouldering_durtles.wk.enums.TimeLineBarChartGridStyle;
import com.smouldering_durtles.wk.enums.TimeLineBarChartStyle;
import com.smouldering_durtles.wk.enums.VoicePreference;
import com.smouldering_durtles.wk.model.SubjectSelectionRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.DEFAULT_OVERDUE_THRESHOLD;
import static com.smouldering_durtles.wk.Constants.NUM_THEME_CUSTOMIZATION_OPTIONS;
import static com.smouldering_durtles.wk.db.Converters.getObjectMapper;
import static com.smouldering_durtles.wk.enums.SessionType.LESSON;
import static com.smouldering_durtles.wk.enums.SessionType.REVIEW;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static java.util.Objects.requireNonNull;

/**
 * A class with a bunch of static accessors for app settings.
 *
 * <p>
 *     Most settings only have getters, since most settings can only be modified
 *     via the settings screen. These accessors also handle default values and
 *     format conversions transparently.
 * </p>
 */
public final class GlobalSettings {
    private static @Nullable WkApplication application = null;

    /**
     * Set the application instance.
     *
     * @param application the application
     */
    public static void setApplication(final WkApplication application) {
        GlobalSettings.application = application;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private GlobalSettings() {
        //
    }

    /**
     * Get the shared preferences object that holds all preferences as a key-value store.
     *
     * @return the setting store
     */
    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(requireNonNull(application));
    }

    /**
     * This is 0 if the user in in the first-time setup phase.
     *
     * @return the value
     */
    public static int getFirstTimeSetup() {
        return prefs().getInt("first_time_setup", 0);
    }

    /**
     * This is 0 if the user in in the first-time setup phase.
     *
     * @param value the value
     */
    public static void setFirstTimeSetup(final int value) {
        final SharedPreferences.Editor editor = prefs().edit();
        editor.putInt("first_time_setup", value);
        editor.apply();
    }

    /**
     * This is the handler for user settings backup
     *
     */
    public static Map<String, ?> getAllSettings() {
        Map<String, ?> entries = prefs().getAll();
        Map<String, Object> result = new HashMap<>();
        // ...

        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Log.d("Settings", "Key: " + key + " Value: " + value.toString() + " Type: " + value.getClass().getSimpleName());
            // ...
            result.put(key, value);
            }
        return result;
    }

    public static void setAllSettings(Map<String, Object> settings) {
        SharedPreferences.Editor editor = prefs().edit();

        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Log.d("Settings", "Key: " + key + " Value: " + value.toString());

            if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else {
                throw new IllegalArgumentException("Unsupported setting type: " + value.getClass().getName());
            }
        }
        editor.apply();
    }



    /**
     * Global unlock of all of the advanced settings.
     *
     * @return the value
     */
    public static boolean getAdvancedEnabled() {
        return prefs().getBoolean("enable_advanced", false);
    }

    /**
     * Global unlock of all of the advanced settings.
     *
     * @param value the value
     */
    public static void setAdvancedEnabled(final boolean value) {
        final SharedPreferences.Editor editor = prefs().edit();
        editor.putBoolean("enable_advanced", value);
        editor.apply();
    }

    /**
     * Get the Anki mode setting for the given combination of session type and question type.
     *
     * @param sessionType the type of the current session
     * @param questionType the type of the current question
     * @return true if Anki mode is enabled
     */
    public static boolean getAnkiMode(final SessionType sessionType, final QuestionType questionType) {
        if (sessionType == LESSON) {
            return questionType.isMeaning()
                    ? AdvancedLesson.getAnkiModeMeaning()
                    : AdvancedLesson.getAnkiModeReading();
        }
        else if (sessionType == REVIEW) {
            return questionType.isMeaning()
                    ? AdvancedReview.getAnkiModeMeaning()
                    : AdvancedReview.getAnkiModeReading();
        }
        else {
            return questionType.isMeaning()
                    ? AdvancedSelfStudy.getAnkiModeMeaning()
                    : AdvancedSelfStudy.getAnkiModeReading();
        }
    }

    /**
     * Get the randomize inflection setting for the given combination of session type.
     *
     * @param sessionType the type of the current session
     * @return true if randomize inflection is enabled
     */
    public static boolean getRandomizeInflections(final SessionType sessionType) {
        if (sessionType == LESSON) {
            return false;
        }
        else if (sessionType == REVIEW) {
            return AdvancedReview.getRandomizeInflections();
        }
        else {
            return AdvancedSelfStudy.getRandomizeInflections();
        }
    }

    /**
     * Get the autoplay setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if autoplay is enabled
     */
    public static boolean getAutoPlay(final SessionType sessionType) {
        return sessionType == LESSON ? Audio.getAutoplayLessons() : Audio.getAutoplayReviews();
    }

    /**
     * Get the shuffle-after-selection setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if shuffle-after-selection is enabled
     */
    public static boolean getShuffleAfterSelection(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getShuffleAfterSelection();
            case REVIEW:
                return AdvancedReview.getShuffleAfterSelection();
            case SELF_STUDY:
                return AdvancedSelfStudy.getShuffleAfterSelection();
            default:
                return false;
        }
    }

    /**
     * Get the back-to-back setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if back-to-back is enabled
     */
    public static boolean getBackToBack(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getBackToBack();
            case REVIEW:
                return AdvancedReview.getBackToBack();
            case SELF_STUDY:
                return AdvancedSelfStudy.getBackToBack();
            default:
                return false;
        }
    }

    /**
     * Get the reading-before-meaning setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if reading-before-meaning is enabled
     */
    public static boolean getReadingFirst(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getReadingFirst();
            case REVIEW:
                return AdvancedReview.getReadingFirst();
            case SELF_STUDY:
                return AdvancedSelfStudy.getReadingFirst();
            default:
                return false;
        }
    }

    /**
     * Get the meaning-before-reading setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if meaning-before-reading is enabled
     */
    public static boolean getMeaningFirst(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getMeaningFirst();
            case REVIEW:
                return AdvancedReview.getMeaningFirst();
            case SELF_STUDY:
                return AdvancedSelfStudy.getMeaningFirst();
            default:
                return false;
        }
    }

    /**
     * Get the subject comparator (for session ordering) for the given session type.
     *
     * @param sessionType the type of the current session
     * @return the comparator
     */
    public static Comparator<Subject> getSubjectComparator(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getOrder().getComparator();
            case REVIEW:
                return AdvancedReview.getOrder().getComparator();
            case SELF_STUDY:
            default:
                return AdvancedSelfStudy.getOrder().getComparator();
        }
    }

    /**
     * Get the order-reversed setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if order-reversed is enabled
     */
    public static boolean getOrderReversed(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getOrderReversed();
            case REVIEW:
                return AdvancedReview.getOrderReversed();
            case SELF_STUDY:
                return AdvancedSelfStudy.getOrderReversed();
            default:
                return false;
        }
    }

    /**
     * Get the order-overdue-first setting for the given session type.
     *
     * @param sessionType the type of the current session
     * @return true if order-overdue-first is enabled
     */
    public static boolean getOrderOverdueFirst(final SessionType sessionType) {
        switch (sessionType) {
            case REVIEW:
                return AdvancedReview.getOrderOverdueFirst();
            case SELF_STUDY:
                return AdvancedSelfStudy.getOrderOverdueFirst();
            case LESSON:
            default:
                return false;
        }
    }

    /**
     * Get the session order priority for the given session type.
     *
     * @param sessionType the type of the current session
     * @return the order priority
     */
    public static SessionPriority getOrderPriority(final SessionType sessionType) {
        switch (sessionType) {
            case LESSON:
                return AdvancedLesson.getOrderPriority();
            case REVIEW:
                return AdvancedReview.getOrderPriority();
            case SELF_STUDY:
            default:
                return AdvancedSelfStudy.getOrderPriority();
        }
    }

    /**
     * Subject selection: min of current-level subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsCurrentMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsCurrentMin();
            case REVIEW:
                return AdvancedReview.getSubjectsCurrentMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsCurrentMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of current-level subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsCurrentMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsCurrentMax();
            case REVIEW:
                return AdvancedReview.getSubjectsCurrentMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsCurrentMax();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: min of earlier-level subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsEarlierMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsEarlierMin();
            case REVIEW:
                return AdvancedReview.getSubjectsEarlierMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsEarlierMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of earlier-level subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsEarlierMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsEarlierMax();
            case REVIEW:
                return AdvancedReview.getSubjectsEarlierMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsEarlierMax();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: min of radical subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsRadicalMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsRadicalMin();
            case REVIEW:
                return AdvancedReview.getSubjectsRadicalMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsRadicalMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of radical subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsRadicalMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsRadicalMax();
            case REVIEW:
                return AdvancedReview.getSubjectsRadicalMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsRadicalMax();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: min of kanji subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsKanjiMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsKanjiMin();
            case REVIEW:
                return AdvancedReview.getSubjectsKanjiMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsKanjiMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of kanji subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsKanjiMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsKanjiMax();
            case REVIEW:
                return AdvancedReview.getSubjectsKanjiMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsKanjiMax();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: min of vocabulary subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsVocabularyMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsVocabularyMin();
            case REVIEW:
                return AdvancedReview.getSubjectsVocabularyMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsVocabularyMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of vocabulary subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsVocabularyMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsVocabularyMax();
            case REVIEW:
                return AdvancedReview.getSubjectsVocabularyMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsVocabularyMax();
            default:
                return -1;
        }
    }
    /**
     * Subject selection: min of kana vocabulary subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsKanaVocabularyMin(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsKanaVocabularyMin();
            case REVIEW:
                return AdvancedReview.getSubjectsKanaVocabularyMin();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsKanaVocabularyMin();
            default:
                return -1;
        }
    }

    /**
     * Subject selection: max of kana vocabulary subjects, for a specific session type.
     *
     * @param type the type of the current session
     * @return the value
     */
    private static int getSubjectsKanaVocabularyMax(final SessionType type) {
        switch (type) {
            case LESSON:
                return AdvancedLesson.getSubjectsKanaVocabularyMax();
            case REVIEW:
                return AdvancedReview.getSubjectsKanaVocabularyMax();
            case SELF_STUDY:
                return AdvancedSelfStudy.getSubjectsKanaVocabularyMax();
            default:
                return -1;
        }
    }
    /**
     * Get the combined subject selection settings for a specific session type.
     *
     * @param type the session type
     * @return the combined rules
     */
    public static SubjectSelectionRules getSubjectSelectionRules(final SessionType type) {
        return new SubjectSelectionRules(
                getSubjectsCurrentMin(type), getSubjectsCurrentMax(type), getSubjectsEarlierMin(type), getSubjectsEarlierMax(type),
                getSubjectsRadicalMin(type), getSubjectsRadicalMax(type), getSubjectsKanjiMin(type), getSubjectsKanjiMax(type),
                getSubjectsVocabularyMin(type), getSubjectsVocabularyMax(type), getSubjectsKanaVocabularyMin(type), getSubjectsKanaVocabularyMax(type));
    }

    /**
     * Reset the various UI confirmations and tutorials.
     */
    public static void resetConfirmationsAndTutorials() {
        UiConfirmations.setUiConfirmAbandonSession(true);
        UiConfirmations.setUiConfirmWrapupSession(true);
        Tutorials.setKeyboardHelpDismissed(false);
        Tutorials.setBrowseOverviewDismissed(false);
        Tutorials.setSearchResultDismissed(false);
        Tutorials.setStartSelfStudyDismissed(false);
        Tutorials.setSessionLogDismissed(false);
    }

    /**
     * Test mode on or off. What this means in practice depends on temporary testing code elsewhere.
     *
     * @return true if test mode is on
     */
    public static boolean getTestMode() {
        return prefs().getBoolean("test_mode", false);
    }

    /**
     * Test mode on or off. What this means in practice depends on temporary testing code elsewhere.
     *
     * @param value true if test mode should be turned on
     */
    public static void setTestMode(final boolean value) {
        try {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("test_mode", value);
            editor.apply();
        }
        catch (final Exception e) {
            //
        }
    }

    /**
     * Api settings.
     */
    public static final class Api {
        private static final Pattern blankTrimPattern = Pattern.compile("[\\p{Z}\\s]+");

        /**
         * Private constructor.
         */
        private Api() {
            //
        }

        /**
         * The API token used to access the WaniKani API.
         *
         * @return the value
         */
        public static @Nullable String getApiKey() {
            @Nullable String key = WkApplication.getEncryptedPreferenceDataStore().getString("api_key", null);
            if (key != null) {
                key = blankTrimPattern.matcher(key).replaceAll("");
            }
            return key;
        }

        /**
         * The API token used to access the WaniKani API.
         *
         * @param apiKey the value
         */
        public static void setApiKey(final @Nullable String apiKey) {
            WkApplication.getEncryptedPreferenceDataStore().putString("api_key", apiKey);
        }

        /**
         * The rule for accessing the network (wifi only or always).
         *
         * @return the value
         */
        public static NetworkRule getNetworkRule() {
            final @Nullable String value = prefs().getString("network_rule", null);
            if (value != null) {
                try {
                    return NetworkRule.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return NetworkRule.DOWNLOAD_WIFI_ONLY;
        }

        /**
         * The preferred location where to store downloaded audio files.
         *
         * @return the value
         */
        public static String getAudioLocation() {
            return prefs().getString("audio_location", "Internal");
        }

        /**
         * Automatically trigger download of audio files for upcoming reviews
         * or the current self-study session.
         *
         * @return the value
         */
        public static boolean getAutoDownloadAudio() {
            return prefs().getBoolean("auto_download_audio", true);
        }

        /**
         * Perform sync (run ApiTasks) in the background.
         *
         * @return the value
         */
        public static boolean getEnableBackgroundSync() {
            return prefs().getBoolean("enable_background_sync", false);
        }

        /**
         * Perform sync (run ApiTasks) when the app is opened.
         *
         * @return the value
         */
        public static boolean getSyncOnOpen() {
            return prefs().getBoolean("sync_on_open", false);
        }

        /**
         * The web password used to resurrect burned subjects.
         *
         * @return the value
         */
        public static @Nullable String getWebPassword() {
            return WkApplication.getEncryptedPreferenceDataStore().getString("web_password", null);
        }
    }

    /**
     * Display settings.
     */
    public static final class Display {
        /**
         * Private constructor.
         */
        private Display() {
            //
        }

        /**
         * The user's selected theme.
         *
         * @return the value
         */
        public static ActiveTheme getTheme() {
            final @Nullable String value = prefs().getString("theme", "LIGHT");
            ActiveTheme theme = ActiveTheme.LIGHT;
            try {
                theme = ActiveTheme.valueOf(value);
            } catch (final Exception e) {
                //
            }

            assert application != null;
            int uiMode = application.getResources().getConfiguration().uiMode;
            uiMode &= Configuration.UI_MODE_NIGHT_MASK;
            if  (uiMode == Configuration.UI_MODE_NIGHT_YES) {
                try {
                    theme = ActiveTheme.valueOf(prefs().getString("nightTheme", "DARK"));
                } catch (Exception e) {
                    //
                }
            }

            return theme;
        }

        /**
         * The theme customizations for a given theme.
         *
         * @param theme the theme to customize
         * @return the value
         */
        public static List<Integer> getThemeCustomizations(final ActiveTheme theme) {
            final @Nullable String value = prefs().getString("theme_customizations_" + theme.name(), null);
            @Nullable List<Integer> list = null;
            if (value != null) {
                try {
                    list = getObjectMapper().readValue(value, new TypeReference<List<Integer>>() {});
                }
                catch (final Exception e) {
                    //
                }
            }

            if (list == null) {
                list = new ArrayList<>();
            }
            while (list.size() < NUM_THEME_CUSTOMIZATION_OPTIONS) {
                list.add(0);
            }

            return list;
        }

        /**
         * The theme customizations for a given theme.
         *
         * @param theme the theme to customize
         * @param value the value
         */
        public static void setThemeCustomizations(final ActiveTheme theme, final int[] value) {
            try {
                final SharedPreferences.Editor editor = prefs().edit();
                editor.putString("theme_customizations_" + theme.name(), getObjectMapper().writeValueAsString(value));
                editor.apply();
                theme.setDirty(true);
            }
            catch (final Exception e) {
                //
            }
        }

        /**
         * Center the caret in the quiz answer edit box.
         *
         * @return the value
         */
        public static boolean getCenterCaret() {
            return prefs().getBoolean("center_caret", false);
        }

        /**
         * Vertical size of the Anki mode buttons.
         *
         * @return the value
         */
        public static int getAnkiButtonsHeight() {
            return prefs().getInt("anki_buttons_height", 50);
        }

        /**
         * Vertical size of the main quiz question view.
         *
         * @return the value
         */
        public static int getQuizQuestionViewHeight() {
            return prefs().getInt("quiz_question_view_height", 0);
        }

        /**
         * Swap the correct/incorrect Anki mode buttons.
         *
         * @return the value
         */
        public static boolean getSwapAnkiButtons() {
            return prefs().getBoolean("swap_anki_buttons", false);
        }

        /**
         * Use slide animations for fragment transitions.
         *
         * @return the value
         */
        public static boolean getSlideAnimations() {
            return prefs().getBoolean("slide_animations", true);
        }

        /**
         * Stretch the question view to fill the screen when keyboard input is active.
         *
         * @return the value
         */
        public static boolean getStretchQuestionView() {
            return prefs().getBoolean("stretch_quiz_question_view", false);
        }
    }

    /**
     * Dashboard settings.
     */
    public static final class Dashboard {
        /**
         * Private constructor.
         */
        private Dashboard() {
            //
        }

        /**
         * Show the lesson/review breakdown on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowLessonReviewBreakdown() {
            return prefs().getBoolean("show_lesson_review_breakdown", false);
        }

        /**
         * Show the timeline bar chart on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowTimeLine() {
            return prefs().getBoolean("show_timeline", true);
        }

        /**
         * The style of the bars in the timeline bar chart.
         *
         * @return the value
         */
        public static TimeLineBarChartStyle getTimeLineChartStyle() {
            final @Nullable String value = prefs().getString("timeline_chart_style", null);
            if (value != null) {
                try {
                    return TimeLineBarChartStyle.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return TimeLineBarChartStyle.SRS_STAGE;
        }

        /**
         * Show the waterfall line for cumulative review count.
         *
         * @return the value
         */
        public static boolean getShowWaterfallLine() {
            return prefs().getBoolean("show_waterfall_line", false);
        }

        /**
         * The style of the Y-axis grid in the timeline bar chart.
         *
         * @return the value
         */
        public static TimeLineBarChartGridStyle getTimeLineChartGridStyle() {
            final @Nullable String value = prefs().getString("timeline_chart_grid_style", null);
            if (value != null) {
                try {
                    return TimeLineBarChartGridStyle.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return TimeLineBarChartGridStyle.FOR_BARS;
        }

        /**
         * The size of the timeline bar chart.
         *
         * @return the value in hours
         */
        public static int getTimeLineChartSize() {
            final String value = prefs().getString("timeline_chart_size", "24");
            try {
                return Integer.parseInt(value, 10);
            }
            catch (final Exception e) {
                return 24;
            }
        }

        /**
         * The size of the shown part of the timeline bar chart.
         *
         * @return the value in hours
         */
        public static int getTimeLineChartSizeShown() {
            final String value = prefs().getString("timeline_chart_size_shown", "24");
            try {
                return Math.min(Integer.parseInt(value, 10), getTimeLineChartSize());
            }
            catch (final Exception e) {
                return 24;
            }
        }

        /**
         * Show the SRS breakdown on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowSrsBreakdown() {
            return prefs().getBoolean("show_srs_breakdown", true);
        }

        /**
         * Show the level progression bars on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowLevelProgression() {
            return prefs().getBoolean("show_level_progression", true);
        }

        /**
         * Show the level progression bars on the dashboard for levels beyond the user's level.
         *
         * @return the value
         */
        public static boolean getShowOverLevelProgression() {
            return prefs().getBoolean("show_over_level_progression", false);
        }

        /**
         * Limit the level progression bars on the dashboard for previously complete levels.
         *
         * @return the value
         */
        public static boolean getLimitPreviousLevelProgression() {
            return prefs().getBoolean("previous_level_progression_limit", false);
        }

        /**
         * Limit the amount of levels previous to show progression bars for on the dashboard.
         *
         * @return the value
         */
        public static int getPreviousLevelProgressionLimitedAmount() {
            try {
                return Integer.parseInt(prefs().getString("previous_level_progression_limit_amount", "1"));
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        /**
         * Show the post-60 progression bar on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowPost60Progression() {
            return prefs().getBoolean("show_post60_progression", false);
        }

        /**
         * Show apprentice and guru subsections in the post-60 progression bar.
         *
         * @return the value
         */
        public static boolean getShowPost60Subsections() {
            return prefs().getBoolean("show_post60_subsections", false);
        }

        /**
         * Show the post-60 progression bar reversed.
         *
         * @return the value
         */
        public static boolean getShowPost60Reverse() {
            return prefs().getBoolean("show_post60_reverse", false);
        }

        /**
         * Show the Joyo progress on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowJoyoProgress() {
            return prefs().getBoolean("show_joyo_progress", false);
        }

        /**
         * Show the JLPT progress on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowJlptProgress() {
            return prefs().getBoolean("show_jlpt_progress", false);
        }

        /**
         * Show the recently unlocked items on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowRecentUnlocks() {
            return prefs().getBoolean("show_recent_unlocks", true);
        }

        /**
         * Show the critical condition items on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowCriticalCondition() {
            return prefs().getBoolean("show_critical_condition", true);
        }

        /**
         * Show the recently burned items on the dashboard.
         *
         * @return the value
         */
        public static boolean getShowBurnedItems() {
            return prefs().getBoolean("show_burned_items", true);
        }
    }

    /**
     * Subject info settings.
     */
    public static final class SubjectInfo {
        /**
         * Private constructor.
         */
        private SubjectInfo() {
            //
        }

        /**
         * Hide readings in lesson presentation until they are tapped in the subject info dump.
         *
         * @return the value
         */
        public static boolean getHideLessonReadings() {
            return prefs().getBoolean("hide_lesson_readings", false);
        }

        /**
         * Hide context sentence translations until they are tapped in the subject info dump.
         *
         * @return the value
         */
        public static boolean getHideSentenceTranslations() {
            return prefs().getBoolean("hide_sentence_translations", false);
        }

        /**
         * Show pitch info.
         *
         * @return the value
         */
        public static boolean getShowPitchInfo() {
            return prefs().getBoolean("show_pitch_info", false);
        }

        /**
         * Swap the 'visually similar' and 'used in' tables in the subject info dump.
         *
         * @return the value
         */
        public static boolean getSwapSimilarAndAmalgamations() {
            return prefs().getBoolean("swap_similar_amalgamations", false);
        }

        /**
         * Highlight subject type tags in mnemonic texts.
         *
         * @return the value
         */
        public static boolean getHighlightSubjectTags() {
            return prefs().getBoolean("highlight_subject_tags", true);
        }

        /**
         * Always hide WaniKani's mnemonics regardless of user notes
         *
         * @return the value
         */
        public static boolean getAlwaysHideWKMnemonic() {
            return prefs().getBoolean("always_hide_wk_mnemonic", false);
        }

        /**
         * Helper method to invert the always_hide_wk_mnemonic preference.
         * Used for enabling/disabling conditional mnemonic hiding preferences.
         *
         * @return true if mnemonics should not always be hidden
         */
        public static boolean getNotAlwaysHideWKMnemonic() {
            return !prefs().getBoolean("always_hide_wk_mnemonic", false);
        }

        /**
         * Helper method to invert the always_hide_wk_mnemonic preference.
         * Used for enabling/disabling conditional mnemonic hiding preferences.
         *
         * @param value true if mnemonics should not always be hidden
         */
        public static void setNotAlwaysHideWKMnemonic(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("not_always_hide_wk_mnemonic", value);
            editor.apply();
        }

        /**
         * Hide WaniKani's meaning mnemonic if a user note is present
         *
         * @return the value
         */
        public static boolean getHideMeaningMnemonic() {
            return prefs().getBoolean("always_hide_wk_mnemonic", false) ||
                   prefs().getBoolean("hide_meaning_mnemonic", false);
        }

        /**
         * Hide WaniKani's reading mnemonic if a user note is present
         *
         * @return the value
         */
        public static boolean getHideReadingMnemonic() {
            return prefs().getBoolean("always_hide_wk_mnemonic", false) ||
                   prefs().getBoolean("hide_reading_mnemonic", false);
        }

        /**
         * Show the legacy names and mnemonics for old radicals.
         *
         * @return the value
         */
        public static boolean getShowLegacy() {
            return prefs().getBoolean("show_legacy", false);
        }

        /**
         * The parts of the subject info to show after a correct answer with lightning mode off (meaning questions).
         *
         * @return the value
         */
        public static SubjectInfoDump getMeaningInfoDump() {
            final @Nullable String value = prefs().getString("meaning_info_dump", null);
            if (value != null) {
                try {
                    return SubjectInfoDump.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SubjectInfoDump.ANSWERS_ONLY;
        }

        /**
         * The parts of the subject info to show after a correct answer with lightning mode off (reading questions).
         *
         * @return the value
         */
        public static SubjectInfoDump getReadingInfoDump() {
            final @Nullable String value = prefs().getString("reading_info_dump", null);
            if (value != null) {
                try {
                    return SubjectInfoDump.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SubjectInfoDump.ANSWERS_ONLY;
        }

        /**
         * The parts of the subject info to show after an incorrect answer (meaning questions).
         *
         * @return the value
         */
        private static SubjectInfoDump getMeaningInfoDumpIncorrect() {
            final @Nullable String value = prefs().getString("meaning_info_dump_incorrect", null);
            if (value != null) {
                try {
                    return SubjectInfoDump.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SubjectInfoDump.ANSWERS_ONLY;
        }

        /**
         * The parts of the subject info to show after an incorrect answer (meaning questions).
         *
         * @param value the value
         */
        public static void setMeaningInfoDumpIncorrect(final SubjectInfoDump value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putString("meaning_info_dump_incorrect", value.name());
            editor.apply();
        }

        /**
         * The parts of the subject info to show after an incorrect answer (reading questions).
         *
         * @return the value
         */
        private static SubjectInfoDump getReadingInfoDumpIncorrect() {
            final @Nullable String value = prefs().getString("reading_info_dump_incorrect", null);
            if (value != null) {
                try {
                    return SubjectInfoDump.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SubjectInfoDump.ANSWERS_ONLY;
        }

        /**
         * The parts of the subject info to show after an incorrect answer (reading questions).
         *
         * @param value the value
         */
        public static void setReadingInfoDumpIncorrect(final SubjectInfoDump value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putString("reading_info_dump_incorrect", value.name());
            editor.apply();
        }

        /**
         * The parts of the subject info to show after an answer.
         *
         * @param questionType the type of the current question
         * @param correct was the answer correct?
         * @return the dump enum instance
         */
        public static SubjectInfoDump getInfoDump(final QuestionType questionType, final boolean correct) {
            if (questionType.isMeaning()) {
                return correct ? getMeaningInfoDump() : getMeaningInfoDumpIncorrect();
            }
            else {
                return correct ? getReadingInfoDump() : getReadingInfoDumpIncorrect();
            }
        }

        /**
         * Show stroke order diagrams.
         *
         * @return the value
         */
        public static boolean getShowStrokeOrder() {
            return prefs().getBoolean("show_stroke_order", false);
        }

        /**
         * Animate the stroke order diagrams.
         *
         * @return the value
         */
        public static boolean getStrokeOrderAnimated() {
            return prefs().getBoolean("stroke_order_animated", true);
        }

        /**
         * Size of the stroke order diagrams.
         *
         * @return the value
         */
        public static int getStrokeOrderSize() {
            return prefs().getInt("stroke_order_size", 250);
        }
    }

    /**
     * Audio settings.
     */
    public static final class Audio {
        /**
         * Private constructor.
         */
        private Audio() {
            //
        }

        /**
         * Autoplay audio in lesson presentation.
         *
         * @return the value
         */
        public static boolean getAutoplayLessonPresentation() {
            return prefs().getBoolean("autoplay_lesson_presentation", true);
        }

        /**
         * Autoplay audio in lesson presentation.
         *
         * @param value the value
         */
        public static void setAutoplayLessonPresentation(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("autoplay_lesson_presentation", value);
            editor.apply();
        }

        /**
         * Autoplay audio in lessons.
         *
         * @return the value
         */
        private static boolean getAutoplayLessons() {
            return prefs().getBoolean("autoplay_lessons", true);
        }

        /**
         * Autoplay audio in reviews.
         *
         * @return the value
         */
        private static boolean getAutoplayReviews() {
            return prefs().getBoolean("autoplay_reviews", true);
        }

        /**
         * Autoplay audio on Anki mode reveal.
         *
         * @return the value
         */
        public static boolean getAutoplayAnkiReveal() {
            return prefs().getBoolean("autoplay_anki_reveal", true);
        }

        /**
         * Autoplay audio in lesson presentation.
         *
         * @param value the value
         */
        public static void setAutoplayAnkiReveal(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("autoplay_anki_reveal", value);
            editor.apply();
        }

        /**
         * Preference for the voice to use for vocab audio.
         *
         * @return the value
         */
        public static VoicePreference getVoicePreference() {
            final @Nullable String value = prefs().getString("voice_preference", null);
            if (value != null) {
                try {
                    return VoicePreference.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return VoicePreference.RANDOM;
        }

        /**
         * Use audio focus for playing audio.
         *
         * @return the value
         */
        public static boolean getUseAudioFocus() {
            return prefs().getBoolean("use_audio_focus", true);
        }
    }

    /**
     * Lesson/review settings.
     */
    public static final class Review {
        /**
         * Private constructor.
         */
        private Review() {
            //
        }

        /**
         * Maximum size of a lesson session.
         *
         * @return the value
         */
        public static int gexMaxLessonSessionSize() {
            try {
                final @Nullable String value = prefs().getString("max_lesson_session_size", null);
                if (isEmpty(value)) {
                    return 5;
                }
                final int n = Integer.parseInt(value);
                if (n < 1) {
                    return 5;
                }
                return n;
            }
            catch (final Exception e) {
                return 5;
            }
        }

        /**
         * Maximum size of a review session.
         *
         * @return the value
         */
        public static int gexMaxReviewSessionSize() {
            try {
                final @Nullable String value = prefs().getString("max_review_session_size", null);
                if (isEmpty(value)) {
                    return 100;
                }
                final int n = Integer.parseInt(value);
                if (n < 1) {
                    return 100;
                }
                return n;
            }
            catch (final Exception e) {
                return 100;
            }
        }

        /**
         * Maximum size of a review session.
         *
         * @return the value
         */
        public static int getMaxSelfStudySessionSize() {
            try {
                final @Nullable String value = prefs().getString("max_self_study_session_size", null);
                if (isEmpty(value)) {
                    return 100;
                }
                final int n = Integer.parseInt(value);
                if (n < 1) {
                    return 100;
                }
                return n;
            }
            catch (final Exception e) {
                return 100;
            }
        }

        /**
         * Delay processing the results of a review session until after the session is finished.
         *
         * @return the value
         */
        public static boolean getDelayResultUpload() {
            return prefs().getBoolean("delay_result_upload", false);
        }

        /**
         * Lightning mode, i.e. auto-advance to the next question on a correct answer.
         *
         * @return the value
         */
        public static boolean getEnableLightningMode() {
            return prefs().getBoolean("enable_lightning_mode", true);
        }

        /**
         * Show a brief check mark / X animation for a correct / incorrect answer.
         *
         * @return the value
         */
        public static boolean getShowAnswerToast() {
            return prefs().getBoolean("show_answer_toast", true);
        }

        /**
         * Show the current SRS stage of an item in a quiz.
         *
         * @return the value
         */
        public static boolean getEnableSrsIndicator() {
            return prefs().getBoolean("enable_srs_indicator", false);
        }

        /**
         * Show a toast when an item changes in SRS stage.
         *
         * @return the value
         */
        public static boolean getEnableSrsToast() {
            return prefs().getBoolean("enable_srs_toast", true);
        }
        /**
         * Show a toast when an item has alternative answers
         *
         * @return the value
         */
        public static boolean getEnableAlternativesToast() {
            return prefs().getBoolean("enable_alternatives_toast", false);
        }
        /**
         * Action to perform when a meaning answer is not quite correct but acceptable within the typo lenience margin.
         *
         * @return the value
         */
        public static boolean enable_haptic_feedback_success() {
            return prefs().getBoolean("enable_haptic_feedback_success", false);
        }

        public static boolean enable_haptic_feedback_failure() {
            return prefs().getBoolean("enable_haptic_feedback_failure", false);
        }

        public static CloseEnoughAction getCloseEnoughAction() {
            final @Nullable String value = prefs().getString("close_enough_action", null);
            if (value != null) {
                try {
                    return CloseEnoughAction.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return CloseEnoughAction.SILENTLY_ACCEPT;
        }
    }

    /**
     * Keyboard settings.
     */
    public static final class Keyboard {
        /**
         * Private constructor.
         */
        private Keyboard() {
            //
        }

        /**
         * Allow auto-correct/auto-suggest in meaning input. Normally this is blocked and the input
         * is set to 'visible password' to avoid inadvertent hints being provided by Android.
         *
         * @return the value
         */
        public static boolean getEnableAutoCorrectMeaning() {
            return prefs().getBoolean("enable_auto_correct_meaning", false);
        }

        /**
         * Allow auto-correct/auto-suggest in meaning input. Normally this is blocked and the input
         * is set to 'visible password' to avoid inadvertent hints being provided by Android.
         *
         * @param value the value
         */
        public static void setEnableAutoCorrectMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("enable_auto_correct_meaning", value);
            editor.apply();
        }

        /**
         * Allow auto-correct/auto-suggest in reading input. Normally this is blocked and the input
         * is set to 'visible password' to avoid inadvertent hints being provided by Android.
         *
         * @return the value
         */
        public static boolean getEnableAutoCorrectReading() {
            return prefs().getBoolean("enable_auto_correct_reading", false);
        }

        /**
         * Allow auto-correct/auto-suggest in reading input. Normally this is blocked and the input
         * is set to 'visible password' to avoid inadvertent hints being provided by Android.
         *
         * @param value the value
         */
        public static void setEnableAutoCorrectReading(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("enable_auto_correct_reading", value);
            editor.apply();
        }

        /**
         * Force an ASCII-capable keyboard for meaning answers.
         *
         * @return the value
         */
        public static boolean getForceAsciiMeaning() {
            return prefs().getBoolean("force_ascii_meaning", false);
        }

        /**
         * Force an ASCII-capable keyboard for meaning answers.
         *
         * @param value the value
         */
        public static void setForceAsciiMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("force_ascii_meaning", value);
            editor.apply();
        }

        /**
         * Force an ASCII-capable keyboard for reading answers.
         *
         * @return the value
         */
        public static boolean getForceAsciiReading() {
            return prefs().getBoolean("force_ascii_reading", false);
        }

        /**
         * Force visual password setting for meaning answers.
         *
         * @return the value
         */
        public static boolean getForceVisiblePasswordMeaning() {
            return prefs().getBoolean("force_visible_password_meaning", false);
        }

        /**
         * Force visual password setting for meaning answers.
         *
         * @param value the value
         */
        public static void setForceVisiblePasswordMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("force_visible_password_meaning", value);
            editor.apply();
        }

        /**
         * Force visual password setting for reading answers.
         *
         * @return the value
         */
        public static boolean getForceVisiblePasswordReading() {
            return prefs().getBoolean("force_visible_password_reading", false);
        }

        /**
         * Force visual password setting for reading answers.
         *
         * @param value the value
         */
        public static void setForceVisiblePasswordReading(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("force_visible_password_reading", value);
            editor.apply();
        }

        /**
         * Set an IME language hint for meanings.
         *
         * @return the value
         */
        public static boolean getImeHintMeaning() {
            return prefs().getBoolean("ime_hint_meaning", false);
        }

        /**
         * Set an IME language hint for readings.
         *
         * @return the value
         */
        public static boolean getImeHintReading() {
            return prefs().getBoolean("ime_hint_reading", false);
        }

        /**
         * Set the "no personalized learning" option on the IME.
         *
         * @return the value
         */
        public static boolean getEnableNoLearning() {
            return prefs().getBoolean("enable_no_learning", true);
        }

        /**
         * Set the "no personalized learning" option on the IME.
         *
         * @param value the value
         */
        public static void setEnableNoLearning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("enable_no_learning", value);
            editor.apply();
        }

        /**
         * Delay in seconds to disable the next/submit button after typing an incorrect answer.
         *
         * @return the value
         */
        public static float getNextButtonDelay() {
            try {
                final @Nullable String value = prefs().getString("next_button_delay", null);
                if (isEmpty(value)) {
                    return 0;
                }
                final float f = Float.parseFloat(value);
                if (f < 1) {
                    return 0;
                }
                return f;
            }
            catch (final Exception e) {
                return 0;
            }
        }
    }

    /**
     * Font settings.
     */
    public static final class Font {
        /**
         * Private constructor.
         */
        private Font() {
            //
        }

        /**
         * Get a list of font IDs for quiz question display. Internal fonts have an int represented
         * as a string "1" .. "8", imported fonts have their file name as font ID.
         *
         * @return the value
         */
        public static List<String> getSelectedFonts() {
            final @Nullable String value = prefs().getString("selected_fonts", null);
            if (isEmpty(value)) {
                return new ArrayList<>();
            }

            return safe(ArrayList::new, () -> {
                if (value.charAt(0) == '[') {
                    return getObjectMapper().readValue(value, new TypeReference<List<String>>() {});
                }
                else {
                    final List<String> result = new ArrayList<>();
                    for (final String s: value.split(" ")) {
                        if (isEmpty(s)) {
                            continue;
                        }
                        result.add(s);
                    }
                    return result;
                }
            });
        }

        /**
         * Test to see if a font has been selected for quiz question display.
         *
         * @param fontId the ID of the font to query
         * @return the value
         */
        public static boolean isFontSelected(final String fontId) {
            return getSelectedFonts().contains(fontId);
        }

        /**
         * Select or un-select a font for quiz question display.
         *
         * @param fontId the ID of the font to adjust
         * @param selected true if this font should be included
         */
        public static void setFontSelected(final String fontId, final boolean selected) {
            final Collection<String> ids = getSelectedFonts();
            if (selected && !ids.contains(fontId)) {
                ids.add(fontId);
            }
            else if (!selected) {
                ids.remove(fontId);
            }

            final SharedPreferences.Editor editor = prefs().edit();
            safe(() -> editor.putString("selected_fonts", getObjectMapper().writeValueAsString(ids)));
            editor.apply();
        }

        /**
         * Maximum size of the font for the subject info button when shown in lesson presentation.
         *
         * @return the value
         */
        public static int getMaxFontSizeLesson() {
            return prefs().getInt("max_font_size_lesson", 128);
        }

        /**
         * Maximum size of the font for the subject info button when shown in quizzes.
         *
         * @return the value
         */
        public static int getMaxFontSizeQuiz() {
            return prefs().getInt("max_font_size_quiz", 28);
        }

        /**
         * Maximum size of the font for the subject info button when shown while browsing the subject database.
         *
         * @return the value
         */
        public static int getMaxFontSizeBrowse() {
            return prefs().getInt("max_font_size_browse", 64);
        }

        /**
         * Maximum size of the font for the quiz question text.
         *
         * @return the value
         */
        public static int getMaxFontSizeQuizText() {
            return prefs().getInt("max_font_size_quiz_text", 100);
        }

        /**
         * Maximum size of the font for the quiz question text.
         *
         * @param value the value
         */
        public static void setMaxFontSizeQuizText(final int value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putInt("max_font_size_quiz_text", value);
            editor.apply();
        }

        /**
         * Font size for the question input text box.
         *
         * @return the value
         */
        public static int getFontSizeQuestionEdit() {
            return prefs().getInt("font_size_question_edit", 14);
        }

        /**
         * Font size for the body text in subject info.
         *
         * @return the value
         */
        public static int getFontSizeSubjectInfo() {
            return prefs().getInt("font_size_subject_info", 14);
        }

        /**
         * Font size for the dashboard subject tables (critical condition, recent unlocks, recently burned).
         *
         * @return the value
         */
        public static int getFontSizeLiveSubjectTable() {
            return prefs().getInt("font_size_live_subject_table", 14);
        }

        /**
         * Font size for the Anki mode answer display.
         *
         * @return the value
         */
        public static int getFontSizeAnkiAnswer() {
            return prefs().getInt("font_size_anki_answer", 18);
        }
    }

    /**
     * Other settings.
     */
    public static final class Other {
        /**
         * Private constructor.
         */
        private Other() {
            //
        }

        /**
         * Enable notifications for new lessons and reviews.
         *
         * @return the value
         */
        public static boolean getEnableNotifications() {
            return prefs().getBoolean("enable_notifications", true);
        }

        /**
         * Notification update frequency.
         *
         * @return the value
         */
        public static NotificationUpdateFrequency getNotificationUpdateFrequency() {
            final @Nullable String value = prefs().getString("notification_update_frequency", null);
            if (value != null) {
                try {
                    return NotificationUpdateFrequency.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return NotificationUpdateFrequency.ONLY_NEW_REVIEWS;
        }

        /**
         * Notification priority.
         *
         * @return the value
         */
        public static NotificationPriority getNotificationPriority() {
            final @Nullable String value = prefs().getString("notification_priority", null);
            if (value != null) {
                try {
                    return NotificationPriority.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return NotificationPriority.DEFAULT;
        }

        /**
         * Notification priority.
         *
         * @param value the value
         */
        public static void setNotificationPriority(final NotificationPriority value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putString("notification_priority", value.name());
            editor.apply();
        }

        /**
         * Notification category.
         *
         * @return the value
         */
        public static NotificationCategory getNotificationCategory() {
            final @Nullable String value = prefs().getString("notification_category", null);
            if (value != null) {
                try {
                    return NotificationCategory.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return NotificationCategory.RECOMMENDATION;
        }

        /**
         * Trigger notifications with a low priority, i.e. silently.
         *
         * @return the value
         */
        public static boolean getNotificationLowPriority() {
            return prefs().getBoolean("notification_low_priority", false);
        }

        /**
         * Show on'yomi readings in katakana instead of hiragana.
         *
         * @return the value
         */
        public static boolean getShowOnInKatakana() {
            return prefs().getBoolean("show_onyomi_in_katakana", false);
        }

        /**
         * When asking for an on'yomi answer, require the answer to be given in katakana instead of hiragana.
         *
         * @return the value
         */
        public static boolean getRequireOnInKatakana() {
            return prefs().getBoolean("require_onyomi_in_katakana", false);
        }

        /**
         * Is there a search engine configured for the given slot?
         *
         * @param index the search engine slot 1..5
         * @return true if a name and URL are configured
         */
        public static boolean hasSearchEngine(final int index) {
            return !isEmpty(getSearchEngineName(index)) && !isEmpty(getSearchEngineUrl(index));
        }

        /**
         * The name for the search engine in the given slot.
         *
         * @param index the search engine slot 1..5
         * @return the name, or an empty string if not configured
         */
        public static String getSearchEngineName(final int index) {
            final @Nullable String value = prefs().getString("search_engine_" + index + "_tag", null);
            if (value != null) {
                return value;
            }
            if (index == 1) {
                return "Wiktionary";
            }
            if (index == 2) {
                return "Jisho";
            }
            return "";
        }

        /**
         * The URL for the search engine in the given slot. In the URL, "%s" is a placeholder for the search query.
         *
         * @param index the search engine slot 1..5
         * @return the URL, or an empty string if not configured
         */
        public static String getSearchEngineUrl(final int index) {
            final @Nullable String value = prefs().getString("search_engine_" + index, null);
            if (value != null) {
                return value;
            }
            if (index == 1) {
                return "https://ja.wiktionary.org/w/index.php?search=%s";
            }
            if (index == 2) {
                return "https://jisho.org/search/%s";
            }
            return "";
        }

        /**
         * Show star ratings for subjects.
         *
         * @return the value
         */
        public static boolean getEnableStarRatings() {
            return prefs().getBoolean("enable_star_ratings", false);
        }
    }

    /**
     * Experimental settings.
     */
    public static final class Experimental {
        /**
         * Private constructor.
         */
        private Experimental() {
            //
        }

        /**
         * Subject card layout for search results.
         *
         * @return the value
         */
        @SuppressWarnings("SameReturnValue")
        public static SubjectCardLayout getSubjectCardLayoutSearch() {
            return SubjectCardLayout.NORMAL;
        }

        /**
         * Subject card layout for grid layouts.
         *
         * @return the value
         */
        @SuppressWarnings("SameReturnValue")
        public static SubjectCardLayout getSubjectCardLayoutOther() {
            return SubjectCardLayout.NORMAL;
        }
    }

    /**
     * Lesson advanced settings.
     */
    public static final class AdvancedLesson {
        /**
         * Private constructor.
         */
        private AdvancedLesson() {
            //
        }

        /**
         * Order of items in lesson sessions.
         *
         * @return the value
         */
        public static LessonOrder getOrder() {
            if (!getAdvancedEnabled()) {
                return LessonOrder.LEVEL_THEN_TYPE;
            }
            final @Nullable String value = prefs().getString("lesson_order", null);
            if (value != null) {
                try {
                    return LessonOrder.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return LessonOrder.LEVEL_THEN_TYPE;
        }

        /**
         * Reverse order of items in lesson sessions.
         *
         * @return the value
         */
        public static boolean getOrderReversed() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("lesson_order_reversed", false);
        }

        /**
         * Select items to prioritize over others in lesson sessions.
         *
         * @return the value
         */
        public static SessionPriority getOrderPriority() {
            if (!getAdvancedEnabled()) {
                return SessionPriority.NONE;
            }
            final @Nullable String value = prefs().getString("lesson_order_priority", null);
            if (value != null) {
                try {
                    return SessionPriority.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SessionPriority.NONE;
        }

        /**
         * Shuffle session items after the items have been selected for the session.
         *
         * @return the value
         */
        private static boolean getShuffleAfterSelection() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("lesson_shuffle_after_selection", false);
        }

        /**
         * Run meaning and reading questions back-to-back for each item in lesson sessions.
         *
         * @return the value
         */
        public static boolean getBackToBack() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("lesson_back_to_back", false);
        }

        /**
         * Always quiz reading before meaning for each item in lesson sessions.
         *
         * @return the value
         */
        public static boolean getReadingFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("lesson_reading_first", false);
        }

        /**
         * Always quiz meaning before reading for each item in lesson sessions.
         *
         * @return the value
         */
        public static boolean getMeaningFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("lesson_meaning_first", false);
        }

        /**
         * Use Anki mode for lesson sessions.
         *
         * @return the value
         */
        public static boolean getAnkiMode() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_lesson", false);
        }

        /**
         * Use Anki mode for meanings in lesson sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeMeaning() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_lesson_meaning", false);
        }

        /**
         * Use Anki mode for meanings in lesson sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_lesson_meaning", value);
            editor.apply();
        }

        /**
         * Use Anki mode for readings in lesson sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeReading() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_lesson_reading", false);
        }

        /**
         * Use Anki mode for readings in lesson sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeReading(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_lesson_reading", value);
            editor.apply();
        }

        /**
         * Subject selection: min of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_current_min", -1);
        }

        /**
         * Subject selection: max of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_current_max", -1);
        }

        /**
         * Subject selection: min of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_earlier_min", -1);
        }

        /**
         * Subject selection: max of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_earlier_max", -1);
        }

        /**
         * Subject selection: min of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_radical_min", -1);
        }

        /**
         * Subject selection: max of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_radical_max", -1);
        }

        /**
         * Subject selection: min of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_kanji_min", -1);
        }

        /**
         * Subject selection: max of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_kanji_max", -1);
        }

        /**
         * Subject selection: min of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_vocabulary_max", -1);
        }
        /**
         * Subject selection: min of kana vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_Kana_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("lesson_subjects_kana_vocabulary_max", -1);
        }
    }

    /**
     * Review advanced settings.
     */
    public static final class AdvancedReview {
        /**
         * Private constructor.
         */
        private AdvancedReview() {
            //
        }

        /**
         * Order of items in review sessions.
         *
         * @return the value
         */
        public static ReviewOrder getOrder() {
            if (!getAdvancedEnabled()) {
                return ReviewOrder.SHUFFLE;
            }
            final @Nullable String value = prefs().getString("review_order", null);
            if (value != null) {
                try {
                    return ReviewOrder.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return ReviewOrder.SHUFFLE;
        }

        /**
         * Reverse order of items in review sessions.
         *
         * @return the value
         */
        public static boolean getOrderReversed() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_order_reversed", false);
        }

        /**
         * Select items to prioritize over others in review sessions.
         *
         * @return the value
         */
        public static SessionPriority getOrderPriority() {
            if (!getAdvancedEnabled()) {
                return SessionPriority.NONE;
            }
            final @Nullable String value = prefs().getString("review_order_priority", null);
            if (value != null) {
                try {
                    return SessionPriority.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SessionPriority.NONE;
        }

        /**
         * Quiz overdue items first in review sessions. The session priority setting takes precedence over this.
         *
         * @return the value
         */
        public static boolean getOrderOverdueFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_order_overdue_first", false);
        }

        /**
         * Shuffle session items after the items have been selected for the session.
         *
         * @return the value
         */
        private static boolean getShuffleAfterSelection() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_shuffle_after_selection", false);
        }

        /**
         * Run meaning and reading questions back-to-back for each item in review sessions.
         *
         * @return the value
         */
        public static boolean getBackToBack() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_back_to_back", false);
        }

        /**
         * Always quiz reading before meaning for each item in review sessions.
         *
         * @return the value
         */
        public static boolean getReadingFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_reading_first", false);
        }

        /**
         * Always quiz meaning before reading for each item in review sessions.
         *
         * @return the value
         */
        public static boolean getMeaningFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_meaning_first", false);
        }

        /**
         * Use Anki mode for review sessions.
         *
         * @return the value
         */
        public static boolean getAnkiMode() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_review", false);
        }

        /**
         * Use Anki mode for meanings in review sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeMeaning() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_review_meaning", false);
        }

        /**
         * Use Anki mode for meanings in review sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_review_meaning", value);
            editor.apply();
        }

        /**
         * Use Anki mode for readings in review sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeReading() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_review_reading", false);
        }

        /**
         * Use Anki mode for readings in review sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeReading(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_review_reading", value);
            editor.apply();
        }

        /**
         * Subject selection: min of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_current_min", -1);
        }

        /**
         * Subject selection: max of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_current_max", -1);
        }

        /**
         * Subject selection: min of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_earlier_min", -1);
        }

        /**
         * Subject selection: max of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_earlier_max", -1);
        }

        /**
         * Subject selection: min of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_radical_min", -1);
        }

        /**
         * Subject selection: max of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_radical_max", -1);
        }

        /**
         * Subject selection: min of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_kanji_min", -1);
        }

        /**
         * Subject selection: max of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_kanji_max", -1);
        }

        /**
         * Subject selection: min of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_vocabulary_max", -1);
        }
        /**
         * Subject selection: min of kana vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_kana_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of kana vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("review_subjects_kana_vocabulary_max", -1);
        }

        /**
         * Randomize inflections in review sessions.
         *
         * @return the value
         */
        private static boolean getRandomizeInflections() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("review_randomize_inflections", false);
        }

        /**
         * Randomize inflections in review sessions.
         *
         * @param value the value
         */
        private static void setRandomizeInflections(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("review_randomize_inflections", value);
            editor.apply();
        }
    }

    /**
     * Self-study advanced settings.
     */
    public static final class AdvancedSelfStudy {
        /**
         * Private constructor.
         */
        private AdvancedSelfStudy() {
            //
        }

        /**
         * Order of items in self-study sessions.
         *
         * @return the value
         */
        public static ReviewOrder getOrder() {
            if (!getAdvancedEnabled()) {
                return ReviewOrder.SHUFFLE;
            }
            final @Nullable String value = prefs().getString("self_study_order", null);
            if (value != null) {
                try {
                    return ReviewOrder.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return ReviewOrder.SHUFFLE;
        }

        /**
         * Reverse order of items in self-study sessions.
         *
         * @return the value
         */
        public static boolean getOrderReversed() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_order_reversed", false);
        }

        /**
         * Select items to prioritize over others in self-study sessions.
         *
         * @return the value
         */
        public static SessionPriority getOrderPriority() {
            if (!getAdvancedEnabled()) {
                return SessionPriority.NONE;
            }
            final @Nullable String value = prefs().getString("self_study_order_priority", null);
            if (value != null) {
                try {
                    return SessionPriority.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SessionPriority.NONE;
        }

        /**
         * Quiz overdue items first in self-study sessions. The session priority setting takes precedence over this.
         *
         * @return the value
         */
        public static boolean getOrderOverdueFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_order_overdue_first", false);
        }

        /**
         * Shuffle session items after the items have been selected for the session.
         *
         * @return the value
         */
        private static boolean getShuffleAfterSelection() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_shuffle_after_selection", false);
        }

        /**
         * Run meaning and reading questions back-to-back for each item in self-study sessions.
         *
         * @return the value
         */
        public static boolean getBackToBack() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_back_to_back", false);
        }

        /**
         * Always quiz reading before meaning for each item in self-study sessions.
         *
         * @return the value
         */
        public static boolean getReadingFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_reading_first", false);
        }

        /**
         * Always quiz meaning before reading for each item in self-study sessions.
         *
         * @return the value
         */
        public static boolean getMeaningFirst() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_meaning_first", false);
        }

        /**
         * Use Anki mode for self-study sessions.
         *
         * @return the value
         */
        public static boolean getAnkiMode() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_self_study", false);
        }

        /**
         * Use Anki mode for meanings in self_study sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeMeaning() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_self_study_meaning", false);
        }

        /**
         * Use Anki mode for meanings in self_study sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeMeaning(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_self_study_meaning", value);
            editor.apply();
        }

        /**
         * Use Anki mode for readings in self_study sessions.
         *
         * @return the value
         */
        private static boolean getAnkiModeReading() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("anki_mode_self_study_reading", false);
        }

        /**
         * Use Anki mode for readings in self_study sessions.
         *
         * @param value the value
         */
        public static void setAnkiModeReading(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("anki_mode_self_study_reading", value);
            editor.apply();
        }

        /**
         * Subject selection: min of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_current_min", -1);
        }

        /**
         * Subject selection: max of current-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsCurrentMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_current_max", -1);
        }

        /**
         * Subject selection: min of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_earlier_min", -1);
        }

        /**
         * Subject selection: max of earlier-level subjects.
         *
         * @return the value
         */
        private static int getSubjectsEarlierMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_earlier_max", -1);
        }

        /**
         * Subject selection: min of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_radical_min", -1);
        }

        /**
         * Subject selection: max of radical subjects.
         *
         * @return the value
         */
        private static int getSubjectsRadicalMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_radical_max", -1);
        }

        /**
         * Subject selection: min of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_kanji_min", -1);
        }

        /**
         * Subject selection: max of kanji subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanjiMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_kanji_max", -1);
        }

        /**
         * Subject selection: min of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_vocabulary_max", -1);
        }
        /**
         * Subject selection: min of kana vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMin() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_kana_vocabulary_min", -1);
        }

        /**
         * Subject selection: max of vocabulary subjects.
         *
         * @return the value
         */
        private static int getSubjectsKanaVocabularyMax() {
            if (!getAdvancedEnabled()) {
                return -1;
            }
            return prefs().getInt("self_study_subjects_kana_vocabulary_max", -1);
        }

        /**
         * Randomize inflections in self-study sessions.
         *
         * @return the value
         */
        private static boolean getRandomizeInflections() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("self_study_randomize_inflections", false);
        }

        /**
         * Randomize inflections in self-study sessions.
         *
         * @param value the value
         */
        private static void setRandomizeInflections(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("self_study_randomize_inflections", value);
            editor.apply();
        }
    }

    /**
     * Other advanced settings.
     */
    public static final class AdvancedOther {
        /**
         * Private constructor.
         */
        private AdvancedOther() {
            //
        }

        /**
         * Quiz on'yomi and kun'yomi separately for kanji items.
         *
         * @return the value
         */
        public static boolean getKanjiModeOnKun() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("kanji_mode_onkun", false);
        }

        /**
         * Threshold for when to consider an item to be overdue.
         *
         * @return the value
         */
        public static float getOverdueThreshold() {
            if (!getAdvancedEnabled()) {
                return DEFAULT_OVERDUE_THRESHOLD;
            }
            try {
                final @Nullable String value = prefs().getString("overdue_threshold", null);
                if (isEmpty(value)) {
                    return DEFAULT_OVERDUE_THRESHOLD;
                }
                final int n = Integer.parseInt(value);
                if (n < 1) {
                    return DEFAULT_OVERDUE_THRESHOLD;
                }
                return n / 100.0f;
            }
            catch (final Exception e) {
                return DEFAULT_OVERDUE_THRESHOLD;
            }
        }

        /**
         * The action to perform when the special button 1 is pressed.
         *
         * @return the value
         */
        public static SpecialButtonBehavior getSpecialButton1Behavior() {
            if (!getAdvancedEnabled()) {
                return SpecialButtonBehavior.DISABLE;
            }
            final @Nullable String value = prefs().getString("undo_button", null);
            if (value != null) {
                try {
                    return SpecialButtonBehavior.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SpecialButtonBehavior.DISABLE;
        }

        /**
         * The action to perform when the special button 2 is pressed.
         *
         * @return the value
         */
        public static SpecialButtonBehavior getSpecialButton2Behavior() {
            if (!getAdvancedEnabled()) {
                return SpecialButtonBehavior.DISABLE;
            }
            final @Nullable String value = prefs().getString("ignore_button", null);
            if (value != null) {
                try {
                    return SpecialButtonBehavior.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SpecialButtonBehavior.DISABLE;
        }

        /**
         * The action to perform when the special button 3 is pressed.
         *
         * @return the value
         */
        public static SpecialButtonBehavior getSpecialButton3Behavior() {
            if (!getAdvancedEnabled()) {
                return SpecialButtonBehavior.DISABLE;
            }
            final @Nullable String value = prefs().getString("special_button_3", null);
            if (value != null) {
                try {
                    return SpecialButtonBehavior.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return SpecialButtonBehavior.DISABLE;
        }

        /**
         * Whether to show the don't know button or not.
         *
         * @return the value
         */
        public static DontKnowButtonBehavior getDontKnowButtonBehavior() {
            if (!getAdvancedEnabled()) {
                return DontKnowButtonBehavior.ONLY_IF_IGNORE_HIDDEN;
            }
            final @Nullable String value = prefs().getString("dont_know_button", null);
            if (value != null) {
                try {
                    return DontKnowButtonBehavior.valueOf(value);
                }
                catch (final Exception e) {
                    //
                }
            }
            return DontKnowButtonBehavior.ONLY_IF_IGNORE_HIDDEN;
        }

        /**
         * Shake-and-retry if a reading answer for a single-kanji vocab is incorrect, but
         * the supplied answer is a known reading for the kanji that vocab is based on.
         *
         * @return the value
         */
        public static boolean getShakeOnMatchingKanji() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("shake_on_matching_kanji", true);
        }

        /**
         * Show the full, unlimited session log, which may contain inappropriate hints or answers.
         *
         * @return the value
         */
        public static boolean getFullSessionLog() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("full_session_log", false);
        }

        /**
         * For a kanji reading question, indicate if the question requires an on'yomi or kun'yomi answer.
         *
         * @return the value
         */
        public static boolean getIndicateKanjiReadingType() {
            if (!getAdvancedEnabled()) {
                return false;
            }
            return prefs().getBoolean("indicate_kanji_reading_type", false);
        }
    }

    /**
     * Options to register dismissed UI confirmations.
     */
    public static final class UiConfirmations {
        private UiConfirmations() {
            //
        }

        /**
         * Ask for UI confirmation before abandoning a session.
         *
         * @return the value
         */
        public static boolean getUiConfirmAbandonSession() {
            return prefs().getBoolean("ui_confirm_abandon_session", true);
        }

        /**
         * Ask for UI confirmation before abandoning a session.
         *
         * @param value the value
         */
        public static void setUiConfirmAbandonSession(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("ui_confirm_abandon_session", value);
            editor.apply();
        }

        /**
         * Ask for UI confirmation before wrapping up a session.
         *
         * @return the value
         */
        public static boolean getUiConfirmWrapupSession() {
            return prefs().getBoolean("ui_confirm_wrapup_session", true);
        }

        /**
         * Ask for UI confirmation before wrapping up a session.
         *
         * @param value the value
         */
        public static void setUiConfirmWrapupSession(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("ui_confirm_wrapup_session", value);
            editor.apply();
        }
    }

    /**
     * Options to register dismissed tutorials.
     */
    public static final class Tutorials {
        private Tutorials() {
            //
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @return the value
         */
        public static boolean getKeyboardHelpDismissed() {
            return prefs().getBoolean("keyboard_help_dismissed", false);
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @param value the value
         */
        public static void setKeyboardHelpDismissed(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("keyboard_help_dismissed", value);
            editor.apply();
        }

        /**
         * Dismiss the tutorial on the browse overview screen.
         *
         * @return the value
         */
        public static boolean getBrowseOverviewDismissed() {
            return prefs().getBoolean("browse_overview_dismissed", false);
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @param value the value
         */
        public static void setBrowseOverviewDismissed(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("browse_overview_dismissed", value);
            editor.apply();
        }

        /**
         * Dismiss the tutorial on the browse overview screen.
         *
         * @return the value
         */
        public static boolean getSearchResultDismissed() {
            return prefs().getBoolean("search_result_dismissed", false);
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @param value the value
         */
        public static void setSearchResultDismissed(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("search_result_dismissed", value);
            editor.apply();
        }

        /**
         * Dismiss the tutorial on the browse overview screen.
         *
         * @return the value
         */
        public static boolean getStartSelfStudyDismissed() {
            return prefs().getBoolean("start_self_study_dismissed", false);
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @param value the value
         */
        public static void setStartSelfStudyDismissed(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("start_self_study_dismissed", value);
            editor.apply();
        }

        /**
         * Dismiss the tutorial on the browse overview screen.
         *
         * @return the value
         */
        public static boolean getSessionLogDismissed() {
            return prefs().getBoolean("session_log_dismissed", false);
        }

        /**
         * Dismiss the dashboard keyboard help popup.
         *
         * @param value the value
         */
        public static void setSessionLogDismissed(final boolean value) {
            final SharedPreferences.Editor editor = prefs().edit();
            editor.putBoolean("session_log_dismissed", value);
            editor.apply();
        }
    }
}
