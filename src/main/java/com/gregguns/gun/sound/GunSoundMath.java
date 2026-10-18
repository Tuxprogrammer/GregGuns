package com.gregguns.gun.sound;

/**
 * Pure numbers for gun sounds: volumes, audible range, the muzzle origin and the channel budget. No Minecraft types,
 * both sides.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/gun-sound-pipeline.md (GGS-03-10);
// docs/techguns-spec/03-gun-visuals-audio/rendering-framework.md section 14
public final class GunSoundMath {

    /** Volume of a fire sound. In 1.7.10 a volume above 1 carries {@code 16 * volume} blocks, so this carries 64. */
    public static final float FIRE_VOLUME = 4.0f;

    /** Volume of reload and other handling sounds; carries 16 blocks. */
    public static final float HANDLING_VOLUME = 1.0f;

    /** Volume of the dry-fire click. */
    public static final float DRY_FIRE_VOLUME = 1.0f;

    /** Volume of a bullet impact. */
    public static final float IMPACT_VOLUME = 1.0f;

    /** Most simultaneous channels one weapon may hold (GGS-03-10). A proposal, to be tuned in M2. */
    public static final int MAX_CHANNELS_PER_WEAPON = 8;

    private GunSoundMath() {}

    /**
     * The distance at which a positional sound is audible, as vanilla 1.7.10 computes it. The server sends the sound
     * to players within this range, and the client attenuates linearly to it.
     *
     * @param volume the sound's volume (1.0 is normal)
     * @return blocks: 16 for a volume of at most 1, otherwise {@code 16 * volume}
     */
    // Source: 1.7.10 WorldManager.playSound sends to players within `volume > 1 ? 16 * volume : 16` blocks, and
    // SoundManager uses the same product as the linear attenuation distance.
    public static double audibleRange(float volume) {
        return volume > 1.0f ? 16.0 * volume : 16.0;
    }

    /**
     * @param yawDegrees the shooter's head yaw in Minecraft degrees (0 faces +Z, 90 faces -X)
     * @param distance   blocks in front of the shooter
     * @return the X offset of the point {@code distance} blocks ahead
     */
    public static double forwardX(float yawDegrees, double distance) {
        return -Math.sin(Math.toRadians(yawDegrees)) * distance;
    }

    /**
     * @param yawDegrees the shooter's head yaw in Minecraft degrees
     * @param distance   blocks in front of the shooter
     * @return the Z offset of the point {@code distance} blocks ahead
     */
    public static double forwardZ(float yawDegrees, double distance) {
        return Math.cos(Math.toRadians(yawDegrees)) * distance;
    }

    /**
     * How many channels a repeating fire sound holds at once.
     *
     * @param longestVariantSeconds length of the longest fire variant, seconds
     * @param shotsPerSecond        the weapon's maximum rate of fire
     * @return {@code ceil(length * rate)}, at least 1
     */
    public static int channelsNeeded(double longestVariantSeconds, double shotsPerSecond) {
        if (longestVariantSeconds < 0 || shotsPerSecond < 0) {
            throw new IllegalArgumentException("length and rate must not be negative");
        }
        return Math.max(1, (int) Math.ceil(longestVariantSeconds * shotsPerSecond - 1e-9));
    }

    /**
     * @param longestVariantSeconds length of the longest fire variant, seconds
     * @param shotsPerSecond        the weapon's maximum rate of fire
     * @return whether the weapon fits {@link #MAX_CHANNELS_PER_WEAPON}; if not it needs a start, loop, stop scheme
     */
    public static boolean withinChannelBudget(double longestVariantSeconds, double shotsPerSecond) {
        return channelsNeeded(longestVariantSeconds, shotsPerSecond) <= MAX_CHANNELS_PER_WEAPON;
    }
}
