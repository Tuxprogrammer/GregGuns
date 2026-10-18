package com.gregguns.recipe;

import com.gregguns.item.part.PartItems;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.objects.MaterialStack;

/**
 * GT recycling data of the gun components, one method per part. Each is the {@code recycling} hook of its
 * {@code PartDefinition}; the amounts come from GT's own per-prefix figures through {@link GunComposition} and
 * {@link AmmoComposition}. Common side, run on GT's before-postload hook (see {@link ProductionRecipes}).
 */
// Source:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359
// (addItemData does not overwrite an existing composition)
public final class PartRecycling {

    private PartRecycling() {}

    /** Iron Barrel (RC-04): a long iron rod and two rings, and two steel screws. */
    public static void barrelIron() {
        long barrelIron = GunComposition
            .ironBarrelIron(OrePrefixes.stickLong.getMaterialAmount(), OrePrefixes.ring.getMaterialAmount());
        long barrelSteel = GunComposition.ironBarrelSteel(OrePrefixes.screw.getMaterialAmount());
        ProductionRecipes.attach(
            PartItems.BARREL_IRON,
            new ItemData(Materials.Iron, barrelIron, new MaterialStack(Materials.Steel, barrelSteel)));
    }

    /** Mechanical Parts T1 (RC-03): bronze gears and a spring. */
    public static void mechanical1() {
        long mechanical1 = GunComposition.mechanical1Bronze(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount());
        ProductionRecipes.attach(PartItems.MECHANICAL_1, new ItemData(Materials.Bronze, mechanical1));
    }

    /** Mechanical Parts T2 (RC-11): steel gears, springs and screws. */
    public static void mechanical2() {
        ProductionRecipes.attach(PartItems.MECHANICAL_2, new ItemData(Materials.Steel, mechanical2Steel()));
    }

    /** Hardened Steel Barrel (RC-05): one small pipe, two rings and a plate. */
    public static void barrelHardened() {
        ProductionRecipes.attach(PartItems.BARREL_HARDENED, new ItemData(Materials.Steel, barrelHardenedSteel()));
    }

    /**
     * Iron Receiver (RC-13): iron plates, the steel screws, and the bronze in the mechanical parts inside it.
     */
    public static void receiverIron() {
        long plate = OrePrefixes.plate.getMaterialAmount();
        long mechanical1 = GunComposition.mechanical1Bronze(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount());
        ProductionRecipes.attach(
            PartItems.RECEIVER_IRON,
            new ItemData(
                Materials.Iron,
                GunComposition.ironReceiverIron(plate),
                new MaterialStack(
                    Materials.Steel,
                    GunComposition.ironReceiverSteel(OrePrefixes.screw.getMaterialAmount())),
                new MaterialStack(Materials.Bronze, mechanical1)));
    }

    /** @return steel in one Mechanical Parts T2, from GT's per-prefix amounts */
    static long mechanical2Steel() {
        return AmmoComposition.mechanical2Steel(
            OrePrefixes.gearGtSmall.getMaterialAmount(),
            OrePrefixes.springSmall.getMaterialAmount(),
            OrePrefixes.screw.getMaterialAmount());
    }

    /** @return steel in one Hardened Steel Barrel */
    static long barrelHardenedSteel() {
        return OrePrefixes.pipeSmall.getMaterialAmount() + 2 * OrePrefixes.ring.getMaterialAmount()
            + OrePrefixes.plate.getMaterialAmount();
    }
}
