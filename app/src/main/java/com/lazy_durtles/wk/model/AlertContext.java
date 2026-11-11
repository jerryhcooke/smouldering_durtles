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

package com.lazy_durtles.wk.model;

/**
 * The context of information needed for widget updates and notifications.
 */
public final class AlertContext {
    private int numLessons = 0;
    private int numReviews = 0;
    private long newestAvailableAt = 0L;
    private long upcomingAvailableAt = 0L;

    /**
     * Number of lessons currently available.
     * @return the value
     */
    public int getNumLessons() {
        return numLessons;
    }

    /**
     * Number of lessons currently available.
     * @param numLessons the value
     */
    public void setNumLessons(final int numLessons) {
        this.numLessons = numLessons;
    }

    /**
     * Number of reviews currently available.
     * @return the value
     */
    public int getNumReviews() {
        return numReviews;
    }

    /**
     * Number of reviews currently available.
     * @param numReviews the value
     */
    public void setNumReviews(final int numReviews) {
        this.numReviews = numReviews;
    }

    /**
     * The date when the newest available review became available.
     * @return the value
     */
    public long getNewestAvailableAt() {
        return newestAvailableAt;
    }

    /**
     * The date when the newest available review became available.
     * @param newestAvailableAt the value
     */
    public void setNewestAvailableAt(final long newestAvailableAt) {
        this.newestAvailableAt = newestAvailableAt;
    }

    /**
     * The date when the next new review will become available.
     * @return the date
     */
    public long getUpcomingAvailableAt() {
        return upcomingAvailableAt;
    }

    /**
     * The date when the next new review will become available.
     * @param upcomingAvailableAt the date
     */
    public void setUpcomingAvailableAt(final long upcomingAvailableAt) {
        this.upcomingAvailableAt = upcomingAvailableAt;
    }
}
