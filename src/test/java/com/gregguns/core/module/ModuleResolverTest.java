package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ModuleResolverTest {

    private static Set<ModuleId> on(ModuleId... ids) {
        return EnumSet.copyOf(Arrays.asList(ids));
    }

    @Test
    void nothingRegisteredResolvesToNothing() {
        ModuleResolution result = ModuleResolver.resolve(Collections.<ModuleId>emptyList(), id -> true);
        assertTrue(
            result.enabled()
                .isEmpty());
        assertTrue(
            result.skipped()
                .isEmpty());
    }

    @Test
    void shippedDefaultsResolveCleanly() {
        ModuleResolution result = ModuleResolver
            .resolve(Arrays.asList(ModuleId.values()), com.gregguns.core.config.ModuleFlags::isEnabled);
        assertEquals(
            Arrays.asList(
                ModuleId.GUNS,
                ModuleId.ARMOR,
                ModuleId.MACHINES,
                ModuleId.ENERGY_WEAPONS,
                ModuleId.UTILITY,
                ModuleId.NPCS,
                ModuleId.WORLDGEN),
            result.enabled());
        // icbm ships off until its beta gate; nothing else is skipped
        assertEquals(
            Arrays.asList(ModuleId.ICBM),
            new java.util.ArrayList<>(
                result.skipped()
                    .keySet()));
    }

    @Test
    void disabledModuleIsSkippedByConfig() {
        ModuleResolution result = ModuleResolver.resolve(Arrays.asList(ModuleId.GUNS), id -> false);
        assertTrue(
            result.enabled()
                .isEmpty());
        assertEquals(
            ModuleResolution.SkipReason.DISABLED_BY_CONFIG,
            result.skipped()
                .get(ModuleId.GUNS));
    }

    @Test
    void enabledModulesRunInDeclarationOrderWhateverTheRegistrationOrder() {
        List<ModuleId> registered = Arrays.asList(ModuleId.UTILITY, ModuleId.MACHINES, ModuleId.GUNS);
        ModuleResolution result = ModuleResolver.resolve(registered, id -> true);
        assertEquals(Arrays.asList(ModuleId.GUNS, ModuleId.MACHINES, ModuleId.UTILITY), result.enabled());
    }

    @Test
    void moduleWithDisabledRequirementIsSkipped() {
        Set<ModuleId> flags = on(ModuleId.ARMOR);
        ModuleResolution result = ModuleResolver.resolve(Arrays.asList(ModuleId.GUNS, ModuleId.ARMOR), flags::contains);
        assertFalse(result.isEnabled(ModuleId.ARMOR));
        assertEquals(
            ModuleResolution.SkipReason.MISSING_DEPENDENCY,
            result.skipped()
                .get(ModuleId.ARMOR));
        assertEquals(
            ModuleResolution.SkipReason.DISABLED_BY_CONFIG,
            result.skipped()
                .get(ModuleId.GUNS));
    }

    @Test
    void moduleWithUnregisteredRequirementIsSkipped() {
        ModuleResolution result = ModuleResolver.resolve(Arrays.asList(ModuleId.ARMOR), id -> true);
        assertEquals(
            ModuleResolution.SkipReason.MISSING_DEPENDENCY,
            result.skipped()
                .get(ModuleId.ARMOR));
    }

    @Test
    void requirementChainsPropagate() {
        // WORLDGEN needs NPCS needs GUNS; with GUNS off, all three are removed.
        Set<ModuleId> flags = on(ModuleId.NPCS, ModuleId.WORLDGEN);
        ModuleResolution result = ModuleResolver
            .resolve(Arrays.asList(ModuleId.GUNS, ModuleId.NPCS, ModuleId.WORLDGEN), flags::contains);
        assertTrue(
            result.enabled()
                .isEmpty());
        assertEquals(
            ModuleResolution.SkipReason.MISSING_DEPENDENCY,
            result.skipped()
                .get(ModuleId.WORLDGEN));
    }

    @Test
    void fullStackRunsWhenEverythingIsOn() {
        ModuleResolution result = ModuleResolver.resolve(Arrays.asList(ModuleId.values()), id -> true);
        assertEquals(Arrays.asList(ModuleId.values()), result.enabled());
    }

    @Test
    void duplicateRegistrationIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ModuleResolver.resolve(Arrays.asList(ModuleId.GUNS, ModuleId.GUNS), id -> true));
    }

    @Test
    void everyModuleIsDeclaredAfterItsRequirements() {
        for (ModuleId id : ModuleId.values()) {
            for (ModuleId required : id.requires()) {
                assertTrue(required.ordinal() < id.ordinal(), id + " must come after " + required);
            }
        }
    }

    @Test
    void configKeysAreUnique() {
        Set<String> keys = new HashSet<>();
        for (ModuleId id : ModuleId.values()) {
            assertTrue(keys.add(id.configKey()), "duplicate key " + id.configKey());
        }
    }
}
