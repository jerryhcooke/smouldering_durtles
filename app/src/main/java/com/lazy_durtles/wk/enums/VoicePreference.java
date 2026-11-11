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
 * The preference for an audio voice.
 */
public enum VoicePreference {
    /**
     * Grab any audio file at random.
     */
    RANDOM,

    /**
     * Prefer male if available.
     */
    MALE,

    /**
     * Prefer female if available.
     */
    FEMALE,

    /**
     * Alternate between male and female as much as possible.
     */
    ALTERNATE
}
