package com.gregguns.entity.npc;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to every client tracking an {@link NpcEntity}: whether it is aiming its gun, which drives the raised-arms
 * pose. Sent on change and repeated while aiming, so a client that starts tracking late catches up. It replaces a
 * DataWatcher slot, which GregGuns never claims (GGS-01-11).
 *
 * <p>
 * Wire: entity id (int), aiming (byte, 0 or 1). Append-only list position: see {@code GregGunsNetwork}. A bad value
 * or a target that is not an NPC is dropped (CORE-40). Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-spec/01-core/networking-gtnhlib.md, tools/asset-gen/entity_models.md (MobAnimSource.isAiming)
public final class PacketNpcAim extends ClientboundPacket {

    private int entityId;
    private int aiming;

    /** Required by the channel. */
    public PacketNpcAim() {}

    /**
     * @param npc    the NPC
     * @param aiming whether it is aiming
     */
    public PacketNpcAim(NpcEntity npc, boolean aiming) {
        this.entityId = npc.getEntityId();
        this.aiming = aiming ? 1 : 0;
    }

    /**
     * @param helicopter the Attack Helicopter, which syncs its engaged flag the same way
     * @param aiming     whether it is engaging a target
     */
    public PacketNpcAim(AttackHelicopter helicopter, boolean aiming) {
        this.entityId = helicopter.getEntityId();
        this.aiming = aiming ? 1 : 0;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(aiming);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        aiming = buf.readByte();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("aiming", aiming, 0, 1)
            .targetOfType("npc", () -> {
                Entity e = WorldTargets.entity(world, entityId);
                return e instanceof NpcEntity || e instanceof AttackHelicopter ? e : null;
            }, Entity.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        Entity e = world.getEntityByID(entityId); // safe: validated
        if (e instanceof AttackHelicopter) {
            ((AttackHelicopter) e).setAiming(aiming != 0);
        } else {
            ((NpcEntity) e).setAiming(aiming != 0);
        }
    }
}
