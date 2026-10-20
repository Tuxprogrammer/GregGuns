package com.gregguns.entity.npc;

import java.io.IOException;

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
            .targetOfType("npc", () -> WorldTargets.entity(world, entityId), NpcEntity.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        ((NpcEntity) world.getEntityByID(entityId)).setAiming(aiming != 0); // safe: validated
    }
}
