package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Drives unfinished {@link ScenarioRun}s from the server tick, and reports each when it is done. It listens on the FML
 * event bus only while a run is waiting. Server side.
 */
public final class ScenarioTicker {

    private final List<ScenarioRun> runs = new ArrayList<>();
    private final List<Consumer<ScenarioSummary>> reporters = new ArrayList<>();

    /** @return {@code true} while a run is still waiting for ticks */
    public boolean isBusy() {
        return !runs.isEmpty();
    }

    /**
     * Starts ticking a run that is not done yet.
     *
     * @param run      the run
     * @param reporter called with the summary once the run is done
     */
    public void track(ScenarioRun run, Consumer<ScenarioSummary> reporter) {
        if (runs.isEmpty()) {
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        }
        runs.add(run);
        reporters.add(reporter);
    }

    /**
     * @param event the server tick; the runs advance at the end of each tick
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (int i = runs.size() - 1; i >= 0; i--) {
            ScenarioRun run = runs.get(i);
            run.tick();
            if (run.isDone()) {
                runs.remove(i);
                reporters.remove(i)
                    .accept(run.summary());
            }
        }
        if (runs.isEmpty()) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
        }
    }
}
