package com.gregguns.armor.slots;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChatComponentTranslation;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.Armor;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.oxygen.FallbackOxygenMask;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.armor.slots.logic.FilterWear;
import com.gregguns.core.config.EquipmentConfig;

/**
 * The per-player tick and queries for special-slot equipment: night vision gear (goggles in {@code head}, Tactical Mask
 * in the helmet slot), the Gas Mask and Tactical Mask cure, and the back device in {@code wings}. The armor module
 * calls {@link #tick} from its single player tick; scenarios call it on a fake player. Server side only for state.
 *
 * <p>
 * Usage: {@code boolean nv = slots.tick(player);} then feed {@code nv} into the night-vision decision.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-05, GGS-05-06); docs/gregguns-spec/05-armor/
// hazard-protection.md (GGS-05-13)
public final class SlotEquipment {

    // Back device each player wore last tick, to fire onBackRemoved once. Cleared by forget(); runtime tracking, not
    // saved state.
    private final Map<UUID, Item> lastBack = new HashMap<>();

    /** Creates the tracker; the armor module owns one instance ({@code ArmorEvents}). */
    public SlotEquipment() {}

    /**
     * Runs one tick of every worn special-slot item for a player.
     *
     * @param player a player; nothing happens on the client
     * @return true when worn gear wants night vision this tick (switched on and the EU was paid)
     */
    public boolean tick(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return false;
        }
        boolean nightVision = false;
        for (ItemStack stack : BaublesCompat.worn(player, BaubleSlot.HEAD)) {
            if (stack.getItem() instanceof NightVisionGear) {
                nightVision |= ((NightVisionGear) stack.getItem()).tickNightVision(stack, player);
            }
        }
        ItemStack helmet = player.getEquipmentInSlot(4);
        if (helmet != null) {
            if (helmet.getItem() instanceof NightVisionGear) {
                nightVision |= ((NightVisionGear) helmet.getItem()).tickNightVision(helmet, player);
            }
            cure(player, helmet);
        }
        if (nightVision) {
            // Goggles lift blindness only while they work (baubles-slots.md, night vision).
            player.removePotionEffect(Potion.blindness.id);
        }
        tickBack(player);
        return nightVision;
    }

    private static void cure(EntityPlayer player, ItemStack helmet) {
        boolean poisoned = player.isPotionActive(Potion.poison);
        boolean withered = player.isPotionActive(Potion.wither);
        if (!poisoned && !withered) {
            return;
        }
        if (helmet.getItem() instanceof ItemGasMask) {
            FilterWear.Result r = FilterWear.tick(helmet.getItemDamage(), helmet.getMaxDamage(), poisoned, withered);
            if (r.curePoison()) {
                player.removePotionEffect(Potion.poison.id);
            }
            if (r.cureWither()) {
                player.removePotionEffect(Potion.wither.id);
            }
            helmet.setItemDamage(r.damage());
        } else if (helmet.getItem() instanceof ItemTacticalMask) {
            // One cure costs the night-vision upkeep in EU (Techguns: one damage each of the shared charge).
            double cost = EquipmentConfig.nightVisionDrainEuPerTick;
            for (Potion potion : new Potion[] { Potion.poison, Potion.wither }) {
                if (player.isPotionActive(potion) && ElectricGear.use(helmet, cost, player)) {
                    player.removePotionEffect(potion.id);
                }
            }
        }
    }

    private void tickBack(EntityPlayer player) {
        ItemStack stack = BaublesCompat.first(player, BaubleSlot.WINGS);
        Item now = stack != null && stack.getItem() instanceof BackDevice ? stack.getItem() : null;
        Item before = lastBack.get(player.getUniqueID());
        if (before != now) {
            if (before instanceof BackDevice) {
                ((BackDevice) before).onBackRemoved(player, before);
            }
            if (now == null) {
                lastBack.remove(player.getUniqueID());
            } else {
                lastBack.put(player.getUniqueID(), now);
            }
        }
        if (now != null) {
            ((BackDevice) now).onBackTick(player, stack);
        }
    }

    /**
     * Forgets a player's tracked state, firing {@code onBackRemoved} for a worn device. Call on logout, death, respawn
     * and dimension change, and from scenarios.
     *
     * @param player a player
     */
    public void forget(EntityPlayer player) {
        Item before = lastBack.remove(player.getUniqueID());
        if (before instanceof BackDevice && !player.worldObj.isRemote) {
            ((BackDevice) before).onBackRemoved(player, before);
        }
    }

    /**
     * Forwards the back-device action key. A later branch's keybind listener calls this.
     *
     * @param player a server player
     * @param down   true on press, false on release
     */
    public static void backKey(EntityPlayer player, boolean down) {
        ItemStack stack = BaublesCompat.first(player, BaubleSlot.WINGS);
        if (stack != null && stack.getItem() instanceof BackDevice) {
            ((BackDevice) stack.getItem()).onBackKey(player, stack, down);
        }
    }

    /**
     * @param player a player
     * @return whether a worn item gives oxygen gear: armor with the OXYGEN_GEAR bonus, a powered Tactical Mask, or the
     *         fallback Oxygen Mask in the head slot
     */
    public static boolean hasOxygenGear(EntityPlayer player) {
        if (Armor.bonus(player, ArmorBonus.OXYGEN_GEAR) > 0.0) {
            return true;
        }
        ItemStack helmet = player.getEquipmentInSlot(4);
        if (helmet != null && helmet.getItem() instanceof ItemTacticalMask
            && ((ItemTacticalMask) helmet.getItem()).powered(helmet)) {
            return true;
        }
        for (ItemStack stack : BaublesCompat.worn(player, BaubleSlot.HEAD)) {
            if (stack.getItem() instanceof FallbackOxygenMask) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flips the night-vision switch of the worn gear (the key action). Goggles in the head slot win over the Tactical
     * Mask. Server side.
     *
     * @param player a server player
     * @return the new state, or null when nothing switchable is worn
     */
    public static @Nullable Boolean toggleNightVision(EntityPlayer player) {
        ItemStack target = null;
        List<ItemStack> head = BaublesCompat.worn(player, BaubleSlot.HEAD);
        for (ItemStack stack : head) {
            if (stack.getItem() instanceof NightVisionGear) {
                target = stack;
            }
        }
        if (target == null) {
            ItemStack helmet = player.getEquipmentInSlot(4);
            if (helmet != null && helmet.getItem() instanceof NightVisionGear) {
                target = helmet;
            }
        }
        if (target == null) {
            return null;
        }
        NightVisionGear gear = (NightVisionGear) target.getItem();
        boolean on = !gear.nightVisionEnabled(target);
        gear.setNightVisionEnabled(target, on);
        player.addChatMessage(
            new ChatComponentTranslation(on ? "gregguns.equipment.nv.on" : "gregguns.equipment.nv.off"));
        return on;
    }
}
