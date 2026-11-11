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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lazy_durtles.wk.components.WaniKaniApiDateDeserializer;
import com.lazy_durtles.wk.components.WaniKaniApiDateSerializer;

import javax.annotation.Nullable;

/**
 * Model class to represent a user record in the API.
 */
@SuppressWarnings("unused")
public final class ApiUser {
    private @Nullable String id = null;
    private int level = 0;
    @JsonProperty("max_level_granted_by_subscription") private int maxLevelGrantedBySubscription = 0;
    @JsonSerialize(using = WaniKaniApiDateSerializer.class)
    @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
    @JsonProperty("current_vacation_started_at") private long currentVacationStartedAt = 0;
    private @Nullable ApiSubscription subscription = null;
    private @Nullable String username = null;

    /**
     * Unique ID for the user that stays the same even if the user changes their username.
     * @return the value
     */
    public @Nullable String getId() {
        return id;
    }

    /**
     * Unique ID for the user that stays the same even if the user changes their username.
     * @param id the value
     */
    public void setId(final @Nullable String id) {
        this.id = id;
    }

    /**
     * The current level of the user.
     * @return the value
     */
    public int getLevel() {
        return level;
    }

    /**
     * The current level of the user.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * Unused but tracked as backup just in case; maximum level granted to the user by their subscription.
     * @return the value
     */
    public int getMaxLevelGrantedBySubscription() {
        return maxLevelGrantedBySubscription;
    }

    /**
     * Unused but tracked as backup just in case; maximum level granted to the user by their subscription.
     * @param maxLevelGrantedBySubscription the value
     */
    public void setMaxLevelGrantedBySubscription(final int maxLevelGrantedBySubscription) {
        this.maxLevelGrantedBySubscription = maxLevelGrantedBySubscription;
    }

    /**
     * Timestamp when the user went on vacation, or 0 if the user is not on vacation.
     * @return the value
     */
    public long getCurrentVacationStartedAt() {
        return currentVacationStartedAt;
    }

    /**
     * Timestamp when the user went on vacation, or 0 if the user is not on vacation.
     * @param currentVacationStartedAt the value
     */
    public void setCurrentVacationStartedAt(final long currentVacationStartedAt) {
        this.currentVacationStartedAt = currentVacationStartedAt;
    }

    /**
     * The user's subscription details.
     * @return the value
     */
    public @Nullable ApiSubscription getSubscription() {
        return subscription;
    }

    /**
     * The user's subscription details.
     * @param subscription the value
     */
    public void setSubscription(final @Nullable ApiSubscription subscription) {
        this.subscription = subscription;
    }

    /**
     * The user's username.
     * @return the value
     */
    public @Nullable String getUsername() {
        return username;
    }

    /**
     * The user's username.
     * @param username the value
     */
    public void setUsername(final @Nullable String username) {
        this.username = username;
    }
}
