package com.gregguns.icbm;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.value.sync.StringSyncValue;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Sync values the ICBM screens share.
 *
 * <p>
 * ModularUI2 drops a client-to-server write unless the value says it allows it ("Sync handler is unable to send
 * packets to server"). Every value the player edits on an ICBM screen goes through {@link #editableText}, so none of
 * them forgets. The setter still runs on the server, where it parses, clamps and checks the player.
 */
public final class IcbmSync {

    private IcbmSync() {}

    /**
     * A text value the client may write.
     *
     * @param getter reads the current value (server side)
     * @param setter takes the typed text and must validate it itself
     * @return the sync value, with client-to-server writes allowed
     */
    public static StringSyncValue editableText(Supplier<String> getter, Consumer<String> setter) {
        return new StringSyncValue(getter, setter).allowC2S();
    }

    /**
     * Whether a machine still has a live tile under it. A GUI keeps syncing on the server after its block is broken
     * or its chunk unloads; every getter on that path must check this first (GT clears the machine's base tile).
     *
     * @param machine the machine behind an open screen
     * @return true when the machine has a base tile that is not invalid
     */
    public static boolean live(IMetaTileEntity machine) {
        IGregTechTileEntity base = machine == null ? null : machine.getBaseMetaTileEntity();
        // GT's own close test for an open screen (MetaTileEntityGuiHandler.canInteractWith): it covers a broken block
        // and an unloaded chunk, and a base that now hosts a different machine.
        return base != null && !(base instanceof TileEntity && ((TileEntity) base).isInvalid())
            && base.canAccessData()
            && base.getMetaTileEntity() == machine;
    }

    /** @return a getter that yields {@code fallback} once the machine's tile is gone */
    public static IntSupplier guard(IMetaTileEntity machine, IntSupplier getter, int fallback) {
        return () -> live(machine) ? getter.getAsInt() : fallback;
    }

    /** @return a getter that yields {@code fallback} once the machine's tile is gone */
    public static LongSupplier guard(IMetaTileEntity machine, LongSupplier getter, long fallback) {
        return () -> live(machine) ? getter.getAsLong() : fallback;
    }

    /** @return a getter that yields {@code fallback} once the machine's tile is gone */
    public static Supplier<String> guard(IMetaTileEntity machine, Supplier<String> getter, String fallback) {
        return () -> live(machine) ? getter.get() : fallback;
    }
}
