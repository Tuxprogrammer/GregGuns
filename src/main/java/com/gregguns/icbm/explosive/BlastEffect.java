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

    /**
     * Whether the effect has scheduled work for the shared budget now. Asked by the scheduler each turn while the blast
     * is in its effect stage; an effect that scans or edits terrain over many ticks (red matter) offers its work here
     * so it is bounded by the same {@link TickBudget} as every crater, instead of running unbudgeted in
     * {@link #onTick}. Keep it cheap; an idle effect returns false and costs nothing.
     *
     * @param ctx the blast
     * @return true when {@link #work} should run
     */
    default boolean wantsWork(BlastContext ctx) {
        return false;
    }

    /**
     * One slice of scheduled work (a few thousand cell checks and a few dozen block edits at most), on the server
     * thread, only after {@link #wantsWork} said yes. Progress lives in {@link BlastContext#state()}.
     *
     * @param ctx the blast
     */
    default void work(BlastContext ctx) {}

    /** Called once when the blast ends, also when it is cancelled by a world unload or {@code /gregguns} clear. */
    default void onFinish(BlastContext ctx) {}
}
