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

package com.smouldering_durtles.wk.jobs;

import static com.smouldering_durtles.wk.Constants.SECOND;
import static com.smouldering_durtles.wk.enums.SessionItemState.REPORTED;
import static com.smouldering_durtles.wk.enums.SessionType.LESSON;
import static com.smouldering_durtles.wk.enums.SessionType.REVIEW;
import static com.smouldering_durtles.wk.util.ObjectSupport.getTopOfHour;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.livedata.LiveAlertContext;
import com.smouldering_durtles.wk.livedata.LiveBurnedItems;
import com.smouldering_durtles.wk.livedata.LiveCriticalCondition;
import com.smouldering_durtles.wk.livedata.LiveLevelProgress;
import com.smouldering_durtles.wk.livedata.LiveReviewTotals;
import com.smouldering_durtles.wk.livedata.LiveSrsBreakDown;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.model.SrsSystem;

import javax.annotation.Nullable;

/**
 * Job to report a session item as finished normally (not abandoned).
 *
 * <p>
 *     At minimum, this will do a last update of the item in the database.
 *     It will also patch the subject locally to reflect the result of the
 *     lesson or review. The next review is also scheduled.
 * </p>
 *
 * <p>
 *     If the session is a lesson or review session, a task is also scheduled
 *     to report the result on the API.
 * </p>
 */
public final class ReportSessionItemJob extends Job {
    private final long subjectId;
    private final long assignmentId;
    private final SessionType type;
    private final int itemMeaningIncorrect;
    private final int itemReadingIncorrect;
    private final long timestamp;
    private final boolean updateLiveData;

    /**
     * The constructor.
     *
     * @param data parameters
     */
    @SuppressWarnings("unused")
    public ReportSessionItemJob(final String data) {
        super(data);
        final String[] parts = data.split(" ");
        subjectId = Long.parseLong(parts[0]);
        assignmentId = Long.parseLong(parts[1]);
        type = SessionType.valueOf(parts[2]);
        itemMeaningIncorrect = Integer.parseInt(parts[3]);
        itemReadingIncorrect = Integer.parseInt(parts[4]);
        timestamp = Long.parseLong(parts[5]);
        updateLiveData = true;
    }

    /**
     * The constructor.
     *
     * @param subjectId The subject ID for this item.
     * @param assignmentId The assignment ID for this item's subject, or 0 if not yet known.
     * @param type The type of the current session.
     * @param itemMeaningIncorrect The final number of incorrect meaning answers.
     * @param itemReadingIncorrect The final number of incorrect reading answers.
     * @param timestamp The Unix timestamp in ms when the last question for this item was answered.
     * @param updateLiveData True if the job should update livedata instances as well.
     */
    public ReportSessionItemJob(final long subjectId, final long assignmentId, final SessionType type,
                                final int itemMeaningIncorrect, final int itemReadingIncorrect, final long timestamp, final boolean updateLiveData) {
        super("");
        this.subjectId = subjectId;
        this.assignmentId = assignmentId;
        this.type = type;
        this.itemMeaningIncorrect = itemMeaningIncorrect;
        this.itemReadingIncorrect = itemReadingIncorrect;
        this.timestamp = timestamp;
        this.updateLiveData = updateLiveData;
    }

    private void processLessonFinished(final long ts) {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable Subject subject = db.subjectDao().getById(subjectId);
        if (subject == null) {
            return;
        }

        final SrsSystem.Stage stage = subject.getSrsSystem().getFirstStartedStage();
        final long interval = stage.getInterval();
        final long available = ts + interval + 30 * SECOND;

        db.subjectSyncDao().patchAssignment(subjectId, stage.getId(), subject.getUnlockedAt(), ts, getTopOfHour(available),
                subject.getPassedAt(), subject.getBurnedAt(), subject.getResurrectedAt());
    }

