package com.gregguns.core.module;

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client, once at login: which modules the server runs. A client whose {@code modules.*} switches differ
 * cannot register the server's content; this logs and shows the difference instead of leaving the player to find a
 * broken item. It does not disconnect: the client stays safe through inert MTE placeholders, and the modules are
 * startup-only, so the fix is the config file.
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

    /** @param mask the server's enabled modules, see {@link ModuleMismatch#mask} */
    public PacketModuleSync(int mask) {
        this.mask = mask;
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
        rules.inRange("mask", mask, 0, ModuleMismatch.maxMask());
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        List<String> diffs = ModuleMismatch.differences(mask, GregGuns.proxy.enabledModuleMask());
        if (diffs.isEmpty()) {
            return;
        }
        GregGuns.LOG.error(
            "GregGuns module mismatch with the server: {}. Machines of the missing modules show as inert placeholders;"
                + " set the same modules.* values in config/gregguns.cfg and restart.",
            diffs);
        localPlayer.addChatMessage(
            new ChatComponentText(
                "[GregGuns] This client's modules.* differ from the server's (" + String.join("; ", diffs)
                    + "). Match config/gregguns.cfg and restart."));
    }
}
