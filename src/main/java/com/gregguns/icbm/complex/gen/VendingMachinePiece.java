package com.gregguns.icbm.complex.gen;

import net.minecraft.block.Block;

import com.gregguns.compat.VendingMachineCompat;
import com.gregguns.core.module.ModPresence;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * The pack's Vending Machine as a piece of a silo complex (D355): its casing block and its controller's meta tile
 * entity, found by registry name and id, so GregGuns has no compile or class-loading link to the mod. The machine is a
 * GT multiblock of five casings around its controller, two wide, three high and one deep; it needs no power, no
 * maintenance and no player to form (its controller checks its structure every 20 ticks on its own), and it holds no
 * stock: its trades are the pack's trade database. Without the mod, or with another machine on its id, it is absent
 * and the complex keeps the style's cabinet in its place. Server side.
 */
// Source (the casing registers as "casing" under the mod's domain, a GT casing block with no tile entity):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/VendingMachineBlocks.java#L9-L11
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/GTGenericBlock.java#L13-L17
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockCasingsAbstract.java#L100-L103
// Source (the controller's id, 2741; no maintenance; the structure check and activation in onPostTick; the same in
// 0.4.103, whose only change to the controller is the uplink hatch's crafting icon):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/VendingMachine.java#L66
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L345-L348
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L582-L593
public final class VendingMachinePiece {

    /** The casing's registry name under the mod's domain; meta 0. */
    public static final String CASING = "casing";

    /** The controller's meta tile entity id. */
    public static final int CONTROLLER_ID = 2741;

    /** The controller's class, checked so another machine on the same id is never taken for it. */
    public static final String CONTROLLER_CLASS = "com.cubefury.vendingmachine.blocks.MTEVendingMachine";

    private VendingMachinePiece() {}

    /** @return the casing block, or null without the Vending Machine mod */
    public static Block casing() {
        if (!ModPresence.isLoaded(VendingMachineCompat.MOD_ID)) return null;
        return GameRegistry.findBlock(VendingMachineCompat.MOD_ID, CASING);
    }

    /** @return the controller's meta tile entity id, or -1 without the mod or with another machine on its id */
    public static int controllerId() {
        if (!ModPresence.isLoaded(VendingMachineCompat.MOD_ID)) return -1;
        IMetaTileEntity m = GregTechAPI.METATILEENTITIES[CONTROLLER_ID];
        return m != null && CONTROLLER_CLASS.equals(
            m.getClass()
                .getName()) ? CONTROLLER_ID : -1;
    }

    /** @return whether the whole machine can be built: its casing and its controller both resolve */
    public static boolean present() {
        return casing() != null && controllerId() >= 0;
    }
}
