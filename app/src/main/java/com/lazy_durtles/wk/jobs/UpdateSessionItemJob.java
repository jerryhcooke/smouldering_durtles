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

package com.lazy_durtles.wk.jobs;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.SessionItem;
import com.lazy_durtles.wk.enums.SessionItemState;

import javax.annotation.Nullable;

/**
 * Job to update a session item in the database.
 */
public final class UpdateSessionItemJob extends Job {
    private final long subjectId;
    private final SessionItemState state;
    private final boolean question1Done;
    private final int question1Incorrect;
    private final boolean question2Done;
    private final int question2Incorrect;
    private final boolean question3Done;
    private final int question3Incorrect;
    private final boolean question4Done;
    private final int question4Incorrect;
    private final int numAnswers;
    private final long lastAnswer;

    /**
     * The constructor.
     *
     * @param data parameters
     */
    public UpdateSessionItemJob(final String data) {
        super(data);
        final String[] parts = data.split(" ");
        subjectId = Long.parseLong(parts[0]);
        state = SessionItemState.valueOf(parts[1]);
        question1Done = Boolean.parseBoolean(parts[2]);
        question1Incorrect = Integer.parseInt(parts[3]);
        question2Done = Boolean.parseBoolean(parts[4]);
        question2Incorrect = Integer.parseInt(parts[5]);
        question3Done = Boolean.parseBoolean(parts[6]);
        question3Incorrect = Integer.parseInt(parts[7]);
        question4Done = Boolean.parseBoolean(parts[8]);
        question4Incorrect = Integer.parseInt(parts[9]);
        numAnswers = Integer.parseInt(parts[10]);
        lastAnswer = Long.parseLong(parts[11]);
    }

    @Override
    public void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final @Nullable SessionItem item = db.sessionItemDao().getById(subjectId);
        if (item != null) {
            item.setState(state);
            item.setQuestion1Done(question1Done);
            item.setQuestion1Incorrect(question1Incorrect);
            item.setQuestion2Done(question2Done);
            item.setQuestion2Incorrect(question2Incorrect);
            item.setQuestion3Done(question3Done);
            item.setQuestion3Incorrect(question3Incorrect);
            item.setQuestion4Done(question4Done);
            item.setQuestion4Incorrect(question4Incorrect);
            item.setNumAnswers(numAnswers);
            item.setLastAnswer(lastAnswer);
            db.sessionItemDao().update(item);
        }
        houseKeeping();
    }
}
