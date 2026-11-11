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

/**
 * LiveData that tracks the progress of the current session.
 *
 * <p>
 *     This is also a degenerate LiveData instance that mostly just exists to wake
 *     up observers, they will typically get the actual session state by just grabbing
 *     it from the Session singleton.
 * </p>
 */
public final class LiveSessionProgress extends ConservativeLiveData<Object> {
    /**
     * The singleton instance.
     */
    private static final LiveSessionProgress instance = new LiveSessionProgress();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveSessionProgress getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveSessionProgress() {
        //
    }

    @Override
    protected void updateLocal() {
        postValue(new Object());
    }

    @Override
    public Object getDefaultValue() {
        return new Object();
    }
}
