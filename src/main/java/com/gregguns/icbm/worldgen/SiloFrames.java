package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootCategoryDefs;
import com.gregguns.worldgen.loot.LootEntry;

/**
 * Which gun hangs in which armory frame (D128, D38): drawn from the GregGuns weapon tables at or below the complex's
 * ceiling, so a frame never shows a gun above it. Pure apart from the caller's gun test; every draw is a hash of the
 * complex's seed and the frame's own cell, never of call order.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Contents); D38, D128
public final class SiloFrames {

    private static final long S_FRAME = 0xF2A3E;

    private SiloFrames() {}

    /**
     * @param ceiling the complex's ceiling (GT voltage index)
     * @param isGun   whether a loot reference is a gun (a weapon table also holds ammo, magazines and grenades)
     * @return the gun references of table {@code tier} (1 to {@link SiloCeiling#tableTier}), weighted lines kept
     *         repeated by weight so a draw is one index
     */
    public static List<String> pool(int ceiling, int tier, Predicate<String> isGun) {
        List<String> out = new ArrayList<>();
        if (tier < 1 || tier > SiloCeiling.tableTier(ceiling)) {
            return out;
        }
        String name = "gregguns:weaponchest_t" + tier;
        for (LootCategory c : LootCategoryDefs.all()) {
            if (!c.name()
                .equals(name)) {
                continue;
            }
            for (LootEntry e : c.entries()) {
                if (isGun.test(e.ref())) {
                    for (int i = 0; i < e.weight(); i++) {
                        out.add(e.ref());
                    }
                }
            }
        }
        return out;
    }

    /**
     * @param ceiling the complex's ceiling
     * @param seed    the complex's seed
     * @param x       the frame's complex-local X
     * @param y       its Y
     * @param z       its Z
     * @param isGun   the gun test
     * @return the loot reference of the gun in this frame, or null when no table holds a gun
     */
    public static String gunFor(int ceiling, long seed, int x, int y, int z, Predicate<String> isGun) {
        int top = SiloCeiling.tableTier(ceiling);
        long h = Mix.of(seed, S_FRAME, x * 4096L + z, y);
        // The top table most often, the two below it sometimes: an armory is mostly the best the complex can hold.
        int down = (int) Long.remainderUnsigned(h, 4);
        int tier = Math.max(1, top - (down == 3 ? 0 : down));
        for (int t = tier; t >= 1; t--) {
            List<String> pool = pool(ceiling, t, isGun);
            if (!pool.isEmpty()) {
                return pool.get((int) Long.remainderUnsigned(Mix.mix(h), pool.size()));
            }
        }
        return null;
    }
}
