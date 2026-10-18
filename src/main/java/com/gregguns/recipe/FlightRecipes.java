package com.gregguns.recipe;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

/**
 * Registers the flight gear Assembler rows from {@link FlightTable}, queued on GT's before-postload hook like the other
 * armor rows. A row whose GregGuns input is not registered in this run (another module is off, or a component branch
 * has not landed) is skipped with an INFO line instead of an error, so every module combination still boots clean.
 * Common side.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md
public final class FlightRecipes {

    private FlightRecipes() {}

    /** Queues the Assembler rows. Call from the armor module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447
        GregTechAPI.sBeforeGTPostload.add(FlightRecipes::addMachineRecipes);
    }

    private static void addMachineRecipes() {
        for (ProductionTable.Row row : FlightTable.ROWS) {
            String missing = missingInput(row);
            if (missing != null) {
                GregGuns.LOG.info("Flight recipe {} deferred: {} is not registered in this run", row.id(), missing);
                continue;
            }
            ProductionRecipes.addMachineRecipe(row);
        }
    }

    /** @return the registry name of the first GregGuns input of the row that is not registered, or null */
    static String missingInput(ProductionTable.Row row) {
        for (ProductionTable.Input input : row.inputs()) {
            if (input.isGregGuns() && GameRegistry.findItem(GregGuns.MODID, input.itemName()) == null) {
                return input.itemName();
            }
        }
        return null;
    }
}
