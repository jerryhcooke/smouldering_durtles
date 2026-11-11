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

import androidx.lifecycle.LiveData;

import static com.lazy_durtles.wk.util.ObjectSupport.runAsync;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;

/**
 * A LiveData subclass that is conservative about updates. If an update is requested
 * for a subclass of this while there are no active observers, then the update is not
 * executed yet but recorded as a pending update. When an observer becomes active, the
 * pending update is executed. This defers unnecessary expensive update actions when
 * nobody is listening. If needed, an update can be forced as well.
 *
 * @param <T> the type of data stored in this instance
 */
public abstract class ConservativeLiveData<T> extends LiveData<T> {
    /**
     * True if an update is pending.
     */
    private boolean pendingUpdate = false;

    /**
     * Update the value in the LiveData from source. If there are no active observers, delay
     * the update until there are.
     */
    public final void update() {
        if (hasActiveObservers() || hasNullValue()) {
            pendingUpdate = false;
            updateLocal();
        }
        else {
            pendingUpdate = true;
        }
    }

    /**
     * Update the value in the LiveData from source. This is a forced update that will always be
     * executed immediately.
     */
    public final void forceUpdate() {
        pendingUpdate = false;
        updateLocal();
    }

    /**
     * Ping the observers by re-reporting the current value.
     */
    public final void ping() {
        if (getValue() != null) {
            postValue(getValue());
        }
    }

    /**
     * Does this instance have a null value, i.e. it hasn't seen a first update yet?.
     *
     * @return true if it does
     */
    public final boolean hasNullValue() {
        return getValue() == null;
    }

    /**
     * Get the value stored in this instance. If null, return a default value instead to avoid null responses.
     *
     * @return the value, real or default
     */
    public final T get() {
        if (getValue() == null) {
            return getDefaultValue();
        }
        return getValue();
    }

    /**
     * Post a new value for this instance.
     *
     * @param value the value
     */
    public final void post(final T value) {
        postValue(value);
    }

    @Override
    protected final void onActive() {
        safe(() -> {
            if (pendingUpdate) {
                pendingUpdate = false;
                runAsync(this::forceUpdate);
            }
        });
    }

    /**
     * Local implementation of the update. This method doesn't have to care about pending
     * updates, and it should finish by calling postValue() with the new value.
     */
    protected abstract void updateLocal();

    /**
     * Get the non-null default value to be returned by get() when no value has been posted yet.
     *
     * @return the default value
     */
    protected abstract T getDefaultValue();
}
