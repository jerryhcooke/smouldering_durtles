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

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nullable;

/**
 * Model class for a 'hidden' subject meaning that is blacklisted or whitelisted.
 */
@SuppressWarnings("unused")
public final class AuxiliaryMeaning {
    private @Nullable String meaning = null;
    private @Nullable String type = null;

    /**
     * The meaning text.
     * @return the value
     */
    public @Nullable String getMeaning() {
        return meaning;
    }

    /**
     * The meaning text.
     * @param meaning the value
     */
    public void setMeaning(final @Nullable String meaning) {
        this.meaning = meaning == null ? null : meaning.intern();
    }

    /**
     * The type of meaning, "blacklist" or "whitelist".
     * @return the value
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * The type of meaning, "blacklist" or "whitelist".
     * @param type the value
     */
    public void setType(final @Nullable String type) {
        this.type = type == null ? null : type.intern();
    }

    /**
     * Check the type to see if this is a whitelist entry.
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isWhiteList() {
        return "whitelist".equals(type);
    }

    /**
     * Check the type to see if this is a blacklist entry.
     *
     * @return true if it is
     */
    @JsonIgnore
    public boolean isBlackList() {
        return "blacklist".equals(type);
    }
}
