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

package com.smouldering_durtles.wk.model;

import com.smouldering_durtles.wk.db.model.SessionItem;

import javax.annotation.Nullable;

/**
 * An event recorded during a session, shown in the session log.
 */
public final class EventItem {
    private final long timestamp;
    private final @Nullable SessionItem sessionItem;
    private final CharSequence text;

    /**
     * The constructor.
     *
     * @param sessionItem the session item this event applies to or null if not applicable
     * @param text the text for this event
     */
    public EventItem(final @Nullable SessionItem sessionItem, final CharSequence text) {
        timestamp = System.currentTimeMillis();
        this.sessionItem = sessionItem;
        this.text = text;
    }

    /**
     * The session item this event applies to or null if not applicable.
     *
     * @return the item
     */
    public @Nullable SessionItem getSessionItem() {
        return sessionItem;
    }

    /**
     * The timestamp when this event was generated.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The text description of this event.
     *
     * @return the text
     */
    public CharSequence getText() {
        return text;
    }
}
