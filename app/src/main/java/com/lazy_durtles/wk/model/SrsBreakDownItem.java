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
 * Model for the database query that gathers the SRS breakdown data.
 */
public final class SrsBreakDownItem {
    private long systemId = 0;
    private long stageId = 0;
    private int count = 0;

    /**
     * The SRS system this summary is for.
     * @return the value
     */
    public long getSystemId() {
        return systemId;
    }

    /**
     * The SRS system this summary is for.
     * @param systemId the value
     */
    public void setSystemId(final long systemId) {
        this.systemId = systemId;
    }

    /**
     * The stage this summary is for.
     * @return the value
     */
    public long getStageId() {
        return stageId;
    }

    /**
     * The stage this summary is for.
     * @param stageId the value
     */
    public void setStageId(final long stageId) {
        this.stageId = stageId;
    }

    /**
     * The number of subjects for this stage.
     * @return the value
     */
    public int getCount() {
        return count;
    }

    /**
     * The number of subjects for this stage.
     * @param count the value
     */
    public void setCount(final int count) {
        this.count = count;
    }
}
