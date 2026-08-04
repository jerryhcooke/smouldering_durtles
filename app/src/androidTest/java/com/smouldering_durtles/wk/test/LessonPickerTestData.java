package com.smouldering_durtles.wk.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.preference.PreferenceManager;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.model.ApiAssignment;
import com.smouldering_durtles.wk.api.model.ApiSubject;
import com.smouldering_durtles.wk.api.model.Meaning;
import com.smouldering_durtles.wk.api.model.Reading;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.dao.SubjectSyncDao;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.util.AsyncTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Controlled test-app data for the lesson picker instrumentation tests.
 */
public final class LessonPickerTestData {
    public static final long RADICAL_LEVEL_2 = 9_100_001L;
    public static final long KANJI_LEVEL_1 = 9_100_002L;
    public static final long VOCABULARY_LEVEL_2 = 9_100_003L;
    public static final long KANA_VOCABULARY_LEVEL_1 = 9_100_004L;
    private static final long DEFAULT_SUBJECT_ID_MIN = RADICAL_LEVEL_2;
    private static final long DEFAULT_SUBJECT_ID_MAX = KANA_VOCABULARY_LEVEL_1;
    private static final long LONG_LIST_SUBJECT_ID_MIN = 9_200_000L;
    private static final long LONG_LIST_SUBJECT_ID_MAX = 9_200_029L;
    private static final String TEST_API_KEY = "lesson-picker-test-placeholder";
    private static final String[] CHANGED_PREFERENCE_KEYS = {
            "first_time_setup",
            "max_lesson_session_size",
            "enable_advanced",
            "lesson_picker_enabled",
            "lesson_order",
            "lesson_order_reversed",
            "lesson_order_priority",
            "auto_download_audio",
            "show_pitch_info",
            "sync_on_open",
            "notification_permission_asked"
    };
    private static final long UNLOCKED_AT = 1_000L;

    private LessonPickerTestData() {
        //
    }

    /**
     * Prepare a verified dedicated test profile without accessing the WaniKani API.
     *
     * @return the state that must be restored after the test
     */
    public static ProfileState reset() {
        waitForSessionLoad();

        final AppDatabase db = WkApplication.getDatabase();
        final Context context = ApplicationProvider.getApplicationContext();
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        assertSafeBeforeSetup(db);
        final ProfileState state = new ProfileState(
                GlobalSettings.Api.getApiKey(),
                db.propertiesDao().getSessionType(),
                db.propertiesDao().getUserLevel(),
                preferences);

        deleteControlledRows(db);
        preferences.edit()
                .putInt("first_time_setup", 1)
                .putString("max_lesson_session_size", "2")
                .putBoolean("enable_advanced", false)
                .putBoolean("lesson_picker_enabled", false)
                .putString("lesson_order", "LEVEL_THEN_TYPE")
                .putBoolean("lesson_order_reversed", false)
                .putString("lesson_order_priority", "NONE")
                .putBoolean("auto_download_audio", false)
                .putBoolean("show_pitch_info", false)
                .putBoolean("sync_on_open", false)
                .putBoolean("notification_permission_asked", true)
                .apply();
        GlobalSettings.Api.setApiKey(TEST_API_KEY);
        return state;
    }

    /**
     * Wait until all previously queued serial app tasks have delivered their callbacks.
     */
    public static void waitForSerialAsyncTasks() {
        final CountDownLatch finished = new CountDownLatch(1);
        new AsyncTask<Void>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                finished.countDown();
            }

