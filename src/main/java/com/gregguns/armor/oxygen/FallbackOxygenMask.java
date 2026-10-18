package com.gregguns.armor.oxygen;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.ItemBauble;
import com.gregguns.armor.slots.logic.BaubleSlot;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Oxygen Mask where Galacticraft is absent (dev runs): a {@code head} bauble whose only job is to count as oxygen
 * gear for the fallback {@link FallbackOxygenTanks}. Same registry name as the Galacticraft item. Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md ("Without GC (dev runs): plain items with the same names");
// docs/techguns-spec/05-armor/special-equipment.md 3.4
public final class FallbackOxygenMask extends ItemBauble {

    @Override
    public BaubleSlot slot() {
        return BaubleSlot.HEAD;
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.space_suit_note"));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
