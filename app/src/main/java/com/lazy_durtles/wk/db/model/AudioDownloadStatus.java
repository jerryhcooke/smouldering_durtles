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

/**
 * Room entity for the audio_download_status table. This caches the status
 * of audio downloads, for the audio download page.
 */
@Entity(tableName = "audio_download_status")
public final class AudioDownloadStatus {
    @PrimaryKey private int level = 0;
    private int numTotal = 0;
    private int numNoAudio = 0;
    private int numMissingAudio = 0;
    private int numPartialAudio = 0;
    private int numFullAudio = 0;

    /**
     * The level for this summary, also primary key.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The level for this summary, also primary key.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * The total number of subjects in this level.
     * @return the value
     */
    public int getNumTotal() {
        return numTotal;
    }

    /**
     * The total number of subjects in this level.
     * @param numTotal the value
     */
    public void setNumTotal(final int numTotal) {
        this.numTotal = numTotal;
    }

    /**
     * The number of subjects that have no audio.
     * @return the value
     */
    public int getNumNoAudio() {
        return numNoAudio;
    }

    /**
     * The number of subjects that have no audio.
     * @param numNoAudio the value
     */
    public void setNumNoAudio(final int numNoAudio) {
        this.numNoAudio = numNoAudio;
    }

    /**
     * The number of subjects that have audio but none are available.
     * @return the value
     */
    public int getNumMissingAudio() {
        return numMissingAudio;
    }

    /**
     * The number of subjects that have audio but none are available.
     * @param numMissingAudio the value
     */
    public void setNumMissingAudio(final int numMissingAudio) {
        this.numMissingAudio = numMissingAudio;
    }

    /**
     * The number of subjects that have audio and some are available, but not all.
     * @return the value
     */
    public int getNumPartialAudio() {
        return numPartialAudio;
    }

    /**
     * The number of subjects that have audio and some are available, but not all.
     * @param numPartialAudio the value
     */
    public void setNumPartialAudio(final int numPartialAudio) {
        this.numPartialAudio = numPartialAudio;
    }

    /**
     * The number of subjects that have audio and all are available.
     * @return the value
     */
    public int getNumFullAudio() {
        return numFullAudio;
    }

    /**
     * The number of subjects that have audio and all are available.
     * @param numFullAudio the value
     */
    public void setNumFullAudio(final int numFullAudio) {
        this.numFullAudio = numFullAudio;
    }
}
