package com.gregguns.icbm.missile;

/**
 * The numbers behind the missile's engine sound: the pitch that rises through the countdown and the volume of the
 * loop. Plain floats, no Minecraft types, both sides, so a unit test can pin them.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/index.md (Launch and flight sound)
public final class MissileEngineMath {

    /** Galacticraft's registered sound event for a rocket's engine loop (namespace galacticraftcore). */
    public static final String EVENT = "galacticraftcore:shuttle.shuttle";

    /** Loop volume; a linear-falloff sound is heard out to 16 blocks times this, so 4 carries about 64 blocks. */
    public static final float VOLUME = 4.0F;

    private MissileEngineMath() {}

    /**
     * @param tick  ticks into the warm-up
     * @param total lead plus countdown
     * @return pitch: 0 at the start of the warm-up rising to 1 at ignition, as Galacticraft's rocket does (the sound
     *         engine clamps what it plays to its own minimum)
     */
    public static float warmupPitch(int tick, int total) {
        if (total <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, tick / (float) total));
    }

    /**
     * @param phase the missile's flight phase
     * @param fried whether an EMP fried the guidance: the missile falls with no more thrust (D16), so the engine is out
     * @return true when the engine loop should be sounding: from the start of the warm-up until the missile is gone or
     *         its guidance is fried
     */
    public static boolean sounding(FlightPhase phase, boolean fried) {
        return phase != FlightPhase.PAD && !fried;
    }
}
