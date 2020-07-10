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

package com.the_tinkering.wk.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.the_tinkering.wk.components.WaniKaniApiDateDeserializer;
import com.the_tinkering.wk.components.WaniKaniApiDateSerializer;

import java.util.Date;

import javax.annotation.Nullable;

/**
 * Model class used in the API to start an assignment (submit a lesson result).
 */
@SuppressWarnings("unused")
public final class ApiStartAssignment {
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("started_at") private @Nullable Date startedAt = null;

    /**
     * The timestamp when the assignment was started, or null to request that the API set the current time as timestamp.
     * @return the value
     */
    public @Nullable Date getStartedAt() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return startedAt;
    }

    /**
     * The timestamp when the assignment was started, or null to request that the API set the current time as timestamp.
     * @param startedAt the value
     */
    public void setStartedAt(final @Nullable Date startedAt) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.startedAt = startedAt;
    }
}
