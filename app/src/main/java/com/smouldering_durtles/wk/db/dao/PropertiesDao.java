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

package com.smouldering_durtles.wk.db.dao;

import android.annotation.SuppressLint;

import androidx.room.Dao;
import androidx.room.Query;

import com.smouldering_durtles.wk.db.Converters;
import com.smouldering_durtles.wk.db.model.Property;
import com.smouldering_durtles.wk.enums.QuestionType;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.model.AlertContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.MONTH;
import static com.smouldering_durtles.wk.db.Converters.sessionTypeToString;
import static com.smouldering_durtles.wk.db.Converters.stringToSessionType;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqualIgnoreCase;
import static com.smouldering_durtles.wk.util.ObjectSupport.runAsync;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * DAO for properties: various key/value records that record useful data that doesn't count as settings.
 */
@Dao
public abstract class PropertiesDao {
    private @Nullable Map<String, String> properties = null;

    /**
     * Room-generated method: get all properties.
     *
     * @return the list
     */
    @Query("SELECT * FROM properties ORDER BY name")
    public abstract List<Property> getAll();

    /**
     * Preload the property cache.
     */
    @SuppressLint("NewApi")
    public final void preload() {
        if (properties != null) {
            return;
        }
        final Semaphore semaphore = new Semaphore(0);
        new Thread(() -> {
            safe(() -> {
                final Map<String, String> map = new HashMap<>();
                getAll().forEach(property -> map.put(property.name, property.value));
                properties = map;
            });
            semaphore.release();
        }).start();
        safe(semaphore::acquire);
    }

    private @Nullable String getProperty(final String name) {
        if (properties == null) {
            preload();
        }
        return properties.get(name);
    }

    /**
     * Room-generated method: set a property.
     *
     * @param name the name of the property
     * @param value the value of the property
     */
    @Query("INSERT OR REPLACE INTO properties (name, value) VALUES (:name, :value)")
    protected abstract void setPropertyHelper(final String name, final String value);

    private void setProperty(final String name, final String value) {
        if (properties == null) {
            preload();
        }
        properties.put(name, value);
        runAsync(() -> setPropertyHelper(name, value));
    }

    /**
     * Room-generated method: delete a property.
     *
     * @param name the name of the property
     */
    @Query("DELETE FROM properties WHERE name = :name")
    protected abstract void deletePropertyHelper(final String name);

    /**
     * Delete a property by name, if it exists.
     *
     * @param name the property's name
     */
    public final void deleteProperty(final String name) {
        if (properties == null) {
            preload();
        }
        properties.remove(name);
        runAsync(() -> deletePropertyHelper(name));
    }

    private boolean getBooleanProperty(final String name) {
        final @Nullable String value = getProperty(name);

        if (value == null) {
            return false;
        }

        return isEqualIgnoreCase(value, "true");
    }

    private void setBooleanProperty(final String name, final boolean value) {
        setProperty(name, Boolean.toString(value));
    }

    private int getIntegerProperty(final String name) {
        final @Nullable String value = getProperty(name);

        if (isEmpty(value)) {
            return 0;
        }

        return safe(0, () -> Integer.parseInt(value, 10));
    }

    private void setIntegerProperty(final String name, final int value) {
        setProperty(name, Integer.toString(value));
    }

    private long getLongProperty(final String name, final long delta) {
        final @Nullable String value = getProperty(name);

        if (isEmpty(value)) {
            return 0;
        }

        long longValue = safe(0L, () -> Long.parseLong(value, 10));
        if (longValue >= delta) {
            longValue -= delta;
        }

        return longValue;
    }

    private void setLongProperty(final String name, final long value) {
        setProperty(name, Long.toString(value));
    }

    /**
     * Was the API key rejected by WK?.
     *
     * @return true if it was.
     */
    public final boolean isApiKeyRejected() {
        return getBooleanProperty("api_key_rejected");
    }

