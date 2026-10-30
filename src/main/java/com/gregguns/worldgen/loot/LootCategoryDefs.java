package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;

/**
 * The GregGuns structure categories with their ceilings and contents. Pure data; {@link LootCategories} registers
 * them with Forge in postInit.
 *
 * <p>
 * Every chest is a mix of the pack-side pools of {@link LootPools}, modeled on the top floors of Roguelike Dungeons
 * (food, everyday materials, ores and ingots, tools, potions, melee weapons, armor, enchanting), with the GregGuns
 * pools of {@link Arsenal} on top (ammo, grenades, parts, guns). The chest kind sets the shares:
 * <ul>
 * <li>{@code supplychest_t<n>}: the vanilla chests of a structure, a general mix with a little GregGuns gear;</li>
 * <li>{@code weaponchest_t<n>}: weapon-leaning, about half GregGuns (mostly ammo) and the rest melee, armor, potions
 * and food;</li>
 * <li>{@code reinforcedchest_t<n>}: GT materials and parts first;</li>
 * <li>{@code barracks}: armor, food and ammo.</li>
 * </ul>
 * Guns are rare and ammo common in every kind (the {@link #GUN_SHARE} and {@link #AMMO_SHARE} pairs), and every gun
 * brings its ammo along as a companion. The table number is the ceiling as a GT voltage index (1 = LV to 7 = ZPM);
 * each pool drops what is above it, so one set of shares serves every tier.
 *
 * <p>
 * The tier 4 to 7 tables (EV to ZPM) exist for Ross128b and Ross128ba, where a structure's rolled loot ceiling can
 * reach them (design record 0019); an Overworld structure never draws from them (the config caps a configured
 * ceiling at 3). Rolled UV and UHV ceilings draw from the ZPM tables. The tier 3 tables are HV: no default structure
 * uses them (GGS-11-22), they exist for an admin who raises a ceiling.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21, GGS-11-22, GGS-11-26);
// docs/gregguns-spec/06-items/pack-loot.md (GregGuns loot categories, Structure chest mix)
public final class LootCategoryDefs {

    /** Weapon chest, tier 1 (LV). */
    public static final String WEAPONCHEST_T1 = "gregguns:weaponchest_t1";
    /** Weapon chest, tier 2 (MV). */
    public static final String WEAPONCHEST_T2 = "gregguns:weaponchest_t2";
    /** Weapon chest, tier 3 (HV). */
    public static final String WEAPONCHEST_T3 = "gregguns:weaponchest_t3";
    /** Reinforced chest, tier 1 (LV parts). */
    public static final String REINFORCEDCHEST_T1 = "gregguns:reinforcedchest_t1";
    /** Reinforced chest, tier 2 (MV parts). */
    public static final String REINFORCEDCHEST_T2 = "gregguns:reinforcedchest_t2";
    /** Reinforced chest, tier 3 (HV parts). */
    public static final String REINFORCEDCHEST_T3 = "gregguns:reinforcedchest_t3";
    /** Supply chest (a structure's vanilla chests), tier 1 (LV). */
    public static final String SUPPLYCHEST_T1 = "gregguns:supplychest_t1";
    /** Supply chest, tier 2 (MV). */
    public static final String SUPPLYCHEST_T2 = "gregguns:supplychest_t2";
    /** Supply chest, tier 3 (HV). */
    public static final String SUPPLYCHEST_T3 = "gregguns:supplychest_t3";
    /** Military base barracks (MV gear). */
    public static final String BARRACKS = "gregguns:barracks";

    /** Share points of the gun pool, by chest kind (supply, weapon, reinforced, barracks). */
    public static final int[] GUN_SHARE = { 2, 5, 0, 0 };
    /** Share points of the ammo pool, by chest kind (supply, weapon, reinforced, barracks). */
    public static final int[] AMMO_SHARE = { 10, 30, 8, 15 };

    private static final String W = "gregguns:weaponchest_t";
    private static final String R = "gregguns:reinforcedchest_t";
    private static final String S = "gregguns:supplychest_t";

    private static final Map<String, Map<String, Integer>> SHARES = new LinkedHashMap<>();
    private static final List<LootCategory> ALL;

    static {
        List<LootCategory> all = new ArrayList<>();
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            all.add(weapon(t));
        }
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            all.add(reinforced(t));
        }
        for (int t = 1; t <= Arsenal.TABLES; t++) {
            all.add(supply(t));
        }
        all.add(barracks());
        ALL = Collections.unmodifiableList(all);
    }

    private LootCategoryDefs() {}

    /** @return the 22 categories in a stable order: weapon, reinforced and supply tables 1 to 7, then barracks */
    public static List<LootCategory> all() {
        return ALL;
    }

    /**
     * @param name a category name
     * @return the category, or null
     */
    public static LootCategory get(String name) {
        for (LootCategory c : ALL) {
            if (c.name()
                .equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param name a category name
     * @return the share points of each pool the category mixes, in order, or an empty map
     */
    public static Map<String, Integer> shares(String name) {
        Map<String, Integer> s = SHARES.get(name);
        return s == null ? Collections.<String, Integer>emptyMap() : Collections.unmodifiableMap(s);
    }

    /**
     * The weapon chest name of a table.
     *
     * @param table 1 to 7
     * @return {@code gregguns:weaponchest_t<table>}
     */
    public static String weaponChest(int table) {
        return W + table;
    }

    /**
     * @param table 1 to 7
     * @return {@code gregguns:reinforcedchest_t<table>}
     */
    public static String reinforcedChest(int table) {
        return R + table;
    }

    /**
     * @param table 1 to 7
     * @return {@code gregguns:supplychest_t<table>}
     */
    public static String supplyChest(int table) {
        return S + table;
    }

    /**
     * The GregGuns-only lines of a table, at small weights, for the ICBM silo tables and armory frames: guns at their
     * pool weight, the guns' ammo at four times the summed weight of the guns that fire it, grenades at twice their
     * pool weight. No pack items and no companions.
     *
     * @param table 1 to 7
     * @return the lines
     */
    public static List<LootEntry> arms(int table) {
        List<LootEntry> out = new ArrayList<>();
        for (LootEntry e : Arsenal.guns(table)
            .lines()) {
            out.add(e.withoutCompanion());
        }
        for (LootEntry e : Arsenal.ammo(table)
            .lines()) {
            out.add(e.withWeight(e.weight() * 4));
        }
        for (LootEntry e : Arsenal.grenades(table)
            .lines()) {
            out.add(e.withWeight(e.weight() * 2));
        }
        return out;
    }

    private static LootCategory weapon(int t) {
        LootMix m = LootMix.of(W + t, Arsenal.ceiling(t), t == 1 ? 4 : t == 2 ? 4 : 5, t == 1 ? 8 : t == 2 ? 9 : 10)
            .pool(AMMO_SHARE[1], Arsenal.ammo(t))
            .pool(GUN_SHARE[1], Arsenal.guns(t))
            .pool(8, Arsenal.grenades(t))
            .pool(5, Arsenal.parts(t))
            .pool(12, LootPools.melee())
            .pool(8, LootPools.armor())
            .pool(8, LootPools.potions())
            .pool(12, LootPools.food())
            .pool(6, LootPools.junk())
            .pool(6, LootPools.ore());
        return record(m);
    }

    private static LootCategory supply(int t) {
        LootMix m = LootMix.of(S + t, Arsenal.ceiling(t), t == 1 ? 4 : 5, t == 1 ? 8 : t == 2 ? 9 : 10)
            .pool(20, LootPools.food())
            .pool(16, LootPools.junk())
            .pool(12, LootPools.ore())
            .pool(9, LootPools.tools())
            .pool(8, LootPools.potions())
            .pool(6, LootPools.melee())
            .pool(6, LootPools.armor())
            .pool(5, LootPools.enchanting())
            .pool(AMMO_SHARE[0], Arsenal.ammo(t))
            .pool(3, Arsenal.grenades(t))
            .pool(3, Arsenal.parts(t))
            .pool(GUN_SHARE[0], Arsenal.guns(t));
        return record(m);
    }

    private static LootCategory reinforced(int t) {
        LootMix m = LootMix.of(R + t, Arsenal.ceiling(t), t == 1 ? 4 : t == 2 ? 4 : 5, t == 1 ? 7 : t == 2 ? 8 : 9)
            .pool(45, LootPools.materials())
            .pool(10, LootPools.ore())
            .pool(10, LootPools.tools())
            .pool(8, LootPools.junk())
            .pool(7, LootPools.food())
            .pool(10, Arsenal.parts(t))
            .pool(AMMO_SHARE[2], Arsenal.ammo(t))
            .pool(2, Arsenal.grenades(t));
        return record(m);
    }

    private static LootCategory barracks() {
        LootMix m = LootMix.of(BARRACKS, Tier.MV, 2, 5)
            .pool(25, LootPools.combatArmor())
            .pool(10, LootPools.armor())
            .pool(25, LootPools.food())
            .pool(10, LootPools.potions())
            .pool(AMMO_SHARE[3], Arsenal.ammo(2))
            .pool(10, LootPools.junk())
            .pool(5, LootPools.melee());
        return record(m);
    }

    private static LootCategory record(LootMix m) {
        LootCategory c = m.build();
        SHARES.put(c.name(), m.shares());
        return c;
    }
}
