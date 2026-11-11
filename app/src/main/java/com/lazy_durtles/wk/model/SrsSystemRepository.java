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

import com.lazy_durtles.wk.livedata.LiveSrsSystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.lazy_durtles.wk.util.ObjectSupport.join;

/**
 * A repository of all known SRS systems. For now, just contains the classic.
 */
public final class SrsSystemRepository {
    private SrsSystemRepository() {
        //
    }

    /**
     * Get a collection of all registered systems.
     *
     * @return the collection
     */
    private static List<SrsSystem> getSystems() {
        return LiveSrsSystems.getInstance().get();
    }

    /**
     * Get the system identified by the given API ID.
     *
     * @param id the ID
     * @return the system, or the classic system if the ID is unknown
     */
    public static SrsSystem getSrsSystem(final long id) {
        for (final SrsSystem system: getSystems()) {
            if (system.getId() == id) {
                return system;
            }
        }
        return LiveSrsSystems.getInstance().getDefaultValue().get(0);
    }

    /**
     * Get the largest number of apprentice stages in any one system.
     *
     * @return the max
     */
    public static int getMaxNumApprenticeStages() {
        int count = 0;
        for (final SrsSystem system: getSystems()) {
            count = Math.max(count, system.getNumPrePassedStages());
        }
        return count;
    }

    /**
     * Get the largest number of guru stages in any one system.
     *
     * @return the max
     */
    public static int getMaxNumGuruStages() {
        int count = 0;
        for (final SrsSystem system: getSystems()) {
            count = Math.max(count, system.getNumPassedStages() - 2);
        }
        return count;
    }

    /**
     * SQL filter to find subjects that are candidates for the critical condition overview.
     *
     * @return the filter expression
     */
    public static String getCriticalConditionFilter() {
        final Collection<String> fragments = new ArrayList<>();
        for (final SrsSystem system: getSystems()) {
            fragments.add(system.getCriticalConditionFilter());
        }
        return join(" OR ", "(", ")", fragments);
    }

    /**
     * SQL filter to find subjects that are candidates for the recently burned overview.
     *
     * @return the filter expression
     */
    public static String getBurnedFilter() {
        final Collection<String> fragments = new ArrayList<>();
        for (final SrsSystem system: getSystems()) {
            fragments.add(system.getBurnedFilter());
        }
        return join(" OR ", "(", ")", fragments);
    }

    /**
     * SQL filter to find subjects that are candidates for the leeches self study filter.
     *
     * @return the filter expression
     */
    public static String getLeechFilter() {
        final Collection<String> fragments = new ArrayList<>();
        for (final SrsSystem system: getSystems()) {
            fragments.add(system.getLeechFilter());
        }
        return join(" OR ", "(", ")", fragments);
    }

    /**
     * Find SRS filter fragments for an SRS stage tag.
     *
     * @param fragments the fragments collection to add to
     * @param tag the stage tag
     */
    public static void addSrsStageFragments(final Collection<? super String> fragments, final String tag) {
        if (tag.equals("locked")) {
            fragments.add("(srsStage = -999)");
        }
        else if (tag.equals("initial")) {
            fragments.add("(srsStage = 0)");
        }
        else {
            for (final SrsSystem system: getSystems()) {
                for (final SrsSystem.Stage stage: system.getStages()) {
                    if (stage.getAdvancedSearchTag().equals(tag)) {
                        fragments.add(String.format(Locale.ROOT, "(srsSystemId = %d AND srsStage = %d)", system.getId(), stage.getId()));
                    }
                }
            }
        }
    }
}
