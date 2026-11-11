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

import com.lazy_durtles.wk.enums.SearchSortOrder;
import com.lazy_durtles.wk.enums.SubjectType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;

import javax.annotation.Nullable;

/**
 * Presets for the advanced search form.
 */
public final class AdvancedSearchParameters {
    public @Nullable Integer minLevel = null;
    public @Nullable Integer maxLevel = null;
    public @Nullable Integer minFrequency = null;
    public @Nullable Integer maxFrequency = null;
    public boolean leechesOnly = false;
    public @Nullable Integer upcomingReviewLessThan = null;
    public @Nullable Integer upcomingReviewMoreThan = null;
    public @Nullable Integer incorrectAnswerWithin = null;
    public @Nullable Integer burnedLessThan = null;
    public @Nullable Integer burnedMoreThan = null;
    public SearchSortOrder sortOrder = SearchSortOrder.TYPE;
    public final Collection<Integer> starRatings = new HashSet<>();
    public final Collection<String> srsStages = new HashSet<>();
    public final Collection<SubjectType> itemTypes = EnumSet.noneOf(SubjectType.class);
    public final Collection<Integer> jlptLevels = new HashSet<>();
    public final Collection<Integer> joyoGrades = new HashSet<>();
}
