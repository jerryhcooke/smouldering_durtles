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

import android.util.Log;

import androidx.room.Dao;
import androidx.room.Query;

import com.lazy_durtles.wk.db.model.LogRecord;
import com.lazy_durtles.wk.db.model.LogRecordSummary;

import java.util.List;

import javax.annotation.Nullable;

/**
 * DAO for debug log records.
 */
@Dao
public abstract class LogRecordDao {
    /**
     * Room-generated method: get the next log record available, with ID after the supplied one.
     *
     * @param id the id
     * @return the record or null if not found
     */
    @Query("SELECT * FROM log_record WHERE id > :id ORDER BY id LIMIT 1")
    protected abstract @Nullable LogRecord getNextHelper(long id);

    /**
     * Get the next record with ID greater than the argument ID.
     * @param id the id
     * @return the next record or null if it doesn't exist
     */
    public final @Nullable LogRecord getNext(final long id) {
        try {
            return getNextHelper(id);
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get the oldest log records as summaries.
     *
     * @param count the batch size to get
     * @return the list
     */
    @Query("SELECT id, length from log_record ORDER BY id LIMIT :count")
    public abstract List<LogRecordSummary> getOldestSummaries(final int count);

    /**
     * Delete the oldest records, up to the one with the supplied id.
     *
     * @param id the id
     */
    @Query("DELETE FROM log_record WHERE id < :id")
    protected abstract void deleteOldestHelper(long id);

    /**
     * Delete the oldest records, up to the size specified.
     *
     * @param excess the amount of excess to trim
     * @param batchSize the size of the batch to load and examine for deletion
     */
    private void deleteOldest(final int excess, final int batchSize) {
        try {
            final List<LogRecordSummary> records = getOldestSummaries(batchSize);
            if (records.isEmpty()) {
                return;
            }
            int size = 0;
            for (final LogRecordSummary record: records) {
                size += record.getLength();
                if (size > excess) {
                    deleteOldestHelper(record.getId());
                    return;
                }
            }
            deleteOldestHelper(records.get(records.size()-1).getId() + 1);
        }
        catch (final Exception e) {
            Log.e("LogRecordDao", "Exception deleting log records", e);
            if (batchSize > 1) {
                deleteOldest(excess, batchSize / 2);
            }
        }
    }

    /**
     * Delete the oldest records, up to the size specified.
     *
     * @param excess the amount of excess to trim
     */
    public final void deleteOldest(final int excess) {
        deleteOldest(excess, 1 << 16);
    }

    /**
     * Room-generated method: get the total size of all records (sum of length columns).
     *
     * @return the total size
     */
    @Query("SELECT SUM(length) FROM log_record")
    public abstract int getTotalSize();

    /**
     * Room-generated method: insert a new record.
     *
     * @param timestamp entity field
     * @param tag entity field
     * @param length entity field
     * @param message entity field
     */
    @Query("INSERT INTO log_record (timestamp, tag, length, message) VALUES (:timestamp, :tag, :length, :message)")
    protected abstract void insertHelper(long timestamp, @androidx.annotation.Nullable String tag,
                                         int length, @androidx.annotation.Nullable String message);

    /**
     * Insert a new record.
     *
     * @param record the record to insert
     */
    public final void insert(final LogRecord record) {
        insertHelper(record.timestamp, record.tag, record.length, record.message);
    }
}
