package com.gregguns.icbm.blast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The worker threads that ray-cast blast snapshots. Daemon threads at low priority; a task that throws is caught by
 * the job that submitted it, so a failure never ends a worker for good (avoids ICBM-02-07). Workers touch only
 * {@link BlastGrid} snapshots, never the world.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (threads)
final class BlastWorkers {

    private static final int THREADS = 2;
    private static ExecutorService pool;

    private BlastWorkers() {}

    /** @param task the work; must catch its own failures */
    static synchronized void submit(Runnable task) {
        if (pool == null) {
            final AtomicInteger n = new AtomicInteger();
            ThreadFactory factory = r -> {
                Thread t = new Thread(r, "gregguns-blast-" + n.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            };
            pool = Executors.newFixedThreadPool(THREADS, factory);
        }
        pool.execute(task);
    }
}
