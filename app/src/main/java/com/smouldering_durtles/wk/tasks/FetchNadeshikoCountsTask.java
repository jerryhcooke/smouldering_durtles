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

package com.smouldering_durtles.wk.tasks;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.NadeshikoCount;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.util.FrequencyUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.orElse;

/**
 * Task to fetch Nadeshiko sentence counts for a batch of subjects. Terms that are already in the
 * local cache are applied without any network request; the rest are fetched in one batched request.
 */
public final class FetchNadeshikoCountsTask extends ApiTask {
    /**
     * Task priority. Lower priority than all API tasks.
     */
    public static final int PRIORITY = 102;

    private final List<Long> subjectIds = new ArrayList<>();

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public FetchNadeshikoCountsTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
        for (final String s: orElse(taskDefinition.getData(), "").split(",")) {
            try {
                subjectIds.add(Long.parseLong(s.trim()));
            }
            catch (final NumberFormatException e) {
                // Ignore
            }
        }
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi();
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        try {
            if (!GlobalSettings.SubjectInfo.getShowNadeshikoCount() || !FrequencyUtil.canUseNadeshikoKey()) {
                return;
            }

            // term -> subject IDs that still need it
            final Map<String, List<Long>> pending = new LinkedHashMap<>();
            for (final long id: subjectIds) {
                final @Nullable Subject subject = db.subjectDao().getById(id);
                if (subject == null || !subject.canHaveFrequencyData() || subject.getNadeshikoCount() >= 0) {
                    continue;
                }
                final String term = FrequencyUtil.normalizeTerm(orElse(subject.getCharacters(), ""));
                if (term.isEmpty()) {
                    continue;
                }
                final @Nullable NadeshikoCount cached = db.frequencyDao().getNadeshikoCount(term);
                if (cached != null) {
                    db.frequencyDao().updateNadeshikoCount(id, cached.matchCount);
                    continue;
                }
                List<Long> ids = pending.get(term);
                if (ids == null) {
                    ids = new ArrayList<>();
                    pending.put(term, ids);
                }
                ids.add(id);
            }

            if (pending.isEmpty()) {
                return;
            }

            final Map<String, Integer> counts = FrequencyUtil.fetchNadeshikoCounts(pending.keySet());
            final long now = System.currentTimeMillis();
            for (final Map.Entry<String, Integer> entry: counts.entrySet()) {
                final NadeshikoCount record = new NadeshikoCount();
                record.term = entry.getKey();
                record.matchCount = entry.getValue();
                record.fetchedAt = now;
                db.frequencyDao().insertNadeshikoCount(record);
                final @Nullable List<Long> ids = pending.get(entry.getKey());
                if (ids != null) {
                    for (final long id: ids) {
                        db.frequencyDao().updateNadeshikoCount(id, entry.getValue());
                    }
                }
            }
        }
        finally {
            // Always remove the task, even on failure, so the task queue can never get stuck on it.
            db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        }
    }
}
