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

import com.lazy_durtles.wk.db.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.util.ObjectSupport.getTopOfHour;

/**
 * A model for the dashboard timeline, containing the available and upcoming lessons and reviews.
 * A lesson or review is represented by the subject it's for.
 */
public final class TimeLine {
    private final long createdAt;
    private final long firstSlot;
    private final List<Subject> availableLessons = new ArrayList<>();
    private final List<Subject> availableReviews = new ArrayList<>();
    private final List<List<Subject>> timeLine = new ArrayList<>();
    private final List<Integer> numRequiredForLevelUp = new ArrayList<>();
    private long longTermUpcomingReviewDate = 0;
    private int numLongTermUpcomingReviews = 0;

    /**
     * The constructor.
     *
     * @param size the size of the timeline in hours.
     */
    public TimeLine(final int size) {
        createdAt = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            timeLine.add(new ArrayList<>());
            numRequiredForLevelUp.add(0);
        }

        firstSlot = getTopOfHour(createdAt);
    }

    /**
     * The date of the first slot of the timeline, the leftmost bar in the chart.
     * This is the top of the hour at the time this object was created.
     * @return the value
     */
    public long getFirstSlot() {
        return firstSlot;
    }

    /**
     * The next timestamp beyond the timeline when a review will become available.
     * @return the value
     */
    public long getLongTermUpcomingReviewDate() {
        return longTermUpcomingReviewDate;
    }

    /**
     * The next timestamp beyond the timeline when a review will become available.
     * @param longTermUpcomingReviewDate the value
     */
    public void setLongTermUpcomingReviewDate(final long longTermUpcomingReviewDate) {
        this.longTermUpcomingReviewDate = longTermUpcomingReviewDate;
    }

    /**
     * The number of reviews that will become available at the above time.
     * @return the value
     */
    public int getNumLongTermUpcomingReviews() {
        return numLongTermUpcomingReviews;
    }

    /**
     * The number of reviews that will become available at the above time.
     * @param numLongTermUpcomingReviews the value
     */
    public void setNumLongTermUpcomingReviews(final int numLongTermUpcomingReviews) {
        this.numLongTermUpcomingReviews = numLongTermUpcomingReviews;
    }

    /**
     * The list of available lessons.
     * @return the value
     */
    public List<Subject> getAvailableLessons() {
        return Collections.unmodifiableList(availableLessons);
    }

    /**
     * The list of reviews that are available right now.
     * @return the value
     */
    public List<Subject> getAvailableReviews() {
        return Collections.unmodifiableList(availableReviews);
    }

    /**
     * The timeline of upcoming reviews. Each element in the list is a window spaced 1 hour apart.
     * The first window is the current hour, and the number of elements is the timeline's size. Any element
     * in the list may be empty, but not null.
     * @return the value
     */
    public List<List<Subject>> getTimeLine() {
        return Collections.unmodifiableList(timeLine);
    }

    /**
     * The number of items on the level-up progression path, per time slot.
     * @return the value
     */
    public List<Integer> getNumRequiredForLevelUp() {
        return Collections.unmodifiableList(numRequiredForLevelUp);
    }

    /**
     * During construction: add a lesson to this instance.
     *
     * @param lesson the lesson to add
     */
    public void addLesson(final Subject lesson) {
        availableLessons.add(lesson);
    }

    /**
     * During construction: add a review to this instance.
     *
     * @param review the review to add
     * @param requiredForLevelUp is this item on the level-up progression path?
     */
    public void addReview(final Subject review, final boolean requiredForLevelUp) {
        if (review.getAvailableAt() == 0) {
            return;
        }
        if (review.getAvailableAt() < createdAt) {
            availableReviews.add(review);
        }

        final long delay = review.getAvailableAt() - firstSlot;
        int slot = (int) (delay / HOUR);
        if (slot < 0) {
            slot = 0;
        }
        if (slot < timeLine.size()) {
            timeLine.get(slot).add(review);
            if (requiredForLevelUp) {
                numRequiredForLevelUp.set(slot, numRequiredForLevelUp.get(slot) + 1);
            }
        }
    }

    /**
     * Does this timeline have any lessons available right now?.
     *
     * @return true if it has
     */
    public boolean hasAvailableLessons() {
        return !availableLessons.isEmpty();
    }

    /**
     * Get the number of currently available lessons.
     *
     * @return the number
     */
    public int getNumAvailableLessons() {
        return availableLessons.size();
    }

    /**
     * Does this timeline have any reviews available right now?.
     *
     * @return true if it has
     */
    public boolean hasAvailableReviews() {
        return !availableReviews.isEmpty();
    }

    /**
     * Get the number of currently available reviews.
     *
     * @return the number
     */
    public int getNumAvailableReviews() {
        return availableReviews.size();
    }

    /**
     * Does this timeline have any reviews that will become available in the future?.
     *
     * @return true if it has
     */
    public boolean hasUpcomingReviews() {
        for (int i=1; i<timeLine.size(); i++) {
            final List<Subject> slot = timeLine.get(i);
            if (!slot.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of reviews in this timeline that will become available in the future.
     *
     * @return the number
     */
    public int getNumUpcomingReviews() {
        int count = 0;
        for (int i=1; i<timeLine.size(); i++) {
            count += timeLine.get(i).size();
        }
        return count;
    }

    /**
     * Get the number of reviews in this timeline that will become available in the next slot in this timeline.
     *
     * @return the number
     */
    public int getNumSingleSlotUpcomingReviews() {
        for (int i=1; i<timeLine.size(); i++) {
            final List<Subject> slot = timeLine.get(i);
            if (!slot.isEmpty()) {
                return slot.size();
            }
        }
        return 0;
    }

    /**
     * Get the next date when reviews in this timeline become available.
     *
     * @return the date, or null if no upcoming reviews in this timeline.
     */
    public long getUpcomingReviewDate() {
        for (int i=1; i<timeLine.size(); i++) {
            final List<Subject> slot = timeLine.get(i);
            if (!slot.isEmpty()) {
                return slot.get(0).getAvailableAt();
            }
        }
        return 0;
    }

    /**
     * Are there any upcoming reviews that will arrive after the last slot in this timeline?.
     *
     * @return true if there are
     */
    public boolean hasLongTermUpcomingReviews() {
        return longTermUpcomingReviewDate != 0;
    }

    /**
     * Get the size of the timeline in hours.
     *
     * @return the size
     */
    public int getSize() {
        return timeLine.size();
    }
}
