package com.smouldering_durtles.wk.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.LessonPickerActivity;
import com.smouldering_durtles.wk.activities.MainActivity;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.model.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation coverage for lesson picker actions on the dashboard.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class LessonPickerDashboardTest {
    private LessonPickerTestData.ProfileState profileState = null;

    @Before
    public void setUp() {
        profileState = LessonPickerTestData.reset();
        LessonPickerTestData.insertDefaultSubjects();
    }

    @After
    public void tearDown() {
        if (profileState != null) {
            LessonPickerTestData.cleanUp(profileState);
        }
    }

    @Test
    public void pickerArrowRequiresBothAdvancedPreferences() {
        setPickerPreferences(false, true);
        assertFalse(GlobalSettings.AdvancedLesson.getLessonPickerEnabled());
        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            waitForVisibility(scenario, R.id.startLessonsButton, View.VISIBLE);
            assertVisibility(scenario, R.id.startLessonsMenuButton, View.GONE);

            setPickerPreferences(true, false);
            assertFalse(GlobalSettings.AdvancedLesson.getLessonPickerEnabled());
            pingTimeline();
            assertVisibility(scenario, R.id.startLessonsMenuButton, View.GONE);

            setPickerPreferences(true, true);
            assertTrue(GlobalSettings.AdvancedLesson.getLessonPickerEnabled());
            pingTimeline();
            waitForVisibility(scenario, R.id.startLessonsMenuButton, View.VISIBLE);
        }
    }

    @Test
    public void pickerArrowIsVisibleOnlyForAvailableInactiveLessons() {
        setPickerPreferences(true, true);
        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            waitForVisibility(scenario, R.id.startLessonsMenuButton, View.VISIBLE);
            scenario.onActivity(activity -> assertEquals(
                    "Choose lessons",
                    activity.findViewById(R.id.startLessonsMenuButton)
                            .getContentDescription()));
        }

        LessonPickerTestData.clearControlledSubjects();
        try (ActivityScenario<MainActivity> scenario = launchDashboard(0)) {
            waitForVisibility(scenario, R.id.sessionButtonsView, View.GONE);
            assertVisibility(scenario, R.id.startLessonsMenuButton, View.GONE);
        }

        LessonPickerTestData.insertDefaultSubjects();
        final List<Subject> subjects = WkApplication.getDatabase().subjectCollectionsDao()
                .getAvailableLessonItems(60);
        Session.getInstance().startNewLessonSession(
                findSubject(subjects, LessonPickerTestData.RADICAL_LEVEL_2));
        Session.getInstance().startQuiz();

        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            waitForVisibility(scenario, R.id.resumeButton, View.VISIBLE);
            assertVisibility(scenario, R.id.startLessonsButton, View.GONE);
            assertVisibility(scenario, R.id.startLessonsMenuButton, View.GONE);
        }
    }

    @Test
    public void automaticVocabularySessionPersistsOnlyFilteredIds() {
        setPickerPreferences(true, true);
        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            choosePickerAction(scenario, "Vocabulary");
            waitForSessionItemCount(2);
            assertSessionIds(
                    LessonPickerTestData.KANA_VOCABULARY_LEVEL_1,
                    LessonPickerTestData.VOCABULARY_LEVEL_2);
        }
    }

    @Test
    public void pickLessonsActionOpensManualPicker() {
        setPickerPreferences(true, true);
        final Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        final Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                LessonPickerActivity.class.getName(), null, false);

        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            choosePickerAction(scenario, "Pick lessons");
            final Activity activity = instrumentation.waitForMonitorWithTimeout(monitor, 5_000L);
            assertNotNull("Lesson picker activity did not open", activity);
            assertTrue(activity instanceof LessonPickerActivity);
            activity.finish();
        }
        finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void pickerEnabledDoesNotAlterOrdinaryStartCandidates() {
        setPickerPreferences(true, true);
        assertTrue(preferences().edit()
                .putString("max_lesson_session_size", "4")
                .commit());
        try (ActivityScenario<MainActivity> scenario = launchDashboard(4)) {
            startOrdinaryLessons(scenario);
            waitForSessionItemCount(4);
            assertSessionIdSet(
                    LessonPickerTestData.RADICAL_LEVEL_2,
                    LessonPickerTestData.KANJI_LEVEL_1,
                    LessonPickerTestData.VOCABULARY_LEVEL_2,
                    LessonPickerTestData.KANA_VOCABULARY_LEVEL_1);
        }
    }

    private static SharedPreferences preferences() {
        final Context context = ApplicationProvider.getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static void setPickerPreferences(final boolean advancedEnabled,
                                             final boolean pickerEnabled) {
        assertTrue(preferences().edit()
                .putBoolean("enable_advanced", advancedEnabled)
                .putBoolean("lesson_picker_enabled", pickerEnabled)
                .commit());
    }

    private static ActivityScenario<MainActivity> launchDashboard(
            final int expectedLessonCount) {
        refreshTimeline(expectedLessonCount);
        return ActivityScenario.launch(MainActivity.class);
    }

    private static void refreshTimeline(final int expectedLessonCount) {
        LessonPickerTestData.waitForSerialAsyncTasks();
        LiveTimeLine.getInstance().forceUpdate();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        final long deadline = System.currentTimeMillis() + 5_000L;
        int actual = -1;
        while (System.currentTimeMillis() < deadline) {
            actual = LiveTimeLine.getInstance().get().getNumAvailableLessons();
            if (actual == expectedLessonCount) {
                return;
            }
            SystemClock.sleep(20L);
        }
        assertEquals("Timeline lesson count did not become ready",
                expectedLessonCount, actual);
    }

    private static void pingTimeline() {
        LiveTimeLine.getInstance().ping();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void openPickerMenu(final ActivityScenario<MainActivity> scenario) {
        waitForVisibility(scenario, R.id.startLessonsMenuButton, View.VISIBLE);
        scenario.onActivity(activity -> assertTrue(
                activity.findViewById(R.id.startLessonsMenuButton).performClick()));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void choosePickerAction(final ActivityScenario<MainActivity> scenario,
                                           final String title) {
        openPickerMenu(scenario);
        final UiAutomation automation =
                InstrumentationRegistry.getInstrumentation().getUiAutomation();
        final long deadline = System.currentTimeMillis() + 5_000L;
        while (System.currentTimeMillis() < deadline) {
            final AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root != null) {
                final List<AccessibilityNodeInfo> matches =
                        root.findAccessibilityNodeInfosByText(title);
                for (final AccessibilityNodeInfo node: matches) {
                    if (title.contentEquals(node.getText())
                            && node.isEnabled()
                            && (clickAccessibilityNode(node)
                            || tapAccessibilityNode(automation, node))) {
                        root.recycle();
                        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                        return;
                    }
                }
                root.recycle();
            }
            SystemClock.sleep(20L);
        }
        throw new AssertionError("Popup item could not be clicked: " + title);
    }

    private static boolean clickAccessibilityNode(final AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        try {
            while (current != null) {
                if (current.isClickable()) {
                    return current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                final AccessibilityNodeInfo parent = current.getParent();
                current.recycle();
                current = parent;
            }
            return false;
        }
        finally {
            if (current != null) {
                current.recycle();
            }
        }
    }

    private static boolean tapAccessibilityNode(
            final UiAutomation automation,
            final AccessibilityNodeInfo node) {
        final Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) {
            return false;
        }

        final long downTime = SystemClock.uptimeMillis();
        final MotionEvent down = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN,
                bounds.exactCenterX(), bounds.exactCenterY(), 0);
        final MotionEvent up = MotionEvent.obtain(
                downTime, downTime + 10L, MotionEvent.ACTION_UP,
                bounds.exactCenterX(), bounds.exactCenterY(), 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            return automation.injectInputEvent(down, true)
                    && automation.injectInputEvent(up, true);
        }
        finally {
            down.recycle();
            up.recycle();
        }
    }

    private static void startOrdinaryLessons(
            final ActivityScenario<MainActivity> scenario) {
        waitForVisibility(scenario, R.id.startLessonsButton, View.VISIBLE);
        scenario.onActivity(activity -> assertTrue(
                activity.findViewById(R.id.startLessonsButton).performClick()));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void waitForVisibility(
            final ActivityScenario<MainActivity> scenario,
            final int viewId,
            final int expected) {
        final long deadline = System.currentTimeMillis() + 5_000L;
        final AtomicInteger actual = new AtomicInteger(-1);
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final View view = activity.findViewById(viewId);
                assertNotNull("View was not found: " + viewId, view);
                actual.set(view.getVisibility());
            });
            if (actual.get() == expected) {
                return;
            }
            SystemClock.sleep(20L);
        }
        assertEquals("View visibility did not become ready: " + viewId,
                expected, actual.get());
    }

    private static void assertVisibility(
            final ActivityScenario<MainActivity> scenario,
            final int viewId,
            final int expected) {
        scenario.onActivity(activity -> {
            final View view = activity.findViewById(viewId);
            assertNotNull("View was not found: " + viewId, view);
            assertEquals(expected, view.getVisibility());
        });
    }

    private static void waitForSessionItemCount(final int expected) {
        final long deadline = System.currentTimeMillis() + 5_000L;
        int actual = -1;
        while (System.currentTimeMillis() < deadline) {
            actual = WkApplication.getDatabase().sessionItemDao().getAll().size();
            if (actual == expected) {
                return;
            }
            SystemClock.sleep(20L);
        }
        assertEquals("Session item count did not become ready", expected, actual);
    }

    private static void assertSessionIds(final long... expectedIds) {
        final List<SessionItem> items =
                WkApplication.getDatabase().sessionItemDao().getAll();
        assertEquals(expectedIds.length, items.size());
        for (int i = 0; i < expectedIds.length; i++) {
            assertEquals(expectedIds[i], items.get(i).getId());
        }
        assertEquals(SessionType.LESSON, Session.getInstance().getType());
    }

    private static void assertSessionIdSet(final long... expectedIds) {
        final List<SessionItem> items =
                WkApplication.getDatabase().sessionItemDao().getAll();
        final Set<Long> actualIds = new HashSet<>();
        for (final SessionItem item: items) {
            actualIds.add(item.getId());
        }

        assertEquals(expectedIds.length, items.size());
        assertEquals(expectedIds.length, actualIds.size());
        for (final long expectedId: expectedIds) {
            assertTrue("Session item was not persisted: " + expectedId,
                    actualIds.remove(expectedId));
        }
        assertTrue("Unexpected session items were persisted", actualIds.isEmpty());
        assertEquals(SessionType.LESSON, Session.getInstance().getType());
    }

    private static List<Subject> findSubject(final List<Subject> subjects,
                                             final long subjectId) {
        for (final Subject subject: subjects) {
            if (subject.getId() == subjectId) {
                return Collections.singletonList(subject);
            }
        }
        throw new AssertionError("Subject not found: " + subjectId);
    }
}
