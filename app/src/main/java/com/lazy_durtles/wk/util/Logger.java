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

package com.lazy_durtles.wk.util;

/**
 * A logging proxy class that captures a class instance and delegates to the DbLogger singleton.
 */
public final class Logger {
    private final Class<?> clas;

    private Logger(final Class<?> clas) {
        this.clas = clas;
    }

    /**
     * Create an instance connected to a specific class.
     *
     * @param clas the class
     * @return the logger
     */
    public static Logger get(final Class<?> clas) {
        return new Logger(clas);
    }

    /**
     * Log at debug level.
     *
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    @SuppressWarnings("unused")
    public void debug(final String format, final Object... values) {
        DbLogger.logDebug(clas, "DEBUG: " + format, values);
    }

    /**
     * Log at debug level.
     *
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    public void info(final String format, final Object... values) {
        DbLogger.logInfo(clas, format, values);
    }

    /**
     * Log at error level, including a full stacktrace. This is for expected errors which will be flagged as such.
     *
     * @param throwable the exception to show the stacktrace for
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    public void error(final Throwable throwable, final String format, final Object... values) {
        DbLogger.logExpectedError(clas, throwable, format, values);
    }

    /**
     * Log at error level, including a full stacktrace. This is for unexpected errors which will be flagged as such.
     *
     * @param throwable the exception to show the stacktrace for
     */
    public void uerr(final Throwable throwable) {
        DbLogger.logUnexpectedError(clas, throwable);
    }
}
