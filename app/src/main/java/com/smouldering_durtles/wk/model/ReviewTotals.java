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

package com.smouldering_durtles.wk.model;

/**
 * A model for the aggregate lesson and answer totals over all subjects.
 */
public final class ReviewTotals {
    /**
     * The number of subjects with at least one lesson done.
     */
    public int lessonsCompleted = 0;
    /**
     * The total number of correct meaning answers.
     */
    public int meaningCorrect = 0;
    /**
     * The total number of incorrect meaning answers.
     */
    public int meaningIncorrect = 0;
    /**
     * The total number of correct reading answers.
     */
    public int readingCorrect = 0;
    /**
     * The total number of incorrect reading answers.
     */
    public int readingIncorrect = 0;
}
