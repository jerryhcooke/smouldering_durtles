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

import com.lazy_durtles.wk.enums.SubjectType;

/**
 * A model for the database query to collect level progress information.
 */
public final class LevelProgressItem {
    private int level = 0;
    private SubjectType type = SubjectType.WANIKANI_RADICAL;
    private int count = 0;

    /**
     * The level for this summary.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The level for this summary.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * The type for this summary.
     * @return the value
     */
    public SubjectType getType() {
        return type;
    }

    /**
     * The type for this summary.
     * @param type the value
     */
    public void setType(final SubjectType type) {
        this.type = type;
    }

    /**
     * The number of subjects represented by this summary.
     * @return the value
     */
    public int getCount() {
        return count;
    }

    /**
     * The number of subjects represented by this summary.
     * @param count the value
     */
    public void setCount(final int count) {
        this.count = count;
    }
}
