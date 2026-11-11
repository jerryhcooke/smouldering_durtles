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

package com.lazy_durtles.wk.db.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.lazy_durtles.wk.tasks.ApiTask;

import javax.annotation.Nullable;

/**
 * Room entity for the task_definition table. These are records of background tasks to run, that involve network interaction.
 */
@Entity(tableName = "task_definition")
public final class TaskDefinition {
    @PrimaryKey(autoGenerate = true) private int id = 0;
    private @Nullable Class<? extends ApiTask> taskClass;
    private int priority = 0;
    private @Nullable String data;

    /**
     * The unique ID.
     * @return the value
     */
    public int getId() {
        return id;
    }

    /**
     * The unique ID.
     * @param id the value
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * The class implementing this type of task.
     * @return the value
     */
    public @Nullable Class<? extends ApiTask> getTaskClass() {
        return taskClass;
    }

    /**
     * The class implementing this type of task.
     * @param taskClass the value
     */
    public void setTaskClass(final @Nullable Class<? extends ApiTask> taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * The priority to execute this task with, lower priority is run earlier.
     * @return the value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * The priority to execute this task with, lower priority is run earlier.
     * @param priority the value
     */
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    /**
     * The parameters for this task, encoded as a string in a class-specific format.
     * @return the value
     */
    public @Nullable String getData() {
        return data;
    }

    /**
     * The parameters for this task, encoded as a string in a class-specific format.
     * @param data the value
     */
    public void setData(final @Nullable String data) {
        this.data = data;
    }
}