            @Override
            protected void onProgressUpdate(final Object[] values) {
                //
            }
        }.execute();
        try {
            assertTrue("Serial async work did not finish",
                    finished.await(5, TimeUnit.SECONDS));
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    /**
     * Remove only controlled fixture data and restore state captured by {@link #reset()}.
     *
     * @param state the state captured before test setup
     */
    public static void cleanUp(final ProfileState state) {
        waitForSessionLoad();

        final AppDatabase db = WkApplication.getDatabase();
        assertOwnedDuringCleanup(db);

        Session.getInstance().reset();
        deleteControlledRows(db);
        db.propertiesDao().setSessionType(state.sessionType);
        db.propertiesDao().setUserLevel(state.userLevel);
        state.restorePreferences();
        GlobalSettings.Api.setApiKey(state.apiKey);
    }

    /**
     * Remove the currently controlled subject fixtures while retaining the test profile.
     */
    public static void clearControlledSubjects() {
        final AppDatabase db = WkApplication.getDatabase();
        assertOwnedDuringCleanup(db);
        deleteControlledSubjects(db);
    }

    /**
     * Insert the mixed set used by most picker tests.
     */
    public static void insertDefaultSubjects() {
        insertAvailable(RADICAL_LEVEL_2, "radical", "⼭", "mountain", 2, 1);
        insertAvailable(KANJI_LEVEL_1, "kanji", "川", "river", 1, 2);
        insertAvailable(VOCABULARY_LEVEL_2, "vocabulary", "火山", "volcano", 2, 3);
        insertAvailable(KANA_VOCABULARY_LEVEL_1, "kana_vocabulary", "こんにちは", "hello", 1, 4);
    }

    /**
     * Insert one currently available lesson subject.
     *
     * @param id subject ID
     * @param object WaniKani subject object name
     * @param characters displayed subject characters
     * @param primaryMeaning primary meaning
     * @param level WaniKani level
     * @param lessonPosition stable lesson position
     */
    public static void insertAvailable(final long id,
                                       final String object,
                                       final String characters,
                                       final String primaryMeaning,
                                       final int level,
                                       final int lessonPosition) {
        final Meaning meaning = new Meaning();
        meaning.setMeaning(primaryMeaning);
        meaning.setPrimary(true);
        meaning.setAcceptedAnswer(true);

        final ApiSubject subject = new ApiSubject();
        subject.setId(id);
        subject.setObject(object);
        subject.setCharacters(characters);
        subject.setSlug(primaryMeaning);
        subject.setLevel(level);
        subject.setLessonPosition(lessonPosition);
        subject.setSrsSystemId(1);
        subject.setMeanings(Collections.singletonList(meaning));
        if (!"radical".equals(object)) {
            final Reading reading = new Reading();
            reading.setReading("かな");
            reading.setPrimary(true);
            reading.setAcceptedAnswer(true);
            reading.setType("kanji".equals(object) ? "onyomi" : null);
            subject.setReadings(Collections.singletonList(reading));
        }

        final SubjectSyncDao dao = WkApplication.getDatabase().subjectSyncDao();
        dao.insertOrUpdate(subject, Collections.emptySet());
        dao.insertOrUpdateAssignment(assignment(id, 0));
    }

    /**
     * Make a fixture subject no longer available for lessons.
     *
     * @param subjectId subject ID
     */
    public static void makeUnavailable(final long subjectId) {
        WkApplication.getDatabase().subjectSyncDao()
                .insertOrUpdateAssignment(assignment(subjectId, 2_000L));
    }

    /**
     * Read a stable fingerprint of fields cancellation must not mutate.
     *
     * @return a stable assignment fingerprint
     */
    public static String assignmentFingerprint() {
        final StringBuilder result = new StringBuilder();
        for (final long id: new long[] {
                RADICAL_LEVEL_2,
                KANJI_LEVEL_1,
                VOCABULARY_LEVEL_2,
                KANA_VOCABULARY_LEVEL_1
        }) {
            final Subject subject = WkApplication.getDatabase().subjectDao().getById(id);
            if (subject != null) {
                result.append(String.format(Locale.ROOT, "%d:%d:%d:%d:%d:%d:%b;",
                        subject.getId(),
                        subject.getAssignmentId(),
                        subject.getUnlockedAt(),
                        subject.getStartedAt(),
                        subject.getAvailableAt(),
                        subject.getBurnedAt(),
                        subject.isAssignmentPatched()));
            }
        }
        return result.toString();
    }

    private static ApiAssignment assignment(final long subjectId, final long startedAt) {
        final ApiAssignment assignment = new ApiAssignment();
        assignment.setId(subjectId + 1_000_000L);
        assignment.setSubjectId(subjectId);
        assignment.setUnlockedAt(UNLOCKED_AT);
        assignment.setStartedAt(startedAt);
        assignment.setSrsStageId(0);
        return assignment;
    }

    private static void waitForSessionLoad() {
        final long deadline = System.currentTimeMillis() + 5_000L;
        while (!Session.getInstance().isLoaded() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(20L);
            }
            catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertTrue("The test application session did not finish loading", Session.getInstance().isLoaded());
    }

    private static void assertSafeBeforeSetup(final AppDatabase db) {
        assertSafeApiKey();
        assertTrue("Lesson picker tests require a profile without a user ID",
                db.propertiesDao().getUserId() == null);
        assertTrue("Lesson picker tests require a profile without a username",
                db.propertiesDao().getUsername() == null);
        assertTrue("Lesson picker tests require no active in-memory session",
                Session.getInstance().isInactive());
        assertTrue("Lesson picker tests require no persisted session items",
                db.sessionItemDao().getAll().isEmpty());
        assertTrue("Lesson picker tests require an inactive persisted session",
                db.propertiesDao().getSessionType() == SessionType.NONE);
        assertOnlyControlledSubjectIds(db);
    }

    private static void assertOwnedDuringCleanup(final AppDatabase db) {
        assertSafeApiKey();
        assertTrue("Lesson picker tests require a profile without a user ID",
                db.propertiesDao().getUserId() == null);
        assertTrue("Lesson picker tests require a profile without a username",
                db.propertiesDao().getUsername() == null);
        assertOnlyControlledSubjectIds(db);
        assertOnlyControlledSessionItemIds(db);
        if (!Session.getInstance().isInactive()) {
            assertTrue("Active session is not owned by lesson picker fixtures",
                    !db.sessionItemDao().getAll().isEmpty());
        }
    }

    private static void assertSafeApiKey() {
        final String apiKey = GlobalSettings.Api.getApiKey();
        assertTrue("Lesson picker tests refuse to overwrite an existing API key",
                apiKey == null || TEST_API_KEY.equals(apiKey));
    }

    private static void assertOnlyControlledSubjectIds(final AppDatabase db) {
        final SupportSQLiteDatabase sql = db.getOpenHelper().getReadableDatabase();
        try (Cursor cursor = sql.query("SELECT id FROM subject")) {
            while (cursor.moveToNext()) {
                assertTrue("Lesson picker tests refuse to delete uncontrolled subjects",
                        isControlledSubjectId(cursor.getLong(0)));
            }
        }
    }

    private static void assertOnlyControlledSessionItemIds(final AppDatabase db) {
        for (final com.smouldering_durtles.wk.db.model.SessionItem item:
                db.sessionItemDao().getAll()) {
            assertTrue("Lesson picker tests refuse to delete uncontrolled session items",
                    isControlledSubjectId(item.getId()));
        }
    }

    private static boolean isControlledSubjectId(final long subjectId) {
        return subjectId >= DEFAULT_SUBJECT_ID_MIN
                && subjectId <= DEFAULT_SUBJECT_ID_MAX
                || subjectId >= LONG_LIST_SUBJECT_ID_MIN
                && subjectId <= LONG_LIST_SUBJECT_ID_MAX;
    }

    private static void deleteControlledRows(final AppDatabase db) {
        final SupportSQLiteDatabase sql = db.getOpenHelper().getWritableDatabase();
        deleteControlledIds(sql, "session_item");
        deleteControlledSubjects(db);
    }

    private static void deleteControlledSubjects(final AppDatabase db) {
        deleteControlledIds(db.getOpenHelper().getWritableDatabase(), "subject");
    }

    private static void deleteControlledIds(final SupportSQLiteDatabase db,
                                            final String table) {
        try (Cursor cursor = db.query("SELECT id FROM " + table)) {
            while (cursor.moveToNext()) {
                final long subjectId = cursor.getLong(0);
                if (isControlledSubjectId(subjectId)) {
                    db.execSQL("DELETE FROM " + table + " WHERE id = ?",
                            new Object[] {subjectId});
                }
            }
        }
    }

    /**
     * State from the installed debug profile that test setup temporarily changes.
     */
    public static final class ProfileState {
        private final String apiKey;
        private final SessionType sessionType;
        private final int userLevel;
        private final SharedPreferences preferences;
        private final Map<String, Object> preferenceValues = new HashMap<>();

        private ProfileState(final String apiKey,
                             final SessionType sessionType,
                             final int userLevel,
                             final SharedPreferences preferences) {
            this.apiKey = apiKey;
            this.sessionType = sessionType;
            this.userLevel = userLevel;
            this.preferences = preferences;
            final Map<String, ?> allPreferences = preferences.getAll();
            for (final String key: CHANGED_PREFERENCE_KEYS) {
                if (allPreferences.containsKey(key)) {
                    preferenceValues.put(key, allPreferences.get(key));
                }
            }
        }

        private void restorePreferences() {
            final SharedPreferences.Editor editor = preferences.edit();
            for (final String key: CHANGED_PREFERENCE_KEYS) {
                editor.remove(key);
            }
            for (final Map.Entry<String, Object> entry: preferenceValues.entrySet()) {
                if (entry.getValue() instanceof String) {
                    editor.putString(entry.getKey(), (String) entry.getValue());
                }
                else if (entry.getValue() instanceof Boolean) {
                    editor.putBoolean(entry.getKey(), (Boolean) entry.getValue());
                }
                else if (entry.getValue() instanceof Integer) {
                    editor.putInt(entry.getKey(), (Integer) entry.getValue());
                }
                else {
                    throw new AssertionError("Unsupported saved preference type");
                }
            }
            editor.apply();
        }
    }
}