    /**
     * Was the API key rejected by WK?.
     *
     * @param value true if it was.
     */
    public final void setApiKeyRejected(final boolean value) {
        setBooleanProperty("api_key_rejected", value);
    }

    /**
     * Did the last API call result in an error?.
     *
     * @return true if it did.
     */
    public final boolean isApiInError() {
        return getBooleanProperty("api_in_error");
    }

    /**
     * Did the last API call result in an error?.
     *
     * @param value true if it did.
     */
    public final void setApiInError(final boolean value) {
        setBooleanProperty("api_in_error", value);
    }

    /**
     * When was the last successful API call?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastApiSuccessDate() {
        return getLongProperty("last_api_success", 0);
    }

    /**
     * When was the last successful API call?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastApiSuccessDate(final long value) {
        setLongProperty("last_api_success", value);
    }

    /**
     * When was the last successful user sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastUserSyncSuccessDate() {
        return getLongProperty("last_user_sync_success", 0);
    }

    /**
     * When was the last successful user sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastUserSyncSuccessDate(final long value) {
        setLongProperty("last_user_sync_success", value);
    }

    /**
     * When was the last successful subject sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    public final long getLastSubjectSyncSuccessDate(final long delta) {
        return getLongProperty("last_subject_sync_success", delta);
    }

    /**
     * When was the last successful subject sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastSubjectSyncSuccessDate(final long value) {
        setLongProperty("last_subject_sync_success", value);
    }

    /**
     * When was the last successful assignment sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    public final long getLastAssignmentSyncSuccessDate(final long delta) {
        return getLongProperty("last_assignment_sync_success", delta);
    }

    /**
     * When was the last successful assignment sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastAssignmentSyncSuccessDate(final long value) {
        setLongProperty("last_assignment_sync_success", value);
    }

    /**
     * When was the last successful review statistic sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    public final long getLastReviewStatisticSyncSuccessDate(final long delta) {
        return getLongProperty("last_review_statistic_sync_success", delta);
    }

    /**
     * When was the last successful review statistic sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastReviewStatisticSyncSuccessDate(final long value) {
        setLongProperty("last_review_statistic_sync_success", value);
    }

    /**
     * When was the last successful study material sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    public final long getLastStudyMaterialSyncSuccessDate(final long delta) {
        return getLongProperty("last_study_material_sync_success", delta);
    }

    /**
     * When was the last successful study material sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastStudyMaterialSyncSuccessDate(final long value) {
        setLongProperty("last_study_material_sync_success", value);
    }

    /**
     * When was the last successful SRS system sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastSrsSystemSyncSuccessDate() {
        return getLongProperty("last_srs_system_sync_success", 0);
    }

    /**
     * When was the last successful SRS system sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastSrsSystemSyncSuccessDate(final long value) {
        setLongProperty("last_srs_system_sync_success", value);
    }

    /**
     * When was the last successful level progression sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    public final long getLastLevelProgressionSyncSuccessDate(final long delta) {
        return getLongProperty("last_level_progression_sync_success", delta);
    }

    /**
     * When was the last successful level progression sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastLevelProgressionSyncSuccessDate(final long value) {
        setLongProperty("last_level_progression_sync_success", value);
    }

    /**
     * When was the last successful summary sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastSummarySyncSuccessDate() {
        return getLongProperty("last_summary_sync_success", 0);
    }

    /**
     * When was the last successful summary sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastSummarySyncSuccessDate(final long value) {
        setLongProperty("last_summary_sync_success", value);
    }

    /**
     * When was the last time there was a check for audio that needed to be downloaded?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastAudioScanDate() {
        return getLongProperty("last_audio_scan", 0);
    }

    /**
     * When was the last time there was a check for audio that needed to be downloaded?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastAudioScanDate(final long value) {
        setLongProperty("last_audio_scan", value);
    }

    /**
     * When was the last time there was a check for pitch info that needed to be downloaded?.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastPitchInfoScanDate() {
        return getLongProperty("last_pitch_info_scan", 0);
    }

    /**
     * When was the last time there was a check for pitch info that needed to be downloaded?.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastPitchInfoScanDate(final long value) {
        setLongProperty("last_pitch_info_scan", value);
    }

    /**
     * The revision of the JPDB frequency dictionary that has been imported into the local cache.
     *
     * @return the revision or null if none imported yet
     */
    public final @Nullable String getJpdbRevision() {
        return getProperty("jpdb_revision");
    }

