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

package com.smouldering_durtles.wk.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.dao.AudioDownloadStatusDao;
import com.smouldering_durtles.wk.db.dao.LevelProgressionDao;
import com.smouldering_durtles.wk.db.dao.LogRecordDao;
import com.smouldering_durtles.wk.db.dao.PropertiesDao;
import com.smouldering_durtles.wk.db.dao.SearchPresetDao;
import com.smouldering_durtles.wk.db.dao.SessionItemDao;
import com.smouldering_durtles.wk.db.dao.SrsSystemDao;
import com.smouldering_durtles.wk.db.dao.SubjectAggregatesDao;
import com.smouldering_durtles.wk.db.dao.SubjectCollectionsDao;
import com.smouldering_durtles.wk.db.dao.SubjectDao;
import com.smouldering_durtles.wk.db.dao.SubjectSyncDao;
import com.smouldering_durtles.wk.db.dao.SubjectViewsDao;
import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao;
import com.smouldering_durtles.wk.db.model.AudioDownloadStatus;
import com.smouldering_durtles.wk.db.model.LevelProgressionEntityDefinition;
import com.smouldering_durtles.wk.db.model.LogRecordEntityDefinition;
import com.smouldering_durtles.wk.db.model.PronunciationAudioOwner;
import com.smouldering_durtles.wk.db.model.Property;
import com.smouldering_durtles.wk.db.model.SearchPreset;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.SrsSystemDefinition;
import com.smouldering_durtles.wk.db.model.SubjectEntityDefinition;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.enums.SessionType;
import com.smouldering_durtles.wk.jobs.TickJob;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.services.JobRunnerService;
import com.smouldering_durtles.wk.tasks.DownloadAudioTask;
import com.smouldering_durtles.wk.tasks.DownloadPitchInfoTask;
import com.smouldering_durtles.wk.tasks.GetAssignmentsTask;
import com.smouldering_durtles.wk.tasks.GetLevelProgressionTask;
import com.smouldering_durtles.wk.tasks.GetPatchedAssignmentsTask;
import com.smouldering_durtles.wk.tasks.GetPatchedReviewStatisticsTask;
import com.smouldering_durtles.wk.tasks.GetPatchedStudyMaterialsTask;
import com.smouldering_durtles.wk.tasks.GetReviewStatisticsTask;
import com.smouldering_durtles.wk.tasks.GetSrsSystemsTask;
import com.smouldering_durtles.wk.tasks.GetStudyMaterialsTask;
import com.smouldering_durtles.wk.tasks.GetSubjectsTask;
import com.smouldering_durtles.wk.tasks.GetSummaryTask;
import com.smouldering_durtles.wk.tasks.GetUserTask;
import com.smouldering_durtles.wk.tasks.LoadReferenceDataTask;
import com.smouldering_durtles.wk.tasks.ReportSessionItemTask;
import com.smouldering_durtles.wk.tasks.ScanAudioDownloadStatusTask;
import com.smouldering_durtles.wk.tasks.SubmitStudyMaterialTask;

import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.DAY;
import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.util.ObjectSupport.join;

/**
 * The Room-wrapped SQLite database.
 */
