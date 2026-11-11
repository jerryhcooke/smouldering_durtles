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

/**
 * LiveData for progress in background tasks.
 *
 * <p>
 *     This is kind of a degenerate LiveData since the actual value is just
 *     an empty Object, but the singleton instance contains the latest information.
 *     The LiveData is just used to trigger observers to examine and update their
 *     display.
 * </p>
 */
public final class LiveApiProgress extends ConservativeLiveData<Object> {
    /**
     * The singleton instance.
     */
    private static final LiveApiProgress instance = new LiveApiProgress();

    private boolean show = false;
    private String entityName = "";
    private int numEntities = 0;
    private int numProcessedEntities = 0;
    private int lastReportedCount = 0;
    private boolean syncReminder = false;

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveApiProgress getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveApiProgress() {
        //
    }

    @Override
    protected void updateLocal() {
        //
    }

    @Override
    public Object getDefaultValue() {
        return new Object();
    }

    /**
     * Report that new entities have been added for the current task to process.
     *
     * @param num the number of additional entities
     */
    public static void addEntities(final int num) {
        instance.numEntities += num;
        instance.postValue(new Object());
    }

    /**
     * Report that one more entity has been processed by the current task.
     */
    public static void addProcessedEntity() {
        instance.numProcessedEntities++;
        if (instance.numProcessedEntities - instance.lastReportedCount >= 100
                || instance.numProcessedEntities >= instance.numEntities
                || instance.numEntities < 100) {
            instance.postValue(new Object());
            instance.lastReportedCount = instance.numProcessedEntities;
        }
    }

    /**
     * Reset the state for a new task.
     *
     * @param show true if the progress for this task must be shown
     * @param entityName the display name of the entity being synced right now, such as "subjects"
     */
    public static void reset(final boolean show, final String entityName) {
        instance.show = show;
        instance.entityName = entityName;
        instance.numEntities = 0;
        instance.numProcessedEntities = 0;
        instance.lastReportedCount = 0;
        instance.postValue(new Object());
    }

    /**
     * Should the progress for the current task be shown?.
     * @return the value
     */
    public static boolean getShow() {
        return instance.show;
    }

    /**
     * The display name of the entity being synced right now, such as "subjects".
     * @return the value
     */
    public static String getEntityName() {
        return instance.entityName;
    }

    /**
     * The total number of entities being processed by the current task.
     * @return the value
     */
    public static int getNumEntities() {
        return instance.numEntities;
    }

    /**
     * The number of entities that have been processed so far by the current task.
     * @return the value
     */
    public static int getNumProcessedEntities() {
        return instance.numProcessedEntities;
    }

    /**
     * Flag to remind the user to sync.
     * @return the value
     */
    public boolean getSyncReminder() {
        return syncReminder;
    }

    /**
     * Flag to remind the user to sync.
     * @param syncReminder the value
     */
    public void setSyncReminder(final boolean syncReminder) {
        this.syncReminder = syncReminder;
        instance.postValue(new Object());
    }
}
