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

package com.smouldering_durtles.wk.proxy;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.madrapps.pikolo.ColorPicker;
import com.madrapps.pikolo.listeners.OnColorSelectionListener;
import com.smouldering_durtles.wk.Actment;
import com.smouldering_durtles.wk.model.Question;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.components.CustomMovementMethod;
import com.smouldering_durtles.wk.model.AdvancedSearchParameters;
import com.smouldering_durtles.wk.model.SrsBreakDown;
import com.smouldering_durtles.wk.model.TypefaceConfiguration;
import com.smouldering_durtles.wk.util.ViewUtil;
import com.smouldering_durtles.wk.views.AdvancedSearchFormView;
import com.smouldering_durtles.wk.views.LevelProgressBarView;
import com.smouldering_durtles.wk.views.Post60ProgressBarView;
import com.smouldering_durtles.wk.views.StarRatingView;
import com.smouldering_durtles.wk.views.StrokeDiagramView;
import com.smouldering_durtles.wk.views.SubjectGridView;
import com.smouldering_durtles.wk.views.SubjectInfoButtonView;
import com.smouldering_durtles.wk.views.SubjectInfoHeadlineView;
import com.smouldering_durtles.wk.views.SubjectInfoView;
import com.smouldering_durtles.wk.views.SwipingScrollView;
import com.smouldering_durtles.wk.views.SynonymRowView;
import com.smouldering_durtles.wk.enums.SessionType;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nullable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.smouldering_durtles.wk.util.TextUtil.renderHtml;

/**
 * A proxy class that delegates some method calls to a delegate object, but only if the delegate is not null and
 * of the appropriate type.
 * Basically, this eliminates a lot of annoying boilerplate null checks and instanceof checks.
 */
@SuppressWarnings("JavaDoc")
public final class ViewProxy {
    private @Nullable WeakReference<View> delegateReference;

    /**
     * The constructor.
     */
    public ViewProxy() {
        this(null);
    }

    /**
     * The constructor.
     *
     * @param delegate the delegate to use
     */
    private ViewProxy(final @Nullable View delegate) {
        delegateReference = delegate == null ? null : new WeakReference<>(delegate);
    }

    /**
     * The constructor.
     *
     * @param parent parent of the delegate view
     * @param id the ID of the delegate view
     */
    public ViewProxy(final View parent, final int id) {
        this(parent.findViewById(id));
    }

    /**
     * The constructor.
     *
     * @param parent parent of the delegate view
     * @param id the ID of the delegate view
     */
    public ViewProxy(final Activity parent, final int id) {
        this(parent.findViewById(id));
    }

    /**
     * Get the delegate for this proxy.
     *
     * @return  the delegate for this proxy
     */
    public @Nullable View getDelegate() {
        if (delegateReference == null) {
            return null;
        }
        return delegateReference.get();
    }

    /**
     * Set the delegate for this proxy.
     *
     * @param delegate the delegate to use
     */
    private void setDelegate(final @Nullable View delegate) {
        delegateReference = delegate == null ? null : new WeakReference<>(delegate);
    }

    /**
     * Set the delegate for this proxy.
     *
     * @param parent parent of the delegate view
     * @param id the ID of the delegate view
     */
    public void setDelegate(final View parent, final int id) {
        setDelegate(parent.findViewById(id));
    }

    /**
     * Set the delegate for this proxy.
     *
     * @param parent parent of the delegate view
     * @param id the ID of the delegate view
     */
    public void setDelegate(final Activity parent, final int id) {
        setDelegate(parent.findViewById(id));
    }

