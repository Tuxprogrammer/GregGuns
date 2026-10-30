package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineType;
import com.gregguns.item.part.PartCatalog;

/**
 * The GregGuns pools of a structure chest, per table (1 = LV to 7 = ZPM): guns, their ammo, grenades and gun parts.
 * Pure data; {@link LootCategoryDefs} mixes these pools on top of the pack mix ({@link LootPools}).
 *
 * <p>
 * Guns are rare and ammo is common: a category gives the gun pool a small share and the ammo pool a share several
 * times larger. The ammo pool is derived from the guns, so it always fits them: each gun's own ammo item (its
 * {@link GunStats#ammoType()}), weighted by the summed weight of the guns that fire it. Every gun line also carries its
 * ammo as a companion ({@link LootEntry#companion()}), so a chest that holds a gun always holds something to fire from
 * it. Energy guns run on EU and have neither.
 *
 * <p>
 * Each table keeps the guns of the tier below it, thinned, as the Techguns tables did. Declared tiers come from the
 * guns' definitions, the ammo registry and {@link PartCatalog}, so a line can never declare less than its item's real
 * tier. Akimbo pairs, the power tools (hammers, chainsaw) and the Golden Gun are not loot.
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (Structure chest mix, Guns and ammo); structure-loot.md (GGS-11-26)
public final class Arsenal {

    /** Pool names, also the {@link LootEntry#pool()} of each line. */
    public static final String GUN = "gun";
    /** The ammo pool. */
    public static final String AMMO = "ammo";
    /** The grenade pool. */
    public static final String GRENADE = "grenade";
    /** The gun parts pool. */
    public static final String PART = "part";

    /** The highest table. */
    public static final int TABLES = 7;

    private static final String G = "gregguns:";

    /** The suffix that makes {@link LootCategories#stack} fill a GregGuns tank. */
    public static final String FULL = "~full";

    /** Guns per table, name and weight within the gun pool: the table's own tier first, then the tier below. */
    private static final String[][] GUNS = {
        // 1 LV
        { "revolver:3", "sawed_off:3", "pistol_9mm:3", "bolt_action_rifle:2", "handcannon:2" },
        // 2 MV
        { "thompson_smg:3", "mac10:2", "ak47:2", "m4:2", "combat_shotgun:2", "revolver:1", "sawed_off:1",
            "pistol_9mm:1", "bolt_action_rifle:1" },
        // 3 HV
        { "flamethrower:2", "rocket_launcher:2", "grenade_launcher:2", "aug:2", "scar:2", "vector:2", "lmg:1", "as50:1",
            "infiltrator:1", "thompson_smg:1", "mac10:1", "ak47:1", "m4:1", "combat_shotgun:1" },
        // 4 EV
        { "pdw:2", "laser_rifle:2", "pulse_rifle:2", "minigun:1", "tesla_rifle:1", "sonic_rifle:1", "bio_gun:1",
            "aug:1", "scar:1", "lmg:1", "rocket_launcher:1", "flamethrower:1" },
        // 5 IV
        { "blaster_rifle:2", "deatomizer_pistol:1", "scatter_laser_rifle:1", "golden_revolver:1", "pdw:1",
            "laser_rifle:1", "pulse_rifle:1", "minigun:1", "tesla_rifle:1" },
        // 6 LuV
        { "nether_blaster:1", "alien_blaster:1", "grim_reaper:1", "blaster_rifle:1", "scatter_laser_rifle:1",
            "golden_revolver:1", "laser_rifle:1", "pulse_rifle:1" },
        // 7 ZPM
        { "nuclear_death_ray:1", "nuke_launcher:1", "nether_blaster:2", "alien_blaster:2", "grim_reaper:1",
            "blaster_rifle:1", "scatter_laser_rifle:1", "pulse_rifle:1" } };

    /** Stack sizes per ammo item: loot line min, max, then companion min, max. */
    private static final Map<String, int[]> COUNTS = new LinkedHashMap<>();

    static {
        COUNTS.put("ammo_stone_bullet", new int[] { 8, 16, 8, 12 });
        COUNTS.put("ammo_9mm_bullet", new int[] { 8, 24, 8, 16 });
        COUNTS.put("ammo_shotgun_shell", new int[] { 6, 16, 6, 12 });
        COUNTS.put("ammo_rifle_bullet", new int[] { 4, 12, 4, 8 });
        COUNTS.put("mag_9mm_pistol", new int[] { 1, 3, 1, 2 });
        COUNTS.put("mag_smg", new int[] { 1, 3, 1, 2 });
        COUNTS.put("mag_assault_rifle", new int[] { 1, 3, 1, 2 });
        COUNTS.put("mag_as50", new int[] { 1, 2, 1, 2 });
        COUNTS.put("mag_lmg", new int[] { 1, 2, 1, 1 });
        COUNTS.put("mag_advanced", new int[] { 1, 3, 1, 2 });
        COUNTS.put("mag_minigun_drum", new int[] { 1, 2, 1, 1 });
        COUNTS.put("ammo_rocket", new int[] { 1, 4, 1, 3 });
        COUNTS.put("ammo_40mm_grenade", new int[] { 2, 6, 2, 4 });
        COUNTS.put("tank_fuel", new int[] { 1, 2, 1, 1 });
        COUNTS.put("tank_bio", new int[] { 1, 2, 1, 1 });
        COUNTS.put("ammo_nether_charge", new int[] { 1, 3, 1, 2 });
        COUNTS.put("ammo_rocket_nuke", new int[] { 1, 1, 1, 1 });
    }

    /** Gun parts that drop as loot (the pickup parts, not the high-end assemblies). */
    private static final String[] PARTS = { "part_barrel_stone", "part_stock_wooden", "part_mechanical_1",
        "part_heavy_cloth", "part_barrel_iron", "part_barrel_hardened", "part_mechanical_2", "part_receiver_iron",
        "part_pump_mechanism", "part_treated_leather", "part_stock_plastic", "part_receiver_steel",
        "part_gas_mask_filter", "part_receiver_hardened", "part_barrel_carbon", "part_stock_carbon",
        "part_receiver_carbon", "part_mechanical_3", "part_laser_focus", "part_power_plating" };

    private Arsenal() {}

    /**
     * @param table 1 (LV) to {@link #TABLES} (ZPM)
     * @return the table's ceiling
     */
    public static Tier ceiling(int table) {
        return Tier.values()[clamp(table)];
    }

    /**
     * @param table 1 to 7
     * @return the gun pool: each gun with its definition's tier and its ammo as companion
     */
    public static LootPools.Pool guns(int table) {
        LootPools.PoolBuilder b = new LootPools.PoolBuilder(GUN);
        for (String spec : GUNS[clamp(table) - 1]) {
            String name = spec.substring(0, spec.indexOf(':'));
            int weight = Integer.parseInt(spec.substring(spec.indexOf(':') + 1));
            GunDefinition def = definition(name);
            String ammo = ammoItem(def);
            LootDrop companion = null;
            if (ammo != null) {
                int[] c = counts(ammo);
                companion = LootDrop.range(G + ammoRef(ammo), c[2], c[3]);
            }
            b.add(G + name, 1, 1, weight, def.tier(), companion);
        }
        return b.build();
    }

    /**
     * @param table 1 to 7
     * @return the ammo pool: one line per ammo item the table's guns fire, weighted by those guns' summed weight
     */
    public static LootPools.Pool ammo(int table) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (String spec : GUNS[clamp(table) - 1]) {
            String name = spec.substring(0, spec.indexOf(':'));
            int weight = Integer.parseInt(spec.substring(spec.indexOf(':') + 1));
            String ammo = ammoItem(definition(name));
            if (ammo != null) {
                weights.merge(ammo, weight, Integer::sum);
            }
        }
        LootPools.PoolBuilder b = new LootPools.PoolBuilder(AMMO);
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            int[] c = counts(e.getKey());
            b.add(G + ammoRef(e.getKey()), c[0], c[1], e.getValue(), ammoTier(e.getKey()));
        }
        return b.build();
    }

    /**
     * @param table 1 to 7
     * @return the grenade pool: Stielhandgranate from LV, frag grenade and flashbang from MV
     */
    public static LootPools.Pool grenades(int table) {
        LootPools.PoolBuilder b = new LootPools.PoolBuilder(GRENADE);
        b.add(G + "stielgranate", 2, 6, 3, Tier.LV);
        if (clamp(table) >= 2) {
            b.add(G + "frag_grenade", 2, 4, 3, Tier.MV);
            b.add(G + "flashbang", 1, 3, 1, Tier.MV);
        }
        return b.build();
    }

    /**
     * @param table 1 to 7
     * @return the parts pool: every loot part at or below the table's tier, the table's own tier weighing 3, the tier
     *         below 2, older parts 1
     */
    public static LootPools.Pool parts(int table) {
        int top = clamp(table);
        LootPools.PoolBuilder b = new LootPools.PoolBuilder(PART);
        for (String part : PARTS) {
            Tier t = PartCatalog.tierOf(part);
            if (t == null || t.ordinal() > top) {
                continue;
            }
            int weight = t.ordinal() == top ? 3 : t.ordinal() == top - 1 ? 2 : 1;
            b.add(G + part, 1, 3, weight, t);
        }
        return b.build();
    }

    /**
     * @param ref a line reference
     * @return the gun name of a {@code gregguns:<gun>} reference, or null when it is not a loot gun
     */
    public static @Nullable String gunName(String ref) {
        if (!ref.startsWith(G)) {
            return null;
        }
        String name = ref.substring(G.length());
        for (String[] table : GUNS) {
            for (String spec : table) {
                if (spec.startsWith(name + ":")) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * @param def a gun definition
     * @return the item name of its ammo, or null for an energy gun
     */
    public static @Nullable String ammoItem(GunDefinition def) {
        String type = def.stats()
            .ammoType();
        if (type == null || type.isEmpty() || GunStats.ENERGY_AMMO_TYPE.equals(type)) {
            return null;
        }
        return type;
    }

    /**
     * @param ammo an ammo item name
     * @return its real tier: from the magazine or round registry, else {@link PartCatalog} (tanks)
     */
    public static Tier ammoTier(String ammo) {
        for (MagazineType m : AmmoTypes.allMagazines()
            .values()) {
            if (m.loadedItemName()
                .equals(ammo)) {
                return m.tier();
            }
        }
        for (AmmoType a : AmmoTypes.allAmmo()
            .values()) {
            if (a.roundItemName()
                .equals(ammo)) {
                return a.tier();
            }
        }
        Tier t = PartCatalog.tierOf(ammo);
        if (t == null) {
            throw new IllegalStateException("no tier for ammo item " + ammo);
        }
        return t;
    }

    /** @return the bare names of every gun some table holds, in table order */
    public static List<String> allGuns() {
        List<String> out = new ArrayList<>();
        for (String[] table : GUNS) {
            for (String spec : table) {
                String name = spec.substring(0, spec.indexOf(':'));
                if (!out.contains(name)) {
                    out.add(name);
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static String ammoRef(String ammo) {
        return ammo.startsWith("tank_") ? ammo + FULL : ammo;
    }

    private static int[] counts(String ammo) {
        int[] c = COUNTS.get(ammo);
        if (c == null) {
            throw new IllegalStateException("no loot counts for ammo item " + ammo);
        }
        return c;
    }

    private static GunDefinition definition(String name) {
        GunDefinition def = GunDefinitions.byName(name);
        if (def == null) {
            throw new IllegalStateException("no gun definition " + name);
        }
        return def;
    }

    private static int clamp(int table) {
        return Math.max(1, Math.min(TABLES, table));
    }
}
