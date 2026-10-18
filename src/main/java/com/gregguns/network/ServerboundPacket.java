package com.gregguns.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.PacketPipeline;
import com.gregguns.network.validation.Rejection;
import com.gregguns.network.validation.RejectionSink;
import com.gregguns.network.validation.Sender;
import com.gregguns.network.validation.SenderRules;
import com.gregguns.network.validation.Violation;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import io.netty.buffer.ByteBuf;

/**
 * Base class for every client-to-server packet. It guarantees, for all subclasses at once:
 * <ul>
 * <li>{@link #handle} runs on the server main thread, never on the Netty thread (CORE-05). GTNHLib's
 * {@code IPacket.onMessage} reschedules before calling {@code executeServer}, which this class implements.</li>
 * <li>Validation always runs first: the sender must be alive and not spectator-like, then the rules declared in
 * {@link #validate}, then {@link #isAuthorized}. A failed packet is dropped and logged at debug level through a rate
 * limiter (CORE-06, CORE-08, CORE-40).</li>
 * <li>Nothing is thrown into Netty: a decode error marks the packet malformed and drops it, and an exception in
 * validation or in {@link #handle} is contained and logged.</li>
 * </ul>
 *
 * Subclasses need a public no-argument constructor (the channel creates instances to decode into). Validation
 * declarations are plain Java, see {@link SenderRules}; the lookup helpers are in {@link WorldTargets}.
 *
 * How to write one:
 *
 * <pre>
 * public class PacketSelectSlot extends ServerboundPacket {
 *
 *     private int dimension, x, y, z, slot;
 *
 *     public PacketSelectSlot() {}
 *
 *     public void encode(PacketBuffer buf) { buf.writeInt(dimension); ... buf.writeByte(slot); }
 *
 *     public void decode(PacketBuffer buf) { dimension = buf.readInt(); ... slot = buf.readByte(); }
 *
 *     protected void validate(SenderRules rules) {
 *         World world = worldOf(rules);
 *         rules.sameDimension(dimension)
 *             .withinReachOfBlock(x, y, z)
 *             .indexInRange("slot", slot, MachineTile.SLOTS)
 *             .targetOfType("machine", () -&gt; WorldTargets.tile(world, x, y, z), MachineTile.class);
 *     }
 *
 *     protected void handle(EntityPlayerMP player) {
 *         MachineTile tile = (MachineTile) player.worldObj.getTileEntity(x, y, z); // safe: validated above
 *         tile.selectSlot(slot);
 *     }
 * }
 * </pre>
 *
 * Side: server (the class is loaded on both sides, but only the server executes it).
 */
public abstract class ServerboundPacket implements IPacket {

    /** Why decoding failed, or {@code null} when it succeeded. Set on the Netty thread, read on the main thread. */
    private volatile String decodeError;

    /**
     * Declares the validation rules for this packet's own fields. Called on the main thread after decoding; the
     * always-on sender checks have already been added, and {@link #isAuthorized} runs after these rules. Declare cheap
     * range checks before target lookups.
     *
     * @param rules where to declare; {@link SenderRules#sender()} is the player
     */
    protected abstract void validate(SenderRules rules);

    /**
     * Does the packet's work. Runs on the server main thread and only after every rule passed, so validated targets
     * can be cast without further checks.
     *
     * @param player the sending player
     */
    protected abstract void handle(EntityPlayerMP player);

    /**
     * Hook for owner and permission checks that later milestones fill in (for example turret ownership). Runs after
     * {@link #validate}, so the target is known to exist. The default allows everyone who passed the other rules.
     *
     * @param sender the sender; cast to {@link PlayerSender} to reach the {@link EntityPlayerMP}
     * @return {@code true} to let the packet through
     */
    protected boolean isAuthorized(Sender sender) {
        return true;
    }

    /**
     * Returns the world the sender is in, for target lookups inside {@link #validate}.
     *
     * @param rules the rules passed to {@link #validate}; its sender must be a {@link PlayerSender}
     */
    protected static World worldOf(SenderRules rules) {
        return ((PlayerSender) rules.sender()).player().worldObj;
    }

    /**
     * Decodes without ever throwing into Netty. A malformed packet is remembered and dropped at execution time.
     */
    @Override
    public final void fromBytes(ByteBuf buf) {
        try {
            decode(new PacketBuffer(buf));
        } catch (IOException | RuntimeException e) {
            decodeError = e.toString();
        }
    }

    /** Entry point called by GTNHLib on the main thread. Do not override. */
    @Override
    public final IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP player = handler.playerEntity;
        if (player == null) {
            return null;
        }
        dispatch(new PlayerSender(player), () -> handle(player), GregGunsNetwork.rejections());
        return null;
    }

    /**
     * The whole server-side pipeline with the Minecraft glue removed, so unit tests can drive it.
     *
     * @param sender  the sender
     * @param handler what {@link #handle} would do
     * @param sink    where drops are reported
     * @return what happened to the packet
     */
    final PacketOutcome dispatch(Sender sender, Runnable handler, RejectionSink sink) {
        String name = getClass().getSimpleName();
        if (decodeError != null) {
            Violation malformed = new Violation(Rejection.MALFORMED, decodeError);
            sink.rejected(name, sender.name(), malformed);
            return PacketOutcome.rejected(malformed);
        }
        return PacketPipeline.run(name, sender.name(), new SenderRules(sender), rules -> {
            rules.senderMayAct();
            validate(rules);
            rules.authorized(name, this::isAuthorized);
        }, handler, sink);
    }
}
