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

import javax.annotation.Nullable;

/**
 * Room entity for the log_record table. This is where debug log records are stored.
 * Each row also contains the length of its message, to make it easier to trim excess
 * entries.
 */
public final class LogRecord {
    /**
     * Primary key.
     */
    public long id = 0L;

    /**
     * Timestamp when the event was generated.
     */
    public long timestamp = 0L;

    /**
     * The tag (class name) for this record.
     */
    public @Nullable String tag;

    /**
     * The length of the message string.
     */
    public int length = 0;

    /**
     * The log message.
     */
    public @Nullable String message;
}
