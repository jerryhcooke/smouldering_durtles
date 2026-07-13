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
import android.os.Bundle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.DAY;
import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.Constants.MINUTE;
import static java.util.Objects.requireNonNull;

/**
 * Various generic object manipulation methods. These are mostly simple methods
 * that in later Java versions are part of the standard library.
 */
public final class ObjectSupport {
    private static final Logger LOGGER = Logger.get(ObjectSupport.class);
    private static final Random random = new Random(System.currentTimeMillis());

    private ObjectSupport() {
        //
    }

    /**
     * CharSequence.isEmpty(), but will return true if the value is null.
     *
     * @param value the string to check
     * @return true if value is empty or null
     */
    public static boolean isEmpty(final @Nullable CharSequence value) {
        return value == null || value.length() == 0;
    }

    /**
     * String.equalsIgnoreCase(), but handle null values. null equals null, but
     * null doesn't equal any other value.
     *
     * @param first the first string to compare
     * @param second the second string to compare
     * @return true of the strings are equals except for case
     */
    public static boolean isEqualIgnoreCase(final @Nullable String first, final @Nullable String second) {
        if (first == null) {
            return second == null;
        }
        if (second == null) {
            return false;
        }
        return first.equalsIgnoreCase(second);
    }

    /**
     * Object.equals(), but handle null values. null equals null, but
     * null doesn't equal any other value.
     *
     * @param first the first object to compare
     * @param second the second object to compare
     * @return true if the objects are equal
     */
    public static boolean isEqual(final @Nullable Object first, final @Nullable Object second) {
        if (first == null) {
            return second == null;
        }
        if (second == null) {
            return false;
        }
        return first.equals(second);
    }

    /**
     * COmpute a hash code for an array of objects, any of which could be null.
     *
     * @param values the objects to calculate for
     * @return the hash code
     */
    public static int hash(final Object... values) {
        return Arrays.hashCode(values);
    }

    /**
     * Return value if it's not empty and not null, otherwise return fallback.
     *
     * @param value the value to check
     * @param fallback the fallback value in case value is empty
     * @return value or fallback
     */
    public static String orElse(final @Nullable String value, final String fallback) {
        if (isEmpty(value)) {
            return fallback;
        }
        return value;
    }

    /**
     * Build a string that concatenates all elements in the iterable of parts.
     *
     * @param delimiter the delimiter between elements
     * @param prefix the prefix before the first element
     * @param suffix the suffix after the last element
     * @param parts the items to list
     * @return the resulting string
     */
    public static String join(final String delimiter, final String prefix, final String suffix, final Iterable<?> parts) {
        final StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        boolean first = true;

        for (final Object part: parts) {
            if (!first) {
                sb.append(delimiter);
            }
            sb.append(part);
            first = false;
        }

        sb.append(suffix);
        return sb.toString();
    }

    /**
     * Remove all empty/null elements from the list in-place.
     *
     * @param list the list to alter
     * @param <T> the type of elements
     */
    public static <T extends CharSequence> void removeEmpty(final List<T> list) {
        int i = 0;
        while (i < list.size()) {
            final T value = list.get(i);
            if (isEmpty(value)) {
                list.remove(i);
            }
            else {
                i++;
            }
        }
    }

    /**
     * Edit a list in-place to remove duplicate elements.
     *
     * @param list the list to clean up
     * @param <T> the type of elements in the list
     */
    public static <T> void removeDuplicates(final @Nullable List<T> list) {
        if (list == null) {
            return;
        }
        int i = 0;
        while (i < list.size()) {
            final T value = list.get(i);
            final int pos = list.indexOf(value);
            if (pos >= 0 && pos < i) {
                list.remove(i);
            }
            else {
                i++;
            }
        }
    }

    /**
     * Generate a random permutation of a list.
     *
     * @param list the list to create a permutation for
     * @param <T> the type of elements
     * @return the randomized list
     */
    public static <T> List<T> shuffle(final List<? extends T> list) {
        final List<T> todo = new ArrayList<>(list);
        final List<T> result = new ArrayList<>();
        while (!todo.isEmpty()) {
            final T t = todo.remove(random.nextInt(todo.size()));
            result.add(t);
        }
        return result;
    }

    /**
     * Return the next random int in the range 0..bound (lower inclusive, upper exclusive).
     * @param bound the upper bund for the returned number
     * @return the random number
     */
    public static int nextRandomInt(final int bound) {
        return random.nextInt(bound);
    }