    /**
     * The revision of the JPDB frequency dictionary that has been imported into the local cache.
     *
     * @param value the revision
     */
    public final void setJpdbRevision(final String value) {
        setProperty("jpdb_revision", value);
    }

    /**
     * Timestamp of the last failed attempt to download the JPDB dictionary.
     *
     * @return the timestamp, or 0 if none
     */
    public final long getLastJpdbDownloadFailure() {
        return getLongProperty("last_jpdb_download_failure", 0);
    }

    /**
     * Timestamp of the last failed attempt to download the JPDB dictionary.
     *
     * @param value the timestamp, or 0 if none
     */
    public final void setLastJpdbDownloadFailure(final long value) {
        setLongProperty("last_jpdb_download_failure", value);
    }

    /**
     * The Nadeshiko API key that was rejected with HTTP 401, so it's not retried until it changes.
     *
     * @return the rejected key, or null if none
     */
    public final @Nullable String getRejectedNadeshikoKey() {
        return getProperty("rejected_nadeshiko_key");
    }

    /**
     * The Nadeshiko API key that was rejected with HTTP 401, so it's not retried until it changes.
     *
     * @param value the rejected key
     */
    public final void setRejectedNadeshikoKey(final String value) {
        setProperty("rejected_nadeshiko_key", value);
    }

    /**
     * When was the last time a notification was posted? This is the top of the hour of the last update.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastNotificationUpdate() {
        return getLongProperty("last_notification_update", 0);
    }

    /**
     * When was the last time a notification was posted? This is the top of the hour of the last update.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastNotificationUpdate(final long value) {
        setLongProperty("last_notification_update", value);
    }

    /**
     * When was the last time a background sync was run? This is the top of the hour of the last update.
     *
     * @return the timestamp, or 0 if not known
     */
    public final long getLastBackgroundSync() {
        return getLongProperty("last_background_sync", 0);
    }

    /**
     * When was the last time a background sync was run? This is the top of the hour of the last update.
     *
     * @param value the timestamp, or 0 if not known
     */
    public final void setLastBackgroundSync(final long value) {
        setLongProperty("last_background_sync", value);
    }

    /**
     * The max level granted by the user's subscription.
     *
     * @return the level, or 3 if not known
     */
    public final int getUserMaxLevelGranted() {
        final long end = getLongProperty("user_max_level_granted_checked", -MONTH);
        if (end == 0 || System.currentTimeMillis() > end) {
            return 3;
        }
        return getIntegerProperty("user_max_level_granted");
    }

    /**
     * The max level granted by the user's subscription.
     *
     * @param value the level, or 3 if not known
     */
    public final void setUserMaxLevelGranted(final int value) {
        setIntegerProperty("user_max_level_granted", value);
        setLongProperty("user_max_level_granted_checked", System.currentTimeMillis());
    }

    /**
     * The user's current level.
     *
     * @return the level, or 0 if not known
     */
    public final int getUserLevel() {
        return getIntegerProperty("user_level");
    }

    /**
     * The user's current level.
     *
     * @param value the level, or 0 if not known
     */
    public final void setUserLevel(final int value) {
        setIntegerProperty("user_level", value);
    }

    /**
     * The user's ID.
     *
     * @return the user ID as a UUID
     */
    public final @Nullable String getUserId() {
        return getProperty("user_id");
    }

    /**
     * The user's ID.
     *
     * @param value the user ID as a UUID
     */
    public final void setUserId(final String value) {
        setProperty("user_id", value);
    }

