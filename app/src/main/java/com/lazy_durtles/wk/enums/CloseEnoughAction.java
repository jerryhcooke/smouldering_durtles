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
 * Action to take when a meaning answer is not quite right, but is acceptable for the typo lenience.
 */
public enum CloseEnoughAction {
    /**
     * Silently accept the answer.
     */
    SILENTLY_ACCEPT(false),

    /**
     * Accept the answer, but show a toast/notification letting the user know it was slightly off.
     */
    ACCEPT_WITH_TOAST(true),

    /**
     * Accept the answer, but show a toast/notification letting the user know it was slightly off.
     * In addition, disable lightning mode for this particular question.
     */
    ACCEPT_WITH_TOAST_NO_LM(true),

    /**
     * Reject the answer but let the user try again.
     */
    SHAKE_AND_RETRY(false),

    /**
     * Reject the answer and mark the question as incorrectly answered.
     */
    REJECT(false);

    private final boolean showToast;

    CloseEnoughAction(final boolean showToast) {
        this.showToast = showToast;
    }

    /**
     * Does this choice require showing a toast?.
     *
     * @return true if it does
     */
    public boolean isShowToast() {
        return showToast;
    }
}
