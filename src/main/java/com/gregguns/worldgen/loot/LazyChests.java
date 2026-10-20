package com.gregguns.worldgen.loot;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

/**
 * The fill step shared by lazy GregGuns chests and eagerly filled vanilla chests. Both run the same seeded draw, so a
 * chest's contents are the same either way (GGS-11-20). Server side.
 *
 * <p>
 * Vanilla {@link TileEntityChest} cannot hold a pending tag, so a structure calls {@link #fillVanillaChest} when it
 * places one. GregGuns chests take a {@link LootPending} (see {@code TileGregChest#setPending}) and fill themselves on
 * first access.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-20); plan M8 Q8 (lazy for ours, eager for vanilla)
public final class LazyChests {

    /** The draw itself; replaceable so unit tests need no registered items. */
    public interface Filler {

        /**
         * @param inv      the inventory to fill
         * @param category the category name
         * @param richness 0..2
         * @param rand     the seeded random
         */
        void fill(IInventory inv, String category, int richness, Random rand);
    }

    private static volatile Filler filler = LootCategories::fill;

    private LazyChests() {}

    /** @param f the filler to use, or null to restore the real {@link LootCategories#fill}; tests only */
    public static void setFiller(Filler f) {
        filler = f != null ? f : LootCategories::fill;
    }

    /**
     * Fills an inventory now from a pending order.
     *
     * @param inv     the inventory
     * @param pending the order
     */
    public static void fillEagerly(IInventory inv, LootPending pending) {
        filler.fill(inv, pending.category(), pending.richness(), pending.random());
    }

    /**
     * Fills the vanilla chest at a position, if there is one. Use for chests that cannot be lazy.
     *
     * @param world   the world (server)
     * @param x       block X
     * @param y       block Y
     * @param z       block Z
     * @param pending the order
     * @return true when a vanilla chest was there and got filled
     */
    public static boolean fillVanillaChest(World world, int x, int y, int z, LootPending pending) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityChest)) {
            return false;
        }
        fillEagerly((TileEntityChest) tile, pending);
        return true;
    }
}
