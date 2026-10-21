package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ModuleStateTest {

    @AfterEach
    void reset() {
        ModuleState.clearServerMask();
    }

    private static Predicate<ModuleId> only(ModuleId... on) {
        Set<ModuleId> set = EnumSet.noneOf(ModuleId.class);
        set.addAll(Arrays.asList(on));
        return set::contains;
    }

    @Test
    void aFlagOnWithItsRequirementsOffIsInactive() {
        // worldgen needs npcs needs guns: the dependency rule is behavior, not registration
        Predicate<ModuleId> flag = only(ModuleId.GUNS, ModuleId.WORLDGEN);
        assertTrue(ModuleState.isActiveLocally(ModuleId.GUNS, flag));
        assertFalse(ModuleState.isActiveLocally(ModuleId.NPCS, flag));
        assertFalse(ModuleState.isActiveLocally(ModuleId.WORLDGEN, flag));
    }

    @Test
    void aChainStaysActiveWhileEveryLinkIsOn() {
        Predicate<ModuleId> flag = only(ModuleId.GUNS, ModuleId.NPCS, ModuleId.WORLDGEN);
        assertTrue(ModuleState.isActiveLocally(ModuleId.WORLDGEN, flag));
        assertEquals(
            (1 << ModuleId.GUNS.ordinal()) | (1 << ModuleId.NPCS.ordinal()) | (1 << ModuleId.WORLDGEN.ordinal()),
            ModuleState.maskOf(flag));
    }

    @Test
    void everyFlagCombinationYieldsAMaskThatFitsThePacket() {
        int combos = 1 << ModuleId.values().length;
        for (int flags = 0; flags < combos; flags++) {
            final int f = flags;
            int mask = ModuleState.maskOf(id -> (f & (1 << id.ordinal())) != 0);
            assertTrue(mask >= 0 && mask <= ModuleState.maxMask());
            assertEquals(mask, mask & f, "an active module always has its own flag on");
        }
    }

    @Test
    void theServerMaskOverridesTheLocalFlagsUntilCleared() {
        int local = ModuleState.localMask();
        assertFalse(ModuleState.usingServerValues());
        ModuleState.applyServerMask(0);
        assertTrue(ModuleState.usingServerValues());
        for (ModuleId id : ModuleId.values()) {
            assertFalse(ModuleState.isActive(id), id.toString());
        }
        assertEquals(0, ModuleState.currentMask());
        ModuleState.applyServerMask(1 << ModuleId.NPCS.ordinal());
        assertTrue(ModuleState.isActive(ModuleId.NPCS));
        assertFalse(ModuleState.isActive(ModuleId.GUNS));
        ModuleState.clearServerMask();
        assertEquals(local, ModuleState.currentMask());
    }

    @Test
    void aMaskWithUnknownBitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ModuleState.applyServerMask(ModuleState.maxMask() + 1));
        assertThrows(IllegalArgumentException.class, () -> ModuleState.applyServerMask(-1));
        assertFalse(ModuleState.usingServerValues());
    }

    @Test
    void disabledLineNamesTheFlag() {
        assertEquals(
            "Disabled by the server's GregGuns config (modules.npcs)",
            ModuleState.disabledLine(ModuleId.NPCS));
    }
}
