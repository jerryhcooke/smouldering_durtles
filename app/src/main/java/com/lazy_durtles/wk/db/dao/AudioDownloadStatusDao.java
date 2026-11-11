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

package com.lazy_durtles.wk.db.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.lazy_durtles.wk.db.model.AudioDownloadStatus;

import java.util.List;

/**
 * DAO for audio download status records.
 */
@Dao
public abstract class AudioDownloadStatusDao {
    /**
     * Room-generated method: get all records currently available.
     *
     * @return the list of records
     */
    @Query("SELECT * FROM audio_download_status ORDER BY level")
    public abstract List<AudioDownloadStatus> getAll();

    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM audio_download_status")
    public abstract void deleteAll();

    /**
     * Room-generated method: insert or update a record.
     *
     * @param level the level
     * @param numTotal AudioDownloadStatus field
     * @param numNoAudio AudioDownloadStatus field
     * @param numMissingAudio AudioDownloadStatus field
     * @param numPartialAudio AudioDownloadStatus field
     * @param numFullAudio AudioDownloadStatus field
     */
    @Query("INSERT OR REPLACE INTO audio_download_status (level, numTotal, numNoAudio, numMissingAudio, numPartialAudio, numFullAudio) "
            + "VALUES (:level, :numTotal, :numNoAudio, :numMissingAudio, :numPartialAudio, :numFullAudio)")
    public abstract void insertOrUpdate(int level, int numTotal, int numNoAudio, int numMissingAudio, int numPartialAudio, int numFullAudio);
}
