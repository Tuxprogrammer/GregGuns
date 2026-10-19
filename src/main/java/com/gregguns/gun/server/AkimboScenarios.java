package com.gregguns.gun.server;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.AkimboNbt;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * The {@code gun.<akimbo>.*} scenarios of an akimbo pair (Akimbo Pistols, Akimbo Revolvers): alternating semi-auto fire
 * that spends one round per pull, the reload that takes two items and returns two empty magazines (or none), the
 * forced-reload refund at the single gun's rate, and the crafting-table combine and split through the real crafting
 * manager and crafted event (ammo summed and split, each gun's camo kept, the extra gun given once). Dev environment
 * only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Akimbo Pistols, Akimbo Revolvers); gun-lifecycle.md
// section 8; docs/gregguns-spec/02-guns/recipes.md (Akimbo combine and split); GUN-26
public final class AkimboScenarios {

    private AkimboScenarios() {}

    /**
     * Registers one akimbo gun's scenarios.
     *
     * @param registry where to add them
     * @param gunName  the akimbo gun's registry name, for example {@code "akimbo_pistols"}
     */
    public static void register(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new Fire(def));
        registry.register(new Reload(def));
        registry.register(new ForcedReload(def));
        registry.register(new CombineSplit(def));
    }

    private static GunRig rig(ScenarioContext ctx, GunDefinition def, int loaded) {
        return new GunRig(ctx, GunItems.get(def.name()), loaded);
    }

    /** Three pulls: one round and one bullet each. */
    private static final class Fire implements Scenario {

        private final GunDefinition def;

        Fire(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "Three pulls on the " + def.name()
                + " each spend one round and launch one bullet (the hands alternate, one bullet per pull).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, stats.magazineSize());
            int gap = stats.fireRateTicks() + 1;
            int[] pulls = { 0 };
            return ScenarioResult.pending(name(), 3 * gap + 20, ticks -> {
                if (pulls[0] < 3 && ticks >= pulls[0] * gap) {
                    rig.press();
                    rig.release();
                    pulls[0]++;
                }
                rig.tick();
                if (ticks < 3 * gap + 3) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds spent by three pulls", 3, rig.shots());
                out.checkEquals("rounds left", stats.magazineSize() - 3, rig.loaded());
                out.checkEquals("bullets launched", 3, rig.bullets());
                return out.build();
            });
        }
    }

    /** An empty gun with two items: both are consumed, the clip fills, empty magazines come back. */
    private static final class Reload implements Scenario {

        private final GunDefinition def;

        Reload(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload";
        }

        @Override
        public String description() {
            return "An empty " + def.name()
                + " reloads after "
                + def.stats()
                    .reloadTicks()
                + " ticks from two ammo items: the clip fills to "
                + def.stats()
                    .magazineSize()
                + " and, for a magazine gun, two empty magazines come back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, 0);
            rig.give(stats.ammoType(), 2);
            int reloadTicks = stats.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("rounds at the end of the reload", stats.magazineSize(), loadedAt[reloadTicks]);
                out.checkEquals("ammo items left", 0, rig.count(stats.ammoType()));
                if (stats.emptyContainerType() != null) {
                    out.checkEquals("empty magazines returned", 2, rig.count(stats.emptyContainerType()));
                }
                return out.build();
            });
        }
    }

    /** A forced reload refunds whole loose rounds at the single gun's rate (GUN-13; GUN-14's factor not repeated). */
    private static final class ForcedReload implements Scenario {

        private final GunDefinition def;

        ForcedReload(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a part-loaded " + def.name()
                + " swaps both ammo items and refunds one loose round per "
                + GunItems.roundsPerLooseItem(def.stats())
                + " rounds left, floored: the single gun's rate, not double.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            int perItem = GunItems.roundsPerLooseItem(stats);
            int loaded = stats.magazineSize() - 3;
            GunRig rig = rig(ctx, def, loaded);
            rig.give(stats.ammoType(), 2);
            int reloadTicks = stats.reloadTicks();
            rig.tick();
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", stats.magazineSize(), rig.loaded());
                out.checkEquals("loose rounds refunded", loaded / perItem, rig.count(stats.looseAmmoType()));
                if (stats.emptyContainerType() != null) {
                    out.checkEquals("empty magazines returned", 2, rig.count(stats.emptyContainerType()));
                }
                return out.build();
            });
        }
    }

    /** The crafting-table combine and split with differing ammo and camo, through the real manager and event. */
    private static final class CombineSplit implements Scenario {

        private final GunDefinition def;

        CombineSplit(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".combine";
        }

        @Override
        public String description() {
            return "Two " + def.singleGun()
                + " (two thirds and a third of a clip less one, camo 0 and 2) craft into one "
                + def.name()
                + " with the rounds summed and the first camo; crafting that back gives one gun with half the rounds (floored) and camo 0 and, once, a second to the player with the remainder and camo 2.";
        }

        private InventoryCrafting grid(ItemStack... stacks) {
            InventoryCrafting grid = new InventoryCrafting(new Container() {

                @Override
                public boolean canInteractWith(EntityPlayer player) {
                    return true;
                }
            }, 3, 3);
            for (int i = 0; i < stacks.length; i++) {
                grid.setInventorySlotContents(i, stacks[i]);
            }
            return grid;
        }

        private ItemStack single(GunDefinition single, int ammo, int camo) {
            ItemStack stack = new ItemStack(GunItems.get(single.name()));
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort(GunNbt.KEY_AMMO, (short) ammo);
            tag.setByte(GunNbt.KEY_CAMO, (byte) camo);
            stack.setTagCompound(tag);
            return stack;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunDefinition singleDef = GunDefinitions.byName(def.singleGun());
            GunStats singleStats = singleDef.stats();
            int roundsA = singleStats.magazineSize() * 2 / 3;
            int roundsB = singleStats.magazineSize() / 3 - 1;
            int total = roundsA + roundsB;
            ItemStack a = single(singleDef, roundsA, 0);
            ItemStack b = single(singleDef, roundsB, 2);

            ItemStack pair = CraftingManager.getInstance()
                .findMatchingRecipe(grid(a, null, b), ctx.world());
            out.check(
                pair != null && pair.getItem() == GunItems.get(def.name()),
                "two guns should craft the akimbo gun");
            if (pair == null || pair.getItem() != GunItems.get(def.name())) {
                return out.build();
            }
            NBTTagCompound pairTag = pair.getTagCompound();
            out.checkEquals(
                "combined rounds",
                total,
                GunNbt.read(pairTag, def.stats())
                    .loaded());
            out.checkEquals(
                "combined camo (the first gun's)",
                0,
                GunNbt.read(pairTag, def.stats())
                    .camo());
            out.check(pairTag.hasKey(AkimboNbt.KEY_SECOND), "the second gun's tag should ride along");
            out.check(
                CraftingManager.getInstance()
                    .findMatchingRecipe(grid(a), ctx.world()) == null,
                "one single gun alone crafts nothing");

            InventoryCrafting splitGrid = grid(pair);
            ItemStack first = CraftingManager.getInstance()
                .findMatchingRecipe(splitGrid, ctx.world());
            out.check(first != null && first.getItem() == GunItems.get(singleDef.name()), "an akimbo gun should split");
            if (first == null || first.getItem() != GunItems.get(singleDef.name())) {
                return out.build();
            }
            out.checkEquals(
                "first gun rounds (floor half)",
                total / 2,
                GunNbt.read(first.getTagCompound(), singleStats)
                    .loaded());
            out.checkEquals(
                "first gun camo",
                0,
                GunNbt.read(first.getTagCompound(), singleStats)
                    .camo());

            EntityPlayer player = GunRig.fakePlayer(ctx, "[GregGunsAkimboTest]", 0);
            int before = countGuns(player, singleDef);
            FMLCommonHandler.instance()
                .firePlayerCraftingEvent(player, first, splitGrid);
            out.checkEquals("guns given besides the result", before + 1, countGuns(player, singleDef));
            ItemStack second = findGun(player, singleDef);
            if (second != null) {
                out.checkEquals(
                    "second gun rounds (the remainder)",
                    total - total / 2,
                    GunNbt.read(second.getTagCompound(), singleStats)
                        .loaded());
                out.checkEquals(
                    "second gun camo (kept, GUN-26)",
                    2,
                    GunNbt.read(second.getTagCompound(), singleStats)
                        .camo());
            } else {
                out.check(false, "the second gun should be in the player's inventory");
            }
            return out.build();
        }

        private static int countGuns(EntityPlayer player, GunDefinition single) {
            int n = 0;
            for (ItemStack s : player.inventory.mainInventory) {
                if (s != null && s.getItem() == GunItems.get(single.name())) {
                    n++;
                }
            }
            return n;
        }

        private static ItemStack findGun(EntityPlayer player, GunDefinition single) {
            for (ItemStack s : player.inventory.mainInventory) {
                if (s != null && s.getItem() == GunItems.get(single.name())) {
                    return s;
                }
            }
            return null;
        }
    }
}