    /**
     * Format a time interval in ms as an informal string such as "in 3 hours".
     *
     * @param waitTime the time to wait
     * @return description of the time to wait
     */
    public static String getWaitTimeAsInformalString(final long waitTime) {
        if (waitTime <= 0) {
            return "now";
        }
        if (waitTime < HOUR * 2) {
            return String.format(Locale.ROOT, "in %d min", (waitTime+ MINUTE/2) / MINUTE);
        }
        if (waitTime < DAY * 2) {
            return String.format(Locale.ROOT, "in %d hours", (waitTime+ HOUR/2) / HOUR);
        }
        return String.format(Locale.ROOT, "in %d days", (waitTime+ DAY/2) / DAY);
    }

    /**
     * Format a time interval in ms as a short informal string such as "3h".
     *
     * @param waitTime the time to wait
     * @return description of the time to wait
     */
    public static String getShortWaitTimeAsInformalString(final long waitTime) {
        if (waitTime <= 0) {
            return "now";
        }
        if (waitTime < HOUR) {
            return String.format(Locale.ROOT, "%dm", (waitTime + MINUTE/2) / MINUTE);
        }
        if (waitTime < DAY * 2) {
            return String.format(Locale.ROOT, "%dh", (waitTime + HOUR/2) / HOUR);
        }
        return String.format(Locale.ROOT, "%dd", (waitTime + DAY/2) / DAY);
    }

    /**
     * Create a timestamp that represents the start of the hour represented by the argument.
     *
     * @param ts the timestamp to check
     * @return the date with minute, second and millisecond set to 0
     */
    @SuppressLint("NewApi")
    public static long getTopOfHour(final long ts) {
        return ts - (ts % HOUR);
    }

    /**
     * Check if value is a Boolean instance and that instance is true.
     *
     * @param value the value to check
     * @return true if it is
     */
    public static boolean isTrue(final @Nullable Object value) {
        return value instanceof Boolean && (boolean) value;
    }

    /**
     * Run some code that produces a non-null return value, while capturing and
     * logging any exceptions thrown.
     *
     * @param defaultValue the default value to return if the supplier throws an exception
     * @param supplier the supplier that produces the result, i.e. the code to run safely
     * @param <T> the type of the return value
     * @return the result from the supplier, or defaultValue if the supplier threw an exception
     */
    public static <T> T safe(final T defaultValue, final ThrowingSupplier<? extends T> supplier) {
        try {
            return requireNonNull(supplier.get());
        }
        catch (final Exception e) {
            LOGGER.uerr(e);
            return defaultValue;
        }
    }

    /**
     * Run some code that produces a non-null return value, while capturing and
     * logging any exceptions thrown. Variant that takes a Supplier to create a defaultValue.
     *
     * @param defaultValueSupplier the default value to return if the supplier throws an exception
     * @param supplier the supplier that produces the result, i.e. the code to run safely
     * @param <T> the type of the return value
     * @return the result from the supplier, or defaultValue if the supplier threw an exception
     */
    @SuppressLint("NewApi")
    public static <T> T safe(final Supplier<T> defaultValueSupplier, final ThrowingSupplier<? extends T> supplier) {
        try {
            return requireNonNull(supplier.get());
        }
        catch (final Exception e) {
            LOGGER.uerr(e);
            return defaultValueSupplier.get();
        }
    }

    /**
     * A variation of safe() that can produce null values.
     *
     * @param supplier the supplier that produces the result, i.e. the code to run safely
     * @param <T> the type of the return value
     * @return the result from the supplier, or null if the supplier threw an exception
     */
    public static <T> @Nullable T safeNullable(final NullableThrowingSupplier<? extends T> supplier) {
        try {
            return supplier.get();
        }
        catch (final Exception e) {
            LOGGER.uerr(e);
            return null;
        }
    }

