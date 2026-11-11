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
 * The direction of the slide animation between questions, as it used to be
 * used for fragment transitions in the legacy session UI.
 */
public enum FragmentTransitionAnimation {
    /**
     * No animation, just a static replace.
     */
    NONE,

    /**
     * Slide in from left to right.
     */
    LTR,

    /**
     * Slide in from right to left.
     */
    RTL,
}
