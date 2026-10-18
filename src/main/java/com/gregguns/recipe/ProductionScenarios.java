package com.gregguns.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.machine.MTEAmmoPress;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/**
 * In-game scenarios for the 9mm production chain: the LV Ammo Press exists and can be placed, its map holds the bullet
 * and magazine recipes at the right EU/t and duration, and the assembler holds the part recipes. Dev environment only,
 * registered from {@code GunsModule}. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md, docs/gregguns-spec/06-items/recipes.md
public final class ProductionScenarios {

    private ProductionScenarios() {}

    /**
     * Registers the production scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new AmmoPressScenario());
        registry.register(new BulletRecipeScenario());
        AmmoPressScenarios.registerAll(registry);
    }

    private static ItemStack mod(String name, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, name);
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    /** The LV Ammo Press is registered, reads the ammo map, and can be placed in the world. */
    private static final class AmmoPressScenario implements Scenario {

        @Override
        public String name() {
            return "production.ammopress";
        }

        @Override
        public String description() {
            return "The LV Ammo Press is registered with the ammo map and places as a GT machine block.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int id = GregGunsMTEIDs.AMMO_PRESS_LV;
            Object prototype = GregTechAPI.METATILEENTITIES[id];
            out.check(prototype instanceof MTEAmmoPress, "MTE " + id + " should be an MTEAmmoPress");
            if (!(prototype instanceof MTEAmmoPress)) {
                return out.build();
            }
            MTEAmmoPress press = (MTEAmmoPress) prototype;
            out.checkEquals("tier (GT voltage index)", 1, (int) press.mTier);
            out.check(press.getRecipeMap() == GregGunsRecipeMaps.AMMO_PRESS, "the press should run gregguns.ammoPress");
            out.check(
                GregGunsRecipeMaps.AMMO_PRESS.unlocalizedName.equals(GregGunsRecipeMaps.AMMO_PRESS_NAME),
                "the map should be named " + GregGunsRecipeMaps.AMMO_PRESS_NAME);
            out.check(
                press.getStackForm(1) != null && press.getStackForm(1)
                    .getItem() != null,
                "the press should have an item form");

            // Place it as GT's item would: the machines block with the base tile entity type as metadata.
            ctx.setBlock(0, 0, 0, GregTechAPI.sBlockMachines, press.getTileEntityBaseType());
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
            out.check(tile instanceof IGregTechTileEntity, "the placed block should have a GT tile entity");
            if (tile instanceof IGregTechTileEntity) {
                IGregTechTileEntity base = (IGregTechTileEntity) tile;
                base.setInitialValuesAsNBT(null, (short) id);
                out.check(
                    base.getMetaTileEntity() instanceof MTEAmmoPress,
                    "the placed machine should be an MTEAmmoPress");
                if (base.getMetaTileEntity() instanceof MTEBasicMachineWithRecipe) {
                    out.check(
                        ((MTEBasicMachineWithRecipe) base.getMetaTileEntity()).getRecipeMap()
                            == GregGunsRecipeMaps.AMMO_PRESS,
                        "the placed machine should keep the ammo map");
                }
            }
            return out.build();
        }
    }

    /** The ammo map holds RC-28, RC-37 and RC-44 at LV, and the assembler holds the two part recipes. */
    private static final class BulletRecipeScenario implements Scenario {

        @Override
        public String name() {
            return "production.recipes";
        }

        @Override
        public String description() {
            return "The 9mm chain recipes exist on the ammo map and assembler with the sheet's EU/t and durations.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            long lvVoltage = GTValues.V[1];

            GTRecipe bullets = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(gt("foil", "Brass", 16), gt("round", "Lead", 16), gt("dust", "Gunpowder", 2))
                .voltage(lvVoltage)
                .find();
            out.check(bullets != null, "RC-28: 16 foil, 16 round and 2 gunpowder should match a bullet recipe");
            if (bullets != null) {
                out.checkEquals("RC-28 EU/t", (int) TierEU.RECIPE_LV, bullets.mEUt);
                out.checkEquals("RC-28 duration (ticks)", 5 * GTRecipeBuilder.SECONDS, bullets.mDuration);
                out.checkEquals("RC-28 output count", 16, bullets.mOutputs[0].stackSize);
                out.check(
                    bullets.mOutputs[0].getItem() == GameRegistry.findItem(GregGuns.MODID, ProductionTable.BULLET),
                    "RC-28 should make the 9mm bullet");
            }

            GTRecipe empties = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(gt("plate", "Steel", 2), gt("springSmall", "Steel", 8), mod("part_mechanical_2", 1))
                .voltage(lvVoltage)
                .find();
            out.check(
                empties != null,
                "RC-37: plates, springs and Mechanical Parts T2 should match the empty magazine");
            if (empties != null) {
                out.checkEquals("RC-37 EU/t", (int) TierEU.RECIPE_LV, empties.mEUt);
                out.checkEquals("RC-37 output count", 8, empties.mOutputs[0].stackSize);
            }

            GTRecipe loaded = GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                .items(mod(ProductionTable.MAGAZINE_EMPTY, 4), mod(ProductionTable.BULLET, 12))
                .voltage(lvVoltage)
                .find();
            out.check(loaded != null, "RC-44: 4 empty magazines and 12 bullets should match the loaded magazine");
            if (loaded != null) {
                out.checkEquals("RC-44 EU/t", (int) TierEU.RECIPE_LV, loaded.mEUt);
                out.checkEquals("RC-44 output count", 4, loaded.mOutputs[0].stackSize);
            }

            boolean barrel = false;
            boolean mechanical = false;
            for (GTRecipe recipe : gregtech.api.recipe.RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mOutputs == null || recipe.mOutputs.length == 0 || recipe.mOutputs[0] == null) {
                    continue;
                }
                Item made = recipe.mOutputs[0].getItem();
                barrel |= made == GameRegistry.findItem(GregGuns.MODID, "part_barrel_hardened")
                    && recipe.mEUt == TierEU.RECIPE_LV;
                mechanical |= made == GameRegistry.findItem(GregGuns.MODID, "part_mechanical_2")
                    && recipe.mEUt == TierEU.RECIPE_LV;
            }
            out.check(barrel, "RC-05: the assembler should make the Hardened Steel Barrel at LV");
            out.check(mechanical, "RC-11: the assembler should make Mechanical Parts T2 at LV");
            return out.build();
        }
    }
}
