package com.gregguns.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * {@code armor.recycling}: every armor piece and metal accessory carries GT {@code ItemData} equal to its recipe's
 * materials, and GT's Macerator has a recycling recipe whose dusts are those materials. Dev environment only. Server
 * side.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md (M3 exit gate, box 5); GGS-06-34
public final class ArmorRecyclingScenario implements Scenario {

    private ArmorRecyclingScenario() {}

    public static void register(ScenarioRegistry registry) {
        registry.register(new ArmorRecyclingScenario());
    }

    @Override
    public String name() {
        return "armor.recycling";
    }

    @Override
    public String description() {
        return "Armor pieces carry GT ItemData matching their recipes; the Macerator returns those dusts.";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        Map<String, Map<String, Long>> expected = ArmorComposition.all(
            prefix -> OrePrefixes.getPrefix(prefix)
                .getMaterialAmount());
        int checked = 0;
        for (Map.Entry<String, Map<String, Long>> entry : expected.entrySet()) {
            ItemStack stack = ProductionRecipes.modStack(entry.getKey(), 1);
            if (stack == null) {
                continue; // an item of a module or branch that is not in this run
            }
            checkItemData(out, entry.getKey(), stack, entry.getValue());
            checked++;
        }
        out.check(checked >= 30, "at least 30 armor items should carry item data, saw " + checked);
        for (String name : new String[] { ArmorCatalog.T1_COMBAT.pieceName(ArmorSlot.HELMET),
            ArmorCatalog.T1_SCOUT.pieceName(ArmorSlot.CHESTPLATE), ArmorCatalog.T1_MINER.pieceName(ArmorSlot.BOOTS),
            ArmorCatalog.T2_RIOT.pieceName(ArmorSlot.HELMET), ArmorCatalog.STEAM.pieceName(ArmorSlot.CHESTPLATE) }) {
            checkMacerator(out, name, expected.get(name));
        }
        return out.build();
    }

    private static List<MaterialStack> materials(ItemData data) {
        List<MaterialStack> all = new ArrayList<>();
        all.add(data.mMaterial);
        for (MaterialStack by : data.mByProducts) {
            all.add(by);
        }
        return all;
    }

    private static void checkItemData(ScenarioResult.Builder out, String name, ItemStack stack,
        Map<String, Long> expected) {
        ItemData data = GTOreDictUnificator.getItemData(stack);
        out.check(data != null && data.hasValidMaterialData(), name + ": should carry GT item data");
        if (data == null || !data.hasValidMaterialData()) {
            return;
        }
        List<MaterialStack> got = materials(data);
        out.checkEquals(name + " material count", expected.size(), got.size());
        for (MaterialStack material : got) {
            Long want = expected.get(material.mMaterial.mName);
            out.check(want != null, name + ": unexpected material " + material.mMaterial.mName);
            if (want != null) {
                out.checkEquals(name + " " + material.mMaterial.mName, want, material.mAmount);
            }
        }
    }

    private static void checkMacerator(ScenarioResult.Builder out, String name, Map<String, Long> expected) {
        ItemStack stack = ProductionRecipes.modStack(name, 1);
        if (stack == null || expected == null) {
            out.check(false, name + ": should be registered with a composition");
            return;
        }
        GTRecipe recipe = RecipeMaps.maceratorRecipes.findRecipeQuery()
            .items(stack)
            .voltage(TierEU.RECIPE_UV)
            .find();
        out.check(recipe != null, name + ": the Macerator should have a recycling recipe");
        if (recipe == null) {
            return;
        }
        int dusts = 0;
        for (Map.Entry<String, Long> entry : expected.entrySet()) {
            if (dusts++ >= 4) {
                break; // GT returns the main material and three byproducts
            }
            Materials material = Materials.get(entry.getKey());
            ItemStack dust = GTOreDictUnificator.getDust(material.mMacerateInto, entry.getValue());
            boolean found = false;
            for (ItemStack output : recipe.mOutputs) {
                found |= dust != null && output != null
                    && output.getItem() == dust.getItem()
                    && output.getItemDamage() == dust.getItemDamage()
                    && output.stackSize == dust.stackSize;
            }
            out.check(found, name + ": the Macerator should return " + entry.getValue() + " of " + entry.getKey());
        }
    }
}
