package com.gregguns.icbm.blast;

import java.util.List;

import com.gregguns.icbm.explosive.TickBudget;

/**
 * Shares one {@link TickBudget} between every running blast: tasks take turns, one slice each, round-robin, until the
 * budget is spent or nothing wants work. The turn order rotates each tick so no blast starves, and the first task in
 * the rotation always gets one slice so a spent budget still makes progress. Server thread only. No Minecraft types.
 *
 * <p>
 * Avoids: ICBM-02-06 (the caller runs it once per tick, at the end phase only; the budget is one number, not one per
 * phase), ICBM-02-90 (a spent budget stops the loop; there is no "one more" past the limit).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (tick budget)
public final class BlastScheduler {

    private int rotation;

    /**
     * Runs one tick's slices.
     *
     * @param tasks  the running tasks; finished ones are removed from this list
     * @param budget the opened budget for this tick
     * @return how many slices ran
     */
    public int runTick(List<? extends BlastTask> tasks, TickBudget budget) {
        int slices = 0;
        if (tasks.isEmpty()) {
            return 0;
        }
        int n = tasks.size();
        int start = Math.floorMod(rotation, n);
        rotation++;
        boolean first = true;
        boolean progressed = true;
        java.util.IdentityHashMap<BlastTask, Boolean> finished = new java.util.IdentityHashMap<>();
        while (progressed) {
            progressed = false;
            for (int i = 0; i < n; i++) {
                BlastTask t = tasks.get((start + i) % n);
                if (finished.containsKey(t) || !t.wantsWork()) {
                    continue;
                }
                if (!first && budget.exhausted()) {
                    removeFinished(tasks, finished);
                    return slices;
                }
                first = false;
                slices++;
                progressed = true;
                if (t.step()) {
                    finished.put(t, Boolean.TRUE);
                }
            }
        }
        removeFinished(tasks, finished);
        return slices;
    }

    private static void removeFinished(List<? extends BlastTask> tasks,
        java.util.IdentityHashMap<BlastTask, Boolean> done) {
        if (!done.isEmpty()) {
            tasks.removeIf(done::containsKey);
        }
    }
}
