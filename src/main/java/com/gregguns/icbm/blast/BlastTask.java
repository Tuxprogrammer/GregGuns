package com.gregguns.icbm.blast;

/** One unit of server-thread work a running blast offers to the {@link BlastScheduler}. No Minecraft types. */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (tick budget)
public interface BlastTask {

    /** @return false while the task waits on something else (the worker thread), so the scheduler skips it */
    boolean wantsWork();

    /**
     * Does one small slice of work, a few dozen block edits at most.
     *
     * @return true when the task is finished and can be dropped
     */
    boolean step();

    /** @return a short name of what the next slice does, for diagnostics */
    default String label() {
        return "";
    }
}
