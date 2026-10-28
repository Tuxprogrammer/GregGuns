package com.gregguns.icbm.worldgen;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.production.ProductionItems;
import com.gregguns.icbm.silo.SiloMachines;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/**
 * Turns a {@link Placement.Bonus} into the item stack a complex chest holds (D17, D128). Server side. An item that does
 * not exist this run (a module that is off, a GT item missing) gives null and is skipped, never a null stack in a
 * chest.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Contents)
public final class SiloBonusItems {

    private SiloBonusItems() {}

    /**
     * @param bonus the bonus
     * @param p     the chest placement (the data stick reads its payload)
     * @return the stack, or null when it cannot be made
     */
    public static ItemStack stack(Placement.Bonus bonus, Placement p) {
        try {
            switch (bonus) {
                case DAMAGED_BODY: {
                    ItemStack s = MissileBodies.body(MissileTier.byNumber(p == null ? 1 : ComplexContents.bodyTier(p.ceiling())));
                    s.setItemDamage(com.gregguns.icbm.missile.ItemBlockMissileBody.SALVAGE_DAMAGE);
                    return s;
                }
                case FUEL_CELLS:
                    return fuelCells(p);
                case SCHEMATIC_CHIP:
                    return ProductionItems.chip(MissileBuildSpec.T1);
                case SILO_CONTROLLER:
                    return SiloMachines.stack(1);
                case SILO_ENERGY_HATCH:
                    return com.gregguns.icbm.complex.gen.GtMachineHook.ENERGY_HATCH.get(1);
                case DATA_STICK:
                    return dataStick(p.extra());
                case CASING_PLATES:
                    return GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 4);
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.warn("Silo bonus {} unavailable: {}", bonus, e.toString());
            return null;
        }
    }

    /** Rocket fuels from weakest to best, each with the lowest ceiling whose planet's rocket may use it. */
    private static final String[] FUELS = { "nitrofuel", "fluid.rocketfuelmixb", "fluid.rocketfuelmixd",
        "fluid.rocketfuelmixc", "fluid.rocketfuelmixa" };
    private static final int[] FUEL_CEILING = { 4, 4, 5, 6, 7 };

    /** @return the name of the best fuel a ceiling allows (the pump room's rule: this fuel or better is never above) */
    static String fuelFor(int ceiling, int skip) {
        int best = -1;
        for (int i = 0; i < FUELS.length; i++) {
            if (FUEL_CEILING[i] <= ceiling) {
                best = i;
            }
        }
        return best - skip >= 0 ? FUELS[best - skip] : null;
    }

    /**
     * Cells of the best rocket fuel the chest's ceiling allows; when that fluid or its cell is missing this run, the
     * next rung down. The count (8 to 16) follows the chest's index.
     */
    private static ItemStack fuelCells(Placement p) {
        int ceiling = p == null ? SiloCeiling.EV : p.ceiling();
        int count = 8 + (p == null ? 0 : Math.floorMod(p.chestIndex() * 5, 9));
        for (int skip = 0; skip < FUELS.length; skip++) {
            String name = fuelFor(ceiling, skip);
            if (name == null) {
                return null;
            }
            Fluid f = FluidRegistry.getFluid(name);
            ItemStack empty = ItemList.Cell_Empty.get(1);
            if (f != null && empty != null) {
                ItemStack cell = GTUtility.fillFluidContainer(new FluidStack(f, 1000), empty, false, true);
                if (cell != null) {
                    cell.stackSize = count;
                    return cell;
                }
            }
        }
        return null;
    }

    private static ItemStack dataStick(String blocks) {
        ItemStack s = ItemList.Tool_DataStick.get(1);
        if (s == null || blocks == null) {
            return null;
        }
        NBTTagCompound tag = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString(ComplexText.get("icbm.complex.datastick.lore", blocks.replace(",", ", "))));
        display.setTag("Lore", lore);
        display.setString("Name", ComplexText.get("icbm.complex.datastick.name"));
        tag.setTag("display", display);
        s.setTagCompound(tag);
        return s;
    }
}
