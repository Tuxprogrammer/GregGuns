package com.gregguns.worldgen.loot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * The seven GregGuns structure categories with their ceilings and contents. Pure data; {@link LootCategories}
 * registers them with Forge in postInit.
 *
 * <p>
 * Counts and weights are Techguns' tables (loot.md) with these changes, all by spec (structure-loot.md, pack-loot.md):
 * a gun or magazine above a table's ceiling moves to a higher table (the Thompson and SMG magazine leave tier 1; the
 * flamethrower, rocket launcher and rockets are only in tier 3); raw metals become GT parts (GGS-06-03); the glider,
 * oxygen gear and the gas-mask line are not in the barracks table (HV in 0001, or not built yet). The tier 3 tables
 * are HV: no default structure uses them (GGS-11-22), they exist for an admin who raises a ceiling.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21, GGS-11-22);
// docs/gregguns-spec/06-items/pack-loot.md
public final class LootCategoryDefs {

    private static final String G = "gregguns:";

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
    /** Military base barracks (MV gear). */
    public static final String BARRACKS = "gregguns:barracks";

    private static final List<LootCategory> ALL = Collections.unmodifiableList(
        Arrays.asList(weaponT1(), weaponT2(), weaponT3(), reinforcedT1(), reinforcedT2(), reinforcedT3(), barracks()));

    private LootCategoryDefs() {}

    /** @return the seven categories in a stable order */
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

    private static LootCategory weaponT1() {
        return LootCategory.builder(WEAPONCHEST_T1, Tier.LV, 2, 8)
            .add(G + "handcannon", 1, 1, 2, Tier.STEAM)
            .add(G + "ammo_stone_bullet", 8, 16, 3, Tier.STEAM)
            .add(G + "ammo_9mm_bullet", 4, 8, 3, Tier.LV)
            .add(G + "ammo_shotgun_shell", 4, 32, 3, Tier.LV)
            .add(G + "revolver", 1, 1, 2, Tier.LV)
            .add(G + "sawed_off", 1, 1, 2, Tier.LV)
            .build();
    }

    private static LootCategory weaponT2() {
        return LootCategory.builder(WEAPONCHEST_T2, Tier.MV, 4, 8)
            .add(G + "ammo_9mm_bullet", 4, 12, 4, Tier.LV)
            .add(G + "ammo_shotgun_shell", 4, 32, 4, Tier.LV)
            .add(G + "revolver", 1, 1, 1, Tier.LV)
            .add(G + "sawed_off", 1, 1, 1, Tier.LV)
            .add(G + "thompson_smg", 1, 1, 1, Tier.MV)
            .add(G + "mag_smg", 3, 6, 3, Tier.MV)
            .add(G + "ammo_rifle_bullet", 4, 12, 4, Tier.LV)
            .add(G + "mag_assault_rifle", 2, 8, 3, Tier.MV)
            .add(G + "bolt_action_rifle", 1, 1, 1, Tier.LV)
            .add(G + "mac10", 1, 1, 1, Tier.MV)
            .add(G + "ak47", 1, 1, 1, Tier.MV)
            .add(G + "m4", 1, 1, 1, Tier.MV)
            .add(G + "pistol_9mm", 1, 1, 1, Tier.LV)
            .add(G + "mag_9mm_pistol", 2, 5, 2, Tier.LV)
            .add(G + "stielgranate", 3, 8, 2, Tier.LV)
            .build();
    }

    private static LootCategory weaponT3() {
        return LootCategory.builder(WEAPONCHEST_T3, Tier.HV, 5, 10)
            .add(G + "ammo_9mm_bullet", 4, 12, 4, Tier.LV)
            .add(G + "ammo_shotgun_shell", 4, 32, 4, Tier.LV)
            .add(G + "ammo_rifle_bullet", 4, 12, 4, Tier.LV)
            .add(G + "mag_assault_rifle", 4, 8, 4, Tier.MV)
            .add(G + "mag_smg", 4, 8, 4, Tier.MV)
            .add(G + "combat_shotgun", 1, 1, 1, Tier.MV)
            .add(G + "bolt_action_rifle", 1, 1, 1, Tier.LV)
            .add(G + "mac10", 1, 1, 1, Tier.MV)
            .add(G + "ak47", 1, 1, 1, Tier.MV)
            .add(G + "m4", 1, 1, 1, Tier.MV)
            .add(G + "pistol_9mm", 1, 1, 1, Tier.LV)
            .add(G + "mag_9mm_pistol", 2, 5, 4, Tier.LV)
            .add(G + "tank_fuel", 3, 8, 3, Tier.HV)
            .add(G + "flamethrower", 1, 1, 1, Tier.HV)
            .add(G + "rocket_launcher", 1, 1, 1, Tier.HV)
            .add(G + "ammo_rocket", 4, 8, 2, Tier.HV)
            .add(G + "frag_grenade", 3, 8, 2, Tier.MV)
            .add(G + "stielgranate", 4, 12, 2, Tier.LV)
            .build();
    }