    /**
     * A variation of safe() that produces no return value.
     *
     * @param runnable the runnable that needs to be run, i.e. the code to run safely
     */
    public static void safe(final ThrowingRunnable runnable) {
        try {
            runnable.run();
        }
        catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * Run an AsyncTask with the supplied handler bodies.
     * Only call the progress and post callbacks if the supplied lifecycleowner is started or resumed.
     *
     * @param lifecycleOwner the lifecycle owner to check for callbacks
     * @param background run on the background thread, returns a result
     * @param progress run on the UI thread to report progress
     * @param post run on the UI thread to report the result
     * @param <Result> the type of the result
     */
    public static <Result> void runAsyncWithProgress(final @Nullable LifecycleOwner lifecycleOwner,
                                                           final Function<Consumer<Object[]>, Result> background,
                                                           final Consumer<Object[]> progress,
                                                           final Consumer<? super Result> post) {
        new AsyncTask<Result>() {
            @SuppressLint("NewApi")
            @Override
            public @Nullable Result doInBackground() {
                return safeNullable(() -> background.apply(this::publishProgress));
            }

            @SuppressLint("NewApi")
            @Override
            public void onProgressUpdate(final Object[] values) {
                safe(() -> {
                    if (lifecycleOwner == null || lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        progress.accept(values);
                    }
                });
            }

            @SuppressLint("NewApi")
            @Override
            public void onPostExecute(final @Nullable Result result) {
                safe(() -> {
                    if (lifecycleOwner == null || lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        post.accept(result);
                    }
                });
            }
        }.execute();
    }

    /**
     * Same as runAsyncWithProgress, but without the progress callback, and without a publisher parameter for the background worker.
     *
     * @param lifecycleOwner the lifecycle owner to check for callbacks
     * @param background run on the background thread, returns a result
     * @param post run on the UI thread to report the result
     * @param <Result> the type of the result
     */
    public static <Result> void runAsync(final @Nullable LifecycleOwner lifecycleOwner,
                                         final NullableThrowingSupplier<Result> background,
                                         final Consumer<? super Result> post) {
        new AsyncTask<Result>() {
            @Override
            public @Nullable Result doInBackground() {
                return safeNullable(background);
            }

            @SuppressLint("NewApi")
            @Override
            public void onProgressUpdate(final Object[] values) {
                //
            }

            @SuppressLint("NewApi")
            @Override
            public void onPostExecute(final @Nullable Result result) {
                safe(() -> {
                    if (lifecycleOwner == null || lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        post.accept(result);
                    }
                });
            }
        }.execute();
    }

    /**
     * Same as runAsyncWithProgress, but without the progress and result callbacks, and without a publisher parameter for the background worker.
     *
     * @param background run on the background thread
     */
    public static void runAsync(final ThrowingRunnable background) {
        new AsyncTask<Void>() {
            @Override
            public @Nullable Void doInBackground() {
                safe(background);
                return null;
            }

            @Override
            public void onProgressUpdate(final Object[] values) {
                //
            }

            @Override
            public void onPostExecute(final @Nullable Void result) {
                //
            }
        }.execute();
    }

    /**
     * Make a read-only list with one element.
     *
     * @param e1 the element
     * @param <T> the element type
     * @return the list
     */
    public static <T> List<T> listOf(final T e1) {
        return Collections.singletonList(e1);
    }

    /**
     * Make a read-only list with two or more elements.
     *
     * @param e1 the first element
     * @param e2 the second element
     * @param rest the rest of the elements in order
     * @param <T> the element type
     * @return the list
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    @SafeVarargs
    public static <T> List<T> listOf(final T e1, final T e2, final T... rest) {
        final List<T> result = new ArrayList<>(rest.length + 2);
        result.add(e1);
        result.add(e2);
        result.addAll(Arrays.asList(rest));
        return Collections.unmodifiableList(result);
    }

    /**
     * Extract a Map from bundle with the same key-value pairs for easier inspection,
     * for example to get a more useful .toString().
     *
     * @param bundle The bundle to extract
     * @return The extracted Map, null if the bundle was null
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static @Nullable Map<String, Object> extractBundle(final @Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final Map<String, Object> result = new HashMap<>();
        for (final String key: bundle.keySet()) {
            result.put(key, bundle.get(key));
        }
        return result;
    }

    /**
     * A Supplier variant that can throw exceptions.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        /**
         * Supply a value of type T.
         *
         * @return the value, may not be null
         * @throws Exception if anything went wrong
         */
        @SuppressWarnings({"RedundantThrows", "RedundantSuppression"})
        T get() throws Exception;
    }

    /**
     * A Supplier variant that can throw exceptions and return null.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface NullableThrowingSupplier<T> {
        /**
         * Supply a value of type T, possibly null.
         *
         * @return the value, may be null
         * @throws Exception if anything went wrong
         */
        @SuppressWarnings({"RedundantThrows", "RedundantSuppression"})
        @Nullable T get() throws Exception;
    }

    /**
     * A Runnable variant that can throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        /**
         * Run the action.
         *
         * @throws Exception if anything went wrong
         */
        @SuppressWarnings({"RedundantThrows", "RedundantSuppression"})
        void run() throws Exception;
    }
}
