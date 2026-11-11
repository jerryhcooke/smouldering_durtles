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

import com.lazy_durtles.wk.db.model.Subject;

/**
 * Functional interface for objects that care about changes to subject assignments.
 */
public interface SubjectChangeListener {
    /**
     * The data for the subject has been changed in some way.
     *
     * @param subject the subject with changes applied
     */
    void onSubjectChange(Subject subject);

    /**
     * Do you care about being notified about this subject ID?.
     *
     * @param subjectId the subject ID to query
     * @return true if the listener is interested
     */
    boolean isInterestedInSubject(long subjectId);
}
