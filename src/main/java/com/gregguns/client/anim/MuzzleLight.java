package com.gregguns.client.anim;

/**
 * The luminance a held gun reports to Angelica's dynamic lights for a few ticks after a shot. Pure, allocation free.
 * The gun item's {@code getLuminance} can call this once it can tell how long ago the holder fired (see the open
 * question in docs/gregguns-spec/04-projectiles-fx/dynamic-lights.md).
 *
 * <p>
 * Spec: GGS-04-03 (at most 12, for at most 3 ticks).
 */
public final class MuzzleLight {

    /** Light level of a muzzle flash. */
    public static final int FLASH_LUMINANCE = 12;
    /** How long the flash light lasts. */
    public static final double FLASH_TICKS = 3.0;

    private MuzzleLight() {}

    /**
     * @param ticksSinceShot ticks since the holder fired, negative or huge if never
     * @return 12 for the first three ticks after a shot, else 0
     */
    public static int luminance(double ticksSinceShot) {
        return ticksSinceShot >= 0.0 && ticksSinceShot < FLASH_TICKS ? FLASH_LUMINANCE : 0;
    }
}
