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

import static java.lang.Math.min;

/**
 * Implementation of the Optimal String Alignment algorithm. This is used
 * to compute an edit distance between two strings. This is similar to
 * Levenshtein or Damerau-Levenshtein, but more flexible than the former
 * (OSA permits transpositions) and more efficient than the latter.
 */
public final class OptimalStringAlignmentDistance {
    private OptimalStringAlignmentDistance() {
        //
    }

    /**
     * Get the edit distance between two strings.
     *
     * @param a one string
     * @param b another string
     * @return the edit distance between a and b according to OSA
     */
    public static int getDistance(final CharSequence a, final CharSequence b) {
        if (a.length() == 0) {
            return b.length();
        }
        if (b.length() == 0) {
            return a.length();
        }

        final int[][] d = new int[a.length() + 1][];
        for (int i=0; i<a.length()+1; i++) {
            d[i] = new int[b.length()+1];
            d[i][0] = i;
        }
        for (int j=0; j<b.length()+1; j++) {
            d[0][j] = j;
        }

        for (int i=0; i<a.length(); i++) {
            for (int j=0; j<b.length(); j++) {
                final int cost = a.charAt(i) == b.charAt(j) ? 0 : 1;
                d[i+1][j+1] = min(min(d[i][j+1] + 1, d[i+1][j] + 1), d[i][j] + cost);
                if (i > 0 && j > 0 && a.charAt(i) == b.charAt(j-1) && a.charAt(i-1) == b.charAt(j)) {
                    d[i+1][j+1] = min(d[i+1][j+1], d[i-1][j-1] + cost);
                }
            }
        }

        return d[a.length()][b.length()];
    }
}