    /**
     * The user's username.
     *
     * @return the username
     */
    public final @Nullable String getUsername() {
        return getProperty("username");
    }

    /**
     * The user's username.
     *
     * @param value the username
     */
    public final void setUsername(final String value) {
        setProperty("username", value);
    }

    /**
     * Is the user in vacation mode?.
     *
     * @return true if they are
     */
    public final boolean getVacationMode() {
        return getBooleanProperty("vacation_mode");
    }

    /**
     * Is the user in vacation mode?.
     *
     * @param value true if they are
     */
    public final void setVacationMode(final boolean value) {
        setBooleanProperty("vacation_mode", value);
    }

    /**
     * The type of the current session.
     *
     * @return the type
     */
    public final SessionType getSessionType() {
        return stringToSessionType(getProperty("session_type"));
    }

    /**
     * The type of the current session.
     *
     * @param sessionType the type
     */
    public final void setSessionType(final SessionType sessionType) {
        setProperty("session_type", sessionTypeToString(sessionType));
    }

    /**
     * Is the on/kun setting active for this session?.
     *
     * @return true if it is
     */
    public final boolean getSessionOnkun() {
        return getBooleanProperty("session_onkun");
    }

    /**
     * Is the on/kun setting active for this session?.
     *
     * @param sessionOnkun true if it is
     */
    public final void setSessionOnkun(final boolean sessionOnkun) {
        setBooleanProperty("session_onkun", sessionOnkun);
    }

    /**
     * The version of the currently loaded reference data.
     *
     * @return the version
     */
    public final int getReferenceDataVersion() {
        return getIntegerProperty("reference_data_version");
    }

    /**
     * The version of the currently loaded reference data.
     *
     * @param referenceDataVersion the version
     */
    public final void setReferenceDataVersion(final int referenceDataVersion) {
        setIntegerProperty("reference_data_version", referenceDataVersion);
    }

    /**
     * Has a notification been set?.
     *
     * @return true if it has
     */
    public final boolean getNotificationSet() {
        return getBooleanProperty("notification_set");
    }

    /**
     * Has a notification been set?.
     *
     * @param notificationSet true if it has
     */
    public final void setNotificationSet(final boolean notificationSet) {
        setBooleanProperty("notification_set", notificationSet);
    }

    /**
     * Has a forced late refresh of all core models been requested?.
     *
     * <p>
     *     This is requested if a subject passes or if the user changes level.
     * </p>
     *
     * @return true if it has
     */
    public final boolean getForceLateRefresh() {
        return getBooleanProperty("force_late_refresh");
    }

    /**
     * Has a forced late refresh of all core models been requested?.
     *
     * <p>
     *     This is requested if a subject passes or if the user changes level.
     * </p>
     *
     * @param forceLateRefresh  true if it has
     */
    public final void setForceLateRefresh(final boolean forceLateRefresh) {
        setBooleanProperty("force_late_refresh", forceLateRefresh);
    }

    /**
     * Has a sync reminder been set?.
     *
     * @return true if it has
     */
    public final boolean getSyncReminder() {
        return getBooleanProperty("sync_reminder");
    }

    /**
     * Has a sync reminder been set?.
     *
     * @param syncReminder true if it has
     */
    public final void setSyncReminder(final boolean syncReminder) {
        setBooleanProperty("sync_reminder", syncReminder);
    }

    /**
     * Get the session item ID (subject ID) for the currently shown item in the session.
     *
     * @return the ID
     */
    public final long getCurrentItemId() {
        return getLongProperty("current_item_id", 0);
    }

    /**
     * Get the session item ID (subject ID) for the currently shown item in the session.
     *
     * @param currentItemId the ID
     */
    public final void setCurrentItemId(final long currentItemId) {
        setLongProperty("current_item_id", currentItemId);
    }

