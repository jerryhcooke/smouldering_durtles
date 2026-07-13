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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LifecycleOwner;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.enums.SubjectType;
import com.smouldering_durtles.wk.livedata.LiveTimeLine;
import com.smouldering_durtles.wk.livedata.LiveVacationMode;
import com.smouldering_durtles.wk.model.SrsSystem;
import com.smouldering_durtles.wk.model.TimeLine;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.FONT_SIZE_NORMAL;
import static com.smouldering_durtles.wk.Constants.HOUR;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.TextUtil.formatShortTimeForDisplay;

/**
 * Custom bar chart for the timeline.
 */
public final class TimeLineBarChart extends View implements GestureDetector.OnGestureListener {
    private final List<BarEntry> entries = new ArrayList<>();
    private int[] segmentColors = new int[0];
    private String[] legendLabels = new String[0];
    private int maxBarCount = 0;
    private int totalCount = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect rect = new Rect();
    private int colorPrimary = 0;
    private int colorPrimaryTonedDown = 0;
    private int colorWaterfall = 0;
    private float density = 1.0f;
    private float originX = 0;
    private float originY = 0;
    private float axisSizeX = 0;
    private float axisSizeY = 0;
    private float pixelsPerUnit = 0;
    private float pixelsPerUnitCumulative = 0;
    private float pixelsPerUnitGrid = 0;
    private int axisIntervalY = 0;
    private float barAdvance = 0;
    private int numShownBars = 24;
    private float scrollOffset = 0;
    private @Nullable GestureDetectorCompat gestureDetector = null;
    private @Nullable Scroller scroller = null;
    private float prevVerticalScrollRawY = 0;
    private long firstSlot = 0;
    private @Nullable Drawable arrowIcon = null;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public TimeLineBarChart(final Context context) {
        this(context, null, 0);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     */
    public TimeLineBarChart(final Context context, final @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs attribute set
     * @param defStyle default style
     */
    public TimeLineBarChart(final Context context, final @Nullable AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        safe(() -> {
            colorPrimary = ThemeUtil.getColor(R.attr.colorPrimary);
            colorPrimaryTonedDown = ThemeUtil.getColor(R.attr.colorPrimaryTonedDown);
            colorWaterfall = ThemeUtil.getColor(R.attr.colorWaterfallLine);
            density = context.getResources().getDisplayMetrics().density;
            arrowIcon = ContextCompat.getDrawable(getContext(), ActiveTheme.getCurrentTheme().getLevelUpArrowDrawableId());
            init();
        });
    }

    /**
     * Initialize the view.
     */
    private void init() {
        safe(() -> {
            setBackgroundResource(R.drawable.main_activity_view_background);
            setPadding(0, (int) (4 * density), 0, (int) (4 * density));
            setNestedScrollingEnabled(true);
        });
    }

    /**
     * Set the lifecycle owner for this view, to hook LiveData observers to.
     *
     * @param lifecycleOwner the lifecycle owner
     */
    public void setLifecycleOwner(final LifecycleOwner lifecycleOwner) {
        safe(() -> LiveTimeLine.getInstance().observe(lifecycleOwner, t -> safe(() -> update(t))));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        safe(() -> {
            setMeasuredDimension(0, 0);

            final int w;
            final int h;
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                w = MeasureSpec.getSize(widthMeasureSpec);
                h = MeasureSpec.getSize(heightMeasureSpec);
            }
            else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
                w = Math.min(
                        MeasureSpec.getSize(widthMeasureSpec),
                        getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight()
                );
                h = Math.min(
                        MeasureSpec.getSize(heightMeasureSpec),
                        getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom()
                );
            }
            else {
                w = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
                h = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();
            }

            setMeasuredDimension(w, h);
        });
    }

    /**
     * Round up an interval to the nearest 'pretty' number: an even power of ten,
     * times 1, 2, 3, 4 or 5.
     *
     * @param value the value to round up
     * @return the rounded value
     */
    private static int roundUpInterval(final int value) {
        if (value <= 0) {
            return 1;
        }
        if (value < 5) {
            return value;
        }
        if (value < 10) {
            return 5;
        }
        return roundUpInterval((value + 9) / 10) * 10;
    }

