package com.gregguns.icbm.silo;

import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.missile.MissileTier;

/**
 * The silo's decisions as plain data: which voltage a body needs, whether a warhead fits it, how much fuel one pump
 * cycle moves and what a cycle costs. No Minecraft types, both sides, so the unit tests pin them (D70).
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Mating, Fuel, Tiers)
public final class SiloRules {

    /** How long mating a body and a warhead takes, ticks (20 seconds). */
    public static final int MATE_TICKS = 400;
    /** How long one fuel pump cycle takes, ticks. */
    public static final int PUMP_TICKS = 40;
    /** The most fuel one pump cycle moves, mB. */
    public static final int PUMP_MB = 2000;

    /** What a mating attempt came to. */
    public enum Mate {
        /** Body and warhead fit and the energy hatch is strong enough. */
        OK,
        /** No missile body in the input buses. */
        NO_BODY,
        /** A body but no warhead. */
        NO_WARHEAD,
        /** The warhead is heavier than the body carries. */
        WRONG_WARHEAD,
        /** The energy hatch is below the body's voltage tier. */
        LOW_VOLTAGE
    }

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
     * @param body         the body tier, null for none
     * @param warhead      the warhead's tier, null for none
     * @param hatchVoltage the voltage index of the weakest energy hatch
     * @return the verdict, first problem first
     */
    public static Mate mate(MissileTier body, ExplosiveTier warhead, int hatchVoltage) {
        if (body == null) {
            return Mate.NO_BODY;
        }
        if (warhead == null) {
            return Mate.NO_WARHEAD;
        }
        if (!body.carries(warhead)) {
            return Mate.WRONG_WARHEAD;
        }
        if (hatchVoltage < requiredVoltageIndex(body)) {
            return Mate.LOW_VOLTAGE;
        }
        return Mate.OK;
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
}
