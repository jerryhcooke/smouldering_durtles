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

package com.lazy_durtles.wk.livedata;

import android.annotation.SuppressLint;

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.enums.SubjectType;
import com.lazy_durtles.wk.model.LevelProgress;
import com.lazy_durtles.wk.model.LevelProgressItem;

/**
 * LiveData that tracks the data for the level progression bars on the dashboard.
 */
public final class LiveLevelProgress extends ConservativeLiveData<LevelProgress> {
    /**
     * The singleton instance.
     */
    private static final LiveLevelProgress instance = new LiveLevelProgress();
    private SubjectType type;
    private int level;
    private int count;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveLevelProgress getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveLevelProgress() {
        //
    }

    public void setType(SubjectType type) {
        this.type = type;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCount(int count) {
        this.count = count;
    }
    public void LevelProgressItem(SubjectType type, int level, int count) {
        this.type = type;
        this.level = level;
        this.count = count;
    }
    @SuppressLint("NewApi")
    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final int userLevel = db.propertiesDao().getUserLevel();
        final int maxLevel;
        if (GlobalSettings.Dashboard.getShowOverLevelProgression()) {
            maxLevel = db.propertiesDao().getUserMaxLevelGranted();
        } else {
            maxLevel = userLevel;
        }
        final LevelProgress levelProgress = new LevelProgress(maxLevel);

        for (final LevelProgressItem item : db.subjectViewsDao().getLevelProgressTotalItems()) {
            if (item.getType() == SubjectType.WANIKANI_KANA_VOCAB) {
                LevelProgressItem combinedItem = new LevelProgressItem();
                combinedItem.setType(SubjectType.WANIKANI_VOCAB);
                combinedItem.setLevel(item.getLevel());
                combinedItem.setCount(item.getCount());
                levelProgress.setTotalCount(combinedItem);
            } else {
                levelProgress.setTotalCount(item);
            }
        }

        for (final LevelProgressItem item : db.subjectViewsDao().getLevelProgressPassedItems()) {
            if (item.getType() == SubjectType.WANIKANI_KANA_VOCAB) {
                LevelProgressItem combinedItem = new LevelProgressItem();
                combinedItem.setType(SubjectType.WANIKANI_VOCAB);
                combinedItem.setLevel(item.getLevel());
                combinedItem.setCount(item.getCount());
                levelProgress.setNumPassed(combinedItem);
            } else {
                levelProgress.setNumPassed(item);
            }
        }

        if (GlobalSettings.Dashboard.getLimitPreviousLevelProgression()) {
            final int limit = GlobalSettings.Dashboard.getPreviousLevelProgressionLimitedAmount();
            levelProgress.removePassedAndLockedAndOldBars(userLevel, limit);
        } else {
            levelProgress.removePassedAndLockedBars(userLevel);
        }

        levelProgress.getEntries().forEach(
                entry -> db.subjectCollectionsDao().getLevelProgressSubjects(entry.getLevel(), entry.getType())
                        .forEach(entry::addSubject));

        instance.postValue(levelProgress);
    }

    @Override
    public LevelProgress getDefaultValue() {
        return new LevelProgress(0);
    }
}
