package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.worldgen.loot.Arsenal;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPools;

/**
 * The arms a silo complex issues (D316): a standard kit, not only its top tier. The guns are the union of the
 * {@link Arsenal} gun tables from two below the complex's top table up to it (never below MV), each table's weight
 * halving per step down; ammo follows those guns; grenades are the top table's. Sidearms (guard posts, foot lockers)
 * are the pistols, revolvers, sawed-offs and SMGs of that union. Pure.
 *
 * <p>
 * Pool names stay {@link Arsenal#GUN} and {@link Arsenal#AMMO}, so a gun keeps its base weight at every richness and
 * brings its ammo along ({@link com.gregguns.worldgen.loot.LootDraw#weight(LootEntry, com.gregguns.api.Tier, int)}).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Silo arms); D316
public final class SiloArms {

    /** One-hand guns and SMGs: what a guard or a sleeping soldier keeps at hand. */
    static final Set<String> SIDEARMS = new HashSet<>(
        Arrays.asList(
            "revolver",
            "sawed_off",
            "pistol_9mm",
            "thompson_smg",
            "mac10",
            "vector",
            "pdw",
            "deatomizer_pistol",
            "golden_revolver"));

    private SiloArms() {}

    /**
     * @param ceiling the complex's ceiling
     * @return the top gun table the ceiling draws from (1 to 7)
     */
    static int top(int ceiling) {
        return SiloCeiling.tableTier(ceiling);
    }

    /** @return the lowest table of the issue union */
    static int low(int ceiling) {
        return Math.max(2, top(ceiling) - 2);
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the issue guns, each bringing its ammo
     */
    public static LootPools.Pool guns(int ceiling) {
        return union(Arsenal.GUN, ceiling, false, false);
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the ammo of the issue guns
     */
    public static LootPools.Pool ammo(int ceiling) {
        return union(Arsenal.AMMO, ceiling, true, false);
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the issue sidearms, each bringing its ammo
     */
    public static LootPools.Pool sidearms(int ceiling) {
        return union(Arsenal.GUN, ceiling, false, true);
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the ammo of the issue sidearms
     */
    public static LootPools.Pool sidearmAmmo(int ceiling) {
        return union(Arsenal.AMMO, ceiling, true, true);
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the top table's grenades
     */
    public static LootPools.Pool grenades(int ceiling) {
        return Arsenal.grenades(top(ceiling));
    }

    /**
     * @param ceiling the complex's ceiling
     * @return the gun parts up to the top table
     */
    public static LootPools.Pool parts(int ceiling) {
        return Arsenal.parts(top(ceiling));
    }

    private static LootPools.Pool union(String name, int ceiling, boolean ammo, boolean sidearmsOnly) {
        int top = top(ceiling);
        int low = Math.min(low(ceiling), top);
        Map<String, LootEntry> merged = new LinkedHashMap<>();
        for (int t = low; t <= top; t++) {
            int scale = 1 << (t - low); // each table weighs twice the one below it
            Set<String> sidearmAmmo = sidearmsOnly && ammo ? sidearmAmmoRefs(t) : null;
            List<LootEntry> lines = (ammo ? Arsenal.ammo(t) : Arsenal.guns(t)).lines();
            for (LootEntry e : lines) {
                if (sidearmsOnly && !ammo && !SIDEARMS.contains(Arsenal.gunName(e.ref()))) {
                    continue;
                }
                if (sidearmAmmo != null && !sidearmAmmo.contains(e.ref())) {
                    continue;
                }
                LootEntry old = merged.get(e.ref());
                int w = e.weight() * scale;
                merged.put(e.ref(), old == null ? e.withWeight(w) : old.withWeight(old.weight() + w));
            }
        }
        return LootPools.Pool.of(name, new ArrayList<>(merged.values()));
    }

    /** @return the ammo references the sidearms of gun table {@code t} fire */
    private static Set<String> sidearmAmmoRefs(int t) {
        Set<String> out = new HashSet<>();
        for (LootEntry e : Arsenal.guns(t)
            .lines()) {
            if (SIDEARMS.contains(Arsenal.gunName(e.ref())) && e.companion() != null) {
                out.add(
                    e.companion()
                        .ref());
            }
        }
        return out;
    }
}
