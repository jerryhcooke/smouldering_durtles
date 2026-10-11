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

/**
 * Room entity for the jpdb_frequency table: one row per (term, reading) pair from the
 * JPDB frequency dictionary. This table is a permanent local copy of the downloaded
 * dictionary, keyed by term rather than by subject, so it survives database resets and
 * subject re-syncs and never needs to be downloaded again.
 */
@Entity(tableName = "jpdb_frequency", primaryKeys = {"term", "reading"})
public final class JpdbFrequency {
    /**
     * The term as written (kanji/kana).
     */
    @NonNull public String term = "";

    /**
     * The reading in hiragana, or an empty string if the dictionary didn't specify one.
     */
    @NonNull public String reading = "";

    /**
     * The JPDB frequency rank, 1 = most common.
     */
    public int freqRank = 0;
}
