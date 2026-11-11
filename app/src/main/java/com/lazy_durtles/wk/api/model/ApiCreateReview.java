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

package com.lazy_durtles.wk.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lazy_durtles.wk.components.WaniKaniApiDateDeserializer;
import com.lazy_durtles.wk.components.WaniKaniApiDateSerializer;

/**
 * Model class used in the API to create a new review record.
 */
@SuppressWarnings("unused")
public final class ApiCreateReview {
    private CreateReviewBody review = new CreateReviewBody();

    /**
     * The body to create.
     * @return the value
     */
    public CreateReviewBody getReview() {
        return review;
    }

    /**
     * The body to create.
     * @param review the value
     */
    public void setReview(final CreateReviewBody review) {
        this.review = review;
    }

    /**
     * Model class of the actual review record to create.
     */
    @SuppressWarnings("unused")
    public static final class CreateReviewBody {
        @JsonProperty("subject_id") private long subjectId = 0;
        @JsonProperty("incorrect_meaning_answers") private int incorrectMeaningAnswers = 0;
        @JsonProperty("incorrect_reading_answers") private int incorrectReadingAnswers = 0;
        @JsonSerialize(using = WaniKaniApiDateSerializer.class)
        @JsonDeserialize(using = WaniKaniApiDateDeserializer.class)
        @JsonProperty("created_at") private long createdAt = 0;

        /**
         * The ID of the subject this review is for.
         * @return the value
         */
        public long getSubjectId() {
            return subjectId;
        }

        /**
         * The ID of the subject this review is for.
         * @param subjectId the value
         */
        public void setSubjectId(final long subjectId) {
            this.subjectId = subjectId;
        }

        /**
         * The number of incorrect meaning answers for this review.
         * @return the value
         */
        public int getIncorrectMeaningAnswers() {
            return incorrectMeaningAnswers;
        }

        /**
         * The number of incorrect meaning answers for this review.
         * @param incorrectMeaningAnswers the value
         */
        public void setIncorrectMeaningAnswers(final int incorrectMeaningAnswers) {
            this.incorrectMeaningAnswers = incorrectMeaningAnswers;
        }

        /**
         * The number of incorrect reading answers for this review.
         * @return the value
         */
        public int getIncorrectReadingAnswers() {
            return incorrectReadingAnswers;
        }

        /**
         * The number of incorrect reading answers for this review.
         * @param incorrectReadingAnswers the value
         */
        public void setIncorrectReadingAnswers(final int incorrectReadingAnswers) {
            this.incorrectReadingAnswers = incorrectReadingAnswers;
        }

        /**
         * The timestamp when the review was created, or 0 to request that the API set the current time as timestamp.
         * @return the value
         */
        public long getCreatedAt() {
            return createdAt;
        }

        /**
         * The timestamp when the review was created, or 0 to request that the API set the current time as timestamp.
         * @param createdAt the value
         */
        public void setCreatedAt(final long createdAt) {
            this.createdAt = createdAt;
        }
    }
}
