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

/**
 * Model class used to update a study material record for the API.
 */
@SuppressWarnings("unused")
public final class ApiUpdateStudyMaterial {
    @JsonProperty("study_material") private ApiStudyMaterial studyMaterial = new ApiStudyMaterial();

    /**
     * The study material record to update.
     * @return the value
     */
    public ApiStudyMaterial getStudyMaterial() {
        return studyMaterial;
    }

    /**
     * The study material record to update.
     * @param studyMaterial the value
     */
    public void setStudyMaterial(final ApiStudyMaterial studyMaterial) {
        this.studyMaterial = studyMaterial;
    }
}
