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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.lazy_durtles.wk.Constants.DAY;
import static com.lazy_durtles.wk.util.ObjectSupport.runAsync;
import static com.lazy_durtles.wk.util.ObjectSupport.safe;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.lazy_durtles.wk.R;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.activities.MainActivity;
import com.lazy_durtles.wk.livedata.LiveAlertContext;
import com.lazy_durtles.wk.model.AlertContext;
import com.lazy_durtles.wk.util.Logger;
import com.lazy_durtles.wk.util.TextUtil;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Implementation of the app widget.
 */
public final class SessionWidgetProvider extends AppWidgetProvider {
    private static final Logger LOGGER = Logger.get(SessionWidgetProvider.class);

    private static boolean widgetUpdatedThisProcess = false;

    @SuppressLint("NewApi")
    private static @Nullable String getUpcomingMessage(final long upcoming) {
        if (upcoming == 0) {
            return null;
        }
        else if (upcoming - System.currentTimeMillis() < DAY) {
            return "More at " + TextUtil.formatShortTimeForDisplay(upcoming, false);
        }
        else {
            final float days = ((float) (upcoming - System.currentTimeMillis())) / DAY;
            return String.format(Locale.ROOT, "More in %.1fd", days);
        }
    }

    /**
     * Update all instances of the widget, using the supplied data.
     *
     * @param ctx notification context for the lesson/review counts
     */
    private static void updateWidgets(final AlertContext ctx) {
        final Context context = WkApplication.getInstance();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)) {
            return;
        }
        final AppWidgetManager manager = AppWidgetManager.getInstance(context);
        final ComponentName name = new ComponentName(context, SessionWidgetProvider.class);
        final Intent intent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        final long upcoming = ctx.getUpcomingAvailableAt();

        final int lessonCount = ctx.getNumLessons();
        final int reviewCount = ctx.getNumReviews();
        final @Nullable String upcomingMessage = getUpcomingMessage(upcoming);

        for (final int id: manager.getAppWidgetIds(name)) {
            final Bundle options = manager.getAppWidgetOptions(id);
            final int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 40);
            final int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40);

            final RemoteViews views;
            if (minWidth < 100) {
                views = new RemoteViews(context.getPackageName(), R.layout.session_appwidget_tall);
            }
            else if (minWidth < 200) {
                views = new RemoteViews(context.getPackageName(), R.layout.session_appwidget);
            }
            else {
                views = new RemoteViews(context.getPackageName(), R.layout.session_appwidget_wide);
            }
            views.setOnClickPendingIntent(R.id.widgetSurface, pendingIntent);

            if (minWidth < 100) {
                if (minHeight < 80) {
                    views.setViewVisibility(R.id.header1, GONE);
                    views.setViewVisibility(R.id.header2, GONE);

                    if (lessonCount > 0) {
                        views.setTextViewText(R.id.line1, String.format(Locale.ROOT, "L: %d", lessonCount));
                        views.setViewVisibility(R.id.line1, VISIBLE);
                    }
                    else {
                        views.setViewVisibility(R.id.line1, GONE);
                    }

                    views.setTextViewText(R.id.line2, String.format(Locale.ROOT, "R: %d", reviewCount));
                }
                else {
                    views.setViewVisibility(R.id.header2, VISIBLE);

                    if (lessonCount > 0) {
                        views.setTextViewText(R.id.header1, "Lessons:");
                        views.setViewVisibility(R.id.header1, VISIBLE);

                        views.setTextViewText(R.id.line1, Integer.toString(lessonCount));
                        views.setTextViewTextSize(R.id.line1, TypedValue.COMPLEX_UNIT_SP, 18);
                        views.setViewVisibility(R.id.line1, VISIBLE);
                    }
                    else {
                        views.setViewVisibility(R.id.header1, GONE);
                        views.setViewVisibility(R.id.line1, GONE);
                    }

                    views.setTextViewText(R.id.header2, "Reviews:");
                    views.setViewVisibility(R.id.header2, VISIBLE);

                    views.setTextViewText(R.id.line2, Integer.toString(reviewCount));
                    views.setTextViewTextSize(R.id.line2, TypedValue.COMPLEX_UNIT_SP, 18);
                }
            }
            else if (minWidth < 200) {
                if (lessonCount > 0) {
                    views.setTextViewText(R.id.header, "Lessons/Reviews:");
                    views.setTextViewText(R.id.body, String.format(Locale.ROOT, "%d/%d", lessonCount, reviewCount));
                }
                else {
                    views.setTextViewText(R.id.header, "Reviews:");
                    views.setTextViewText(R.id.body, Integer.toString(reviewCount));
                }
            }
            else if (minWidth < 250) {
                if (lessonCount > 0) {
                    views.setTextViewText(R.id.leader, "L/R");
                    views.setTextViewText(R.id.body, String.format(Locale.ROOT, "%d/%d", lessonCount, reviewCount));
                }
                else {
                    views.setTextViewText(R.id.leader, "Rev");
                    views.setTextViewText(R.id.body, Integer.toString(reviewCount));
                }
            }
            else {
                if (lessonCount > 0) {
                    views.setTextViewText(R.id.leader, "Lessons/\nReviews");
                    views.setTextViewText(R.id.body, String.format(Locale.ROOT, "%d/%d", lessonCount, reviewCount));
                }
                else {
                    views.setTextViewText(R.id.leader, "Reviews");
                    views.setTextViewText(R.id.body, Integer.toString(reviewCount));
                }
            }

            if (upcomingMessage == null) {
                views.setViewVisibility(R.id.footer, GONE);
            }
            else {
                views.setViewVisibility(R.id.footer, VISIBLE);
                views.setTextViewText(R.id.footer, upcomingMessage);
            }

            manager.updateAppWidget(id, views);
        }
    }

    /**
     * Does this device have any instances of the widget deployed?.
     *
     * @return true if it does
     */
    public static boolean hasWidgets() {
        return safe(false, () -> {
            final Context context = WkApplication.getInstance();
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)) {
                final AppWidgetManager manager = AppWidgetManager.getInstance(context);
                final ComponentName name = new ComponentName(context, SessionWidgetProvider.class);
                return manager.getAppWidgetIds(name).length > 0;
            }
            return false;
        });
    }

    /**
     * Process a background alarm event, whether triggered by an actual system alarm, or a database update that can affect
     * widgets and/or notifications. Always runs on a background thread.
     *
     * @param ctx the details for the widgets
     */
    public static void processAlarm(final AlertContext ctx) {
        safe(() -> {
            if (hasWidgets()) {
                final AlertContext lastCtx = WkApplication.getDatabase().propertiesDao().getLastWidgetAlertContext();
                if (lastCtx.getNumLessons() != ctx.getNumLessons()
                        || lastCtx.getNumReviews() != ctx.getNumReviews()
                        || lastCtx.getUpcomingAvailableAt() != ctx.getUpcomingAvailableAt()
                        || !widgetUpdatedThisProcess) {
                    LOGGER.info("Widget update starts: %s %s '%s'", ctx.getNumLessons(), ctx.getNumReviews(),
                            TextUtil.formatTimestampForApi(ctx.getUpcomingAvailableAt()));
                    widgetUpdatedThisProcess = true;
                    WkApplication.getDatabase().propertiesDao().setLastWidgetAlertContext(ctx);
                    updateWidgets(ctx);
                    LOGGER.info("Widget update ends");
                }
            }
        });
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        updateWidgets(LiveAlertContext.getInstance().get());
        runAsync(() -> LiveAlertContext.getInstance().update());
    }

    @Override
    public void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final Bundle newOptions) {
        updateWidgets(LiveAlertContext.getInstance().get());
        runAsync(() -> LiveAlertContext.getInstance().update());
    }
}
