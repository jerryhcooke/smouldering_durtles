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

package com.the_tinkering.wk.views;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.airbnb.lottie.SimpleColorFilter;
import com.the_tinkering.wk.Constants;
import com.the_tinkering.wk.GlobalSettings;
import com.the_tinkering.wk.R;
import com.the_tinkering.wk.db.model.Subject;
import com.the_tinkering.wk.model.TypefaceConfiguration;
import com.the_tinkering.wk.util.Logger;
import com.the_tinkering.wk.util.ObjectSupport;
import com.the_tinkering.wk.util.ViewUtil;

import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Custom view to show the subject info button with the subject's text or radical image.
 */
public final class SubjectInfoButtonView extends View {
    private static final Logger LOGGER = Logger.get(SubjectInfoButtonView.class);
    private static final Pattern SEARCH_URL_PATTERN = Pattern.compile("%s");

    private TypefaceConfiguration typefaceConfiguration = TypefaceConfiguration.DEFAULT;
    private String characters = "";
    private int textColor = 0;
    private @Nullable ColorFilter textColorFilter = null;
    private int backgroundColor = 0;
    private @Nullable Drawable image = null;
    private int sizeSp = -1;
    private int maxWidth = -1;
    private int maxHeight = -1;
    private boolean transparent = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect rect = new Rect();
    private int displayHeight = 0;
    private int topMargin = 0;
    private int bottomMargin = 0;
    private int absoluteMinTextHeight = 0;
    private boolean sizeForQuiz = false;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private int textWidth = 0;
    private int textHeight = 0;
    private int fontPaddingTop = 0;
    private int fontPaddingLeft = 0;
    private @Nullable ActionMode actionMode = null;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public SubjectInfoButtonView(final Context context) {
        super(context);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public SubjectInfoButtonView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyleAttr default style attribute
     */
    public SubjectInfoButtonView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        try {
            displayHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            topMargin = dp2px(28);
            bottomMargin = dp2px(24);
            absoluteMinTextHeight = sp2px(14);

            setLongClickable(true);
            setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    try {
                        if (actionMode != null) {
                            return false;
                        }

                        if (image != null) {
                            Toast.makeText(getContext(), "This radical has no text character. Can't copy or search for an image-only radical.",
                                    Toast.LENGTH_LONG).show();
                            return false;
                        }

                        setSelected(true);

                        actionMode = startActionMode(new ActionMode.Callback() {
                            @Override
                            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                                try {
                                    menu.add(0, Menu.NONE, 1, "Copy title");

                                    for (int i=1; i<=5; i++) {
                                        if (GlobalSettings.Other.hasSearchEngine(i)) {
                                            menu.add(0, Menu.NONE, i+1, GlobalSettings.Other.getSearchEngineName(i));
                                        }
                                    }
                                    return true;
                                } catch (final Exception e) {
                                    LOGGER.uerr(e);
                                    return true;
                                }
                            }

                            @Override
                            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                                return false;
                            }

                            @Override
                            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                                try {
                                    final int order = item.getOrder();
                                    if (order == 1) {
                                        final @Nullable ClipboardManager clipboard =
                                                (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null) {
                                            final ClipData clip = ClipData.newPlainText("title", characters);
                                            clipboard.setPrimaryClip(clip);
                                            Toast.makeText(getContext(), "Subject title copied", Toast.LENGTH_SHORT).show();
                                        }
                                        mode.finish();
                                        return true;
                                    }
                                    if (order >= 2 && order <= 6 && GlobalSettings.Other.hasSearchEngine(order-1)) {
                                        final String query = URLEncoder.encode(characters, "UTF-8");
                                        final String urlPattern = GlobalSettings.Other.getSearchEngineUrl(order-1);
                                        final String url = SEARCH_URL_PATTERN.matcher(urlPattern).replaceAll(query);
                                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        getContext().startActivity(intent);
                                        mode.finish();
                                        return true;
                                    }
                                    return false;
                                } catch (final Exception e) {
                                    LOGGER.uerr(e);
                                    return false;
                                }
                            }

                            @Override
                            public void onDestroyActionMode(final ActionMode mode) {
                                actionMode = null;
                            }
                        });

