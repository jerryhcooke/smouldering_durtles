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
public enum NotificationCategory {
    CALL(NotificationCompat.CATEGORY_CALL),
    NAVIGATION(NotificationCompat.CATEGORY_NAVIGATION),
    MESSAGE(NotificationCompat.CATEGORY_MESSAGE),
    EMAIL(NotificationCompat.CATEGORY_EMAIL),
    EVENT(NotificationCompat.CATEGORY_EVENT),
    PROMO(NotificationCompat.CATEGORY_PROMO),
    ALARM(NotificationCompat.CATEGORY_ALARM),
    PROGRESS(NotificationCompat.CATEGORY_PROGRESS),
    SOCIAL(NotificationCompat.CATEGORY_SOCIAL),
    ERROR(NotificationCompat.CATEGORY_ERROR),
    TRANSPORT(NotificationCompat.CATEGORY_TRANSPORT),
    SYSTEM(NotificationCompat.CATEGORY_SYSTEM),
    SERVICE(NotificationCompat.CATEGORY_SERVICE),
    REMINDER(NotificationCompat.CATEGORY_REMINDER),
    RECOMMENDATION(NotificationCompat.CATEGORY_RECOMMENDATION),
    STATUS(NotificationCompat.CATEGORY_STATUS);

    private final String compatCategory;

    NotificationCategory(final String compatCategory) {
            this.compatCategory = compatCategory;
    }

    /**
     * The category as a category value from the NotificationCompat class.
     * @return the value
     */
    public String getCompatCategory() {
        return compatCategory;
    }
}
