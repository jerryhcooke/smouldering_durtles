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

import com.lazy_durtles.wk.GlobalSettings;

/**
 * Status of the device's network connection.
 */
public enum OnlineStatus {
    /**
     * No connection at all.
     */
    NO_CONNECTION() {
        @Override
        public boolean canDownloadAudio() {
            return false;
        }

        @Override
        public boolean canCallApi() {
            return false;
        }
    },

    /**
     * Connected to an unmetered connection like WiFi.
     */
    UNMETERED() {
        @Override
        public boolean canDownloadAudio() {
            return true;
        }

        @Override
        public boolean canCallApi() {
            return true;
        }
    },

    /**
     * Connected to a metered connection like cellular data.
     */
    METERED() {
        @Override
        public boolean canDownloadAudio() {
            return GlobalSettings.Api.getNetworkRule() == NetworkRule.ALWAYS;
        }

        @Override
        public boolean canCallApi() {
            return GlobalSettings.Api.getNetworkRule() != NetworkRule.WIFI_ONLY;
        }
    };

    /**
     * Does this status allow for downloading audio files, taking into account
     * current settings?.
     *
     * @return true if allowed
     */
    public abstract boolean canDownloadAudio();

    /**
     * Does this status allow for API calls, taking into account
     * current settings?.
     *
     * @return true if allowed
     */
    public abstract boolean canCallApi();
}
