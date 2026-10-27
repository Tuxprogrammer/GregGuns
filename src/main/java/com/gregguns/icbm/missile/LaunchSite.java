package com.gregguns.icbm.missile;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Where an unlaunched missile stands and can be fired from: a Galacticraft pad or the GregGuns silo. The Launch Control
 * asks this and nothing else, so it needs no pad or silo type (D70). Pads keep their static {@link PadMissiles} API;
 * {@link PadMissiles#site} wraps it. Server side for {@link #launch}.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The launch site API)
public interface LaunchSite {

    /** @return X of the missile's column center (the launch point), for the range check */
    double centerX();

    /** @return Z of the missile's column center */
    double centerZ();

    /** @return what stands there; {@link MissileStatus#NONE} for nothing */
    MissileStatus status();

    /** @return the largest launch range of the missile in blocks, 0 when there is none */
    int maxRange();

    /**
     * @param targetX target X
     * @param targetZ target Z
     * @return the blast radius at that target with the fuel bonus (D20), 0 when there is no missile
     */
    double expectedRadius(double targetX, double targetZ);

    /**
     * A refusal the site knows before any launch call, so the Launch Control shows it and keeps the Launch button
     * locked: the silo answers {@link LaunchResult#SILO_TIER} when its energy hatch is below the missile body's tier.
     *
     * @return {@link LaunchResult#OK} when the site has no objection (the default, and always for a pad)
     */
    default LaunchResult hold() {
        return LaunchResult.OK;
    }

    /**
     * Launches the missile; every rule is checked in the call.
     *
     * @param targetX the target X
     * @param targetZ the target Z
     * @param source  who fired it, may be null
     * @return the outcome
     */
    LaunchResult launch(double targetX, double targetZ, EntityPlayer source);

    /**
     * @return ticks left on the visible countdown of a launch in progress (the warm-up, D88), or -1 when none is
     *         running
     */
    default int countdownTicks() {
        return -1;
    }

    /** @return true from ignition until the missile has climbed away: the launch can no longer be aborted */
    default boolean ignited() {
        return false;
    }

    /**
     * Stops a countdown in progress (D88): the missile stays armed and fueled on its pad, and a silo closes its doors.
     * Refused once the engines have ignited. Server side.
     *
     * @return true when a launch was aborted
     */
    default boolean abort() {
        return false;
    }
}
