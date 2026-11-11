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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Sort order for advanced search.
 */
@SuppressLint("NewApi")
public enum SearchSortOrder {
    TYPE("Type", true) {
        @Override
        public Comparator<Subject> getComparator(final long searchTime) {
            return Comparator.comparingInt(Subject::getTypeOrder);
        }

        @Override
        public String getTopLevelTag(final Subject subject, final long searchTime) {
            return subject.getType().name();
        }
    },

    LEVEL_TYPE("Level, Type", false) {
        @Override
        public Comparator<Subject> getComparator(final long searchTime) {
            return Comparator.comparingInt(Subject::getLevel).thenComparingInt(Subject::getTypeOrder);
        }

        @Override
        public String getTopLevelTag(final Subject subject, final long searchTime) {
            return Integer.toString(subject.getLevel());
        }
    },

    AVAILABLE_AT_TYPE("Next review, Type", false) {
        @Override
        public Comparator<Subject> getComparator(final long searchTime) {
            return Comparator.<Subject>comparingLong(subject -> {
                final long date = subject.getAvailableAt();
                return date == 0 ? 0 : Math.max(date, searchTime);
            }).thenComparingInt(Subject::getTypeOrder);
        }

        @Override
        public String getTopLevelTag(final Subject subject, final long searchTime) {
            final long date = subject.getAvailableAt();
            if (date == 0) {
                return "0";
            }
            else if (date <= searchTime) {
                return Long.toString(searchTime);
            }
            return Long.toString(date);
        }
    },

    STAGE_TYPE("SRS Stage, Type", false) {
        @Override
        public Comparator<Subject> getComparator(final long searchTime) {
            return Comparator.comparing(Subject::getSrsStage).thenComparingInt(Subject::getTypeOrder);
        }

        @Override
        public String getTopLevelTag(final Subject subject, final long searchTime) {
            return subject.getSrsStage().getAdvancedSearchTag();
        }
    };

    private final String description;
    private final boolean singleLevel;

    SearchSortOrder(final String description, final boolean singleLevel) {
        this.description = description;
        this.singleLevel = singleLevel;
    }

    /**
     * Find an instance by its description.
     *
     * @param description the description to look for
     * @param defaultValue the default value if not found
     * @return the order or defaultValue
     */
    public static SearchSortOrder forDescription(final String description, final SearchSortOrder defaultValue) {
        for (final SearchSortOrder value: values()) {
            if (value.description.equals(description)) {
                return value;
            }
        }
        return defaultValue;
    }

    /**
     * Get the list of descriptions for the values of this enum.
     *
     * @return the list of descriptions
     */
    public static List<String> descriptionValues() {
        final List<String> result = new ArrayList<>();
        for (final SearchSortOrder value: values()) {
            result.add(value.description);
        }
        return result;
    }

    /**
     * Does this order contain only a single header level?.
     *
     * @return true if it does
     */
    public final boolean isSingleLevel() {
        return singleLevel;
    }

    /**
     * The comparator that implements this sort order.
     *
     * @param searchTime the timestamp the search was started
     * @return the comparator
     */
    public abstract Comparator<Subject> getComparator(long searchTime);

    /**
     * Get the header tag for a top level item corresponding to the given subject.
     * @param subject the subject
     * @param searchTime the timestamp the search was started
     * @return the tag
     */
    public abstract String getTopLevelTag(Subject subject, long searchTime);
}
