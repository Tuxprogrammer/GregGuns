package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

/** The login sync: the packet's wire form and the client applying the server's values. */
class ModuleSyncTest {

    @AfterEach
    void reset() {
        ModuleState.clearServerMask();
    }

    @Test
    void theMaskSurvivesTheWire() throws IOException {
        for (int mask : new int[] { 0, 1, 0b1010101, ModuleState.maxMask() }) {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            new PacketModuleSync(mask).encode(buf);
            assertEquals(4, buf.readableBytes());
            PacketModuleSync back = new PacketModuleSync();
            back.decode(buf);
            assertEquals(mask, back.mask());
        }
    }

    @Test
    void theServerSendsItsOwnResolvedMask() {
        assertEquals(ModuleState.localMask(), ModuleSyncLogic.serverMask());
    }

    @Test
    void aClientWithEverythingOffFollowsAServerWithEverythingOn() {
        int server = ModuleState.maxMask();
        List<String> diffs = ModuleSyncLogic.apply(server, 0);
        assertEquals(ModuleId.values().length, diffs.size(), "every module differs, and is only reported");
        for (ModuleId id : ModuleId.values()) {
            assertTrue(ModuleState.isActive(id), id.toString());
        }
    }

    @Test
    void theClientDropsTheServerValuesOnDisconnect() {
        ModuleSyncLogic.apply(0, ModuleState.localMask());
        assertFalse(ModuleState.isActive(ModuleId.GUNS));
        ModuleState.clearServerMask();
        assertEquals(ModuleState.localMask(), ModuleState.currentMask());
    }
}
