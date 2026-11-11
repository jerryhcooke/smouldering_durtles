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

package com.lazy_durtles.wk.db;

import androidx.room.TypeConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lazy_durtles.wk.enums.KanjiAcceptedReadingType;
import com.lazy_durtles.wk.enums.SessionItemState;
import com.lazy_durtles.wk.enums.SessionType;
import com.lazy_durtles.wk.enums.SubjectType;
import com.lazy_durtles.wk.tasks.ApiTask;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nullable;

/**
 * Various data conversion tools.
 */
public final class Converters {
    /**
     * A singleton instance of the object mapper, preconfigured to handle timestamps as needed for the API.
     */
    private static @Nullable ObjectMapper objectMapper = null;

    /**
     * Private constructor to prevent instantiation.
     */
    private Converters() {
        //
    }

    /**
     * Get a singleton instance of the object mapper, preconfigured to handle timestamps as needed for the API.
     *
     * @return the instance
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            //noinspection NonThreadSafeLazyInitialization
            objectMapper = new ObjectMapper();
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.ENGLISH));
            objectMapper.getDateFormat().setTimeZone(TimeZone.getTimeZone("Z"));
        }
        return objectMapper;
    }

    /**
     * Convert a String to an ApiTask subclass.
     *
     * @param value the canonical name or null
     * @return the class instance or null
     */
    @TypeConverter
    public static @Nullable Class<? extends ApiTask> stringToTaskClass(final @Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return Class.forName(value).asSubclass(ApiTask.class);
        } catch (final ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Convert an ApiTask subclass to a String.
     *
     * @param value the class instance or null
     * @return the canonical name or null
     */
    @TypeConverter
    public static @Nullable String taskClassToString(final @Nullable Class<? extends ApiTask> value) {
        if (value == null) {
            return null;
        }
        return value.getCanonicalName();
    }

    /**
     * Convert a String (may be null) to an enum value of type SessionItemState.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @TypeConverter
    public static SessionItemState stringToSessionItemState(final @Nullable String value) {
        if (value == null || value.equals("NEW") || value.equals("STARTED")) {
            return SessionItemState.ACTIVE;
        }
        return SessionItemState.valueOf(value);
    }

    /**
     * Convert an enum value of type SessionItemState (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @TypeConverter
    public static String sessionItemStateToString(final @Nullable SessionItemState value) {
        if (value == null) {
            return SessionItemState.ACTIVE.name();
        }
        return value.name();
    }

    /**
     * Convert a String (may be null) to an enum value of type SessionType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @TypeConverter
    public static SessionType stringToSessionType(final @Nullable String value) {
        if (value == null) {
            return SessionType.NONE;
        }
        return SessionType.valueOf(value);
    }

    /**
     * Convert an enum value of type SessionType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @TypeConverter
    public static String sessionTypeToString(final @Nullable SessionType value) {
        if (value == null) {
            return SessionType.NONE.name();
        }
        return value.name();
    }

    /**
     * Convert a String (may be null) to an enum value of type KanjiAcceptedReadingType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @TypeConverter
    public static KanjiAcceptedReadingType stringToKanjiAcceptedReadingType(final @Nullable String value) {
        if (value == null) {
            return KanjiAcceptedReadingType.NEITHER;
        }
        return KanjiAcceptedReadingType.valueOf(value);
    }

    /**
     * Convert an enum value of type KanjiAcceptedReadingType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @TypeConverter
    public static String kanjiAcceptedReadingTypeToString(final @Nullable KanjiAcceptedReadingType value) {
        if (value == null) {
            return KanjiAcceptedReadingType.NEITHER.name();
        }
        return value.name();
    }

    /**
     * Convert a String (may be null) to an enum value of type SubjectType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @TypeConverter
    public static @Nullable SubjectType stringToSubjectType(final @Nullable String value) {
        if (value == null) {
            return null;
        }
        return SubjectType.from(value);
    }

    /**
     * Convert an enum value of type SubjectType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @TypeConverter
    public static @Nullable String subjectTypeToString(final @Nullable SubjectType value) {
        if (value == null) {
            return null;
        }
        return value.getDbTypeName();
    }
}
