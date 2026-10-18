package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.slots.logic.BaubleSlot;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Ammo Pouch: a stack-size-1 Baubles {@code quiver} item with a 6-slot GUI. Worn, its contents are searched by the
 * gun reload path before the main inventory ({@link AmmoPouch#SOURCE}). Right-click opens the GUI; sneak + right-click
 * equips it. Contents live in the stack NBT.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03); docs/design/0009 decision 30;
// docs/gregguns-spec/12-questbook/quest-line.md (gg.gear.pouch: MV)
// Deviation: Techguns' pouch tab had six ammo slots plus three food slots; the food slots are dropped
// (baubles-slots.md,
// Open questions), so this pouch holds only the six ammunition slots.
public final class ItemAmmoPouch extends ItemBauble {

    /** Registry name. */
    public static final String NAME = "ammo_pouch";

    @Override
    public BaubleSlot slot() {
        return BaubleSlot.QUIVER;
    }

    @Override
    public Tier getTier() {
        return Tier.MV;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            return super.onItemRightClick(stack, world, player);
        }
        if (!world.isRemote) {
            player.openGui(GregGuns.instance, AmmoPouch.GUI_HELD, world, player.inventory.currentItem, 0, 0);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.pouch"));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
