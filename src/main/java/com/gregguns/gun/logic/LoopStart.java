package com.gregguns.gun.logic;

/**
 * Which sound a continuous-fire gun plays for a shot: the start sound after a pause, the loop sound otherwise. The
 * Flamethrower's {@code flamethrowerStart} plays on the first shot after at least ten idle ticks and every later shot
 * of the burst plays {@code flamethrowerFire}. Pure; the caller supplies the world tick, so no clock is read.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Flamethrower, section 4, loopSoundDelay 10)
public final class LoopStart {

    /** Idle ticks after which the next shot plays the start sound again. */
    public static final int IDLE_TICKS = 10;

    /** The "never fired" value of a last-shot tick, so the first shot is a start. */
    public static final long NEVER = Long.MIN_VALUE / 2;

    private LoopStart() {}

    /**
     * @param nowTick      the world tick of this shot
     * @param lastShotTick the world tick of the previous shot of this shooter, or {@link #NEVER}
     * @return true if this shot plays the start sound
     */
    public static boolean isStart(long nowTick, long lastShotTick) {
        return nowTick - lastShotTick >= IDLE_TICKS;
    }
}
