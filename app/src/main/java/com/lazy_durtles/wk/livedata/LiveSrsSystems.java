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

package com.lazy_durtles.wk.livedata;

import com.lazy_durtles.wk.WkApplication;
import com.lazy_durtles.wk.api.model.ApiStage;
import com.lazy_durtles.wk.db.AppDatabase;
import com.lazy_durtles.wk.db.model.SrsSystemDefinition;
import com.lazy_durtles.wk.model.SrsSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lazy_durtles.wk.Constants.DAY;
import static com.lazy_durtles.wk.Constants.HOUR;
import static com.lazy_durtles.wk.Constants.MINUTE;
import static com.lazy_durtles.wk.Constants.SECOND;
import static com.lazy_durtles.wk.Constants.WEEK;

/**
 * LiveData that tracks the (mostly static) SRS system definitions. This mostly exists
 * to make it easy for the UI thread to get at this data without having to kick off a
 * background thread for database access.
 */
public final class LiveSrsSystems extends ConservativeLiveData<List<SrsSystem>> {
    private static final List<SrsSystem> fallback;
    private static final SrsSystem CLASSIC = new SrsSystem(1, "Classic",
            0, 1, 5, 9);
    private static final SrsSystem CLASSIC_ACCELERATED = new SrsSystem(2, "Classic accelerated",
            0, 1, 5, 9);

    static {
        final List<SrsSystem> list = new ArrayList<>();
        CLASSIC.addStage(0, 0);
        CLASSIC.addStage(1, 14400000);
        CLASSIC.addStage(2, 28800000);
        CLASSIC.addStage(3, 82800000);
        CLASSIC.addStage(4, 169200000);
        CLASSIC.addStage(5, 601200000);
        CLASSIC.addStage(6, 1206000000);
        CLASSIC.addStage(7, 2588400000L);
        CLASSIC.addStage(8, 10364400000L);
        CLASSIC.addStage(9, 0);
        CLASSIC.finish();
        list.add(CLASSIC);
        CLASSIC_ACCELERATED.addStage(0, 0);
        CLASSIC_ACCELERATED.addStage(1, 7200000);
        CLASSIC_ACCELERATED.addStage(2, 14400000);
        CLASSIC_ACCELERATED.addStage(3, 28800000);
        CLASSIC_ACCELERATED.addStage(4, 82800000);
        CLASSIC_ACCELERATED.addStage(5, 601200000);
        CLASSIC_ACCELERATED.addStage(6, 1206000000);
        CLASSIC_ACCELERATED.addStage(7, 2588400000L);
        CLASSIC_ACCELERATED.addStage(8, 10364400000L);
        CLASSIC_ACCELERATED.addStage(9, 0);
        CLASSIC_ACCELERATED.finish();
        list.add(CLASSIC_ACCELERATED);
        fallback = Collections.unmodifiableList(list);
    }

    /**
     * The singleton instance.
     */
    private static final LiveSrsSystems instance = new LiveSrsSystems();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static LiveSrsSystems getInstance() {
        return instance;
    }

    /**
     * Private constructor.
     */
    private LiveSrsSystems() {
        //
    }

    @Override
    protected void updateLocal() {
        final AppDatabase db = WkApplication.getDatabase();
        final List<SrsSystemDefinition> definitions = db.srsSystemDao().getAll();
        if (definitions.isEmpty()) {
            instance.postValue(fallback);
            return;
        }
        final List<SrsSystem> systems = new ArrayList<>();
        for (final SrsSystemDefinition definition: definitions) {
            final String name = definition.name == null ? Long.toString(definition.id) : definition.name;
            final SrsSystem system = new SrsSystem(definition.id, name,
                    definition.unlockingStagePosition, definition.startingStagePosition, definition.passingStagePosition, definition.burningStagePosition);
            for (final ApiStage apiStage: definition.getParsedStages()) {
                final long unit;
                if (apiStage.intervalUnit == null) {
                    unit = SECOND;
                }
                else if (apiStage.intervalUnit.equals("milliseconds")) {
                    unit = 1;
                }
                else if (apiStage.intervalUnit.equals("seconds")) {
                    unit = SECOND;
                }
                else if (apiStage.intervalUnit.equals("minutes")) {
                    unit = MINUTE;
                }
                else if (apiStage.intervalUnit.equals("hours")) {
                    unit = HOUR;
                }
                else if (apiStage.intervalUnit.equals("days")) {
                    unit = DAY;
                }
                else if (apiStage.intervalUnit.equals("weeks")) {
                    unit = WEEK;
                }
                else {
                    unit = SECOND;
                }
                system.addStage(apiStage.position, apiStage.interval * unit);
            }
            system.finish();
            systems.add(system);
        }
        instance.postValue(systems);
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public List<SrsSystem> getDefaultValue() {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return fallback;
    }
}
