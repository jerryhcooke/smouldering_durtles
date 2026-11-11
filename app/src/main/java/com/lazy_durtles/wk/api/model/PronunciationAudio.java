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

package com.lazy_durtles.wk.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Model class representing a pronunciation audio file for a vocab subject.
 */
@SuppressWarnings("unused")
public final class PronunciationAudio {
    private @Nullable String url = null;
    @JsonProperty("content_type") private @Nullable String contentType = null;
    private PronunciationAudioMeta metadata = new PronunciationAudioMeta();

    /**
     * The public URL for the audio file.
     * @return the value
     */
    public @Nullable String getUrl() {
        return url;
    }

    /**
     * The public URL for the audio file.
     * @param url the value
     */
    public void setUrl(final @Nullable String url) {
        this.url = url;
    }

    /**
     * The content type for this file.
     * @return the value
     */
    public @Nullable String getContentType() {
        return contentType;
    }

    /**
     * The content type for this file.
     * @param contentType the value
     */
    public void setContentType(final @Nullable String contentType) {
        this.contentType = contentType == null ? null : contentType.intern();
    }

    /**
     * The metadata for this file.
     * @return the value
     */
    public PronunciationAudioMeta getMetadata() {
        return metadata;
    }

    /**
     * The metadata for this file.
     * @param metadata the value
     */
    public void setMetadata(final @Nullable PronunciationAudioMeta metadata) {
        this.metadata = metadata == null ? new PronunciationAudioMeta() : metadata;
    }
}
