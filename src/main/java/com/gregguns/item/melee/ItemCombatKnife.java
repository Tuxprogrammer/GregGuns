package com.gregguns.item.melee;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.combat.DamageType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Combat Knife: a vanilla sword of steel (damage 6.5 on top of the hand's 1, 1000 uses) whose hits are
 * {@link DamageType#PHYSICAL} GregGuns hits. Blocks like a sword. Both sides; the hit is dealt on the server.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Sheet: Combat Knife); docs/design/0001-tier-map.md (LV)
public final class ItemCombatKnife extends ItemSword implements Tiered {

    /** Creates the knife. */
    public ItemCombatKnife() {
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
        MeleeTooltips.add(tooltip, MeleeMath.HAND_DAMAGE + MeleeMath.knifeBonus(), false);
    }
}
