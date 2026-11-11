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

package com.lazy_durtles.wk.tasks;

import static com.lazy_durtles.wk.Constants.API_RETRY_DELAY;
import static com.lazy_durtles.wk.Constants.MINUTE;
import static com.lazy_durtles.wk.Constants.NUM_API_TRIES;
import static com.lazy_durtles.wk.enums.SessionType.LESSON;
import static com.lazy_durtles.wk.enums.SessionType.REVIEW;
import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

import com.fasterxml.jackson.databind.JsonNode;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.ApiState;
import com.lazy_durtles.wk.api.model.ApiAssignment;
import com.lazy_durtles.wk.api.model.ApiCreateReview;
import com.lazy_durtles.wk.api.model.ApiReviewStatistic;
import com.lazy_durtles.wk.api.model.ApiStartAssignment;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.enums.SessionType;
import com.lazy_durtles.wk.livedata.LiveAlertContext;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveBurnedItems;
import com.lazy_durtles.wk.livedata.LiveCriticalCondition;
import com.lazy_durtles.wk.livedata.LiveJlptProgress;
import com.lazy_durtles.wk.livedata.LiveJoyoProgress;
import com.lazy_durtles.wk.livedata.LiveLevelDuration;
import com.lazy_durtles.wk.livedata.LiveLevelProgress;
import com.lazy_durtles.wk.livedata.LiveRecentUnlocks;
import com.lazy_durtles.wk.livedata.LiveSrsBreakDown;
import com.lazy_durtles.wk.livedata.LiveTimeLine;
import com.lazy_durtles.wk.util.Logger;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Task to report a completed session item to the API. In case of a lesson
 * session, start the subject's assignment. In case of a review session, create
 * a review.
 */
public final class ReportSessionItemTask extends ApiTask {
    private static final Logger LOGGER = Logger.get(ReportSessionItemTask.class);

    /**
     * Task priority. Medium priority ahead of the model refreshes, to make sure the model
     * refreshes don't force the app to record stale data that will be invalidated by this
     * task anyway.
     */
    public static final int PRIORITY = 15;

