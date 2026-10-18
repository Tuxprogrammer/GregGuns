package com.gregguns.armor.flight;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.armor.flight.logic.BackKind;
import com.gregguns.armor.flight.logic.FlightStatus;
import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client, to the wearer only: which back device is worn, whether it is engaged and whether it has the energy
 * for its next action. The client predicts glide, boost jump and flight from it (1.7.10 clients own their motion); the
 * server alone spends EU. Sent on change and repeated every 100 ticks.
 *
 * <p>
 * Wire: kind (byte), flags (byte: bit 0 engaged, bit 1 powered). Append-only list position: see
 * {@code GregGunsNetwork}. A bad kind is dropped, not cast (CORE-40). Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-18); docs/gregguns-spec/01-core/networking-gtnhlib.md
public final class PacketFlightStatus extends ClientboundPacket {

    private static final int ENGAGED = 1;
    private static final int POWERED = 2;

    private int kind;
    private int flags;

    /** Required by the channel. */
    public PacketFlightStatus() {}

    /** @param status the status to report */
    public PacketFlightStatus(FlightStatus status) {
        this.kind = status.kind()
            .ordinal();
        this.flags = (status.engaged() ? ENGAGED : 0) | (status.powered() ? POWERED : 0);
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(kind);
        buf.writeByte(flags);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        kind = buf.readByte();
        flags = buf.readByte();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.ordinalInRange("kind", kind, BackKind.class)
            .inRange("flags", flags, 0, ENGAGED | POWERED);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        Flight.mirror()
            .set(
                localPlayer,
                new FlightStatus(BackKind.values()[kind], (flags & ENGAGED) != 0, (flags & POWERED) != 0)); // safe:
                                                                                                            // validated
    }
}
