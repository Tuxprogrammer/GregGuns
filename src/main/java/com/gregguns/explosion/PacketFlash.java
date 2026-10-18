package com.gregguns.explosion;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to one client: a flashbang went off in view, white out the screen for this many ticks. The potion effects
 * (blindness, nausea) travel by vanilla; this only adds the white flash they cannot draw.
 *
 * <p>
 * Wire: ticks (short). Side: sent by the server, handled by the client through {@link FlashHooks}.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang)
public final class PacketFlash extends ClientboundPacket {

    private int ticks;

    /** Required by the channel. */
    public PacketFlash() {}

    /**
     * @param ticks length of the effect, 1 to {@link FlashMath#MAX_TICKS}
     */
    public PacketFlash(int ticks) {
        this.ticks = Math.max(1, Math.min(FlashMath.MAX_TICKS, ticks));
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeShort(ticks);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        ticks = buf.readShort();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("ticks", ticks, 1, FlashMath.MAX_TICKS);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        FlashHooks.sink()
            .flash(ticks);
    }
}
