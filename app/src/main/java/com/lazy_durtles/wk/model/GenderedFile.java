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

import java.io.File;

import javax.annotation.Nullable;

/**
 * A file subclass for audio files that carry a boolean to indicate gender.
 */
public final class GenderedFile extends File {
    /**
     * For serialization.
     */
    private static final long serialVersionUID = -4750279007205277109L;

    /**
     * True if this file represents an audio file with a male voice.
     */
    private final boolean male;

    /**
     * The constructor.
     *
     * @param parent the parent directory
     * @param child the child name
     * @param male true if the voice in the audio file is male
s     */
    public GenderedFile(final @Nullable File parent, final String child, final boolean male) {
        super(parent, child);
        this.male = male;
    }

    /**
     * True if this file represents an audio file with a male voice.
     *
     * @return true if male
     */
    public boolean isMale() {
        return male;
    }
}
