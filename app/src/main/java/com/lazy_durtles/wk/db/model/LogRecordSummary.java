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

/**
 * Room entity for a subset of the log_record table. Only contains the id and length, for trimming.
 */
public final class LogRecordSummary {
    private long id = 0L;
    private int length = 0;

    /**
     * The unique ID.
     * @return the value
     */
    public long getId() {
        return id;
    }

    /**
     * The unique ID.
     * @param id the value
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * The length of the message string.
     * @return the value
     */
    public int getLength() {
        return length;
    }

    /**
     * The length of the message string.
     * @param length the value
     */
    public void setLength(final int length) {
        this.length = length;
    }
}
