package com.gregguns.item.melee;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Crowbar: a vanilla pickaxe of steel (harvest level 2, efficiency 7.5, 1000 uses, damage 4.5 on top of the
 * hand's 1) whose hits are PHYSICAL GregGuns hits.
 *
 * <p>
 * Deviation: the Techguns crowbar also reported a "wrench" tool class to rotate and dismantle its machines, pick up
 * chests and break ore clusters. Those blocks do not exist yet, and GregGuns machines are GregTech machines that take
 * GT's own wrench. The crowbar reports only {@code pickaxe} (so the inconsistent class data of ITEM-15 cannot
 * happen); the cluster-breaking use comes with the worldgen milestone (M8).
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Sheet: Crowbar); docs/design/0001-tier-map.md (LV)
public final class ItemCrowbar extends ItemPickaxe implements Tiered {

    /** Creates the crowbar. */
    public ItemCrowbar() {
        super(MeleeMaterials.steel());
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.worldObj.isRemote) {
            MeleeStrike.attack(stack, player, entity);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        MeleeTooltips.add(tooltip, MeleeMath.HAND_DAMAGE + MeleeMath.crowbarBonus(), true);
    }
}
