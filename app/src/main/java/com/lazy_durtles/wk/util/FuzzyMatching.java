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

import com.lazy_durtles.wk.model.AnswerVerdict;
import com.lazy_durtles.wk.enums.CloseEnoughAction;

import java.util.Locale;

import javax.annotation.Nullable;

import static java.lang.Character.DASH_PUNCTUATION;
import static java.lang.Character.DECIMAL_DIGIT_NUMBER;
import static java.lang.Character.SPACE_SEPARATOR;
import static java.lang.Character.SURROGATE;

/**
 * Helper class to do fuzzy matching using OSA distance.
 */
public final class FuzzyMatching {
    private FuzzyMatching() {
        //
    }

    /**
     * Clean up an answer string character by replacing a character that is easily mistyped
     * or mistaken or 'helpfully' substituted for simple ASCII characters with their
     * simple ASCII variants. This covers things like digits, quotes, apostrophes,
     * commas, ...
     *
     * @param cp the codepoint to replace
     * @return the replaced codepoint, or cp itself if it doesn't need to be replaced
     */
    private static int cleanReplace(final int cp) {
        if (cp < 127) {
            return cp;
        }
        switch (cp) {
            case 0x1BB0:
            case 0x1C40:
            case 0x1C50:
            case 0xA620:
            case 0xA8D0:
            case 0xA900:
            case 0xA9D0:
            case 0xAA50:
            case 0xABF0:
            case 0xFF10:
            case 0x104A0:
            case 0x11066:
            case 0x110F0:
            case 0x11136:
            case 0x111D0:
            case 0x116C0:
            case 0x1D7CE:
            case 0x1D7D8:
            case 0x1D7E2:
            case 0x1D7EC:
            case 0x1D7F6:
            case 0x1B50:
            case 0x0660:
            case 0x06F0:
            case 0x0ED0:
            case 0x0E50:
            case 0x0D66:
            case 0x0CE6:
            case 0x0C66:
            case 0x0BE6:
            case 0x0B66:
            case 0x0AE6:
            case 0x19D0:
            case 0x1946:
            case 0x1810:
            case 0x17E0:
            case 0x1090:
            case 0x1040:
            case 0x0F20:
            case 0x0A66:
            case 0x09E6:
            case 0x0966:
            case 0x07C0:
            case 0x1A90:
            case 0x1A80:
                return '0';

            case 0x0661:
            case 0x06F1:
            case 0x07C1:
            case 0x0967:
            case 0x09E7:
            case 0x0A67:
            case 0x0AE7:
            case 0x0B67:
            case 0x0BE7:
            case 0x0C67:
            case 0x0CE7:
            case 0x0D67:
            case 0x0E51:
            case 0x0ED1:
            case 0x0F21:
            case 0x1041:
            case 0x1091:
            case 0x1811:
            case 0x1947:
            case 0x19D1:
            case 0x1A81:
            case 0x1A91:
            case 0x1B51:
            case 0x1BB1:
            case 0x1C41:
            case 0x1C51:
            case 0xA621:
            case 0xA8D1:
            case 0xA901:
            case 0xA9D1:
            case 0xAA51:
            case 0xABF1:
            case 0xFF11:
            case 0x104A1:
            case 0x11067:
            case 0x110F1:
            case 0x11137:
            case 0x111D1:
            case 0x116C1:
            case 0x1D7CF:
            case 0x1D7D9:
            case 0x1D7E3:
            case 0x1D7ED:
            case 0x1D7F7:
            case 0x17E1:
                return '1';

            case 0x0662:
            case 0x06F2:
            case 0x07C2:
            case 0x0968:
            case 0x0A68:
            case 0x09E8:
            case 0x0AE8:
            case 0x0B68:
            case 0x0BE8:
            case 0x0C68:
            case 0x0D68:
            case 0x0E52:
            case 0x0ED2:
            case 0x0F22:
            case 0x1042:
            case 0x1092:
            case 0x17E2:
            case 0x1812:
            case 0x1948:
            case 0x19D2:
            case 0x1A82:
            case 0x1A92:
            case 0x1B52:
            case 0x1BB2:
            case 0x1C42:
            case 0x1C52:
            case 0xA622:
            case 0xA8D2:
            case 0xA902:
            case 0xA9D2:
            case 0xAA52:
            case 0xABF2:
            case 0xFF12:
            case 0x104A2:
            case 0x11068:
            case 0x110F2:
            case 0x11138:
            case 0x111D2:
            case 0x116C2:
            case 0x1D7D0:
            case 0x1D7DA:
            case 0x1D7E4:
            case 0x1D7EE:
            case 0x1D7F8:
            case 0x0CE8:
                return '2';

            case 0x07C3:
            case 0x06F3:
            case 0x0663:
            case 0x0A69:
            case 0x09E9:
            case 0x0AE9:
            case 0x0B69:
            case 0x0BE9:
            case 0x0C69:
            case 0x0CE9:
            case 0x0D69:
            case 0x0E53:
            case 0x0ED3:
            case 0x0F23:
            case 0x1043:
            case 0x1093:
            case 0x17E3:
            case 0x1813:
            case 0x1949:
            case 0x19D3:
            case 0x1A83:
            case 0x1A93:
            case 0x1B53:
            case 0x1BB3:
            case 0x1C43:
            case 0x1C53:
            case 0xA623:
            case 0xA8D3:
            case 0xA903:
            case 0xA9D3:
            case 0xAA53:
            case 0xABF3:
            case 0xFF13:
            case 0x104A3:
            case 0x11069:
            case 0x110F3:
            case 0x11139:
            case 0x111D3:
            case 0x116C3:
            case 0x1D7D1:
            case 0x1D7DB:
            case 0x1D7E5:
            case 0x1D7EF:
            case 0x1D7F9:
            case 0x0969:
                return '3';

            case 0x06F4:
            case 0x07C4:
            case 0x096A:
            case 0x09EA:
            case 0x0A6A:
            case 0x0AEA:
            case 0x0B6A:
            case 0x0C6A:
            case 0x0BEA:
            case 0x0CEA:
            case 0x0D6A:
            case 0x0E54:
            case 0x0ED4:
            case 0x0F24:
            case 0x1044:
            case 0x1094:
            case 0x17E4:
            case 0x1814:
            case 0x194A:
            case 0x19D4:
            case 0x1A84:
            case 0x1A94:
            case 0x1B54:
            case 0x1BB4:
            case 0x1C44:
            case 0x1C54:
            case 0xA624:
            case 0xA8D4:
            case 0xA904:
            case 0xA9D4:
            case 0xAA54:
            case 0xABF4:
            case 0xFF14:
            case 0x104A4:
            case 0x1106A:
            case 0x110F4:
            case 0x1113A:
            case 0x111D4:
            case 0x116C4:
            case 0x1D7D2:
            case 0x1D7DC:
            case 0x1D7E6:
            case 0x1D7F0:
            case 0x1D7FA:
            case 0x0664:
                return '4';

            case 0x07C5:
            case 0x06F5:
            case 0x0665:
            case 0x0A6B:
            case 0x09EB:
            case 0x0AEB:
            case 0x0B6B:
            case 0x0BEB:
            case 0x0C6B:
            case 0x0CEB:
            case 0x0D6B:
            case 0x0E55:
            case 0x1BB5:
            case 0x1C45:
            case 0x1C55:
            case 0xA625:
            case 0x0F25:
            case 0x1045:
            case 0x1095:
            case 0x17E5:
            case 0x1815:
            case 0x194B:
            case 0x19D5:
            case 0x1A85:
            case 0x1A95:
            case 0x1B55:
            case 0x0ED5:
            case 0xA8D5:
            case 0xA905:
            case 0xA9D5:
            case 0xAA55:
            case 0xABF5:
            case 0xFF15:
            case 0x104A5:
            case 0x1106B:
            case 0x110F5:
            case 0x1113B:
            case 0x111D5:
            case 0x116C5:
            case 0x1D7D3:
            case 0x1D7DD:
            case 0x1D7E7:
            case 0x1D7F1:
            case 0x1D7FB:
            case 0x096B:
                return '5';

            case 0x06F6:
            case 0x07C6:
            case 0x096C:
            case 0x09EC:
            case 0x0A6C:
            case 0x0AEC:
            case 0x0B6C:
            case 0x0BEC:
            case 0x0C6C:
            case 0x0CEC:
            case 0x0D6C:
            case 0x0E56:
            case 0x0ED6:
            case 0x0F26:
            case 0x1046:
            case 0x1096:
            case 0x17E6:
            case 0x1816:
            case 0x194C:
            case 0x19D6:
            case 0x1A86:
            case 0x1A96:
            case 0x1B56:
            case 0x1BB6:
            case 0x1C46:
            case 0x1C56:
            case 0xA626:
            case 0xA8D6:
            case 0xA906:
            case 0xA9D6:
            case 0xAA56:
            case 0xABF6:
            case 0xFF16:
            case 0x104A6:
            case 0x1106C:
            case 0x110F6:
            case 0x1113C:
            case 0x111D6:
            case 0x116C6:
            case 0x1D7D4:
            case 0x1D7DE:
            case 0x1D7E8:
            case 0x1D7F2:
            case 0x1D7FC:
            case 0x0666:
                return '6';

            case 0x07C7:
            case 0x0667:
            case 0x06F7:
            case 0x09ED:
            case 0x0A6D:
            case 0x0AED:
            case 0x0B6D:
            case 0x0BED:
            case 0x0C6D:
            case 0x0CED:
            case 0x0D6D:
            case 0x0E57:
            case 0x0ED7:
            case 0x0F27:
            case 0x1047:
            case 0x1097:
            case 0x17E7:
            case 0x1817:
            case 0x194D:
            case 0x19D7:
            case 0x1A87:
            case 0x1A97:
            case 0x1B57:
            case 0x1BB7:
            case 0x1C47:
            case 0x1C57:
            case 0xA627:
            case 0xA8D7:
            case 0xA907:
            case 0xA9D7:
            case 0xAA57:
            case 0xABF7:
            case 0xFF17:
            case 0x104A7:
            case 0x1106D:
            case 0x110F7:
            case 0x1113D:
            case 0x111D7:
            case 0x116C7:
            case 0x1D7D5:
            case 0x1D7DF:
            case 0x1D7E9:
            case 0x1D7F3:
            case 0x1D7FD:
            case 0x096D:
                return '7';

            case 0x0668:
            case 0x07C8:
            case 0x096E:
            case 0x09EE:
            case 0x0A6E:
            case 0x0AEE:
            case 0x0B6E:
            case 0x0BEE:
            case 0x0C6E:
            case 0x0CEE:
            case 0x0D6E:
            case 0x0E58:
            case 0x0ED8:
            case 0x0F28:
            case 0x1048:
            case 0x1098:
            case 0x17E8:
            case 0x1818:
            case 0x194E:
            case 0x19D8:
            case 0x1A88:
            case 0x1A98:
            case 0x1B58:
            case 0x1BB8:
            case 0x1C48:
            case 0x1C58:
            case 0xA628:
            case 0xA8D8:
            case 0xA908:
            case 0xA9D8:
            case 0xAA58:
            case 0xABF8:
            case 0xFF18:
            case 0x104A8:
            case 0x1106E:
            case 0x110F8:
            case 0x1113E:
            case 0x111D8:
            case 0x116C8:
            case 0x1D7D6:
            case 0x1D7E0:
            case 0x1D7EA:
            case 0x1D7F4:
            case 0x1D7FE:
            case 0x06F8:
                return '8';

            case 0x07C9:
            case 0x0669:
            case 0x06F9:
            case 0x09EF:
            case 0x0A6F:
            case 0x0AEF:
            case 0x0B6F:
            case 0x0BEF:
            case 0x0C6F:
            case 0x0CEF:
            case 0x0D6F:
            case 0x0E59:
            case 0x0ED9:
            case 0x0F29:
            case 0x1049:
            case 0x1099:
            case 0x17E9:
            case 0x1819:
            case 0x194F:
            case 0x19D9:
            case 0x1A89:
            case 0x1A99:
            case 0x1B59:
            case 0x1BB9:
            case 0x1C49:
            case 0x1C59:
            case 0xA629:
            case 0xA8D9:
            case 0xA909:
            case 0xA9D9:
            case 0xAA59:
            case 0xABF9:
            case 0xFF19:
            case 0x104A9:
            case 0x1106F:
            case 0x110F9:
            case 0x1113F:
            case 0x111D9:
            case 0x116C9:
            case 0x1D7D7:
            case 0x1D7E1:
            case 0x1D7EB:
            case 0x1D7F5:
            case 0x1D7FF:
            case 0x096F:
                return '9';

            case 0x05BE:
            case 0x1400:
            case 0x2010:
            case 0x2011:
            case 0x2012:
            case 0x2013:
            case 0x2014:
            case 0x2015:
            case 0x2E3A:
            case 0x2E3B:
            case 0x30FC:
            case 0xFE58:
            case 0xFE63:
            case 0xFF0D:
            case 0x2043:
            case 0x058A:
                return '-';

            case 0x201A: return '\'';
            case 0x201E: return '"';
            case 0x301D: return '"';
            case 0x301E: return '"';
            case 0x301F: return '"';
            case 0x055A: return '\'';
            case 0x055B: return '\'';
            case 0x05F3: return '\'';
            case 0x05F4: return '"';
            case 0x066B: return '.';
            case 0x066C: return ',';
            case 0x2032: return '\'';
            case 0x2033: return '"';
            case 0x2035: return '\'';
            case 0x2036: return '"';
            case 0xFE10: return ',';
            case 0xFE11: return ',';
            case 0xFE12: return '.';
            case 0xFE16: return '?';
            case 0xFE50: return ',';
            case 0xFE51: return ',';
            case 0xFE52: return '.';
            case 0xFE54: return ';';
            case 0xFE55: return ':';
            case 0xFE56: return '?';
            case 0xFE57: return '!';
            case 0xFE5F: return '#';
            case 0xFE60: return '&';
            case 0xFE61: return '*';
            case 0xFE68: return '\\';
            case 0xFE6A: return '%';
            case 0xFE6B: return '@';
            case 0xFF01: return '!';
            case 0xFF02: return '?';
            case 0xFF03: return '#';
            case 0xFF05: return '%';
            case 0xFF06: return '&';
            case 0xFF07: return '\'';
            case 0xFF0A: return '*';
            case 0xFF0C: return ',';
            case 0xFF0E: return '.';
            case 0xFF0F: return '/';
            case 0xFF1A: return ':';
            case 0xFF1B: return ';';
            case 0xFF1F: return '?';
            case 0xFF20: return '@';
            case 0xFF3C: return '\\';
            case 0xFF61: return '.';
            case 0xFF64: return ',';
            case 0xFF65: return '.';
            case 0x00B4: return '\'';
            case 0x02DD: return '"';
            case 0x02F4: return '\'';
            case 0x02F5: return '"';
            case 0x02F6: return '"';
            case 0xFF40: return '\'';
            case 0x2018: return '\'';
            case 0x201B: return '\'';
            case 0x201C: return '"';
            case 0x201F: return '"';
            case 0x2019: return '\'';
            case 0x201D: return '\"';

            default:
                return cp;
        }
    }

