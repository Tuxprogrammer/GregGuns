package com.gregguns.client.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.production.IcbmRecipeMaps;
import com.gregguns.icbm.production.MissileBenchLayout;
import com.gregguns.icbm.production.MissileBenchRules;
import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.production.gc.MissileBenchArt;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.util.GTRecipe;

/**
 * The NEI page of the NASA Workbench's missile builds: the same schematic as the in-game page (a rocket silhouette with
 * the slots in the shape of the missile), one page per body and guidance computer. Display only: it reads the rows of
 * {@link IcbmRecipeMaps#MISSILE_WORKBENCH}, which no machine runs. Client only.
 */
// Source: Galacticraft's own rocket workbench page is a TemplateRecipeHandler that draws the bench texture behind
// PositionedStacks, one recipe per page:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/nei/RocketT1RecipeHandler.java
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NEI)
public class MissileWorkbenchHandler extends TemplateRecipeHandler {

    /** Height of one page: the schematic plus room for the labels. */
    private static final int PAGE_HEIGHT = MissileBenchLayout.HEIGHT + 24;

    public String getRecipeID() {
        return IcbmRecipeMaps.MISSILE_WORKBENCH_NAME;
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal(IcbmRecipeMaps.MISSILE_WORKBENCH_NAME);
    }

    @Override
    public String getGuiTexture() {
        return "textures/gui/container/crafting_table.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void loadTransferRects() {}

    private void loadAll(ItemStack result, ItemStack ingredient) {
        for (GTRecipe r : IcbmRecipeMaps.MISSILE_WORKBENCH.getAllRecipes()) {
            CachedBuild c = build(r);
            if (c == null) {
                continue;
            }
            if (result != null && !NEIServerUtils.areStacksSameTypeCrafting(c.output.item, result)) {
                continue;
            }
            if (ingredient != null && !c.uses(ingredient)) {
                continue;
            }
            arecipes.add(c);
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(getRecipeID())) {
            loadAll(null, null);
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        loadAll(result, null);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        loadAll(null, ingredient);
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        CachedBuild c = (CachedBuild) arecipes.get(recipe);
        Gui.drawRect(0, 0, MissileBenchLayout.WIDTH, PAGE_HEIGHT, 0xFFC6C6C6);
        MissileBenchArt.draw(c.spec, 0, 0, true);
        MissileBenchArt.drawLabels(Minecraft.getMinecraft().fontRenderer, c.spec, 0, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private CachedBuild build(GTRecipe r) {
        if (r.mInputs == null || r.mInputs.length < 4 || r.mOutputs == null || r.mOutputs.length < 1) {
            return null;
        }
        com.gregguns.icbm.missile.MissileTier tier = MissileBodies.tierOf(r.mOutputs[0]);
        MissileBuildSpec spec = tier == null ? null : MissileBuildSpec.byNumber(tier.number());
        return spec == null ? null : new CachedBuild(spec, r);
    }

    /** One build: the plates, four fins, the engine and the computer, in their places in the missile's shape. */
    public class CachedBuild extends CachedRecipe {

        final MissileBuildSpec spec;
        final PositionedStack output;
        final List<PositionedStack> inputs = new ArrayList<>();

        private CachedBuild(MissileBuildSpec spec, GTRecipe r) {
            this.spec = spec;
            ItemStack plates = one(r.mInputs[0]);
            ItemStack fins = one(r.mInputs[1]);
            for (int i = 0; i < MissileBenchRules.plateSlots(spec); i++) {
                inputs.add(new PositionedStack(plates, MissileBenchLayout.plateX(i), MissileBenchLayout.plateY(i)));
            }
            for (int i = 0; i < MissileBenchRules.FIN_SLOTS; i++) {
                inputs.add(new PositionedStack(fins, MissileBenchLayout.finX(i), MissileBenchLayout.finY(spec)));
            }
            inputs.add(
                new PositionedStack(one(r.mInputs[2]), MissileBenchLayout.engineX(), MissileBenchLayout.engineY(spec)));
            inputs.add(
                new PositionedStack(one(r.mInputs[3]), MissileBenchLayout.guidanceX(), MissileBenchLayout.guidanceY()));
            this.output = new PositionedStack(
                r.mOutputs[0],
                MissileBenchLayout.resultX(),
                MissileBenchLayout.resultY(spec));
        }

        boolean uses(ItemStack ingredient) {
            for (PositionedStack p : inputs) {
                if (NEIServerUtils.areStacksSameTypeCrafting(p.item, ingredient)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, inputs);
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }

    private static ItemStack one(ItemStack s) {
        ItemStack c = s.copy();
        c.stackSize = 1;
        return c;
    }
}
