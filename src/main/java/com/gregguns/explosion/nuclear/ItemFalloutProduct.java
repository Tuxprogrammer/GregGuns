package com.gregguns.explosion.nuclear;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.util.GTUtility;

/**
 * Trinitite shards and fallout goo, with their processing intermediates (D21): damage 0 and 1 are the finds (shard and
 * crushed trinitite for the shard item; goo and hot goo for the goo item), damage 2 and 3 of the goo item are the
 * dissolved forms the Chemical Reactor makes. Like rubble they carry <b>no GT item data</b> (no recycling, no
 * unification) and irradiate a holder through GT's own check while carried. Borrows the NTM art, tinted per kind.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
// Source: GTProxy.applyItemEffects, the carried-item check this mirrors
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTProxy.java#L1286-L1311
public final class ItemFalloutProduct extends Item implements com.gregguns.api.Tiered {

    /** Shard item damage: a trinitite shard. */
    public static final int SHARD = 0;
    /** Shard item damage: macerated trinitite. */
    public static final int CRUSHED = 1;
    /** Goo item damage: goo. */
    public static final int GOO = 0;
    /** Goo item damage: hot goo (ZPM or better). */
    public static final int HOT_GOO = 1;
    /** Goo item damage: goo after the nitric acid bath. */
    public static final int DISSOLVED = 2;
    /** Goo item damage: hot goo after the bath. */
    public static final int DISSOLVED_HOT = 3;

    private final boolean goo;
    private final String iconName;

    @SideOnly(Side.CLIENT)
    private IIcon icon;

    /**
     * @param goo      true for the goo item (four kinds), false for the shard item (two)
     * @param iconName the texture name
     */
    public ItemFalloutProduct(boolean goo, String iconName) {
        this.goo = goo;
        this.iconName = iconName;
        setHasSubtypes(true);
    }

    /** @return how many damage values this item has */
    public int kinds() {
        return goo ? 4 : 2;
    }

    /**
     * @param damage a damage value
     * @return the GT radioactivity level a holder takes: that of rubble for goo, 1 for trinitite; hot goo one more
     */
    public int level(int damage) {
        if (!goo) {
            return 1;
        }
        int base = ItemIrradiatedRubble.level();
        return (damage & 1) == 1 ? base + 1 : base;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity holder, int slot, boolean held) {
        if (!world.isRemote && holder instanceof EntityPlayer
            && holder.ticksExisted % ItemIrradiatedRubble.PERIOD_TICKS == 0
            && !((EntityPlayer) holder).capabilities.isCreativeMode) {
            GTUtility.applyRadioactivity((EntityLivingBase) holder, level(stack.getItemDamage()), stack.stackSize);
        }
    }

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return getUnlocalizedName() + "." + Math.min(kinds() - 1, Math.max(0, stack.getItemDamage()));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon(iconName);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        switch (stack.getItemDamage()) {
            case 1:
                return goo ? ItemIrradiatedRubble.HOT_TINT : 0xB8B8B8;
            case 2:
                return 0x7FD08A;
            case 3:
                return 0xC8E060;
            default:
                return 0xFFFFFF;
        }
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
        for (int i = 0; i < kinds(); i++) {
            list.add(new ItemStack(item, 1, i));
        }
    }
}
