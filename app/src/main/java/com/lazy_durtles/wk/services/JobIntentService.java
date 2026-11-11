/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright 2019-2022 Ernst Jan Plugge <rmc@dds.nl>
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

package com.lazy_durtles.wk.services;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobServiceEngine;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;

import com.lazy_durtles.wk.util.AsyncTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A slightly modified version of JobIntentService now that Android's original
 * has been deprecated.
 */
@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized", "SynchronizationOnStaticField", "JavaDoc"})
public abstract class JobIntentService extends Service {
    private @Nullable CompatJobEngine mJobImpl = null;
    private @Nullable WorkEnqueuer mCompatWorkEnqueuer = null;
    private @Nullable CommandProcessor mCurProcessor = null;
    private boolean mDestroyed = false;

    private final @Nullable List<CompatWorkItem> mCompatQueue;

    private static final Object sLock = new Object();
    private static final HashMap<ComponentName, WorkEnqueuer> sClassWorkEnqueuer = new HashMap<>();

    @SuppressWarnings("NoopMethodInAbstractClass")
    private abstract static class WorkEnqueuer {
        protected final ComponentName mComponentName;

        private boolean mHasJobId = false;
        private int mJobId = 0;

        WorkEnqueuer(final ComponentName cn) {
            mComponentName = cn;
        }

        protected final void ensureJobId(final int jobId) {
            if (!mHasJobId) {
                mHasJobId = true;
                mJobId = jobId;
            } else if (mJobId != jobId) {
                throw new IllegalArgumentException("Given job ID " + jobId
                        + " is different than previous " + mJobId);
            }
        }

        abstract void enqueueWork(Intent work);

        protected void serviceStartReceived() {
        }

        protected void serviceProcessingStarted() {
        }

        protected void serviceProcessingFinished() {
        }
    }

    private interface CompatJobEngine {
        IBinder compatGetBinder();
        @Nullable GenericWorkItem dequeueWork();
    }

    @SuppressWarnings({"SynchronizeOnThis", "MethodMayBeSynchronized"})
    private static final class CompatWorkEnqueuer extends WorkEnqueuer {
        private final Context mContext;
        private final PowerManager.WakeLock mLaunchWakeLock;
        private final PowerManager.WakeLock mRunWakeLock;
        private boolean mLaunchingService = false;
        private boolean mServiceProcessing = false;

