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

package com.lazy_durtles.wk.services;

import android.content.Intent;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.jobs.Job;
import com.lazy_durtles.wk.services.JobIntentService;

import javax.annotation.Nonnull;

import static com.lazy_durtles.wk.StableIds.JOB_RUNNER_SERVICE_JOB_ID;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;
import static java.util.Objects.requireNonNull;

/**
 * An intent service for running jobs. Jobs are tasks that have to be
 * pushed to a background task and should only run one at a time,
 * but should be run as soon as reasonably possible. Jobs are not persisted,
 * they will not survice app restarts or failures.
 *
 * <p>
 *     Jobs are mostly about doing database writes in the background,
 *     and to do regular background housekeeping.
 * </p>
 */
public final class JobRunnerService extends JobIntentService {
    /**
     * Schedule a job for this service. It goes into a queue of pending jobs,
     * and will be executed as soon as the service has time for it.
     *
     * @param jobClass the class that implements the job being scheduled
     * @param jobData parameters for this job, encoded in a class-specific format
     */
    public static void schedule(final Class<? extends Job> jobClass, final String jobData) {
        safe(() -> {
            final Intent intent = new Intent(WkApplication.getInstance(), JobRunnerService.class);
            intent.putExtra("com.lazy_durtles.wk.JOB_CLASS", jobClass.getCanonicalName());
            intent.putExtra("com.lazy_durtles.wk.JOB_DATA", jobData);
            enqueueWork(WkApplication.getInstance(), JobRunnerService.class, JOB_RUNNER_SERVICE_JOB_ID, intent);
        });
    }

    /**
     * Run one job. The implementing class is instantiated, and the instance is then
     * left to do the actual work.
     *
     * @param intent the intent encoding the relevant parameters
     */
    @Override
    protected void onHandleWork(final @Nonnull Intent intent) {
        safe(() -> {
            final String jobClassName = requireNonNull(intent.getStringExtra("com.lazy_durtles.wk.JOB_CLASS"));
            final Class<? extends Job> jobClass = Class.forName(jobClassName).asSubclass(Job.class);
            final String jobData = requireNonNull(intent.getStringExtra("com.lazy_durtles.wk.JOB_DATA"));
            final Job job = jobClass
                    .getConstructor(String.class)
                    .newInstance(jobData);
            job.run();
        });
    }
}
