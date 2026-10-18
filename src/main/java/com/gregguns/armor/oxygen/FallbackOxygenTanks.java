package com.gregguns.armor.oxygen;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.ItemBackDevice;
import com.gregguns.armor.slots.SlotEquipment;
import com.gregguns.armor.slots.logic.AirTopUp;
import com.gregguns.core.config.EquipmentConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Oxygen Tanks where Galacticraft is absent (dev runs): a {@code wings} back device that tops up the vanilla air
 * meter while some worn item gives oxygen gear, like Techguns' tanks. It exists under the same registry name as the
 * Galacticraft item so the recipes and lang keys do not change; it is the first user of the back-device hook. The
 * charge is the item's durability. Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md ("Without GC (dev runs): plain items with the same names");
// docs/techguns-spec/05-armor/special-equipment.md 4.1
public final class FallbackOxygenTanks extends ItemBackDevice {

    /** Creates the tanks; the capacity is {@code equipment.oxygenTankCapacity}. */
    public FallbackOxygenTanks() {
        setMaxDamage(EquipmentConfig.oxygenTankCapacity);
        setNoRepair();
    }

    @Override
    public Tier getTier() {
        return Tier.HV;
    }

    @Override
    public void onBackTick(EntityPlayer player, ItemStack stack) {
        if (AirTopUp
            .applies(player.getAir(), stack.getItemDamage(), getMaxDamage(), SlotEquipment.hasOxygenGear(player))) {
            player.setAir(AirTopUp.topUp(player.getAir()));
            stack.setItemDamage(stack.getItemDamage() + 1);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.equipment.tooltip.air_left",
                getMaxDamage() - stack.getItemDamage(),
                getMaxDamage()));
        tooltip.add(StatCollector.translateToLocal("gregguns.equipment.tooltip.space_suit_note"));
        super.addInformation(stack, player, tooltip, advanced);
    }
}
