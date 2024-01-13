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

package com.smouldering_durtles.wk.util;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.smouldering_durtles.wk.BuildConfig;
import com.smouldering_durtles.wk.Constants;
import com.smouldering_durtles.wk.Identification;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.LogRecord;
import com.smouldering_durtles.wk.db.model.Property;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;

import static com.smouldering_durtles.wk.Constants.MINUTE;
import static com.smouldering_durtles.wk.Constants.SECOND;

/**
 * A logging class that will log both to the Android logger and a circular log in the database.
 */
public final class DbLogger {
    private static @Nullable DbLogger instance = null;

    private final AppDatabase db;
    private int mark = -1;

    /**
     * Are we currently on the main thread?. If so, we need to shunt the
     * database work off to a background thread.
     *
     * @return true if this is the main thread
     */
    private static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Get the simple class name of a class, without a package specifier.
     * Make sure there is always something sensible to return, even for anonymous
     * classes that don't have a simple name.
     *
     * @param clas the class to name
     * @return the name
     */
    public static String getSimpleClassName(final Class<?> clas) {
        final String name = clas.getName();
        final int pos = name.lastIndexOf('.');
        if (pos < 0) {
            return name;
        }
        return name.substring(pos+1);
    }

    /**
     * Initialize the singleton instance.
     *
     * @param db the database to log into
     */
    public static void initializeInstance(final AppDatabase db) {
        if (instance == null) {
            //noinspection NonThreadSafeLazyInitialization
            instance = new DbLogger(db);
        }
    }

    /**
     * The singleton constructor.
     *
     * @param db the database to log to
     */
    private DbLogger(final AppDatabase db) {
        this.db = db;
    }

    /**
     * Log at debug level.
     *
     * @param clas class to tag the log event with
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    public static void logDebug(final Class<?> clas, final String format, final Object... values) {
        try {
            final String message = String.format(format, values);
            if (BuildConfig.DEBUG) {
                Log.d(getSimpleClassName(clas), message);
            }
            if (instance == null) {
                return;
            }
            instance.writeRecord(System.currentTimeMillis(), clas, message, 20_000);
        } catch (final Exception e) {
            Log.e("CircularLogFile", "Exception while logging", e);
        }
    }

    /**
     * Log at info level.
     *
     * @param clas class to tag the log event with
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    public static void logInfo(final Class<?> clas, final String format, final Object... values) {
        try {
            final String message = String.format(format, values);
            if (BuildConfig.DEBUG) {
                Log.i(getSimpleClassName(clas), message);
            }
            if (instance == null) {
                return;
            }
            instance.writeRecord(System.currentTimeMillis(), clas, message, 20_000);
        } catch (final Exception e) {
            Log.e("CircularLogFile", "Exception while logging", e);
        }
    }

    /**
     * Log at error level, including a full stacktrace.
     *
     * @param clas class to tag the log event with
     * @param throwable the exception to show the stacktrace for
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    private static void logErrorException(final Class<?> clas, final Throwable throwable,
                                                       final String format, final Object... values) {
        try {
            final String message = String.format(format, values);
            Log.e(getSimpleClassName(clas), message, throwable);
            if (instance == null) {
                return;
            }
            instance.writeRecord(System.currentTimeMillis(), clas, message, 20_000);
            final StringWriter writer = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            printWriter.close();
            writer.flush();
            writer.close();
            instance.writeRecord(System.currentTimeMillis(), clas, writer.toString(), 50_000);
        } catch (final Exception e) {
            Log.e("CircularLogFile", "Exception while logging", e);
        }
    }

    /**
     * Log at error level, including a full stacktrace. This is for unexpected errors which will be flagged as such.
     *
     * @param clas class to tag the log event with
     * @param throwable the exception to show the stacktrace for
     */
    public static void logUnexpectedError(final Class<?> clas, final Throwable throwable) {
        logErrorException(clas, throwable, "Unexpected error");
    }

    /**
     * Log at error level, including a full stacktrace. This is for expected errors which will be flagged as such.
     *
     * @param clas class to tag the log event with
     * @param throwable the exception to show the stacktrace for
     * @param format String.format() formatting string
     * @param values String.format() formatting values
     */
    public static void logExpectedError(final Class<?> clas, final Throwable throwable, final String format, final Object... values) {
        logErrorException(clas, throwable, format, values);
    }

    /**
     * Write a logging entry to the database.
     *
     * @param timestamp the timestamp for this entry
     * @param clas the class responsible for this entry
     * @param logMessage the message
     * @param maxLength the maximum length of the message to log
     */
    private void writeRecord(final long timestamp, final Class<?> clas, final String logMessage, final int maxLength) {
        if (isOnMainThread()) {
            new AsyncTask<Void>() {
                @Override
                public Void doInBackground() {
                    try {
                        writeRecord(timestamp, clas, logMessage, maxLength);
                    } catch (final Exception e) {
                        logUnexpectedError(getClass(), e);
                    }
                    return null;
                }

                @Override
                public void onPostExecute(@Nullable final Void result) {
                    //
                }

                @Override
                public void onProgressUpdate(final Object[] values) {
                    //
                }
            }.execute();
            return;
        }

        String message = logMessage;
        if (message.length() > maxLength) {
            message = message.substring(0, maxLength);
        }

        final LogRecord record = new LogRecord();
        record.timestamp = timestamp;
        record.tag = getSimpleClassName(clas);
        record.message = message;
        record.length = message.length();
        db.logRecordDao().insert(record);
        if (mark >= 0) {
            mark += message.length();
        }
    }

    /**
     * Write the contents of the log to an output stream.
     *
     * @param stream the stream to write to
     * @throws IOException if the writing fails for some reason
     */
    @SuppressLint("NewApi")
    private void writeLogContents(final OutputStream stream) throws IOException {
        stream.write(String.format("%s version %s, username %s\n", Identification.APP_NAME, BuildConfig.VERSION_NAME,
                db.propertiesDao().getUsername()).getBytes("UTF-8"));
        long id = 0;
        while (true) {
            final @Nullable LogRecord record = db.logRecordDao().getNext(id);
            if (record == null) {
                return;
            }
            final ZonedDateTime dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp), ZoneId.systemDefault());
            final String data = String.format("%s %s %s\n", dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), record.tag, record.message);
            stream.write(data.getBytes("UTF-8"));
            id = record.id;
        }
    }

    /**
     * Remove old logging records from the database if it has become too large.
     */
    public static void trim() {
        if (instance != null) {
            instance.trimImpl();
        }
    }

    private void trimImpl() {
        if (mark < 0) {
            mark = db.logRecordDao().getTotalSize();
        }
        if (mark > Constants.LOG_FILE_SIZE) {
            final int excess = mark - Constants.LOG_FILE_SIZE / 2;
            db.logRecordDao().deleteOldest(excess);
        }
        mark = db.logRecordDao().getTotalSize();
    }
}
