package com.gregguns.entity.npc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.ArmorItems;
import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.entity.npc.loot.EquipmentProfile;

/**
 * Applies a rolled {@link EquipmentProfile.Gear} to an NPC through the vanilla equipment slots. A gun or armor set that
 * is not registered (its module is off, or it is not built yet) is skipped, and the roll never picks a gun that does
 * not exist. Armor is the existing {@code ArmorItems} with the camo in the stack NBT. Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-17),
// docs/techguns-spec/10-entities-npcs/per-mob.md
public final class NpcEquipment {

    private NpcEquipment() {}

    /**
     * Rolls and applies equipment with the entity's own random source.
     *
     * @param npc        the NPC
     * @param profile    what it may carry; null leaves it as is
     * @param difficulty the spawn danger 0 to 5
     */
    public static void apply(NpcEntity npc, EquipmentProfile profile, int difficulty) {
        if (profile == null) {
            return;
        }
        EquipmentProfile.Gear gear = profile
            .roll(npc.getRNG(), difficulty, ref -> NpcItems.item(ref) != null, NpcEquipment::camoCount);
        if (gear.weapon() != null) {
            Item weapon = NpcItems.item(gear.weapon());
            npc.setCurrentItemOrArmor(0, new ItemStack(weapon));
        }
        for (EquipmentProfile.ArmorPick pick : gear.armor()) {
            SetDef set = ArmorCatalog.find(pick.setId());
            ItemGregGunsArmor piece = set == null ? null : ArmorItems.get(set, ArmorSlot.fromArmorType(pick.slot()));
            if (piece == null) {
                continue;
            }
            ItemStack stack = new ItemStack(piece);
            piece.setCamo(stack, pick.camo());
            // Vanilla equipment slot: 1 boots to 4 helmet.
            npc.setCurrentItemOrArmor(4 - pick.slot(), stack);
        }
    }

    private static int camoCount(String setId) {
        SetDef set = ArmorCatalog.find(setId);
        return set == null ? 1 : set.camoCount();
    }
}
