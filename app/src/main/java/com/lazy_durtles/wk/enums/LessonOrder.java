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

package com.lazy_durtles.wk.enums;

import android.annotation.SuppressLint;

import com.lazy_durtles.wk.db.model.Subject;

import java.util.Comparator;

/**
 * The order to present lessons in. Each value will produce a comparator on demand.
 */
@SuppressWarnings("unused")
@SuppressLint("NewApi")
public enum LessonOrder {
    /**
     * Completely random order. I.e., the comparator always returns 0 and doesn't impose an order.
     */
    SHUFFLE(true) {
        @Override
        public Comparator<Subject> getComparator() {
            return (o1, o2) -> 0;
        }
    },

    /**
     * First by level, then by type.
     */
    LEVEL_THEN_TYPE(false) {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel).thenComparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * First radicals, then for the rest first by level, then by type.
     */
    RADICALS_THEN_LEVEL_THEN_TYPE(false) {
        @Override
        public Comparator<Subject> getComparator() {
            final Comparator<Subject> c = (o1, o2) -> Boolean.compare(o2.getType().isRadical(), o1.getType().isRadical());
            return c.thenComparingInt(Subject::getLevel).thenComparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * First by type, then by level.
     */
    TYPE_THEN_LEVEL(false) {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder).thenComparingInt(Subject::getLevel);
        }
    },

    /**
     * First by level, then shuffle.
     */
    LEVEL_THEN_SHUFFLE(true) {
        @Override
        public Comparator<Subject> getComparator () {
            return Comparator.comparingInt(Subject::getLevel);
        }
    },

    /**
     * First by type, then shuffle.
     */
    TYPE_THEN_SHUFFLE(true) {
        @Override
        public Comparator<Subject> getComparator () {
            return Comparator.comparingInt(Subject::getTypeOrder);
        }
    };

    private final boolean shuffle;

    LessonOrder(final boolean shuffle) {
        this.shuffle = shuffle;
    }

    /**
     * Create a comparator to compare two subjects according to this order.
     *
     * @return the comparator
     */
    public abstract Comparator<Subject> getComparator();

    /**
     * Does this order require shuffling before applying the sorting order?.
     *
     * @return true if it does
     */
    public boolean isShuffle() {
        return shuffle;
    }
}
