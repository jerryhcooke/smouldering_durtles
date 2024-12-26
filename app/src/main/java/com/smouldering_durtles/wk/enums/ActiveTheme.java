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

package com.smouldering_durtles.wk.enums;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.R;

import java.util.List;

/**
 * Enum for the currently active theme.
 */
@SuppressWarnings({"JavaDoc", "WeakerAccess", "RedundantSuppression"})
public enum ActiveTheme {
    LIGHT(
            true,
            R.style.WKLightTheme,
            R.drawable.ic_arrow_up_small,
            new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3,0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFFEEEEEE, 0xFFC1C0C1, 0xFFD80088, 0xFF7D2893, 0xFF2344D6, 0xFF0094EB, 0xFF444444},
            new int[] {0xFFD6AFCA, 0xFFCB8FB3, 0xFFD660AB, 0xFFD80088},
            new int[] {0xFF9D30A3, 0xFF7D2893},
            new int[] {0xFF621899, 0xFFB24188, 0xFFDE5FAE, 0xFFD86CB0, 0xFFD077AF, 0xFFCB8FB3, 0xFFD09FBE, 0xFFD6AFCA, 0xFFC1C0C1, 0xFFEEEEEE},
            new int[] {0xFF909000, 0xFF909000, 0xFF00A000, 0xFFA00000, 0xFFFFFFFF, 0xFF909000}) {
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = baseSubjectTypeTextColors[i];
                subjectTypeBackgroundColors[i] = (color == 0) ? baseSubjectTypeBackgroundColors[i] : color;
                subjectTypeButtonBackgroundColors[i] = (color == 0) ? baseSubjectTypeButtonBackgroundColors[i] : color;
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+15);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    },
    DARK_WANIKANI(
            true,
            R.style.WKDarkTheme,
            R.drawable.ic_arrow_up_small_dark,
            new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3,0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFFEEEEEE, 0xFFC1C0C1, 0xFFD80088, 0xFF7D2893, 0xFF2344D6, 0xFF0094EB, 0xFF444444},
            new int[] {0xFFD6AFCA, 0xFFCB8FB3, 0xFFD660AB, 0xFFD80088},
            new int[] {0xFF9D30A3, 0xFF7D2893},
            new int[] {0xFF621899, 0xFFB24188, 0xFFDE5FAE, 0xFFD86CB0, 0xFFD077AF, 0xFFCB8FB3, 0xFFD09FBE, 0xFFD6AFCA, 0xFFC1C0C1, 0xFFEEEEEE},
            new int[] {0xFF909000, 0xFF909000, 0xFF00A000, 0xFFA00000, 0xFFFFFFFF, 0xFF909000}) {
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = baseSubjectTypeTextColors[i];
                subjectTypeBackgroundColors[i] = (color == 0) ? baseSubjectTypeBackgroundColors[i] : color;
                subjectTypeButtonBackgroundColors[i] = (color == 0) ? baseSubjectTypeButtonBackgroundColors[i] : color;
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+15);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    },
    BLACK_WANIKANI(
            true,
            R.style.WKBlackTheme,
            R.drawable.ic_arrow_up_small_dark,
            new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3,0xFF9808F3},
            new int[] {0xFF0098F0, 0xFFE80092, 0xFF9808F3, 0xFF9808F3},
            new int[] {0xFFEEEEEE, 0xFFC1C0C1, 0xFFD80088, 0xFF7D2893, 0xFF2344D6, 0xFF0094EB, 0xFF444444},
            new int[] {0xFFD6AFCA, 0xFFCB8FB3, 0xFFD660AB, 0xFFD80088},
            new int[] {0xFF9D30A3, 0xFF7D2893},
            new int[] {0xFF621899, 0xFFB24188, 0xFFDE5FAE, 0xFFD86CB0, 0xFFD077AF, 0xFFCB8FB3, 0xFFD09FBE, 0xFFD6AFCA, 0xFFC1C0C1, 0xFFEEEEEE},
            new int[] {0xFF909000, 0xFF909000, 0xFF00A000, 0xFFA00000, 0xFFFFFFFF, 0xFF909000}) {
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = baseSubjectTypeTextColors[i];
                subjectTypeBackgroundColors[i] = (color == 0) ? baseSubjectTypeBackgroundColors[i] : color;
                subjectTypeButtonBackgroundColors[i] = (color == 0) ? baseSubjectTypeButtonBackgroundColors[i] : color;
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+15);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    },
    @SuppressWarnings("unused")
    DARK(
            false,
            R.style.WKDarkTheme,
            R.drawable.ic_arrow_up_small_dark,
            new int[] {0xFF3DAEE9, 0xFFFDBC4B, 0xFF2ECC71, 0xFF2ECC71},
            new int[] {0xFF232629, 0xFF232629, 0xFF232629, 0xFF232629},
            new int[] {0xFF404040, 0xFF404040, 0xFF404040, 0xFF404040},
            new int[] {0xFF3DAEE9, 0xFFFDBC4B, 0xFF2ECC71, 0xFFF65700},
            new int[] {0xFF1D2023, 0xFFC1C0C1, 0xFF1D99F3, 0xFF1CDC9A, 0xFFC9CE3B, 0xFFF67400, 0xFFD53B49},
            new int[] {0xFF7DC9FC, 0xFF5DB9F9, 0xFF3DA9F6, 0xFF0D89E3},
            new int[] {0xFF20FCAA, 0xFF1CDC9A},
            new int[] {0xFF1A3A45, 0xFF0D89E3, 0xFF2599EC, 0xFF3DA9F6, 0xFF4DB1F7, 0xFF5DB9F9, 0xFF6DC1FA, 0xFF7DC9FC, 0xFFC1C0C1, 0xFF1D2023},
            new int[] {0xFF606000, 0xFF606000, 0xFF006000, 0xFF600000, 0xFFE8E8E8, 0xFF606000}) {
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = (color == 0) ? baseSubjectTypeTextColors[i] : color;
                subjectTypeBackgroundColors[i] = baseSubjectTypeBackgroundColors[i];
                subjectTypeButtonBackgroundColors[i] = baseSubjectTypeButtonBackgroundColors[i];
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+14);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    },
    @SuppressWarnings("unused")
    NORD_POLAR_NIGHT(
            false,
            R.style.WKNordPolarNightAuroraTheme,
            R.drawable.ic_arrow_up_small_dark,
            new int[] {0xFF5E81AC, 0xFFB48EAD, 0xFFA3BE8C, 0xFFA3BE8C}, // The colour of text on top of the colours on the dash and browse
            new int[] {0xFF2E3440, 0xFF2E3440, 0xFF2E3440, 0xFF2E3440}, // Sets the background colour for their highlights
            new int[] {0xFF2E3440, 0xFF2E3440, 0xFF2E3440, 0xFF2E3440}, //Sets the background colour for their buttons
            new int[] {0xFF5E81AC, 0xFFB48EAD, 0xFFA3BE8C, 0xFFA3BE8C}, // Sets the background colour for their segments on the bars
            new int[] {0xFFD8DEE9, 0xFF81A1C1, 0xFF5E81AC, 0xFFA3BE8C, 0xFFB48EAD, 0xFFD08770, 0xFFBF616A}, // SRS segment colours
            new int[] {0xFF5E81AC, 0xFF597AA3, 0xFF55749C, 0xFF516E94}, // post 60 progress bar
            new int[] {0xFFA3BE8C, 0xFFBBDAA0}, // post 60 progress bar
            new int[] {0xFF34475F, 0xFF3F5673, 0xFF455F7F, 0xFF527095, 0xFF55749C, 0xFF527096, 0xFF6486B1, 0xFF6789B3, 0xFF81A1C1, 0xFF2E3440}, //level progression chart SRS stages
            new int[] {0xFF2E3440, 0xFF8FBCBB, 0xFFA3BE8C, 0xFFBF616A, 0xFFD8DEE9, 0x2E3440}) { //Anki mode colours
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = (color == 0) ? baseSubjectTypeTextColors[i] : color;
                subjectTypeBackgroundColors[i] = baseSubjectTypeBackgroundColors[i];
                subjectTypeButtonBackgroundColors[i] = baseSubjectTypeButtonBackgroundColors[i];
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+14);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    },

    @SuppressWarnings("unused")
    BLACK_BREEZE(
            false,
            R.style.WKBlackTheme,
            R.drawable.ic_arrow_up_small_dark,
            new int[] {0xFF3DAEE9, 0xFFFDBC4B, 0xFF2ECC71, 0xFF2ECC71},
            new int[] {0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000},
            new int[] {0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000},
            new int[] {0xFF3DAEE9, 0xFFFDBC4B, 0xFF2ECC71, 0xFF2ECC71},
            new int[] {0xFF1D2023, 0xFFC1C0C1, 0xFF1D99F3, 0xFF1CDC9A, 0xFFC9CE3B, 0xFFF67400, 0xFFD53B49},
            new int[] {0xFF7DC9FC, 0xFF5DB9F9, 0xFF3DA9F6, 0xFF0D89E3},
            new int[] {0xFF20FCAA, 0xFF1CDC9A},
            new int[] {0xFF1A3A45, 0xFF0D89E3, 0xFF2599EC, 0xFF3DA9F6, 0xFF4DB1F7, 0xFF5DB9F9, 0xFF6DC1FA, 0xFF7DC9FC, 0xFFC1C0C1, 0xFF1D2023},
            new int[] {0xFF606000, 0xFF606000, 0xFF006000, 0xFF600000, 0xFFE8E8E8, 0xFF606000}) {
        @Override
        protected void loadCustomizations() {
            final List<Integer> custom = GlobalSettings.Display.getThemeCustomizations(this);
            for (int i=0; i<4; i++) {
                final int color = custom.get(i);
                subjectTypeTextColors[i] = (color == 0) ? baseSubjectTypeTextColors[i] : color;
                subjectTypeBackgroundColors[i] = baseSubjectTypeBackgroundColors[i];
                subjectTypeButtonBackgroundColors[i] = baseSubjectTypeButtonBackgroundColors[i];
            }
            for (int i=0; i<4; i++) {
                if (i < 3) {
                    final int color = custom.get(i);
                    subjectTypeBucketColors[i] = (color == 0) ? baseSubjectTypeBucketColors[i] : color;
                }
            }
            for (int i=0; i<7; i++) {
                final int color = custom.get(i+4);
                stageBucketColors7[i] = (color == 0) ? baseStageBucketColors[i] : color;
            }
            final int[] prePassed = new int[4];
            final int[] passed = new int[2];
            for (int i=0; i<4; i++) {
                final int color = custom.get(i+11);
                prePassed[i] = (color == 0) ? baseStagePrePassedBucketColors[i] : color;
            }
            for (int i=0; i<2; i++) {
                final int color = custom.get(i+15);
                passed[i] = (color == 0) ? baseStagePassedBucketColors[i] : color;
            }
            for (int i=0; i<10; i++) {
                final int color = custom.get(i+17);
                levelProgressionBucketColors[i] = (color == 0) ? baseLevelProgressionBucketColors[i] : color;
            }
            for (int i=0; i<6; i++) {
                final int color = custom.get(i+27);
                ankiColors[i] = (color == 0) ? baseAnkiColors[i] : color;
            }
            stageBucketColors4[0] = stageBucketColors7[2];
            stageBucketColors4[1] = stageBucketColors7[3];
            stageBucketColors4[2] = stageBucketColors7[4];
            stageBucketColors4[3] = stageBucketColors7[5];
            stageBucketColors5[0] = stageBucketColors7[2];
            stageBucketColors5[1] = stageBucketColors7[3];
            stageBucketColors5[2] = stageBucketColors7[4];
            stageBucketColors5[3] = stageBucketColors7[5];
            stageBucketColors5[4] = stageBucketColors7[6];
            stageDeepBucketColors[0] = stageBucketColors7[0];
            stageDeepBucketColors[1] = stageBucketColors7[1];
            stageDeepBucketColors[2] = prePassed[0];
            stageDeepBucketColors[3] = prePassed[1];
            stageDeepBucketColors[4] = prePassed[2];
            stageDeepBucketColors[5] = prePassed[3];
            stageDeepBucketColors[6] = passed[0];
            stageDeepBucketColors[7] = passed[1];
            stageDeepBucketColors[8] = stageBucketColors7[4];
            stageDeepBucketColors[9] = stageBucketColors7[5];
            stageDeepBucketColors[10] = stageBucketColors7[6];
            dirty = false;
        }
    };

    private final boolean identBackground;
    private final int styleId;
    private final int levelUpArrowDrawableId;
    protected final int[] baseSubjectTypeTextColors;
    protected final int[] baseSubjectTypeBackgroundColors;
    protected final int[] baseSubjectTypeButtonBackgroundColors;
    protected final int[] baseSubjectTypeBucketColors;
    protected final int[] subjectTypeTextColors;
    protected final int[] subjectTypeBackgroundColors;
    protected final int[] subjectTypeButtonBackgroundColors;
    protected final int[] subjectTypeBucketColors;
    protected final int[] baseStageBucketColors;
    protected final int[] baseStagePrePassedBucketColors;
    protected final int[] baseStagePassedBucketColors;
    protected final int[] stageBucketColors4;
    protected final int[] stageBucketColors5;
    protected final int[] stageBucketColors7;
    protected final int[] stageDeepBucketColors;
    protected final int[] baseLevelProgressionBucketColors;
    protected final int[] levelProgressionBucketColors;
    protected final int[] baseAnkiColors;
    protected final int[] ankiColors;

    @SuppressWarnings("NonFinalFieldInEnum")
    protected boolean dirty = true;

    ActiveTheme(final boolean identBackground, final int styleId, final int levelUpArrowDrawableId,
                final int[] subjectTypeTextColors, final int[] subjectTypeBackgroundColors, final int[] subjectTypeButtonBackgroundColors,
                final int[] subjectTypeBucketColors, final int[] stageBucketColors,
                final int[] stagePrePassedBucketColors, final int[] stagePassedBucketColors, final int[] levelProgressionBucketColors,
                final int[] ankiColors) {
        this.identBackground = identBackground;
        this.styleId = styleId;
        this.levelUpArrowDrawableId = levelUpArrowDrawableId;
        baseSubjectTypeTextColors = subjectTypeTextColors;
        baseSubjectTypeBackgroundColors = subjectTypeBackgroundColors;
        baseSubjectTypeButtonBackgroundColors = subjectTypeButtonBackgroundColors;
        baseSubjectTypeBucketColors = subjectTypeBucketColors;
        this.subjectTypeTextColors = new int[subjectTypeTextColors.length];
        this.subjectTypeBackgroundColors = new int[subjectTypeBackgroundColors.length];
        this.subjectTypeButtonBackgroundColors = new int[subjectTypeButtonBackgroundColors.length];
        this.subjectTypeBucketColors = new int[subjectTypeBucketColors.length];
        baseStageBucketColors = stageBucketColors;
        baseStagePrePassedBucketColors = stagePrePassedBucketColors;
        baseStagePassedBucketColors = stagePassedBucketColors;
        stageBucketColors7 = new int[7];
        stageBucketColors4 = new int[4];
        stageBucketColors5 = new int[5];
        stageDeepBucketColors = new int[11];
        baseLevelProgressionBucketColors = levelProgressionBucketColors;
        this.levelProgressionBucketColors = new int[10];
        baseAnkiColors = ankiColors;
        this.ankiColors = ankiColors.clone();
    }

    /**
     * The style ID for this theme.
     * @return the value
     */
    public final int getStyleId() {
        return styleId;
    }

    /**
     * Resource ID for the little up arrow in the timeline bar chart that indicates level-up items.
     * @return the value
     */
    public final int getLevelUpArrowDrawableId() {
        return levelUpArrowDrawableId;
    }

    /**
     * Does this theme use the subject type ident colours as backgrounds instead of foregrounds?.
     *
     * @return true if it does
     */
    public final boolean hasIdentBackground() {
        return identBackground;
    }

    /**
     * Indicate that the theme customizations have changed and the colors for the theme should be reloaded.
     * @param dirty the value
     */
    public final void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Get the current theme from user settings.
     *
     * @return the theme
     */
    public static ActiveTheme getCurrentTheme() {
        return GlobalSettings.Display.getTheme();
    }

    public static int[] getBaseSubjectTypeTextColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseSubjectTypeTextColors;
    }

    public static int[] getSubjectTypeTextColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.subjectTypeTextColors;
    }

    public static int[] getBaseSubjectTypeBackgroundColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseSubjectTypeBackgroundColors;
    }

    public static int[] getSubjectTypeBackgroundColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.subjectTypeBackgroundColors;
    }

    public static int[] getSubjectTypeButtonBackgroundColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.subjectTypeButtonBackgroundColors;
    }

    public static int[] getBaseSubjectTypeBucketColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseSubjectTypeBucketColors;
    }

    public static int[] getSubjectTypeBucketColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.subjectTypeBucketColors;
    }

    public static int[] getBaseShallowStageBucketColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseStageBucketColors;
    }

    public static int[] getShallowStageBucketColors4() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.stageBucketColors4;
    }

    public static int[] getShallowStageBucketColors5() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.stageBucketColors5;
    }

    public static int[] getShallowStageBucketColors7() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.stageBucketColors7;
    }

    public static int[] getBasePrePassedBucketColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseStagePrePassedBucketColors;
    }

    public static int[] getBasePassedBucketColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseStagePassedBucketColors;
    }

    public static int[] getStageDeepBucketColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.stageDeepBucketColors;
    }

    public static int[] getBaseLevelProgressionBucketColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseLevelProgressionBucketColors;
    }

    public static int[] getLevelProgressionBucketColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.levelProgressionBucketColors;
    }

    public static int[] getAnkiColors() {
        final ActiveTheme theme = getCurrentTheme();
        if (theme.dirty) {
            theme.loadCustomizations();
        }
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return theme.ankiColors;
    }

    public static int[] getBaseAnkiColors() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return getCurrentTheme().baseAnkiColors;
    }

    /**
     * Recompute the variable colours after a change to the theme customizations.
     */
    protected abstract void loadCustomizations();
}
