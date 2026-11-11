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

package com.smouldering_durtles.wk.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import com.madrapps.pikolo.HSLColorPicker;

import javax.annotation.Nullable;

/**
 * Subclass of the color picker that will selfishly keep touch events from ancestor views.
 */
public final class SelfishColorPicker extends HSLColorPicker {
    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SelfishColorPicker(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs declared attributes
     */
    public SelfishColorPicker(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs declared attributes
     * @param defStyleAttr default style attribute
     */
    public SelfishColorPicker(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (super.onTouchEvent(event)) {
            final @Nullable ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
        return super.onTouchEvent(event);
    }
}
