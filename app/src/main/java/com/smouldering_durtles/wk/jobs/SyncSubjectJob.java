package com.smouldering_durtles.wk.jobs;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;

public class SyncSubjectJob extends Job {
    /**
     * The constructor.
     *
     * @param data parameters for this job, encoded in a string in a class-specific format
     */
    public SyncSubjectJob(String data) {
        super(data);
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();

        db.assertGetSubjectTask(data);
        houseKeeping();
    }
}
