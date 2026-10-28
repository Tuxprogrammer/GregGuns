package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.gregguns.GregGuns;

/**
 * One run of a list of scenarios that may span several server ticks. Scenarios run one after another (they share the
 * origin area), each with its own context, which is cleaned up as soon as that scenario finishes. A scenario that
 * returns {@link ScenarioResult#pending} is polled once per call to {@link #tick()} until it answers or times out.
 * Scenarios that finish at once run straight through, so an all-synchronous list is done when
 * {@link ScenarioRunner#begin} returns. Plain Java; not thread-safe, drive it from the server thread.
 */
public final class ScenarioRun {

    private final Function<Scenario, ScenarioContext> contexts;
    private final List<Scenario> queue;
    private final List<ScenarioResult> results = new ArrayList<>();
    private int next;
    private Scenario current;
    private ScenarioContext currentContext;
    private ConfigSnapshot configBefore;
    private ScenarioResult pending;
    private int elapsed;

    ScenarioRun(Function<Scenario, ScenarioContext> contexts, List<Scenario> scenarios) {
        this.contexts = contexts;
        this.queue = new ArrayList<>(scenarios);
        startNext();
    }

    /** @return {@code true} once every scenario has a result */
    public boolean isDone() {
        return pending == null && next >= queue.size();
    }

    /**
     * Advances a waiting scenario by one tick. Call once per server tick; does nothing when the run is done.
     */
    public void tick() {
        if (pending == null) {
            return;
        }
        elapsed++;
        ScenarioResult answer;
        try {
            answer = pending.poller()
                .poll(elapsed);
        } catch (RuntimeException | AssertionError e) {
            GregGuns.LOG.error("Scenario {} threw while waiting", current.name(), e);
            answer = ScenarioResult.fail(current.name(), "threw " + e);
        }
        if (answer == null) {
            if (elapsed < pending.timeoutTicks()) {
                return;
            }
            answer = ScenarioResult.fail(current.name(), "timed out after " + pending.timeoutTicks() + " ticks");
        } else if (answer.isPending()) {
            answer = ScenarioResult
                .fail(current.name(), "a poller must return a final result, not another pending one");
        }
        finishCurrent(answer);
        startNext();
    }

    /**
     * Fails the waiting scenario at once, with the given reason, and moves on. Used when nothing will tick the run.
     *
     * @param reason why it was abandoned
     */
    public void abandonPending(String reason) {
        if (pending == null) {
            return;
        }
        finishCurrent(ScenarioResult.fail(current.name(), reason));
        startNext();
    }

    /** @return the results so far, in run order; complete once {@link #isDone()} */
    public ScenarioSummary summary() {
        return new ScenarioSummary(results);
    }

    private void startNext() {
        while (pending == null && next < queue.size()) {
            current = queue.get(next++);
            elapsed = 0;
            currentContext = null;
            configBefore = ConfigSnapshot.ofAll();
            ScenarioResult result;
            try {
                currentContext = contexts.apply(current);
                logFreeTickets(current.name());
                result = current.run(currentContext);
                if (result == null) {
                    result = ScenarioResult.fail(current.name(), "scenario returned no result");
                }
            } catch (RuntimeException | AssertionError e) {
                GregGuns.LOG.error("Scenario {} threw", current.name(), e);
                result = ScenarioResult.fail(current.name(), "threw " + e);
            }
            if (result.isPending()) {
                pending = result;
            } else {
                finishCurrent(result);
            }
        }
    }

    /** Logs how many chunk tickets are left, so a leaking scenario shows up as a steady drop (see ScenarioChunks). */
    private static void logFreeTickets(String scenario) {
        net.minecraft.world.World w = net.minecraftforge.common.DimensionManager.getWorld(0);
        if (w != null && !w.isRemote) {
            GregGuns.LOG.info("tickets free before {}: {}", scenario, ScenarioChunks.freeTickets(w));
        }
    }

    private void finishCurrent(ScenarioResult result) {
        ScenarioResult finished = result;
        if (currentContext != null) {
            try {
                currentContext.cleanup();
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Cleanup after scenario {} threw", current.name(), e);
                finished = ScenarioResult.fail(current.name(), "cleanup threw " + e);
            }
        }
        currentContext = null;
        pending = null;
        if (configBefore != null) {
            // A scenario must not leak a config change into the next one (CONTRIBUTING: restore every global switch).
            java.util.List<String> leaked = configBefore.restore();
            if (!leaked.isEmpty()) {
                GregGuns.LOG.warn("Scenario {} left config changed, restored: {}", current.name(), leaked);
            }
            configBefore = null;
        }
        results.add(finished);
    }
}
