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

import androidx.core.app.NotificationCompat;

/**
 * Priority setting for notifications.
 */
@SuppressWarnings("unused")
public enum NotificationPriority {
    MIN(1, NotificationCompat.PRIORITY_MIN),
    LOW(2, NotificationCompat.PRIORITY_LOW),
    DEFAULT(3, NotificationCompat.PRIORITY_DEFAULT),
    HIGH(4, NotificationCompat.PRIORITY_HIGH),
    MAX(5, NotificationCompat.PRIORITY_MAX);

    private final int managerImportance;
    private final int compatPriority;

    NotificationPriority(final int managerImportance, final int compatPriority) {
        this.managerImportance = managerImportance;
            this.compatPriority = compatPriority;
    }

    /**
     * The priority as an importance value from the NotificationManager class.
     * @return the value
     */
    public int getManagerImportance() {
        return managerImportance;
    }

    /**
     * The priority as a priority value from the NotificationCompat class.
     * @return the value
     */
    public int getCompatPriority() {
        return compatPriority;
    }
}
