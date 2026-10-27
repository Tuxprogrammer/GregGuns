package com.gregguns.icbm.defense;

/**
 * The radar numbers as plain data: range by voltage tier, the power draw, the scan period and the protected radius
 * limits (D14, D91). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Numbers)
public final class RadarRules {

    /** Voltage index of an HV radar, the weakest there is (D14: none below HV). */
    public static final int MIN_TIER = 3;
    /** Voltage index of the weakest radar tower hatch (IV). */
    public static final int TOWER_MIN_TIER = 5;
    /** Ticks between scans. */
    public static final int SCAN_TICKS = 5;
    /** Protected radius a new radar starts with, blocks. */
    public static final int DEFAULT_RADIUS = 64;
    /** Smallest protected radius, blocks. */
    public static final int MIN_RADIUS = 8;
    /** Highest redstone strength the output gives. */
    public static final int MAX_REDSTONE = 15;

    private RadarRules() {}

    /**
     * @param tier a voltage index (HV is 3)
     * @return the detection range in blocks; 0 below HV
     */
    public static int range(int tier) {
        switch (tier) {
            case 3:
                return 128;
            case 4:
                return 256;
            case 5:
                return 512;
            case 6:
                return 768;
            case 7:
                return 1024;
            case 8:
                return 1280;
            default:
                return tier > 8 ? 1536 : 0;
        }
    }

    /**
     * @param euPerTickAtTier the machine's voltage, EU per tick for one amp
     * @return the EU one tick of scanning costs: a quarter amp
     */
    public static long euPerTick(long euPerTickAtTier) {
        return euPerTickAtTier / 4L;
    }

    /**
     * @param wanted a protected radius a player typed
     * @param range  the radar's range
     * @return the radius clamped to {@link #MIN_RADIUS} and the range
     */
    public static int clampRadius(int wanted, int range) {
        return Math.max(MIN_RADIUS, Math.min(Math.max(MIN_RADIUS, range), wanted));
    }

    /**
     * @param incoming how many contacts are incoming
     * @return the redstone strength: the count, at most {@link #MAX_REDSTONE}
     */
    public static int redstone(int incoming) {
        return Math.max(0, Math.min(MAX_REDSTONE, incoming));
    }

    /**
     * @param powered  whether the radar can pay for its scan
     * @param detected contacts in range
     * @param incoming contacts whose predicted impact is in the protected radius
     * @return what the radar reports
     */
    public static State state(boolean powered, int detected, int incoming) {
        if (!powered) {
            return State.OFF;
        }
        if (incoming > 0) {
            return State.THREAT;
        }
        return detected > 0 ? State.TRACKING : State.CLEAR;
    }

    /** What a radar reports; one lang key per value, {@code gregguns.icbm.radar.state.<name lowercased>}. */
    public enum State {

        /** It cannot pay for its scan, or the tower is not formed. */
        OFF,
        /** Scanning, nothing in range. */
        CLEAR,
        /** Missiles in range, none aimed inside the protected radius. */
        TRACKING,
        /** At least one missile is aimed inside the protected radius. */
        THREAT;

        /** @return the lang key of the state's name */
        public String langKey() {
            return "gregguns.icbm.radar.state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
