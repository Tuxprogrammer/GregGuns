package com.gregguns.gun.net;

import java.io.IOException;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.gun.GunClientHooks;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client: entity X fired, started or ended a reload, or (to the shooter only) changed zoom. Other clients
 * start the matching animation; the shooter's own client uses it to correct its prediction.
 *
 * <h2>Who receives what</h2>
 * The server sends {@link GunEventKind#SHOT_FIRED} to the players tracking the shooter, <em>not</em> the shooter, who
 * predicted it. Reload events go to the trackers and to the shooter. Zoom goes to the shooter only. Sounds do not
 * travel in this packet; the server plays them with {@code GunSoundPlayer}.
 *
 * <p>
 * Wire: entity id (int), kind (byte), argument (short). The packet names no gun: the receiver reads the entity's held
 * item, so it cannot show a gun the entity does not hold.
 *
 * <p>
 * Spec: docs/techguns-spec/01-core/networking.md (GunFiredMessage, ReloadStartedMessage). Fixes CORE-40: a missing
 * entity, wrong entity type or bad kind index is dropped, not cast. Side: sent by the server, handled by the client.
 */
public final class PacketGunEvent extends ClientboundPacket {

    /** Longest reload the packet will announce, ticks (one minute). */
    static final int MAX_ARGUMENT = 1200;

    private int entityId;
    private int kind;
    private int argument;

    /** Required by the channel. */
    public PacketGunEvent() {}

    /**
     * @param entity   the entity the event is about
     * @param kind     what happened
     * @param argument kind-specific value, see {@link GunEventKind}
     */
    public PacketGunEvent(EntityLivingBase entity, GunEventKind kind, int argument) {
        this.entityId = entity.getEntityId();
        this.kind = kind.ordinal();
        this.argument = Math.max(0, Math.min(MAX_ARGUMENT, argument));
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(kind);
        buf.writeShort(argument);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        kind = buf.readByte();
        argument = buf.readShort();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.ordinalInRange("kind", kind, GunEventKind.class)
            .inRange("argument", argument, 0, MAX_ARGUMENT)
            .targetOfType("shooter", () -> WorldTargets.entity(world, entityId), EntityLivingBase.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        EntityLivingBase entity = (EntityLivingBase) world.getEntityByID(entityId); // safe: validated
        boolean self = entity == localPlayer;
        GunStats stats = heldStats(entity);
        switch (GunEventKind.values()[kind]) {
            case SHOT_FIRED:
                if (!self && stats != null) {
                    GunClientHooks.events()
                        .onShotFired(entity, stats, argument);
                }
                break;
            case RELOAD_STARTED:
                if (self) {
                    GunClientHooks.sync()
                        .serverReloadStarted(entity, argument);
                } else if (stats != null) {
                    GunClientHooks.events()
                        .onReloadStarted(entity, stats, argument);
                }
                break;
            case RELOAD_ENDED:
                if (self) {
                    GunClientHooks.sync()
                        .serverReloadEnded(entity);
                } else {
                    GunClientHooks.events()
                        .onReloadCancelled(entity);
                }
                break;
            case ZOOM:
                if (self) {
                    GunClientHooks.sync()
                        .serverZoom(entity, argument != 0);
                }
                break;
            default:
                break;
        }
    }

    private static GunStats heldStats(EntityLivingBase entity) {
        ItemStack held = entity.getHeldItem();
        return held != null && held.getItem() instanceof ItemGun ? ((ItemGun) held.getItem()).stats() : null;
    }
}
