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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lazy_durtles.wk.util;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Stripped down version of the Android AsyncTask that is now deprecated in Android 11.
 */
@SuppressWarnings({"AccessToStaticFieldLockedOnInstance", "FieldAccessedSynchronizedAndUnsynchronized", "SynchronizedMethod"})
public abstract class AsyncTask<Result> {
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int BACKUP_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;

    private static final ThreadFactory threadFactory = new ThreadFactory() {
        public Thread newThread(final Runnable r) {
            return new Thread(r, "AsyncTask #" + count.getAndIncrement());
        }
    };

    private static final AtomicInteger count = new AtomicInteger(1);

    private static @Nullable ThreadPoolExecutor backupExecutor = null;

    private static final RejectedExecutionHandler runOnSerialPolicy =
            new RejectedExecutionHandler() {
                public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
                    //noinspection SynchronizeOnThis
                    synchronized (this) {
                        if (backupExecutor == null) {
                            final LinkedBlockingQueue<Runnable> sBackupExecutorQueue = new LinkedBlockingQueue<>();
                            backupExecutor = new ThreadPoolExecutor(
                                    BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                                    TimeUnit.SECONDS, sBackupExecutorQueue, threadFactory);
                            backupExecutor.allowCoreThreadTimeOut(true);
                        }
                    }
                    backupExecutor.execute(r);
                }
            };

    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<>(), threadFactory);
        threadPoolExecutor.setRejectedExecutionHandler(runOnSerialPolicy);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    private static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private static final Executor defaultExecutor = SERIAL_EXECUTOR;
    private static @Nullable Handler sHandler = null;

    private final FutureTask<Result> future;

    private final Handler handler;

    private static final class SerialExecutor implements Executor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private @Nullable Runnable active = null;

        public synchronized void execute(final Runnable command) {
            tasks.offer(() -> {
                try {
                    command.run();
                }
                finally {
                    scheduleNext();
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        private synchronized void scheduleNext() {
            active = tasks.poll();
            if (active != null) {
                THREAD_POOL_EXECUTOR.execute(active);
            }
        }
    }

    private static synchronized Handler getMainHandler() {
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        return sHandler;
    }

    /**
     * The constructor.
     */
    protected AsyncTask() {
        handler = getMainHandler();

        future = new FutureTask<>(() -> {
            @Nullable Result result = null;
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                result = doInBackground();
                Binder.flushPendingCommands();
            }
            finally {
                postResult(result);
            }
            return result;
        });
    }

    private void postResult(final @Nullable Result result) {
        handler.post(() -> onPostExecute(result));
    }

    /**
     * This method is called on a background thread to do some work and produce a result which may be null.
     *
     * @return the result
     */
    protected abstract @Nullable Result doInBackground();

    /**
     * This method is called on the UI thread and delivers the result.
     *
     * <p>
     * If something goes wrong during execution (exception in doInBackground, thread interrupted, ...) this
     * is still called, but with a null argument.
     * </p>
     *
     * @param result the result or null
     */
    protected abstract void onPostExecute(final @Nullable Result result);

    /**
     * This method is called on the UI thread when the doInBackground method calls publishProgress.
     *
     * @param values the progress values
     */
    protected abstract void onProgressUpdate(final Object[] values);

    /**
     * Run this task in the background. Can only be called once per instance.
     */
    public final void execute() {
        defaultExecutor.execute(future);
    }

    /**
     * Run this task in the background. Can only be called once per instance.
     * Run on the supplied executor instead of the normal serial executor.
     *
     * @param exec the task executor to use
     */
    public final void executeOnExecutor(final Executor exec) {
        exec.execute(future);
    }

    /**
     * Can be called by doInBackground from the background thread. This posts the progress values
     * to the UI thread.
     *
     * @param values the values
     */
    protected final void publishProgress(final Object[] values) {
        handler.post(() -> onProgressUpdate(values));
    }
}
