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

import static com.lazy_durtles.wk.Constants.DAY;

/**
 * A model for the user's level and how long they've been on it.
 */
public final class LevelDuration {
    private final int level;
    private final long since;
    private final String username;

    /**
     * The constructor.
     *
     * @param level the user's level
     * @param since the date of the first unlock in this level
     * @param username the user's username
     */
    public LevelDuration(final int level, final long since, final String username) {
        this.level = level;
        this.since = since;
        this.username = username;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LevelDuration other = (LevelDuration) obj;
        return level == other.level && since == other.since && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return (int) since + level + username.hashCode();
    }

    /**
     * The user's level.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the time since this level started in days.
     *
     * @return the number of days
     */
    public float getDaysAtCurrentLevel() {
        long duration = System.currentTimeMillis() - since;
        if (duration < 0) {
            duration = 0;
        }
        return duration / (float) DAY;
    }

    /**
     * The user's username.
     * @return the value
     */
    public String getUsername() {
        return username;
    }
}
