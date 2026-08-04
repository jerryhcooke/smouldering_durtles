package com.smouldering_durtles.wk.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityEvent;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.adapter.lessonpicker.LessonPickerAdapter;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.livedata.LiveSessionState;
import com.smouldering_durtles.wk.model.LessonPickerModel;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.proxy.ViewProxy;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * Activity for manually selecting the subjects in a new lesson session.
 */
public final class LessonPickerActivity extends AbstractActivity {
    private static final String SELECTED_SUBJECT_IDS = "selectedSubjectIds";
    private static final String SESSION_START_IN_PROGRESS = "sessionStartInProgress";
    private static final Session session = Session.getInstance();

    private final ViewProxy selectionCount = new ViewProxy();
    private final ViewProxy progress = new ViewProxy();
    private final ViewProxy empty = new ViewProxy();
    private final ViewProxy subjectList = new ViewProxy();
    private final ViewProxy start = new ViewProxy();

    private @Nullable LessonPickerModel model = null;
    private @Nullable LessonPickerAdapter adapter = null;
    private @Nullable ViewPropertyAnimator selectionCountAnimator = null;
    private int pulseGeneration = 0;
    private boolean availabilityLoaded = false;
    private boolean sessionStartInProgress = false;
    private @Nullable List<Subject> pendingAvailabilityResult = null;
    private boolean availabilityResultPending = false;
    private @Nullable StartResult pendingStartResult = null;
    private boolean startResultPending = false;

    /**
     * The constructor.
     */
    public LessonPickerActivity() {
        super(R.layout.activity_lesson_picker, R.menu.generic_options_menu);
    }

    @Override
    protected void onCreateLocal(final @Nullable Bundle savedInstanceState) {
        sessionStartInProgress = savedInstanceState != null
                && savedInstanceState.getBoolean(SESSION_START_IN_PROGRESS);
        if (!session.isInactive()) {
            if (sessionStartInProgress) {
                openStartedSession();
            }
            else {
                finish();
            }
            return;
        }

        selectionCount.setDelegate(this, R.id.lessonPickerSelectionCount);
        progress.setDelegate(this, R.id.lessonPickerProgress);
        empty.setDelegate(this, R.id.lessonPickerEmpty);
        subjectList.setDelegate(this, R.id.lessonPickerList);
        start.setDelegate(this, R.id.lessonPickerStart);

        model = new LessonPickerModel(GlobalSettings.Review.gexMaxLessonSessionSize());
        if (savedInstanceState != null) {
            final @Nullable long[] selectedSubjectIds =
                    savedInstanceState.getLongArray(SELECTED_SUBJECT_IDS);
            if (selectedSubjectIds != null) {
                model.restoreSelectedSubjectIds(selectedSubjectIds);
            }
        }

        adapter = new LessonPickerAdapter(model, this::onSubjectClicked);
        subjectList.setLayoutManager(new LinearLayoutManager(this));
        subjectList.setAdapter(adapter);
        subjectList.setHasFixedSize(false);
        start.setOnClickListener(view -> safe(this::startSelectedLessons));
        LiveSessionState.getInstance().observe(this, state ->
                safe(this::finishIfAnotherSessionStarted));

        updateSelectionControls();
        if (sessionStartInProgress) {
            disableInteraction();
        }
        loadAvailability();
    }

    @Override
    protected void onResumeLocal() {
        if (!session.isInactive()) {
            if (sessionStartInProgress) {
                openStartedSession();
            }
            else {
                finish();
            }
            return;
        }
        consumePendingAvailabilityResult();
        consumePendingStartResult();
    }

