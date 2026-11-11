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

package com.lazy_durtles.wk.api.model;

import javax.annotation.Nullable;

/**
 * Convenience interface for WK API entities to allow their ID and object to be set
 * outside of the normal ObjectMapper.
 */
public interface WaniKaniEntity {
    /**
     * The unique ID for this entity.
     *
     * @param id the ID
     */
    void setId(long id);

    /**
     * The object type for this entity.
     *
     * @param object the type
     */
    void setObject(@Nullable String object);
}
