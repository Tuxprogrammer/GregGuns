package com.gregguns.icbm.silo;

import com.gregguns.icbm.missile.MissileTier;

/**
 * The silo's decisions as plain data: which voltage a body needs, how long each assembly cycle runs, how much fuel
 * one pump cycle moves and when a step may start. The order of the steps is {@link SiloSequence}. No Minecraft types,
 * both sides, so the unit tests pin them (D70).
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The assembly sequence, Fuel, Tiers)
public final class SiloRules {

    /** How long the body's working cycle takes once the body stands on the pad, ticks (10 seconds, D286). */
    public static final int BODY_TICKS = 200;
    /** How long mating the warhead to the body takes once it sits on the nose, ticks (10 seconds, D286). */
    public static final int WARHEAD_TICKS = 200;
    /** How long one fuel pump cycle takes, ticks. */
    public static final int PUMP_TICKS = 40;
    /** The most fuel one pump cycle moves, mB. */
    public static final int PUMP_MB = 2000;

    /** How many ticks of a step's draw the hatches must already hold before the silo starts it. */
    public static final int MIN_BUFFER_TICKS = 2;

    private SiloRules() {}

    /**
     * @param body a body tier
     * @return the GregTech voltage index the silo's weakest energy hatch must reach to handle it: HV for T1, EV for T2,
     *         LuV for T3 (the body's warhead tier voltage, D5)
     */
    public static int requiredVoltageIndex(MissileTier body) {
        return body.warheadTier()
            .minVoltageTier();
    }

    /**
     * @param capacityMb the missile's tank size
     * @param storedMb   what is in it
     * @param offeredMb  what the hatches hold of an accepted fuel
     * @return how much one pump cycle moves: the least of {@link #PUMP_MB}, the room left and what is offered
     */
    public static int pumpAmount(int capacityMb, int storedMb, int offeredMb) {
        return Math.max(0, Math.min(PUMP_MB, Math.min(capacityMb - storedMb, offeredMb)));
    }

    /**
     * Whether the silo may start a step. GT stops a machine for good ("power loss") the first tick it cannot pay, so a
     * step that takes its parts must not begin without EU on hand: it waits, idle, and starts when the power arrives.
     *
     * @param storedEu  the EU in all the energy hatches
     * @param euPerTick what the step draws each tick
     * @return whether the hatches hold at least {@link #MIN_BUFFER_TICKS} ticks of the draw
     */
    public static boolean canStart(long storedEu, long euPerTick) {
        return euPerTick > 0 && storedEu >= euPerTick * MIN_BUFFER_TICKS;
    }
}
