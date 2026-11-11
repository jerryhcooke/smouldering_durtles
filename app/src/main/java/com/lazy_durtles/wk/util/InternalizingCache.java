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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Internalizing cache to eliminate multiple object instances with the same value.
 *
 * @param <T> the type to internalize
 */
public final class InternalizingCache<T> {
    private final Map<T, T> store = new HashMap<>();

    /**
     * Internalize a value. Either return a cached instance with the same value,
     * or put this value into the cache and return it.
     *
     * @param value the value to internalize
     * @return the value or an equivalent object, null if the argument is null
     */
    public @Nullable T internalize(final @Nullable T value) {
        if (value == null) {
            return null;
        }

        final @Nullable T result = store.get(value);

        if (result == null) {
            store.put(value, value);
            return value;
        }

        return result;
    }
}
