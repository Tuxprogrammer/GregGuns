package com.gregguns.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.gregguns.explosion.nuclear.FalloutRegistration;
import com.gregguns.machine.MteModules;

/** The module-membership rules that decide what NEI and the creative tab hide while a module is off. */
class ModuleContentTest {

    @Test
    void anItemKeepsItsRegisteringModuleUnlessADamageRuleClaimsTheStack() {
        Item item = new Item();
        ModuleContent.assign(item, ModuleId.GUNS);
        ModuleContent.assignByDamage(item, FalloutRegistration::rubbleOwner);
        assertEquals(ModuleId.GUNS, ModuleContent.moduleOf(new ItemStack(item, 1, 0)));
        assertEquals(ModuleId.ICBM, ModuleContent.moduleOf(new ItemStack(item, 1, 1)));
    }

    @Test
    void anItemWholeAssignedToIcbmIsIcbmContentAtEveryDamage() {
        Item item = new Item();
        ModuleContent.assign(item, ModuleId.GUNS);
        ModuleContent.assign(item, ModuleId.ICBM);
        for (int damage = 0; damage < 4; damage++) {
            assertEquals(ModuleId.ICBM, ModuleContent.moduleOf(new ItemStack(item, 1, damage)));
        }
    }

    @Test
    void anUnassignedItemBelongsToNoModule() {
        assertNull(ModuleContent.moduleOf(new ItemStack(new Item())));
        assertNull(ModuleContent.moduleOf(null));
    }

    @Test
    void onlyHotRubbleIsIcbmContent() {
        assertNull(FalloutRegistration.rubbleOwner(0));
        assertEquals(ModuleId.ICBM, FalloutRegistration.rubbleOwner(1));
    }

    @Test
    void theRadarCasingIsIcbmContentAndTheOtherCasingsKeepTheirModules() {
        int radar = com.gregguns.block.GregGunsCasings.RADAR_CASING;
        assertEquals(ModuleId.ICBM, MteModules.casingModule(radar));
        for (int meta = 0; meta < com.gregguns.block.GregGunsCasings.META_COUNT; meta++) {
            if (meta == radar) continue;
            ModuleId owner = MteModules.casingModule(meta);
            assertEquals(false, owner == ModuleId.ICBM, "casing meta " + meta);
        }
        assertEquals(ModuleId.UTILITY, MteModules.casingModule(com.gregguns.block.GregGunsCasings.BASTION_CARBON));
    }
}
