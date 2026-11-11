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
 * The status of an item (subject) in a session.
 */
public enum SessionItemState {
    /**
     * This item is active; questions may have been answered already, but it hasn't been finished yet.
     */
    ACTIVE,

    /**
     * This item has been finished, but the result hasn't been reported yet.
     * This state can only occur if the delay result upload setting is on.
     */
    PENDING,

    /**
     * This item has been finished. If it's for a lesson or review session, the background
     * task to report the result to the API has been scheduled, but may not have been
     * executed yet.
     */
    REPORTED,

    /**
     * This item has been abandoned and won't be reported.
     */
    ABANDONED
}