    public @Nullable Object getTag() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            return delegate.getTag();
        }
        return null;
    }

    public @Nullable Object getTag(final int key) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            return delegate.getTag(key);
        }
        return null;
    }

    public void setTag(final @Nullable Object tag) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setTag(tag);
        }
    }

    public void setTag(final int key, final @Nullable Object tag) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setTag(key, tag);
        }
    }

    public void startAnimation(final Animation animation) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.startAnimation(animation);
        }
    }

    public void clearAnimation() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.clearAnimation();
        }
    }

    public void setAnimation(final int id) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LottieAnimationView) {
            ((LottieAnimationView) delegate).setAnimation(id);
        }
    }

    public void setAnimation(final AnimationSet animation) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setAnimation(animation);
        }
    }

    public void setRepeatCount(final int count) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LottieAnimationView) {
            ((LottieAnimationView) delegate).setRepeatCount(count);
        }
    }

    public void addAnimatorListener(final Animator.AnimatorListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LottieAnimationView) {
            ((LottieAnimationView) delegate).addAnimatorListener(listener);
        }
    }

    public void removeAnimatorListener(final Animator.AnimatorListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LottieAnimationView) {
            ((LottieAnimationView) delegate).removeAnimatorListener(listener);
        }
    }

    public void playAnimation() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LottieAnimationView) {
            ((LottieAnimationView) delegate).playAnimation();
        }
    }

    public void setTransparent(final boolean transparent) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setTransparent(transparent);
        }
    }

    public void setSizeForQuiz(final boolean sizeForQuiz) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setSizeForQuiz(sizeForQuiz);
        }
    }

    public void setTypefaceConfiguration(final TypefaceConfiguration typefaceConfiguration) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setTypefaceConfiguration(typefaceConfiguration);
        }
    }

    public @Nullable TypefaceConfiguration getTypefaceConfiguration() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            return ((SubjectInfoButtonView) delegate).getTypefaceConfiguration();
        }
        return null;
    }

    public void setSubject(final Subject subject) {
        setSubject(subject, null, null);
    }

    public void setSubject(final Subject subject, @Nullable final Question question, @Nullable final SessionType sessionType) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setSubject(subject, question, sessionType);
        }
        else if (delegate instanceof SubjectInfoHeadlineView) {
            ((SubjectInfoHeadlineView) delegate).setSubject(subject);
        }
        else if (delegate instanceof StarRatingView) {
            ((StarRatingView) delegate).setSubject(subject);
        }
    }

    public void setSubject(final Actment actment, final Subject subject) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoView) {
            ((SubjectInfoView) delegate).setSubject(actment, subject);
        }
    }

    public void setSubjects(final Actment actment, final Iterable<Subject> subjects, final boolean showMeaning, final boolean showReading) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectGridView) {
            ((SubjectGridView) delegate).setSubjects(actment, subjects, showMeaning, showReading);
        }
    }

    public void setSubjectIds(final Actment actment, final Collection<Long> subjectIds, final boolean showMeaning, final boolean showReading) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectGridView) {
            ((SubjectGridView) delegate).setSubjectIds(actment, subjectIds, showMeaning, showReading);
        }
    }

    public void removeSubject(final long id) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectGridView) {
            ((SubjectGridView) delegate).removeSubject(id);
        }
    }

    public void setToolbar(final @Nullable Toolbar toolbar) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoView) {
            ((SubjectInfoView) delegate).setToolbar(toolbar);
        }
        if (delegate instanceof SubjectInfoHeadlineView) {
            ((SubjectInfoHeadlineView) delegate).setToolbar(toolbar);
        }
    }

    public void setMaxFontSize(final int maxFontSize) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoView) {
            ((SubjectInfoView) delegate).setMaxFontSize(maxFontSize);
        }
        if (delegate instanceof SubjectInfoHeadlineView) {
            ((SubjectInfoHeadlineView) delegate).setMaxFontSize(maxFontSize);
        }
    }

    public void setMaxSize(final int maxWidthPx, final int maxHeightPx) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setMaxSize(maxWidthPx, maxHeightPx);
        }
    }

    public void setSizeSp(final int sizeSp) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoButtonView) {
            ((SubjectInfoButtonView) delegate).setSizeSp(sizeSp);
        }
    }

    public void setSize(final int size) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof StrokeDiagramView) {
            ((StrokeDiagramView) delegate).setSize(size);
        }
    }

    public void setAnimated(final boolean animated) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof StrokeDiagramView) {
            ((StrokeDiagramView) delegate).setAnimated(animated);
        }
    }

    public void setStrokeData(final Collection<String> strokeData) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof StrokeDiagramView) {
            ((StrokeDiagramView) delegate).setStrokeData(strokeData);
        }
    }

    public void setContainerType(final SubjectInfoView.ContainerType containerType) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SubjectInfoView) {
            ((SubjectInfoView) delegate).setContainerType(containerType);
        }
    }

    public void setBreakdown(final SrsBreakDown breakdown) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Post60ProgressBarView) {
            ((Post60ProgressBarView) delegate).setBreakdown(breakdown);
        }
    }

    public void setValues(final int[] values) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LevelProgressBarView) {
            ((LevelProgressBarView) delegate).setValues(values);
        }
    }

    public void setShowTarget(final boolean showTarget) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof LevelProgressBarView) {
            ((LevelProgressBarView) delegate).setShowTarget(showTarget);
        }
    }

    public void setSwipeListener(final SwipingScrollView.OnSwipeListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof SwipingScrollView) {
            ((SwipingScrollView) delegate).setSwipeListener(listener);
        }
    }

    public void setColor(final int color) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ColorPicker) {
            ((ColorPicker) delegate).setColor(color);
        }
    }

    public void setColorSelectionListener(final OnColorSelectionListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ColorPicker) {
            ((ColorPicker) delegate).setColorSelectionListener(listener);
        }
    }

    public String getText() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            final @Nullable CharSequence text = ((TextView) delegate).getText();
            return text == null ? "" : text.toString();
        }
        else if (delegate instanceof SynonymRowView) {
            return ((SynonymRowView) delegate).getText();
        }
        return "";
    }

    public void setText(final @Nullable CharSequence text) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setText(text == null ? "" : text);
        }
        else if (delegate instanceof SynonymRowView) {
            ((SynonymRowView) delegate).setText(text == null ? "" : text);
        }
    }

    public void setText(final int n) {
        setText(Integer.toString(n));
    }

    public void setTextHtml(final String html) {
        setText(renderHtml(html));
    }

    public void setTextFormat(final String format, final Object... values) {
        setText(String.format(Locale.ROOT, format, values));
    }

    public void setTextOrBlankIfZero(final int n) {
        if (n == 0) {
            setText("");
        }
        else {
            setText(n);
        }
    }

    public void setImageDrawable(final Drawable drawable) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ImageView) {
            ((ImageView) delegate).setImageDrawable(drawable);
        }
    }

    public void setVisibility(final boolean visible) {
        setVisibility(visible ? VISIBLE : GONE);
    }

    public void setVisibility(final int visibility) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setVisibility(visibility);
        }
    }

    public void setParentVisibility(final boolean visible) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            final @Nullable ViewParent parent = delegate.getParent();
            if (parent instanceof View) {
                ((View) parent).setVisibility(visible ? VISIBLE : GONE);
            }
        }
    }

    public void enableInteraction() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setClickable(true);
            delegate.setEnabled(true);
        }
    }

    public void disableInteraction() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setClickable(false);
            delegate.setEnabled(false);
        }
    }

    public void requestFocus() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null && !delegate.isInTouchMode()) {
            delegate.setFocusable(true);
            delegate.requestFocus();
        }
    }

    public void requestFocusInTouchMode() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setFocusable(true);
            delegate.requestFocus();
        }
    }

    public void setRootLocale() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ViewUtil.setRootLocale((TextView) delegate);
        }
    }

    public void setJapaneseLocale() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ViewUtil.setJapaneseLocale((TextView) delegate);
        }
    }

    public void setTextColor(final int color) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setTextColor(color);
        }
    }

    public void setHintTextColor(final int color) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setHintTextColor(color);
        }
    }

    public void setTextSize(final float size) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setTextSize(size);
        }
    }

    public void setHint(final CharSequence hint) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setHint(hint);
        }
    }

    public void setImeOptions(final int imeOptions) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setImeOptions(imeOptions);
        }
    }

    public void setImeHintLocales(final Locale locale) {
        final @Nullable View delegate = getDelegate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && delegate instanceof TextView) {
            ((TextView) delegate).setImeHintLocales(new LocaleList(locale));
        }
    }

    public void setImeHintLocales() {
        final @Nullable View delegate = getDelegate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && delegate instanceof TextView) {
            ((TextView) delegate).setImeHintLocales(null);
        }
    }

    public void setInputType(final int inputType) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setInputType(inputType);
        }
    }

    public void setGravity(final int gravity) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setGravity(gravity);
        }
    }

    public void setMinEms(final int minEms) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setMinEms(minEms);
        }
    }

    public void setSingleLine() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setSingleLine();
        }
    }

    public void setMaxLines(final int maxLines) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setMaxLines(maxLines);
        }
    }

    public void setHorizontallyScrolling(final boolean horizontallyScrolling) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setHorizontallyScrolling(horizontallyScrolling);
        }
    }

    public void setContentDescription(final CharSequence contentDescription) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setContentDescription(contentDescription);
        }
    }

    public void addTextChangedListener(final TextWatcher textWatcher) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).addTextChangedListener(textWatcher);
        }
    }

    public void setBackgroundColor(final int color) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setBackgroundColor(color);
        }
    }

    public void setBackground(final Drawable drawable) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setBackground(drawable);
        }
    }

    public void setShadowLayer(final float radius, final float dx, final float dy, final int color) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setShadowLayer(radius, dx, dy, color);
        }
    }

    public @Nullable Drawable getBackground() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            return delegate.getBackground();
        }
        return null;
    }

    public void setClickableAndNotFocusable(final boolean clickable) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setClickable(clickable);
            delegate.setFocusable(false);
        }
    }

    public void setOnClickListener(final @Nullable View.OnClickListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setOnClickListener(listener);
        }
    }

    public void setOnEditorActionListener(final TextView.OnEditorActionListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setOnEditorActionListener(listener);
        }
    }

    public void setOnCheckedChangeListener(final CompoundButton.OnCheckedChangeListener listener) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof CompoundButton) {
            ((CompoundButton) delegate).setOnCheckedChangeListener(listener);
        }
    }

    public boolean isChecked() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Checkable) {
            return ((Checkable) delegate).isChecked();
        }
        return false;
    }

    public void setChecked(final boolean checked) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Checkable) {
            ((Checkable) delegate).setChecked(checked);
        }
    }

    public void setTypeface(final Typeface typeface) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setTypeface(typeface);
        }
    }

    public void setLinkMovementMethod() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof TextView) {
            ((TextView) delegate).setMovementMethod(new CustomMovementMethod());
        }
    }

    public void removeAllViews() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ViewGroup) {
            ((ViewGroup) delegate).removeAllViews();
        }
    }

    public void removeViewAt(final int index) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ViewGroup) {
            ((ViewGroup) delegate).removeViewAt(index);
        }
    }

    public @Nullable View getChildAt(final int index) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ViewGroup) {
            return ((ViewGroup) delegate).getChildAt(index);
        }
        return null;
    }

    public void addView(final View child, final ViewGroup.LayoutParams layoutParams) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ViewManager) {
            ((ViewManager) delegate).addView(child, layoutParams);
        }
    }

    public int getChildCount() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ViewGroup) {
            return ((ViewGroup) delegate).getChildCount();
        }
        return 0;
    }

    public @Nullable ViewGroup.LayoutParams getLayoutParams() {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            return delegate.getLayoutParams();
        }
        return null;
    }

    public void setLayoutParams(final ViewGroup.LayoutParams params) {
        final @Nullable View delegate = getDelegate();
        if (delegate != null) {
            delegate.setLayoutParams(params);
        }
    }

    public void setProgress(final int progress) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ProgressBar) {
            ((ProgressBar) delegate).setProgress(progress);
        }
    }

    public void setMax(final int max) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ProgressBar) {
            ((ProgressBar) delegate).setMax(max);
        }
    }

    public int getMax() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof ProgressBar) {
            return ((ProgressBar) delegate).getMax();
        }
        return 0;
    }

    public @Nullable AdvancedSearchParameters extractParameters() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof AdvancedSearchFormView) {
            return ((AdvancedSearchFormView) delegate).extractParameters();
        }
        return null;
    }

    public void injectParameters(final AdvancedSearchParameters parameters) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof AdvancedSearchFormView) {
            ((AdvancedSearchFormView) delegate).injectParameters(parameters);
        }
    }

    public void setSearchButtonLabel(final String label) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof AdvancedSearchFormView) {
            ((AdvancedSearchFormView) delegate).setSearchButtonLabel(label);
        }
    }

    public void setSortOrderVisibility(final boolean visibility) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof AdvancedSearchFormView) {
            ((AdvancedSearchFormView) delegate).setSortOrderVisibility(visibility);
        }
    }

    public @Nullable RecyclerView.LayoutManager getLayoutManager() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof RecyclerView) {
            return ((RecyclerView) delegate).getLayoutManager();
        }
        return null;
    }

    public void setLayoutManager(final RecyclerView.LayoutManager layoutManager) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof RecyclerView) {
            ((RecyclerView) delegate).setLayoutManager(layoutManager);
        }
    }

    public void setAdapter(final RecyclerView.Adapter<?> adapter) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof RecyclerView) {
            ((RecyclerView) delegate).setAdapter(adapter);
        }
    }

    public void setAdapter(final SpinnerAdapter adapter) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Spinner) {
            //noinspection OverlyStrongTypeCast
            ((Spinner) delegate).setAdapter(adapter);
        }
    }

    public void setArrayAdapter(final ArrayAdapter<?> adapter) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) delegate).setAdapter(adapter);
        }
    }

    public @Nullable Object getSelection() {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Spinner) {
            //noinspection OverlyStrongTypeCast
            return ((Spinner) delegate).getSelectedItem();
        }
        return null;
    }

    public void setSelection(final int position) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof Spinner) {
            //noinspection OverlyStrongTypeCast
            ((Spinner) delegate).setSelection(position);
        }
    }

    public void setHasFixedSize(final boolean value) {
        final @Nullable View delegate = getDelegate();
        if (delegate instanceof RecyclerView) {
            ((RecyclerView) delegate).setHasFixedSize(value);
        }
    }
}