    /**
     * Get the question type for the currently shown question in the session.
     *
     * @return the type
     */
    public final QuestionType getCurrentQuestionType() {
        final @Nullable String value = getProperty("current_question_type");
        if (value == null) {
            return QuestionType.WANIKANI_RADICAL_NAME;
        }
        try {
            return QuestionType.valueOf(value);
        }
        catch (final Exception e) {
            return QuestionType.WANIKANI_RADICAL_NAME;
        }
    }

    /**
     * Get the question type for the currently shown question in the session.
     *
     * @param currentQuestionType the type
     */
    public final void setCurrentQuestionType(final QuestionType currentQuestionType) {
        setProperty("current_question_type", currentQuestionType.toString());
    }

    /**
     * The last used AlertContext for notifications.
     *
     * @return the context, or a dummy instance if not present
     */
    public final AlertContext getLastNotificationAlertContext() {
        return safe(() -> {
            final AlertContext ctx = new AlertContext();
            ctx.setNumLessons(-1);
            ctx.setNumReviews(-1);
            return ctx;
        }, () -> {
            final @Nullable String s = getProperty("last_notification_alertcontext");
            if (s == null) {
                final AlertContext ctx = new AlertContext();
                ctx.setNumLessons(-1);
                ctx.setNumReviews(-1);
                return ctx;
            }
            return Converters.getObjectMapper().readValue(s, AlertContext.class);
        });
    }

    /**
     * The last used AlertContext for notifications.
     *
     * @param value the context
     */
    public final void setLastNotificationAlertContext(final AlertContext value) {
        safe(() -> setProperty("last_notification_alertcontext", Converters.getObjectMapper().writeValueAsString(value)));
    }

    /**
     * The last used AlertContext for widgets.
     *
     * @return the context, or a dummy instance if not present
     */
    public final AlertContext getLastWidgetAlertContext() {
        return safe(() -> {
            final AlertContext ctx = new AlertContext();
            ctx.setNumLessons(-1);
            ctx.setNumReviews(-1);
            return ctx;
        }, () -> {
            final @Nullable String s = getProperty("last_widget_alertcontext");
            if (s == null) {
                final AlertContext ctx = new AlertContext();
                ctx.setNumLessons(-1);
                ctx.setNumReviews(-1);
                return ctx;
            }
            return Converters.getObjectMapper().readValue(s, AlertContext.class);
        });
    }

    /**
     * The last used AlertContext for widgets.
     *
     * @param value the context
     */
    public final void setLastWidgetAlertContext(final AlertContext value) {
        safe(() -> setProperty("last_widget_alertcontext", Converters.getObjectMapper().writeValueAsString(value)));
    }

    /**
     * Has the audio been muted?.
     *
     * @return true if it has
     */
    public final boolean getIsMuted() {
        return getBooleanProperty("is_muted");
    }

    /**
     * Has the audio been muted?.
     *
     * @param value true if it has
     */
    public final void setIsMuted(final boolean value) {
        setBooleanProperty("is_muted", value);
    }

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    public final boolean getMigrationDoneAnkiSplit() {
        return getBooleanProperty("migration_done_anki_split");
    }

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    public final void setMigrationDoneAnkiSplit(final boolean value) {
        setBooleanProperty("migration_done_anki_split", value);
    }

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    public final boolean getMigrationDoneAudio2() {
        return getBooleanProperty("migration_done_audio2");
    }

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    public final void setMigrationDoneAudio2(final boolean value) {
        setBooleanProperty("migration_done_audio2", value);
    }

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    public final boolean getMigrationDoneNotif() {
        return getBooleanProperty("migration_done_notif");
    }

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    public final void setMigrationDoneNotif(final boolean value) {
        setBooleanProperty("migration_done_notif", value);
    }

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    public final boolean getMigrationDoneDump() {
        return getBooleanProperty("migration_done_dump");
    }

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    public final void setMigrationDoneDump(final boolean value) {
        setBooleanProperty("migration_done_dump", value);
    }
}
