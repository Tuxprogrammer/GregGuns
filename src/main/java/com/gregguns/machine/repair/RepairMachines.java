package com.gregguns.machine.repair;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;

/**
 * Creates the Repair Benches (LV to IV) and the Camo Bench and adds their hand recipes. Registration is a plain call
 * from {@code MachinesModule.init}, inside GT's MTE window (after GT's preload, before its postload); the recipes are
 * queued on GT's before-postload hook so {@code ItemList} is filled. Common side.
 *
 * <p>
 * Usage: {@code RepairMachines.benchStack(Tier.LV, 1)} is a Repair Bench item.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Repair Bench, Camo Bench, GGS-09-26, GGS-09-27, GGS-09-33),
// docs/gregguns-spec/08-machines-production/machine-roster.md
public final class RepairMachines {

    private static final String BENCH_TOOLTIP = "gregguns.machine.repair_bench.tooltip";
    private static final String BENCH_TIER_TOOLTIP = "gregguns.machine.repair_bench.tier";
    private static final String CAMO_TOOLTIP = "gregguns.machine.camo_bench.tooltip";

    private static final String[] BENCH_NAMES = { "Basic Repair Bench", "Advanced Repair Bench",
        "Advanced Repair Bench II", "Advanced Repair Bench III", "Advanced Repair Bench IV" };

    private RepairMachines() {}

    /** Constructs every bench. Call once, from {@code MachinesModule.init}. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        RepairRecipeSpec.Row[] rows = RepairRecipeSpec.Row.values();
        for (int i = 0; i < rows.length; i++) {
            new MTERepairBench(
                rows[i].mteId(),
                "gregguns.repairbench.tier.0" + (i + 1),
                BENCH_NAMES[i],
                rows[i].tier()
                    .voltageIndex(),
                new String[] { BENCH_TOOLTIP, BENCH_TIER_TOOLTIP });
        }
        new MTECamoBench(GregGunsMTEIDs.CAMO_BENCH, "gregguns.camobench", "Camo Bench", new String[] { CAMO_TOOLTIP });
    }

    /** Queues the hand recipes; call from {@code MachinesModule.init}. */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(RepairMachines::addCraftingRecipes);
    }

    /**
     * @param tier   LV to IV
     * @param amount stack size
     * @return the Repair Bench of the tier as an item stack
     * @throws IllegalStateException if the bench was not registered (the machines module is off, or too early)
     */
    public static ItemStack benchStack(Tier tier, int amount) {
        return stackOf(
            RepairRecipeSpec.rowFor(tier)
                .mteId(),
            amount);
    }

    /**
     * @param amount stack size
     * @return the Camo Bench as an item stack
     * @throws IllegalStateException if the bench was not registered
     */
    public static ItemStack camoBenchStack(int amount) {
        return stackOf(GregGunsMTEIDs.CAMO_BENCH, amount);
    }

    private static ItemStack stackOf(int id, int amount) {
        if (GregTechAPI.METATILEENTITIES[id] == null) {
            throw new IllegalStateException("MTE " + id + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[id].getStackForm(amount);
    }

    private static Object cable(Tier tier) {
        // Source (cable per tier, the Brewery and Miner recipes):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3351-L3380
        Materials m;
        switch (tier) {
            case LV:
                m = Materials.Tin;
                break;
            case MV:
                m = Materials.AnyCopper;
                break;
            case HV:
                m = Materials.Gold;
                break;
            case EV:
                m = Materials.Aluminium;
                break;
            default:
                m = Materials.Tungsten;
                break;
        }
        return OrePrefixes.cableGt01.get(m);
    }

    private static void addCraftingRecipes() {
        // Source (a machine around its hull, plates on top, GT machine style):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3351-L3380
        // Gate: each recipe holds its tier's hull, plate (GateRule.gateMaterial) and Robot Arm (RepairRecipeSpecTest).
        for (RepairRecipeSpec.Row row : RepairRecipeSpec.Row.values()) {
            Tier tier = row.tier();
            GTModHandler.addCraftingRecipe(
                benchStack(tier, 1),
                GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
                new Object[] { "PRP", "WMW", "CTC", 'P', OrePrefixes.plate.get(Materials.get(row.plateMaterial())), 'R',
                    ItemList.valueOf("Robot_Arm_" + tier.gtName()), 'W', cable(tier), 'M',
                    ItemList.valueOf("Hull_" + tier.gtName()), 'C',
                    OrePrefixes.circuit.get(Materials.get(row.circuitMaterial())), 'T', Blocks.crafting_table });
        }
        // Source: the same machine-around-a-hull pattern; the sheet's Camo Bench is three dyes over the LV hull.
        GTModHandler.addCraftingRecipe(
            camoBenchStack(1),
            GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
            new Object[] { "DDD", "WMW", "CTC", 'D', "dye", 'W', cable(Tier.LV), 'M', ItemList.Hull_LV, 'C',
                OrePrefixes.circuit.get(Materials.LV), 'T', Blocks.crafting_table });
        GregGuns.LOG.debug("Queued the Repair Bench and Camo Bench hand recipes");
    }
}