    @Override
    protected void onPauseLocal() {
        pulseGeneration++;
        final @Nullable View countView = selectionCount.getDelegate();
        if (selectionCountAnimator != null) {
            selectionCountAnimator.cancel();
            selectionCountAnimator = null;
        }
        if (countView != null) {
            countView.setAlpha(1f);
            countView.setScaleX(1f);
            countView.setScaleY(1f);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        safe(() -> {
            super.onSaveInstanceState(outState);
            final @Nullable LessonPickerModel currentModel = model;
            if (currentModel != null) {
                outState.putLongArray(
                        SELECTED_SUBJECT_IDS, currentModel.getSelectedSubjectIds());
            }
            outState.putBoolean(SESSION_START_IN_PROGRESS, sessionStartInProgress);
        });
    }

    @Override
    protected void enableInteractionLocal() {
        subjectList.enableInteraction();
        updateSelectionControls();
    }

    @Override
    protected void disableInteractionLocal() {
        subjectList.disableInteraction();
        start.disableInteraction();
    }

    @Override
    protected boolean showWithoutApiKey() {
        return false;
    }

    private void loadAvailability() {
        availabilityLoaded = false;
        progress.setVisibility(true);
        empty.setVisibility(false);
        subjectList.setVisibility(false);
        updateSelectionControls();

        runAsync(null, () -> {
            final int maxLevel =
                    WkApplication.getDatabase().propertiesDao().getUserMaxLevelGranted();
            return WkApplication.getDatabase().subjectCollectionsDao()
                    .getAvailableLessonItems(maxLevel);
        }, this::deliverAvailabilityResult);
    }

    private void deliverAvailabilityResult(final @Nullable List<Subject> result) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        pendingAvailabilityResult =
                result == null ? Collections.emptyList() : result;
        availabilityResultPending = true;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            consumePendingAvailabilityResult();
        }
    }

    private void consumePendingAvailabilityResult() {
        if (!availabilityResultPending) {
            return;
        }
        final List<Subject> result = pendingAvailabilityResult == null
                ? Collections.emptyList()
                : pendingAvailabilityResult;
        pendingAvailabilityResult = null;
        availabilityResultPending = false;
        showAvailability(result);
    }

    private void showAvailability(final List<Subject> subjects) {
        final @Nullable LessonPickerModel currentModel = model;
        final @Nullable LessonPickerAdapter currentAdapter = adapter;
        if (currentModel == null || currentAdapter == null) {
            return;
        }

        currentModel.setAvailableSubjects(subjects);
        currentAdapter.refresh();
        availabilityLoaded = true;
        progress.setVisibility(false);
        empty.setVisibility(subjects.isEmpty());
        subjectList.setVisibility(!subjects.isEmpty());
        updateSelectionControls();
        if (sessionStartInProgress) {
            if (!session.isInactive()) {
                openStartedSession();
            }
            else {
                // AsyncTask work is serial, so this reload finished after the surviving start task.
                sessionStartInProgress = false;
                enableInteraction();
                updateSelectionControls();
            }
        }
    }

    private void updateSelectionControls() {
        final @Nullable LessonPickerModel currentModel = model;
        if (currentModel == null) {
            return;
        }
        selectionCount.setText(String.format(Locale.ROOT, "Selected %d of %d",
                currentModel.getSelectedCount(),
                currentModel.getMaximumSelectionCount()));

        final @Nullable View startView = start.getDelegate();
        if (startView != null) {
            startView.setEnabled(interactionEnabled
                    && availabilityLoaded
                    && currentModel.getSelectedCount() > 0);
        }
    }

    private void onSubjectClicked(final long subjectId) {
        safe(() -> {
            final @Nullable LessonPickerModel currentModel = model;
            final @Nullable LessonPickerAdapter currentAdapter = adapter;
            if (!interactionEnabled || currentModel == null || currentAdapter == null) {
                return;
            }

            final LessonPickerModel.ToggleResult result = currentModel.toggle(subjectId);
            if (result == LessonPickerModel.ToggleResult.LIMIT_REACHED) {
                pulseSelectionLimit(currentModel.getMaximumSelectionCount());
                return;
            }

            currentAdapter.refresh();
            updateSelectionControls();
        });
    }

    @SuppressWarnings("deprecation")
    private void pulseSelectionLimit(final int maximumSelectionCount) {
        final @Nullable View countView = selectionCount.getDelegate();
        if (countView == null) {
            return;
        }

        pulseGeneration++;
        final int generation = pulseGeneration;
        if (selectionCountAnimator != null) {
            selectionCountAnimator.cancel();
        }
        countView.animate().cancel();
        countView.setAlpha(1f);
        countView.setScaleX(1f);
        countView.setScaleY(1f);

        selectionCountAnimator = countView.animate()
                .alpha(0.55f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setStartDelay(0)
                .setDuration(100)
                .withEndAction(() -> safe(() -> {
                    if (generation != pulseGeneration) {
                        return;
                    }
                    selectionCountAnimator = countView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setStartDelay(0)
                            .setDuration(180);
                    selectionCountAnimator.start();
                }));
        selectionCountAnimator.start();

        final String announcement = String.format(
                Locale.ROOT, "Maximum %d lessons selected", maximumSelectionCount);
        final AccessibilityEvent event =
                AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setClassName(countView.getClass().getName());
        event.setPackageName(getPackageName());
        event.getText().add(announcement);
        countView.sendAccessibilityEventUnchecked(event);
    }

    private void startSelectedLessons() {
        final @Nullable LessonPickerModel currentModel = model;
        if (!interactionEnabled || currentModel == null
                || currentModel.getSelectedCount() == 0) {
            return;
        }

        final long[] selectedIds = currentModel.getSelectedSubjectIds();
        sessionStartInProgress = true;
        disableInteraction();
        runAsync(null, () -> {
            final int maxLevel =
                    WkApplication.getDatabase().propertiesDao().getUserMaxLevelGranted();
            final List<Subject> available =
                    WkApplication.getDatabase().subjectCollectionsDao()
                            .getAvailableLessonItems(maxLevel);
            final List<Subject> revalidatedSubjects =
                    LessonPickerModel.retainSelected(available, selectedIds);
            if (!session.isInactive()) {
                return StartResult.sessionActive(available);
            }
            if (revalidatedSubjects.isEmpty()) {
                return StartResult.empty(available);
            }
            Session.getInstance().startNewLessonSession(revalidatedSubjects);
            return StartResult.started(available);
        }, this::deliverStartResult);
    }

    private void deliverStartResult(final @Nullable StartResult result) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        pendingStartResult = result;
        startResultPending = true;
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            consumePendingStartResult();
        }
    }

    private void consumePendingStartResult() {
        if (!startResultPending) {
            return;
        }
        final @Nullable StartResult result = pendingStartResult;
        pendingStartResult = null;
        startResultPending = false;
        onStartFinished(result);
    }

    private void onStartFinished(final @Nullable StartResult result) {
        if (result == null) {
            sessionStartInProgress = false;
            enableInteraction();
            return;
        }
        if (result.sessionActiveBeforeStart) {
            sessionStartInProgress = false;
            finish();
            return;
        }
        if (result.started) {
            openStartedSession();
            return;
        }

        sessionStartInProgress = false;
        showAvailability(result.availableSubjects);
        enableInteraction();
    }

    private void openStartedSession() {
        sessionStartInProgress = false;
        goToActivity(SessionActivity.class);
    }

    private void finishIfAnotherSessionStarted() {
        if (!session.isInactive()) {
            if (sessionStartInProgress) {
                openStartedSession();
            }
            else {
                finish();
            }
        }
    }

    private static final class StartResult {
        private final List<Subject> availableSubjects;
        private final boolean started;
        private final boolean sessionActiveBeforeStart;

        private StartResult(final List<Subject> availableSubjects,
                            final boolean started,
                            final boolean sessionActiveBeforeStart) {
            this.availableSubjects = availableSubjects;
            this.started = started;
            this.sessionActiveBeforeStart = sessionActiveBeforeStart;
        }

        private static StartResult started(final List<Subject> availableSubjects) {
            return new StartResult(availableSubjects, true, false);
        }

        private static StartResult empty(final List<Subject> availableSubjects) {
            return new StartResult(availableSubjects, false, false);
        }

        private static StartResult sessionActive(final List<Subject> availableSubjects) {
            return new StartResult(availableSubjects, false, true);
        }
    }
}
