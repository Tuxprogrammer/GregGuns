package com.gregguns.client.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.Tags;
import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.core.module.ModuleContent;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.registry.Naming;
import com.gregguns.item.NeiOrder;
import com.gregguns.item.NeiOrderRemap;
import com.gregguns.item.ammo.ItemAmmoRound;
import com.gregguns.item.ammo.ItemMagazine;
import com.gregguns.item.ammo.ItemMagazineEmpty;
import com.gregguns.item.gun.ItemGun;

import codechicken.nei.ItemSorter;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.api.ItemFilter;

/**
 * NEI plugin that makes NEI list GregGuns items in {@link NeiOrder} order in every world, including worlds created
 * before the order was fixed (those keep their saved numeric item ids, which is what NEI's "Item ID" sort reads).
 *
 * <p>
 * NEI has no "custom order" setting and no IMC message for ordering. Its only ordering hook is the sort-option list
 * ({@code API.addSortOption}, {@code ItemSorter}); the pack's {@code client.cfg} ships it as
 * {@code minecraft, mod, id, default, damage, name}. A new option would be saved after {@code name}, where it can never
 * decide anything, so this plugin swaps the comparator of the existing {@code nei.itemsort.id} entry instead: the row
 * keeps its place in the player's sort list, and GregGuns items compare by their effective id (see
 * {@link NeiOrderRemap}) while every other item compares by its real id. Client only; NEI discovers this class by its
 * {@code NEI...Config} name.
 */
// Source:
// https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/ItemSorter.java#L84-L89
// (the "nei.itemsort.id" comparator) and
// https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/NEIClientConfig.java#L1017-L1034
// (plugins' loadConfig runs before ItemSorter.loadConfig rebuilds the active list from the entries)
// https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L319-L322
// (addSubset(String, ItemFilter): the dotted name builds the subset menu path)
// Spec: docs/gregguns-spec/06-items/nei-order.md, docs/gregguns-spec/06-items/nei-visibility.md
public class NEIGregGunsConfig implements IConfigureNEI {

    private static final String ID_SORT = "nei.itemsort.id";

    @Override
    public void loadConfig() {
        // Content of a module that is off stays registered but is hidden from NEI. A filter provider (not a one-shot
        // API.hideItem list) because the flags in force change when the server's flags arrive at login.
        API.addItemFilter(() -> {
            int mask = ModuleState.currentMask();
            return stack -> {
                if (isTechnical(stack)) {
                    return false;
                }
                ModuleId owner = ModuleContent.moduleOf(stack);
                return owner == null || (mask & (1 << owner.ordinal())) != 0;
            };
        });
        // Subsets (NEI's "Item Subsets" menu): the in-code stand-in for the pack's collapsibleitems.cfg, which NEI
        // exposes no API for. The lists build lazily, from the registered items, when the menu is first opened.
        addSubset("Guns", stack -> stack.getItem() instanceof ItemGun);
        addSubset(
            "Ammo",
            stack -> stack.getItem() instanceof ItemAmmoRound || stack.getItem() instanceof ItemMagazine
                || stack.getItem() instanceof ItemMagazineEmpty);
        addSubset("Armor", stack -> stack.getItem() instanceof ItemGregGunsArmor);
        Map<String, Integer> effective = NeiOrderRemap.effectiveIds(qualifiedNames(), NEIGregGunsConfig::realId);
        if (effective.isEmpty()) {
            return;
        }
        ItemSorter.SortEntry original = ItemSorter.find(ID_SORT);
        if (original == null) {
            GregGuns.LOG.warn("NEI has no {} sort entry; GregGuns items keep NEI's own order", ID_SORT);
            return;
        }
        ItemSorter.SortEntry ours = new ItemSorter.SortEntry(
            ID_SORT,
            (a, b) -> Integer.compare(key(a, effective), key(b, effective)));
        replace(ItemSorter.entries, original, ours);
        replace(ItemSorter.list, original, ours);
        GregGuns.LOG.info("NEI: {} GregGuns items sorted in table order (existing worlds included)", effective.size());
    }

    /**
     * @param stack a stack
     * @return true for a GregGuns placeholder or technical item or block (marked {@link HiddenFromCreativeTab}), which
     *         NEI must not list (GGS-06-26)
     */
    static boolean isTechnical(ItemStack stack) {
        Item item = stack.getItem();
        if (item == null) {
            return false;
        }
        if (item instanceof HiddenFromCreativeTab) {
            return true;
        }
        Block block = Block.getBlockFromItem(item);
        return block instanceof HiddenFromCreativeTab;
    }

    private static void addSubset(String name, ItemFilter filter) {
        API.addSubset("GregGuns." + name, filter);
    }

    private static int key(ItemStack stack, Map<String, Integer> effective) {
        Item item = stack.getItem();
        if (item == null) {
            return Integer.MAX_VALUE;
        }
        String name = Item.itemRegistry.getNameForObject(item);
        Integer id = name == null ? null : effective.get(name);
        return id != null ? id : Item.getIdFromItem(item);
    }

    private static void replace(List<ItemSorter.SortEntry> entries, ItemSorter.SortEntry from,
        ItemSorter.SortEntry to) {
        int index = entries.indexOf(from);
        if (index >= 0) {
            entries.set(index, to);
        }
    }

    private static List<String> qualifiedNames() {
        List<String> out = new ArrayList<>();
        for (String name : NeiOrder.names()) {
            out.add(Naming.qualifiedName(name));
        }
        return out;
    }

    private static Integer realId(String qualifiedName) {
        Item item = (Item) Item.itemRegistry.getObject(qualifiedName);
        return item == null ? null : Item.getIdFromItem(item);
    }

    @Override
    public String getName() {
        return "GregGuns NEI Plugin";
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
