package com.gregguns.explosion.nuclear;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * What irradiated earth drops. A GT radioactive item (its GT item data names a radioactive material, attached in
 * {@link FalloutRegistration#attachItemData}), so an unprotected player carrying it takes GT's radioactivity effect
 * through the existing carried-item mechanism. The byproduct chain that processes it arrives with I5. Borrows the
 * vanilla flint icon, tinted green.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class ItemIrradiatedRubble extends Item implements com.gregguns.api.Tiered {

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon("flint");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return BlockIrradiatedEarth.TINT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.fallout.rubble.tooltip"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }
}
