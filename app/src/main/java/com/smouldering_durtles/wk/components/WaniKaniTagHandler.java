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

package com.smouldering_durtles.wk.components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.LocaleSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.UpdateAppearance;

import androidx.core.content.ContextCompat;

import com.airbnb.lottie.SimpleColorFilter;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.enums.ActiveTheme;
import com.smouldering_durtles.wk.util.ThemeUtil;

import org.xml.sax.XMLReader;

import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * A tag handler for texts like mnemonics in the subject info. Used to translate a few WK-specific tags like '&lt;kanji&gt;'.
 */
public final class WaniKaniTagHandler implements Html.TagHandler {
    @Override
    public void handleTag(final boolean opening, final String tag, final Editable output, final XMLReader xmlReader) {
        if (!(output instanceof SpannableStringBuilder)) {
            return;
        }
        if (tag.equalsIgnoreCase("ja") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (opening) {
                start(output, new Ja());
            }
            else {
                end(output, Ja.class, new LocaleSpan(new LocaleList(Locale.JAPAN, Locale.ROOT)));
            }
        }
        else if (tag.equalsIgnoreCase("ja") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (opening) {
                start(output, new Ja());
            }
            else {
                end(output, Ja.class, new LocaleSpan(Locale.JAPAN));
            }
        }
        else if (tag.equalsIgnoreCase("kan") || tag.equalsIgnoreCase("kanji")) {
            if (opening) {
                start(output, new Kanji());
            }
            else {
                if (GlobalSettings.SubjectInfo.getHighlightSubjectTags()) {
                    end(output, Kanji.class,
                            new ButtonStyleSpan(ActiveTheme.getSubjectTypeTextColors()[1], ActiveTheme.getSubjectTypeBackgroundColors()[1]));
                }
                else {
                    end(output, Kanji.class, new StyleSpan(Typeface.BOLD));
                }
            }
        }
        else if (tag.equalsIgnoreCase("radical") || tag.equalsIgnoreCase("rd")) {
            if (opening) {
                start(output, new Radical());
            }
            else {
                if (GlobalSettings.SubjectInfo.getHighlightSubjectTags()) {
                    end(output, Radical.class,
                            new ButtonStyleSpan(ActiveTheme.getSubjectTypeTextColors()[0], ActiveTheme.getSubjectTypeBackgroundColors()[0]));
                }
                else {
                    end(output, Radical.class, new StyleSpan(Typeface.BOLD));
                }
            }
        }
        else if (tag.equalsIgnoreCase("vocabulary") || tag.equalsIgnoreCase("voc")) {
            if (opening) {
                start(output, new Vocabulary());
            }
            else {
                if (GlobalSettings.SubjectInfo.getHighlightSubjectTags()) {
                    end(output, Vocabulary.class,
                            new ButtonStyleSpan(ActiveTheme.getSubjectTypeTextColors()[2], ActiveTheme.getSubjectTypeBackgroundColors()[2]));
                }
                else {
                    end(output, Vocabulary.class, new StyleSpan(Typeface.BOLD));
                }
            }
        }
        else if (tag.equalsIgnoreCase("kana_vocabulary") || tag.equalsIgnoreCase("kanavoc")) {
            if (opening) {
                start(output, new kanaVocabulary());
            }
            else {
                if (GlobalSettings.SubjectInfo.getHighlightSubjectTags()) {
                    end(output, kanaVocabulary.class,
                            new ButtonStyleSpan(ActiveTheme.getSubjectTypeTextColors()[3], ActiveTheme.getSubjectTypeBackgroundColors()[3]));
                }
                else {
                    end(output, kanaVocabulary.class, new StyleSpan(Typeface.BOLD));
                }
            }
        }
        else if (tag.equalsIgnoreCase("reading")) {
            if (opening) {
                start(output, new Reading());
            }
            else {
                if (GlobalSettings.SubjectInfo.getHighlightSubjectTags()) {
                    end(output, Reading.class,
                            new ButtonStyleSpan(ThemeUtil.getColor(R.attr.colorPrimaryLight), ThemeUtil.getColor(androidx.appcompat.R.attr.colorPrimaryDark)));
                }
                else {
                    end(output, Reading.class, new StyleSpan(Typeface.BOLD));
                }
            }
        }
        else if (tag.startsWith("title-image-")) {
            if (opening) {
                start(output, new TitleImage());
            }
            else {
                safe(() -> {
                    final int imageId = Integer.parseInt(tag.substring(12), 10);
                    end(output, TitleImage.class, new TitleImageSpan(imageId));
                });
            }
        }
    }

