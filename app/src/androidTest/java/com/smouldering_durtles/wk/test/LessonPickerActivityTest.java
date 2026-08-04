package com.smouldering_durtles.wk.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.activities.LessonPickerActivity;
import com.smouldering_durtles.wk.activities.SessionActivity;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.util.AsyncTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation coverage for the manual lesson picker.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class LessonPickerActivityTest {
    private static final int DEFAULT_ITEM_COUNT = 11;
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
    public void typeAndLevelOrderingCombinesBothVocabularyTypes() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            assertHeaderAt(scenario, 0, "Radicals");
            assertAccessibilityHeadingAt(scenario, 0);
            assertHeaderAt(scenario, 1, "Level 2");
            assertAccessibilityHeadingAt(scenario, 1);
            assertSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            assertHeaderAt(scenario, 3, "Kanji");
            assertHeaderAt(scenario, 4, "Level 1");
            assertSubjectAt(scenario, 5, LessonPickerTestData.KANJI_LEVEL_1);
            assertHeaderAt(scenario, 6, "Vocabulary");
            assertHeaderAt(scenario, 7, "Level 1");
            assertSubjectAt(scenario, 8, LessonPickerTestData.KANA_VOCABULARY_LEVEL_1);
            assertHeaderAt(scenario, 9, "Level 2");
            assertSubjectAt(scenario, 10, LessonPickerTestData.VOCABULARY_LEVEL_2);
        }
    }

    @Test
    public void selectionUpdatesCountStartButtonAndAccessibleCheckedState() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            assertSelectionState(scenario, "Selected 0 of 2", false);

            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            assertSelectionState(scenario, "Selected 1 of 2", true);
            withHolderAt(scenario, 2, holder -> {
                final CheckBox selected =
                        holder.itemView.findViewById(R.id.lessonPickerSelected);
                assertTrue(selected.isChecked());

                final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
                ViewCompat.onInitializeAccessibilityNodeInfo(holder.itemView, info);
                assertTrue(info.isCheckable());
                assertTrue(info.isChecked());
                final String description = String.valueOf(info.getContentDescription());
                assertTrue(description.contains("⼭"));
                assertTrue(description.contains("Level 2"));
                assertTrue(description.contains("mountain"));
                assertTrue(description.contains("selected"));
                info.recycle();
            });

            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            assertSelectionState(scenario, "Selected 0 of 2", false);
            assertSubjectCheckedAt(scenario, 2,
                    LessonPickerTestData.RADICAL_LEVEL_2, false);
        }
    }

    @Test
    public void selectionLimitRejectsNextRowPreservesTicksAndAnnounces() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            clickSubjectAt(scenario, 5, LessonPickerTestData.KANJI_LEVEL_1);
            scrollToPosition(scenario, 8);

            final AccessibilityEvent[] announcement = {null};
            scenario.onActivity(activity -> {
                final View count = activity.findViewById(R.id.lessonPickerSelectionCount);
                count.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                    @Override
                    public void sendAccessibilityEventUnchecked(
                            final View host, final AccessibilityEvent event) {
                        if (event.getEventType() == AccessibilityEvent.TYPE_ANNOUNCEMENT) {
                            announcement[0] = AccessibilityEvent.obtain(event);
                        }
                        super.sendAccessibilityEventUnchecked(host, event);
                    }
                });
            });

            clickSubjectAt(scenario, 8, LessonPickerTestData.KANA_VOCABULARY_LEVEL_1);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNotNull(announcement[0]);
            try {
                assertTrue(announcement[0].getText().toString()
                        .contains("Maximum 2 lessons selected"));
            } finally {
                announcement[0].recycle();
            }

            assertSelectionState(scenario, "Selected 2 of 2", true);
            assertSubjectCheckedAt(scenario, 8,
                    LessonPickerTestData.KANA_VOCABULARY_LEVEL_1, false);
            assertSubjectCheckedAt(scenario, 2,
                    LessonPickerTestData.RADICAL_LEVEL_2, true);
            assertSubjectCheckedAt(scenario, 5,
                    LessonPickerTestData.KANJI_LEVEL_1, true);
        }
    }

    @Test
    public void recreationPreservesSelectionAndReloadsCurrentAvailability() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            clickSubjectAt(scenario, 10, LessonPickerTestData.VOCABULARY_LEVEL_2);
            LessonPickerTestData.makeUnavailable(LessonPickerTestData.VOCABULARY_LEVEL_2);
            scenario.recreate();
            waitForItemCount(scenario, DEFAULT_ITEM_COUNT - 2);

            assertSelectionState(scenario, "Selected 1 of 2", true);
            assertSubjectCheckedAt(scenario, 2,
                    LessonPickerTestData.RADICAL_LEVEL_2, true);
        }
    }

    @Test
    public void recreationWhileSessionStartIsPendingOpensCreatedSession() {
        final Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        final Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                SessionActivity.class.getName(), null, false);
        final CountDownLatch blockerStarted = new CountDownLatch(1);
        final CountDownLatch releaseBlocker = new CountDownLatch(1);

        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            new AsyncTask<Void>() {
                @Override
                protected Void doInBackground() {
                    blockerStarted.countDown();
                    await(releaseBlocker);
                    return null;
                }

                @Override
                protected void onPostExecute(final Void result) {
                    //
                }

                @Override
                protected void onProgressUpdate(final Object[] values) {
                    //
                }
            }.execute();
            assertTrue("Serial task blocker did not start", await(blockerStarted));

            clickStart(scenario);
            scenario.recreate();
            releaseBlocker.countDown();

            final Activity activity =
                    instrumentation.waitForMonitorWithTimeout(monitor, 5_000L);
            assertNotNull("Started lesson session did not open after recreation", activity);
            assertTrue(activity instanceof SessionActivity);
            instrumentation.runOnMainSync(activity::finish);
            instrumentation.waitForIdleSync();
        }
        finally {
            releaseBlocker.countDown();
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void longListRecyclingKeepsTicksAttachedToSubjectIds() {
        LessonPickerTestData.clearControlledSubjects();
        for (int i = 0; i < 30; i++) {
            LessonPickerTestData.insertAvailable(
                    9_200_000L + i,
                    "vocabulary",
                    "語" + i,
                    "word " + i,
                    3,
                    i);
        }

        try (ActivityScenario<LessonPickerActivity> scenario =
                     ActivityScenario.launch(LessonPickerActivity.class)) {
            waitForItemCount(scenario, 32);
            clickSubjectAt(scenario, 2, 9_200_000L);
            clickSubjectAt(scenario, 31, 9_200_029L);

            assertSubjectCheckedAt(scenario, 15, 9_200_013L, false);
            assertSubjectCheckedAt(scenario, 2, 9_200_000L, true);
            assertSubjectCheckedAt(scenario, 31, 9_200_029L, true);
        }
    }

    @Test
    public void backCreatesNoSessionAndReopenStartsWithEmptySelection() {
        final String before = LessonPickerTestData.assignmentFingerprint();
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            scenario.onActivity(activity -> activity.onBackPressed());
            waitForState(scenario, Lifecycle.State.DESTROYED);
        }
        assertCancellationHadNoSideEffects(before);

        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            assertSelectionState(scenario, "Selected 0 of 2", false);
            assertSubjectCheckedAt(scenario, 2,
                    LessonPickerTestData.RADICAL_LEVEL_2, false);
        }
    }

    @Test
    public void newlyUnavailableSelectedSubjectIsDroppedBeforeStart() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            clickSubjectAt(scenario, 5, LessonPickerTestData.KANJI_LEVEL_1);
            LessonPickerTestData.makeUnavailable(LessonPickerTestData.RADICAL_LEVEL_2);

            clickStart(scenario);
            waitForSessionItemCount(1);
            final List<SessionItem> items =
                    WkApplication.getDatabase().sessionItemDao().getAll();
            assertEquals(LessonPickerTestData.KANJI_LEVEL_1, items.get(0).getId());
        }
    }

    @Test
    public void allSelectedSubjectsBecomingUnavailableLeavesPickerOpenWithZeroSelection() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            clickSubjectAt(scenario, 5, LessonPickerTestData.KANJI_LEVEL_1);
            LessonPickerTestData.makeUnavailable(LessonPickerTestData.RADICAL_LEVEL_2);
            LessonPickerTestData.makeUnavailable(LessonPickerTestData.KANJI_LEVEL_1);

            clickStart(scenario);
            waitForSelectionText(scenario, "Selected 0 of 2");
            assertTrue(Session.getInstance().isInactive());
            assertTrue(WkApplication.getDatabase().sessionItemDao().getAll().isEmpty());
            assertTrue(scenario.getState().isAtLeast(Lifecycle.State.STARTED));
            assertSelectionState(scenario, "Selected 0 of 2", false);
        }
    }

    @Test
    public void manualSelectedSessionPersistsExactIdsToSessionItemDao() {
        try (ActivityScenario<LessonPickerActivity> scenario = launchDefault()) {
            clickSubjectAt(scenario, 2, LessonPickerTestData.RADICAL_LEVEL_2);
            clickSubjectAt(scenario, 8,
                    LessonPickerTestData.KANA_VOCABULARY_LEVEL_1);

            clickStart(scenario);
            waitForSessionItemCount(2);
            final List<SessionItem> items =
                    WkApplication.getDatabase().sessionItemDao().getAll();
            assertEquals(LessonPickerTestData.KANA_VOCABULARY_LEVEL_1,
                    items.get(0).getId());
            assertEquals(LessonPickerTestData.RADICAL_LEVEL_2, items.get(1).getId());
        }
    }

    @Test
    public void openingWhileSessionIsActiveFinishesImmediately() {
        final List<Subject> subjects = WkApplication.getDatabase().subjectCollectionsDao()
                .getAvailableLessonItems(60);
        Session.getInstance().startNewLessonSession(
                findSubject(subjects, LessonPickerTestData.RADICAL_LEVEL_2));
        Session.getInstance().startQuiz();
        assertTrue(Session.getInstance().isActive());

        try (ActivityScenario<LessonPickerActivity> scenario =
                     ActivityScenario.launch(LessonPickerActivity.class)) {
            waitForState(scenario, Lifecycle.State.DESTROYED);
        }
    }

    private static ActivityScenario<LessonPickerActivity> launchDefault() {
        final ActivityScenario<LessonPickerActivity> scenario =
                ActivityScenario.launch(LessonPickerActivity.class);
        waitForItemCount(scenario, DEFAULT_ITEM_COUNT);
        return scenario;
    }

    private static void assertHeaderAt(final ActivityScenario<LessonPickerActivity> scenario,
                                       final int position,
                                       final String expected) {
        withHolderAt(scenario, position, holder ->
                assertEquals(expected, ((TextView) holder.itemView).getText().toString()));
    }

    private static void assertAccessibilityHeadingAt(
            final ActivityScenario<LessonPickerActivity> scenario,
            final int position) {
        withHolderAt(scenario, position, holder -> {
            final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
            ViewCompat.onInitializeAccessibilityNodeInfo(holder.itemView, info);
            assertTrue(info.isHeading());
            info.recycle();
        });
    }

    private static void assertSubjectAt(final ActivityScenario<LessonPickerActivity> scenario,
                                        final int position,
                                        final long subjectId) {
        withHolderAt(scenario, position, holder ->
                assertEquals(subjectId, holder.itemView.getTag(R.id.subjectId)));
    }

    private static void clickSubjectAt(final ActivityScenario<LessonPickerActivity> scenario,
                                       final int position,
                                       final long subjectId) {
        withHolderAt(scenario, position, holder -> {
            assertEquals(subjectId, holder.itemView.getTag(R.id.subjectId));
            assertTrue(holder.itemView.performClick());
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void assertSubjectCheckedAt(
            final ActivityScenario<LessonPickerActivity> scenario,
            final int position,
            final long subjectId,
            final boolean expected) {
        withHolderAt(scenario, position, holder -> {
            assertEquals(subjectId, holder.itemView.getTag(R.id.subjectId));
            final CheckBox selected =
                    holder.itemView.findViewById(R.id.lessonPickerSelected);
            assertEquals(expected, selected.isChecked());
        });
    }

    private static void assertSelectionState(
            final ActivityScenario<LessonPickerActivity> scenario,
            final String expectedText,
            final boolean startEnabled) {
        scenario.onActivity(activity -> {
            final TextView count =
                    activity.findViewById(R.id.lessonPickerSelectionCount);
            final View start = activity.findViewById(R.id.lessonPickerStart);
            assertEquals(expectedText, count.getText().toString());
            assertEquals(startEnabled, start.isEnabled());
        });
    }

    private static void clickStart(final ActivityScenario<LessonPickerActivity> scenario) {
        scenario.onActivity(activity ->
                assertTrue(activity.findViewById(R.id.lessonPickerStart).performClick()));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void scrollToPosition(
            final ActivityScenario<LessonPickerActivity> scenario,
            final int position) {
        scenario.onActivity(activity -> {
            final RecyclerView list = activity.findViewById(R.id.lessonPickerList);
            list.scrollToPosition(position);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void withHolderAt(final ActivityScenario<LessonPickerActivity> scenario,
                                     final int position,
                                     final HolderAction action) {
        scrollToPosition(scenario, position);
        scenario.onActivity(activity -> {
            final RecyclerView list = activity.findViewById(R.id.lessonPickerList);
            final RecyclerView.ViewHolder holder =
                    list.findViewHolderForAdapterPosition(position);
            assertNotNull("No holder at adapter position " + position, holder);
            action.run(holder);
        });
    }

    private static void waitForItemCount(final ActivityScenario<LessonPickerActivity> scenario,
                                         final int expected) {
        final long deadline = System.currentTimeMillis() + 5_000L;
        final AtomicInteger actual = new AtomicInteger(-1);
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final RecyclerView list = activity.findViewById(R.id.lessonPickerList);
                final RecyclerView.Adapter<?> adapter = list.getAdapter();
                actual.set(adapter == null ? -1 : adapter.getItemCount());
            });
            if (actual.get() == expected) {
                return;
            }
            SystemClock.sleep(20L);
        }
        assertEquals("Picker item count did not become ready", expected, actual.get());
    }

    private static void waitForSelectionText(
            final ActivityScenario<LessonPickerActivity> scenario,
            final String expected) {
        final long deadline = System.currentTimeMillis() + 5_000L;
        final String[] actual = {null};
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final TextView count =
                        activity.findViewById(R.id.lessonPickerSelectionCount);
                actual[0] = count.getText().toString();
            });
            if (expected.equals(actual[0])) {
                return;
            }
            SystemClock.sleep(20L);
        }
        assertEquals(expected, actual[0]);
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

    private static void waitForState(final ActivityScenario<LessonPickerActivity> scenario,
                                     final Lifecycle.State state) {
        final long deadline = System.currentTimeMillis() + 5_000L;
        while (scenario.getState() != state && System.currentTimeMillis() < deadline) {
            SystemClock.sleep(20L);
        }
        assertEquals(state, scenario.getState());
    }

    private static void assertCancellationHadNoSideEffects(final String before) {
        assertTrue(Session.getInstance().isInactive());
        assertTrue(WkApplication.getDatabase().sessionItemDao().getAll().isEmpty());
        assertEquals(before, LessonPickerTestData.assignmentFingerprint());
    }

    private static boolean await(final CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
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

    private interface HolderAction {
        void run(RecyclerView.ViewHolder holder);
    }
}
