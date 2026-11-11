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

package com.lazy_durtles.wk.db.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lazy_durtles.wk.api.model.ApiStage;
import com.lazy_durtles.wk.db.Converters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.isEmpty;

/**
 * Room entity for the srs_system table. This represents an SRS system.
 */
@Entity(tableName = "srs_system")
public final class SrsSystemDefinition {
    /**
     * The system's unique ID
     */
    @PrimaryKey public long id = 0L;

    /**
     * The name.
     */
    public @Nullable String name;

    /**
     * A description.
     */
    public @Nullable String description;

    /**
     * The stages in the system, encoded as a JSON string.
     */
    public @Nullable String stages;

    /**
     * The initiate stage.
     */
    public long unlockingStagePosition = 0L;

    /**
     * The first post-initiate stage.
     */
    public long startingStagePosition = 0L;

    /**
     * The first passing stage.
     */
    public long passingStagePosition = 0L;

    /**
     * The burned stage.
     */
    public long burningStagePosition = 0L;

    /**
     * Get the parsed version of the stages list.
     *
     * @return the list
     */
    @Ignore
    public List<ApiStage> getParsedStages() {
        if (isEmpty(stages)) {
            return Collections.emptyList();
        }
        try {
            return Converters.getObjectMapper().readValue(stages, new TypeReference<List<ApiStage>>() {});
        } catch (final IOException e) {
            return Collections.emptyList();
        }
    }
}
