package com.gregguns.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Resolves an item from a {@code modid:name} id, such as the pack's Tanned Leather ({@code Backpack:tannedLeather},
 * which the armor recipes and repairs consume), or from a bare GregGuns registry name. Answers {@code null} when the
 * mod
 * or the item is missing, so callers skip the recipe or the match instead of failing. Common side; call after preInit.
 */
public final class PackItems {

    private PackItems() {}

    /**
     * @param id a {@code modid:name} item id, or a bare GregGuns registry name
     * @return the registered item, or {@code null} when the id is malformed or nothing is registered under it
     */
    public static @Nullable Item find(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) {
            return GameRegistry.findItem(GregGuns.MODID, id);
        }
        if (colon == 0 || colon == id.length() - 1) {
            return null;
        }
        return GameRegistry.findItem(id.substring(0, colon), id.substring(colon + 1));
    }

    /**
     * @param id    a {@code modid:name} item id, or a bare GregGuns registry name
     * @param count stack size
     * @return a damage-0 stack of the item, or {@code null} when it does not resolve
     */
    public static @Nullable ItemStack stack(String id, int count) {
        Item item = find(id);
        return item == null ? null : new ItemStack(item, count);
    }
}
