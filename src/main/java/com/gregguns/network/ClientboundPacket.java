package com.gregguns.network;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.validation.ClientRules;
import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.PacketPipeline;
import com.gregguns.network.validation.Rejection;
import com.gregguns.network.validation.RejectionSink;
import com.gregguns.network.validation.Violation;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Base class for every server-to-client packet (server sync, effects). It guarantees:
 * <ul>
 * <li>{@link #handle} runs on the client main thread (GTNHLib reschedules it, CORE-05).</li>
 * <li>Entity ids and enum ordinals are checked through {@link #validate} before use, and a packet that fails is
 * ignored rather than throwing (CORE-40, GGS-01-10).</li>
 * <li>Nothing is thrown into Netty or the client tick.</li>
 * <li>Side safety: this class references client classes only inside the one {@code @SideOnly(CLIENT)} method, so it
 * loads on a dedicated server. {@link #handle} receives common types only; a handler that needs a client class must
 * go through the proxy ({@code GregGuns.proxy}) or a {@code @SideOnly(Side.CLIENT)} helper under
 * {@code com.gregguns.client}.</li>
 * </ul>
 *
 * Subclasses need a public no-argument constructor. Send them with
 * {@code GregGunsNetwork.channel().sendTo(packet, player)} or one of the other {@code send*} methods.
 *
 * How to write one:
 *
 * <pre>
 * public class PacketReloadStarted extends ClientboundPacket {
 *
 *     private int entityId, phase;
 *
 *     public PacketReloadStarted() {}
 *
 *     public void encode(PacketBuffer buf) { buf.writeInt(entityId); buf.writeByte(phase); }
 *
 *     public void decode(PacketBuffer buf) { entityId = buf.readInt(); phase = buf.readByte(); }
 *
 *     protected void validate(ClientRules rules, World world) {
 *         rules.ordinalInRange("phase", phase, ReloadPhase.class)
 *             .targetOfType("shooter", () -&gt; WorldTargets.entity(world, entityId), EntityLivingBase.class);
 *     }
 *
 *     protected void handle(World world, EntityPlayer localPlayer) {
 *         EntityLivingBase shooter = (EntityLivingBase) world.getEntityByID(entityId); // safe: validated
 *         ...
 *     }
 * }
 * </pre>
 *
 * Side: client executes; the class loads on both.
 */
public abstract class ClientboundPacket implements IPacket {

    private volatile String decodeError;

    /**
     * Declares the validation rules for this packet's fields. Called on the client main thread.
     *
     * @param rules where to declare
     * @param world the client world, for target lookups
     */
    protected abstract void validate(ClientRules rules, World world);

    /**
     * Applies the packet. Runs on the client main thread after every rule passed.
     *
     * @param world       the client world
     * @param localPlayer the local player
     */
    protected abstract void handle(World world, EntityPlayer localPlayer);

    /** Decodes without ever throwing into Netty. A malformed packet is remembered and ignored later. */
    @Override
    public final void fromBytes(ByteBuf buf) {
        try {
            decode(new PacketBuffer(buf));
        } catch (IOException | RuntimeException e) {
            decodeError = e.toString();
        }
    }

    /** Entry point called by GTNHLib on the client main thread. Do not override. */
    @Override
    @SideOnly(Side.CLIENT)
    public final IPacket executeClient(NetHandlerPlayClient handler) {
        Minecraft mc = Minecraft.getMinecraft();
        dispatch(mc.theWorld, mc.thePlayer, GregGunsNetwork.rejections());
        return null;
    }

    /**
     * The client-side pipeline without the {@code Minecraft} lookup, so tests can drive it.
     *
     * @param world       the client world, possibly {@code null} while disconnecting
     * @param localPlayer the local player, possibly {@code null}
     * @param sink        where drops are reported
     * @return what happened to the packet
     */
    final PacketOutcome dispatch(World world, EntityPlayer localPlayer, RejectionSink sink) {
        String name = getClass().getSimpleName();
        Violation early = null;
        if (decodeError != null) {
            early = new Violation(Rejection.MALFORMED, decodeError);
        } else if (world == null || localPlayer == null) {
            early = new Violation(Rejection.TARGET_MISSING, "no client world or player");
        }
        if (early != null) {
            sink.rejected(name, "server", early);
            return PacketOutcome.rejected(early);
        }
        return PacketPipeline.run(
            name,
            "server",
            new ClientRules(),
            rules -> validate(rules, world),
            () -> handle(world, localPlayer),
            sink);
    }
}
