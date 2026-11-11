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

package com.lazy_durtles.wk.api.model;

import java.util.Collections;
import java.util.List;

/**
 * Model class for upcoming lessons and reviews for the next 24 hours.
 *
 * <p>
 *     This isn't used for putting together the timeline or lesson/review sessions.
 *     It's only used as a last resort panic button to fix the app and the WK servers
 *     getting out of sync.
 * </p>
 */
@SuppressWarnings("unused")
public final class ApiSummary {
    private List<ApiSummarySession> lessons = Collections.emptyList();
    private List<ApiSummarySession> reviews = Collections.emptyList();

    /**
     * The available lesson. This only contains one session, since lessons are not planned ahead.
     * @return the value
     */
    public List<ApiSummarySession> getLessons() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return lessons;
    }

    /**
     * The available lesson. This only contains one session, since lessons are not planned ahead.
     * @param lessons the value
     */
    public void setLessons(final List<ApiSummarySession> lessons) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.lessons = lessons;
    }

    /**
     * The available reviews for up to 24 one-hour time slots in advance.
     * @return the value
     */
    public List<ApiSummarySession> getReviews() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return reviews;
    }

    /**
     * The available reviews for up to 24 one-hour time slots in advance.
     * @param reviews the value
     */
    public void setReviews(final List<ApiSummarySession> reviews) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.reviews = reviews;
    }
}