@Database(entities = {
        TaskDefinition.class,
        Property.class,
        SubjectEntityDefinition.class,
        SrsSystemDefinition.class,
        LevelProgressionEntityDefinition.class,
        SessionItem.class,
        LogRecordEntityDefinition.class,
        AudioDownloadStatus.class,
        SearchPreset.class
}, version = 68)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    /**
     * The singleton instance.
     */
    private static @Nullable AppDatabase instance = null;

    /**
     * The internal name of the database.
     */
    private static final String DATABASE_NAME = "wanikani";

    /**
     * Migration from 48 to 49: add an index on the characters column.
     */
    public static final Migration MIGRATION_48_49 = new Migration(48, 49) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_subject_characters` ON subject (`characters`)");
        }
    };

    /**
     * Migration from 49 to 50: Fix the subject progress view to use the passed boolean instead of the passedAt date.
     */
    public static final Migration MIGRATION_49_50 = new Migration(49, 50) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("DROP VIEW IF EXISTS `SubjectProgressItem`");
            database.execSQL("CREATE VIEW `SubjectProgressItem` AS SELECT level, typeCode, srsStage, passed, unlockedAt "
                    + "FROM subject WHERE subject.hiddenAt = 0 AND object IS NOT NULL ORDER BY subject.level, subject.typeCode");
        }
    };

    /**
     * Migration from 50 to 51: drop the views.
     */
    public static final Migration MIGRATION_50_51 = new Migration(50, 51) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("DROP VIEW IF EXISTS `AudioDownloadOverviewItem`");
            database.execSQL("DROP VIEW IF EXISTS `SrsBreakDownItem`");
            database.execSQL("DROP VIEW IF EXISTS `SubjectProgressItem`");
        }
    };

    /**
     * Migration from 51 to 52: add levelProgressScore as a precomputed field.
     */
    public static final Migration MIGRATION_51_52 = new Migration(51, 52) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE subject ADD COLUMN levelProgressScore INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE subject SET levelProgressScore=0 WHERE passed");
            database.execSQL("UPDATE subject SET levelProgressScore=6 WHERE NOT passed AND (unlockedAt = 0 OR unlockedAt IS NULL)");
            database.execSQL("UPDATE subject SET levelProgressScore=1 WHERE NOT passed AND unlockedAt != 0 AND srsStage = 4");
            database.execSQL("UPDATE subject SET levelProgressScore=2 WHERE NOT passed AND unlockedAt != 0 AND srsStage = 3");
            database.execSQL("UPDATE subject SET levelProgressScore=3 WHERE NOT passed AND unlockedAt != 0 AND srsStage = 2");
            database.execSQL("UPDATE subject SET levelProgressScore=4 WHERE NOT passed AND unlockedAt != 0 AND srsStage = 1");
            database.execSQL("UPDATE subject SET levelProgressScore=5 WHERE NOT passed AND unlockedAt != 0 AND srsStage = 0");
        }
    };

    /**
     * Migration from 52 to 53: add numAnswers to session_item for improved undo.
     */
    public static final Migration MIGRATION_52_53 = new Migration(52, 53) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE session_item ADD COLUMN numAnswers INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * Migration from 53 to 54: add lastAnswer to session_item to record the timestamp the last question was answered for this item.
     */
    public static final Migration MIGRATION_53_54 = new Migration(53, 54) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE session_item ADD COLUMN lastAnswer INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * Migration from 54 to 55: add level_progression table for level duration tracking.
     */
    public static final Migration MIGRATION_54_55 = new Migration(54, 55) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `level_progression` (`id` INTEGER NOT NULL, `abandonedAt` INTEGER,"
                    + "`completedAt` INTEGER, `createdAt` INTEGER, `passedAt` INTEGER, `startedAt` INTEGER,"
                    + "`unlockedAt` INTEGER, `level` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    /**
     * Migration from 55 to 56: add log_record table for debug logging.
     */
    public static final Migration MIGRATION_55_56 = new Migration(55, 56) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `log_record` (`id` INTEGER NOT NULL, `timestamp` INTEGER, `tag` TEXT, "
                    + "`length` INTEGER NOT NULL, `message` TEXT, PRIMARY KEY(`id`))");
        }
    };

    /**
     * Migration from 56 to 57: add lastIncorrectAnswer column to subject, for a self-study filter.
     */
    public static final Migration MIGRATION_56_57 = new Migration(56, 57) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE subject ADD COLUMN lastIncorrectAnswer INTEGER");
        }
    };

    /**
     * Migration from 57 to 58: add audio_download_status table.
     */
    public static final Migration MIGRATION_57_58 = new Migration(57, 58) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `audio_download_status` (`level` INTEGER NOT NULL, `numTotal` INTEGER NOT NULL, "
                    + "`numNoAudio` INTEGER NOT NULL, `numMissingAudio` INTEGER NOT NULL, `numPartialAudio` INTEGER NOT NULL, "
                    + "`numFullAudio` INTEGER NOT NULL, PRIMARY KEY(`level`))");
        }
    };

    /**
     * Migration from 58 to 59: add pitchInfo column to subject table.
     */
    public static final Migration MIGRATION_58_59 = new Migration(58, 59) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE subject ADD COLUMN pitchInfo TEXT");
        }
    };

    /**
     * Migration from 59 to 60: add kanjiAcceptedReadingType to session_item table.
     */
    public static final Migration MIGRATION_59_60 = new Migration(59, 60) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE session_item ADD COLUMN kanjiAcceptedReadingType TEXT");
        }
    };

    /**
     * Migration from 60 to 61: add srsSystemId to session_item table and subject table.
     */
    public static final Migration MIGRATION_60_61 = new Migration(60, 61) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE session_item ADD COLUMN srsSystemId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE subject ADD COLUMN srsSystemId INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * Migration from 61 to 62: add documentUrl column.
     */
    public static final Migration MIGRATION_61_62 = new Migration(61, 62) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE subject ADD COLUMN documentUrl TEXT");
        }
    };

    /**
     * Migration from 62 to 63: add SRS systems, drop SRS stages
     */
    public static final Migration MIGRATION_62_63 = new Migration(62, 63) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE srs_stage");
            database.execSQL("CREATE TABLE IF NOT EXISTS `srs_system` (`id` INTEGER NOT NULL, `name` TEXT, `description` TEXT, `stages` TEXT,"
                    + " `unlockingStagePosition` INTEGER NOT NULL, `startingStagePosition` INTEGER NOT NULL,"
                    + " `passingStagePosition` INTEGER NOT NULL, `burningStagePosition` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    /**
     * Migration from 63 to 64: Add search_preset table.
     */
    public static final Migration MIGRATION_63_64 = new Migration(63, 64) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `search_preset` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL,"
                    + " `data` TEXT NOT NULL, PRIMARY KEY(`name`))");
        }
    };

    /**
     * Migration from 64 to 65: Use SRS stage -999 for locked.
     */
    public static final Migration MIGRATION_64_65 = new Migration(64, 65) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("UPDATE subject SET srsStage=-999 WHERE unlockedAt = 0 OR unlockedAt IS NULL");
        }
    };

    /**
     * Migration from 65 to 66: Clear out null values in date columns, prepare for starred items.
     */
    public static final Migration MIGRATION_65_66 = new Migration(65, 66) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("UPDATE subject SET resurrectedAt = 0 WHERE resurrectedAt IS NULL");
            database.execSQL("UPDATE subject SET burnedAt = 0 WHERE burnedAt IS NULL");
            database.execSQL("UPDATE subject SET unlockedAt = 0 WHERE unlockedAt IS NULL");
            database.execSQL("UPDATE subject SET startedAt = 0 WHERE startedAt IS NULL");
            database.execSQL("UPDATE subject SET passedAt = 0 WHERE passedAt IS NULL");
            database.execSQL("UPDATE subject SET availableAt = 0 WHERE availableAt IS NULL");
            database.execSQL("UPDATE subject SET typeCode = 0");
            database.execSQL("UPDATE subject SET srsSystemId = 1 WHERE srsSystemId < 1 AND level > 2");
            database.execSQL("UPDATE subject SET srsSystemId = 2 WHERE srsSystemId < 1 AND level <= 2");
        }
    };

    /**
     * Migration from 66 to 67: add strokeData column.
     */
    public static final Migration MIGRATION_66_67 = new Migration(66, 67) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE subject ADD COLUMN strokeData TEXT");
        }
    };

    /**
     * Migration from 65 to 66: Clear out null values in date columns, prepare for starred items.
     */
    public static final Migration MIGRATION_67_68 = new Migration(67, 68) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("UPDATE subject SET hiddenAt = 0 WHERE hiddenAt IS NULL");
            database.execSQL("UPDATE subject SET lastIncorrectAnswer = 0 WHERE lastIncorrectAnswer IS NULL");
        }
    };

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static AppDatabase getInstance() {
        if (instance == null) {
            //noinspection NonThreadSafeLazyInitialization
            instance = Room.databaseBuilder(WkApplication.getInstance(), AppDatabase.class, DATABASE_NAME)
                    .addMigrations(
                            MIGRATION_48_49,
                            MIGRATION_49_50,
                            MIGRATION_50_51,
                            MIGRATION_51_52,
                            MIGRATION_52_53,
                            MIGRATION_53_54,
                            MIGRATION_54_55,
                            MIGRATION_55_56,
                            MIGRATION_56_57,
                            MIGRATION_57_58,
                            MIGRATION_58_59,
                            MIGRATION_59_60,
                            MIGRATION_60_61,
                            MIGRATION_61_62,
                            MIGRATION_62_63,
                            MIGRATION_63_64,
                            MIGRATION_64_65,
                            MIGRATION_65_66,
                            MIGRATION_66_67,
                            MIGRATION_67_68)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    /**
     * Are there any API tasks pending?.
     *
     * @return true if there are
     */
    public final boolean hasPendingApiTasks() {
        return taskDefinitionDao().getCount() > 0;
    }

    /**
     * Add a task for fetching the user endpoint if it doesn't exist already.
     */
    public final void assertGetUserTask() {
        final int count = taskDefinitionDao().getCountByType(GetUserTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetUserTask.class);
            taskDefinition.setPriority(GetUserTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the subjects endpoint if it doesn't exist already.
     */
    public final void assertGetSubjectsTask() {
        final int count = taskDefinitionDao().getCountByType(GetSubjectsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetSubjectsTask.class);
            taskDefinition.setPriority(GetSubjectsTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the assignments endpoint if it doesn't exist already.
     */
    public final void assertGetAssignmentsTask() {
        final int count = taskDefinitionDao().getCountByType(GetAssignmentsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetAssignmentsTask.class);
            taskDefinition.setPriority(GetAssignmentsTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the assignments for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    public final void assertGetPatchedAssignmentsTask(final Iterable<Long> subjectIds) {
        final int count = taskDefinitionDao().getCountByType(GetPatchedAssignmentsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetPatchedAssignmentsTask.class);
            taskDefinition.setPriority(GetPatchedAssignmentsTask.PRIORITY);
            taskDefinition.setData(join(",", "", "", subjectIds));
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the review statistics endpoint if it doesn't exist already.
     */
    public final void assertGetReviewStatisticsTask() {
        final int count = taskDefinitionDao().getCountByType(GetReviewStatisticsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetReviewStatisticsTask.class);
            taskDefinition.setPriority(GetReviewStatisticsTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the review statistics for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    public final void assertGetPatchedReviewStatisticsTask(final Iterable<Long> subjectIds) {
        final int count = taskDefinitionDao().getCountByType(GetPatchedReviewStatisticsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetPatchedReviewStatisticsTask.class);
            taskDefinition.setPriority(GetPatchedReviewStatisticsTask.PRIORITY);
            taskDefinition.setData(join(",", "", "", subjectIds));
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the study materials endpoint if it doesn't exist already.
     */
    public final void assertGetStudyMaterialsTask() {
        final int count = taskDefinitionDao().getCountByType(GetStudyMaterialsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetStudyMaterialsTask.class);
            taskDefinition.setPriority(GetStudyMaterialsTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the study materials for a set of subjects if it doesn't exist already.
     *
     * @param subjectIds the subject IDs to fetch for
     */
    public final void assertGetPatchedStudyMaterialsTask(final Iterable<Long> subjectIds) {
        final int count = taskDefinitionDao().getCountByType(GetPatchedStudyMaterialsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetPatchedStudyMaterialsTask.class);
            taskDefinition.setPriority(GetPatchedStudyMaterialsTask.PRIORITY);
            taskDefinition.setData(join(",", "", "", subjectIds));
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the SRS systems endpoint if it doesn't exist already.
     */
    public final void assertGetSrsSystemsTask() {
        final int count = taskDefinitionDao().getCountByType(GetSrsSystemsTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetSrsSystemsTask.class);
            taskDefinition.setPriority(GetSrsSystemsTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the summary endpoint if it doesn't exist already.
     */
    public final void assertGetSummaryTask() {
        final int count = taskDefinitionDao().getCountByType(GetSummaryTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetSummaryTask.class);
            taskDefinition.setPriority(GetSummaryTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for fetching the level progression endpoint if it doesn't exist already.
     */
    public final void assertGetLevelProgressionTask() {
        final int count = taskDefinitionDao().getCountByType(GetLevelProgressionTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(GetLevelProgressionTask.class);
            taskDefinition.setPriority(GetLevelProgressionTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for reporting the status of a finished session item.
     *
     * @param timeStamp the timetamp the item was finished
     * @param subjectId the subject ID this task applies to
     * @param assignmentId the assignment ID for this item, or 0 if not known
     * @param type the type of the session this item is from
     * @param meaningIncorrect number of incorrect meaning answers
     * @param readingIncorrect number of incorrect reading answers
     * @param justPassed true if the subject just passed with this update (went to Guru I for the first time)
     */
    public final void assertReportSessionItemTask(final long timeStamp, final long subjectId, final long assignmentId, final SessionType type,
                                                  final int meaningIncorrect, final int readingIncorrect, final boolean justPassed) {
        final TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setTaskClass(ReportSessionItemTask.class);
        taskDefinition.setPriority(ReportSessionItemTask.PRIORITY);
        taskDefinition.setData(String.format(Locale.ROOT, "%d %d %d %s %d %d %s", timeStamp,
                subjectId, assignmentId, type, meaningIncorrect, readingIncorrect, justPassed));
        taskDefinitionDao().insertTaskDefinition(taskDefinition);
    }

    /**
     * Add a task for downloading audio for a subject.
     *
     * @param subject the subject to download for
     */
    public final void assertDownloadAudioTask(final PronunciationAudioOwner subject) {
        final TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setTaskClass(DownloadAudioTask.class);
        taskDefinition.setPriority(DownloadAudioTask.PRIORITY);
        taskDefinition.setData(Long.toString(subject.getId()));
        taskDefinitionDao().insertTaskDefinition(taskDefinition);
    }

    /**
     * Add a task for downloading pitch info for a subject.
     *
     * @param subjectId the subject to download for
     */
    public final void assertDownloadPitchInfoTask(final long subjectId) {
        final TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setTaskClass(DownloadPitchInfoTask.class);
        taskDefinition.setPriority(DownloadPitchInfoTask.PRIORITY);
        taskDefinition.setData(Long.toString(subjectId));
        taskDefinitionDao().insertTaskDefinition(taskDefinition);
    }

    /**
     * Add a task for saving/updating study materials.
     *
     * @param data the prepared data string for the task
     */
    public final void assertSubmitStudyMaterialTask(final String data) {
        final TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setTaskClass(SubmitStudyMaterialTask.class);
        taskDefinition.setPriority(SubmitStudyMaterialTask.PRIORITY);
        taskDefinition.setData(data);
        taskDefinitionDao().insertTaskDefinition(taskDefinition);
    }

    /**
     * Add a task for loading reference data for all subjects in one go.
     */
    public final void loadReferenceData() {
        final int count = taskDefinitionDao().getCountByType(LoadReferenceDataTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(LoadReferenceDataTask.class);
            taskDefinition.setPriority(LoadReferenceDataTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Add a task for scanning audio download status for all subjects in one go.
     */
    public final void assertScanAudioDownloadStatusTask() {
        final int count = taskDefinitionDao().getCountByType(ScanAudioDownloadStatusTask.class);
        if (count == 0) {
            final TaskDefinition taskDefinition = new TaskDefinition();
            taskDefinition.setTaskClass(ScanAudioDownloadStatusTask.class);
            taskDefinition.setPriority(ScanAudioDownloadStatusTask.PRIORITY);
            taskDefinition.setData("");
            taskDefinitionDao().insertTaskDefinition(taskDefinition);
        }
    }

    /**
     * Make sure there are tasks to update all of the core API models.
     */
    public final void assertRefreshForAllModels() {
        assertGetUserTask();
        assertGetAssignmentsTask();
        assertGetReviewStatisticsTask();
        assertGetStudyMaterialsTask();
        assertGetSummaryTask();
        final long lastSubjectSyncSuccessDate = propertiesDao().getLastSubjectSyncSuccessDate(0);
        if (lastSubjectSyncSuccessDate == 0
                || System.currentTimeMillis() - lastSubjectSyncSuccessDate > HOUR) {
            assertGetSubjectsTask();
        }
        final long lastGetSrsSystemsSuccess = propertiesDao().getLastSrsSystemSyncSuccessDate();
        if (lastGetSrsSystemsSuccess == 0
                || System.currentTimeMillis() - lastGetSrsSystemsSuccess > DAY) {
            assertGetSrsSystemsTask();
        }
        JobRunnerService.schedule(TickJob.class, "");
    }

    /**
     * Clear all API data out of the database.
     */
    public final void resetDatabase() {
        propertiesDao().setApiInError(false);
        propertiesDao().setApiKeyRejected(false);
        propertiesDao().setLastApiSuccessDate(0);
        propertiesDao().setLastUserSyncSuccessDate(0);
        propertiesDao().setLastSubjectSyncSuccessDate(0);
        propertiesDao().setLastAssignmentSyncSuccessDate(0);
        propertiesDao().setLastReviewStatisticSyncSuccessDate(0);
        propertiesDao().setLastStudyMaterialSyncSuccessDate(0);
        propertiesDao().setLastSrsSystemSyncSuccessDate(0);
        propertiesDao().setLastLevelProgressionSyncSuccessDate(0);
        propertiesDao().setLastSummarySyncSuccessDate(0);
        propertiesDao().setSessionType(SessionType.NONE);
        propertiesDao().setSessionOnkun(false);
        Session.getInstance().reset();
        taskDefinitionDao().deleteAll();
        subjectDao().deleteAll();
        srsSystemDao().deleteAll();
        sessionItemDao().deleteAll();
        levelProgressionDao().deleteAll();
        assertGetSubjectsTask();
        assertRefreshForAllModels();
        GlobalSettings.setFirstTimeSetup(0);
    }

    /**
     * Get the DAO instance for properties.
     *
     * @return the DAO
     */
    public abstract PropertiesDao propertiesDao();

    /**
     * Get the DAO instance for task definitions.
     *
     * @return the DAO
     */
    public abstract TaskDefinitionDao taskDefinitionDao();

    /**
     * Get the DAO instance for subjects.
     *
     * @return the DAO
     */
    public abstract SubjectDao subjectDao();

    /**
     * Get the DAO instance for fetching various collections of subjects.
     *
     * @return the DAO
     */
    public abstract SubjectCollectionsDao subjectCollectionsDao();

    /**
     * Get the DAO instance for fetching various aggregates of subjects.
     *
     * @return the DAO
     */
    public abstract SubjectAggregatesDao subjectAggregatesDao();

    /**
     * Get the DAO instance for fetching various subset views of subjects.
     *
     * @return the DAO
     */
    public abstract SubjectViewsDao subjectViewsDao();

    /**
     * Get the DAO instance for sync actions on subjects.
     *
     * @return the DAO
     */
    public abstract SubjectSyncDao subjectSyncDao();

    /**
     * Get the DAO instance for SRS systems.
     *
     * @return the DAO
     */
    public abstract SrsSystemDao srsSystemDao();

    /**
     * Get the DAO instance for level progression records.
     *
     * @return the DAO
     */
    public abstract LevelProgressionDao levelProgressionDao();

    /**
     * Get the DAO instance for session items.
     *
     * @return the DAO
     */
    public abstract SessionItemDao sessionItemDao();

    /**
     * Get the DAO instance for log records.
     *
     * @return the DAO
     */
    public abstract LogRecordDao logRecordDao();

    /**
     * Get the DAO instance for audio download status.
     *
     * @return the DAO
     */
    public abstract AudioDownloadStatusDao audioDownloadStatusDao();

    /**
     * Get the DAO instance for search presets.
     *
     * @return the DAO
     */
    public abstract SearchPresetDao searchPresetDao();
}
