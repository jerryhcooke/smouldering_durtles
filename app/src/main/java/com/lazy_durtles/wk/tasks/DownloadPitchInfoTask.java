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

package com.lazy_durtles.wk.tasks;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.Converters;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.model.PitchInfo;
import com.lazy_durtles.wk.util.PitchInfoUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.util.ObjectSupport.orElse;
import static java.util.Objects.requireNonNull;

/**
 * Task to download pitch info for a single subject.
 */
public final class DownloadPitchInfoTask extends ApiTask {
    /**
     * Task priority. This is low priority, all API tasks will be taken care of first.
     */
    public static final int PRIORITY = 101;

    private final int subjectId;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public DownloadPitchInfoTask(final TaskDefinition taskDefinition) {
        super(taskDefinition);
        subjectId = Integer.parseInt(orElse(taskDefinition.getData(), "-1"));
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canDownloadAudio();
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        final @Nullable Subject subject = db.subjectDao().getById(subjectId);

        if (subject != null && subject.needsPitchInfoDownload(HOUR)) {
            final @Nullable String body = PitchInfoUtil.downloadWeblioPage(requireNonNull(subject.getCharacters()));

            String pitchInfoString = String.format(Locale.ROOT, "@%d", System.currentTimeMillis());
            if (body != null) {
                final @Nullable Set<PitchInfo> pitchInfos = PitchInfoUtil.parseWeblioPage(body);
                if (pitchInfos != null) {
                    final List<PitchInfo> list = new ArrayList<>(pitchInfos);
                    Collections.sort(list);
                    try {
                        pitchInfoString = Converters.getObjectMapper().writeValueAsString(list);
                    }
                    catch (final Exception e) {
                        // This can't realistically happen
                    }
                }
            }

            db.subjectDao().updateReferenceData(subjectId, subject.getFrequency(), subject.getJoyoGrade(),
                    subject.getJlptLevel(), pitchInfoString, subject.getStrokeData());
        }

        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
    }
}
