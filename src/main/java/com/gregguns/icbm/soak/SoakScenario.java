package com.gregguns.icbm.soak;

import java.util.List;
import java.util.Locale;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.gregguns.GregGuns;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;

/**
 * One ICBM soak run (I7): set up, let the server settle, take a baseline, fire a burst, follow it until it ends, then
 * take a recovery window and judge the owner's bar ({@link SoakVerdict}). Every second of the burst logs a
 * {@code SOAK} line (the MSPT and TPS curve, with a {@link SoakProbe} sample), and the end logs one
 * {@code SOAKRESULT} line the driver ({@code tools/soak/icbm_soak.py}) turns into the result table. Opt-in: never in
 * {@code gregguns test all}. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public abstract class SoakScenario implements Scenario {

    /** Ticks after setup before the baseline starts, so the generated chunks have lit and saved. */
    static final int SETTLE_TICKS = 200;
    /** Ticks in the baseline. */
    static final int BASELINE_TICKS = 600;
    /** Ticks in the recovery window, right after the burst ends. */
    static final int RECOVERY_TICKS = 600;
    /** Ticks the setup may take. */
    static final int SETUP_LIMIT = 6000;
    /** Buckets of the curve printed in the result note. */
    private static final int NOTE_BUCKETS = 40;
    /** Length of {@code MinecraftServer.tickTimeArray}. */
    private static final int TICK_ARRAY = 100;
    /** Overrides {@link SoakVerdict#RECOVERY_MARGIN_MS}. */
    static final String MARGIN_PROPERTY = "gregguns.soak.recoveryMarginMs";

    /** What a soak sets off, and how it knows it is over. */
    interface Burst {

        /**
         * Builds what the burst needs, a little each tick.
         *
         * @param tick 1 on the first setup tick
         * @return true when ready
         */
        boolean prepare(int tick);

        /** Sets everything off, in one tick. */
        void fire(ScenarioResult.Builder out);

        /** @return true once everything the burst started has ended */
        boolean done();

        /** @return a short state line for the per-second log */
        String status();

        /** The burst ran past its limit: end what is left of it. */
        void stop();

        /**
         * Adds the scenario's own checks and notes.
         *
         * @param out    the result
         * @param before the sample taken right before the burst
         * @param after  the sample taken after the recovery
         * @param peak   the largest value of each field seen during the burst
         */
        void check(ScenarioResult.Builder out, SoakProbe before, SoakProbe after, SoakProbe peak);

        /** Removes what the soak built and releases its chunk holds. Idempotent. */
        void cleanup();
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    /** @return ticks the burst may run before it counts as a runaway */
    abstract int burstLimit();

    /**
     * @param ctx the scenario's context
     * @return the burst
     */
    abstract Burst plan(ScenarioContext ctx);

    @Override
    public final ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return ScenarioResult.skip(name(), "the ICBM module is off");
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || !(ctx.world() instanceof WorldServer)) {
            return ScenarioResult.fail(name(), "needs a running server world");
        }
        final Burst burst = plan(ctx);
        final Run run = new Run(this, ctx, server, burst);
        int timeout = SETUP_LIMIT + SETTLE_TICKS + BASELINE_TICKS + burstLimit() + RECOVERY_TICKS + 400;
        return ScenarioResult.pending(name(), timeout, ticks -> {
            try {
                return run.step();
            } catch (RuntimeException | AssertionError e) {
                burst.stop();
                burst.cleanup();
                throw e;
            }
        });
    }

    private enum Phase {
        SETUP,
        SETTLE,
        BASELINE,
        BURST,
        RECOVER
    }

    /** The state of one run, owned by its poller. */
    private static final class Run {

        final SoakScenario scenario;
        final ScenarioContext ctx;
        final MinecraftServer server;
        final Burst burst;
        final ScenarioResult.Builder out;
        final SoakCurve baseline = new SoakCurve(BASELINE_TICKS);
        final SoakCurve during;
        final SoakCurve recovery = new SoakCurve(RECOVERY_TICKS);
        final SoakProbe peak = new SoakProbe();
        Phase phase = Phase.SETUP;
        int phaseTicks;
        int lastCounter;
        boolean ended;
        SoakProbe before;

        Run(SoakScenario scenario, ScenarioContext ctx, MinecraftServer server, Burst burst) {
            this.scenario = scenario;
            this.ctx = ctx;
            this.server = server;
            this.burst = burst;
            this.out = ScenarioResult.builder(scenario.name());
            this.during = new SoakCurve(scenario.burstLimit());
            this.lastCounter = server.getTickCounter();
        }

        /** One server tick; the final result on the last one, otherwise null. */
        ScenarioResult step() {
            // tickTimeArray[n % 100] holds tick n, written at the end of that tick; the newest finished one is the
            // one before the counter. Each is read once.
            int counter = server.getTickCounter();
            boolean fresh = counter != lastCounter;
            lastCounter = counter;
            double lastMs = server.tickTimeArray[Math.floorMod(counter - 1, TICK_ARRAY)] / 1.0e6;
            phaseTicks++;
            switch (phase) {
                case SETUP:
                    if (burst.prepare(phaseTicks)) {
                        next(Phase.SETTLE);
                    } else if (phaseTicks >= SETUP_LIMIT) {
                        burst.cleanup();
                        return ScenarioResult.fail(scenario.name(), "the setup did not finish in time");
                    }
                    return null;
                case SETTLE:
                    if (phaseTicks >= SETTLE_TICKS) {
                        next(Phase.BASELINE);
                    }
                    return null;
                case BASELINE:
                    if (fresh) {
                        baseline.add(lastMs);
                    }
                    if (baseline.size() >= BASELINE_TICKS) {
                        before = SoakProbe.sample();
                        GregGuns.LOG.info("SOAK {} before: {}", scenario.name(), before.line());
                        burst.fire(out);
                        next(Phase.BURST);
                    }
                    return null;
                case BURST:
                    if (fresh) {
                        during.add(lastMs);
                    }
                    if (phaseTicks % SoakCurve.BUCKET == 0) {
                        SoakProbe now = SoakProbe.sample();
                        keepPeak(now);
                        GregGuns.LOG.info(
                            "SOAK {} t={}s {} | {} | {}",
                            scenario.name(),
                            phaseTicks / SoakCurve.BUCKET,
                            lastSecond(),
                            burst.status(),
                            now.line());
                    }
                    if (burst.done()) {
                        ended = true;
                        next(Phase.RECOVER);
                    } else if (phaseTicks >= scenario.burstLimit()) {
                        GregGuns.LOG.warn("SOAK {}: the burst ran past its limit, stopping it", scenario.name());
                        burst.stop();
                        next(Phase.RECOVER);
                    }
                    return null;
                default:
                    if (fresh) {
                        recovery.add(lastMs);
                    }
                    if (recovery.size() < RECOVERY_TICKS) {
                        return null;
                    }
                    try {
                        return judge();
                    } finally {
                        burst.cleanup();
                    }
            }
        }

        private void next(Phase p) {
            phase = p;
            phaseTicks = 0;
        }

        /** @return the mean and longest tick of the last second of the burst, and the TPS it allows */
        private String lastSecond() {
            SoakCurve last = new SoakCurve(SoakCurve.BUCKET);
            for (double v : during.tail(SoakCurve.BUCKET)) {
                last.add(v);
            }
            double mean = last.stats()
                .mean();
            return String.format(
                Locale.ROOT,
                "mspt=%.1f max=%.1f tps=%.1f",
                mean,
                last.stats()
                    .max(),
                SoakCurve.tps(mean));
        }

        private void keepPeak(SoakProbe s) {
            peak.tickets = Math.max(peak.tickets, s.tickets);
            peak.forcedChunks = Math.max(peak.forcedChunks, s.forcedChunks);
            peak.allForcedChunks = Math.max(peak.allForcedChunks, s.allForcedChunks);
            peak.loadedChunks = Math.max(peak.loadedChunks, s.loadedChunks);
            peak.entities = Math.max(peak.entities, s.entities);
            peak.missiles = Math.max(peak.missiles, s.missiles);
            peak.missilesFlying = Math.max(peak.missilesFlying, s.missilesFlying);
            peak.flyingBlocks = Math.max(peak.flyingBlocks, s.flyingBlocks);
            peak.items = Math.max(peak.items, s.items);
            peak.blasts = Math.max(peak.blasts, s.blasts);
            peak.pendingBlocks = Math.max(peak.pendingBlocks, s.pendingBlocks);
            peak.gridBytes = Math.max(peak.gridBytes, s.gridBytes);
            peak.falloutBlocks = Math.max(peak.falloutBlocks, s.falloutBlocks);
            peak.heapUsedMb = Math.max(peak.heapUsedMb, s.heapUsedMb);
        }

        private ScenarioResult judge() {
            SoakProbe after = SoakProbe.sample();
            GregGuns.LOG.info("SOAK {} after: {}", scenario.name(), after.line());
            SoakVerdict.Measured m = new SoakVerdict.Measured();
            m.baselineMs = baseline.trimmedMean(SoakVerdict.TRIM);
            m.baselineTicks = baseline.size();
            m.recoveryMs = recovery.trimmedMean(SoakVerdict.TRIM);
            m.recoveryTicks = recovery.size();
            m.ended = ended;
            m.limitTicks = scenario.burstLimit();
            m.ticketsBefore = before.tickets;
            m.ticketsAfter = after.tickets;
            m.forcedBefore = before.forcedChunks;
            m.forcedAfter = after.forcedChunks;
            double margin = Double
                .parseDouble(System.getProperty(MARGIN_PROPERTY, Double.toString(SoakVerdict.RECOVERY_MARGIN_MS)));
            List<String> fails = SoakVerdict.judge(m, margin);
            burst.check(out, before, after, peak);
            for (String f : fails) {
                out.check(false, f);
            }
            int recoveredAt = during.recoveredAt();
            out.note("baseline: " + baseline.stats() + String.format(Locale.ROOT, ", trimmed %.2f ms", m.baselineMs));
            out.note(
                "burst: " + during.stats()
                    + String.format(
                        Locale.ROOT,
                        ", %.1f s of game time, %.1f s of server time, back at 20 TPS for good after %.1f s",
                        during.size() / 20.0,
                        during.totalMs() / 1000.0,
                        recoveredAt / 20.0));
            out.note("burst curve, mean/max ms per second: " + during.buckets(NOTE_BUCKETS));
            out.note("recovery: " + recovery.stats() + String.format(Locale.ROOT, ", trimmed %.2f ms", m.recoveryMs));
            out.note("before: " + before.line());
            out.note("peak: " + peak.line());
            out.note("after: " + after.line());
            ScenarioResult result = out.build();
            GregGuns.LOG.info(
                "SOAKRESULT name={} verdict={} baseline_ms={} burst_mean_ms={} burst_p95_ms={} burst_max_ms={}"
                    + " burst_s={} burst_server_s={} recovered_after_s={} recovery_ms={} tickets={}->{} forced={}->{}"
                    + " loaded_chunks={}->{} peak_loaded_chunks={} peak_heap_mb={} peak_entities={} peak_flying={}"
                    + " peak_tickets={} peak_forced={} peak_blasts={} peak_flying_blocks={} peak_items={}",
                scenario.name(),
                result.passed() ? "PASS" : "FAIL",
                fmt(m.baselineMs),
                fmt(
                    during.stats()
                        .mean()),
                fmt(
                    during.stats()
                        .p95()),
                fmt(
                    during.stats()
                        .max()),
                fmt(during.size() / 20.0),
                fmt(during.totalMs() / 1000.0),
                fmt(recoveredAt / 20.0),
                fmt(m.recoveryMs),
                before.tickets,
                after.tickets,
                before.forcedChunks,
                after.forcedChunks,
                before.loadedChunks,
                after.loadedChunks,
                peak.loadedChunks,
                peak.heapUsedMb,
                peak.entities,
                peak.missilesFlying,
                peak.tickets,
                peak.forcedChunks,
                peak.blasts,
                peak.flyingBlocks,
                peak.items);
            return result;
        }

        private static String fmt(double v) {
            return String.format(Locale.ROOT, "%.2f", v);
        }
    }
}
