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

package com.smouldering_durtles.wk.model;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.SimpleColorFilter;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.SubjectCardLayout;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.proxy.ViewProxy;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A class that can bind a subject's data to a subject card view, as directed by configuration properties.
 */
public final class SubjectCardBinder {
    private final SubjectCardLayout layout;
    private final List<Integer> layoutIds = new ArrayList<>();

    /**
     * The constructor.
     * @param layout the layout style
     */
    public SubjectCardBinder(final SubjectCardLayout layout) {
        this.layout = layout;
        if (layout == SubjectCardLayout.NORMAL) {
            layoutIds.add(R.layout.subject_card_radical);
            layoutIds.add(R.layout.subject_card_kanji);
            layoutIds.add(R.layout.subject_card_vocabulary);
            layoutIds.add(R.layout.subject_card_kana_vocabulary); //added kana_vocabulary
        }
        else {
            layoutIds.add(R.layout.subject_card_radical_compact);
            layoutIds.add(R.layout.subject_card_kanji_compact);
            layoutIds.add(R.layout.subject_card_vocabulary_compact);
            layoutIds.add(R.layout.subject_card_kana_vocabulary_compact); //added kana_vocabulary
        }
    }

    /**
     * Create a view for a subject type with the given parent. The returned view is not bound to anything yet,
     * but it's suitable to be passed to a later bind() call.
     *
     * @param subjectType the subject's type
     * @param parent the parent view this will belong to
     * @return the view
     */
    public View createView(final SubjectType subjectType, final ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (subjectType.isRadical()) {
            return inflater.inflate(layoutIds.get(0), parent, false);
        }
        if (subjectType.isKanji()) {
            return inflater.inflate(layoutIds.get(1), parent, false);
        }
        if (subjectType.isVocabulary()) {
            return inflater.inflate(layoutIds.get(2), parent, false);
        }
        if (subjectType.isKanaVocabulary()) { // Add this block for kana vocabulary
            return inflater.inflate(layoutIds.get(3), parent, false);
        }
        return inflater.inflate(layoutIds.get(3), parent, false); // returns kana_vocabulary as a fallback
    }

    private static void bindCommon(final View view, final Subject subject, final View.OnClickListener onClickListener,
                            final boolean showMeaning, final boolean showReading) {
        final ViewProxy button = new ViewProxy(view, R.id.button);
        final ViewProxy meaning = new ViewProxy(view, R.id.meaning);
        final ViewProxy reading = new ViewProxy(view, R.id.reading);

        view.setBackgroundColor(subject.getButtonBackgroundColor());

        button.setSubject(subject);
        button.setSizeSp(24);
        button.setTransparent(true);

        if (subject.hasMeanings() && showMeaning) {
            meaning.setText(subject.getOneMeaning());
            meaning.setVisibility(true);
        }
        else {
            meaning.setVisibility(false);
        }

        if (subject.hasReadings() && showReading) {
            reading.setText(subject.getOneReading());
            reading.setJapaneseLocale();
            reading.setVisibility(true);
        }
        else if (subject.getType().isKanaVocabulary()) {
            reading.setText(subject.getCharacters());
            reading.setVisibility(true);
        }
        else {
            reading.setVisibility(false);
        }

        view.setOnClickListener(onClickListener);
        button.setOnClickListener(onClickListener);

        final @Nullable Drawable bgDrawable = ContextCompat.getDrawable(view.getContext(), R.drawable.small_rounded_corners);
        if (bgDrawable != null) {
            bgDrawable.setColorFilter(new SimpleColorFilter(subject.getButtonBackgroundColor()));
            view.setBackground(bgDrawable);
        }
    }

