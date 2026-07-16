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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.model.SrsBreakDown;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view for a bar in the post-60 progress bar chart.
 */
public final class Post60ProgressBarView extends View {
    private int[] values = new int[0];
    private int[] colors = new int[0];
    private int[] textColors = new int[0];
    private boolean[] textShadow = new boolean[0];
    private boolean dirty = true;
    private int height = 0;
    private int total = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect rect = new Rect();

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public Post60ProgressBarView(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public Post60ProgressBarView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyleAttr default style attribute
     */
    public Post60ProgressBarView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Precompute the details of this instance, to speed up later drawing and measuring.
     */
    private void prepare() {
        if (!dirty) {
            return;
        }
        dirty = false;

        height = dp2px(24);

        total = 0;
        for (final int value: values) {
            total += value;
        }

        textColors = new int[colors.length];
        textShadow = new boolean[colors.length];
        for (int i=0; i<colors.length; i++) {
            final boolean light = ThemeUtil.isLightColor(colors[i]);
            textColors[i] = light
                    ? ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimaryDark)
                    : ThemeUtil.getColor(R.attr.colorPrimaryLight);
            textShadow[i] = !light;
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        safe(() -> {
            super.onDraw(canvas);
            prepare();

            if (total == 0) {
                return;
            }

            final int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int done = 0;

            for (int i=0; i<values.length; i++) {
                final int value = values[i];
                if (value == 0) {
                    continue;
                }

                final int start = getPaddingLeft() + (done * width) / total;
                final int end = getPaddingLeft() + ((done + value) * width) / total;

                paint.setColor(colors[i]);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(0);
                canvas.drawRect(start, getPaddingTop(), end, getPaddingTop() + height, paint);

                if (total > 0) {
                    paint.setColor(textColors[i]);
                    if (textShadow[i]) {
                        paint.setShadowLayer(3, 1, 1, Color.BLACK);
                    }
                    else {
                        paint.clearShadowLayer();
                    }
                    paint.setTextSize(dp2px(10));
                    paint.setTextAlign(Paint.Align.CENTER);
                    final String longText = String.format(Locale.ROOT, "%.1f%%", (values[i] * 100.0f) / total);
                    paint.getTextBounds(longText, 0, longText.length(), rect);
                    if (rect.width() + dp2px(2) <= (end-start)) {
                        canvas.drawText(longText, (start+end) / 2.0f, getPaddingTop() + height / 2.0f - (paint.ascent() + paint.descent()) / 2, paint);
                    }
                    else {
                        final String shortText = String.format(Locale.ROOT, "%d%%", (values[i] * 100) / total);
                        paint.getTextBounds(shortText, 0, shortText.length(), rect);
                        if (rect.width() + dp2px(2) <= (end-start)) {
                            canvas.drawText(shortText, (start+end) / 2.0f, getPaddingTop() + height / 2.0f - (paint.ascent() + paint.descent()) / 2, paint);
                        }
                    }
                }

                done += value;
            }
        });
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        safe(() -> {
            setMeasuredDimension(0, 0);

            prepare();

            final int w;
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                w = MeasureSpec.getSize(widthMeasureSpec);
            }
            else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
                w = Math.min(
                        MeasureSpec.getSize(widthMeasureSpec),
                        getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight()
                );
            }
            else {
                w = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
            }

            setMeasuredDimension(w, height + getPaddingTop() + getPaddingBottom());
        });
    }

    /**
     * Set the SRS breakdown data.
     *
     * @param breakDown the data
     */
    public void setBreakdown(final SrsBreakDown breakDown) {
        safe(() -> {
            final boolean subsections = GlobalSettings.Dashboard.getShowPost60Subsections();
            final boolean reversed = GlobalSettings.Dashboard.getShowPost60Reverse();

            if (subsections) {
                values = new int[] {
                        breakDown.getPost60DeepCount(0),
                        breakDown.getPost60DeepCount(1),
                        breakDown.getPost60DeepCount(2),
                        breakDown.getPost60DeepCount(3),
                        breakDown.getPost60DeepCount(4),
                        breakDown.getPost60DeepCount(5),
                        breakDown.getPost60DeepCount(6),
                        breakDown.getPost60DeepCount(7),
                        breakDown.getPost60DeepCount(8),
                        breakDown.getPost60DeepCount(9),
                        breakDown.getPost60DeepCount(10)
                };

                colors = ActiveTheme.getStageDeepBucketColors();
            }
            else {
                values = new int[] {
                        breakDown.getPost60ShallowCount(0),
                        breakDown.getPost60ShallowCount(1),
                        breakDown.getPost60ShallowCount(2),
                        breakDown.getPost60ShallowCount(3),
                        breakDown.getPost60ShallowCount(4),
                        breakDown.getPost60ShallowCount(5),
                        breakDown.getPost60ShallowCount(6),
                };

                colors = ActiveTheme.getShallowStageBucketColors7();
            }

            if (reversed) {
                for (int i=0, j=values.length-1; i<j; i++, j--) {
                    final int tmp = values[i];
                    values[i] = values[j];
                    values[j] = tmp;
                }
                colors = colors.clone();
                for (int i=0, j=colors.length-1; i<j; i++, j--) {
                    final int tmp = colors[i];
                    colors[i] = colors[j];
                    colors[j] = tmp;
                }
            }

            dirty = true;
            invalidate();
            requestLayout();
        });
    }

    private int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
