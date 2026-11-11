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
 * The order to present reviews/self-study in. Each value will produce a comparator on demand.
 */
@SuppressLint("NewApi")
@SuppressWarnings("unused")
public enum ReviewOrder {
    /**
     * Completely random order. I.e., the comparator always returns 0 and doesn't impose an order.
     */
    SHUFFLE() {
        @Override
        public Comparator<Subject> getComparator() {
            return (o1, o2) -> 0;
        }
    },

    /**
     * By level.
     */
    LEVEL() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel);
        }
    },

    /**
     * By type.
     */
    TYPE() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * By SRS stage.
     */
    SRS() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparing(Subject::getSrsStage);
        }
    },

    /**
     * By level, then by type.
     */
    LEVEL_THEN_TYPE() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel).thenComparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * By level, then by SRS stage.
     */
    LEVEL_THEN_SRS() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel).thenComparing(Subject::getSrsStage);
        }
    },

    /**
     * By type, then by level.
     */
    TYPE_THEN_LEVEL() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder).thenComparingInt(Subject::getLevel);
        }
    },

    /**
     * By type, then by SRS stage.
     */
    TYPE_THEN_SRS() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder).thenComparing(Subject::getSrsStage);
        }
    },

    /**
     * By SRS stage, then by type.
     */
    SRS_THEN_TYPE() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparing(Subject::getSrsStage).thenComparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * By SRS stage, then by level.
     */
    SRS_THEN_LEVEL() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparing(Subject::getSrsStage).thenComparingInt(Subject::getLevel);
        }
    },

    /**
     * By level, then by type, then by SRS stage.
     */
    LEVEL_THEN_TYPE_THEN_SRS() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel)
                    .thenComparingInt(Subject::getTypeOrder)
                    .thenComparing(Subject::getSrsStage);
        }
    },

    /**
     * By level, then by SRS stage, then by type.
     */
    LEVEL_THEN_SRS_THEN_TYPE() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getLevel)
                    .thenComparing(Subject::getSrsStage)
                    .thenComparingInt(Subject::getTypeOrder);
        }
    },

    /**
     * By type, then by level, then by SRS stage.
     */
    TYPE_THEN_LEVEL_THEN_SRS() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder)
                    .thenComparingInt(Subject::getLevel)
                    .thenComparing(Subject::getSrsStage);
        }
    },

    /**
     * By type, then by SRS stage, then by level.
     */
    TYPE_THEN_SRS_THEN_LEVEL() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparingInt(Subject::getTypeOrder)
                    .thenComparing(Subject::getSrsStage)
                    .thenComparingInt(Subject::getLevel);
        }
    },

    /**
     * By SRS stage, then by type, then by level.
     */
    SRS_THEN_TYPE_THEN_LEVEL() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparing(Subject::getSrsStage)
                    .thenComparingInt(Subject::getTypeOrder)
                    .thenComparingInt(Subject::getLevel);
        }
    },

    /**
     * By SRS stage, then by level, then by type.
     */
    SRS_THEN_LEVEL_THEN_TYPE() {
        @Override
        public Comparator<Subject> getComparator() {
            return Comparator.comparing(Subject::getSrsStage)
                    .thenComparingInt(Subject::getLevel)
                    .thenComparingInt(Subject::getTypeOrder);
        }
    };

    /**
     * The comparator to compare two subjects according to this order.
     *
     * @return the comparator
     */
    public abstract Comparator<Subject> getComparator();
}
