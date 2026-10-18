package com.gregguns.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gregguns.network.validation.Sender;

/**
 * The real {@link Sender}: a thin adapter over the {@link EntityPlayerMP} that sent a packet. Authorisation hooks that
 * need the player (for example a turret owner check) cast their {@link Sender} to this and call {@link #player()}.
 *
 * Side: server.
 */
public final class PlayerSender implements Sender {

    private final EntityPlayerMP player;

    /** Wraps the sending player. */
    public PlayerSender(EntityPlayerMP player) {
        this.player = player;
    }

    /** Returns the sending player. */
    public EntityPlayerMP player() {
        return player;
    }

    @Override
    public String name() {
        return player.getCommandSenderName();
    }

    @Override
    public boolean isAlive() {
        return player.isEntityAlive() && player.playerNetServerHandler != null;
    }

    /**
     * 1.7.10 has no spectator mode, so this covers the equivalents: a no-clip player, or a player who can neither take
     * damage nor edit the world.
     */
    @Override
    public boolean isSpectatorLike() {
        return player.noClip || (player.capabilities.disableDamage && !player.capabilities.allowEdit);
    }

    @Override
    public int dimensionId() {
        return player.worldObj.provider.dimensionId;
    }

    @Override
    public double x() {
        return player.posX;
    }

    @Override
    public double y() {
        return player.posY;
    }

    @Override
    public double z() {
        return player.posZ;
    }
}
