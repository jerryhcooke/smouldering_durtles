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
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.api.model.Reading;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.SubjectInfoDump;
import com.smouldering_durtles.wk.model.FloatingUiState;
import com.smouldering_durtles.wk.model.GenderedFile;
import com.smouldering_durtles.wk.model.PitchInfo;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.AudioUtil;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static com.smouldering_durtles.wk.Constants.FONT_SIZE_NORMAL;
import static com.smouldering_durtles.wk.Constants.FONT_SIZE_SMALL;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ViewUtil.getNearestEnclosingViewOfType;

/**
 * Custom view that shows the headline of the subject info dump. It is only used
 * as a child of SubjectInfoView.
 */
public final class SubjectInfoHeadlineView extends ConstraintLayout {
    private final ViewProxy button = new ViewProxy();
    private final ViewProxy title = new ViewProxy();
    private final ViewProxy meaning = new ViewProxy();
    private final ViewProxy reading = new ViewProxy();
    private final ViewProxy revealButton = new ViewProxy();
    private final ViewProxy buttonsColumn = new ViewProxy();

    private @Nullable Toolbar toolbar;
    private int maxFontSize = 0;
    private int textSize = FONT_SIZE_NORMAL;
    private @Nullable Subject subject = null;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SubjectInfoHeadlineView(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public SubjectInfoHeadlineView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyleAttr the default style
     */
    public SubjectInfoHeadlineView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * The the toolbar for the activity showing this info dump. If set,
     * this view will update the title based on the subject contents.
     *
     * @param toolbar the toolbar instance
     */
    public void setToolbar(final @Nullable Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    /**
     * The maximum font size allowed for the subject title in SP.
     * @param maxFontSize the value
     */
    public void setMaxFontSize(final int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

    /**
     * The normal text size for the text in the headline.
     * @param textSize the value
     */
    public void setTextSize(final int textSize) {
        this.textSize = textSize;
    }

    private SubjectInfoDump getSubjectInfoDump() {
        final @Nullable SubjectInfoView parent = getNearestEnclosingViewOfType(this, SubjectInfoView.class);
        if (parent == null) {
            return SubjectInfoDump.ALL;
        }
        return parent.getSubjectInfoDump();
    }

    /**
     * Set the subject to show in this instance.
     *
     * @param newSubject the subject
     */
    public void setSubject(final Subject newSubject) {
        safe(() -> {
            subject = newSubject;
            layoutSubject();
        });
    }

    private void layoutSubject() {
        if (subject == null) {
            return;
        }

        final boolean showMeaningAnswers = getSubjectInfoDump().getShowMeaningAnswers(Session.getInstance().getCurrentQuestion());
        final boolean showReadingAnswers = getSubjectInfoDump().getShowReadingAnswers(Session.getInstance().getCurrentQuestion());

        if (toolbar != null) {
            toolbar.setTitle(subject.getInfoTitle("", ""));
            toolbar.setBackgroundColor(subject.getBackgroundColor());
        }

        final boolean showPitchInfo = GlobalSettings.SubjectInfo.getShowPitchInfo() && subject.hasPitchInfo() && showReadingAnswers;
        boolean wide = false;

        if (maxFontSize == 0) {
            removeAllViews();
            inflate(getContext(), R.layout.subject_info_headline_narrow, this);
            button.setDelegate(this, R.id.button);
            title.setDelegate(this, R.id.title);
            meaning.setDelegate(this, R.id.meaning);
            reading.setDelegate(this, R.id.reading);
            revealButton.setDelegate(this, R.id.revealButton);
            buttonsColumn.setDelegate(this, R.id.buttonsColumn);
            button.setVisibility(false);
        }
        else {
            final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            final int maxWidthPx = displayMetrics.widthPixels - dp2px(80);

            final SubjectInfoButtonView testView = new SubjectInfoButtonView(getContext());
            testView.setSubject(subject);
            testView.setMaxSize(maxWidthPx, sp2px(maxFontSize));
            final int textWidthPx = testView.getCalculatedWidth();

            removeAllViews();
            if (textWidthPx * 10 >= maxWidthPx * 4 || showPitchInfo && textWidthPx * 20 >= maxWidthPx * 3) {
                inflate(getContext(), R.layout.subject_info_headline_wide, this);
                wide = true;
            }
            else {
                inflate(getContext(), R.layout.subject_info_headline_narrow, this);
            }
            button.setDelegate(this, R.id.button);
            title.setDelegate(this, R.id.title);
            meaning.setDelegate(this, R.id.meaning);
            reading.setDelegate(this, R.id.reading);
            revealButton.setDelegate(this, R.id.revealButton);
            buttonsColumn.setDelegate(this, R.id.buttonsColumn);

            button.setSubject(subject);
            button.setMaxSize(maxWidthPx, sp2px(maxFontSize));
            button.setVisibility(true);
        }

        // Title text
        title.setText(subject.getSimpleInfoTitle());

        // Meaning text
        meaning.setText(subject.getMeaningRichText(""));
        meaning.setVisibility(showMeaningAnswers && subject.hasMeanings());

        // Reading text
        if (showPitchInfo) {
            reading.setVisibility(false);

            int prevId = R.id.reading;
            for (final Reading r: subject.getReadings()) {
                final @Nullable String kana = r.getReading();
                if (isEmpty(kana)) {
                    continue;
                }
                final List<PitchInfo> pitchInfos = subject.getPitchInfoFor(kana);
                if (!pitchInfos.isEmpty()) {
                    final PitchInfoView pitchInfoView = new PitchInfoView(getContext());
                    pitchInfoView.setId(ViewCompat.generateViewId());
                    pitchInfoView.setTextSize(textSize);
                    pitchInfoView.setPitchInfo(kana, pitchInfos);
                    final LayoutParams params = new LayoutParams(0, 0);
                    params.width = MATCH_CONSTRAINT;
                    params.height = WRAP_CONTENT;
                    params.topToBottom = prevId;
                    if (wide) {
                        params.setMargins(0, dp2px(4), 0, 0);
                        params.startToStart = PARENT_ID;
                    }
                    else {
                        params.setMargins(dp2px(8), dp2px(4), 0, 0);
                        params.startToEnd = R.id.button;
                    }
                    params.endToStart = R.id.buttonsColumn;
                    addView(pitchInfoView, params);
                    prevId = pitchInfoView.getId();
                }
            }
        }
        else {
            reading.setText(subject.getRegularReadingRichText(""));
            reading.setVisibility(showReadingAnswers && subject.hasReadings());
            reading.setJapaneseLocale();
        }

        // Play audio button(s)
        if (showReadingAnswers || subject.getType().isKanaVocabulary()) {
            if (subject.getType().isKanaVocabulary() || !subject.getReadings().isEmpty()) {
                final Button playButton = new Button(getContext());
                playButton.setText(subject.getCharacters());
                playButton.setTextSize(FONT_SIZE_SMALL);
                playButton.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_volume_up_24px), null, null, null);
                playButton.setOnClickListener(v -> safe(() -> AudioUtil.playAudio(subject, null)));
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, 0);
                layoutParams.width = WRAP_CONTENT;
                layoutParams.height = WRAP_CONTENT;
                buttonsColumn.addView(playButton, layoutParams);
            } else {
                for (final Reading r : subject.getReadings()) {
                    final Button playButton = new Button(getContext());
                    playButton.setText(r.getValue(GlobalSettings.Other.getShowOnInKatakana()));
                    playButton.setTextSize(FONT_SIZE_SMALL);
                    playButton.setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_volume_up_24px), null, null, null);
                    playButton.setOnClickListener(v -> safe(() -> AudioUtil.playAudio(subject, r.getReading())));
                    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, 0);
                    layoutParams.width = WRAP_CONTENT;
                    layoutParams.height = WRAP_CONTENT;
                    buttonsColumn.addView(playButton, layoutParams);
                }
            }
        }

        revealButton.setText(getSubjectInfoDump().getRevealButtonLabel());

        // Show all button
        revealButton.setOnClickListener(v -> safe(() -> {
            final @Nullable SubjectInfoDump dump = FloatingUiState.showDumpStage;
            if (dump != null) {
                FloatingUiState.showDumpStage = dump.getNextStage();
            }
            final @Nullable SubjectInfoView parent = getNearestEnclosingViewOfType(this, SubjectInfoView.class);
            if (parent != null) {
                parent.layoutSubject(true);
            }
            layoutSubject();
        }));
        revealButton.setVisibility(getSubjectInfoDump().getShowRevealButton());

        title.setTextSize(textSize);
        meaning.setTextSize(textSize + 4);
        reading.setTextSize(textSize + 4);
    }

    private int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int sp2px(final int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
