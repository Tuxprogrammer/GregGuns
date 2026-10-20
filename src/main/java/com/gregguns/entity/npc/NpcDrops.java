package com.gregguns.entity.npc;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;

import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.LootTable;
import com.gregguns.entity.npc.loot.NpcProfile;

/**
 * Builds an NPC's drops from its {@link NpcProfile}. Called from the {@code dropFewItems} and {@code dropRareDrop}
 * overrides of {@link NpcEntity}.
 *
 * <p>
 * Simulation-friendly (Mobs Info replaces the entity's {@code rand} and walks every branch): every draw uses the
 * entity's own {@link Random}, nothing reads world state, and the number of draws does not depend on whether an item
 * resolves. Fixed drops fall whatever killed the mob; table rolls are {@link LootTable#rolls}; the rare drop is a fresh
 * copy of the held item (count 1, no NBT), on player kills only, and only for profiles that allow it. Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Drops), docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md
// (GGS-10-15, GGS-10-16, GGS-10-19), BUGS NPC-04, NPC-05
public final class NpcDrops {

    private NpcDrops() {}

    /**
     * Drops the fixed items and the table rolls.
     *
     * @param npc     the dying NPC
     * @param profile its profile
     * @param looting looting level of the kill
     */
    public static void dropFewItems(EntityLiving npc, NpcProfile profile, int looting) {
        Random rand = npc.getRNG();
        for (LootDrop fixed : profile.fixedDrops()) {
            drop(npc, NpcItems.stack(fixed, rand));
        }
        LootTable table = profile.table();
        if (table == null) {
            return;
        }
        int rolls = LootTable.rolls(rand, looting);
        for (int i = 0; i < rolls; i++) {
            LootDrop pick = table.pick(rand);
            if (pick != null) {
                drop(npc, NpcItems.stack(pick, rand));
            }
        }
    }

    /**
     * Drops a fresh copy of the held item, if the profile allows it.
     *
     * @param npc     the dying NPC
     * @param profile its profile
     */
    public static void dropRareDrop(EntityLiving npc, NpcProfile profile) {
        ItemStack held = npc.getHeldItem();
        if (profile.rareDrop() && held != null) {
            drop(npc, new ItemStack(held.getItem(), 1));
        }
    }

    private static void drop(EntityLiving npc, ItemStack stack) {
        if (stack != null && stack.stackSize > 0) {
            npc.entityDropItem(stack, 0.0F);
        }
    }
}
