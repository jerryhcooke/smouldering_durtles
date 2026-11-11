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

package com.lazy_durtles.wk.livedata;

import com.lazy_durtles.wk.api.ApiState;

/**
 * LiveData for the current API state. The API state determines if the app has a 'healthy'
 * connection to the API right now, or is in error state, or something like that.
 * See ApiState for an overview of possible states and what they mean.
 */
public final class LiveApiState extends ConservativeLiveData<ApiState> {
    /**
     * The singleton instance.
     */
    private static final LiveApiState instance = new LiveApiState();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveApiState getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveApiState() {
        //
    }

    @Override
    protected void updateLocal() {
        instance.postValue(ApiState.getCurrentApiState());

    }

    @Override
    public ApiState getDefaultValue() {
        return ApiState.UNKNOWN;
    }
}