    private static void bindNormal(final View view, final Subject subject) {
        final ViewProxy progress = new ViewProxy(view, R.id.progress);

        final SrsSystem.Stage stage = subject.getSrsStage();
        final boolean isVocabulary = subject.getType().isVocabulary();
        final boolean isKanaVocabulary = subject.getType().isKanaVocabulary();
        final String stageName = isVocabulary || isKanaVocabulary ? stage.getName() : stage.getShortName();
        if (stage.isLocked()) {
            progress.setVisibility(false);
        }
        else if (subject.getAvailableAt() == 0) {
            progress.setText(stageName);
            progress.setVisibility(true);
        }
        else {
            progress.setText(stageName + " - " + subject.getShortNextReviewWaitTime());
            progress.setVisibility(true);
        }
    }

    private void bindCompact(final View view, final Subject subject) {
        final ViewProxy waitTime = new ViewProxy(view, R.id.waitTime);
        final ViewProxy stageLetter = new ViewProxy(view, R.id.stageLetter);

        if (layout == SubjectCardLayout.COMPACT_NO_PROGRESSION) {
            waitTime.setVisibility(false);
            stageLetter.setVisibility(false);
            return;
        }

        final long availableAt = subject.getAvailableAt();
        final boolean availableNow = availableAt != 0 && availableAt < System.currentTimeMillis();
        if (availableAt == 0) {
            waitTime.setVisibility(false);
        }
        else {
            waitTime.setText(subject.getShortNextReviewWaitTime());
            waitTime.setVisibility(true);
        }
        final @Nullable Drawable bgDrawable = ContextCompat.getDrawable(view.getContext(), R.drawable.small_rounded_corners);
        if (bgDrawable != null) {
            final int textColor;
            final int bgColor;
            if (availableNow) {
                textColor = ThemeUtil.getColor(R.attr.colorBackground);
                bgColor = ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary);
            }
            else {
                textColor = ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary);
                bgColor = ThemeUtil.getColor(R.attr.colorBackground);
            }

            if (ThemeUtil.isLightColor(textColor)) {
                waitTime.setShadowLayer(3, 1, 1, Color.BLACK);
            }
            else {
                waitTime.setShadowLayer(0, 0, 0, 0);
            }

            bgDrawable.setColorFilter(new SimpleColorFilter(bgColor));
            waitTime.setBackground(bgDrawable);
            waitTime.setTextColor(textColor);
        }

        final SrsSystem.Stage stage = subject.getSrsStage();
        final String letter = stage.getNameLetter();
        final @Nullable Drawable stageBgDrawable = ContextCompat.getDrawable(view.getContext(), R.drawable.small_rounded_corners);
        if (stageBgDrawable != null) {
            final int textColor;
            final int bgColor = ActiveTheme.getShallowStageBucketColors7()[stage.getGeneralStageBucket()];

            if (ThemeUtil.isLightColor(bgColor)) {
                stageLetter.setShadowLayer(0, 0, 0, 0);
                textColor = ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimaryDark);
            }
            else {
                stageLetter.setShadowLayer(3, 1, 1, Color.BLACK);
                textColor = ThemeUtil.getColor(R.attr.colorPrimaryLight);
            }

            stageLetter.setTextColor(textColor);
            stageBgDrawable.setColorFilter(new SimpleColorFilter(bgColor));
            stageLetter.setBackground(stageBgDrawable);
        }
        stageLetter.setText(letter);
    }

    /**
     * Bind a subject to a view, filling in all of the relevant details, and setting an onClick listener.
     * This does not register a SubjectChangeListener.
     * @param view the view to bind to
     * @param subject the subject to bind to the view
     * @param onClickListener the onClick listener for the view
     * @param showMeaning show a meaning if the subject has meanings
     * @param showReading show a reading if the subject has readings
     */
    public void bind(final View view, final Subject subject, final View.OnClickListener onClickListener,
                     final boolean showMeaning, final boolean showReading) {
        bindCommon(view, subject, onClickListener, showMeaning, showReading);
        if (layout == SubjectCardLayout.NORMAL) {
            bindNormal(view, subject);
        }
        else {
            bindCompact(view, subject);
        }
    }
}
