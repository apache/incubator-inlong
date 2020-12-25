/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tubemq.agent.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Providing work threads management, those threads run
 * periodically until agent is stopped.
 */
public abstract class AbstractDaemon implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDaemon.class);

    // worker thread pool, share it
    private static final ExecutorService workerServices = Executors.newCachedThreadPool();
    private final List<CompletableFuture<?>> workerFutures;
    private boolean runnable = true;

    public AbstractDaemon() {
        this.workerFutures = new ArrayList<>();
    }

    /**
     * Whether threads can in running state with while loop.
     *
     * @return - true if threads can run
     */
    public boolean isRunnable() {
        return runnable;
    }

    /**
     * Stop running threads.
     */
    public void stopRunningThreads() {
        runnable = false;
    }

    /**
     * Submit work thread to thread pool.
     *
     * @param worker - work thread
     */
    public void submitWorker(Runnable worker) {
        CompletableFuture<?> future = CompletableFuture.runAsync(worker, workerServices);
        workerFutures.add(future);
        LOGGER.info("{} running worker number is {}", this.getClass().getName(),
                workerFutures.size());
    }

    /**
     * Wait for threads finish.
     */
    public void join() {
        for (CompletableFuture<?> future : workerFutures) {
            future.join();
        }
    }

    /**
     * Stop thread pool and running threads if they're in the running state.
     */
    public void waitForTerminate() {
        // stop running threads.
        if (isRunnable()) {
            stopRunningThreads();
        }
    }
}
