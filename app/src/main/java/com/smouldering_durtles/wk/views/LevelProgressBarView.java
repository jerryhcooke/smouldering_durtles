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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.util.ThemeUtil;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A custom view for a bar in the level progress bar chart.
 */
public final class LevelProgressBarView extends View {
    private int[] values = new int[0];
    private boolean showTarget = false;
    private boolean dirty = true;
    private int height = 0;
    private int[] colors = new int[0];
    private int targetColor = 0;
    private int lineThickness = 0;
    private int total = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public LevelProgressBarView(final Context context) {
        super(context);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public LevelProgressBarView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyleAttr default style attribute
     */
    public LevelProgressBarView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
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
        lineThickness = dp2px(2);

        colors = ActiveTheme.getLevelProgressionBucketColors();

        targetColor = ThemeUtil.getColor(R.attr.colorLevelProgressTarget);

        total = 0;
        for (final int value: values) {
            total += value;
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

            for (int i=0; i<10; i++) {
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

                done += value;
            }

            if (showTarget) {
                final int threshold = (total * 9 + 9) / 10;
                final int start = getPaddingLeft() + (threshold * width) / total - lineThickness / 2;
                final int end = start + lineThickness;

                paint.setColor(targetColor);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(0);
                canvas.drawRect(start, getPaddingTop(), end, getPaddingTop() + height, paint);
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
     * The sizes for the segments in the bar.
     *
     * @param values the array of values, always exactly 10 elements.
     */
    public void setValues(final int[] values) {
        safe(() -> {
            this.values = values.clone();
            dirty = true;
            invalidate();
            requestLayout();
        });
    }

    /**
     * Should a marker bar for the level's target be shown?.
     *
     * @param showTarget true if it should
     */
    public void setShowTarget(final boolean showTarget) {
        safe(() -> {
            this.showTarget = showTarget;
            dirty = true;
            invalidate();
            requestLayout();
        });
    }

    private int dp2px(final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
