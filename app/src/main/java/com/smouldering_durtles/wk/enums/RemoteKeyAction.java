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

/**
 * The action that a button on an external remote control (such as a Bluetooth
 * "anki ring") performs in Anki mode.
 *
 * <p>
 *     Most of these rings only have a handful of buttons, so the actions are
 *     deliberately context sensitive: a single button can drive a whole session
 *     because its meaning depends on the state of the current question.
 * </p>
 */
public enum RemoteKeyAction {
    /**
     * The button does nothing at all.
     */
    NONE,

    /**
     * Context sensitive "primary" action: reveal the answer if it is still hidden,
     * mark the answer correct if it is showing, or advance to the next question if
     * the question has already been answered.
     */
    ADVANCE_OR_CORRECT,

    /**
     * Context sensitive "secondary" action: reveal the answer if it is still hidden,
     * mark the answer incorrect if it is showing, or advance to the next question if
     * the question has already been answered.
     */
    ADVANCE_OR_INCORRECT,

    /**
     * Only reveal the answer, never grade it.
     */
    SHOW_ANSWER,

    /**
     * Only mark correct, and advance when already answered.
     */
    CORRECT,

    /**
     * Only mark incorrect, and advance when already answered.
     */
    INCORRECT,

    /**
     * Advance to the next question when the current one has been answered.
     */
    NEXT,

    /**
     * Play the audio for the current subject.
     */
    PLAY_AUDIO,

    /**
     * Perform the action of special button 1.
     */
    SPECIAL_BUTTON_1,

    /**
     * Perform the action of special button 2.
     */
    SPECIAL_BUTTON_2,

    /**
     * Perform the action of special button 3.
     */
    SPECIAL_BUTTON_3
}
