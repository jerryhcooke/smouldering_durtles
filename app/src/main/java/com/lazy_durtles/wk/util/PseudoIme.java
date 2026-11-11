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

package com.lazy_durtles.wk.util;

import android.text.Editable;
import android.text.InputFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.ObjectSupport.orElse;

/**
 * A Pseudo-IME implementation that will replace Romaji in a string
 * with corresponding Kana characters, in roughly the same way that a Japanese IME
 * with Romaji -> Kana translation would do it.
 */
public final class PseudoIme {
    private static final Map<String, String> KANA_MAP = new HashMap<>();

    static {
        fillKanaMap();
    }

    private PseudoIme() {
        //
    }

    /**
     * Fill the kana map with static date, covering both Hiragana and Katakana.
     */
    private static void fillKanaMap() {
        // Single vowels
        KANA_MAP.put("a", "あ");
        KANA_MAP.put("i", "い");
        KANA_MAP.put("u", "う");
        KANA_MAP.put("e", "え");
        KANA_MAP.put("o", "お");

        // K and G
        KANA_MAP.put("ka", "か");
        KANA_MAP.put("ga", "が");
        KANA_MAP.put("ki", "き");
        KANA_MAP.put("gi", "ぎ");
        KANA_MAP.put("ku", "く");
        KANA_MAP.put("gu", "ぐ");
        KANA_MAP.put("ke", "け");
        KANA_MAP.put("ge", "げ");
        KANA_MAP.put("ko", "こ");
        KANA_MAP.put("go", "ご");

        // K variants
        KANA_MAP.put("ca", "か");
        KANA_MAP.put("ci", "き");
        KANA_MAP.put("cu", "く");
        KANA_MAP.put("ce", "け");
        KANA_MAP.put("co", "こ");

        // S and Z, special case shi/si
        KANA_MAP.put("sa", "さ");
        KANA_MAP.put("za", "ざ");
        KANA_MAP.put("shi", "し");
        KANA_MAP.put("si", "し");
        KANA_MAP.put("ji", "じ");
        KANA_MAP.put("zi", "じ");
        KANA_MAP.put("su", "す");
        KANA_MAP.put("zu", "ず");
        KANA_MAP.put("se", "せ");
        KANA_MAP.put("ze", "ぜ");
        KANA_MAP.put("so", "そ");
        KANA_MAP.put("zo", "ぞ");

        // T and D, special cases chi/ti, tsu/tu and the small tsu
        KANA_MAP.put("ta", "た");
        KANA_MAP.put("da", "だ");
        KANA_MAP.put("chi", "ち");
        KANA_MAP.put("ti", "ち");
        KANA_MAP.put("di", "ぢ");
        KANA_MAP.put("tsu", "つ");
        KANA_MAP.put("tu", "つ");
        KANA_MAP.put("du", "づ");
        KANA_MAP.put("te", "て");
        KANA_MAP.put("de", "で");
        KANA_MAP.put("to", "と");
        KANA_MAP.put("do", "ど");

        // Standalone small tsu, when not triggered a a double consonant
        KANA_MAP.put("ltu", "っ");
        KANA_MAP.put("xtu", "っ");
        KANA_MAP.put("ltsu", "っ");

        // N but without the standalone N
        KANA_MAP.put("na", "な");
        KANA_MAP.put("ni", "に");
        KANA_MAP.put("nu", "ぬ");
        KANA_MAP.put("ne", "ね");
        KANA_MAP.put("no", "の");

        // H, B and P, with special case fu/hu
        KANA_MAP.put("ha", "は");
        KANA_MAP.put("ba", "ば");
        KANA_MAP.put("pa", "ぱ");
        KANA_MAP.put("hi", "ひ");
        KANA_MAP.put("bi", "び");
        KANA_MAP.put("pi", "ぴ");
        KANA_MAP.put("fu", "ふ");
        KANA_MAP.put("hu", "ふ");
        KANA_MAP.put("bu", "ぶ");
        KANA_MAP.put("pu", "ぷ");
        KANA_MAP.put("he", "へ");
        KANA_MAP.put("be", "べ");
        KANA_MAP.put("pe", "ぺ");
        KANA_MAP.put("ho", "ほ");
        KANA_MAP.put("bo", "ぼ");
        KANA_MAP.put("po", "ぽ");

        // M
        KANA_MAP.put("ma", "ま");
        KANA_MAP.put("mi", "み");
        KANA_MAP.put("mu", "む");
        KANA_MAP.put("me", "め");
        KANA_MAP.put("mo", "も");

        // Y with special cases the small ones
        KANA_MAP.put("ya", "や");
        KANA_MAP.put("yu", "ゆ");
        KANA_MAP.put("yo", "よ");

        // Small ya/yu/yo standalone, when not triggered by a contraction like 'kyo'
        KANA_MAP.put("xya", "ゃ");
        KANA_MAP.put("xyu", "ゅ");
        KANA_MAP.put("xyo", "ょ");

        // R
        KANA_MAP.put("ra", "ら");
        KANA_MAP.put("ri", "り");
        KANA_MAP.put("ru", "る");
        KANA_MAP.put("re", "れ");
        KANA_MAP.put("ro", "ろ");

        // R variants for loan words with L sounds
        KANA_MAP.put("la", "ら");
        KANA_MAP.put("li", "り");
        KANA_MAP.put("lu", "る");
        KANA_MAP.put("le", "れ");
        KANA_MAP.put("lo", "ろ");

        // W with special case small wa
        // wi and we are not represented here, they are used for digraphs 'ui', 'ue'
        KANA_MAP.put("wa", "わ");
        KANA_MAP.put("wo", "を");
        KANA_MAP.put("lwe", "ゎ");
        KANA_MAP.put("xwa", "ゎ");

        // Standalone N
        // This one has some special cases when followed by a non-vowel, such as nk -> んk
        KANA_MAP.put("nn", "ん");
        KANA_MAP.put("n ", "ん");
        KANA_MAP.put("xn", "ん");

        // Regular ya/yu/yo digraphs, many with variants
        KANA_MAP.put("kya", "きゃ");
        KANA_MAP.put("kyu", "きゅ");
        KANA_MAP.put("kyo", "きょ");
        KANA_MAP.put("gya", "ぎゃ");
        KANA_MAP.put("gyu", "ぎゅ");
        KANA_MAP.put("gyo", "ぎょ");
        KANA_MAP.put("qya", "くゃ");
        KANA_MAP.put("qyu", "くゅ");
        KANA_MAP.put("qyo", "くょ");
        KANA_MAP.put("sha", "しゃ");
        KANA_MAP.put("shya", "しゃ");
        KANA_MAP.put("sya", "しゃ");
        KANA_MAP.put("shu", "しゅ");
        KANA_MAP.put("shyu", "しゅ");
        KANA_MAP.put("syu", "しゅ");
        KANA_MAP.put("sho", "しょ");
        KANA_MAP.put("shyo", "しょ");
        KANA_MAP.put("syo", "しょ");
        KANA_MAP.put("zya", "じゃ");
        KANA_MAP.put("zyu", "じゅ");
        KANA_MAP.put("zyo", "じょ");
        KANA_MAP.put("ja", "じゃ");
        KANA_MAP.put("ju", "じゅ");
        KANA_MAP.put("jo", "じょ");
        KANA_MAP.put("jya", "じゃ");
        KANA_MAP.put("jyu", "じゅ");
        KANA_MAP.put("jyo", "じょ");
        KANA_MAP.put("cha", "ちゃ");
        KANA_MAP.put("cya", "ちゃ");
        KANA_MAP.put("chya", "ちゃ");
        KANA_MAP.put("tya", "ちゃ");
        KANA_MAP.put("chu", "ちゅ");
        KANA_MAP.put("cyu", "ちゅ");
        KANA_MAP.put("chyu", "ちゅ");
        KANA_MAP.put("tyu", "ちゅ");
        KANA_MAP.put("cho", "ちょ");
        KANA_MAP.put("cyo", "ちょ");
        KANA_MAP.put("chyo", "ちょ");
        KANA_MAP.put("tyo", "ちょ");
        KANA_MAP.put("dya", "ぢゃ");
        KANA_MAP.put("dyu", "ぢゅ");
        KANA_MAP.put("dyo", "ぢょ");
        KANA_MAP.put("tha", "てゃ");
        KANA_MAP.put("thu", "てゅ");
        KANA_MAP.put("tho", "てょ");
        KANA_MAP.put("dha", "でゃ");
        KANA_MAP.put("dhu", "でゅ");
        KANA_MAP.put("dho", "でょ");
        KANA_MAP.put("nya", "にゃ");
        KANA_MAP.put("nyu", "にゅ");
        KANA_MAP.put("nyo", "にょ");
        KANA_MAP.put("hya", "ひゃ");
        KANA_MAP.put("hyu", "ひゅ");
        KANA_MAP.put("hyo", "ひょ");
        KANA_MAP.put("bya", "びゃ");
        KANA_MAP.put("byu", "びゅ");
        KANA_MAP.put("byo", "びょ");
        KANA_MAP.put("pya", "ぴゃ");
        KANA_MAP.put("pyu", "ぴゅ");
        KANA_MAP.put("pyo", "ぴょ");
        KANA_MAP.put("fya", "ふゃ");
        KANA_MAP.put("fyu", "ふゅ");
        KANA_MAP.put("fyo", "ふょ");
        KANA_MAP.put("mya", "みゃ");
        KANA_MAP.put("myu", "みゅ");
        KANA_MAP.put("myo", "みょ");
        KANA_MAP.put("rya", "りゃ");
        KANA_MAP.put("ryu", "りゅ");
        KANA_MAP.put("ryo", "りょ");
        KANA_MAP.put("lya", "りゃ");
        KANA_MAP.put("lyu", "りゅ");
        KANA_MAP.put("lyo", "りょ");
        KANA_MAP.put("vya", "ゔゃ");
        KANA_MAP.put("vyu", "ゔゅ");
        KANA_MAP.put("vyo", "ゔょ");

        // A bunch of digraphs that use small a/i/u/e/o vowels
        KANA_MAP.put("ye", "いぇ");
        KANA_MAP.put("wha", "うぁ");
        KANA_MAP.put("whi", "うぃ");
        KANA_MAP.put("whe", "うぇ");
        KANA_MAP.put("who", "うぉ");
        KANA_MAP.put("wi", "うぃ");
        KANA_MAP.put("we", "うぇ");
        KANA_MAP.put("kyi", "きぃ");
        KANA_MAP.put("kye", "きぇ");
        KANA_MAP.put("qwa", "くぁ");
        KANA_MAP.put("qwi", "くぃ");
        KANA_MAP.put("qwu", "くぅ");
        KANA_MAP.put("qwe", "くぇ");
        KANA_MAP.put("qwo", "くぉ");
        KANA_MAP.put("qa", "くぁ");
        KANA_MAP.put("qi", "くぃ");
        KANA_MAP.put("qe", "くぇ");
        KANA_MAP.put("qo", "くぉ");
        KANA_MAP.put("kwa", "くぁ");
        KANA_MAP.put("qyi", "くぃ");
        KANA_MAP.put("qye", "くぇ");
        KANA_MAP.put("gyi", "ぎぃ");
        KANA_MAP.put("gye", "ぎぇ");
        KANA_MAP.put("gwa", "ぐぁ");
        KANA_MAP.put("gwi", "ぐぃ");
        KANA_MAP.put("gwu", "ぐぅ");
        KANA_MAP.put("gwe", "ぐぇ");
        KANA_MAP.put("gwo", "ぐぉ");
        KANA_MAP.put("syi", "しぃ");
        KANA_MAP.put("sye", "しぇ");
        KANA_MAP.put("she", "しぇ");
        KANA_MAP.put("shye", "しぇ");
        KANA_MAP.put("zyi", "じぃ");
        KANA_MAP.put("zye", "じぇ");
        KANA_MAP.put("je", "じぇ");
        KANA_MAP.put("jyi", "じぃ");
        KANA_MAP.put("jye", "じぇ");
        KANA_MAP.put("swa", "すぁ");
        KANA_MAP.put("swi", "すぃ");
        KANA_MAP.put("swu", "すぅ");
        KANA_MAP.put("swe", "すぇ");
        KANA_MAP.put("swo", "すぉ");
        KANA_MAP.put("che", "ちぇ");
        KANA_MAP.put("cyi", "ちぃ");
        KANA_MAP.put("cye", "ちぇ");
        KANA_MAP.put("tyi", "ちぃ");
        KANA_MAP.put("tye", "ちぇ");
        KANA_MAP.put("chye", "ちぇ");
        KANA_MAP.put("dyi", "ぢぃ");
        KANA_MAP.put("dye", "ぢぇ");
        KANA_MAP.put("tsa", "つぁ");
        KANA_MAP.put("tsi", "つぃ");
        KANA_MAP.put("tse", "つぇ");
        KANA_MAP.put("tso", "つぉ");
        KANA_MAP.put("thi", "てぃ");
        KANA_MAP.put("the", "てぇ");
        KANA_MAP.put("twa", "とぁ");
        KANA_MAP.put("twi", "とぃ");
        KANA_MAP.put("twu", "とぅ");
        KANA_MAP.put("twe", "とぇ");
        KANA_MAP.put("two", "とぉ");
        KANA_MAP.put("dhi", "でぃ");
        KANA_MAP.put("dhe", "でぇ");
        KANA_MAP.put("dwa", "どぁ");
        KANA_MAP.put("dwi", "どぃ");
        KANA_MAP.put("dwu", "どぅ");
        KANA_MAP.put("dwe", "どぇ");
        KANA_MAP.put("dwo", "どぉ");
        KANA_MAP.put("nyi", "にぃ");
        KANA_MAP.put("nye", "にぇ");
        KANA_MAP.put("hyi", "ひぃ");
        KANA_MAP.put("hye", "ひぇ");
        KANA_MAP.put("fwa", "ふぁ");
        KANA_MAP.put("fwi", "ふぃ");
        KANA_MAP.put("fwu", "ふぅ");
        KANA_MAP.put("fwe", "ふぇ");
        KANA_MAP.put("fwo", "ふぉ");
        KANA_MAP.put("fa", "ふぁ");
        KANA_MAP.put("fi", "ふぃ");
        KANA_MAP.put("fe", "ふぇ");
        KANA_MAP.put("fo", "ふぉ");
        KANA_MAP.put("fyi", "ふぃ");
        KANA_MAP.put("fye", "ふぇ");
        KANA_MAP.put("byi", "びぃ");
        KANA_MAP.put("bye", "びぇ");
        KANA_MAP.put("pyi", "ぴぃ");
        KANA_MAP.put("pye", "ぴぇ");
        KANA_MAP.put("myi", "みぃ");
        KANA_MAP.put("mye", "みぇ");
        KANA_MAP.put("ryi", "りぃ");
        KANA_MAP.put("rye", "りぇ");
        KANA_MAP.put("lyi", "りぃ");
        KANA_MAP.put("lye", "りぇ");
        KANA_MAP.put("ve", "ゔぇ");
        KANA_MAP.put("vo", "ゔぉ");
        KANA_MAP.put("vyi", "ゔぃ");
        KANA_MAP.put("vye", "ゔぇ");
        KANA_MAP.put("va", "ゔぁ");
        KANA_MAP.put("vi", "ゔぃ");

        // Some misc vowel representations
        KANA_MAP.put("yi", "い");
        KANA_MAP.put("wu", "う");
        KANA_MAP.put("whu", "う");
        KANA_MAP.put("xa", "ぁ");
        KANA_MAP.put("xi", "ぃ");
        KANA_MAP.put("xu", "ぅ");
        KANA_MAP.put("xe", "ぇ");
        KANA_MAP.put("xo", "ぉ");
        KANA_MAP.put("xyi", "ぃ");
        KANA_MAP.put("xye", "ぇ");

        // 'u' with dakuten -> vu sound
        KANA_MAP.put("vu", "ゔ");

        // Small ka/ke
        KANA_MAP.put("lka", "ゕ");
        KANA_MAP.put("lke", "ゖ");
        KANA_MAP.put("xka", "ゕ");
        KANA_MAP.put("xke", "ゖ");
        KANA_MAP.put("lca", "ゕ");
        KANA_MAP.put("lce", "ゖ");
        KANA_MAP.put("xca", "ゕ");
        KANA_MAP.put("xce", "ゖ");

        // Vowel doubler '-' -> 'ー'
        KANA_MAP.put("-", "ー");

        for (final String key: new ArrayList<>(KANA_MAP.keySet())) {
            final @Nullable String value = KANA_MAP.get(key);
            if (value != null) {
                KANA_MAP.put(key.toUpperCase(Locale.ENGLISH), orElse(toKatakana(value), ""));
            }
        }
    }

