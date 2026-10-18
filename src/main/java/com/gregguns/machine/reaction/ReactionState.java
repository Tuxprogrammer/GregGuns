package com.gregguns.machine.reaction;

import java.util.Random;

/**
 * The Reaction Chamber's run: preferred-intensity drift, completion count and the 60-tick reaction clock. Plain Java
 * with no Minecraft types, shared by the single block and the Retort.
 *
 * <p>
 * Call {@link #start} when GT starts a recipe, then {@link #tick} once per game tick the machine actually runs (power
 * drawn). Every {@value #REACTION_TICK} running ticks is a reaction tick: if the player's intensity setting equals the
 * current preferred intensity, completion goes up; then the preferred intensity drifts. The final reaction tick is
 * evaluated (Techguns skipped it: MPRD-18). Server side only; the state is saved with {@link #toArray}.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (GT5-Unofficial: running the Techguns
// mechanics on ProcessingLogic), docs/techguns-spec/08-machines-production/reaction-chamber.md (section 4)
public final class ReactionState {

    /** Running ticks per reaction tick (3 seconds). */
    public static final int REACTION_TICK = 60;
    /** Highest intensity setting. */
    public static final int MAX_INTENSITY = 10;

    /** The result of one {@link #tick}. */
    public enum Outcome {
        /** The run goes on. */
        RUNNING,
        /** {@code completion >= required}: deliver the outputs now. */
        SUCCESS,
        /** The duration ended short of the required completion: apply the risk. */
        FAILURE
    }

    private boolean active;
    private int baseIntensity;
    private int preferred;
    private int margin;
    private int required;
    private int completion;
    private int counter;
    private int elapsed;
    private int maxTicks;
    private ReactionRisk risk = ReactionRisk.NONE;

    /**
     * Begins a run.
     *
     * @param preferredIntensity the recipe's preferred intensity, 0 to 10
     * @param intensityMargin    how far the preferred intensity may drift either way; 0 never drifts
     * @param requiredCompletion good reaction ticks needed
     * @param durationTicks      the recipe duration in game ticks (reaction ticks times 60)
     * @param failureRisk        applied by the caller on {@link Outcome#FAILURE}
     */
    public void start(int preferredIntensity, int intensityMargin, int requiredCompletion, int durationTicks,
        ReactionRisk failureRisk) {
        this.active = true;
        this.baseIntensity = preferredIntensity;
        this.preferred = preferredIntensity;
        this.margin = Math.max(0, intensityMargin);
        this.required = requiredCompletion;
        this.completion = 0;
        this.counter = 0;
        this.elapsed = 0;
        this.maxTicks = durationTicks;
        this.risk = failureRisk == null ? ReactionRisk.NONE : failureRisk;
    }

    /** Ends the run without an outcome (the machine was stopped). */
    public void clear() {
        active = false;
    }

    /**
     * Advances one running game tick.
     *
     * @param intensitySetting the player's current intensity setting
     * @param random           the source of the drift roll; seed it in tests
     * @return the outcome; after {@code SUCCESS} or {@code FAILURE} the run is no longer active
     */
    public Outcome tick(int intensitySetting, Random random) {
        if (!active) {
            return Outcome.RUNNING;
        }
        elapsed++;
        counter++;
        if (counter >= REACTION_TICK) {
            counter = 0;
            if (intensitySetting == preferred) {
                completion++;
            }
            drift(random);
        }
        if (completion >= required) {
            active = false;
            return Outcome.SUCCESS;
        }
        if (elapsed >= maxTicks) {
            active = false;
            return Outcome.FAILURE;
        }
        return Outcome.RUNNING;
    }

    private void drift(Random random) {
        if (margin == 0) {
            return;
        }
        // Techguns rule: a third down, a third same, a third up, clamped to preferred +/- margin.
        int roll = random.nextInt(3);
        if (roll == 0 && preferred > Math.max(0, baseIntensity - margin)) {
            preferred--;
        } else if (roll == 2 && preferred < Math.min(MAX_INTENSITY, baseIntensity + margin)) {
            preferred++;
        }
    }

    /**
     * Clamps an intensity setting written by a client.
     *
     * @param value any int
     * @return the value limited to 0 to 10
     */
    public static int clampIntensity(int value) {
        return Math.max(0, Math.min(MAX_INTENSITY, value));
    }

    /** @return whether a run is in progress */
    public boolean isActive() {
        return active;
    }

    /** @return the current preferred intensity */
    public int preferredIntensity() {
        return preferred;
    }

    /** @return good reaction ticks so far */
    public int completion() {
        return completion;
    }

    /** @return good reaction ticks needed */
    public int required() {
        return required;
    }

    /** @return running ticks since the start */
    public int elapsed() {
        return elapsed;
    }

    /** @return the risk applied on failure */
    public ReactionRisk risk() {
        return risk;
    }

    /**
     * @return the state as ints for NBT: active, base, preferred, margin, required, completion, counter, elapsed, max,
     *         risk
     */
    public int[] toArray() {
        return new int[] { active ? 1 : 0, baseIntensity, preferred, margin, required, completion, counter, elapsed,
            maxTicks, risk.ordinal() };
    }

    /**
     * Restores a saved state.
     *
     * @param data the result of {@link #toArray}; a wrong-sized array leaves the state inactive
     */
    public void fromArray(int[] data) {
        if (data == null || data.length != 10) {
            active = false;
            return;
        }
        active = data[0] != 0;
        baseIntensity = data[1];
        preferred = data[2];
        margin = data[3];
        required = data[4];
        completion = data[5];
        counter = data[6];
        elapsed = data[7];
        maxTicks = data[8];
        ReactionRisk[] risks = ReactionRisk.values();
        risk = data[9] >= 0 && data[9] < risks.length ? risks[data[9]] : ReactionRisk.NONE;
    }
}
