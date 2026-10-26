package com.gregguns.armor;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.PowerRules;
import com.gregguns.armor.logic.PowerSpec;
import com.gregguns.armor.logic.SetDef;

import gregtech.api.enums.ItemList;

/**
 * Runtime side of powered armor: finds a wearer's chestplate, asks whether the set is powered, takes the walking and
 * action costs, and refuels a steam chestplate from Coal or Charcoal, or a Compressed Air Tank. The numbers are in
 * {@link PowerRules}.
 *
 * <p>
 * The chestplate holds the energy. Each server tick {@link #tick} drains it, refuels it, and writes a {@code powered}
 * flag to the other worn pieces of the set so GT's hazard answers (which only see the piece's own stack) can follow the
 * charge. Bonuses never read the flag: {@link Armor#worn} asks {@link #setPowered} directly. Server for state, both
 * sides for the reads.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 9 (power system); docs/gregguns-spec/05-armor/
// hazard-protection.md (GGS-05-11)
public final class ArmorPower {

    private ArmorPower() {}

    /** Equipment slot index of the chestplate in {@code getEquipmentInSlot} (0 held, 1 boots to 4 helmet). */
    private static final int CHEST_SLOT = 3;

    /**
     * @param entity a living entity
     * @param set    a powered set
     * @return the entity's worn chestplate stack when it is a chestplate of {@code set}, else null
     */
    public static @Nullable ItemStack chestOf(EntityLivingBase entity, SetDef set) {
        ItemStack stack = entity.getEquipmentInSlot(CHEST_SLOT);
        ItemGregGunsArmor item = stack == null ? null : ArmorItems.of(stack.getItem());
        return item != null && item.set() == set ? stack : null;
    }

    /**
     * @param entity a living entity
     * @param set    a set
     * @return true when the set has power, its chestplate is worn and intact, and it holds charge
     */
    public static boolean setPowered(EntityLivingBase entity, SetDef set) {
        if (!set.power()
            .powered()) {
            return false;
        }
        ItemStack chest = chestOf(entity, set);
        if (chest == null) {
            return false;
        }
        ItemGregGunsArmor item = (ItemGregGunsArmor) chest.getItem();
        return !item.isBroken(chest) && PowerRules.isPowered(item.powerUnits(chest));
    }

    /**
     * Takes units from the wearer's chestplate of {@code set}. Server side.
     *
     * @param entity the wearer
     * @param set    the powered set
     * @param units  the cost
     * @return the units taken, 0 without a chestplate
     */
    public static int spend(EntityLivingBase entity, SetDef set, int units) {
        ItemStack chest = chestOf(entity, set);
        return chest == null ? 0 : ((ItemGregGunsArmor) chest.getItem()).spendPower(chest, units, entity);
    }

    /**
     * One server tick of the wearer's powered set: refuel an empty steam chestplate, drain the walking cost, and mirror
     * the result into the other pieces' flags. Does nothing for a wearer without a chestplate of a powered set (their
     * other powered pieces get their flag cleared).
     *
     * @param player    the wearer
     * @param moving    whether the wearer walked this tick ({@link PowerRules#isMoving})
     * @param worldTime the world's total tick count
     */
    public static void tick(EntityPlayer player, boolean moving, long worldTime) {
        ItemStack chestStack = player.getEquipmentInSlot(CHEST_SLOT);
        ItemGregGunsArmor chest = chestStack == null ? null : ArmorItems.of(chestStack.getItem());
        SetDef set = chest != null && chest.set()
            .power()
            .powered() ? chest.set() : null;
        boolean powered = false;
        if (set != null && !chest.isBroken(chestStack)) {
            int units = chest.powerUnits(chestStack);
            if (set.power()
                .kind() == PowerSpec.PowerKind.STEAM && refuel(player, chest, chestStack, units, worldTime)) {
                units = chest.powerUnits(chestStack);
            }
            if (PowerRules.isPowered(units)) {
                chest.spendPower(
                    chestStack,
                    PowerRules.drainThisTick(intactPieces(player, set), moving, worldTime),
                    player);
                powered = PowerRules.isPowered(chest.powerUnits(chestStack));
            }
        }
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = player.getEquipmentInSlot(slot);
            ItemGregGunsArmor item = stack == null ? null : ArmorItems.of(stack.getItem());
            if (item != null && item.set()
                .power()
                .powered() && slot != CHEST_SLOT) {
                item.setPoweredFlag(stack, powered && item.set() == set);
            }
        }
    }

    private static int intactPieces(EntityLivingBase entity, SetDef set) {
        int n = 0;
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = entity.getEquipmentInSlot(slot);
            ItemGregGunsArmor item = stack == null ? null : ArmorItems.of(stack.getItem());
            if (item != null && item.set() == set && !item.isBroken(stack)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Refuels a steam chestplate: a Coal or Charcoal from the inventory when the chestplate has room for a full
     * {@link PowerRules#FUEL_REFUEL_UNITS} (checked once a second, so a full inventory scan is not a per-tick cost,
     * and at once when the chestplate is empty), else a Compressed Air Tank when it is empty.
     *
     * @return whether anything was burned
     */
    private static boolean refuel(EntityPlayer player, ItemGregGunsArmor chest, ItemStack chestStack, int units,
        long worldTime) {
        int capacity = chest.set()
            .power()
            .capacityUnits();
        boolean empty = !PowerRules.isPowered(units);
        if ((empty || worldTime % 20L == 0L) && PowerRules.canTakeFuel(units, capacity)
            && burnFuel(player, chest, chestStack, units, capacity)) {
            return true;
        }
        return empty && refuelFromTank(player, chest, chestStack);
    }

    /** Burns one Coal or Charcoal from the main inventory into the chestplate. */
    private static boolean burnFuel(EntityPlayer player, ItemGregGunsArmor chest, ItemStack chestStack, int units,
        int capacity) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && isFuel(stack)) {
                if (--stack.stackSize <= 0) {
                    player.inventory.mainInventory[i] = null;
                }
                chest.setPowerUnits(chestStack, PowerRules.afterFuel(units, capacity));
                player.inventoryContainer.detectAndSendChanges();
                return true;
            }
        }
        return false;
    }

    /**
     * @param stack an item stack
     * @return whether it is Coal or Charcoal: the vanilla item, or anything GT or another mod files under
     *         {@code gemCoal} or {@code gemCharcoal}
     */
    static boolean isFuel(ItemStack stack) {
        if (stack.getItem() == Items.coal && stack.getItemDamage() <= 1) {
            return true;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (PowerRules.isFuelOre(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Burns one Compressed Air Tank from the wearer's inventory into a full steam chestplate and hands back an empty
     * cell (Deviation: Techguns returns an "empty tank" item that GregGuns does not have; the tank is made from a GT
     * Empty Cell, RC-54).
     */
    private static boolean refuelFromTank(EntityPlayer player, ItemGregGunsArmor chest, ItemStack chestStack) {
        if (ArmorParts.tankAir() == null) {
            return false;
        }
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.getItem() == ArmorParts.tankAir()) {
                if (--stack.stackSize <= 0) {
                    player.inventory.mainInventory[i] = null;
                }
                chest.setPowerUnits(
                    chestStack,
                    chest.set()
                        .power()
                        .capacityUnits());
                ItemStack empty = ItemList.Cell_Empty.get(1);
                if (empty != null && !player.inventory.addItemStackToInventory(empty)) {
                    player.dropPlayerItemWithRandomChoice(empty, false);
                }
                player.inventoryContainer.detectAndSendChanges();
                return true;
            }
        }
        return false;
    }

    /**
     * @param entity a living entity
     * @param slot   the slot to look at
     * @return the worn powered piece in that slot when its set is powered right now, else null
     */
    public static @Nullable ItemGregGunsArmor poweredPiece(EntityLivingBase entity, ArmorSlot slot) {
        ItemStack stack = entity.getEquipmentInSlot(4 - slot.armorType());
        ItemGregGunsArmor item = stack == null ? null : ArmorItems.of(stack.getItem());
        if (item == null || item.isBroken(stack) || !setPowered(entity, item.set())) {
            return null;
        }
        return item;
    }
}
