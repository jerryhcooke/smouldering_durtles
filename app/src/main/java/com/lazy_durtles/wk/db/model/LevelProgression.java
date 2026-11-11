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
 * Room entity for the level_progression table. This records the coarse-grained progression in levels.
 */
public final class LevelProgression {
    private long id = 0L;
    private long abandonedAt = 0L;
    private long completedAt = 0L;
    private long createdAt = 0L;
    private long passedAt = 0L;
    private long startedAt = 0L;
    private long unlockedAt = 0L;
    private int level = 0;

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
     * Timestamp when this level was abandoned (because of a reset), or null if not abandoned.
     * @return the value
     */
    public long getAbandonedAt() {
        return abandonedAt;
    }

    /**
     * Timestamp when this level was abandoned (because of a reset), or null if not abandoned.
     * @param abandonedAt the value
     */
    public void setAbandonedAt(final long abandonedAt) {
        this.abandonedAt = abandonedAt;
    }

    /**
     * Timestamp when this level was completed (all subjects burned), or null if not completed.
     * @return the value
     */
    public long getCompletedAt() {
        return completedAt;
    }

    /**
     * Timestamp when this level was completed (all subjects burned), or null if not completed.
     * @param completedAt the value
     */
    public void setCompletedAt(final long completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * Timestamp when this record was created.
     * @return the value
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Timestamp when this record was created.
     * @param createdAt the value
     */
    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Timestamp when this level was passed (all subjects passed), or null if not passed.
     * @return the value
     */
    public long getPassedAt() {
        return passedAt;
    }

    /**
     * Timestamp when this level was passed (all subjects passed), or null if not passed.
     * @param passedAt the value
     */
    public void setPassedAt(final long passedAt) {
        this.passedAt = passedAt;
    }

    /**
     * Timestamp when this level was started, or null if not started.
     * @return the value
     */
    public long getStartedAt() {
        return startedAt;
    }

    /**
     * Timestamp when this level was started, or null if not started.
     * @param startedAt the value
     */
    public void setStartedAt(final long startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Timestamp when this level was unlocked, or null if not unlocked.
     * @return the value
     */
    public long getUnlockedAt() {
        return unlockedAt;
    }

    /**
     * Timestamp when this level was unlocked, or null if not unlocked.
     * @param unlockedAt the value
     */
    public void setUnlockedAt(final long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    /**
     * The level this record applies to.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The level this record applies to.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * Get the timestamp since when the user reached this level.
     * Based on unlockedAt, with startedAt as fallback.
     *
     * @return the date
     */
    public long getSince() {
        if (unlockedAt != 0) {
            return unlockedAt;
        }
        if (startedAt != 0) {
            return startedAt;
        }
        return 0;
    }
}
