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

package com.lazy_durtles.wk.model;

import com.lazy_durtles.wk.db.model.SearchPreset;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Export/import entity for search presets.
 */
public final class SearchPresetExport {
    private List<SearchPreset> namedPresets = new ArrayList<>();
    private @Nullable SearchPreset selfStudyPreset = null;

    /**
     * The named presets created by the user.
     *
     * @return the list of presets
     */
    public List<SearchPreset> getNamedPresets() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return namedPresets;
    }

    /**
     * The named presets created by the user.
     *
     * @param namedPresets the list of presets
     */
    @SuppressWarnings("unused")
    public void setNamedPresets(final List<SearchPreset> namedPresets) {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.namedPresets = namedPresets;
    }

    /**
     * The default self-study preset.
     *
     * @return the preset or null if never set
     */
    public @Nullable SearchPreset getSelfStudyPreset() {
        return selfStudyPreset;
    }

    /**
     * The default self-study preset.
     *
     * @param selfStudyPreset the preset or null if never set
     */
    public void setSelfStudyPreset(final @Nullable SearchPreset selfStudyPreset) {
        this.selfStudyPreset = selfStudyPreset;
    }
}
