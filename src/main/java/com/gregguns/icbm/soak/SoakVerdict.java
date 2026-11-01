package com.gregguns.icbm.soak;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The owner's soak bar (D332) as a pure rule: a burst may lag the server, but it must end, the server must come back
 * to playable speed once it has, and no chunk ticket or forced chunk may be left behind. Spikes during the burst are
 * recorded, never judged. No Minecraft types.
 *
 * <p>
 * Speed is judged on trimmed means ({@link SoakCurve#trimmedMean}, the slowest {@value #TRIM} left out) so a world
 * save that happens to fall in the recovery window and not in the baseline does not decide the result.
 */
// Spec: docs/gregguns-icbm-spec/01-core/soak.md
public final class SoakVerdict {

    /** Absolute cap on the recovery tick time: 50 ms is 20 TPS. */
    public static final double PLAYABLE_MEAN_MS = 50.0;
    /**
     * Largest rise of the recovery tick time over the baseline, ms. A real-pack server's own load wanders by a few
     * milliseconds from minute to minute (mob spawning, chunk saves), so the margin is absolute, as in npc.soak.
     */
    public static final double RECOVERY_MARGIN_MS = 10.0;
    /** The share of the slowest ticks the speed checks leave out. */
    public static final double TRIM = 0.05;

    private SoakVerdict() {}

    /** What one soak measured, for the judge. */
    public static final class Measured {

        /** Trimmed mean tick time before the burst, nothing of the soak running, ms. */
        public double baselineMs;
        /** Ticks in the baseline. */
        public int baselineTicks;
        /** Trimmed mean tick time after the burst ended, ms. */
        public double recoveryMs;
        /** Ticks in the recovery window. */
        public int recoveryTicks;
        /** The longest the recovery could wait for the window to come back, ticks; 0 when it judged at once. */
        public int recoveryLimitTicks;
        /** Whether everything the burst started ended inside its time limit. */
        public boolean ended;
        /** The burst's time limit, ticks. */
        public int limitTicks;
        /** GregGuns chunk tickets before the burst. */
        public int ticketsBefore;
        /** GregGuns chunk tickets after the recovery. */
        public int ticketsAfter;
        /** Chunks GregGuns tickets force before the burst. */
        public int forcedBefore;
        /** Chunks GregGuns tickets force after the recovery. */
        public int forcedAfter;
    }

    /**
     * @param m        the measurements
     * @param marginMs the largest allowed rise of the recovery tick time over the baseline
     * @return the failures, empty when the soak meets the bar
     */
    public static List<String> judge(Measured m, double marginMs) {
        List<String> out = new ArrayList<>();
        if (!m.ended) {
            out.add(String.format(Locale.ROOT, "the burst was still running after %d ticks (runaway)", m.limitTicks));
        }
        if (m.baselineTicks == 0 || m.recoveryTicks == 0) {
            out.add("no tick samples were taken");
        } else {
            if (m.recoveryMs >= PLAYABLE_MEAN_MS) {
                out.add(
                    String.format(
                        Locale.ROOT,
                        "not playable after the burst: recovery tick %.2f ms is not under %.0f ms",
                        m.recoveryMs,
                        PLAYABLE_MEAN_MS));
            }
            double rise = m.recoveryMs - m.baselineMs;
            if (rise > marginMs) {
                out.add(
                    String.format(
                        Locale.ROOT,
                        "did not recover%s: the tick stayed %.2f ms over the baseline (%.2f -> %.2f ms), margin %.1f ms",
                        m.recoveryLimitTicks > 0 ? String.format(Locale.ROOT, " in %d s", m.recoveryLimitTicks / 20)
                            : "",
                        rise,
                        m.baselineMs,
                        m.recoveryMs,
                        marginMs));
            }
        }
        if (m.ticketsAfter != m.ticketsBefore) {
            out.add(
                String
                    .format(Locale.ROOT, "chunk tickets leaked: %d before, %d after", m.ticketsBefore, m.ticketsAfter));
        }
        if (m.forcedAfter != m.forcedBefore) {
            out.add(
                String.format(Locale.ROOT, "forced chunks leaked: %d before, %d after", m.forcedBefore, m.forcedAfter));
        }
        return out;
    }
}
