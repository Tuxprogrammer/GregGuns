package com.gregguns.icbm.missile;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to every client tracking a {@link EntityMissile}: its flight phase, warhead casing, nuclear flag and fuel,
 * which drive the
 * engine flames and the nose model. Sent on change and repeated about once a second, so a client that starts tracking
 * late catches up. It replaces DataWatcher slots, which GregGuns never claims (GGS-01-11).
 *
 * <p>
 * Wire: entity id (int), phase (byte), casing (byte), fuel (int), nuclear flag (byte, picks the nuclear model), warm-up
 * tick, lead and total (ints, D88).
 * Append-only list position: see
 * {@code GregGunsNetwork}. A bad value or a target that is not a missile is dropped (CORE-40). Side: sent by the
 * server, handled by the client.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-entity.md (Client state),
// docs/gregguns-spec/01-core/networking-gtnhlib.md
public final class PacketMissileState extends ClientboundPacket {

    private int entityId;
    private int phase;
    private int casing;
    private int fuel;
    private int nuclear;
    private int tick;
    private int lead;
    private int total;

    /** Required by the channel. */
    public PacketMissileState() {}

    /**
     * @param missile the missile
     */
    public PacketMissileState(EntityMissile missile) {
        this.entityId = missile.getEntityId();
        this.phase = missile.phase()
            .ordinal();
        this.casing = missile.casingNumber();
        this.fuel = missile.fuelMb();
        this.nuclear = "nuclear".equals(missile.explosiveId()) ? 1 : 0;
        this.tick = missile.warmupTick();
        this.lead = missile.warmupLead();
        this.total = missile.warmupTotal();
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(phase);
        buf.writeByte(casing);
        buf.writeInt(fuel);
        buf.writeByte(nuclear);
        buf.writeInt(tick);
        buf.writeInt(lead);
        buf.writeInt(total);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        phase = buf.readByte();
        casing = buf.readByte();
        fuel = buf.readInt();
        nuclear = buf.readByte();
        tick = buf.readInt();
        lead = buf.readInt();
        total = buf.readInt();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("phase", phase, 0, FlightPhase.values().length - 1)
            .inRange("casing", casing, 1, 5)
            .inRange("nuclear", nuclear, 0, 1)
            .inRange("fuel", fuel, 0, 10_000_000)
            .inRange("tick", tick, 0, 100_000)
            .inRange("lead", lead, 0, 100_000)
            .inRange("total", total, 0, 100_000)
            .targetOfType("missile", () -> {
                Entity e = WorldTargets.entity(world, entityId);
                return e instanceof EntityMissile ? e : null;
            }, Entity.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        EntityMissile m = (EntityMissile) world.getEntityByID(entityId);
        m.applyState(FlightPhase.values()[phase], casing, fuel, nuclear == 1);
        m.applyCountdown(tick, lead, total);
    }
}
