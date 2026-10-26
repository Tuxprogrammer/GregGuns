package com.gregguns.icbm.production.gc;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.gregguns.icbm.production.ContainerMissileBench;
import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.production.ProductionItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import micdoodle8.mods.galacticraft.api.recipe.SchematicPage;
import micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry;

/**
 * One page of Galacticraft's NASA Workbench for a missile body (D8). It unlocks with the matching Missile Schematic
 * and opens {@link ContainerMissileBench}. This is the only class that names Galacticraft's page type; it is touched
 * only after the ICBM module gate. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (NASA Workbench page)
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/fbdc41b97e8178ac07e58ccaa93da2474b1e2664/src/main/java/micdoodle8/mods/galacticraft/api/recipe/ISchematicPage.java
// (page ID, GUI ID, the unlock item, the screen and the container) and core/client/gui/GuiHandler.java#L154-L158
// (the GUI ID is matched against the player's unlocked pages).
public final class MissileSchematicPage extends SchematicPage {

    /** First page ID; T1 takes this, T2 the next. GC says page IDs must be unique between addons. */
    public static final int PAGE_ID_FIRST = 4310;
    /** First GUI ID; T1 takes this, T2 the next. */
    public static final int GUI_ID_FIRST = 7310;

    private final MissileBuildSpec spec;

    private MissileSchematicPage(MissileBuildSpec spec) {
        this.spec = spec;
    }

    /** Registers both pages with Galacticraft. Call once, in the module's init, after the items exist. */
    public static void registerAll() {
        for (MissileBuildSpec s : MissileBuildSpec.values()) {
            SchematicRegistry.registerSchematicRecipe(new MissileSchematicPage(s));
        }
    }

    @Override
    public int getPageID() {
        return PAGE_ID_FIRST + spec.number() - 1;
    }

    @Override
    public int getGuiID() {
        return GUI_ID_FIRST + spec.number() - 1;
    }

    @Override
    public ItemStack getRequiredItem() {
        return ProductionItems.schematic(spec);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getResultScreen(EntityPlayer player, int x, int y, int z) {
        return new GuiMissileBench(player.inventory, spec);
    }

    @Override
    public Container getResultContainer(EntityPlayer player, int x, int y, int z) {
        return new ContainerMissileBench(player.inventory, spec);
    }
}