    private static LootCategory reinforcedT1() {
        return LootCategory.builder(REINFORCEDCHEST_T1, Tier.LV, 3, 6)
            .add("minecraft:coal", 4, 16, 3, Tier.STEAM)
            .add("gt:ingot.Iron", 4, 16, 3, Tier.STEAM)
            .add("minecraft:stone", 16, 32, 3, Tier.STEAM)
            .add("minecraft:log", 8, 16, 3, Tier.STEAM)
            .add("minecraft:gunpowder", 4, 8, 3, Tier.STEAM)
            .add("gt:ingot.Tin", 8, 32, 3, Tier.STEAM)
            .add("gt:ingot.Copper", 8, 32, 3, Tier.STEAM)
            .add("gt:ingot.Lead", 6, 24, 3, Tier.STEAM)
            .build();
    }

    private static LootCategory reinforcedT2() {
        return LootCategory.builder(REINFORCEDCHEST_T2, Tier.MV, 4, 8)
            .add("minecraft:coal", 4, 16, 3, Tier.STEAM)
            .add("gt:ingot.Iron", 3, 12, 3, Tier.STEAM)
            .add("minecraft:stone", 16, 32, 3, Tier.STEAM)
            .add("minecraft:log", 6, 12, 3, Tier.STEAM)
            .add("minecraft:gunpowder", 4, 8, 3, Tier.STEAM)
            .add("gt:ingot.Gold", 2, 6, 3, Tier.STEAM)
            .add("gt:plate.Rubber", 2, 8, 3, Tier.LV)
            .add("gt:ingot.Tin", 4, 16, 3, Tier.STEAM)
            .add("gt:ingot.Copper", 4, 16, 3, Tier.STEAM)
            .add("gt:ingot.Lead", 3, 12, 3, Tier.STEAM)
            .add("gt:ingot.Silver", 2, 8, 3, Tier.LV)
            .add("gt:ingot.Steel", 2, 8, 3, Tier.LV)
            .build();
    }

    private static LootCategory reinforcedT3() {
        return LootCategory.builder(REINFORCEDCHEST_T3, Tier.HV, 5, 9)
            .add("minecraft:coal", 8, 16, 3, Tier.STEAM)
            .add("gt:ingot.Iron", 8, 16, 3, Tier.STEAM)
            .add("minecraft:stone", 24, 32, 3, Tier.STEAM)
            .add("minecraft:log", 8, 16, 3, Tier.STEAM)
            .add("minecraft:gunpowder", 6, 12, 3, Tier.STEAM)
            .add("gt:ingot.Gold", 6, 18, 3, Tier.STEAM)
            .add("gt:plate.Rubber", 3, 10, 3, Tier.LV)
            .add("gt:plate.Plastic", 2, 8, 3, Tier.MV)
            .add("gt:ingot.Tin", 8, 16, 3, Tier.STEAM)
            .add("gt:ingot.Copper", 8, 16, 3, Tier.STEAM)
            .add("gt:ingot.Lead", 8, 16, 3, Tier.STEAM)
            .add("gt:ingot.Silver", 4, 16, 3, Tier.LV)
            .add("gt:ingot.Steel", 4, 16, 3, Tier.LV)
            .build();
    }

    private static LootCategory barracks() {
        LootCategory.Builder b = LootCategory.builder(BARRACKS, Tier.MV, 1, 4);
        for (String slot : new String[] { "helmet", "chestplate", "leggings", "boots" }) {
            b.add(G + "t1_combat_" + slot, 1, 1, 2, Tier.LV);
            b.add(G + "t2_combat_" + slot, 1, 1, 1, Tier.MV);
        }
        return b.build();
    }
}
