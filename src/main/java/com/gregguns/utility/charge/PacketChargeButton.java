package com.gregguns.utility.charge;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.network.PlayerSender;
import com.gregguns.network.ServerboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.PacketOutcome;
import com.gregguns.network.validation.PacketPipeline;
import com.gregguns.network.validation.RejectionSink;
import com.gregguns.network.validation.Sender;
import com.gregguns.network.validation.SenderRules;
import com.gregguns.network.validation.Violation;

/**
 * Client to server: a button of the charge GUI (arm, radius, length, fuse). Validation: same dimension, within reach, a
 * known button, a charge at the position, and the sender is the owner or on the owner's ServerUtilities team. The last
 * rule is the fix for MUTL-31, where the baseline let anyone change or arm any charge by sending the button id; it
 * covers the arm button as well. Side: the client sends, the server handles.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-15), docs/techguns-spec/BUGS.md (MUTL-31)
public final class PacketChargeButton extends ServerboundPacket {

    private int dimension;
    private int x;
    private int y;
    private int z;
    private int button;

    /** Required by the channel. */
    public PacketChargeButton() {}

    /**
     * @param dimension the sender's dimension
     * @param x         charge x
     * @param y         charge y
     * @param z         charge z
     * @param button    a {@code ChargeParams.BTN_*} id
     */
    public PacketChargeButton(int dimension, int x, int y, int z, int button) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.button = button;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(button);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        dimension = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        button = buf.readByte();
    }

    private static TileExplosiveCharge chargeAt(World world, int x, int y, int z) {
        Object tile = WorldTargets.tile(world, x, y, z);
        return tile instanceof TileExplosiveCharge ? (TileExplosiveCharge) tile : null;
    }

    @Override
    protected void validate(SenderRules rules) {
        World world = worldOf(rules);
        rules.sameDimension(dimension)
            .withinReachOfBlock(x, y, z)
            .inRange("button", button, 0, ChargeParams.BUTTON_COUNT - 1)
            .targetOfType("explosiveCharge", () -> chargeAt(world, x, y, z), TileExplosiveCharge.class);
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        EntityPlayerMP player = ((PlayerSender) sender).player();
        TileExplosiveCharge charge = chargeAt(player.worldObj, x, y, z);
        return charge != null && charge.mayConfigure(player);
    }

    @Override
    protected void handle(EntityPlayerMP player) {
        // Safe: validate proved a charge is here.
        chargeAt(player.worldObj, x, y, z).press(button);
    }

    /**
     * Runs the whole server pipeline for a player without a network connection, so scenarios can drive the packet
     * path. The network handler runs the same steps.
     *
     * @param player the sender
     * @return what happened to the packet
     */
    public PacketOutcome applyAs(EntityPlayerMP player) {
        Sender sender = new PlayerSender(player);
        String name = getClass().getSimpleName();
        return PacketPipeline.run(name, sender.name(), new SenderRules(sender), rules -> {
            rules.senderMayAct();
            validate(rules);
            rules.authorized(name, this::isAuthorized);
        }, () -> handle(player), new DebugSink());
    }

    /** Logs drops at debug level; the shared rate-limited sink of the network package is not visible here. */
    private static final class DebugSink implements RejectionSink {

        @Override
        public void rejected(String packet, String origin, Violation violation) {
            GregGuns.LOG.debug("Dropped {} from {}: {}", packet, origin, violation);
        }

        @Override
        public void handlerFailed(String packet, String origin, RuntimeException error) {
            GregGuns.LOG.error("Handler of {} failed for {}", packet, origin, error);
        }

        @Override
        public void suppressed(long count) {
            GregGuns.LOG.debug("{} further drops suppressed", count);
        }
    }
}
