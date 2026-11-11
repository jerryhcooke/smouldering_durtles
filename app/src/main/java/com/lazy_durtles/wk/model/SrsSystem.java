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

package com.lazy_durtles.wk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.lazy_durtles.wk.util.TextUtil.getRomanNumeral;

/**
 * An class to represent the logic of the SRS system and its stages.
 */
public final class SrsSystem implements Comparable<SrsSystem> {
    private final long id;
    private final String name;
    private final List<Stage> stages;
    private final long initialStageId;
    private final long startingStageId;
    private final long passedStageId;
    private final long completedStageId;
    private long enlightenedStageId = -1;
    private long masterStageId = -1;
    private int numPrePassedStages = 0;
    private int numPassedStages = 0;

    /**
     * The constructor.
     *
     * @param id the API ID
     * @param name the system's name
     * @param initialStageId the ID of the initial stage of unlocked items (lesson not completed yet)
     * @param startingStageId the ID of the first normal stage after completing the lesson
     * @param passedStageId the ID of the first passed stage
     * @param completedStageId the ID of the final completed (burned) stage
     */
    public SrsSystem(final long id, final String name,
                     final long initialStageId, final long startingStageId, final long passedStageId, final long completedStageId) {
        this.id = id;
        this.name = name;
        this.initialStageId = initialStageId;
        this.startingStageId = startingStageId;
        this.passedStageId = passedStageId;
        this.completedStageId = completedStageId;
        stages = new ArrayList<>();
        stages.add(new Stage(-999, 0));
    }

    /**
     * The API ID of this system.
     *
     * @return the ID
     */
    public long getId() {
        return id;
    }

    /**
     * The name of this system.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the number of pre-passed stages excluding locked and initiate.
     *
     * @return the number
     */
    public int getNumPrePassedStages() {
        return numPrePassedStages;
    }

