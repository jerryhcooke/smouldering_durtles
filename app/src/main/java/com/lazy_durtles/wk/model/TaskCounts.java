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

package com.lazy_durtles.wk.model;

/**
 * Model for the number of active network background tasks.
 */
public final class TaskCounts {
    private final int apiCount;
    private final int audioCount;
    private final int pitchInfoCount;

    /**
     * The constructor.
     *
     * @param apiCount the total number of API tasks
     * @param audioCount the total number of audio download tasks
     * @param pitchInfoCount the total number of pitch info download tasks
     */
    public TaskCounts(final int apiCount, final int audioCount, final int pitchInfoCount) {
        this.apiCount = apiCount;
        this.audioCount = audioCount;
        this.pitchInfoCount = pitchInfoCount;
    }

    /**
     * The total number of API tasks.
     * @return the value
     */
    public int getApiCount() {
        return apiCount;
    }

    /**
     * The total number of audio download tasks.
     * @return the value
     */
    public int getAudioCount() {
        return audioCount;
    }

    /**
     * The total number of pitch info download tasks.
     * @return the value
     */
    public int getPitchInfoCount() {
        return pitchInfoCount;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TaskCounts other = (TaskCounts) obj;
        return apiCount == other.apiCount && audioCount == other.audioCount && pitchInfoCount == other.pitchInfoCount;
    }

    @Override
    public int hashCode() {
        return apiCount + audioCount + pitchInfoCount;
    }

    /**
     * Is the collection of tasks empty?.
     *
     * @return true if it is
     */
    public boolean isEmpty() {
        return apiCount == 0 && audioCount == 0 && pitchInfoCount == 0;
    }
}
