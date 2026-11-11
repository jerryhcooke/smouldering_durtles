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

import com.lazy_durtles.wk.GlobalSettings;
import com.lazy_durtles.wk.model.Session;

/**
 * Behaviour for the don't know button, specifically, when it should be shown.
 */
@SuppressWarnings("unused")
public enum DontKnowButtonBehavior {
    /**
     * Hide the button completely.
     */
    DISABLE() {
        @Override public boolean canShow() {
            return false;
        }
    },

    /**
     * Show the button on all questions.
     */
    ENABLE() {
        @Override public boolean canShow() {
            return !Session.getInstance().isAnswered();
        }
    },

    /**
     * Show the button, but only if the special button 2 is not shown.
     */
    ONLY_IF_IGNORE_HIDDEN() {
        @Override public boolean canShow() {
            return !Session.getInstance().isAnswered() && !GlobalSettings.AdvancedOther.getSpecialButton2Behavior().canShow();
        }
    };

    /**
     * Determine if the don't know button should be shown under the current circumstances.
     *
     * @return true if the button should be shown
     */
    public abstract boolean canShow();
}
