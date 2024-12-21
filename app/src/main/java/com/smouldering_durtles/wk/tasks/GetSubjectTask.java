package com.smouldering_durtles.wk.tasks;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.ApiState;
import com.smouldering_durtles.wk.api.model.ApiSubject;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.livedata.LiveApiState;


import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

public class GetSubjectTask extends ApiTask {
    /**
     * Task priority.
     */
    public static final int PRIORITY = 18;

    /**
     * The constructor.
     *
     * @param taskDefinition the definition of this task in the database
     */
    public GetSubjectTask(TaskDefinition taskDefinition) {
        super(taskDefinition);
    }

    @Override
    public boolean canRun() {
        return WkApplication.getInstance().getOnlineStatus().canCallApi() && ApiState.getCurrentApiState() == ApiState.OK;
    }

    @Override
    protected void runLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final String data = Objects.requireNonNull(taskDefinition.getData());
        final @Nullable ApiSubject apiSubject = singleEntityApiCall(String.format("/v2/subjects/%s", data), ApiSubject.class);

        final Set<Long> existingSubjectIds = db.subjectViewsDao().getAllSubjectIds();
        if (apiSubject != null) {
            db.subjectSyncDao().insertOrUpdate(apiSubject, existingSubjectIds);
        }

        db.propertiesDao().setLastApiSuccessDate(System.currentTimeMillis());
        db.taskDefinitionDao().deleteTaskDefinition(taskDefinition);
        LiveApiState.getInstance().forceUpdate();
    }
}
