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

package com.lazy_durtles.wk.util;

import android.content.res.Resources;
import android.util.TypedValue;

import com.lazy_durtles.wk.WkApplication;

import javax.annotation.Nullable;

/**
 * Utility class for getting theme values.
 */
public final class ThemeUtil {
    private ThemeUtil() {
        //
    }

    /**
     * Get a color from the theme from an attribute ID, dereferencing as needed until a color is reached.
     *
     * @param id the attribute resource ID
     * @return the color as an ARGB int
     */
    public static int getColor(final int id) {
        final @Nullable Resources.Theme theme = WkApplication.getInstance().getTheme();
        if (theme == null) {
            return 0xFFFF0000;
        }
        final TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(id, typedValue, true);
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return typedValue.data;
        }
        return 0xFFFF0000;
    }

    /**
     * Determine if a color is light. Add up the RGB components, and if they exceed a specific level, the color is
     * considered light.
     *
     * @param color the color to check
     * @return true if it's considered light
     */
    public static boolean isLightColor(final int color) {
        final int level = (color & 0xFF) + ((color >> 8) & 0xFF) + ((color >> 16) & 0xFF);
        return level > 0x140;
    }
}
