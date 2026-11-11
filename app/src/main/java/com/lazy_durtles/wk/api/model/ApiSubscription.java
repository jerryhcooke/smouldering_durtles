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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class for subscription information in the user record.
 */
@SuppressWarnings("unused")
public final class ApiSubscription {
    @JsonProperty("max_level_granted") private int maxLevelGranted = 0;

    /**
     * The maximum level granted by the user's current subscription.
     * @return the value
     */
    public int getMaxLevelGranted() {
        return maxLevelGranted;
    }

    /**
     * The maximum level granted by the user's current subscription.
     * @param maxLevelGranted the value
     */
    public void setMaxLevelGranted(final int maxLevelGranted) {
        this.maxLevelGranted = maxLevelGranted;
    }
}
