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

import java.util.Collections;
import java.util.List;

/**
 * Model class for a lesson/review session in the summary endpoint of the API.
 */
@SuppressWarnings("unused")
public final class ApiSummarySession {
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("available_at") private long availableAt = 0;
    @JsonProperty("subject_ids") private List<Long> subjectIds = Collections.emptyList();

    /**
     * Timestamp when the subjects in this session will become or have become available.
     * @return the value
     */
    public long getAvailableAt() {
        return availableAt;
    }

    /**
     * Timestamp when the subjects in this session will become or have become available.
     * @param availableAt the value
     */
    public void setAvailableAt(final long availableAt) {
        this.availableAt = availableAt;
    }

    /**
     * The subject IDs for this session.
     * @return the value
     */
    public List<Long> getSubjectIds() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return subjectIds;
    }

    /**
     * The subject IDs for this session.
     * @param subjectIds the value
     */
    public void setSubjectIds(final List<Long> subjectIds) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.subjectIds = subjectIds;
    }
}
