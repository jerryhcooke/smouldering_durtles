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
 * Enum to indicate the type of reading (on'yomi or kun'yomi) that is required to
 * answer a reading question for a kanji subject.
 */
public enum KanjiAcceptedReadingType {
    /**
     * The answer must be on'yomi.
     */
    ONYOMI,

    /**
     * The answer must be kun'yomi.
     */
    KUNYOMI,

    /**
     * The answer is neither (not a kanji).
     */
    NEITHER,

    /**
     * The answer could be either on or kun.
     */
    BOTH
}
