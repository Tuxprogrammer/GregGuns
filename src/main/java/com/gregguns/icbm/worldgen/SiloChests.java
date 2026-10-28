package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.ChestGenHooks;

import com.gregguns.GregGuns;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootPending;

/**
 * Registers the silo complex loot categories with Forge and fills a chest from a {@link Placement}. Chests fill at
 * generation, from a seed made of the world seed, the complex's cell and the chest's index only, so a chest holds the
 * same items whichever way it is first reached (GGS-11-20). Server side, common.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Tables)
public final class SiloChests {

    private static boolean registered;

    private SiloChests() {}

    /**
     * Registers every key and ceiling once, in postInit (GT parts resolve then). A line that does not resolve is
     * skipped.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (String key : SiloLootTables.KEYS) {
            for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                LootCategory cat = SiloLootTables.category(key, c);
                ChestGenHooks info = ChestGenHooks.getInfo(cat.name());
                info.setMin(cat.minItems());
                info.setMax(cat.maxItems());
                for (SiloLootTables.Line l : SiloLootTables.lines(key, c)) {
                    ItemStack s = LootCategories.stack(l.ref(), 1);
                    if (s == null) {
                        GregGuns.LOG.debug("Silo loot {} skips {}: not registered", cat.name(), l.ref());
                        continue;
                    }
                    info.addItem(new net.minecraft.util.WeightedRandomChestContent(s, l.min(), l.max(), l.weight()));
                }
            }
        }
    }

    /**
     * Fills an inventory for a chest placement: the table's draw, then the bonus items in free slots.
     *
     * @param inv       the chest
     * @param p         a {@link Placement.Kind#CHEST} placement
     * @param worldSeed the world seed
     * @param cellX     the complex's grid cell X
     * @param cellZ     the complex's grid cell Z
     */
    public static void fill(IInventory inv, Placement p, long worldSeed, int cellX, int cellZ) {
        Random rand = new LootPending(
            p.category(),
            p.richness(),
            LootPending.seed(worldSeed, cellX, cellZ, p.chestIndex())).random();
        LootCategories.fill(inv, p.category(), p.richness(), rand);
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (inv.getStackInSlot(i) == null) {
                free.add(i);
            }
        }
        for (Placement.Bonus b : p.bonuses()) {
            ItemStack s = SiloBonusItems.stack(b, p);
            if (s == null || free.isEmpty()) {
                continue;
            }
            inv.setInventorySlotContents(free.remove(rand.nextInt(free.size())), s);
        }
    }

    /**
     * @param p         a chest placement
     * @param worldSeed the world seed
     * @param cellX     cell X
     * @param cellZ     cell Z
     * @return a new vanilla chest tile, filled
     */
    public static TileEntityChest filledChest(Placement p, long worldSeed, int cellX, int cellZ) {
        TileEntityChest chest = new TileEntityChest();
        fill(chest, p, worldSeed, cellX, cellZ);
        return chest;
    }
}