    /**
     * Advance the IME processing. If any modifications are necessary,
     * they are applied to the Editable in place.
     *
     * @param s the Editable containing the input text
     * @param editStart the start of the latest change to process
     * @param editEnd the end of the latest change to process
     */
    public static void fixup(final Editable s, final int editStart, final int editEnd) {
        int start = editStart;
        int end = editEnd;

        while (start >= 0 && start < end) {
            if (start > 0) {
                final char c1 = s.charAt(start-1);
                final char c2 = s.charAt(start);
                if (c1 == 'n' && "aiueoyn ".indexOf(c2) < 0) {
                    s.replace(start-1, start, "ん");
                    continue;
                }
                if (c1 == c2 && "bcdfghjklmpqrstvwxz".indexOf(c1) >= 0) {
                    s.replace(start-1, start, "っ");
                    continue;
                }
            }

            boolean found = false;
            for (int i=start-3; i<=start; i++) {
                if (i < 0) {
                    continue;
                }
                final String key = s.subSequence(i, start+1).toString();
                final @Nullable String replacement = KANA_MAP.get(key);
                if (replacement != null) {
                    s.replace(i, start+1, replacement);
                    end -= start + 1 - i;
                    end += replacement.length();
                    if (end > s.length()) {
                        end = s.length();
                    }
                    start = i + replacement.length();
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }

            if (Character.isWhitespace(s.charAt(start))) {
                s.delete(start, start+1);
                end--;
                continue;
            }

            start++;
        }
    }

    /**
     * Simulate inputting each character in a string in turn and applying the IME to it.
     *
     * @param input the input to simulate
     * @return the transformed text
     */
    public static String simulateInput(final String input) {
        final Editable editable = new DummyEditable(input);
        fixup(editable, 0, input.length());
        if (editable.charAt(editable.length()-1) == 'n') {
            editable.replace(editable.length()-1, editable.length(), "ん");
        }
        return editable.toString();
    }

    /**
     * Convert Hiragana characters in a string to Katakana.
     *
     * @param hiragana the source
     * @return the output
     */
    public static @Nullable String toKatakana(final @Nullable CharSequence hiragana) {
        if (hiragana == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(hiragana.length());
        for (int i=0; i<hiragana.length(); i++) {
            char c = hiragana.charAt(i);
            if (c >= 0x3040 && c <= 0x3096) {
                c += 0x60;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * A dummy implementation of Editable that is just capable enough for the fixup() method.
      */
    private static final class DummyEditable implements Editable {
        /**
         * The current contents of the editable.
         */
        private String value;

        /**
         * The constructor.
         *
         * @param value initial value for the editable
         */
        DummyEditable(final String value) {
            this.value = value;
        }

        @Override
        public @Nonnull Editable replace(final int st, final int en, final CharSequence source, final int start, final int end) {
            value = value.substring(0, st) + source.subSequence(start, end) + value.substring(en);
            return this;
        }

        @Override
        public Editable replace(final int st, final int en, final CharSequence text) {
            return replace(st, en, text, 0, text.length());
        }

        @Override
        public Editable insert(final int where, final CharSequence text, final int start, final int end) {
            return replace(where, where, text, start, end);
        }

        @Override
        public Editable insert(final int where, final CharSequence text) {
            return replace(where, where, text, 0, text.length());
        }

        @Override
        public Editable delete(final int st, final int en) {
            return replace(st, en, "", 0, 0);
        }

        @Override
        public @Nonnull Editable append(final @Nonnull CharSequence text) {
            return replace(length(), length(), text, 0, text.length());
        }

        @Override
        public @Nonnull Editable append(final @Nonnull CharSequence text, final int start, final int end) {
            return replace(length(), length(), text, start, end);
        }

        @Override
        public @Nonnull Editable append(final char text) {
            return append(String.valueOf(text));
        }

        @Override
        public void clear() {
            value = "";
        }

        @Override
        public void clearSpans() {
            //
        }

        @Override
        public void setFilters(final InputFilter[] filters) {
            //
        }

        @Override
        public InputFilter[] getFilters() {
            return new InputFilter[0];
        }

        @Override
        public void getChars(final int start, final int end, final char[] dest, final int destoff) {
            //
        }

        @Override
        public void setSpan(final Object what, final int start, final int end, final int flags) {
            //
        }

        @Override
        public void removeSpan(final Object what) {
            //
        }

        @SuppressWarnings("RedundantSuppression")
        @Override
        public <T> T[] getSpans(final int start, final int end, final Class<T> type) {
            //noinspection ConstantConditions,ReturnOfNull
            return null;
        }

        @Override
        public int getSpanStart(final Object tag) {
            return 0;
        }

        @Override
        public int getSpanEnd(final Object tag) {
            return 0;
        }

        @Override
        public int getSpanFlags(final Object tag) {
            return 0;
        }

        @Override
        public int nextSpanTransition(final int start, final int limit, final Class type) {
            return limit;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(final int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return value.subSequence(start, end);
        }

        @Override
        public @Nonnull String toString() {
            return value;
        }
    }
}
