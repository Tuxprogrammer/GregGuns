package com.gregguns.icbm.worldgen;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import com.gregguns.GregGuns;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.production.MissileBuildSpec;
import com.gregguns.icbm.production.ProductionItems;
import com.gregguns.icbm.silo.SiloMachines;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;

import gregtech.api.enums.ItemList;

/**
 * Turns a {@link Placement.Bonus} into the item stack a complex chest holds (D17, D128). Server side. An item that does
 * not exist this run (a module that is off, a GT item missing) gives null and is skipped, never a null stack in a
 * chest.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Contents)
public final class SiloBonusItems {

    /** NBT flag on a body stack: damaged, so it cannot be placed and exists to be recycled (D17). */
    public static final String K_DAMAGED = "damaged";

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
                    ItemStack s = MissileBodies.body(MissileTier.T1);
                    NBTTagCompound tag = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
                    tag.setBoolean(K_DAMAGED, true);
                    s.setTagCompound(tag);
                    return s;
                }
                case SCHEMATIC_CHIP:
                    return ProductionItems.chip(MissileBuildSpec.T1);
                case SILO_CONTROLLER:
                    return SiloMachines.stack(1);
                case SILO_ENERGY_HATCH:
                    return ItemList.Hatch_Energy_HV.get(1);
                case DATA_STICK:
                    return dataStick(p.extra());
                case INTACT_WARHEAD: {
                    Item item = Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T1));
                    return WarheadStacks.make(item, WarheadCasing.T1, "condensed", WarheadCasing.T1.fillCap());
                }
                case EMPTY_CASINGS:
                    return new ItemStack(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T1)), 2);
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.warn("Silo bonus {} unavailable: {}", bonus, e.toString());
            return null;
        }
    }

    private static ItemStack dataStick(String blocks) {
        ItemStack s = ItemList.Tool_DataStick.get(1);
        if (s == null || blocks == null) {
            return null;
        }
        NBTTagCompound tag = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("Silo complex at " + blocks.replace(",", ", ")));
        display.setTag("Lore", lore);
        display.setString("Name", "Silo Complex Coordinates");
        tag.setTag("display", display);
        s.setTagCompound(tag);
        return s;
    }
}
