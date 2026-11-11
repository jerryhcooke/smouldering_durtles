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

import android.graphics.Typeface;

/**
 * Wrapper class that contains a typeface and assorted information needed for rendering.
 */
public final class TypefaceConfiguration {
    /**
     * The configuration matching the default typeface.
     */
    public static final TypefaceConfiguration DEFAULT = new TypefaceConfiguration(Typeface.DEFAULT);

    private final Typeface typeface;
    private final int paddingPercTop;
    private final int paddingPercBottom;
    private final int paddingPercLeft;
    private final int paddingPercRight;

    /**
     * Constructor with padding.
     *
     * @param typeface The typeface to render.
     * @param paddingPercTop Padding at the top as a percentage.
     * @param paddingPercBottom Padding at the bottom as a percentage.
     * @param paddingPercLeft Padding at the left as a percentage.
     * @param paddingPercRight Padding at the right as a percentage.
     */
    public TypefaceConfiguration(final Typeface typeface, final int paddingPercTop, final int paddingPercBottom,
                                 final int paddingPercLeft, final int paddingPercRight) {
        this.typeface = typeface;
        this.paddingPercTop = paddingPercTop;
        this.paddingPercBottom = paddingPercBottom;
        this.paddingPercLeft = paddingPercLeft;
        this.paddingPercRight = paddingPercRight;
    }

    /**
     * Constructor without padding.
     *
     * @param typeface The typeface to render.
     */
    public TypefaceConfiguration(final Typeface typeface) {
        this(typeface, 0, 0, 0, 0);
    }

    /**
     * The typeface to render.
     * @return the value
     */
    public Typeface getTypeface() {
        return typeface;
    }

    /**
     * Padding at the top as a percentage.
     * @return the value
     */
    public int getPaddingPercTop() {
        return paddingPercTop;
    }

    /**
     * Padding at the bottom as a percentage.
     * @return the value
     */
    public int getPaddingPercBottom() {
        return paddingPercBottom;
    }

    /**
     * Padding at the left as a percentage.
     * @return the value
     */
    public int getPaddingPercLeft() {
        return paddingPercLeft;
    }

    /**
     * Padding at the right as a percentage.
     * @return the value
     */
    public int getPaddingPercRight() {
        return paddingPercRight;
    }
}
