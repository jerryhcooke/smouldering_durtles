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

import com.fasterxml.jackson.core.type.TypeReference;
import com.lazy_durtles.wk.api.model.PronunciationAudio;
import com.lazy_durtles.wk.db.Converters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.isEmpty;

/**
 * Subset of Subject used for audio file scanning.
 */
public final class SubjectPronunciationAudio implements PronunciationAudioOwner {
    private long id = 0L;
    private int level = 0;
    private @Nullable String pronunciationAudios;

    @Override
    public long getId() {
        return id;
    }

    /**
     * The subject ID.
     * @param id the value
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * The audio for this vocab, empty for radicals and kanji. Encoded as a JSON string.
     * @return the value
     */
    @SuppressWarnings("unused")
    public @Nullable String getPronunciationAudios() {
        return pronunciationAudios;
    }

    /**
     * The audio for this vocab, empty for radicals and kanji. Encoded as a JSON string.
     * @param pronunciationAudios the value
     */
    public void setPronunciationAudios(final @Nullable String pronunciationAudios) {
        this.pronunciationAudios = pronunciationAudios;
    }

    @Override
    public int getLevel() {
        return level;
    }

    /**
     * The level this subject belongs to.
     * @param level the value
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    @Override
    public List<PronunciationAudio> getParsedPronunciationAudios() {
        if (isEmpty(pronunciationAudios)) {
            return Collections.emptyList();
        }
        try {
            return Converters.getObjectMapper().readValue(pronunciationAudios, new TypeReference<List<PronunciationAudio>>() {});
        } catch (final IOException e) {
            return Collections.emptyList();
        }
    }
}
