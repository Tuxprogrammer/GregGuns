package com.gregguns.machine.repair;

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

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Client to server: the player pressed a camo arrow on the Camo Bench. It names the bench, what to cycle and the
 * direction; the server decides everything else. Validation: same dimension, within 8 blocks of the bench, a known
 * target, a Camo Bench at the position, and the player allowed to use that bench ({@code isUseableByPlayer}).
 *
 * <p>
 * Side: the client sends, the server handles.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md (button handling, isUseableByPlayer),
// docs/gregguns-spec/01-core/networking-gtnhlib.md
public final class PacketCamoCycle extends ServerboundPacket {

    private int dimension;
    private int x;
    private int y;
    private int z;
    private int target;
    private boolean back;

    /** Required by the channel. */
    public PacketCamoCycle() {}

    /**
     * @param dimension the sender's dimension
     * @param x         bench block x
     * @param y         bench block y
     * @param z         bench block z
     * @param target    a {@link CamoCycle.Target} ordinal
     * @param back      true to step backward
     */
    public PacketCamoCycle(int dimension, int x, int y, int z, int target, boolean back) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.target = target;
        this.back = back;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(target);
        buf.writeBoolean(back);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        dimension = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        target = buf.readByte();
        back = buf.readBoolean();
    }

    private static MTECamoBench benchAt(World world, int x, int y, int z) {
        Object tile = WorldTargets.tile(world, x, y, z);
        if (tile instanceof IGregTechTileEntity) {
            IMetaTileEntity mte = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (mte instanceof MTECamoBench) {
                return (MTECamoBench) mte;
            }
        }
        return null;
    }

    @Override
    protected void validate(SenderRules rules) {
        World world = worldOf(rules);
        rules.sameDimension(dimension)
            .withinReachOfBlock(x, y, z)
            .ordinalInRange("target", target, CamoCycle.Target.class)
            .targetOfType("camoBench", () -> benchAt(world, x, y, z), MTECamoBench.class);
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        // GT's own access rule: distance, a loaded tile and the machine's owner or private-use settings.
        EntityPlayerMP player = ((PlayerSender) sender).player();
        MTECamoBench bench = benchAt(player.worldObj, x, y, z);
        return bench != null && bench.getBaseMetaTileEntity()
            .isUseableByPlayer(player);
    }

    @Override
    protected void handle(EntityPlayerMP player) {
        // Safe: validate proved a Camo Bench is here.
        benchAt(player.worldObj, x, y, z).cycle(player, CamoCycle.Target.fromOrdinal(target), back);
    }

    /**
     * Runs the whole server pipeline (always-on sender checks, {@link #validate}, {@link #isAuthorized},
     * {@link #handle}) for a player without a network connection, so scenarios and tests can drive the packet path.
     * The network handler runs the same steps.
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
