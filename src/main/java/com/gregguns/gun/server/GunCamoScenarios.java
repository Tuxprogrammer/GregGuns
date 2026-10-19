package com.gregguns.gun.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.machine.repair.MTECamoBench;

/**
 * {@code gun.camo.cycle}: every gun in {@link GunDefinitions#ALL} is a {@code CamoChangeable} with the camo count its
 * definition declares, and the Camo Bench's own stepping routine ({@code MTECamoBench.cycleStack}) cycles it forward
 * and back through every skin and around, keeping the rounds loaded (GUN-01) and cycling a tag-less gun without
 * emptying it. Dev environment only, server side; it needs no bench block, because the bench's click handler calls
 * exactly that routine.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md (ICamoChangeable);
// docs/techguns-spec/02-guns/gun-lifecycle.md
// (GUN-01: cycling the camo must not change the rounds)
public final class GunCamoScenarios {

    private GunCamoScenarios() {}

    /**
     * Registers the camo scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Cycle());
    }

    private static final class Cycle implements Scenario {

        @Override
        public String name() {
            return "gun.camo.cycle";
        }

        @Override
        public String description() {
            return "Every gun cycles through its definition's camo count with the Camo Bench routine, forward, back and around, and keeps its loaded rounds; a tag-less gun stays full.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (GunDefinition def : GunDefinitions.ALL) {
                ItemGun gun = GunItems.get(def.name());
                out.check(gun != null, def.name() + " should be registered");
                if (gun == null) {
                    continue;
                }
                GunStats stats = def.stats();
                int count = def.camoCount();
                int rounds = Math.max(1, stats.magazineSize() - 1);
                ItemStack stack = withRounds(gun, rounds);
                out.checkEquals(def.name() + " camoCount", count, gun.camoCount(stack));
                out.checkEquals(def.name() + " starts on camo", 0, gun.getCamo(stack));

                for (int step = 1; step <= count; step++) {
                    boolean changed = MTECamoBench.cycleStack(stack, false);
                    out.checkEquals(def.name() + " cycle changed (step " + step + ")", count > 1, changed);
                    out.checkEquals(def.name() + " camo after step " + step, step % count, gun.getCamo(stack));
                    out.checkEquals(def.name() + " rounds after step " + step, rounds, loaded(stack, stats));
                }
                if (count > 1) {
                    MTECamoBench.cycleStack(stack, true);
                    out.checkEquals(def.name() + " camo wraps back below zero", count - 1, gun.getCamo(stack));
                    out.checkEquals(def.name() + " rounds after stepping back", rounds, loaded(stack, stats));
                }

                ItemStack bare = new ItemStack(gun);
                MTECamoBench.cycleStack(bare, false);
                out.checkEquals(def.name() + " tag-less gun stays full", stats.magazineSize(), loaded(bare, stats));
                out.checkEquals(def.name() + " tag-less gun camo", count > 1 ? 1 : 0, gun.getCamo(bare));

                gun.setCamo(stack, count + 5);
                out.checkEquals(def.name() + " out-of-range camo stores 0", 0, gun.getCamo(stack));
                out.checkEquals(def.name() + " rounds after an out-of-range camo", rounds, loaded(stack, stats));
            }
            return out.build();
        }

        private static ItemStack withRounds(ItemGun gun, int rounds) {
            ItemStack stack = new ItemStack(gun);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort(GunNbt.KEY_AMMO, (short) rounds);
            tag.setByte(GunNbt.KEY_CAMO, (byte) 0);
            stack.setTagCompound(tag);
            return stack;
        }

        private static int loaded(ItemStack stack, GunStats stats) {
            return GunNbt.read(stack.getTagCompound(), stats)
                .loaded();
        }
    }
}