    private final long timestamp;
    private final long subjectId;
    private long assignmentId;
    private final SessionType type;
    private final int meaningIncorrect;
    private final int readingIncorrect;
    private final boolean justPassed;
    private boolean keepTask = false;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public ReportSessionItemTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
        final String[] parts = orElse(taskDefinition.getData(), "").split(" ");
        timestamp = Long.parseLong(parts[0]);
        subjectId = Long.parseLong(parts[1]);
        assignmentId = Long.parseLong(parts[2]);
        type = SessionType.valueOf(parts[3]);
        meaningIncorrect = Integer.parseInt(parts[4]);
        readingIncorrect = Integer.parseInt(parts[5]);
        justPassed = parts.length >= 7 && Boolean.parseBoolean(parts[6]);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    /**
     * In case the subject's assignment ID isn't known yet, find it. First check the
     * local database, and ask the API as a fallback if needed.
     *
     * <p>
     *     With the current limited predictive logic, this should never be needed anymore,
     *     the assignment ID should always be filled in in realistic scenarios. So this
     *     is only still around as a last-ditch fallback and maybe for future changes to
     *     predictive logic.
     * </p>
     */
    private void findAssignmentId() {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable Subject subject = db.subjectDao().getById(subjectId);
        if (subject != null && subject.getAssignmentId() > 0) {
            assignmentId = subject.getAssignmentId();
            return;
        }

        final String uri = "/v2/assignments?subject_ids=" + subjectId;
        if (!collectionApiCall(uri, ApiAssignment.class, t -> assignmentId = t.getId())) {
            keepTask = true;
        }
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        keepTask = false;

        if (type == LESSON) {
            if (assignmentId <= 0) {
                findAssignmentId();
            }

            if (assignmentId > 0) {
                final ApiStartAssignment requestBody = new ApiStartAssignment();
                if (timestamp > 0 && System.currentTimeMillis() - timestamp > MINUTE * 5) {
                    requestBody.setStartedAt(timestamp);
                }
                final String url = String.format(Locale.ROOT, "/v2/assignments/%d/start", assignmentId);
                final @Nullable JsonNode responseBody = postApiCallWithRetry(url, "PUT", requestBody, NUM_API_TRIES, API_RETRY_DELAY);
                if (responseBody == null) {
                    keepTask = true;
                }
                if (responseBody != null && responseBody.has("id")) {
                    try {
                        final @Nullable ApiAssignment assignment = parseEntity(responseBody, ApiAssignment.class);
                        if (assignment != null) {
                            db.subjectSyncDao().insertOrUpdateAssignment(assignment);
                            LiveApiState.getInstance().forceUpdate();
                            LiveTimeLine.getInstance().update();
                            LiveSrsBreakDown.getInstance().update();
                            LiveLevelProgress.getInstance().update();
                            LiveJoyoProgress.getInstance().update();
                            LiveJlptProgress.getInstance().update();
                            LiveRecentUnlocks.getInstance().update();
                            LiveCriticalCondition.getInstance().update();
                            LiveBurnedItems.getInstance().update();
                            LiveLevelDuration.getInstance().forceUpdate();
                            LiveAlertContext.getInstance().update();
                        }
                    } catch (final Exception e) {
                        LOGGER.error(e, "Error parsing start-assignment response");
                    }
                }
            }
        }

        if (type == REVIEW) {
            final ApiCreateReview requestBody = new ApiCreateReview();
            requestBody.getReview().setSubjectId(subjectId);
            requestBody.getReview().setIncorrectMeaningAnswers(meaningIncorrect);
            requestBody.getReview().setIncorrectReadingAnswers(readingIncorrect);
            if (timestamp > 0 && System.currentTimeMillis() - timestamp > MINUTE * 5) {
                requestBody.getReview().setCreatedAt(timestamp);
            }
            final String url = "/v2/reviews";
            final @Nullable JsonNode responseBody = postApiCallWithRetry(url, "POST", requestBody, NUM_API_TRIES, API_RETRY_DELAY);
            if (responseBody == null) {
                keepTask = true;
            }
            if (responseBody != null && responseBody.has("id") && responseBody.has("resources_updated")) {
                final JsonNode resourcesUpdated = responseBody.get("resources_updated");
                if (resourcesUpdated.has("assignment")) {
                    final JsonNode assignmentJson = resourcesUpdated.get("assignment");
                    try {
                        final @Nullable ApiAssignment assignment = parseEntity(assignmentJson, ApiAssignment.class);
                        if (assignment != null) {
                            db.subjectSyncDao().insertOrUpdateAssignment(assignment);
                        }
                    } catch (final Exception e) {
                        LOGGER.error(e, "Error parsing create-review response");
                    }
                }
                if (resourcesUpdated.has("review_statistic")) {
                    final JsonNode reviewStatisticJson = resourcesUpdated.get("review_statistic");
                    try {
                        final @Nullable ApiReviewStatistic reviewStatistic = parseEntity(reviewStatisticJson, ApiReviewStatistic.class);
                        if (reviewStatistic != null) {
                            db.subjectSyncDao().insertOrUpdateReviewStatistic(reviewStatistic);
                        }
                    } catch (final Exception e) {
                        LOGGER.error(e, "Error parsing create-review response");
                    }
                }
                LiveApiState.getInstance().forceUpdate();
                LiveTimeLine.getInstance().update();
                LiveSrsBreakDown.getInstance().update();
                LiveLevelProgress.getInstance().update();
                LiveJoyoProgress.getInstance().update();
                LiveJlptProgress.getInstance().update();
                LiveRecentUnlocks.getInstance().update();
                LiveCriticalCondition.getInstance().update();
                LiveBurnedItems.getInstance().update();
                LiveLevelDuration.getInstance().forceUpdate();
                LiveAlertContext.getInstance().update();
            }
        }

        if (justPassed) {
            db.propertiesDao().setForceLateRefresh(true);
        }

        if (!keepTask) {
            db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        }
    }
}
