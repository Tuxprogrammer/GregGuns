package com.gregguns.utility.turret;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;

/**
 * Creates the four Turrets (MV to IV) and adds their hand recipes. Registration is a plain call from
 * {@code TurretRegistration.init}, inside GT's MTE window (after GT's preload, before its postload); the recipes are
 * queued on GT's before-postload hook so {@code ItemList} is filled. Common side.
 *
 * <p>
 * Usage: {@code TurretMachines.stack(Tier.HV, 1)} is a HV Turret item.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Turret, Turret HV to IV, GGS-09-33, GGS-09-38)
public final class TurretMachines {

    private static final String TOOLTIP = "gregguns.machine.turret.tooltip";
    private static final String TIER_TOOLTIP = "gregguns.machine.turret.tier";
    private static final String[] NAMES = { "Gun Turret", "Advanced Gun Turret", "Advanced Gun Turret II",
        "Advanced Gun Turret III" };

    private TurretMachines() {}

    /** Constructs every Turret. Call once, from {@code TurretRegistration.init}. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        TurretRecipeSpec.Row[] rows = TurretRecipeSpec.Row.values();
        for (int i = 0; i < rows.length; i++) {
            new MTETurret(
                rows[i].mteId(),
                "gregguns.turret.tier.0" + (i + 1),
                NAMES[i],
                rows[i].tier()
                    .voltageIndex(),
                new String[] { TOOLTIP, TIER_TOOLTIP });
        }
    }

    /** Queues the hand recipes; call from {@code TurretRegistration.init}. */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(TurretMachines::addCraftingRecipes);
    }

    /**
     * @param tier   MV to IV
     * @param amount stack size
     * @return the Turret of the tier as an item stack
     * @throws IllegalStateException if the Turret was not registered (the utility module is off, or too early)
     */
    public static ItemStack stack(Tier tier, int amount) {
        int id = TurretRecipeSpec.rowFor(tier)
            .mteId();
        if (GregTechAPI.METATILEENTITIES[id] == null) {
            throw new IllegalStateException("MTE " + id + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[id].getStackForm(amount);
    }

    private static void addCraftingRecipes() {
        // Source (a machine around its hull, plates and tier parts, GT machine style):
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L3382-L3400
        // Deviation: the sheet's ballast is GregGuns Concrete, which is not built yet (area 07). Until it is, the key
        // is GT's own concrete block; the grid and every other part follow the sheet.
        ItemStack ballast = GregTechAPI.sBlockConcretes != null ? new ItemStack(GregTechAPI.sBlockConcretes, 1, 8)
            : new ItemStack(Blocks.stone);
        for (TurretRecipeSpec.Row row : TurretRecipeSpec.Row.values()) {
            Tier tier = row.tier();
            GTModHandler.addCraftingRecipe(
                stack(tier, 1),
                GTModHandler.RecipeBits.BITS | GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS,
                new Object[] { TurretRecipeSpec.GRID[0], TurretRecipeSpec.GRID[1], TurretRecipeSpec.GRID[2], 'P',
                    OrePrefixes.plate.get(Materials.get(row.plateMaterial())), 'S',
                    ItemList.valueOf("Sensor_" + tier.gtName()), 'C',
                    OrePrefixes.circuit.get(Materials.get(row.circuitMaterial())), 'M',
                    ItemList.valueOf("Hull_" + tier.gtName()), 'K', ballast, 'R',
                    ItemList.valueOf("Robot_Arm_" + tier.gtName()) });
        }
        GregGuns.LOG.debug("Queued the Turret hand recipes");
    }
}
