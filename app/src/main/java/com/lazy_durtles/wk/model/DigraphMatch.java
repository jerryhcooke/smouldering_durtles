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

import java.util.Locale;

import javax.annotation.Nonnull;

/**
 * Details of a reading question match that highlight an incorrect answer that is only different
 * in the use of small vs. regular kana, most notably the common beginner mistake of niyuu vs. nyuu
 * for the 入 (Enter) kanji.
 */
public final class DigraphMatch {
    private final char regularKana;
    private final char smallKana;

    /**
     * The constructor.
     *
     * @param regularKana the regular kana that was mixed up with the small one
     * @param smallKana the small kana that was mixed up with the regular one
     */
    public DigraphMatch(final char regularKana, final char smallKana) {
        this.regularKana = regularKana;
        this.smallKana = smallKana;
    }

    /**
     * The regular kana that was mixed up with the small one.
     *
     * @return the kana
     */
    public char getRegularKana() {
        return regularKana;
    }

    /**
     * The small kana that was mixed up with the regular one.
     *
     * @return the kana
     */
    public char getSmallKana() {
        return smallKana;
    }

    @Override
    public @Nonnull String toString() {
        return String.format(Locale.ROOT, "Digraph:%c/%c", regularKana, smallKana);
    }
}
