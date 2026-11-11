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

package com.lazy_durtles.wk.api;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.util.ObjectSupport.isEmpty;

/**
 * An enum to capture what state the API is currently in. It reflects
 * past errors, rejected keys, and things like that.
 */
public enum ApiState {
    /**
     * No API key has been configured, no communication is possible.
     */
    API_KEY_MISSING(false),

    /**
     * An API key is present, but it has been rejected. Stop communication
     * until the user chooses to retry.
     */
    API_KEY_REJECTED(false),

    /**
     * Another type of error has been received from the API. Stop communication
     * until the user chooses to retry. This also covers network problems.
     */
    ERROR(false),

    /**
     * State is unknown, an attempt to get user details must be made to progress
     * to a more useful state.
     */
    UNKNOWN(true),

    /**
     * The state used to be good but hasn't been refreshed for a while. Any call
     * can be done to refresh it.
     */
    EXPIRED(true),

    /**
     * State is okay, but user data must be fetched to refresh the state.
     */
    REFRESH_USER_DATA(true),

    /**
     * All is OK, any API call can be made.
     */
    OK(true);

    private final boolean canGetUserData;

    /**
     * Enum constructor.
     *
     * @param canGetUserData Is it okay to fetch the user endpoint in this state?
     */
    ApiState(final boolean canGetUserData) {
        this.canGetUserData = canGetUserData;
    }

    /**
     * Determine what the current API state is.
     *
     * @return the state
     */
    public static ApiState getCurrentApiState() {
        final @Nullable String apiKey = GlobalSettings.Api.getApiKey();
        if (isEmpty(apiKey)) {
            return API_KEY_MISSING;
        }

        final AppDatabase db = WkApplication.getDatabase();
        final boolean apiKeyRejected = db.propertiesDao().isApiKeyRejected();
        if (apiKeyRejected) {
            return API_KEY_REJECTED;
        }

        final boolean apiInError = db.propertiesDao().isApiInError();
        if (apiInError) {
            return ERROR;
        }

        final long lastApiSuccess = db.propertiesDao().getLastApiSuccessDate();
        if (lastApiSuccess == 0) {
            return UNKNOWN;
        }

        if (System.currentTimeMillis() - lastApiSuccess > HOUR) {
            return EXPIRED;
        }

        final long lastGetUserSuccess = db.propertiesDao().getLastUserSyncSuccessDate();
        if (lastGetUserSuccess == 0 || System.currentTimeMillis() - lastGetUserSuccess > HOUR) {
            return REFRESH_USER_DATA;
        }
        return OK;
    }

    /**
     * Does this state allow fetching the user details from the API.
     *
     * @return true if it does
     */
    public boolean canGetUserData() {
        return canGetUserData;
    }
}