    /**
     * Helper: find the marker object of the given class.
     *
     * @param text the text to search
     * @param kind the marker class to look for
     * @param <T> the type of the marker
     * @return the marker, or null if not found
     */
    private static @Nullable <T> Object getLast(final Spanned text, final Class<T> kind) {
        final Object[] objs = text.getSpans(0, text.length(), kind);
        return objs.length == 0 ? null : objs[objs.length - 1];
    }

    /**
     * Place the start marker for the start tag.
     *
     * @param text the text to place the marker in
     * @param mark the marker
     */
    private static void start(final Spannable text, final Object mark) {
        final int len = text.length();
        text.setSpan(mark, len, len, Spanned.SPAN_MARK_MARK);
    }

    /**
     * Find the previously placed marker and replace it with an appropriate span.
     *
     * @param text the text to modify
     * @param kind the marker class to find and replace
     * @param repl the replacement span
     * @param <T> the type of the marker
     */
    private static <T> void end(final Spannable text, final Class<T> kind, final Object repl) {
        final int len = text.length();
        final @Nullable Object obj = getLast(text, kind);
        if (obj != null) {
            final int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where != len) {
                text.setSpan(repl, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * Replacement span that draws contained text with a button-style background.
     */
    private static final class ButtonStyleSpan extends CharacterStyle implements UpdateAppearance {
        /**
         * Theme text color.
         */
        private final int textColor;

        /**
         * Theme background color.
         */
        private final int bgColor;

        /**
         * The constructor.
         *
         * @param textColor Theme text color.
         * @param bgColor Theme background color.
         */
        private ButtonStyleSpan(final int textColor, final int bgColor) {
            this.textColor = textColor;
            this.bgColor = bgColor;
        }

        @Override
        public void updateDrawState(final TextPaint tp) {
            tp.bgColor = bgColor;
            tp.setColor(textColor);
        }
    }

    private static final class TitleImageSpan extends ReplacementSpan {
        private final @Nullable Drawable drawable;

        private TitleImageSpan(final int imageId) {
            drawable = ContextCompat.getDrawable(WkApplication.getInstance(), imageId);
        }

        @Override
        public int getSize(final Paint paint, final CharSequence text,
                           final int start, final int end,
                           final @Nullable Paint.FontMetricsInt fm) {
            return (int) paint.getTextSize();
        }

        @Override
        public void draw(final Canvas canvas, final CharSequence text,
                         final int start, final int end, final float x,
                         final int top, final int y, final int bottom, final Paint paint) {
            if (drawable == null) {
                return;
            }

            final int size = (int) paint.getTextSize();

            drawable.setBounds(-1, -1, size - 1, size - 1);
            drawable.setColorFilter(new SimpleColorFilter(Color.BLACK));
            canvas.save();
            canvas.translate(x, bottom - drawable.getBounds().bottom - paint.getFontMetricsInt().descent / 2.0f);
            drawable.draw(canvas);
            canvas.restore();

            drawable.setBounds(2, 2, size + 2, size + 2);
            drawable.setColorFilter(new SimpleColorFilter(Color.BLACK));
            canvas.save();
            canvas.translate(x, bottom - drawable.getBounds().bottom - paint.getFontMetricsInt().descent / 2.0f);
            drawable.draw(canvas);
            canvas.restore();

            drawable.setBounds(0, 0, size, size);
            drawable.setColorFilter(new SimpleColorFilter(paint.getColor()));
            canvas.save();
            canvas.translate(x, bottom - drawable.getBounds().bottom - paint.getFontMetricsInt().descent / 2.0f);
            drawable.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Dummy marker class.
     */
    private static final class Ja {
        //
    }

    /**
     * Dummy marker class.
     */
    private static final class Kanji {
        //
    }

    /**
     * Dummy marker class.
     */
    private static final class Radical {
        //
    }

    /**
     * Dummy marker class.
     */
    private static final class Vocabulary {
        //
    }
    /**
     * Dummy marker class.
     */
    private static final class kanaVocabulary {
        //
    }

    /**
     * Dummy marker class.
     */
    private static final class Reading {
        //
    }

    /**
     * Dummy marker class.
     */
    private static final class TitleImage {
        //
    }
}
