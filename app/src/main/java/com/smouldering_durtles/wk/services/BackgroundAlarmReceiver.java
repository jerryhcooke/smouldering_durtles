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

package com.smouldering_durtles.wk.services;

import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.Constants.MINUTE;
import static com.smouldering_durtles.wk.util.ObjectSupport.getTopOfHour;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.StableIds;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.livedata.LiveAlertContext;
import com.smouldering_durtles.wk.model.AlertContext;
import com.smouldering_durtles.wk.util.Logger;

import javax.annotation.Nullable;

/**
 * The alarm receiver that gets triggered once per hour, and is responsible for
 * notifications, widgets and background sync.
 */
public final class BackgroundAlarmReceiver extends BroadcastReceiver {
    private static final Logger LOGGER = Logger.get(BackgroundAlarmReceiver.class);

    /**
     * Based on user settings, is an hourly background alarm required?.
     *
     * @return true if it is
     */
    private static boolean isAlarmRequired() {
        return GlobalSettings.Other.getEnableNotifications() || SessionWidgetProvider.hasWidgets();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        safe(() -> {
            LOGGER.info("Background alarm pre19 received");
            runAsync(() -> LiveAlertContext.getInstance().update());
        });
        safe(BackgroundAlarmReceiver::scheduleOrCancelAlarm);
    }

    /**
     * Schedule the alarm for notifications. It is scheduled for the top
     * of each hour, but depending on circumstances, the delivery of the alarm
     * can be delayed a bit by the device.
     */
    private static void scheduleAlarm() {
        final long nextTrigger = getTopOfHour(System.currentTimeMillis()) + HOUR;
        final @Nullable AlarmManager alarmManager = (AlarmManager) WkApplication.getInstance().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            final Intent intent = new Intent(WkApplication.getInstance(), BackgroundAlarmReceiver.class);
            final int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(WkApplication.getInstance(),
                    StableIds.BACKGROUND_ALARM_REQUEST_CODE_1, intent, flags);
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent);
        }
    }

    /**
     * Cancel the notification alarm.
     */
    private static void cancelAlarm() {
        final @Nullable AlarmManager alarmManager = (AlarmManager) WkApplication.getInstance().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            final Intent intent = new Intent(WkApplication.getInstance(), BackgroundAlarmReceiver.class);
            final int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(WkApplication.getInstance(),
                    StableIds.BACKGROUND_ALARM_REQUEST_CODE_1, intent, flags);
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Schedule or cancel depending on user settings.
     */
    public static void scheduleOrCancelAlarm() {
        safe(() -> {
            if (isAlarmRequired()) {
                scheduleAlarm();
            } else {
                cancelAlarm();
            }
        });
    }

    /**
     * Process a background alarm event, whether triggered by an actual system alarm, or a database update that can affect
     * widgets and/or notifications.
     *
     * @param ctx Data class for alert parameters.
     */
    public static void processAlarm(final AlertContext ctx) {
        runAsync(() -> {
            if (isAlarmRequired()) {
                if (ctx.getNumLessons() < 0 || ctx.getNumReviews() < 0) {
                    return;
                }
                safe(() -> NotificationWorker.processAlarm(ctx));
                safe(() -> SessionWidgetProvider.processAlarm(ctx));
            }
        });
    }
}
