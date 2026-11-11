/*
 * Copyright 2019-2022 Ernst Jan Plugge <rmc@dds.nl>
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

package com.lazy_durtles.wk.livedata;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.model.AlertContext;

/**
 * LiveData that records the current context data needed for notifications and widget updates.
 */
public final class LiveAlertContext extends ConservativeLiveData<AlertContext> {
    /**
     * The singleton instance.
     */
    private static final LiveAlertContext instance = new LiveAlertContext();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveAlertContext getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveAlertContext() {
        //
    }

    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final int maxLevel = db.propertiesDao().getUserMaxLevelGranted();
        final long now = System.currentTimeMillis();
        final AlertContext ctx = db.subjectAggregatesDao().getAlertContext(maxLevel, now);
        instance.postValue(ctx);
    }

    @Override
    public AlertContext getDefaultValue() {
        final AlertContext ctx = new AlertContext();
        ctx.setNumLessons(-1);
        ctx.setNumReviews(-1);
        return ctx;
    }
}
