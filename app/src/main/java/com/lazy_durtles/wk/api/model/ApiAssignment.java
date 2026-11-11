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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lazy_durtles.wk.components.WaniKaniApiDateDeserializer;
import com.lazy_durtles.wk.components.WaniKaniApiDateSerializer;

import javax.annotation.Nullable;

/**
 * Model class representing a user's assignment for a subject, as defined by the API.
 */
@SuppressWarnings("unused")
public final class ApiAssignment implements WaniKaniEntity {
    private long id = 0;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("available_at") private long availableAt = 0L;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("burned_at") private long burnedAt = 0L;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("passed_at") private long passedAt = 0L;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("resurrected_at") private long resurrectedAt = 0L;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("started_at") private long startedAt = 0L;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("unlocked_at") private long unlockedAt = 0L;
    @JsonProperty("srs_stage") private long srsStageId = 0;
    @JsonProperty("subject_id") private long subjectId = 0;

    /**
     * The WK ID of the entity.
     *
     * @return the ID.
     */
    public long getId() {
        return id;
    }

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public void setObject(final @Nullable String object) {
        //
    }

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or 0 if no review is scheduled yet.
     * @return the value
     */
    public long getAvailableAt() {
        return availableAt;
    }

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or 0 if no review is scheduled yet.
     * @param availableAt the value
     */
    public void setAvailableAt(final long availableAt) {
        this.availableAt = availableAt;
    }

    /**
     * The timestamp when this subject was burned, or 0 if it hasn't been burned yet.
     * @return the value
     */
    public long getBurnedAt() {
        return burnedAt;
    }

    /**
     * The timestamp when this subject was burned, or 0 if it hasn't been burned yet.
     * @param burnedAt the value
     */
    public void setBurnedAt(final long burnedAt) {
        this.burnedAt = burnedAt;
    }

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field is not filled in, but the passed boolean is
     * always reliable.
     * @return the value
     */
    public long getPassedAt() {
        return passedAt;
    }

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field is not filled in, but the passed boolean is
     * always reliable.
     * @param passedAt the value
     */
    public void setPassedAt(final long passedAt) {
        this.passedAt = passedAt;
    }

    /**
     * The timestamp when this subject was resurrected from burned status, or 0 if it hasn't been resurrected.
     * @return the value
     */
    public long getResurrectedAt() {
        return resurrectedAt;
    }

    /**
     * The timestamp when this subject was resurrected from burned status, or 0 if it hasn't been resurrected.
     * @param resurrectedAt the value
     */
    public void setResurrectedAt(final long resurrectedAt) {
        this.resurrectedAt = resurrectedAt;
    }

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or 0 if it hasn't been started yet.
     * @return the value
     */
    public long getStartedAt() {
        return startedAt;
    }

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or 0 if it hasn't been started yet.
     * @param startedAt the value
     */
    public void setStartedAt(final long startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @return the value
     */
    public long getUnlockedAt() {
        return unlockedAt;
    }

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @param unlockedAt the value
     */
    public void setUnlockedAt(final long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    /**
     * The current SRS stage for this subject, which is 0 for both locked and Initiate items.
     * @return the value
     */
    public long getSrsStageId() {
        return srsStageId;
    }

    /**
     * The current SRS stage for this subject, which is 0 for both locked and Initiate items.
     * @param srsStageId the value
     */
    public void setSrsStageId(final long srsStageId) {
        this.srsStageId = srsStageId;
    }

    /**
     * The subject ID this assignment is associated with.
     * @return the value
     */
    public long getSubjectId() {
        return subjectId;
    }

    /**
     * The subject ID this assignment is associated with.
     * @param subjectId the value
     */
    public void setSubjectId(final long subjectId) {
        this.subjectId = subjectId;
    }
}
