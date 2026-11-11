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

package com.lazy_durtles.wk.jobs;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.livedata.LiveApiState;
import com.lazy_durtles.wk.livedata.LiveBurnedItems;
import com.lazy_durtles.wk.livedata.LiveCriticalCondition;
import com.lazy_durtles.wk.livedata.LiveFirstTimeSetup;
import com.lazy_durtles.wk.livedata.LiveJlptProgress;
import com.lazy_durtles.wk.livedata.LiveJoyoProgress;
import com.lazy_durtles.wk.livedata.LiveLevelProgress;
import com.lazy_durtles.wk.livedata.LiveRecentUnlocks;
import com.lazy_durtles.wk.livedata.LiveTimeLine;
import com.lazy_durtles.wk.services.BackgroundAlarmReceiver;
import com.lazy_durtles.wk.services.BackgroundSyncWorker;

/**
 * Job that is triggered every time a setting changes value. This used to
 * trigger actions needed for the setting change to take effect.
 */
public final class SettingChangedJob extends Job {
    /**
     * The constructor.
     *
     * @param data parameters
     */
    public SettingChangedJob(final String data) {
        super(data);
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        switch (data) {
            case "api_key":
                db.propertiesDao().setApiKeyRejected(false);
                db.propertiesDao().setApiInError(false);
                db.propertiesDao().setLastApiSuccessDate(0);
                db.propertiesDao().setLastUserSyncSuccessDate(0);
                db.assertGetUserTask();
                LiveApiState.getInstance().forceUpdate();
                break;
            case "show_timeline":
            case "timeline_chart_style":
                LiveTimeLine.getInstance().update();
                break;
            case "show_level_progression":
                LiveLevelProgress.getInstance().update();
                break;
            case "show_joyo_progress":
                LiveJoyoProgress.getInstance().update();
                break;
            case "show_jlpt_progress":
                LiveJlptProgress.getInstance().update();
                break;
            case "show_recent_unlocks":
                LiveRecentUnlocks.getInstance().update();
                break;
            case "show_critical_condition":
                LiveCriticalCondition.getInstance().update();
                break;
            case "show_burned_items":
                LiveBurnedItems.getInstance().update();
                break;
            case "first_time_setup":
                LiveFirstTimeSetup.getInstance().update();
                break;
            case "enable_notifications":
                BackgroundAlarmReceiver.scheduleOrCancelAlarm();
                break;
            case "enable_background_sync":
                BackgroundSyncWorker.scheduleOrCancelWork();
                break;
            default:
                break;
        }
        houseKeeping();
    }
}
