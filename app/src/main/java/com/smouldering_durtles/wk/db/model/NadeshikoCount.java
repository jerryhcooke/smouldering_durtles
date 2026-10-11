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

package com.smouldering_durtles.wk.db.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for the nadeshiko_count table: the number of sentence matches on Nadeshiko
 * for a term. Only successful API responses are stored here (including a count of 0),
 * and a stored row is never fetched again.
 */
@Entity(tableName = "nadeshiko_count")
public final class NadeshikoCount {
    /**
     * The term that was searched for.
     */
    @PrimaryKey @NonNull public String term = "";

    /**
     * The number of matches.
     */
    public int matchCount = 0;

    /**
     * Timestamp of the fetch.
     */
    public long fetchedAt = 0;
}
