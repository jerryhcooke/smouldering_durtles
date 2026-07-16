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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.util.ThemeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static java.util.Objects.requireNonNull;

/**
 * A diagram of the stroke order of a character, possibly animated stroke by stroke to show the order.
 *
 * <p>
 *     This is basically a modified Java port of https://github.com/badoualy/kanji-strokeview
 * </p>
 */
public final class StrokeDiagramView extends View implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener, View.OnClickListener {
    private static final Pattern svgInstructionPattern = Pattern.compile("([a-zA-Z])([^a-zA-Z]+)");
    private static final Pattern svgCoordinatesPattern = Pattern.compile("-?\\d*\\.?\\d*");
    private static final RectF inputRect = new RectF(0, 0, 109, 109);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    private final float[] pos = new float[2];

    private List<String> strokeData = Collections.emptyList();
    private List<StrokeData> strokes = Collections.emptyList();
    private List<PathMeasure> strokeMeasures = Collections.emptyList();
    private List<Float> strokeLengths = Collections.emptyList();
    private int numStrokes = 0;
    private boolean animated = true;
    private int size = 0;

    private int finishedStrokes = 0;
    private int animatingStroke = 0;
    private boolean lingering = false;

    private boolean dirty = true;

    /**
     * The constructor.
     *
     * @param context Android context
     */
    public StrokeDiagramView(final Context context) {
        super(context);
        safe(this::init);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs configuration attributes
     */
    public StrokeDiagramView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
        safe(this::init);
    }

    /**
     * The constructor.
     *
     * @param context Android context
     * @param attrs configuration attributes
     * @param defStyleAttr default style attribute
     */
    public StrokeDiagramView(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        safe(this::init);
    }

    private static List<Float> splitCoordinates(final CharSequence str) {
        final Matcher matcher = svgCoordinatesPattern.matcher(str);
        final List<Float> result = new ArrayList<>();

        while (matcher.find()) {
            final @Nullable String s = matcher.group();
            if (!isEmpty(s)) {
                result.add(Float.parseFloat(s));
            }
        }

        return result;
    }

