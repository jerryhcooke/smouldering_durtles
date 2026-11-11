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
 * A model for the JLPT progress.
 */
public final class JlptProgress {
    private final int[] lockedCount = new int[5];
    private final int[] prePassedCount = new int[5];
    private final int[] passedCount = new int[5];
    private final int[] burnedCount = new int[5];

    /**
     * Add one data item from the database.
     *
     * @param item the item to add
     */
    public void addItem(final JlptProgressItem item) {
        final SrsSystem system = SrsSystemRepository.getSrsSystem(item.srsSystemId);
        final SrsSystem.Stage stage = system.getStage(item.srsStage);

        if (stage.isLocked()) {
            lockedCount[item.jlptLevel-1] += item.count;
        }
        else if (stage.isCompleted()) {
            burnedCount[item.jlptLevel-1] += item.count;
        }
        else if (stage.isPassed()) {
            passedCount[item.jlptLevel-1] += item.count;
        }
        else {
            prePassedCount[item.jlptLevel-1] += item.count;
        }
    }

    /**
     * Get the count of locked subjects in a given level.
     *
     * @param level the level, 1-5
     * @return the count
     */
    public int getLocked(final int level) {
        if (level >= 1 && level <= 5) {
            return lockedCount[level-1];
        }
        return 0;
    }

    /**
     * Get the count of pre-passed subjects in a given level.
     *
     * @param level the level, 1-5
     * @return the count
     */
    public int getPrePassed(final int level) {
        if (level >= 1 && level <= 5) {
            return prePassedCount[level-1];
        }
        return 0;
    }

    /**
     * Get the count of passed subjects in a given level.
     *
     * @param level the level, 1-5
     * @return the count
     */
    public int getPassed(final int level) {
        if (level >= 1 && level <= 5) {
            return passedCount[level-1];
        }
        return 0;
    }

    /**
     * Get the count of burned subjects in a given level.
     *
     * @param level the level, 1-5
     * @return the count
     */
    public int getBurned(final int level) {
        if (level >= 1 && level <= 5) {
            return burnedCount[level-1];
        }
        return 0;
    }
}
