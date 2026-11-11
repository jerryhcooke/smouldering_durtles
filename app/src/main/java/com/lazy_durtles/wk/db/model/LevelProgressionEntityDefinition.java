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
import androidx.room.PrimaryKey;

import javax.annotation.Nullable;

/**
 * Room entity for the level_progression table. See LogRecordEntityDefinition for an explanation of why this class exists.
 */
@SuppressWarnings("unused")
@Entity(tableName = "level_progression")
public final class LevelProgressionEntityDefinition {
    @PrimaryKey public long id = 0L;
    public @Nullable Long abandonedAt;
    public @Nullable Long completedAt;
    public @Nullable Long createdAt;
    public @Nullable Long passedAt;
    public @Nullable Long startedAt;
    public @Nullable Long unlockedAt;
    public int level = 0;
}
