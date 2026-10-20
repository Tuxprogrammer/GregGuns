package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.storage.TileGregChest;

/**
 * Scenarios of the lazy structure chests (M8 G8). Dev environment only; needs {@code utility} and {@code worldgen} in
 * {@code GG_MODULES}. Server side. Each scenario empties the chests and kills the item entities it caused, so nothing
 * is left behind.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-20)
public final class ChestScenarios {

    private static final long SEED = 0x5EEDC0DEL;

    private ChestScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new LazyFill());
        registry.register(new LazyBreak());
    }

    private static List<ItemStack> contents(net.minecraft.inventory.IInventory inv) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            out.add(s == null ? null : s.copy());
        }
        return out;
    }

    private static boolean same(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            ItemStack x = a.get(i);
            ItemStack y = b.get(i);
            if (x == null ? y != null : y == null || !ItemStack.areItemStacksEqual(x, y)) {
                return false;
            }
        }
        return true;
    }

    private static int count(List<ItemStack> list) {
        int n = 0;
        for (ItemStack s : list) {
            if (s != null) {
                n++;
            }
        }
        return n;
    }

    private static final class LazyFill implements Scenario {

        @Override
        public String name() {
            return "worldgen.lazy_chest_fill";
        }

        @Override
        public String description() {
            return "A chest with a pending loot tag fills on its first inventory read (a hopper-like accessor), holds the"
                + " same items as an independent draw of the same seed, and keeps them on reopen (GGS-11-20).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.setBlock(0, 0, 0, WorldBlockRegistration.reinforcedChest(), 3);
            World world = ctx.world();
            TileEntity te = world.getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
            if (!(te instanceof TileGregChest)) {
                out.check(false, "the chest has no GregGuns tile");
                return out.build();
            }
            TileGregChest chest = (TileGregChest) te;
            LootPending order = new LootPending(LootCategoryDefs.WEAPONCHEST_T1, 1, SEED);
            chest.setPending(order);
            out.check(chest.pending() != null, "the chest starts unfilled");
            // Hopper-like read: size, then slot by slot, no GUI.
            List<ItemStack> first = contents(chest);
            out.check(chest.pending() == null, "the first read filled the chest");
            out.check(count(first) > 0, "the chest holds items after the first read");
            // The same draw, made independently, must match (order independence).
            InventoryBasic ref = new InventoryBasic("ref", false, TileGregChest.SLOTS);
            LazyChests.fillEagerly(ref, order);
            out.check(same(first, contents(ref)), "contents equal an independent draw of the same seed");
            // Reopen: unchanged.
            chest.openInventory();
            chest.closeInventory();
            out.check(same(first, contents(chest)), "contents unchanged after reopen");
            // Clear so the cleanup's block restore drops nothing.
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                chest.setInventorySlotContents(i, null);
            }
            return out.build();
        }
    }

    private static final class LazyBreak implements Scenario {

        @Override
        public String name() {
            return "worldgen.lazy_chest_break";
        }

        @Override
        public String description() {
            return "Breaking an unfilled chest fills it first, so it drops its loot; the drops are removed afterwards.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.setBlock(0, 0, 0, WorldBlockRegistration.weaponChest(), 3);
            World world = ctx.world();
            int x = ctx.originX();
            int y = ctx.originY();
            int z = ctx.originZ();
            TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof TileGregChest)) {
                out.check(false, "the chest has no GregGuns tile");
                return out.build();
            }
            ((TileGregChest) te).setPending(new LootPending(LootCategoryDefs.WEAPONCHEST_T1, 1, SEED + 1));
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - 3, y - 3, z - 3, x + 4, y + 5, z + 4);
            List<?> before = world.getEntitiesWithinAABB(EntityItem.class, box);
            world.setBlockToAir(x, y, z);
            List<?> after = world.getEntitiesWithinAABB(EntityItem.class, box);
            int dropped = 0;
            for (Object o : after) {
                if (!before.contains(o)) {
                    dropped++;
                    ((EntityItem) o).setDead();
                }
            }
            out.check(dropped > 0, "breaking the unfilled chest dropped loot");
            List<?> left = world.getEntitiesWithinAABB(EntityItem.class, box);
            int stray = 0;
            for (Object o : left) {
                if (!before.contains(o) && !((EntityItem) o).isDead) {
                    stray++;
                }
            }
            out.checkEquals("item entities left behind", 0, stray);
            return out.build();
        }
    }
}
