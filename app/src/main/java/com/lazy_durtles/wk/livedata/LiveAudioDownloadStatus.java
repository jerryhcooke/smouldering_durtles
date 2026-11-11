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

package com.lazy_durtles.wk.livedata;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.AudioDownloadStatus;

import java.util.Collections;
import java.util.List;

/**
 * LiveData that tracks the audio download status.
 */
public final class LiveAudioDownloadStatus extends ConservativeLiveData<List<AudioDownloadStatus>> {
    /**
     * The singleton instance.
     */
    private static final LiveAudioDownloadStatus instance = new LiveAudioDownloadStatus();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveAudioDownloadStatus getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveAudioDownloadStatus() {
        //
    }

    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        postValue(db.audioDownloadStatusDao().getAll());
    }

    @Override
    public List<AudioDownloadStatus> getDefaultValue() {
        return Collections.emptyList();
    }
}
