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

import static com.lazy_durtles.wk.util.ObjectSupport.getTopOfHour;
import static com.lazy_durtles.wk.util.ObjectSupport.runAsync;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.R;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.activities.MainActivity;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.enums.NotificationUpdateFrequency;
import com.lazy_durtles.wk.livedata.LiveTimeLine;
import com.lazy_durtles.wk.model.AlertContext;
import com.lazy_durtles.wk.util.Logger;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The worker that sets or cancels notifications as needed.
 */
public final class NotificationWorker {
    private static final Logger LOGGER = Logger.get(NotificationWorker.class);

    private NotificationWorker() {
        //
    }

    private static void postNotification(final boolean needsSound, final AlertContext ctx) {
        final String title;
        final String text;
        if (ctx.getNumLessons() > 0) {
            title = "New lessons and reviews available";
            text = String.format(Locale.ROOT, "%d lessons and %d reviews available", ctx.getNumLessons(), ctx.getNumReviews());
        }
        else {
            title = "New reviews available";
            text = String.format(Locale.ROOT, "%d reviews available", ctx.getNumReviews());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String name = "NewReviewsChannel";
            final String description = "New reviews available";
            final int importance = GlobalSettings.Other.getNotificationPriority().getManagerImportance();
            @SuppressLint("WrongConstant") final NotificationChannel channel = new NotificationChannel(name, name, importance);
            channel.setDescription(description);
            final @Nullable NotificationManager notificationManager = WkApplication.getInstance().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        final Intent intent2 = new Intent(WkApplication.getInstance(), MainActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = 0;
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(WkApplication.getInstance(), 0, intent2, flags);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(WkApplication.getInstance(), "NewReviewsChannel");
        builder.setSmallIcon(R.drawable.ic_stat_name);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setPriority(GlobalSettings.Other.getNotificationPriority().getCompatPriority());
        builder.setCategory(GlobalSettings.Other.getNotificationCategory().getCompatCategory());
        builder.setContentIntent(pendingIntent);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setOnlyAlertOnce(!needsSound);
        builder.setAutoCancel(true);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(WkApplication.getInstance());
        // Without POST_NOTIFICATIONS (Android 13+, requested by MainActivity), notify()
        // would throw a SecurityException; skip posting instead.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(WkApplication.getInstance(), android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }

        final AppDatabase db = WkApplication.getDatabase();
        db.propertiesDao().setNotificationSet(true);

        runAsync(() -> LiveTimeLine.getInstance().update());
    }

    private static void cancelNotification() {
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(WkApplication.getInstance());
        notificationManager.cancel(1);
        final AppDatabase db = WkApplication.getDatabase();
        db.propertiesDao().setNotificationSet(false);
    }

    private static void postOrCancelNotification(final boolean needsSound, final AlertContext ctx) {
        safe(() -> {
            if (ctx.getNumLessons() == 0 && ctx.getNumReviews() == 0) {
                cancelNotification();
            }
            else {
                postNotification(needsSound, ctx);
            }
        });
    }

    private static void processAlarmHelper(final AlertContext ctx) {
        if (GlobalSettings.Other.getEnableNotifications()) {
            final AppDatabase db = WkApplication.getDatabase();
            final NotificationUpdateFrequency frequency = GlobalSettings.Other.getNotificationUpdateFrequency();
            final long topOfHour1 = getTopOfHour(System.currentTimeMillis());
            boolean needsPost = false;
            boolean needsSound = false;
            if (db.propertiesDao().getNotificationSet() && ctx.getNumLessons() == 0 && ctx.getNumReviews() == 0) {
                needsPost = true;
            }
            final AlertContext lastCtx = db.propertiesDao().getLastNotificationAlertContext();
            if (ctx.getNewestAvailableAt() > lastCtx.getNewestAvailableAt()) {
                needsPost = true;
                needsSound = true;
            }
            if (ctx.getNumLessons() == 0 && ctx.getNumReviews() == 0 && (lastCtx.getNumLessons() != 0 || lastCtx.getNumReviews() != 0)) {
                needsPost = true;
            }
            switch (frequency) {
                case ONLY_NEW_REVIEWS:
                    break;
                case ONCE_PER_HOUR: {
                    final long topOfHour2 = db.propertiesDao().getLastNotificationUpdate();
                    if (topOfHour1 != topOfHour2 && (lastCtx.getNumLessons() != ctx.getNumLessons()
                            || lastCtx.getNumReviews() != ctx.getNumReviews())) {
                        needsPost = true;
                    }
                    break;
                }
                case CONTINUOUSLY:
                    if (lastCtx.getNumLessons() != ctx.getNumLessons()
                            || lastCtx.getNumReviews() != ctx.getNumReviews()) {
                        needsPost = true;
                    }
                    break;
            }
            if (needsPost) {
                LOGGER.info("Notification update starts: %s %s", ctx.getNumLessons(), ctx.getNumReviews());
                db.propertiesDao().setLastNotificationUpdate(topOfHour1);
                db.propertiesDao().setLastNotificationAlertContext(ctx);
                postOrCancelNotification(needsSound, ctx);
                LOGGER.info("Notification update ends");
            }
        }
    }

    /**
     * Process a background alarm event, whether triggered by an actual system alarm, or a database update that can affect
     * widgets and/or notifications. Always runs on a background thread.
     *
     * @param ctx the details for the notifications
     */
    public static void processAlarm(final AlertContext ctx) {
        safe(() -> processAlarmHelper(ctx));
    }

    public static void triggerTestNotification() {
        // Create a dummy AlertContext for testing
        AlertContext testContext = new AlertContext();
        testContext.setNumLessons(5);  // Set some test values
        testContext.setNumReviews(10); // Set some test values

        // Call the method you want to test
        postNotification(true, testContext);
    }
}
