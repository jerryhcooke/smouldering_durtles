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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.lifecycle.LifecycleOwner;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.model.TimeLine;
import com.smouldering_durtles.wk.proxy.ViewProxy;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view that shows the buttons on the dashboard for starting/resuming a session.
 */
public final class SessionButtonsView extends LinearLayout {
    private final ViewProxy resumeButton = new ViewProxy();
    private final ViewProxy resumeButtonRow = new ViewProxy();
    private final ViewProxy startLessonsButton = new ViewProxy();
    private final ViewProxy startReviewsButton = new ViewProxy();
    private final ViewProxy startButtonsRow = new ViewProxy();

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SessionButtonsView(final Context context) {
        super(context);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs   attribute set
     */
    public SessionButtonsView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initialize the view by observing the relevant LiveData instances.
     */
    private void init() {
        safe(() -> {
            inflate(getContext(), R.layout.session_buttons, this);
            setOrientation(HORIZONTAL);

            resumeButton.setDelegate(this, R.id.resumeButton);
            resumeButtonRow.setDelegate(this, R.id.resumeButtonRow);
            startLessonsButton.setDelegate(this, R.id.startLessonsButton);
            startReviewsButton.setDelegate(this, R.id.startReviewsButton);
            startButtonsRow.setDelegate(this, R.id.startButtonsRow);
        });
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param lifecycleOwner the lifecycle owner
     */
    public void setLifecycleOwner(final LifecycleOwner lifecycleOwner) {
        safe(() -> LiveTimeLine.getInstance().observe(lifecycleOwner, t -> safe(() -> {
            if (t != null) {
                update(t);
            }
        })));
    }

    /**
     * Update the text based on the latest TimeLine data available.
     *
     * @param timeLine the timeline
     */
    private void update(final TimeLine timeLine) {
        if (GlobalSettings.getFirstTimeSetup() == 0 || !Session.getInstance().isLoaded()) {
            setVisibility(GONE);
            return;
        }

        boolean resumeButtonVisible = false;
        boolean startLessonButtonVisible = false;
        boolean startReviewButtonVisible = false;
        boolean startRowVisible = false;

        if (Session.getInstance().isInactive()) {
            if (timeLine.hasAvailableLessons()) {
                startLessonButtonVisible = true;
                startRowVisible = true;
            }
            if (timeLine.hasAvailableReviews()) {
                startReviewButtonVisible = true;
                startRowVisible = true;
            }
        } else {
            resumeButtonVisible = true;
        }

        resumeButton.setVisibility(resumeButtonVisible);
        resumeButtonRow.setVisibility(resumeButtonVisible);
        startLessonsButton.setVisibility(startLessonButtonVisible);
        startReviewsButton.setVisibility(startReviewButtonVisible);
        startButtonsRow.setVisibility(startRowVisible);
        setVisibility(startRowVisible || resumeButtonVisible ? VISIBLE : GONE);
    }

    /**
     * Enable the buttons.
     */
    public void enableInteraction() {
        safe(() -> {
            startLessonsButton.enableInteraction();
            startReviewsButton.enableInteraction();
            resumeButton.enableInteraction();
        });
    }

    /**
     * Disable the buttons.
     */
    public void disableInteraction() {
        safe(() -> {
            startLessonsButton.disableInteraction();
            startReviewsButton.disableInteraction();
            resumeButton.disableInteraction();
        });
    }
}
