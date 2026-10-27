package com.gregguns.icbm.silo;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to every client tracking an {@link EntitySiloDoors}: the door state, the low 32 bits of the game time the
 * current swing began, and the swing duration. Sent on change and repeated about once a second, so a client that starts
 * tracking late catches up; clients interpolate with {@link SiloDoors#progress}. A bad value or a target that is not a
 * door entity is dropped (CORE-40).
 *
 * <p>
 * Wire: entity id (int), state (byte), start (int), duration (int). Append-only list position: see
 * {@code GregGunsNetwork}. Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The doors)
public final class PacketSiloDoors extends ClientboundPacket {

    private int entityId;
    private int state;
    private int start;
    private int duration;

    /** Required by the channel. */
    public PacketSiloDoors() {}

    /**
     * @param doors the door entity, with the controller's state already pushed
     */
    PacketSiloDoors(EntitySiloDoors doors) {
        this.entityId = doors.getEntityId();
        this.state = doors.doors()
            .state()
            .ordinal();
        this.start = (int) doors.doors()
            .start();
        this.duration = doors.doors()
            .duration();
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(state);
        buf.writeInt(start);
        buf.writeInt(duration);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        state = buf.readByte();
        start = buf.readInt();
        duration = buf.readInt();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("state", state, 0, SiloDoors.State.values().length - 1)
            .inRange("duration", duration, 1, 100_000)
            .targetOfType("silo doors", () -> {
                Entity e = WorldTargets.entity(world, entityId);
                return e instanceof EntitySiloDoors ? e : null;
            }, Entity.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        ((EntitySiloDoors) world.getEntityByID(entityId)).applyState(state, start, duration);
    }
}
