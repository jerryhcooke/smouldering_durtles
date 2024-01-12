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

package com.smouldering_durtles.wk.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import javax.annotation.Nullable;

/**
 * A ScrollView subclass that detects horizontal swipe gestures without interfering with scrolling and child views.
 */
public final class SwipingScrollView extends ScrollView {
    private @Nullable OnSwipeListener swipeListener = null;
    private boolean active = false;
    private float startX = 0;
    private float startY = 0;
    private float endX = 0;
    private float endY = 0;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SwipingScrollView(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes for this instance
     */
    public SwipingScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attributes for this instance
     * @param defStyleAttr default style attribute ID
     */
    public SwipingScrollView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * The listener that will be informed of swipe-left and swipe-right events.
     * @param swipeListener the value
     */
    public void setSwipeListener(final OnSwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    private void handleTouchEvent(final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!active) {
                active = true;
                startX = event.getRawX();
                startY = event.getRawY();
                endX = startX;
                endY = startY;
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (active) {
                endX = event.getRawX();
                endY = event.getRawY();

            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (active) {
                active = false;
                final float deltaX = endX - startX;
                final float deltaY = endY - startY;
                final float density = getContext().getResources().getDisplayMetrics().density;
                final boolean horizontal = Math.abs(deltaX) > Math.abs(deltaY);
                if (swipeListener != null && horizontal && deltaX < -100 * density) {
                    swipeListener.onSwipeRight(this);
                }
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            active = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        handleTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        handleTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    /**
     * Listener interface for receive swipe events.
     */
    public interface OnSwipeListener {

        /**
         * Called when a swipe right is detected.
         * @param view the view triggering the swipe event
         */
        void onSwipeRight(@SuppressWarnings("unused") SwipingScrollView view);
    }
}
