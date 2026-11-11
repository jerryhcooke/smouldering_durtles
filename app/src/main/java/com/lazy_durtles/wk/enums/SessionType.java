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

package com.lazy_durtles.wk.enums;

/**
 * The type of session.
 */
public enum SessionType {
    /**
     * None, the session isn't active.
     */
    NONE(false, false, "None"),

    /**
     * A lesson session.
     */
    LESSON(false, true, "Lesson"),

    /**
     * A review session.
     */
    REVIEW(true, true, "Review"),

    /**
     * A self-study session which is not reported back to the mothership.
     */
    SELF_STUDY(false, false, "Self-study");

    private final boolean srsRelevant;
    private final boolean reportingTaskNeeded;
    private final String description;

    /**
     * The constructor.
     *
     * @param srsRelevant Are SRS stage changes relevant for this session type?.
     * @param reportingTaskNeeded When a session item's result is reported, does this session type also require a reporting task?.
     */
    SessionType(final boolean srsRelevant, final boolean reportingTaskNeeded, final String description) {
        this.srsRelevant = srsRelevant;
        this.reportingTaskNeeded = reportingTaskNeeded;
        this.description = description;
    }

    /**
     * Are SRS stage changes relevant for this session type?.
     * @return the value
     */
    public boolean isSrsRelevant() {
        return srsRelevant;
    }

    /**
     * When a session item's result is reported, does this session type also require a reporting task?.
     * @return the value
     */
    public boolean isReportingTaskNeeded() {
        return reportingTaskNeeded;
    }

    /**
     * The human readable description for this session type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
