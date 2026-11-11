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

package com.lazy_durtles.wk.enums;

/**
 * The state of the current lesson/review/self-study session.
 */
public enum SessionState {
    /**
     * Session is inactive.
     */
    INACTIVE,

    /**
     * Sessions is a lesson session, and the quiz hasn't started yet.
     * The UI is displaying the subjects in the lesson.
     */
    IN_LESSON_PRESENTATION,

    /**
     * The quiz part of the session is currently ongoing.
     */
    ACTIVE,

    /**
     * All items in the session have been finihed or abandoned. The session is
     * deactivated as soon as the session summary has been acknowledged by the user.
     */
    FINISHING
}