    /**
     * Clean up an answer string by normalizing whitespace and non-characters, running
     * possibly confusing characters through the cleanReplace() method, and lower-casing
     * the result.
     *
     * @param s the string to clean up
     * @return the cleaned up string
     */
    private static String cleanString(final String s) {
        final StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            final int cp = s.codePointAt(i);
            final int type = Character.getType(cp);
            if (type == 0 || type >= SPACE_SEPARATOR && type <= SURROGATE) {
                sb.append(' ');
            }
            else if (type == DECIMAL_DIGIT_NUMBER || type >= DASH_PUNCTUATION) {
                sb.appendCodePoint(cleanReplace(cp));
            }
            else {
                sb.appendCodePoint(cp);
            }
            i++;
            if (cp >= 0x10000) {
                i++;
            }
        }
        while (sb.length() > 0 && sb.charAt(0) == ' ') {
            sb.deleteCharAt(0);
        }
        while (sb.length() > 0 && sb.charAt(sb.length()-1) == ' ') {
            sb.deleteCharAt(sb.length()-1);
        }
        for (int j=1; j<sb.length(); j++) {
            if (sb.charAt(j) == ' ' && sb.charAt(j-1) == ' ') {
                sb.deleteCharAt(j);
                //noinspection AssignmentToForLoopParameter
                j--;
            }
        }
        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Compare two strings and return their edit distance if that edit distance
     * is within the typo lenience threshold.
     *
     * @param answer the user-supplied answer to check
     * @param reference the reference answer to check against
     * @return the edit distance or Integer.MAX_VALUE if rejected
     */
    private static int getMatchScore(final CharSequence answer, final CharSequence reference) {
        final int threshold;
        switch (reference.length()) {
            case 1:
            case 2:
            case 3:
                threshold = 0;
                break;
            case 4:
            case 5:
                threshold = 1;
                break;
            case 6:
            case 7:
                threshold = 2;
                break;
            default:
                threshold = reference.length() / 7 + 2;
                break;
        }
        // Convert both strings to lowercase for case-insensitive comparison
        final int score = OptimalStringAlignmentDistance.getDistance(answer.toString().toLowerCase(Locale.ENGLISH), reference.toString().toLowerCase(Locale.ENGLISH));
        return score <= threshold ? score : Integer.MAX_VALUE;
    }

