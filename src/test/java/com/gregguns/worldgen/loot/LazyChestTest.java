package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gregguns.utility.storage.TileGregChest;

/**
 * Lazy chests: the order survives NBT, the chest fills exactly once whichever accessor comes first, and contents
 * depend only on the seed (GGS-11-20, WGEN-25). The draw is replaced by a seeded fake so no items need registering.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-20); fixes WGEN-25
class LazyChestTest {

    private static final Item FAKE = new Item();
    private int draws;

    @BeforeEach
    void fakeFiller() {
        draws = 0;
        LazyChests.setFiller((inv, category, richness, rand) -> {
            draws++;
            int n = 3 + richness + rand.nextInt(4);
            for (int i = 0; i < n; i++) {
                inv.setInventorySlotContents(rand.nextInt(inv.getSizeInventory()), new ItemStack(FAKE, 1 + rand.nextInt(9)));
            }
        });
    }

    @AfterEach
    void restore() {
        LazyChests.setFiller(null);
    }

    private static List<String> snapshot(IInventory inv) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            out.add(s == null ? "-" : String.valueOf(s.stackSize));
        }
        return out;
    }

    private static TileGregChest pendingChest(long seed) {
        TileGregChest chest = new TileGregChest();
        chest.setPending(new LootPending("gregguns:test", 1, seed));
        return chest;
    }

    @Test
    void fillsExactlyOnce() {
        TileGregChest chest = pendingChest(42L);
        assertNotNull(chest.pending());
        List<String> first = snapshot(chest);
        assertEquals(1, draws);
        assertNull(chest.pending());
        assertEquals(first, snapshot(chest));
        chest.openInventory();
        chest.ensureFilled();
        assertEquals(1, draws, "later accessors must not draw again");
        assertTrue(first.stream().anyMatch(s -> !s.equals("-")));
    }

    @Test
    void emptiedChestStaysEmpty() {
        TileGregChest chest = pendingChest(7L);
        for (int i = 0; i < TileGregChest.SLOTS; i++) {
            chest.setInventorySlotContents(i, null);
        }
        assertEquals(1, draws);
        assertTrue(snapshot(chest).stream().allMatch(s -> s.equals("-")), "a looted chest is not refilled");
    }

    @Test
    void everyAccessorFillsAndSeedDecidesContents() {
        List<String> reference = snapshot(pendingChest(99L));
        TileGregChest viaDecr = pendingChest(99L);
        viaDecr.decrStackSize(0, 1);
        assertEquals(1, draws - 1);
        TileGregChest viaClosing = pendingChest(99L);
        viaClosing.getStackInSlotOnClosing(0);
        TileGregChest viaInsert = pendingChest(99L);
        viaInsert.insert(new ItemStack(FAKE, 1));
        TileGregChest viaRaw = pendingChest(99L);
        viaRaw.openInventory();
        // Same seed, same draw, whichever accessor ran first (insert and decr change the touched slot only).
        assertEquals(reference, snapshot(viaRaw));
        assertEquals(5, draws);
        assertNull(viaDecr.pending());
        assertNull(viaClosing.pending());
        assertNull(viaInsert.pending());
    }

    @Test
    void openOrderDoesNotMatter() {
        TileGregChest a1 = pendingChest(1L);
        TileGregChest b1 = pendingChest(2L);
        List<String> a1First = snapshot(a1);
        List<String> b1Second = snapshot(b1);
        TileGregChest b2 = pendingChest(2L);
        TileGregChest a2 = pendingChest(1L);
        List<String> b2First = snapshot(b2);
        List<String> a2Second = snapshot(a2);
        assertEquals(a1First, a2Second);
        assertEquals(b1Second, b2First);
    }

    @Test
    void seedDependsOnEveryInput() {
        long base = LootPending.seed(1234L, 3, -5, 0);
        assertEquals(base, LootPending.seed(1234L, 3, -5, 0));
        assertNotEquals(base, LootPending.seed(1235L, 3, -5, 0));
        assertNotEquals(base, LootPending.seed(1234L, 4, -5, 0));
        assertNotEquals(base, LootPending.seed(1234L, 3, -4, 0));
        assertNotEquals(base, LootPending.seed(1234L, 3, -5, 1));
        assertNotEquals(LootPending.seed(1234L, 3, 5, 0), LootPending.seed(1234L, 5, 3, 0));
    }

    @Test
    void pendingRoundTripsThroughNbtUnfilled() {
        TileGregChest chest = pendingChest(555L);
        NBTTagCompound tag = new NBTTagCompound();
        chest.writeToNBT(tag);
        assertEquals(0, draws, "saving must not fill");
        TileGregChest loaded = new TileGregChest();
        loaded.readFromNBT(tag);
        assertEquals(new LootPending("gregguns:test", 1, 555L), loaded.pending());
        assertEquals(snapshot(pendingChest(555L)), snapshot(loaded));
    }

    @Test
    void filledChestWritesNoPendingTag() {
        TileGregChest chest = pendingChest(8L);
        snapshot(chest);
        NBTTagCompound tag = new NBTTagCompound();
        chest.writeToNBT(tag);
        assertTrue(!tag.hasKey(LootPending.KEY));
        TileGregChest loaded = new TileGregChest();
        loaded.readFromNBT(tag);
        assertNull(loaded.pending());
        assertEquals(snapshot(chest), snapshot(loaded));
        assertEquals(1, draws);
    }

    @Test
    void richnessIsClampedAndEagerHelperMatchesLazy() {
        assertEquals(2, new LootPending("c", 9, 1L).richness());
        assertEquals(0, new LootPending("c", -3, 1L).richness());
        TileGregChest lazy = pendingChest(31L);
        TileGregChest eager = new TileGregChest();
        LazyChests.fillEagerly(eager, new LootPending("gregguns:test", 1, 31L));
        assertEquals(snapshot(lazy), snapshot(eager));
        assertEquals(new Random(5L).nextInt(), new LootPending("c", 0, 5L).random().nextInt());
    }
}
