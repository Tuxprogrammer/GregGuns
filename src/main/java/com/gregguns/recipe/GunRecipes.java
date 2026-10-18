package com.gregguns.recipe;

import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.item.part.PartItems;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the guns' final-assembly recipes on GT's Assembler (the 9mm Pistol for M1) and the GT recycling data of
 * the guns, from {@link GunTable}. Parts come through the unificator, never as raw ore-dictionary names.
 *
 * <p>
 * Timing: {@link #register()} queues the work on {@code GregTechAPI.sBeforeGTPostload}, exactly as
 * {@link ProductionRecipes} does, so the barrel and mechanical-parts recipes and item data it needs already exist
 * and GT's postload generators see the gun's {@link ItemData}. A row with an unresolved input is skipped with an
 * error in the log, and the dev self-check fails the boot on it. Common side.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LV table, 9mm Pistol), docs/gregguns-spec/06-items/recipes.md GGS-06-34
public final class GunRecipes {

    private GunRecipes() {}

    /** Queues the registration on GT's before-postload hook. Call from the guns module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(GunRecipes::addAll);
    }

    private static void addAll() {
        for (ProductionTable.Row row : GunTable.ROWS) {
            ProductionRecipes.addMachineRecipe(row);
        }
        PartCraftingRecipes.register();
        addRecyclingData();
    }

    /**
     * What recycling returns for the pistol: the steel and rubber of its direct inputs plus the steel inside its
     * barrel and mechanical parts. Amounts come from GT's own per-prefix figures.
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
    private static void addRecyclingData() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long barrel = OrePrefixes.pipeSmall.getMaterialAmount() + 2 * OrePrefixes.ring.getMaterialAmount() + plate;
        long mechanical2 = AmmoComposition.mechanical2Steel(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            OrePrefixes.screw.getMaterialAmount());
        long steel = barrel + mechanical2
            + GunComposition.pistolDirectSteel(
                plate,
                OrePrefixes.screw.getMaterialAmount(),
                OrePrefixes.springSmall.getMaterialAmount());
        ItemStack pistol = ProductionRecipes.modStack(GunTable.PISTOL.output(), 1);
        if (pistol == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", GunTable.PISTOL.output());
            return;
        }
        GTOreDictUnificator
            .addItemData(pistol, new ItemData(Materials.Steel, steel, new MaterialStack(Materials.Rubber, 2 * plate)));
        addRevolverData(plate);
        addSawedOffData(plate);
    }

    /**
     * Recycling data of the Sawed Off: the steel plate, screws and spring of the gun itself plus the iron and steel of
     * its two Iron Barrels (the barrel part carries its own data from {@link #addRevolverData}).
     */
    private static void addSawedOffData(long plate) {
        long screw = OrePrefixes.screw.getMaterialAmount();
        long spring = OrePrefixes.springSmall.getMaterialAmount();
        long barrelIron = GunComposition
            .ironBarrelIron(OrePrefixes.stickLong.getMaterialAmount(), OrePrefixes.ring.getMaterialAmount());
        long barrelSteel = GunComposition.ironBarrelSteel(screw);
        long steel = GunComposition.sawedOffDirectSteel(plate, screw, spring) + 2 * barrelSteel;
        attach(
            GunTable.SAWED_OFF.output(),
            new ItemData(Materials.Steel, steel, new MaterialStack(Materials.Iron, 2 * barrelIron)));
    }

    /**
     * Recycling data of the Revolver and its iron and hand-made parts: iron and steel in the barrel, bronze in the
     * mechanical parts, and the plates, screws and spring of the gun itself.
     */
    private static void addRevolverData(long plate) {
        long screw = OrePrefixes.screw.getMaterialAmount();
        long spring = OrePrefixes.springSmall.getMaterialAmount();
        long barrelIron = GunComposition
            .ironBarrelIron(OrePrefixes.stickLong.getMaterialAmount(), OrePrefixes.ring.getMaterialAmount());
        long barrelSteel = GunComposition.ironBarrelSteel(screw);
        long mechanical1 = GunComposition.mechanical1Bronze(OrePrefixes.gearGtSmall.getMaterialAmount(), spring);
        attach(
            PartItems.BARREL_IRON,
            new ItemData(Materials.Iron, barrelIron, new MaterialStack(Materials.Steel, barrelSteel)));
        attach(PartItems.MECHANICAL_1, new ItemData(Materials.Bronze, mechanical1));
        long steel = GunComposition.revolverDirectSteel(plate, screw, spring) + barrelSteel;
        attach(
            GunTable.REVOLVER.output(),
            new ItemData(
                Materials.Steel,
                steel,
                new MaterialStack(Materials.Iron, barrelIron),
                new MaterialStack(Materials.Bronze, mechanical1)));
    }

    private static void attach(String registryName, ItemData data) {
        ItemStack stack = ProductionRecipes.modStack(registryName, 1);
        if (stack == null) {
            GregGuns.LOG.error("Cannot attach ItemData to {}: not registered", registryName);
            return;
        }
        GTOreDictUnificator.addItemData(stack, data);
    }
}
