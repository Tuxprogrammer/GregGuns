package com.gregguns.armor;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.PieceDef;
import com.gregguns.armor.logic.PowerRules;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.slots.ElectricGear;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.IElectricItem;

/**
 * The chestplate of an electric powered set (the HEV suit): an IC2 electric item at the set's tier, so GT chargers and
 * battery buffers fill it. Its EU convert to power units at the set's {@code euPerUnit}; the rest of the set reads
 * whether the chestplate is charged. Comes out of the Assembler empty (design 0003, 0008, GGS-05-39). Server and
 * client.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (HEV, GGS-05-39); docs/gregguns-spec/05-armor/hazard-protection.md
// (GGS-05-11); docs/gregguns-spec/05-armor/powered-flight.md (GGS-05-14)
public final class ItemElectricArmor extends ItemGregGunsArmor implements IElectricItem {

    /**
     * @param set   an electric set
     * @param piece its chestplate
     */
    public ItemElectricArmor(SetDef set, PieceDef piece) {
        super(set, piece);
        if (piece.slot() != ArmorSlot.CHESTPLATE) {
            throw new IllegalArgumentException("only a chestplate holds a charge");
        }
        // Like GT's Mech Armor: IC2 shows the charge in the item damage when the item has durability, so this piece has
        // none and never wears. Deviation: spec 05-armor "broken" state does not apply to the electric chestplate.
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L93
        setMaxDamage(0);
    }

    @Override
    protected int wearOf(ItemStack stack) {
        return 0;
    }

    @Override
    protected int durability() {
        return set().material()
            .pieceDurability();
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, net.minecraft.util.DamageSource source,
        int damage, int slot) {
        // No wear: the protection of this piece does not depend on a durability bar.
    }

    @Override
    public @org.jetbrains.annotations.Nullable com.gregguns.armor.logic.RepairSpec repairSpec(ItemStack stack) {
        return null;
    }

    @Override
    public int powerUnits(ItemStack stack) {
        return PowerRules.unitsFromEu(
            ElectricGear.charge(stack),
            set().power()
                .euPerUnit());
    }

    @Override
    public void setPowerUnits(ItemStack stack, int units) {
        // Scenario helper: fill to exactly this many units by topping up from empty or drawing down.
        double want = Math.min(
            units,
            set().power()
                .capacityUnits())
            * set().power()
                .euPerUnit();
        double have = ElectricGear.charge(stack);
        if (want > have) {
            ElectricGear.charge(stack, want - have, set().tier());
        } else if (want < have) {
            ElectricGear.use(stack, have - want, null);
        }
    }

    @Override
    public int spendPower(ItemStack stack, int units, EntityLivingBase user) {
        int before = powerUnits(stack);
        int cost = Math.min(before, Math.max(0, units));
        if (cost > 0) {
            ElectricGear.use(
                stack,
                cost * set().power()
                    .euPerUnit(),
                user);
        }
        return cost;
    }

    // --- IElectricItem ------------------------------------------------------------------------------------------

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public Item getChargedItem(ItemStack stack) {
        return this;
    }

    @Override
    public Item getEmptyItem(ItemStack stack) {
        return this;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return set().power()
            .capacityEu();
    }

    @Override
    public int getTier(ItemStack stack) {
        return ElectricGear.ic2Tier(set().tier());
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return ElectricGear.transferLimit(set().tier());
    }

    // --- display ------------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ElectricGear.addCreativeVariants(
            item,
            set().power()
                .capacityEu(),
            set().tier(),
            list);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addPowerTooltip(ItemStack stack, List tooltip) {
        ElectricGear.addTooltip(
            tooltip,
            stack,
            set().power()
                .capacityEu(),
            false);
    }
}
