package com.gregguns.item.ammo;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A loaded magazine. It is a separate item from the empty one and holds no NBT: a magazine is full or empty, as in
 * Techguns. Sneak + right-click unloads one magazine into an empty magazine and the loose rounds.
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md (magazine items), docs/gregguns-spec/06-items/ammo.md. Both sides; the
 * unload runs on the server only.
 */
// Deviation: Techguns' "Right click to put in ammo inventory" belongs to area 05 (special slots), so only the sneak
// line is shown for now.
public final class ItemMagazine extends Item implements Tiered {

    private final MagazineType type;
    private final Item emptyMagazine;
    private final Item round;

    /**
     * @param type          the magazine family
     * @param emptyMagazine the registered empty magazine item handed back on unload
     * @param round         the registered loose round item handed back on unload
     */
    public ItemMagazine(MagazineType type, Item emptyMagazine, Item round) {
        this.type = type;
        this.emptyMagazine = emptyMagazine;
        this.round = round;
        setMaxStackSize(16);
    }

    /** @return the magazine family */
    public MagazineType type() {
        return type;
    }

    @Override
    public Tier getTier() {
        return type.tier();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || !player.isSneaking()) {
            return stack;
        }
        // One magazine per click. Whatever the inventory cannot take is dropped, never duplicated or voided
        // (ITEM-07 / CORE-04).
        stack.stackSize--;
        give(player, new ItemStack(emptyMagazine, 1));
        give(player, new ItemStack(round, MagazineLogic.roundsReturned(type, 1)));
        return stack.stackSize <= 0 ? null : stack;
    }

    private static void give(EntityPlayer player, ItemStack stack) {
        if (!player.inventory.addItemStackToInventory(stack) && stack.stackSize > 0) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip
            .add(StatCollector.translateToLocalFormatted("gregguns.tooltip.magazine.loaded", type.roundsPerMagazine()));
        tooltip.add(StatCollector.translateToLocal("gregguns.tooltip.magazine.sneak"));
        AmmoTooltips.forMagazine(tooltip, type);
    }
}
