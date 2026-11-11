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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Model class for SRS system records as reported by the API.
 */
@SuppressWarnings({"CanBeFinal", "unused"})
public final class ApiSrsSystem implements WaniKaniEntity {
    /**
     * The unique ID.
     */
    public long id = 0;

    /**
     * The system's name.
     */
    public @Nullable String name = null;

    /**
     * A description.
     */
    public @Nullable String description = null;

    /**
     * The stages in the system.
     */
    public Collection<ApiStage> stages = new ArrayList<>();

    /**
     * The initiate stage.
     */
    @JsonProperty("unlocking_stage_position")
    public long unlockingStagePosition = 0L;

    /**
     * The first post-initiate stage.
     */
    @JsonProperty("starting_stage_position")
    public long startingStagePosition = 0L;

    /**
     * The first passing stage.
     */
    @JsonProperty("passing_stage_position")
    public long passingStagePosition = 0L;

    /**
     * The burned stage.
     */
    @JsonProperty("burning_stage_position")
    public long burningStagePosition = 0L;

    @Override
    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public void setObject(final @Nullable String object) {
        //
    }
}
