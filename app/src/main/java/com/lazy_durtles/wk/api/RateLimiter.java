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

package com.lazy_durtles.wk.api;

/**
 * A simple burst-capable rate limiter for the WaniKani API. To be a good API
 * citizen, the request rate is set to an average of one call per 1.1 seconds,
 * with a burst of 1.
 */
public final class RateLimiter {
    private static final RateLimiter instance = new RateLimiter();
    private static final int BURST = 1;
    private final long delay;
    private int load = 0;
    private long lastUpdate = System.currentTimeMillis();

    /**
     * Get the singleton instance.
     *
     * @return the instance
     */
    public static RateLimiter getInstance() {
        return instance;
    }

    private RateLimiter() {
        delay = 1100;
    }

    /**
     * If the API reports an HTTP 429 indicating we're talking too fast, back off
     * for a moment by simulating we've gone over the burst by a few requests.
     * This forces the app to back off for a few seconds before trying again.
     */
    public void pause() {
        load = BURST + 3;
    }

    /**
     * Prepare for an API call by waiting for a burst slot to become available,
     * if necessary.
     */
    public void prepare() {
        while (true) {
            if (load < BURST) {
                if (load == 0) {
                    lastUpdate = System.currentTimeMillis();
                }
                load++;
                return;
            }
            try {
                final long waitTime = Math.min(delay - (System.currentTimeMillis() - lastUpdate), delay);
                if (waitTime > 0) {
                    //noinspection BusyWait
                    Thread.sleep(waitTime);
                }
            } catch (final InterruptedException e) {
                //
            }
            while (load > 0 && lastUpdate + delay <= System.currentTimeMillis()) {
                load--;
                lastUpdate += delay;
            }
        }
    }
}
