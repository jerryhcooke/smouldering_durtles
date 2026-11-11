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

/**
 * The style of bars on the timeline bar chart.
 */
public enum TimeLineBarChartStyle {
    /**
     * Broken down by SRS stage (roughly): apprentice, guru, master, enlightened.
     */
    SRS_STAGE,

    /**
     * Broken down by next SRS stage (roughly): apprentice, guru, master, enlightened, burned.
     * This is the stage the subject will go to on a no-errors review.
     */
    NEXT_SRS_STAGE,

    /**
     * Broken down by item type: radical, kanji, vocab, kana_vocab.
     */
    ITEM_TYPE
}