                        return true;
                    } catch (final Exception e) {
                        LOGGER.uerr(e);
                        return false;
                    }
                }
            });
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * True if this view should be sized specifically for quiz question display.
     *
     * @param sizeForQuiz true if it should
     */
    public void setSizeForQuiz(final boolean sizeForQuiz) {
        this.sizeForQuiz = sizeForQuiz;
    }

    /**
     * Get the width of the text when rendered, taking into account surrounding spacing.
     *
     * @param text the text to measure
     * @return the width in pixels
     */
    private int getWidth(final String text) {
        paint.getTextBounds("ああ", 0, 2, rect);
        final int refWidth = rect.width();
        final String paddedText = String.format(Locale.ROOT, "あ%sあ", text);
        paint.getTextBounds(paddedText, 0, paddedText.length(), rect);
        return rect.width() - refWidth;
    }

    private void binSearch(final int minTextHeight, final int maxTextHeight,
                           final int maxViewWidth, final int maxViewHeight,
                           final int top, final int bottom) {
        int min = minTextHeight;
        int max = maxTextHeight + 1;
        while (max - min >= 2) {
            final int mid = (min + max) / 2;
            final int vh = mid + top + bottom
                    + (mid * (typefaceConfiguration.getPaddingPercTop() + typefaceConfiguration.getPaddingPercBottom())) / 100;
            if (vh > maxViewHeight) {
                max = mid;
                continue;
            }
            paint.setTextSize(mid);
            int vw = getWidth(characters);
            vw += (vw * (typefaceConfiguration.getPaddingPercLeft() + typefaceConfiguration.getPaddingPercRight())) / 100;
            if (vw <= maxViewWidth) {
                min = mid;
            }
            else {
                max = mid;
            }
        }
        paint.setTextSize(min);
        textWidth = getWidth(characters);
        textHeight = min;
        viewWidth = textWidth + (textWidth * (typefaceConfiguration.getPaddingPercLeft() + typefaceConfiguration.getPaddingPercRight())) / 100;
        viewHeight = textHeight + top + bottom
                + (textHeight * (typefaceConfiguration.getPaddingPercTop() + typefaceConfiguration.getPaddingPercBottom())) / 100;
        fontPaddingTop = top + (textHeight * typefaceConfiguration.getPaddingPercTop()) / 100;
        fontPaddingLeft = (textWidth * typefaceConfiguration.getPaddingPercLeft()) / 100;
    }

    /**
     * Precompute the details of this instance.
     */
    private void prepare(final int measureMaxWidth, final int measureMaxHeight) {
        paint.setTypeface(typefaceConfiguration.getTypeface());
        ViewUtil.setJapaneseLocale(paint);
        setBackgroundColor(transparent ? Constants.TRANSPARENT : backgroundColor);

        if (sizeSp >= 0) {
            textHeight = sp2px(sizeSp);
            viewHeight = textHeight;
            if (image == null) {
                paint.setTextSize(textHeight);
                textWidth = getWidth(characters);
            }
            else {
                //noinspection SuspiciousNameCombination
                textWidth = textHeight;
            }
            viewWidth = textWidth;
            fontPaddingTop = 0;
            fontPaddingLeft = 0;
            return;
        }

        final int finalMaxWidth = Math.min(maxWidth, measureMaxWidth);
        final int finalMaxHeight = Math.min(maxHeight, measureMaxHeight);

        if (!sizeForQuiz) {
            if (image == null) {
                binSearch(absoluteMinTextHeight, displayHeight, finalMaxWidth, finalMaxHeight, 0, 0);
            }
            else {
                final int size = Math.max(Math.min(finalMaxWidth, finalMaxHeight), absoluteMinTextHeight);
                textWidth = size;
                textHeight = size;
                viewWidth = textWidth;
                viewHeight = textHeight;
                fontPaddingTop = 0;
                fontPaddingLeft = 0;
            }
            return;
        }

        if (image == null) {
            binSearch(absoluteMinTextHeight, displayHeight, finalMaxWidth, finalMaxHeight, topMargin, bottomMargin);
        }
        else {
            final int wideSize = Math.max(Math.min(finalMaxWidth, finalMaxHeight) - topMargin - bottomMargin, absoluteMinTextHeight);
            textWidth = wideSize;
            textHeight = wideSize;
            viewWidth = textWidth;
            viewHeight = textHeight + topMargin + bottomMargin;
            fontPaddingTop = topMargin;
            fontPaddingLeft = 0;
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        try {
            super.onDraw(canvas);

            if (textWidth <= 0 || textHeight <= 0) {
                return;
            }

            if (image == null) {
                paint.setTextSize(textHeight);
                paint.setColor(textColor);
                paint.setTextAlign(Paint.Align.LEFT);
                final int x = getPaddingLeft() + fontPaddingLeft;
                final int y = getPaddingTop() + fontPaddingTop + textHeight / 2 - (int) (paint.ascent() + paint.descent()) / 2;
                canvas.drawText(characters, x, y, paint);
            }
            else {
                image.setBounds(
                        getPaddingLeft() + fontPaddingLeft,
                        getPaddingTop() + fontPaddingTop,
                        getPaddingLeft() + fontPaddingLeft + textWidth,
                        getPaddingTop() + fontPaddingTop + textHeight);
                if (textColorFilter != null) {
                    image.setColorFilter(textColorFilter);
                }
                image.draw(canvas);
            }
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        try {
            final int measureMaxWidth = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                    ? getContext().getResources().getDisplayMetrics().widthPixels
                    : MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            final int measureMaxHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                    ? getContext().getResources().getDisplayMetrics().heightPixels
                    : MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
            prepare(measureMaxWidth, measureMaxHeight);
            setMeasuredDimension(
                    viewWidth + getPaddingLeft() + getPaddingRight(),
                    viewHeight + getPaddingTop() + getPaddingBottom());
        } catch (final Exception e) {
            LOGGER.uerr(e);
            setMeasuredDimension(0, 0);
        }
    }

    /**
     * Set the subject for this button.
     *
     * @param subject the subject
     */
    public void setSubject(final Subject subject) {
        try {
            characters = ObjectSupport.orElse(subject.getCharacters(), "");
            image = subject.needsTitleImage() ? getContext().getResources().getDrawable(subject.getTitleImageId()) : null;
            textColor = subject.getTextColor();
            if (image != null) {
                textColorFilter = new SimpleColorFilter(textColor);
            }
            backgroundColor = subject.getButtonBackgroundColor();
            setTag(R.id.subjectId, subject.getId());
            invalidate();
            requestLayout();
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * Fixed size (height) in SP. If set, maxWidth and maxHeight are set to -1.
     * Either this or maxWidth and maxHeight are used.
     *
     * @param sizeSp the size in SP
     */
    public void setSizeSp(final int sizeSp) {
        try {
            this.sizeSp = sizeSp;
            maxWidth = -1;
            maxHeight = -1;
            invalidate();
            requestLayout();
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * The maximum width and height for size calculation. If set, sizeSp is set to -1.
     * Either this or sizeSp are used.
     *
     * @param maxWidthPx the maximum width in pixels
     * @param maxHeightPx the maximum width in pixels
     */
    public void setMaxSize(final int maxWidthPx, final int maxHeightPx) {
        try {
            maxWidth = maxWidthPx;
            maxHeight = maxHeightPx;
            sizeSp = -1;
            invalidate();
            requestLayout();
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * True if this button should be transparent.
     *
     * @param transparent true if it should
     */
    public void setTransparent(final boolean transparent) {
        try {
            this.transparent = transparent;
            invalidate();
            requestLayout();
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * The typeface configuration for the text if this is not a radical with an image.
     *
     * @param typefaceConfiguration the typeface configuration
     */
    public void setTypefaceConfiguration(final TypefaceConfiguration typefaceConfiguration) {
        try {
            this.typefaceConfiguration = typefaceConfiguration;
            invalidate();
            requestLayout();
        } catch (final Exception e) {
            LOGGER.uerr(e);
        }
    }

    /**
     * Get the calculated width for the view, excluding padding.
     *
     * @return the width in pixels
     */
    public int getCalculatedWidth() {
        try {
            prepare(maxWidth, maxHeight);
            return viewWidth;
        } catch (final Exception e) {
            LOGGER.uerr(e);
            return 0;
        }
    }

    private int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int sp2px(final int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
