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
 * Room entity for the log_record table. The entity class actually used is LogRecord, this is just
 * a class used to define the schema for this table. The reason for two separate classes is a schema
 * migration: I moved from timestamps as Date instances to long Java timestamps in application code.
 * The database is already storing timestamps as long Java timestamps, but the application code was
 * using Date. An artifact of the current schema is that the timestamp columns are defined to be
 * nullable, even though a primitive long is not nullable.
 * Until I do a schema change to recreate the table with non-nullable timestamp columns,
 * this is a workaround so application code can use primitive longs but the database schema can stay
 * the same.
 *
 * TLDR: ignore this class except when modifying the database schema. Drop this class when the inevitable
 * upcoming schema overhaul is done.
 */
@SuppressWarnings("unused")
@Entity(tableName = "log_record")
public final class LogRecordEntityDefinition {
    @PrimaryKey(autoGenerate = true) public long id = 0L;
    public @Nullable Long timestamp;
    public @Nullable String tag;
    public int length = 0;
    public @Nullable String message;
}
