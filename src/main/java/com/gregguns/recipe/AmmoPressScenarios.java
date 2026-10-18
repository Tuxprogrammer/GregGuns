package com.gregguns.recipe;

import java.util.Locale;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.AmmoPressMachines;
import com.gregguns.machine.AmmoPressTier;
import com.gregguns.machine.MTEAmmoPress;
import com.gregguns.machine.MTEAmmoPressSteam;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * In-game scenarios for the five Ammo Press tiers: registration facts, tier gating on the ammo map, and one placed,
 * powered press per tier that runs a recipe to completion. Dev environment only, registered from
 * {@link ProductionScenarios#registerAll}. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (Tiers and what each runs), recipes.md
public final class AmmoPressScenarios {

    private AmmoPressScenarios() {}

    /**
     * Registers the tier scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new TiersScenario());
        for (AmmoPressTier tier : AmmoPressTier.values()) {
            registry.register(new PressRunScenario(tier));
        }
    }

    private static ItemStack mod(String name, int count) {
        Item item = GameRegistry.findItem(GregGuns.MODID, name);
        return item == null ? null : new ItemStack(item, count);
    }

    private static ItemStack gt(String prefix, String material, int count) {
        return GTOreDictUnificator.get(OrePrefixes.getPrefix(prefix), Materials.get(material), count);
    }

    /** Registration facts of all five presses, the tier gate on the map, and the hand recipes. */
    private static final class TiersScenario implements Scenario {

        @Override
        public String name() {
            return "production.ammopress.tiers";
        }

        @Override
        public String description() {
            return "All five Ammo Press tiers have the right class, GT tier, slots, map, catalyst eligibility and hand recipe.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (AmmoPressTier tier : AmmoPressTier.values()) {
                Object prototype = GregTechAPI.METATILEENTITIES[tier.mteId()];
                String label = tier + " (" + tier.mteId() + ")";
                if (tier.isSteam()) {
                    out.check(prototype instanceof MTEAmmoPressSteam, label + " should be an MTEAmmoPressSteam");
                } else {
                    out.check(prototype instanceof MTEAmmoPress, label + " should be an MTEAmmoPress");
                }
                if (!(prototype instanceof MTEBasicMachine)) {
                    continue;
                }
                MTEBasicMachine machine = (MTEBasicMachine) prototype;
                // GT's bronze base class reports tier 1 for the (low pressure) steam machine; electric tiers are the
                // voltage index of the D1 tier.
                int expectedTier = tier.isSteam() ? 1
                    : tier.tier()
                        .voltageIndex();
                out.checkEquals(label + " GT tier", expectedTier, (int) machine.mTier);
                out.check(machine.getRecipeMap() == GregGunsRecipeMaps.AMMO_PRESS, label + " should run the ammo map");
                out.check(
                    machine instanceof RecipeMapWorkable,
                    label + " should be RecipeMapWorkable, which is what makes GT add it as a NEI catalyst");
                out.checkEquals(label + " item inputs", tier.isSteam() ? 4 : 6, machine.mInputSlotCount);
                out.checkEquals(label + " item outputs", tier.isSteam() ? 1 : 3, machine.mOutputItems.length);
                out.check(hasCraftingRecipe(AmmoPressMachines.stack(tier, 1)), label + " should have a hand recipe");
            }
            // Gating: an LV recipe is found at LV voltage and up, and not at ULV (steam).
            ItemStack[] bulletInputs = { gt("foil", "Brass", 16), gt("round", "Lead", 16), gt("dust", "Gunpowder", 2) };
            out.check(
                GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                    .items(bulletInputs)
                    .voltage(TierEU.ULV)
                    .find() == null,
                "RC-28 (LV) must not match at ULV voltage");
            for (Tier tier : new Tier[] { Tier.LV, Tier.MV, Tier.HV, Tier.EV }) {
                out.check(
                    GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                        .items(bulletInputs)
                        .voltage(tier.voltage())
                        .find() != null,
                    "RC-28 (LV) should match at " + tier.gtName() + " voltage");
            }
            return out.build();
        }

        private static boolean hasCraftingRecipe(ItemStack result) {
            for (Object o : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack made = ((IRecipe) o).getRecipeOutput();
                if (made != null && made.getItem() == result.getItem()
                    && made.getItemDamage() == result.getItemDamage()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Places one press, powers it, and runs a recipe to completion. */
    private static final class PressRunScenario implements Scenario {

        private static final int TIMEOUT_TICKS = 600;
        private final AmmoPressTier tier;

        PressRunScenario(AmmoPressTier tier) {
            this.tier = tier;
        }

        @Override
        public String name() {
            return "production.ammopress.run." + tier.name()
                .toLowerCase(Locale.ROOT);
        }

        @Override
        public String description() {
            return tier.isSteam()
                ? "The Steam Ammo Press, given steam, runs a ULV test recipe (registered by the scenario) and outputs."
                : "The " + tier + " Ammo Press, given EU, runs RC-28 and outputs 16 9mm bullets.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder fail = ScenarioResult.builder(name());
            Object prototype = GregTechAPI.METATILEENTITIES[tier.mteId()];
            if (!(prototype instanceof MTEBasicMachine)) {
                fail.check(false, "MTE " + tier.mteId() + " is not registered");
                return fail.build();
            }
            MTEBasicMachine press = (MTEBasicMachine) prototype;
            ItemStack[] inputs;
            ItemStack expected;
            if (tier.isSteam()) {
                // Fixture: the Stone Bullet recipe belongs to the M2 branch. This one-off ULV recipe stands in so the
                // steam path runs. It stays on the map for the rest of the dev session.
                inputs = new ItemStack[] { new ItemStack(Blocks.cobblestone, 2) };
                expected = new ItemStack(Items.flint, 1);
                if (GregGunsRecipeMaps.AMMO_PRESS.findRecipeQuery()
                    .items(new ItemStack(Blocks.cobblestone, 2))
                    .voltage(TierEU.ULV)
                    .find() == null) {
                    GTValues.RA.stdBuilder()
                        .itemInputs(new ItemStack(Blocks.cobblestone, 2))
                        .itemOutputs(new ItemStack(Items.flint, 1))
                        .duration(2 * GTRecipeBuilder.SECONDS)
                        .eut(TierEU.RECIPE_ULV)
                        .addTo(GregGunsRecipeMaps.AMMO_PRESS);
                }
            } else {
                inputs = new ItemStack[] { gt("foil", "Brass", 16), gt("round", "Lead", 16),
                    gt("dust", "Gunpowder", 2) };
                expected = mod(ProductionTable.BULLET, 16);
            }
            ctx.setBlock(0, 0, 0, GregTechAPI.sBlockMachines, press.getTileEntityBaseType());
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
            if (!(tile instanceof IGregTechTileEntity)) {
                fail.check(false, "the placed block should have a GT tile entity");
                return fail.build();
            }
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) tier.mteId());
            MTEBasicMachine placed = (MTEBasicMachine) base.getMetaTileEntity();
            for (int i = 0; i < inputs.length; i++) {
                base.setInventorySlotContents(placed.getInputSlot() + i, inputs[i]);
            }
            // Enough for the whole run at the tier's voltage, whatever the overclock.
            if (tier.isSteam()) {
                base.increaseStoredSteam(8000, true);
            } else {
                base.increaseStoredEnergyUnits(
                    tier.tier()
                        .voltage() * 400,
                    true);
            }
            final ItemStack want = expected;
            return ScenarioResult.pending(name(), TIMEOUT_TICKS, ticks -> {
                int found = 0;
                for (int i = 0; i < placed.mOutputItems.length; i++) {
                    ItemStack out = base.getStackInSlot(placed.getOutputSlot() + i);
                    if (out != null && out.getItem() == want.getItem()) {
                        found += out.stackSize;
                    }
                }
                if (found < want.stackSize && ticks < TIMEOUT_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("output count", want.stackSize, found);
                out.note("finished after " + ticks + " ticks");
                return out.build();
            });
        }
    }
}
