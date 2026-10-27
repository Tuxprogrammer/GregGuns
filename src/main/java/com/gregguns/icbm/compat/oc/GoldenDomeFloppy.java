package com.gregguns.icbm.compat.oc;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Items;

/**
 * The Golden Dome floppy (D18, GGI-01-07 to 09): an OpenComputers loot disk whose read-only file system is the
 * {@code assets/gregguns/lua/golden_dome/} folder of our jar (the programs {@code launch}, {@code radarwatch},
 * {@code autodefense} and {@code emp}, the library {@code goldendome} and an {@code .autorun.lua} that puts them on
 * the shell path). It exists only when OpenComputers is loaded: this class is called from {@link OcCompat} behind the
 * module gate, so a pack without OpenComputers never loads it. Common side, init phase.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (the Golden Dome floppy)
public final class GoldenDomeFloppy {

    /** The disk's label and OC's identifier for its file system factory. */
    public static final String LABEL = "Golden Dome";
    /** OC's disk color: a Minecraft dye index, 11 is yellow. */
    public static final int COLOR_YELLOW = 11;
    /** The programmed circuit of the Assembler recipe. */
    public static final int CIRCUIT = 1;
    /** The root of the disk inside the jar. */
    public static final String ROOT = "lua/golden_dome";
    /** Every file the disk must hold. */
    public static final String[] FILES = { ".autorun.lua", "bin/launch.lua", "bin/radarwatch.lua",
        "bin/autodefense.lua", "bin/emp.lua", "lib/goldendome.lua" };

    private static ItemStack stack;

    private GoldenDomeFloppy() {}

    /** Registers the disk and queues its recipe. Init phase only (OC wants it after its own pre-init). */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Items.java#L101
        stack = Items.registerFloppy(LABEL, COLOR_YELLOW, GoldenDomeFloppy::newFileSystem, false);
        if (stack == null) {
            GregGuns.LOG.warn("ICBM: OpenComputers did not register the Golden Dome floppy");
            return;
        }
        GregTechAPI.sBeforeGTPostload.add(GoldenDomeFloppy::addRecipe);
    }

    /** @return a fresh read-only file system serving the disk's folder */
    public static li.cil.oc.api.fs.FileSystem newFileSystem() {
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/FileSystem.java
        return FileSystem.asReadOnly(FileSystem.fromClass(GoldenDomeFloppy.class, GregGuns.MODID, ROOT));
    }

    /** @return one Golden Dome floppy; null when OpenComputers did not register it */
    public static ItemStack stack() {
        return stack == null ? null : stack.copy();
    }

    private static void addRecipe() {
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Items.java#L21
        // (Items.get("floppy") is OpenComputers' blank floppy.)
        li.cil.oc.api.detail.ItemInfo blank = Items.get("floppy");
        ItemStack circuit = IcbmIngredients.stack(Ing.gt("circuit", "Good", 1, Tier.MV, 2.0));
        if (blank == null || circuit == null || stack == null) {
            GregGuns.LOG.error("ICBM: skipping the Golden Dome recipe: floppy or MV circuit missing");
            return;
        }
        // Run at HV: D69 puts a floor of HV under every ICBM recipe; the MV circuit is an input, not the machine.
        GTValues.RA.stdBuilder()
            .circuit(CIRCUIT)
            .itemInputs(blank.createItemStack(1), circuit)
            .itemOutputs(stack.copy())
            .duration(200)
            .eut(GTValues.VP[Tier.HV.voltageIndex()])
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