    /**
     * Get the number of passed stages excluding burned.
     *
     * @return the number
     */
    public int getNumPassedStages() {
        return numPassedStages;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SrsSystem other = (SrsSystem) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public int compareTo(final @Nullable SrsSystem o) {
        if (o == null) {
            return 1;
        }
        return Long.compare(id, o.id);
    }

    /**
     * A sorted list of the SRS stages in this system.
     *
     * @return the stages
     */
    public List<Stage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /**
     * Get a stage indexed by its API ID.
     *
     * @param stageId the stage's ID
     * @return the stage, possibly the first stage as a fallback if it's not known
     */
    public Stage getStage(final long stageId) {
        for (final Stage stage: stages) {
            if (stage.id == stageId) {
                return stage;
            }
        }
        if (stages.isEmpty()) {
            return new Stage(0, 0);
        }
        return stages.get(0);
    }

    /**
     * Add a stage to this system. Only used during construction from the database.
     * @param stageId the stage ID
     * @param interval the interval in ms
     */
    public void addStage(final long stageId, final long interval) {
        stages.add(new Stage(stageId, interval));
    }

    /**
     * After adding all stages, finish up the properties on the system and the stages.
     */
    public void finish() {
        Collections.sort(stages, Comparator.comparingLong(o -> o.id));
        numPassedStages = 0;
        numPrePassedStages = 0;
        for (final Stage stage: stages) {
            if (stage.isLocked()) {
                stage.name = "Locked";
                stage.shortName = "";
                stage.nameLetter = "L";
                stage.advancedSearchTag = "locked";
                stage.passedIndex = -1;
                stage.prePassedIndex = -1;
            }
            else if (stage.isInitial()) {
                stage.name = "Initiate";
                stage.shortName = stage.name;
                stage.nameLetter = "I";
                stage.advancedSearchTag = "initial";
                stage.passedIndex = -1;
                stage.prePassedIndex = -1;
            }
            else if (stage.isCompleted()) {
                stage.name = "Burned";
                stage.shortName = "Burned";
                stage.nameLetter = "B";
                stage.advancedSearchTag = "burned";
                stage.passedIndex = -1;
                stage.prePassedIndex = -1;
            }
            else if (stage.isPassed()) {
                stage.name = "Guru " + getRomanNumeral(numPassedStages + 1);
                stage.shortName = stage.name;
                stage.nameLetter = "G";
                stage.advancedSearchTag = "pass:" + numPassedStages;
                stage.passedIndex = numPassedStages;
                stage.prePassedIndex = -1;
                numPassedStages++;
            }
            else {
                stage.name = "Apprentice " + getRomanNumeral(numPrePassedStages + 1);
                stage.shortName = "Appr " + getRomanNumeral(numPrePassedStages + 1);
                stage.nameLetter = "A";
                stage.advancedSearchTag = "prepass:" + numPrePassedStages;
                stage.passedIndex = -1;
                stage.prePassedIndex = numPrePassedStages;
                numPrePassedStages++;
            }
        }
        boolean enlightenedFound = false;
        boolean masterFound = false;
        for (int i=stages.size()-1; i>=0; i--) {
            final Stage stage = stages.get(i);
            if (stage.isCompleted() || !stage.isPassed()) {
                continue;
            }
            if (!enlightenedFound) {
                enlightenedFound = true;
                stage.name = "Enlightened";
                stage.shortName = "Enl";
                stage.nameLetter = "E";
                stage.advancedSearchTag = "enlightened";
                enlightenedStageId = stage.id;
            }
            else if (!masterFound) {
                masterFound = true;
                stage.name = "Master";
                stage.shortName = stage.name;
                stage.nameLetter = "M";
                stage.advancedSearchTag = "master";
                masterStageId = stage.id;
            }
        }
        for (final Stage stage: stages) {
            stage.levelProgressBucket = stage.findLevelProgressBucket();
            stage.post60DeepBucket = stage.findPost60DeepBucket();
            stage.post60ShallowBucket = stage.findPost60ShallowBucket();
            stage.srsBreakdownBucket = stage.findSrsBreakdownBucket();
            stage.timeLineBarChartBucket = stage.findTimeLineBarChartBucket();
            stage.generalStageBucket = stage.findGeneralStageBucket();
        }
    }

    /**
     * The locked stage for this system.
     *
     * @return the stage
     */
    public Stage getLockedStage() {
        return getStage(-999);
    }

    /**
     * Get the initial stage for this system.
     *
     * @return the stage
     */
    public Stage getInitialStage() {
        return getStage(initialStageId);
    }

    /**
     * Get the first started (i.e. non-initial) stage for this system.
     *
     * @return the stage
     */
    public Stage getFirstStartedStage() {
        return getStage(startingStageId);
    }

    /**
     * Get the completed (i.e. burned) stage for this system.
     *
     * @return the stage
     */
    public Stage getCompletedStage() {
        return getStage(completedStageId);
    }

    /**
     * A filter fragment for the critical condition view.
     *
     * @return the filter
     */
    public String getCriticalConditionFilter() {
        return String.format(Locale.ROOT, "(srsSystemId = %d AND srsStage >= %d AND srsStage < %d)",
                id, startingStageId, passedStageId);
    }

    /**
     * A filter fragment for the recently burned view.
     *
     * @return the filter
     */
    public String getBurnedFilter() {
        return String.format(Locale.ROOT, "(srsSystemId = %d AND srsStage = %d)",
                id, completedStageId);
    }

    /**
     * A filter fragment for the leeches self-study filter.
     *
     * @return the filter
     */
    public String getLeechFilter() {
        return String.format(Locale.ROOT, "(srsSystemId = %d AND srsStage > %d AND srsStage < %d)",
                id, initialStageId, completedStageId);
    }

    /**
     * Interface for an individual stage within the system.
     */
    public final class Stage implements Comparable<Stage> {
        @SuppressWarnings("InnerClassFieldHidesOuterClassField")
        private final long id;
        private final long interval;
        @SuppressWarnings("InnerClassFieldHidesOuterClassField")
        private String name = "";
        private String shortName = "";
        private String nameLetter = "";
        private String advancedSearchTag = "";
        private int passedIndex = 0;
        private int prePassedIndex = 0;
        private int levelProgressBucket = 0;
        private int post60DeepBucket = 0;
        private int post60ShallowBucket = 0;
        private int srsBreakdownBucket = 0;
        private int timeLineBarChartBucket = 0;
        private int generalStageBucket = 0;

        /**
         * The constructor.
         *
         * @param id the ID (position) of this stage
         * @param interval the interval in ms
         */
        private Stage(final long id, final long interval) {
            this.id = id;
            this.interval = interval;
        }

        /**
         * The SRS system this stage belongs to.
         *
         * @return the system
         */
        public SrsSystem getSystem() {
            return SrsSystem.this;
        }

        /**
         * The ID (position) of this stage.
         *
         * @return the id
         */
        public long getId() {
            return id;
        }

        /**
         * The stage's interval until the next review.
         *
         * @return the interval in ms
         */
        public long getInterval() {
            return interval;
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final Stage stage = (Stage) obj;
            return getSystem().equals(stage.getSystem()) && id == stage.id;
        }

        @Override
        public int hashCode() {
            return getSystem().hashCode() + (int) id;
        }

        @Override
        public int compareTo(final Stage o) {
            if (isLocked()) {
                return o.isLocked() ? 0 : -1;
            }
            if (o.isLocked()) {
                return 1;
            }
            if (isInitial()) {
                return o.isInitial() ? 0 : -1;
            }
            if (o.isInitial()) {
                return 1;
            }
            if (isCompleted()) {
                return o.isCompleted() ? 0 : 1;
            }
            if (o.isCompleted()) {
                return -1;
            }
            if (isEnlightened()) {
                return o.isEnlightened() ? 0 : 1;
            }
            if (o.isEnlightened()) {
                return -1;
            }
            if (isMaster()) {
                return o.isMaster() ? 0 : 1;
            }
            if (o.isMaster()) {
                return -1;
            }
            // All the special cases are gone, only the various apprentice and guru stages remain
            if (isPassed()) {
                if (!o.isPassed()) {
                    return 1;
                }
                return Integer.compare(passedIndex, o.passedIndex);
            }
            if (o.isPassed()) {
                return -1;
            }
            return Integer.compare(prePassedIndex, o.prePassedIndex);
        }

        /**
         * Is this the locked stage.
         * @return true if it is
         */
        public boolean isLocked() {
            return id == -999;
        }

        /**
         * Is this the initial stage (unlocked, lesson not completed) for the system.
         * @return true if it is
         */
        public boolean isInitial() {
            return id == initialStageId;
        }

        /**
         * Is this the final burned stage for the system.
         * @return true if it is
         */
        public boolean isCompleted() {
            return id == completedStageId;
        }

        /**
         * Is this a passed stage for the system.
         * @return true if it is
         */
        public boolean isPassed() {
            return id >= passedStageId;
        }

        private boolean isEnlightened() {
            return id == enlightenedStageId;
        }

        private boolean isMaster() {
            return id == masterStageId;
        }

        /**
         * The stage's name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * The stage's short name.
         *
         * @return the name
         */
        public String getShortName() {
            return shortName;
        }

        /**
         * A single letter to identify the stage.
         *
         * @return the letter
         */
        public String getNameLetter() {
            return nameLetter;
        }

        /**
         * Get the next stage for a subject, assuming that a review is entered with the given number of incorrect answers.
         *
         * @param numIncorrect the number of incorrect answers
         * @return the new stage
         */
        public Stage getNewStage(final int numIncorrect) {
            int index = stages.indexOf(this);
            if (numIncorrect == 0) {
                index++;
            }
            else {
                index -= (numIncorrect+1) / 2;
                if (isPassed()) {
                    index -= (numIncorrect+1) / 2;
                }
            }
            final int startingIndex = stages.indexOf(getFirstStartedStage());
            if (index < startingIndex) {
                return getFirstStartedStage();
            }
            final int burnedIndex = stages.indexOf(getCompletedStage());
            if (index > burnedIndex) {
                return getCompletedStage();
            }
            return stages.get(index);
        }

        /**
         * Get the bucket this stage belongs to in the level progress chart.
         * @return the bucket 0..8
         */
        public int getLevelProgressBucket() {
            return levelProgressBucket;
        }

        private int findLevelProgressBucket() {
            if (isLocked()) {
                return 9;
            }
            if (isInitial()) {
                return 8;
            }
            if (isPassed()) {
                return 0;
            }
            switch (numPrePassedStages) {
                case 0:
                case 1:
                    return 1;
                case 2:
                    return prePassedIndex == 0 ? 7 : 1;
                case 3:
                    switch (prePassedIndex) {
                        case 0:
                            return 7;
                        case 1:
                            return 4;
                        case 2:
                        default:
                            return 1;
                    }
                case 4:
                    switch (prePassedIndex) {
                        case 0:
                            return 7;
                        case 1:
                            return 5;
                        case 2:
                            return 3;
                        case 3:
                        default:
                            return 1;
                    }
                case 5:
                    switch (prePassedIndex) {
                        case 0:
                            return 7;
                        case 1:
                            return 5;
                        case 2:
                            return 3;
                        case 3:
                            return 2;
                        case 4:
                        default:
                            return 1;
                    }
                case 6:
                    switch (prePassedIndex) {
                        case 0:
                            return 7;
                        case 1:
                            return 5;
                        case 2:
                            return 4;
                        case 3:
                            return 3;
                        case 4:
                            return 2;
                        case 5:
                        default:
                            return 1;
                    }
                default:
                    switch (prePassedIndex) {
                        case 0:
                            return 7;
                        case 1:
                            return 6;
                        case 2:
                            return 5;
                        case 3:
                            return 4;
                        case 4:
                            return 3;
                        case 5:
                            return 2;
                        case 6:
                        default:
                            return 1;
                    }
            }
        }

        /**
         * Get the bucket this stage belongs to in the post 60 progress bar.
         * This is for the version that breaks down the apprentice and guru buckets.
         * @return the bucket 1..10
         */
        public int getPost60DeepBucket() {
            return post60DeepBucket;
        }

        private int findPost60DeepBucket() {
            if (isLocked()) {
                return 0;
            }
            if (isInitial()) {
                return 1;
            }
            if (isCompleted()) {
                return 10;
            }
            if (isEnlightened()) {
                return 9;
            }
            if (isMaster()) {
                return 8;
            }
            if (isPassed()) {
                int index = passedIndex;
                if (index >= 2) {
                    index = 1;
                }
                return index + 6;
            }
            int index = prePassedIndex;
            if (index >= 4) {
                index = 3;
            }
            return index + 2;
        }

        /**
         * Get the bucket this stage belongs to in the post 60 progress bar.
         * This is for the version that doesn't break down the apprentice and guru buckets.
         * @return the bucket 1..6
         */
        public int getPost60ShallowBucket() {
            return post60ShallowBucket;
        }

        private int findPost60ShallowBucket() {
            if (isLocked()) {
                return 0;
            }
            if (isInitial()) {
                return 1;
            }
            if (isCompleted()) {
                return 6;
            }
            if (isEnlightened()) {
                return 5;
            }
            if (isMaster()) {
                return 4;
            }
            if (isPassed()) {
                return 3;
            }
            return 2;
        }

        /**
         * Get the bucket this stage belongs to in the SRS breakdown.
         * @return the bucket 0..4
         */
        public int getSrsBreakdownBucket() {
            return srsBreakdownBucket;
        }

        private int findSrsBreakdownBucket() {
            if (isLocked()) {
                return -1;
            }
            if (isInitial()) {
                return 0;
            }
            if (isCompleted()) {
                return 4;
            }
            if (isEnlightened()) {
                return 3;
            }
            if (isMaster()) {
                return 2;
            }
            if (isPassed()) {
                return 1;
            }
            return 0;
        }

        /**
         * Get the bucket this stage belongs to in the timeline bar chart.
         * @return the bucket 0..4
         */
        public int getTimeLineBarChartBucket() {
            return timeLineBarChartBucket;
        }

        private int findTimeLineBarChartBucket() {
            if (isLocked()) {
                return 0;
            }
            if (isInitial()) {
                return 0;
            }
            if (isCompleted()) {
                return 4;
            }
            if (isEnlightened()) {
                return 3;
            }
            if (isMaster()) {
                return 2;
            }
            if (isPassed()) {
                return 1;
            }
            return 0;
        }

        /**
         * Get the bucket this stage belongs to in a general shallow SRS progression.
         * @return the bucket 0..6
         */
        public int getGeneralStageBucket() {
            return generalStageBucket;
        }

        private int findGeneralStageBucket() {
            if (isLocked()) {
                return 0;
            }
            if (isInitial()) {
                return 1;
            }
            if (isCompleted()) {
                return 6;
            }
            if (isEnlightened()) {
                return 5;
            }
            if (isMaster()) {
                return 4;
            }
            if (isPassed()) {
                return 3;
            }
            return 2;
        }

        /**
         * Get the tag of this stage for use in advanced search.
         *
         * @return the tag
         */
        public String getAdvancedSearchTag() {
            return advancedSearchTag;
        }
    }
}
