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

import java.util.ArrayList;
import java.util.List;

/**
 * Export/import entity for search presets.
 */
public final class StarRatingsExport {
    private List<Long> stars1 = new ArrayList<>();
    private List<Long> stars2 = new ArrayList<>();
    private List<Long> stars3 = new ArrayList<>();
    private List<Long> stars4 = new ArrayList<>();
    private List<Long> stars5 = new ArrayList<>();

    /**
     * The subject IDs that have this many stars.
     *
     * @return the list of IDs
     */
    public List<Long> getStars1() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return stars1;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @param stars1 the list of IDs
     */
    public void setStars1(final List<Long> stars1) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.stars1 = stars1;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @return the list of IDs
     */
    public List<Long> getStars2() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return stars2;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @param stars2 the list of IDs
     */
    public void setStars2(final List<Long> stars2) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.stars2 = stars2;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @return the list of IDs
     */
    public List<Long> getStars3() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return stars3;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @param stars3 the list of IDs
     */
    public void setStars3(final List<Long> stars3) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.stars3 = stars3;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @return the list of IDs
     */
    public List<Long> getStars4() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return stars4;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @param stars4 the list of IDs
     */
    public void setStars4(final List<Long> stars4) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.stars4 = stars4;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @return the list of IDs
     */
    public List<Long> getStars5() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return stars5;
    }

    /**
     * The subject IDs that have this many stars.
     *
     * @param stars5 the list of IDs
     */
    public void setStars5(final List<Long> stars5) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.stars5 = stars5;
    }
}