    private static void buildStrokeDataHelper(final StrokeData stroke, final CharSequence pathData) {
        final Matcher matcher = svgInstructionPattern.matcher(pathData);

        float lastX = 0.0f;
        float lastY = 0.0f;
        float lastX1 = 0.0f;
        float lastY1 = 0.0f;
        float subPathStartX = 0.0f;
        float subPathStartY = 0.0f;
        boolean curve = false;

        while (matcher.find()) {
            final char command  = requireNonNull(matcher.group(1)).charAt(0);
            final List<Float> coordinates = splitCoordinates(requireNonNull(matcher.group(2)));

            switch (command) {
                case 'T': {
                    stroke.strokeNumber = coordinates.get(0).intValue();
                    stroke.labelX = coordinates.get(1);
                    stroke.labelY = coordinates.get(2);
                    break;
                }
                case 'm':
                case 'M': {
                    final float x = coordinates.get(0);
                    final float y = coordinates.get(1);
                    if (command == 'M') {
                        subPathStartX = x;
                        subPathStartY = y;
                        stroke.path.moveTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    else {
                        subPathStartX += x;
                        subPathStartY += y;
                        stroke.path.rMoveTo(x, y);
                        lastX += x;
                        lastY += y;
                    }
                    break;
                }
                case 'l':
                case 'L': {
                    final float x = coordinates.get(0);
                    final float y = coordinates.get(1);
                    if (command == 'L') {
                        stroke.path.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    else {
                        stroke.path.rLineTo(x, y);
                        lastX += x;
                        lastY += y;
                    }
                    break;
                }
                case 'v':
                case 'V': {
                    for (final float y: coordinates) {
                        if (command == 'V') {
                            stroke.path.lineTo(lastX, y);
                            lastY = y;
                        }
                        else {
                            stroke.path.rLineTo(0.0f, y);
                            lastY += y;
                        }
                    }
                    break;
                }
                case 'h':
                case 'H': {
                    for (final float x: coordinates) {
                        if (command == 'H') {
                            stroke.path.lineTo(x, lastY);
                            lastX = x;
                        }
                        else {
                            stroke.path.rLineTo(x, 0.0f);
                            lastX += x;
                        }
                    }
                    break;
                }
                case 'c':
                case 'C': {
                    curve = true;
                    int index = 0;
                    while (index + 6 <= coordinates.size()) {
                        float x1 = coordinates.get(index);
                        float y1 = coordinates.get(index+1);
                        float x2 = coordinates.get(index+2);
                        float y2 = coordinates.get(index+3);
                        float x = coordinates.get(index+4);
                        float y = coordinates.get(index+5);
                        if (command == 'c') {
                            x1 += lastX;
                            x2 += lastX;
                            x += lastX;
                            y1 += lastY;
                            y2 += lastY;
                            y += lastY;
                        }
                        stroke.path.cubicTo(x1, y1, x2, y2, x, y);
                        lastX1 = x2;
                        lastY1 = y2;
                        lastX = x;
                        lastY = y;
                        index += 6;
                    }
                    break;
                }
                case 's':
                case 'S': {
                    curve = true;
                    int index = 0;
                    while (index + 4 <= coordinates.size()) {
                        float x2 = coordinates.get(index);
                        float y2 = coordinates.get(index+1);
                        float x = coordinates.get(index+2);
                        float y = coordinates.get(index+3);
                        if (command == 's') {
                            x2 += lastX;
                            x += lastX;
                            y2 += lastY;
                            y += lastY;
                        }
                        final float x1 = 2 * lastX - lastX1;
                        final float y1 = 2 * lastY - lastY1;
                        stroke.path.cubicTo(x1, y1, x2, y2, x, y);
                        lastX1 = x2;
                        lastY1 = y2;
                        lastX = x;
                        lastY = y;
                        index += 4;
                    }
                    break;
                }
                case 'z':
                case 'Z': {
                    stroke.path.close();
                    stroke.path.moveTo(subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    curve = true;
                    break;
                }
            }

            if (!curve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
        }
    }

    private static StrokeData buildStrokeData(final CharSequence strokeData) {
        final StrokeData stroke = new StrokeData();
        safe(() -> buildStrokeDataHelper(stroke, strokeData));
        return stroke;
    }

    private void init() {
        setOnClickListener(this);
        animator.addListener(this);
        animator.addUpdateListener(this);
        size = dp2px(250);
    }

    /**
     * Set the SVG-style path data for the strokes to show in this diagram.
     *
     * @param strokeData the stroke data
     */
    public void setStrokeData(final Collection<String> strokeData) {
        if (animator.isStarted()) {
            animator.cancel();
        }
        lingering = false;
        finishedStrokes = 0;
        animatingStroke = 0;
        this.strokeData = new ArrayList<>(strokeData);
        dirty = true;
        invalidate();
    }

    /**
     * Should the stroke diagram be animated?.
     *
     * @param animated true if it should
     */
    public void setAnimated(final boolean animated) {
        if (animator.isStarted()) {
            animator.cancel();
        }
        this.animated = animated;
        lingering = false;
        finishedStrokes = 0;
        invalidate();
    }

    /**
     * The size of the diagram in pixels (square).
     *
     * @param size the size
     */
    public void setSize(final int size) {
        this.size = size;
        dirty = true;
        invalidate();
        requestLayout();
    }

    @SuppressLint("NewApi")
    private void prepare() {
        if (!dirty) {
            return;
        }
        dirty = false;

        final Matrix matrix = new Matrix();
        matrix.setRectToRect(inputRect, new RectF(0, 0, getWidth(), getHeight()), Matrix.ScaleToFit.FILL);

        strokes = strokeData.stream()
                .map(StrokeDiagramView::buildStrokeData)
                .collect(Collectors.toList());
        strokeLengths = strokes.stream()
                .map(stroke -> new PathMeasure(stroke.path, false).getLength())
                .collect(Collectors.toList());
        strokes.forEach(stroke -> {
            stroke.path.transform(matrix);
            pos[0] = stroke.labelX;
            pos[1] = stroke.labelY;
            matrix.mapPoints(pos);
            stroke.labelX = pos[0];
            stroke.labelY = pos[1];
        });
        strokeMeasures = strokes.stream()
                .map(stroke -> new PathMeasure(stroke.path, false))
                .collect(Collectors.toList());
        numStrokes = strokes.size();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        if (w != oldw || h != oldh) {
            dirty = true;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        setMeasuredDimension(size, size);
    }

    @SuppressLint("NewApi")
    private void onDrawHelper(final Canvas canvas) {
        prepare();

        if (animated) {
            if (!lingering && numStrokes > 0 && finishedStrokes >= numStrokes) {
                lingering = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (lingering) {
                        finishedStrokes = 0;
                        lingering = false;
                        invalidate();
                    }
                }, 1500);
            }
        }
        else {
            finishedStrokes = numStrokes;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size/50.0f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextSize(dp2px(10));

        paint.setColor(ThemeUtil.getColor(R.attr.strokeDiagramGhostColor));
        textPaint.setColor(paint.getColor());
        strokes.stream().skip(finishedStrokes).forEach(stroke -> canvas.drawPath(stroke.path, paint));
        paint.setColor(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary));
        textPaint.setColor(paint.getColor());
        strokes.stream().limit(finishedStrokes).forEach(stroke -> canvas.drawPath(stroke.path, paint));

        textPaint.setColor(ThemeUtil.getColor(R.attr.strokeDiagramGhostColor));
        strokes.stream().skip(finishedStrokes+1)
                .forEach(stroke -> canvas.drawText(Integer.toString(stroke.strokeNumber), stroke.labelX, stroke.labelY, textPaint));
        textPaint.setColor(ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimary));
        strokes.stream().limit(finishedStrokes+1)
                .forEach(stroke -> canvas.drawText(Integer.toString(stroke.strokeNumber), stroke.labelX, stroke.labelY, textPaint));

        if (animated) {
            if (animator.isStarted()) {
                if (animatingStroke < numStrokes) {
                    final float length = strokeMeasures.get(animatingStroke).getLength();
                    final DashPathEffect effect = new DashPathEffect(new float[] {length, length}, length * (1 - animator.getAnimatedFraction()));
                    strokeMeasures.get(animatingStroke).getPosTan(length * animator.getAnimatedFraction(), pos, null);
                    paint.setPathEffect(effect);
                    canvas.drawPath(strokes.get(animatingStroke).path, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(pos[0], pos[1], size/50.0f, paint);
                    paint.setPathEffect(null);
                }
            }
            else if (finishedStrokes < numStrokes) {
                animatingStroke = finishedStrokes;
                animator.setStartDelay(200);
                animator.setDuration((long) (strokeLengths.get(animatingStroke) * 5));
                animator.start();
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        safe(() -> onDrawHelper(canvas));
    }

    @Override
    public void onClick(final View v) {
        setAnimated(!animated);
    }

    @Override
    public void onAnimationStart(final Animator animation) {
        if (animated) {
            invalidate();
        }
    }

    @Override
    public void onAnimationUpdate(final ValueAnimator animation) {
        if (animated) {
            invalidate();
        }
    }

    @Override
    public void onAnimationEnd(final Animator animation) {
        if (animated) {
            finishedStrokes = animatingStroke + 1;
            invalidate();
        }
    }

    @Override
    public void onAnimationCancel(final Animator animation) {
        //
    }

    @Override
    public void onAnimationRepeat(final Animator animation) {
        //
    }

    private int dp2px(@SuppressWarnings("SameParameterValue") final int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static final class StrokeData {
        private final Path path = new Path();
        private float labelX = 0;
        private float labelY = 0;
        private int strokeNumber = 0;
    }
}
