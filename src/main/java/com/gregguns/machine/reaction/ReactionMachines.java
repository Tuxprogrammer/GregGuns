package com.gregguns.machine.reaction;

import net.minecraft.item.ItemStack;

import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.machine.OverlayIcons;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;

/**
 * Creates the Reaction Chamber single blocks (EV, IV) and the Retort controller, and looks them up.
 *
 * <p>
 * Registration is legal only between GT's preload and postload, which {@code MachinesModule.init} is inside (see
 * {@code AmmoPressMachines}). Common side.
 */
// Spec: docs/gregguns-spec/08-machines-production/machine-roster.md, recipes.md (Machine build recipes; Retort
// controller)
public final class ReactionMachines {

    private static final String[] CHAMBER_TOOLTIP = { "gregguns.machine.reaction_chamber.tooltip",
        "gregguns.machine.reaction_chamber.tooltip2", "gregguns.machine.reaction_chamber.type" };

    private ReactionMachines() {}

    /** Constructs the three machines. Call once from {@code MachinesModule.init}. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L668-L720
        // (the X placeholders are swapped for the tier's hull, circuit, wire and parts); the grid is the sheet's
        // "EPE / CMC / WSW" (recipes.md, Reaction Chamber single block).
        new MTEReactionChamber(
            GregGunsMTEIDs.REACTION_CHAMBER_EV,
            "gregguns.reactionchamber.tier.04",
            "Reaction Chamber (EV)",
            VoltageIndex.EV,
            CHAMBER_TOOLTIP,
            chamberRecipe());
        new MTEReactionChamber(
            GregGunsMTEIDs.REACTION_CHAMBER_IV,
            "gregguns.reactionchamber.tier.05",
            "Reaction Chamber (IV)",
            VoltageIndex.IV,
            CHAMBER_TOOLTIP,
            chamberRecipe());
        // Source: the controller grid is the same three tier parts (recipes.md, Retort controller); the casing in the
        // centre is the Reaction Casing, added with the other crafting recipes by ReactionCraftingRecipes.
        // The controller builds its textures lazily, so its overlay icons are requested here, before GT stitches (a
        // container created later keeps a null icon and the item render throws). See OverlayIcons.
        OverlayIcons.preload(
            "machines/retort/",
            "OVERLAY_FRONT",
            "OVERLAY_FRONT_GLOW",
            "OVERLAY_FRONT_ACTIVE",
            "OVERLAY_FRONT_ACTIVE_GLOW");
        new MTERetort(GregGunsMTEIDs.RETORT, "gregguns.retort", "Retort");
    }

    private static Object[] chamberRecipe() {
        return new Object[] { "EPE", "CMC", "WSW", 'E', MTEBasicMachineWithRecipe.X.EMITTER, 'P',
            MTEBasicMachineWithRecipe.X.PUMP, 'C', MTEBasicMachineWithRecipe.X.CIRCUIT, 'M',
            MTEBasicMachineWithRecipe.X.HULL, 'W', MTEBasicMachineWithRecipe.X.WIRE, 'S',
            MTEBasicMachineWithRecipe.X.SENSOR };
    }

    /**
     * @param id     one of the three MTE ids
     * @param amount stack size
     * @return the machine as an item stack
     * @throws IllegalStateException if it is not registered (machines module off, or too early)
     */
    public static ItemStack stack(int id, int amount) {
        if (GregTechAPI.METATILEENTITIES[id] == null) {
            throw new IllegalStateException("MTE " + id + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[id].getStackForm(amount);
    }
}
