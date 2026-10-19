package com.gregguns.gun.logic;

/**
 * Resolves the capacity of one energy gun from its default and the config (GGS-02-14). Pure: the config values are
 * passed in.
 *
 * <p>
 * An override line is {@code gunId=eu}, for example {@code laser_rifle=8000000}. Lines that do not parse, name
 * another gun or hold a value below one shot are ignored, so a typo in a config file never disables a gun. The
 * percent scale applies to the default only; an explicit override wins as written.
 *
 * <p>
 * Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-14).
 */
public final class EnergyCapacity {

    private EnergyCapacity() {}

    /**
     * Capacity in EU of a gun.
     *
     * @param gunId        the gun's id ({@link GunStats#id()})
     * @param defaultEu    the capacity without config
     * @param euPerShot    the gun's cost per shot; the result is never below it
     * @param scalePercent global scale of the default, 1 or more (100 = unchanged)
     * @param overrides    {@code gunId=eu} lines, may be null
     */
    public static long resolve(String gunId, long defaultEu, long euPerShot, int scalePercent, String[] overrides) {
        long floor = Math.max(1L, euPerShot);
        if (overrides != null) {
            for (String line : overrides) {
                long value = parse(gunId, line);
                if (value >= floor) {
                    return value;
                }
            }
        }
        long scaled = defaultEu * Math.max(1, scalePercent) / 100L;
        return Math.max(floor, scaled);
    }

    private static long parse(String gunId, String line) {
        if (line == null) {
            return -1L;
        }
        int eq = line.indexOf('=');
        if (eq < 0 || !line.substring(0, eq)
            .trim()
            .equals(gunId)) {
            return -1L;
        }
        try {
            return Long.parseLong(
                line.substring(eq + 1)
                    .trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
