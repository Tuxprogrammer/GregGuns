package com.gregguns.entity.npc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.item.gun.GunItems;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Turns the loot core's item references into stacks. {@code mod:name} is looked up in the item registry,
 * {@code ore:Name} takes the first stack of the ore dictionary entry (GT first), and a bare name is a GregGuns gun.
 * An unknown reference gives null (the drop is skipped), never an exception: a pack without the item still plays.
 * Common side, server thread.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md (GGS-10-15, GGS-10-17)
public final class NpcItems {

    private static final String ORE = "ore:";

    private NpcItems() {}

    /**
     * @param ref   an item reference
     * @param count the stack size
     * @return the stack, or null when the reference does not resolve
     */
    public static @Nullable ItemStack stack(String ref, int count) {
        Item item = item(ref);
        if (item != null) {
            return new ItemStack(item, count);
        }
        if (ref.startsWith(ORE)) {
            java.util.List<ItemStack> ores = OreDictionary.getOres(ref.substring(ORE.length()));
            if (!ores.isEmpty()) {
                ItemStack copy = ores.get(0)
                    .copy();
                copy.stackSize = count;
                if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                    copy.setItemDamage(0);
                }
                return copy;
            }
        }
        return null;
    }

    /**
     * @param drop a loot drop
     * @param rand the entity's random source
     * @return the stack with a drawn count, or null when the reference does not resolve
     */
    public static @Nullable ItemStack stack(LootDrop drop, java.util.Random rand) {
        return stack(drop.ref(), drop.count(rand));
    }

    /**
     * @param ref an item reference
     * @return the item for a {@code mod:name} reference or a bare gun name, or null (including for {@code ore:})
     */
    public static @Nullable Item item(String ref) {
        if (ref.startsWith(ORE)) {
            return null;
        }
        int colon = ref.indexOf(':');
        if (colon < 0) {
            return GunItems.get(ref);
        }
        Item item = GameRegistry.findItem(ref.substring(0, colon), ref.substring(colon + 1));
        if (item == null) {
            GregGuns.LOG.debug("NPC item reference {} is not registered", ref);
        }
        return item;
    }
}