    /**
     * Get the time label for a bar, identified by its 0-based index.
     *
     * @param index the timeslot
     * @return the formatted label
     */
    @SuppressLint("NewApi")
    private String getBarLabel(final int index) {
        return formatShortTimeForDisplay(firstSlot + index * HOUR, index >= 24);
    }

    /**
     * Draw the background grid for the chart, including axis labels.
     *
     * @param canvas the canvas to draw on
     */
    private void drawGrid(final Canvas canvas) {
        paint.setColor(colorPrimary);
        paint.setStrokeWidth(0);
        canvas.drawLine(originX, originY, originX, originY - axisSizeY, paint);

        paint.setTextSize(density * 10);
        paint.setTextAlign(Paint.Align.RIGHT);

        if (axisIntervalY == 0) {
            canvas.drawLine(originX, originY, originX + axisSizeX, originY, paint);
        }
        else {
            int level = 0;
            while (true) {
                final float offset = level * pixelsPerUnitGrid;
                if (offset >= axisSizeY) {
                    break;
                }
                canvas.drawLine(originX, originY - offset, originX + axisSizeX, originY - offset, paint);
                if (level > 0) {
                    canvas.drawText(Integer.toString(level), originX - density * 4, originY - offset - (paint.ascent() + paint.descent()) / 2, paint);
                }
                level += axisIntervalY;
            }
        }
    }

    @SuppressLint("NewApi")
    private List<BarEntry> getSortedEntries() {
        final List<BarEntry> sortedEntries = new ArrayList<>(entries);
        Collections.sort(sortedEntries,
                Comparator.<BarEntry>comparingInt(entry -> entry.barCount)
                        .thenComparingInt(entry -> entry.index));
        return sortedEntries;
    }

