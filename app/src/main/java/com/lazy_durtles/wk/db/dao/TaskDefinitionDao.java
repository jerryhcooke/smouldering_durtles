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

package com.lazy_durtles.wk.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.model.TaskCounts;
import com.lazy_durtles.wk.tasks.ApiTask;

import javax.annotation.Nullable;

/**
 * DAO for task definitions.
 */
@Dao
public abstract class TaskDefinitionDao {
    /**
     * Room-generated method: delete all records.
     */
    @Query("DELETE FROM task_definition")
    public abstract void deleteAll();

    /**
     * Room-generated method: delete all audio download tasks.
     */
    @Query("DELETE FROM task_definition WHERE taskClass = 'com.lazy_durtles.wk.tasks.DownloadAudioTask'")
    public abstract void deleteAudioDownloads();

    /**
     * Room-generated method: get the total number of tasks.
     *
     * @return the number
     */
    @Query("SELECT COUNT(*) FROM task_definition")
    public abstract int getCount();

    /**
     * Room-generated method: get the total number of tasks excluding audio download tasks.
     *
     * @return the number
     */
    @Query("SELECT COUNT(*) FROM task_definition WHERE taskClass!='com.lazy_durtles.wk.tasks.DownloadAudioTask' "
            + "AND taskClass!='com.lazy_durtles.wk.tasks.DownloadPitchInfoTask'")
    public abstract int getApiCount();

    /**
     * Room-generated method: get a LiveData instance containing the counts of tasks (API and audio separately).
     *
     * @return the LiveData instance
     */
    @Query("SELECT apiCount, audioCount, pitchInfoCount FROM "
            + "(SELECT COUNT(*) AS apiCount FROM task_definition "
            + "WHERE taskClass!='com.lazy_durtles.wk.tasks.DownloadAudioTask' AND taskClass!='com.lazy_durtles.wk.tasks.DownloadPitchInfoTask'), "
            + "(SELECT count(*) AS audioCount FROM task_definition "
            + "WHERE taskClass='com.lazy_durtles.wk.tasks.DownloadAudioTask'), "
            + "(SELECT count(*) AS pitchInfoCount FROM task_definition "
            + "WHERE taskClass='com.lazy_durtles.wk.tasks.DownloadPitchInfoTask');")
    public abstract LiveData<TaskCounts> getLiveCounts();

    /**
     * Room-generated method: get the next task to execute.
     *
     * @return the task or null if none are pending
     */
    @Query("SELECT * FROM task_definition ORDER BY priority, id LIMIT 1")
    public abstract @Nullable TaskDefinition getNextTaskDefinition();

    /**
     * Room-generated method: get the number of tasks for a certain task class.
     *
     * @param taskClass the class to look for
     * @return the number
     */
    @Query("SELECT COUNT(*) FROM task_definition WHERE taskClass = :taskClass")
    public abstract int getCountByType(Class<? extends ApiTask> taskClass);

    /**
     * Room-generated method: insert a new task.
     *
     * @param taskDefinition the task to insert
     */
    @Insert
    public abstract void insertTaskDefinition(TaskDefinition taskDefinition);

    /**
     * Room-generated method: delete a task.
     *
     * @param taskDefinition the task to delete
     */
    @Delete
    public abstract void deleteTaskDefinition(TaskDefinition taskDefinition);
}
