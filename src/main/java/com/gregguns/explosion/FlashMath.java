package com.gregguns.explosion;

/**
 * The numbers of a flashbang as pure logic: how strongly a blast at some distance affects an entity, how much of that
 * a player keeps when looking away, how much a wall hides, and how long each effect lasts. No Minecraft types, both
 * sides.
 *
 * <p>
 * Finished design (Techguns never registered its Flashbang, GUN-24, and its effect also blinded the thrower with no
 * facing or wall check, PFX-22): {@code intensity = falloff x visibility x facing}, each from 0 to 1. The facing factor
 * applies to players only, since a mob has no screen to whiten.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     double i = FlashMath.intensity(4.0, 1.0, FlashMath.visibility(true, true, true), true); // 0.5 facing it, 4
 *                                                                                             // blocks
 *     int ticks = FlashMath.effectTicks(i); // 80
 * }
 * </pre>
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang);
// docs/techguns-spec/04-projectiles-fx/projectiles.md
// (FlashBangProjectile.explode: strength 1 - d / sqrt(32), duration strength x 200, only above 20 ticks)
public final class FlashMath {

    /** Distance at which the effect reaches zero, blocks. */
    public static final double RADIUS = 8.0;
    /** Effect duration at full intensity, ticks (8 s). */
    public static final int MAX_TICKS = 160;
    /** Shortest effect applied, ticks; weaker flashes do nothing (the Techguns threshold). */
    public static final int MIN_TICKS = 20;
    /** The share of the effect a player keeps with the blast directly behind them. */
    public static final double BEHIND_FLOOR = 0.15;
    /** Weight of the eye line in {@link #visibility}. */
    public static final double EYE_WEIGHT = 0.6;
    /** Weight of the chest line in {@link #visibility}. */
    public static final double CHEST_WEIGHT = 0.3;
    /** Weight of the feet line in {@link #visibility}. */
    public static final double FEET_WEIGHT = 0.1;
    /** Intensity from which a mob gets the heavy slowness. */
    public static final double HEAVY_MOB_INTENSITY = 0.5;
    /** Share of a player's blindness that nausea lasts. */
    public static final double NAUSEA_SHARE = 0.5;
    /** Share of the effect time that the white-out holds at full opacity before it fades. */
    public static final double OVERLAY_HOLD = 0.3;

    private FlashMath() {}

    /**
     * @param distance blocks from the blast to the entity
     * @param radius   distance at which the effect is zero
     * @return 1 at the blast, falling linearly to 0 at the radius, 0 beyond
     */
    public static double falloff(double distance, double radius) {
        if (Double.isNaN(distance) || distance >= radius || radius <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - Math.max(0.0, distance) / radius);
    }

    /**
     * How much of the entity can see the blast.
     *
     * @param eye   whether the blast has a clear line to the eyes
     * @param chest whether it has a clear line to the chest
     * @param feet  whether it has a clear line to the feet
     * @return 0 (fully hidden) to 1; the eyes weigh most because a flash is a thing you see
     */
    public static double visibility(boolean eye, boolean chest, boolean feet) {
        return (eye ? EYE_WEIGHT : 0.0) + (chest ? CHEST_WEIGHT : 0.0) + (feet ? FEET_WEIGHT : 0.0);
    }

    /**
     * @param dot the dot product of the viewer's look direction and the direction from the viewer to the blast, both
     *            unit vectors: 1 looking straight at it, -1 with it directly behind
     * @return {@link #BEHIND_FLOOR} to 1, rising with the square of how much the viewer faces the blast
     */
    public static double facing(double dot) {
        double t = (Math.max(-1.0, Math.min(1.0, dot)) + 1.0) * 0.5;
        return BEHIND_FLOOR + (1.0 - BEHIND_FLOOR) * t * t;
    }

    /**
     * @param distance   blocks from the blast to the entity
     * @param dot        the facing dot product, see {@link #facing}; ignored when {@code player} is false
     * @param visibility see {@link #visibility}
     * @param player     whether the entity has a screen (the facing factor applies)
     * @return the intensity, 0 to 1
     */
    public static double intensity(double distance, double dot, double visibility, boolean player) {
        double result = falloff(distance, RADIUS) * visibility;
        return player ? result * facing(dot) : result;
    }

    /**
     * @param intensity see {@link #intensity}
     * @return how long the main effect lasts, ticks; 0 when it would be shorter than {@link #MIN_TICKS}
     */
    public static int effectTicks(double intensity) {
        int ticks = (int) Math.round(Math.max(0.0, Math.min(1.0, intensity)) * MAX_TICKS);
        return ticks < MIN_TICKS ? 0 : ticks;
    }

    /**
     * @param blindTicks a player's blindness duration, ticks
     * @return the nausea that goes with it, ticks (0 when there is no blindness)
     */
    public static int nauseaTicks(int blindTicks) {
        return (int) Math.round(blindTicks * NAUSEA_SHARE);
    }

    /**
     * @param intensity see {@link #intensity}
     * @return the slowness level (0 based) of a mob: 3 from {@link #HEAVY_MOB_INTENSITY}, else 1
     */
    public static int mobSlownessLevel(double intensity) {
        return intensity >= HEAVY_MOB_INTENSITY ? 3 : 1;
    }

    /**
     * The opacity of the client white-out: full while the flash is fresh, then fading to nothing, eased.
     *
     * @param elapsed ticks since the flash (may include a fraction)
     * @param total   the effect's length, ticks
     * @return 0 to 1
     */
    public static double overlayAlpha(double elapsed, int total) {
        if (total <= 0 || elapsed >= total) {
            return 0.0;
        }
        double hold = total * OVERLAY_HOLD;
        if (elapsed <= hold) {
            return 1.0;
        }
        double left = 1.0 - (elapsed - hold) / (total - hold);
        return left * left;
    }
}
