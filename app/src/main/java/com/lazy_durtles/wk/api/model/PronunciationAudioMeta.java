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
 * Model class for metadata for an audio file.
 */
@SuppressWarnings("unused")
public final class PronunciationAudioMeta {
    private @Nullable String gender = null;
    private @Nullable String pronunciation = null;
    @JsonProperty("source_id") private long sourceId = -1;
    @JsonProperty("voice_actor_id") private long voiceActorId = -1;
    @JsonProperty("voice_actor_name") private @Nullable String voiceActorName = null;
    @JsonProperty("voice_description") private @Nullable String voiceDescription = null;

    /**
     * The gender of the voice actor, "male" or "female".
     * @return the value
     */
    public @Nullable String getGender() {
        return gender;
    }

    /**
     * The gender of the voice actor, "male" or "female".
     * @param gender the value
     */
    public void setGender(final @Nullable String gender) {
        this.gender = gender == null ? null : gender.intern();
    }

    /**
     * The reading pronounced in this file.
     * @return the value
     */
    public @Nullable String getPronunciation() {
        return pronunciation;
    }

    /**
     * The reading pronounced in this file.
     * @param pronunciation the value
     */
    public void setPronunciation(final @Nullable String pronunciation) {
        this.pronunciation = pronunciation;
    }

    /**
     * The recording source ID.
     * @return the value
     */
    public long getSourceId() {
        return sourceId;
    }

    /**
     * The recording source ID.
     * @param sourceId the value
     */
    public void setSourceId(final long sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * The voice actor's ID.
     * @return the value
     */
    public long getVoiceActorId() {
        return voiceActorId;
    }

    /**
     * The voice actor's ID.
     * @param voiceActorId the value
     */
    public void setVoiceActorId(final long voiceActorId) {
        this.voiceActorId = voiceActorId;
    }

    /**
     * Name of the voice actor.
     * @return the value
     */
    public @Nullable String getVoiceActorName() {
        return voiceActorName;
    }

    /**
     * Name of the voice actor.
     * @param voiceActorName the value
     */
    public void setVoiceActorName(final @Nullable String voiceActorName) {
        this.voiceActorName = voiceActorName == null ? null : voiceActorName.intern();
    }

    /**
     * Description of the actor's voice.
     * @return the value
     */
    public @Nullable String getVoiceDescription() {
        return voiceDescription;
    }

    /**
     * Description of the actor's voice.
     * @param voiceDescription the value
     */
    public void setVoiceDescription(final @Nullable String voiceDescription) {
        this.voiceDescription = voiceDescription == null ? null : voiceDescription.intern();
    }

    /**
     * Is the gender of this file male?.
     *
     * @return true if it is
     */
    public boolean isMale() {
        return "male".equalsIgnoreCase(gender);
    }

    /**
     * Is the gender of this file female?.
     *
     * @return true if it is
     */
    public boolean isFemale() {
        return !isMale();
    }
}