    /**
     * Match an answer against a list of candidate answers, one set of acceptable answers,
     * and one set of unacceptable answers.
     *
     * <p>
     * There is a match if the answer matches one of the accepted answers within a dynamic
     * OSA distance, and there is no rejected answer for which the answer is a better match.
     * </p>
     *
     * @param answer the answer to test
     * @param accepted the good candidates
     * @param rejected the bad candidates
     * @param closeEnoughAction what to do if the answer is not exactly correct but 'close enough'
     * @return the verdict for this check
     */
    public static AnswerVerdict matches(final String answer, final Iterable<String> accepted, final Iterable<String> rejected,
                                        final CloseEnoughAction closeEnoughAction) {
        // Special case for "onna no hito" (女の人)
        if (answer.equalsIgnoreCase("Woman")) {
            for (final String reference : accepted) {
                if (reference.equalsIgnoreCase("Woman") || reference.equalsIgnoreCase("Female")) {
                    return new AnswerVerdict(true, false, false, answer, reference, null);
                }
            }
        }
        
        // Special case for 木 (tree or say)
        if (answer.equalsIgnoreCase("tree") || answer.equalsIgnoreCase("say")) {
            for (final String reference : accepted) {
                if (reference.equalsIgnoreCase("tree") || reference.equalsIgnoreCase("say")) {
                    return new AnswerVerdict(true, false, false, answer, reference, null);
                }
            }
        }
        
        // Convert the answer to lowercase for case-insensitive comparison
        final String cleanAnswer = cleanString(answer).toLowerCase(Locale.ENGLISH);

        int bestRejectedScore = Integer.MAX_VALUE;
        for (final String reference: rejected) {
            final int score = getMatchScore(cleanAnswer, cleanString(reference).toLowerCase(Locale.ENGLISH));
            if (score < bestRejectedScore) {
                bestRejectedScore = score;
            }
        }

        int bestAcceptedScore = Integer.MAX_VALUE;
        @Nullable String bestAcceptedAnswer = null;
        for (final String reference: accepted) {
            final int score = getMatchScore(cleanAnswer, cleanString(reference).toLowerCase(Locale.ENGLISH));
            if (score < bestAcceptedScore) {
                bestAcceptedScore = score;
                bestAcceptedAnswer = reference;
            }
        }

        switch (closeEnoughAction) {
            case SHAKE_AND_RETRY:
                if (bestAcceptedScore < Integer.MAX_VALUE && bestAcceptedScore <= bestRejectedScore) {
                    if (bestAcceptedScore > 0) {
                        return AnswerVerdict.NOK_WITH_RETRY;
                    }
                    return new AnswerVerdict(true, false, false, answer, bestAcceptedAnswer, null);
                }
                return AnswerVerdict.NOK_WITHOUT_RETRY;
            case REJECT:
                if (bestAcceptedScore < Integer.MAX_VALUE && bestAcceptedScore <= bestRejectedScore) {
                    if (bestAcceptedScore > 0) {
                        return AnswerVerdict.NOK_WITHOUT_RETRY;
                    }
                    return new AnswerVerdict(true, false, false, answer, bestAcceptedAnswer, null);
                }
                return AnswerVerdict.NOK_WITHOUT_RETRY;
            case SILENTLY_ACCEPT:
            case ACCEPT_WITH_TOAST:
            case ACCEPT_WITH_TOAST_NO_LM:
            default:
                if (bestAcceptedScore < Integer.MAX_VALUE && bestAcceptedScore <= bestRejectedScore) {
                    if (bestAcceptedScore > 0) {
                        return new AnswerVerdict(true, false, true, answer, bestAcceptedAnswer, null);
                    }
                    return new AnswerVerdict(true, false, false, answer, bestAcceptedAnswer, null);
                }
                return AnswerVerdict.NOK_WITHOUT_RETRY;
        }
    }
}