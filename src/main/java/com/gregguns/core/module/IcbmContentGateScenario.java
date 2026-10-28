package com.gregguns.core.module;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.explosion.nuclear.ItemFalloutProduct;

/**
 * The content only the ICBM module can make (the Radar Casing, trinitite, fallout goo, hot rubble) belongs to the ICBM
 * module even though the casing block and the fallout registration live elsewhere, so the module being off hides it
 * from NEI and the creative tab. Runs in either configuration: with ICBM off every stack must report the disabled
 * module, with ICBM on none may. Plain irradiated rubble and earth stay with the guns module. Server side.
 */
// Spec: docs/gregguns-spec/06-items/nei-visibility.md
public final class IcbmContentGateScenario implements Scenario {

    @Override
    public String name() {
        return "icbm.content_gate";
    }

    @Override
    public String description() {
        return "The Radar Casing, trinitite, the goo block and items and hot rubble are ICBM content: hidden and unusable"
            + " exactly when the ICBM module is inactive; plain rubble and irradiated earth stay with guns.";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        boolean icbmOff = !ModuleState.isActive(ModuleId.ICBM);
        out.note("ICBM module " + (icbmOff ? "inactive" : "active"));
        ItemStack[] icbmOnly = {
            new ItemStack(Item.getItemFromBlock(GregGunsCasings.INSTANCE), 1, GregGunsCasings.RADAR_CASING),
            new ItemStack(Item.getItemFromBlock(FalloutBlocks.trinitite()), 1, 0),
            new ItemStack(Item.getItemFromBlock(FalloutBlocks.goo()), 1, 0),
            new ItemStack(Item.getItemFromBlock(FalloutBlocks.goo()), 1, 1),
            new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.SHARD),
            new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.CRUSHED),
            new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.GOO),
            new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.HOT_GOO),
            new ItemStack(FalloutBlocks.rubble(), 1, 1) };
        for (ItemStack s : icbmOnly) {
            String label = s.getDisplayName() + " (" + s.getUnlocalizedName() + "/" + s.getItemDamage() + ")";
            out.checkEquals(label + ": owned by ICBM", ModuleId.ICBM, ModuleContent.moduleOf(s));
            out.checkEquals(label + ": disabled exactly when ICBM is inactive", icbmOff, !ModuleContent.isUsable(s));
        }
        ItemStack[] guns = { new ItemStack(FalloutBlocks.rubble(), 1, 0),
            new ItemStack(Item.getItemFromBlock(FalloutBlocks.irradiated()), 1, FalloutBlocks.DIRT),
            new ItemStack(Item.getItemFromBlock(GregGunsCasings.INSTANCE), 1, GregGunsCasings.BASTION_IRON) };
        for (ItemStack s : guns) {
            String label = s.getUnlocalizedName() + "/" + s.getItemDamage();
            out.check(ModuleContent.moduleOf(s) != ModuleId.ICBM, label + ": not ICBM content");
        }
        return out.build();
    }
}
