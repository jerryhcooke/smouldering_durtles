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

import com.lazy_durtles.wk.enums.SessionState;

/**
 * LiveData that tracks the state of the current session, mostly used to bounce the
 * user around to the right activities at the right time.
 *
 * <p>
 *     This is also a degenerate LiveData instance that mostly just exists to wake
 *     up observers, they will typically get the actual session state by just grabbing
 *     it from the Session singleton.
 * </p>
 */
public final class LiveSessionState extends ConservativeLiveData<SessionState> {
    /**
     * The singleton instance.
     */
    private static final LiveSessionState instance = new LiveSessionState();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveSessionState getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveSessionState() {
        //
    }

    @Override
    protected void updateLocal() {
        //
    }

    @Override
    public SessionState getDefaultValue() {
        return SessionState.INACTIVE;
    }
}
