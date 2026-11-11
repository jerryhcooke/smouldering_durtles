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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A model for the storage of the SRS breakdown for the dashboard.
 */
public final class SrsBreakDown {
    private final Map<SrsSystem.Stage, Integer> counts = new HashMap<>();

    /**
     * Add a count of subjects that are in a specific stage.
     *
     * @param stage the stage being counted
     * @param count the subject count
     */
    public void addCount(final SrsSystem.Stage stage, final int count) {
        @Nullable Integer n = counts.get(stage);
        if (n == null) {
            n = 0;
        }
        counts.put(stage, n + count);
    }

    /**
     * Get the number of subjects that belong in a specific bucket for the Post-60 progress bar.
     * This is for the version that breaks down Apprentice and Guru into its individual sub-stages.
     *
     * @param bucket the bucket to fill
     * @return the count
     */
    public int getPost60DeepCount(final int bucket) {
        int count = 0;
        for (final Map.Entry<SrsSystem.Stage, Integer> entry : counts.entrySet()) {
            if (entry.getKey().getPost60DeepBucket() == bucket) {
                count += entry.getValue();
            }
        }
        return count;
    }

    /**
     * Get the number of subjects that belong in a specific bucket for the Post-60 progress bar.
     * This is for the version that does not break down Apprentice and Guru into its individual sub-stages.
     *
     * @param bucket the bucket to fill
     * @return the count
     */
    public int getPost60ShallowCount(final int bucket) {
        int count = 0;
        for (final Map.Entry<SrsSystem.Stage, Integer> entry : counts.entrySet()) {
            if (entry.getKey().getPost60ShallowBucket() == bucket) {
                count += entry.getValue();
            }
        }
        return count;
    }

    /**
     * Get the number of subjects that belong in a specific bucket for the SRS breakdown view.
     *
     * @param bucket the bucket to fill
     * @return the count
     */
    public int getSrsBreakdownCount(final int bucket) {
        int count = 0;
        for (final Map.Entry<SrsSystem.Stage, Integer> entry : counts.entrySet()) {
            if (entry.getKey().isInitial()) {
                continue;
            }
            if (entry.getKey().getSrsBreakdownBucket() == bucket) {
                count += entry.getValue();
            }
        }
        return count;
    }
}
