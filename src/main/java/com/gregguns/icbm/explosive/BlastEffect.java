package com.gregguns.icbm.explosive;

/**
 * The per-type effect hook (gas, incendiary, smoke, shrapnel and so on, D10). An effect is stateless: anything it
 * must remember lives in {@link BlastContext#state()}, which the engine saves and reloads with the blast. All calls
 * are on the server thread. The engine already did terrain and the damage ramp; the effect adds the rest.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md (effect hook)
public interface BlastEffect {

    /** An effect that does nothing. */
    BlastEffect NONE = new BlastEffect() {};

    /** Called once, when the blast is accepted (after {@code ExplosionEvent.Start}), before terrain work starts. */
    default void onDetonate(BlastContext ctx) {}

    /**
     * Called once per server tick while the blast runs (after detonation, until this returns false and the terrain
     * work is done). Keep it cheap and use {@link BlastContext#budget()} for anything that scales.
     *
     * @param ctx the blast
     * @param age ticks since detonation, saved with the blast
     * @return true to keep ticking, false when the effect is finished
     */
    default boolean onTick(BlastContext ctx, int age) {
        return false;
    }

    /** Called once when the blast ends, also when it is cancelled by a world unload or {@code /gregguns} clear. */
    default void onFinish(BlastContext ctx) {}
}
