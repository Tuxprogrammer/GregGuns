package com.gregguns.core.module;

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client, once at login: which modules are active on the server. The client uses these values for behavior
 * and visibility (recipes aside) while connected, and its own flags again after it leaves ({@link ModuleState}).
 * Nothing
 * is registered conditionally, so a client with other flags joins without trouble; the difference is only logged. The
 * rest of the gameplay config is synced by GTNHLib ({@code @Config.Sync}, see docs/config.md).
 *
 * <p>
 * Wire: mask (int, bit per {@link ModuleId} ordinal). Append-only list position: see {@code GregGunsNetwork}. Side:
 * sent by the server, handled by the client.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches)
public final class PacketModuleSync extends ClientboundPacket {

    private int mask;

    /** Required by the channel. */
    public PacketModuleSync() {}

    /** @param mask the server's active modules, see {@link ModuleState#localMask} */
    public PacketModuleSync(int mask) {
        this.mask = mask;
    }

    /** @return the mask this packet carries (for tests) */
    public int mask() {
        return mask;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(mask);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        mask = buf.readInt();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.inRange("mask", mask, 0, ModuleState.maxMask());
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        List<String> diffs = ModuleSyncLogic.apply(mask, ModuleState.localMask());
        if (!diffs.isEmpty()) {
            GregGuns.LOG.info("The server's GregGuns module flags apply here, not this client's: {}", diffs);
        }
        GregGuns.proxy.moduleStateChanged();
    }
}
