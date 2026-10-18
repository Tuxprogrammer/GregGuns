package com.gregguns.armor.flight;

import java.lang.ref.WeakReference;

import net.minecraft.entity.Entity;

import com.gregguns.armor.flight.logic.FlightStatus;

/**
 * The client's copy of the local player's back-device status, filled by {@link PacketFlightStatus}. It is tied to one
 * player entity, so a respawn or a new connection reads as "no device" until the server repeats the status. Client
 * thread only (a dedicated server never writes it).
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-18)
public final class FlightMirror {

    private WeakReference<Entity> owner = new WeakReference<>(null);
    private FlightStatus status = FlightStatus.NONE;

    /**
     * @param player the local player entity the status belongs to
     * @param status the status the server reported
     */
    public void set(Entity player, FlightStatus status) {
        this.owner = new WeakReference<>(player);
        this.status = status;
    }

    /**
     * @param entity any entity
     * @return the server-reported status when {@code entity} is the player it was reported for, else
     *         {@link FlightStatus#NONE}
     */
    public FlightStatus status(Entity entity) {
        return entity != null && owner.get() == entity ? status : FlightStatus.NONE;
    }
}
