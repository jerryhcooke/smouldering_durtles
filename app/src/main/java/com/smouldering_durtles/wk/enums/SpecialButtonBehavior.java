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

package com.smouldering_durtles.wk.enums;

import com.smouldering_durtles.wk.model.Session;

/**
 * The behaviour for the special buttons.
 */
@SuppressWarnings("unused")
public enum SpecialButtonBehavior {
    /**
     * Disable, the button is not shown at all.
     */
    DISABLE("") {
        @Override public boolean canShow() {
            return false;
        }

        @Override
        public void perform() {
            //
        }
    },

    /**
     * Ignore the last incorrect answer, and pretend it was correct instead.
     */
    UNDO_AND_MARK_CORRECT("Ignore") {
        @Override public boolean canShow() {
            final Session session = Session.getInstance();
            return session.canUndo() && session.isAnswered() && !session.isCorrect();
        }

        @Override
        public void perform() {
            if (canShow()) {
                final Session session = Session.getInstance();
                session.ignore();
            }
        }
    },

    /**
     * Undo the last answer and retry it immediately.
     */
    UNDO_AND_RETRY_NOW("Undo") {
        @Override
        public boolean canShow() {
            return Session.getInstance().canUndo();
        }

        @Override
        public void perform() {
            if (canShow()) {
                Session.getInstance().undoAndRetry();
            }
        }
    },

    /**
     * Undo the last answer and put it back in the pool to be re-asked at some point in the future.
     */
    UNDO_AND_PUT_BACK("Undo") {
        @Override
        public boolean canShow() {
            return Session.getInstance().canUndo();
        }

        @Override
        public void perform() {
            if (canShow()) {
                Session.getInstance().undoAndPutBack();
            }
        }
    },

    /**
     * Skip this item for now, shuffle it back into the queue.
     */
    SKIP("Skip") {
        @Override
        public boolean canShow() {
            final Session session = Session.getInstance();
            return session.isActive() && !session.isAnswered();
        }

        @Override
        public void perform() {
            if (canShow()) {
                Session.getInstance().skip();
            }
        }
    },

    /**
     * Toggle lightning mode on/off temporarily.
     */
    TOGGLE_LIGHTNING_MODE("Lightning") {
        @Override
        public boolean canShow() {
            return Session.getInstance().isActive();
        }

        @Override
        public void perform() {
            if (canShow()) {
                Session.getInstance().toggleLightningMode();
            }
        }
    };

    /**
     * The label for the button.
     */
    private final String label;

    /**
     * The constructor.
     *
     * @param label The label for the button.
     */
    SpecialButtonBehavior(final String label) {
        this.label = label;
    }

    /**
     * The label for the button.
     *
     * @return the label
     */
    public final String getLabel() {
        if (this == TOGGLE_LIGHTNING_MODE) {
            final Session session = Session.getInstance();
            return session.isLightningModeActive() ? "Lightning" : "L̶i̶g̶h̶t̶n̶i̶n̶g̶";
        }
        return label;
    }

    /**
     * Can the button be shown now?.
     *
     * @return true if it can
     */
    public abstract boolean canShow();

    /**
     * Perform the action for pressing the button.
     */
    public abstract void perform();
}
