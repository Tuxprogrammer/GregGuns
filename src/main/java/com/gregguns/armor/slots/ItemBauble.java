package com.gregguns.armor.slots;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gregguns.api.Tiered;
import com.gregguns.armor.slots.logic.BaubleSlot;

import baubles.api.BaubleType;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base of every GregGuns item worn in a Baubles Expanded slot: it names its slot type, equips on right-click, shows the
 * slot in its tooltip and does nothing on its own worn tick.
 *
 * <p>
 * Baubles Expanded is a soft dependency (GGS-05-07): the {@code IBaubleExpanded} interface and every method that
 * mentions a Baubles type are stripped by FML's {@code @Optional} when the mod is absent, and the item then loads
 * normally and can only be held. Worn effects are driven by the module's central player tick ({@link SlotEquipment}),
 * not by {@code onWornTick}, so they work the same for any entity that carries the item (a scenario's fake player
 * included) and run on the server only (GGS-05-05).
 *
 * <p>
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-02, GGS-05-05, GGS-05-07)
// Deviation: baubles-slots.md says "onWornTick runs server-side"; the effects run from SlotEquipment.tick instead,
// which
// the armor module calls once per server player tick and scenarios can call directly. onWornTick stays an empty hook.
// Source: precedent for an IBaubleExpanded item with tooltip and right-click equip, GT's MagLev Harness:
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/ItemMagLevHarness.java#L41-L43
@Optional.Interface(iface = "baubles.api.expanded.IBaubleExpanded", modid = BaublesCompat.MODID)
public abstract class ItemBauble extends Item implements IBaubleExpanded, Tiered {

    protected ItemBauble() {
        setMaxStackSize(1);
    }

    /** @return the Baubles Expanded slot type this item is worn in */
    public abstract BaubleSlot slot();

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public String[] getBaubleTypes(ItemStack stack) {
        return slot().types();
    }

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public BaubleType getBaubleType(ItemStack stack) {
        // IBauble requires an answer; Baubles Expanded reads getBaubleTypes instead.
        return BaubleType.AMULET;
    }

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    @Optional.Method(modid = BaublesCompat.MODID)
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return BaublesCompat.rightClickEquip(stack, world, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        BaublesCompat.addSlotInformation(tooltip, slot());
    }
}
