package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineType;

/**
 * The armory's display wall: which gun hangs at a display cell and what the chest below it holds. The gun is a hash
 * of the complex and the display's own cell ({@link SiloFrames}), so the frame and its chest, built in different
 * windows, always agree. The chest holds the ammo of that gun: its loaded magazines and loose rounds, or the one item
 * it burns (a rocket, a fuel tank). Pure: registry names in, registry names out, no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Armory display walls)
public final class ArmoryDisplay {

    /** The Y of the frame above a display chest at layer 1: the layer of the chest plus one. */
    public static final int FRAME_Y = 2;

    /** Whether a loot reference names a gun (a weapon table also holds ammo and grenades). */
    public static final Predicate<String> IS_GUN = ref -> GunDefinitions.byName(bare(ref)) != null;

    private ArmoryDisplay() {}

    /** One line of a display chest: an item registry name (no namespace) and its stack range. */
    public static final class Stock {

        private final String item;
        private final int min;
        private final int max;

        Stock(String item, int min, int max) {
            this.item = item;
            this.min = min;
            this.max = max;
        }

        /** @return the GregGuns item registry name */
        public String item() {
            return item;
        }

        /** @return smallest stack */
        public int min() {
            return min;
        }

        /** @return largest stack, before the item's own stack limit */
        public int max() {
            return max;
        }
    }

    /**
     * @param ceiling the complex's ceiling
     * @param seed    the complex's seed, as {@code worldSeed ^ (cellX * 31 + cellZ)}
     * @param x       the display's complex-local X (chest and frame share it)
     * @param z       its Z
     * @return the gun's loot reference, or null when no weapon table holds a gun
     */
    public static String gunAt(int ceiling, long seed, int x, int z) {
        return SiloFrames.gunFor(ceiling, seed, x, FRAME_Y, z, IS_GUN);
    }

    /**
     * @param gun a gun's loot reference, with or without the {@code gregguns:} prefix
     * @return what its display chest holds, each line a stack roll; empty for an energy gun or an unknown name
     */
    public static List<Stock> stockFor(String gun) {
        List<Stock> out = new ArrayList<>();
        GunDefinition def = gun == null ? null : GunDefinitions.byName(bare(gun));
        if (def == null || def.stats()
            .isEnergy()) {
            return out;
        }
        String ammo = def.stats()
            .ammoType();
        MagazineType mag = magazineByLoaded(ammo);
        if (mag != null) {
            out.add(new Stock(mag.loadedItemName(), 4, 8));
            out.add(
                new Stock(
                    mag.ammo()
                        .roundItemName(),
                    24,
                    64));
            return out;
        }
        AmmoType round = roundByName(ammo);
        if (round != null) {
            out.add(new Stock(round.roundItemName(), 32, 64));
            for (MagazineType m : AmmoTypes.allMagazines()
                .values()) {
                if (m.ammo() == round) {
                    out.add(new Stock(m.loadedItemName(), 2, 4));
                    break;
                }
            }
            return out;
        }
        out.add(new Stock(ammo, 8, 24)); // a tank, a charge or a grenade belt: the one item the gun burns
        return out;
    }

    /** @return every item registry name a gun's display chest may hold */
    public static List<String> itemsFor(String gun) {
        List<String> out = new ArrayList<>();
        for (Stock s : stockFor(gun)) {
            out.add(s.item());
        }
        return out;
    }

    private static String bare(String ref) {
        int colon = ref.indexOf(':');
        return colon < 0 ? ref : ref.substring(colon + 1);
    }

    private static MagazineType magazineByLoaded(String name) {
        for (MagazineType m : AmmoTypes.allMagazines()
            .values()) {
            if (m.loadedItemName()
                .equals(name)) {
                return m;
            }
        }
        return null;
    }

    private static AmmoType roundByName(String name) {
        for (AmmoType t : AmmoTypes.allAmmo()
            .values()) {
            if (t.roundItemName()
                .equals(name)) {
                return t;
            }
        }
        return null;
    }
}
