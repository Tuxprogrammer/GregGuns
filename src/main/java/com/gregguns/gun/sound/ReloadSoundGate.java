package com.gregguns.gun.sound;

/**
 * Suppresses a second reload sound that starts too soon after the previous one, so overlapping reload calls (an
 * automatic reload and the reload key at the same moment) do not double up. Time is in game ticks, so it needs no wall
 * clock. Not thread-safe; use from the client thread.
 */
// Spec: docs/techguns-spec/03-gun-visuals-audio/sounds.md (Reload sound: a second local reload sound within 500 ms is
// suppressed); 500 ms is 10 ticks.
public final class ReloadSoundGate {

    /** The suppression window, ticks (500 ms at 20 ticks per second). */
    public static final int WINDOW_TICKS = 10;

    private long lastStartTick = Long.MIN_VALUE;

    /**
     * Asks to start a reload sound.
     *
     * @param nowTick the current game tick
     * @return true if the sound may play (the window restarts), false if it is within the window of the last one
     */
    public boolean tryStart(long nowTick) {
        if (lastStartTick != Long.MIN_VALUE && nowTick >= lastStartTick && nowTick - lastStartTick < WINDOW_TICKS) {
            return false;
        }
        lastStartTick = nowTick;
        return true;
    }

    /** Forgets the last start. */
    public void reset() {
        lastStartTick = Long.MIN_VALUE;
    }
}
