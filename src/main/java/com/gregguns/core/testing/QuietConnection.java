package com.gregguns.core.testing;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;

/**
 * Gives a scenario's fake player a network handler that drops everything sent to it. Dev environment only. Server side.
 *
 * <p>
 * A {@code NetworkManager} built with {@code new NetworkManager(false)} has no channel until netty activates it. Mods
 * that message a player through an FML channel when it joins the world (Witchery's shapeshift sync on
 * {@code EntityJoinWorldEvent}) then fail on the null channel, and FML logs an error pair for every fake player a
 * scenario spawns (I6b review m3, M9 soak W2). Vanilla packets on such a manager pile up in its outbound queue for as
 * long as the player lives. Here the manager sits on an open {@link EmbeddedChannel} whose only outbound handler
 * drops every write, so both paths end quietly.
 *
 * <p>
 * Source: Forge 1.7.10-10.13.4.1614 {@code cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER}
 * reads the player's {@code NetworkDispatcher} from {@code playerNetServerHandler.netManager.channel()} and sends to
 * nobody when the channel has none (checked with {@code javap} against the pack's Forge jar: the null test follows the
 * attribute read). Vanilla 1.7.10 {@code NetworkManager.channelActive} stores the channel, which
 * {@code EmbeddedChannel}'s constructor fires.
 */
public final class QuietConnection {

    private QuietConnection() {}

    /**
     * @param player a fake player without a network handler
     * @return the handler now set on it (the constructor installs it as {@code playerNetServerHandler})
     */
    public static NetHandlerPlayServer attach(EntityPlayerMP player) {
        NetworkManager manager = new NetworkManager(false);
        // head -> Drop -> manager: a write from the manager or the channel reaches Drop and goes no further.
        new EmbeddedChannel(new Drop(), manager);
        return new NetHandlerPlayServer(MinecraftServer.getServer(), manager, player) {

            @Override
            public void sendPacket(Packet packet) {}
        };
    }

    /** Swallows every outbound message. */
    private static final class Drop extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            ReferenceCountUtil.release(msg);
            promise.setSuccess();
        }
    }
}