    /**
     * Draw the actual bars for the chart, including labels, level-up markers, and the waterfall line.
     *
     * @param canvas the canvas to draw on
     */
    private void drawBars(final Canvas canvas) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0);
        paint.setTextSize(density * 10);
        paint.setTextAlign(Paint.Align.CENTER);

        // The actual bars
        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);
            final float left = Math.max(originX, originX + barAdvance * i + barAdvance * 0.075f - scrollOffset);
            final float right = Math.min(originX + axisSizeX, originX + barAdvance * i + barAdvance * 0.925f - scrollOffset);
            if (right < originX || left > originX + axisSizeX || left > right) {
                continue;
            }
            float bottom = originY;
            for (int j=0; j<entry.values.length; j++) {
                paint.setColor(segmentColors[j]);
                final float top = bottom - entry.values[j] * pixelsPerUnit;
                canvas.drawRect(left, top, right, bottom, paint);
                bottom = top;
            }
        }

        paint.setColor(colorPrimary);

        final List<RectF> labels = new ArrayList<>();
        final List<BarEntry> sortedEntries = getSortedEntries();

        // The count above each bar
        for (final BarEntry entry: sortedEntries) {
            final int i = entry.index;

            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            final float y = originY - entry.barCount * pixelsPerUnit - density * 1 - paint.descent();
            if (entry.barCount == 0 || x < originX || x > originX + axisSizeX) {
                continue;
            }

            final String text = Integer.toString(entry.barCount);
            final float textWidth = paint.measureText(text);
            final RectF rectf = new RectF(x - textWidth/2 - density, y + paint.ascent() - density,
                    x + textWidth/2 + density, y + paint.descent() + density);

            while (!labels.isEmpty() && labels.get(0).bottom <= rectf.top) {
                labels.remove(0);
            }
            boolean ok = true;
            for (final RectF r: labels) {
                if (RectF.intersects(r, rectf)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }

            canvas.drawText(text, x, y, paint);
            labels.add(rectf);
        }

        paint.setTextSize(density * 9);
        paint.setColor(colorPrimaryTonedDown);

        // The count of level-up items, including the arrow icons
        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);

            final int y = getPaddingTop();
            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (entry.numLevelUpItems == 0 || arrowIcon == null || x < originX || x > originX + axisSizeX) {
                continue;
            }

            arrowIcon.setBounds((int) (x - density * 6), y, (int) (x + density * 6), (int) (y + density * 9));
            arrowIcon.draw(canvas);
        }

        float lastX = -1;

        for (int i=0; i<entries.size(); i++) {
            final BarEntry entry = entries.get(i);

            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (entry.numLevelUpItems == 0 || arrowIcon == null || x < originX || x > originX + axisSizeX) {
                continue;
            }

            final String text = Integer.toString(entry.numLevelUpItems);
            final float textWidth = paint.measureText(text);
            if (x - textWidth/2 - density < lastX) {
                continue;
            }
            lastX = x + textWidth/2 + density;

            canvas.drawText(text, x, getPaddingTop() - paint.ascent(), paint);
        }

        paint.setColor(colorPrimary);
        paint.setTextSize(density * 10);

        // The time labels on the X axis
        for (int i=0; i<entries.size(); i+=numShownBars/6) {
            final String label = getBarLabel(i);
            final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
            if (x < originX || x > originX + axisSizeX) {
                continue;
            }
            final float y = originY + density * 4 - paint.ascent();
            canvas.drawText(label, x, y, paint);
        }

        // The waterfall line
        if (GlobalSettings.Dashboard.getShowWaterfallLine()) {
            paint.setColor(colorWaterfall);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(density);

            float prevX = -1;
            float prevY = -1;

            for (int i=0; i<entries.size(); i++) {
                final BarEntry entry = entries.get(i);
                final float x = originX + barAdvance * i + barAdvance / 2 - scrollOffset;
                final float y = originY - entry.cumulativeCount * pixelsPerUnitCumulative;
                if (prevX >= originX && x <= originX + axisSizeX) {
                    canvas.drawLine(prevX, prevY, x, y, paint);
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    /**
     * Draw the legend at the bottom of the chart.
     *
     * @param canvas the canvas to draw on
     */
    private void drawLegend(final Canvas canvas) {
        final float boxSize = density * 8;
        final float gapSize = density * 5;

        paint.setTextSize(density * 10);
        paint.setTextAlign(Paint.Align.LEFT);

        float x = density * 8;
        final float boxY = originY + density * 18;
        final float textY = originY + density * 26;

        for (int i=0; i<segmentColors.length; i++) {
            paint.setColor(segmentColors[i]);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(0);
            canvas.drawRect(x, boxY, x + boxSize, boxY + boxSize, paint);
            x += boxSize + gapSize;

            final String label = legendLabels[i];
            paint.setColor(colorPrimary);
            canvas.drawText(label, x, textY, paint);
            paint.getTextBounds(label, 0, label.length(), rect);
            x += rect.width() + gapSize;
        }
    }

    private void onDrawImpl(final Canvas canvas) {
        final float baseWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        final float baseHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        paint.setTypeface(Typeface.DEFAULT);

        if (totalCount == 0) {
            paint.setColor(colorPrimary);
            paint.setTextSize(density * FONT_SIZE_NORMAL);
            final String s = "No upcoming reviews";
            paint.getTextBounds(s, 0, s.length(), rect);
            final float x = getPaddingLeft() + (baseWidth - rect.width()) / 2;
            final float y = getPaddingTop() + baseHeight / 2 - (paint.ascent() + paint.descent()) / 2;
            canvas.drawText(s, x, y, paint);
            return;
        }

        paint.setTextSize(density * 10);

        originY = getPaddingTop() + baseHeight - density * 30;
        axisSizeY = baseHeight - density * 45;
        if (axisSizeY <= 0) {
            return;
        }
        pixelsPerUnit = (axisSizeY / 1.1f) / maxBarCount;
        pixelsPerUnitCumulative = (axisSizeY / 1.1f) / totalCount;
        final float lineSpacing = Math.max(Math.abs(paint.getFontSpacing()), density * 10) * 1.8f;
        final String topLabel;
        switch (GlobalSettings.Dashboard.getTimeLineChartGridStyle()) {
            case FOR_WATERFALL:
                pixelsPerUnitGrid = pixelsPerUnitCumulative;
                axisIntervalY = roundUpInterval((int) Math.ceil(lineSpacing / pixelsPerUnitGrid));
                topLabel = Integer.toString(((totalCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY);
                break;
            case OFF:
                pixelsPerUnitGrid = pixelsPerUnit;
                axisIntervalY = 0;
                topLabel = "";
                break;
            case FOR_BARS:
            default:
                pixelsPerUnitGrid = pixelsPerUnit;
                axisIntervalY = roundUpInterval((int) Math.ceil(lineSpacing / pixelsPerUnitGrid));
                topLabel = Integer.toString(((maxBarCount + axisIntervalY - 1) / axisIntervalY) * axisIntervalY);
                break;
        }

        paint.getTextBounds(topLabel, 0, topLabel.length(), rect);
        originX = getPaddingLeft() + rect.width() + density * 12;
        axisSizeX = baseWidth - rect.width() - density * 20;
        if (axisSizeX <= 0) {
            return;
        }
        barAdvance = axisSizeX / numShownBars;

        drawGrid(canvas);
        drawBars(canvas);
        drawLegend(canvas);

        if (scroller != null && !scroller.isFinished() && scroller.computeScrollOffset()) {
            scrollOffset = Math.min(entries.size() * barAdvance - axisSizeX, Math.max(0, scroller.getCurrX()));
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        safe(() -> {
            super.onDraw(canvas);
            onDrawImpl(canvas);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final boolean result = safe(false, () -> {
            if (numShownBars < entries.size()) {
                if (gestureDetector == null) {
                    gestureDetector = new GestureDetectorCompat(getContext(), this);
                }
                gestureDetector.onTouchEvent(event);
                return true;
            }
            return false;
        });
        return result || super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        safe(() -> {
            if (scroller != null) {
                scroller.forceFinished(true);
            }
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
            prevVerticalScrollRawY = e.getRawY();
        });
        return true;
    }

    @Override
    public void onShowPress(final MotionEvent e) {
        //
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        safe(() -> {
            scrollOffset = Math.min(entries.size() * barAdvance - axisSizeX, Math.max(0, scrollOffset + distanceX));
            final float distance = prevVerticalScrollRawY - e2.getRawY();
            dispatchNestedScroll(0, 0, 0, (int) distance, null);
            prevVerticalScrollRawY = e2.getRawY();
            invalidate();
        });
        return true;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        //
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        safe(() -> {
            if (scroller == null) {
                scroller = new Scroller(getContext());
            }
            scroller.forceFinished(true);
            scroller.fling((int) scrollOffset, 0, (int) -velocityX, 0,
                    0, (int) (entries.size() * barAdvance - axisSizeX), 0, 0);
            dispatchNestedFling(0, -velocityY, false);
            invalidate();
        });
        return true;
    }

    /**
     * Build the dataset based on the SRS stage style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetSrsStage(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                final SrsSystem.Stage stage = review.getSrsStage();
                int bucket = stage.getTimeLineBarChartBucket();
                if (bucket == 4) {
                    bucket = 3;
                }
                values[bucket]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getShallowStageBucketColors4();

        legendLabels = new String[] {"Apprentice", "Guru", "Master", "Enlightened"};
    }

    /**
     * Build the dataset based on the Next SRS stage style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetNextSrsStage(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                final SrsSystem.Stage stage = review.getSrsStage().getNewStage(0);
                values[stage.getTimeLineBarChartBucket()]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getShallowStageBucketColors5();

        legendLabels = new String[] {"Apprentice", "Guru", "Master", "Enlightened", "Burned"};
    }

    /**
     * Build the dataset based on the item type style.
     *
     * @param timeLine the timeline
     */
    private void buildDataSetItemType(final TimeLine timeLine) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < timeLine.getSize(); i++) {
            final List<Subject> reviews = timeLine.getTimeLine().get(i);
            final int[] values = {0, 0, 0};
            int count = 0;
            for (final Subject review: reviews) {
                int bucketIndex = review.getType().getTimeLineBarChartBucket();

                // Check if the item type is WANIKANI_KANA_VOCAB and update the bucket index accordingly
                if (review.getType() == SubjectType.WANIKANI_KANA_VOCAB) {
                    bucketIndex = SubjectType.WANIKANI_VOCAB.getTimeLineBarChartBucket();
                }

                values[bucketIndex]++;
                count++;
            }
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, timeLine.getNumRequiredForLevelUp().get(i), count, totalCount));
        }

        segmentColors = ActiveTheme.getSubjectTypeBucketColors();

        legendLabels = new String[] {"Radical", "Kanji", "Vocabulary"};
    }

    /**
     * Build a dummy data test set for testing of the chart code.
     *
     * @param size the size in hours
     */
    @SuppressWarnings("unused")
    private void buildDataSetTest(@SuppressWarnings("SameParameterValue") final int size) {
        entries.clear();
        totalCount = 0;
        maxBarCount = 0;
        for (int i = 0; i < size; i++) {
            final int count = i + 10;
            final int[] values = {count};
            if (count > maxBarCount) {
                maxBarCount = count;
            }
            totalCount += count;
            entries.add(new BarEntry(i, values, 3, count, totalCount));
        }

        segmentColors = new int[] {0xFF2ECC71};

        legendLabels = new String[] {"Test"};
    }

    /**
     * Update the chart if the LiveData delivers a new timeline.
     *
     * @param timeLine the new timeline
     */
    private void update(final TimeLine timeLine) {
        if (LiveVacationMode.getInstance().get() || !GlobalSettings.Dashboard.getShowTimeLine()
                || GlobalSettings.getFirstTimeSetup() == 0) {
            setVisibility(View.GONE);
            return;
        }

        // buildDataSetTest(336);
        switch (GlobalSettings.Dashboard.getTimeLineChartStyle()) {
            case ITEM_TYPE:
                buildDataSetItemType(timeLine);
                break;
            case NEXT_SRS_STAGE:
                buildDataSetNextSrsStage(timeLine);
                break;
            case SRS_STAGE:
            default:
                buildDataSetSrsStage(timeLine);
                break;
        }

        firstSlot = timeLine.getFirstSlot();
        numShownBars = GlobalSettings.Dashboard.getTimeLineChartSizeShown();

        setVisibility(View.VISIBLE);
        invalidate();
    }

    /**
     * A data set entry for one timeslot in the timeline.
     */
    private static final class BarEntry {
        /**
         * The index of the bar in the overall timeline.
         */
        private final int index;

        /**
         * Values for the segments that make up a bar.
         */
        private final int[] values;

        /**
         * The number of items on the level-up progression path in this timeslot.
         */
        private final int numLevelUpItems;

        /**
         * The total number of items in this bar (sum of values).
         */
        private final int barCount;

        /**
         * The cumulative total count of items in all timeslots up to and including this one.
         */
        private final int cumulativeCount;

        /**
         * The constructor.
         *
         * @param index The index of the bar in the overall timeline.
         * @param values Values for the segments that make up a bar.
         * @param numLevelUpItems The number of items on the level-up progression path in this timeslot.
         * @param barCount The total number of items in this bar (sum of values).
         * @param cumulativeCount The cumulative total count of items in all timeslots up to and including this one.
         */
        BarEntry(final int index, final int[] values, final int numLevelUpItems, final int barCount, final int cumulativeCount) {
            this.index = index;
            this.values = values;
            this.numLevelUpItems = numLevelUpItems;
            this.barCount = barCount;
            this.cumulativeCount = cumulativeCount;
        }
    }
}
