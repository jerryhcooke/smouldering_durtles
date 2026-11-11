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
import com.lazy_durtles.wk.api.model.PronunciationAudio;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.dao.SubjectDao;
import com.lazy_durtles.wk.db.model.Subject;
import com.lazy_durtles.wk.db.model.TaskDefinition;
import com.lazy_durtles.wk.util.AudioUtil;

import java.io.File;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.isEqual;
import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

/**
 * Task to download audio files for a single subject. This will download
 * one file for each recording source ID, i.e. all readings and all voices
 * will be represented, but this task won't download e.g. an MP3 file if
 * the corresponding OGG file is already present.
 *
 * <p>
 *     Afterwards, the audio download status is updated in the database.
 *     Errors are ignored, but the audio download status will reflect any
 *     audio files that may still be missing.
 * </p>
 */
public final class DownloadAudioTask extends ApiTask {
    /**
     * Task priority. This is low priority, all API tasks will be taken care of first.
     */
    public static final int PRIORITY = 100;

    private final int subjectId;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public DownloadAudioTask(final TaskDefinition taskDefinition) {
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
        final SubjectDao subjectDao = db.subjectDao();

        final @Nullable Subject subject = subjectDao.getById(subjectId);

        if (subject != null) {
            boolean scan = false;
            final Iterable<String> locationValues = AudioUtil.getLocationValues();

            for (final PronunciationAudio audio: subject.getParsedPronunciationAudios()) {
                if (isEqual(audio.getContentType(), "audio/ogg")) {
                    continue;
                }
                if (AudioUtil.hasAudioFileFor(subject.getLevel(), audio, locationValues)) {
                    continue;
                }
                final @Nullable File output = AudioUtil.getNewFileForAudio(subject.getLevel(), audio);
                if (output == null) {
                    continue;
                }
                final @Nullable File tempFile = AudioUtil.getTempFile(output);
                if (tempFile == null) {
                    continue;
                }
                if (audio.getUrl() != null) {
                    downloadFile(audio.getUrl(), tempFile, output);
                    scan = true;
                }
            }

            for (final PronunciationAudio audio: subject.getParsedPronunciationAudios()) {
                if (!isEqual(audio.getContentType(), "audio/ogg")) {
                    continue;
                }
                if (AudioUtil.hasAudioFileFor(subject.getLevel(), audio, locationValues)) {
                    continue;
                }
                final @Nullable File output = AudioUtil.getNewFileForAudio(subject.getLevel(), audio);
                if (output == null) {
                    continue;
                }
                final @Nullable File tempFile = AudioUtil.getTempFile(output);
                if (tempFile == null) {
                    continue;
                }
                if (audio.getUrl() != null) {
                    downloadFile(audio.getUrl(), tempFile, output);
                    scan = true;
                }
            }

            if (scan) {
                AudioUtil.updateDownloadStatus(subject.getLevel());
            }
        }

        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
    }
}