        CompatWorkEnqueuer(final Context context, final ComponentName cn) {
            super(cn);
            mContext = context.getApplicationContext();
            final PowerManager pm = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            mLaunchWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, cn.getClassName() + ":launch");
            mLaunchWakeLock.setReferenceCounted(false);
            mRunWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, cn.getClassName() + ":run");
            mRunWakeLock.setReferenceCounted(false);
        }

        @Override
        void enqueueWork(final Intent work) {
            final Intent intent = new Intent(work);
            intent.setComponent(mComponentName);
            if (mContext.startService(intent) != null) {
                synchronized (this) {
                    if (!mLaunchingService) {
                        mLaunchingService = true;
                        if (!mServiceProcessing) {
                            mLaunchWakeLock.acquire(60 * 1000);
                        }
                    }
                }
            }
        }

        @Override
        public void serviceStartReceived() {
            synchronized (this) {
                mLaunchingService = false;
            }
        }

        @Override
        public void serviceProcessingStarted() {
            synchronized (this) {
                if (!mServiceProcessing) {
                    mServiceProcessing = true;
                    mRunWakeLock.acquire(10 * 60 * 1000L);
                    mLaunchWakeLock.release();
                }
            }
        }

        @Override
        public void serviceProcessingFinished() {
            synchronized (this) {
                if (mServiceProcessing) {
                    if (mLaunchingService) {
                        mLaunchWakeLock.acquire(60 * 1000);
                    }
                    mServiceProcessing = false;
                    mRunWakeLock.release();
                }
            }
        }
    }

    @RequiresApi(26)
    private static final class JobServiceEngineImpl extends JobServiceEngine implements JobIntentService.CompatJobEngine {
        private final JobIntentService mService;
        private final Object mLock = new Object();
        private @Nullable JobParameters mParams = null;

        private final class WrapperWorkItem implements JobIntentService.GenericWorkItem {
            private final JobWorkItem mJobWork;

            WrapperWorkItem(final JobWorkItem jobWork) {
                mJobWork = jobWork;
            }

            @Override
            public Intent getIntent() {
                return mJobWork.getIntent();
            }

            @Override
            public void complete() {
                synchronized (mLock) {
                    if (mParams != null) {
                        mParams.completeWork(mJobWork);
                    }
                }
            }
        }

        JobServiceEngineImpl(final JobIntentService service) {
            super(service);
            mService = service;
        }

        @Override
        public IBinder compatGetBinder() {
            return getBinder();
        }

        @Override
        public boolean onStartJob(final JobParameters params) {
            mParams = params;
            mService.ensureProcessorRunningLocked(false);
            return true;
        }

        @Override
        public boolean onStopJob(final JobParameters params) {
            synchronized (mLock) {
                mParams = null;
            }
            return true;
        }

        @Override
        public @Nullable JobIntentService.GenericWorkItem dequeueWork() {
            final JobWorkItem work;
            synchronized (mLock) {
                if (mParams == null) {
                    return null;
                }
                work = mParams.dequeueWork();
            }
            if (work != null) {
                work.getIntent().setExtrasClassLoader(mService.getClassLoader());
                return new WrapperWorkItem(work);
            } else {
                return null;
            }
        }
    }

    @RequiresApi(26)
    private static final class JobWorkEnqueuer extends JobIntentService.WorkEnqueuer {
        private final JobInfo mJobInfo;
        private final JobScheduler mJobScheduler;

        JobWorkEnqueuer(final Context context, final ComponentName cn, final int jobId) {
            super(cn);
            ensureJobId(jobId);
            final JobInfo.Builder b = new JobInfo.Builder(jobId, mComponentName);
            mJobInfo = b.setOverrideDeadline(0).build();
            mJobScheduler = (JobScheduler) context.getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        }

        @Override
        void enqueueWork(final Intent work) {
            mJobScheduler.enqueue(mJobInfo, new JobWorkItem(work));
        }
    }

    private interface GenericWorkItem {
        Intent getIntent();
        void complete();
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    private final class CompatWorkItem implements GenericWorkItem {
        private final Intent mIntent;
        private final int mStartId;

        CompatWorkItem(final Intent intent, final int startId) {
            mIntent = intent;
            mStartId = startId;
        }

        @Override
        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public void complete() {
            stopSelf(mStartId);
        }
    }

    @SuppressWarnings("NestedAssignment")
    private final class CommandProcessor extends AsyncTask<Void> {
        @Override
        protected @Nullable Void doInBackground() {
            @Nullable GenericWorkItem work;

            while ((work = dequeueWork()) != null) {
                onHandleWork(work.getIntent());
                work.complete();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final @Nullable Void aVoid) {
            processorFinished();
        }

        @Override
        protected void onProgressUpdate(final Object[] values) {
            //
        }
    }

    protected JobIntentService() {
        if (Build.VERSION.SDK_INT >= 26) {
            mCompatQueue = null;
        } else {
            mCompatQueue = new ArrayList<>();
        }
    }

    @SuppressWarnings({"DesignForExtension", "CanBeFinal"})
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            mJobImpl = new JobServiceEngineImpl(this);
            mCompatWorkEnqueuer = null;
        } else {
            mJobImpl = null;
            final ComponentName cn = new ComponentName(this, getClass());
            mCompatWorkEnqueuer = getWorkEnqueuer(this, cn, false, 0);
        }
    }

    @SuppressWarnings({"DesignForExtension", "CanBeFinal"})
    @Override
    public int onStartCommand(final @Nullable Intent intent, final int flags, final int startId) {
        if (mCompatQueue != null) {
            if (mCompatWorkEnqueuer != null) {
                mCompatWorkEnqueuer.serviceStartReceived();
            }
            synchronized (mCompatQueue) {
                mCompatQueue.add(new CompatWorkItem(intent != null ? intent : new Intent(), startId));
                ensureProcessorRunningLocked(true);
            }
            return START_REDELIVER_INTENT;
        } else {
            return START_NOT_STICKY;
        }
    }

    @SuppressWarnings({"DesignForExtension", "CanBeFinal"})
    @Override
    public @Nullable IBinder onBind(final Intent intent) {
        if (mJobImpl != null) {
            return mJobImpl.compatGetBinder();
        } else {
            return null;
        }
    }

    @SuppressWarnings({"DesignForExtension", "CanBeFinal"})
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompatQueue != null) {
            synchronized (mCompatQueue) {
                mDestroyed = true;
                if (mCompatWorkEnqueuer != null) {
                    mCompatWorkEnqueuer.serviceProcessingFinished();
                }
            }
        }
    }

    protected static void enqueueWork(final Context context, final Class<?> cls, final int jobId, final Intent work) {
        enqueueWork(context, new ComponentName(context, cls), jobId, work);
    }

    private static void enqueueWork(final Context context, final ComponentName component, final int jobId, final @Nullable Intent work) {
        if (work == null) {
            throw new IllegalArgumentException("work must not be null");
        }
        synchronized (sLock) {
            final WorkEnqueuer we = getWorkEnqueuer(context, component, true, jobId);
            we.ensureJobId(jobId);
            we.enqueueWork(work);
        }
    }

    private static WorkEnqueuer getWorkEnqueuer(final Context context, final ComponentName cn, final boolean hasJobId, final int jobId) {
        @Nullable WorkEnqueuer we = sClassWorkEnqueuer.get(cn);
        if (we == null) {
            if (Build.VERSION.SDK_INT >= 26) {
                if (!hasJobId) {
                    throw new IllegalArgumentException("Can't be here without a job id");
                }
                we = new JobWorkEnqueuer(context, cn, jobId);
            } else {
                we = new CompatWorkEnqueuer(context, cn);
            }
            sClassWorkEnqueuer.put(cn, we);
        }
        return we;
    }

    protected abstract void onHandleWork(final Intent intent);

    private void ensureProcessorRunningLocked(final boolean reportStarted) {
        if (mCurProcessor == null) {
            mCurProcessor = new CommandProcessor();
            if (mCompatWorkEnqueuer != null && reportStarted) {
                mCompatWorkEnqueuer.serviceProcessingStarted();
            }
            mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void processorFinished() {
        if (mCompatQueue != null) {
            synchronized (mCompatQueue) {
                mCurProcessor = null;
                if (!mCompatQueue.isEmpty()) {
                    ensureProcessorRunningLocked(false);
                } else if (!mDestroyed) {
                    if (mCompatWorkEnqueuer != null) {
                        mCompatWorkEnqueuer.serviceProcessingFinished();
                    }
                }
            }
        }
    }

    @Nullable
    private GenericWorkItem dequeueWork() {
        if (mJobImpl != null) {
            return mJobImpl.dequeueWork();
        } else if (mCompatQueue != null) {
            synchronized (mCompatQueue) {
                if (mCompatQueue.isEmpty()) {
                    return null;
                } else {
                    return mCompatQueue.remove(0);
                }
            }
        }
        return null;
    }
}