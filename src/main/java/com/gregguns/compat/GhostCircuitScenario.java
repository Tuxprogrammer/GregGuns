package com.gregguns.compat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * No GregGuns recipe makes, takes or resolves to one of GT's ghost circuits ({@link GhostCircuits}): every GT machine
 * recipe and crafting recipe with a GregGuns output is checked, and the stack GT unifies each tier's circuit to is a
 * real circuit, and so is an NPC loot drop named by a circuit ore name. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D362)
public final class GhostCircuitScenario implements Scenario {

    private static final Materials[] TIERS = { Materials.ULV, Materials.LV, Materials.MV, Materials.HV, Materials.EV,
        Materials.IV, Materials.LuV, Materials.ZPM, Materials.UV, Materials.UHV };

    @Override
    public String name() {
        return "item.ghost_circuit.none";
    }

    @Override
    public String description() {
        return "No GregGuns machine or crafting recipe makes, takes or unifies to GT's unobtainable circuits";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        out.checkEquals(
            "ghost circuits GT registered",
            GhostCircuits.NAMES.size(),
            GhostCircuits.stacks()
                .size());
        for (Materials tier : TIERS) {
            ItemStack target = GTOreDictUnificator.get(OrePrefixes.circuit, tier, 1);
            out.check(!GhostCircuits.isGhost(target), "the " + tier.mName + " circuit unifies to a real circuit");
        }
        for (Materials tier : TIERS) {
            String ore = OrePrefixes.circuit.getName() + tier.mName;
            ItemStack drop = com.gregguns.entity.npc.NpcItems.stack("ore:" + ore, 1);
            out.check(!GhostCircuits.isGhost(drop), "an NPC drop of ore:" + ore + " is a real circuit");
        }
        int machine = 0;
        for (Map.Entry<String, RecipeMap<?>> e : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            Collection<GTRecipe> all = e.getValue()
                .getAllRecipes();
            for (GTRecipe r : all) {
                if (!ours(r.mOutputs)) continue;
                machine++;
                ItemStack ghost = firstGhost(r.mInputs);
                if (ghost == null) ghost = firstGhost(r.mOutputs);
                if (ghost != null) {
                    out.check(false, e.getKey() + " recipe for " + first(r.mOutputs).getDisplayName() + " uses a ghost");
                }
            }
        }
        out.check(machine > 0, "GregGuns machine recipes were checked (" + machine + ")");
        int crafting = 0;
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe r = (IRecipe) o;
            ItemStack result = r.getRecipeOutput();
            if (!ours(new ItemStack[] { result })) continue;
            crafting++;
            for (Object in : inputs(r)) {
                if (in instanceof ItemStack && GhostCircuits.isGhost((ItemStack) in)) {
                    out.check(false, "crafting recipe for " + result.getDisplayName() + " takes a ghost circuit");
                }
            }
        }
        out.note(crafting + " GregGuns crafting recipes checked");
        return out.build();
    }

    private static Object[] inputs(IRecipe r) {
        if (r instanceof ShapedOreRecipe) return ((ShapedOreRecipe) r).getInput();
        if (r instanceof ShapelessOreRecipe) return ((ShapelessOreRecipe) r).getInput()
            .toArray();
        if (r instanceof ShapedRecipes) return ((ShapedRecipes) r).recipeItems;
        if (r instanceof ShapelessRecipes) return ((List<?>) ((ShapelessRecipes) r).recipeItems).toArray();
        return new Object[0];
    }

    private static boolean ours(ItemStack[] stacks) {
        if (stacks == null) return false;
        for (ItemStack s : stacks) {
            if (s == null || s.getItem() == null) continue;
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(s.getItem());
            if (id != null && GregGuns.MODID.equals(id.modId)) return true;
        }
        return false;
    }

    private static ItemStack firstGhost(ItemStack[] stacks) {
        if (stacks == null) return null;
        for (ItemStack s : stacks) {
            if (GhostCircuits.isGhost(s)) return s;
        }
        return null;
    }

    private static ItemStack first(ItemStack[] stacks) {
        for (ItemStack s : stacks) {
            if (s != null && s.getItem() != null) return s;
        }
        return null;
    }
}
