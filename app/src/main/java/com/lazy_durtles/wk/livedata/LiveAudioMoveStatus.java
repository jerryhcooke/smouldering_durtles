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

/**
 * LiveData that tracks the audio move status, i.e. the status of the
 * currently running task that moves audio files around.
 */
public final class LiveAudioMoveStatus extends ConservativeLiveData<Object> {
    /**
     * The singleton instance.
     */
    private static final LiveAudioMoveStatus instance = new LiveAudioMoveStatus();

    private boolean active = false;
    private int numDone = 0;
    private int numTotal = 0;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveAudioMoveStatus getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveAudioMoveStatus() {
        //
    }

    @Override
    protected void updateLocal() {
        postValue(new Object());
    }

    @Override
    public Object getDefaultValue() {
        return new Object();
    }

    /**
     * True if the task is active.
     * @return the value
     */
    public boolean isActive() {
        return active;
    }

    /**
     * True if the task is active.
     * @param active the value
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Number of files done.
     * @return the value
     */
    public int getNumDone() {
        return numDone;
    }

    /**
     * Number of files done.
     * @param numDone the value
     */
    public void setNumDone(final int numDone) {
        this.numDone = numDone;
    }

    /**
     * Total number of files in the task.
     * @return the value
     */
    public int getNumTotal() {
        return numTotal;
    }

    /**
     * Total number of files in the task.
     * @param numTotal the value
     */
    public void setNumTotal(final int numTotal) {
        this.numTotal = numTotal;
    }
}