    private boolean processReviewFinished(final long ts) {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable Subject subject = db.subjectDao().getById(subjectId);
        if (subject == null) {
            return false;
        }

        final SrsSystem.Stage newSrsStage = subject.getSrsStage().getNewStage(itemMeaningIncorrect + itemReadingIncorrect);
        final long interval = newSrsStage.getInterval();
        final long available = ts + interval + 30 * SECOND;

        final long availableAt;
        long burnedAt = subject.getBurnedAt();
        long passedAt = subject.getPassedAt();
        if (newSrsStage.isCompleted()) {
            if (burnedAt == 0) {
                burnedAt = ts;
            }
            availableAt = 0;
        }
        else {
            availableAt = getTopOfHour(available);
        }
        boolean justPassed = false;
        if (newSrsStage.isPassed() && !subject.isPassed()) {
            passedAt = ts;
            if (subject.hasAmalgamations()) {
                justPassed = true;
                db.propertiesDao().setSyncReminder(true);
            }
        }

        db.subjectSyncDao().patchAssignment(subject.getId(), newSrsStage.getId(), subject.getUnlockedAt(),
                subject.getStartedAt(), availableAt, passedAt, burnedAt, subject.getResurrectedAt());

        int meaningCorrect = subject.getMeaningCorrect();
        int meaningIncorrect = subject.getMeaningIncorrect();
        int meaningCurrentStreak = subject.getMeaningCurrentStreak();
        int meaningMaxStreak = subject.getMeaningMaxStreak();
        int readingCorrect = subject.getReadingCorrect();
        int readingIncorrect = subject.getReadingIncorrect();
        int readingCurrentStreak = subject.getReadingCurrentStreak();
        int readingMaxStreak = subject.getReadingMaxStreak();
        final int percentageCorrect;

        if (itemMeaningIncorrect == 0) {
            meaningCorrect++;
            meaningCurrentStreak++;
            if (meaningCurrentStreak > meaningMaxStreak) {
                meaningMaxStreak = meaningCurrentStreak;
            }
        }
        else {
            meaningIncorrect++;
            meaningCurrentStreak = 0;
        }

        if (itemReadingIncorrect == 0) {
            readingCorrect++;
            readingCurrentStreak++;
            if (readingCurrentStreak > readingMaxStreak) {
                readingMaxStreak = readingCurrentStreak;
            }
        }
        else {
            readingIncorrect++;
            readingCurrentStreak = 0;
        }

        final int total = meaningCorrect + meaningIncorrect + readingCorrect + readingIncorrect;
        final int totalCorrect = meaningCorrect + readingCorrect;
        percentageCorrect = (totalCorrect * 100) / total;

        db.subjectSyncDao().patchReviewStatistic(subjectId,
                meaningCorrect, meaningIncorrect, meaningCurrentStreak, meaningMaxStreak,
                readingCorrect, readingIncorrect, readingCurrentStreak, readingMaxStreak,
                percentageCorrect);

        return justPassed;
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        final @Nullable SessionItem item = db.sessionItemDao().getById(subjectId);
        if (item != null) {
            item.setState(REPORTED);
            item.setQuestion1Done(true);
            item.setQuestion2Done(true);
            item.setQuestion3Done(true);
            item.setQuestion4Done(true);
            item.setLastAnswer(timestamp);
            db.sessionItemDao().update(item);
        }

        final long ts = timestamp > 0 ? timestamp : System.currentTimeMillis();
        boolean justPassed = false;

        if (type == LESSON) {
            processLessonFinished(ts);

            if (updateLiveData) {
                LiveTimeLine.getInstance().update();
                LiveSrsBreakDown.getInstance().update();
                LiveLevelProgress.getInstance().update();
                LiveReviewTotals.getInstance().update();
                LiveAlertContext.getInstance().update();
            }
        }

        if (type == REVIEW) {
            justPassed = processReviewFinished(ts);

            if (updateLiveData) {
                LiveTimeLine.getInstance().update();
                LiveSrsBreakDown.getInstance().update();
                LiveLevelProgress.getInstance().update();
                LiveCriticalCondition.getInstance().update();
                LiveBurnedItems.getInstance().update();
                LiveReviewTotals.getInstance().update();
                LiveAlertContext.getInstance().update();
            }
        }

        if (type.isReportingTaskNeeded()) {
            db.assertReportSessionItemTask(ts, subjectId, assignmentId, type, itemMeaningIncorrect, itemReadingIncorrect, justPassed);
        }

        if (itemMeaningIncorrect > 0 || itemReadingIncorrect > 0) {
            db.subjectDao().updateLastIncorrectAnswer(subjectId, ts);
        }

        if (updateLiveData) {
            houseKeeping();
        }
    }
}
