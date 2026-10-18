package com.gregguns.armor.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * Every armor set GregGuns defines, as data. To add a set (T2, T3, powered), add a {@link SetDef} here and list it in
 * {@link #ALL}; items, lang keys (see the dev self-check), tooltips, bonuses, hazard flags and layer textures follow
 * from the data. Plain Java, both sides.
 *
 * <p>
 * M3 wave A: the three T1 sets. Stats are the Techguns values ({@code armor-catalog.md} 5.1 to 5.3). Deviations:
 * repair metal is the set's GT plate (Steel for Combat, Cast Iron, GT's wrought iron, for Miner) instead of iron; the
 * Miner's Pants cost
 * 3 parts, not Techguns' 2 (ARM-33 looks like a typo).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 2 and 5.1 to 5.3; docs/design/0001-tier-map.md
public final class ArmorCatalog {

    /** Registry name of the cloth part all T1 sets are repaired with. */
    public static final String HEAVY_CLOTH = "part_heavy_cloth";

    private static final ArmorMaterialDef T1_COMBAT_MATERIAL = ArmorMaterialDef.uniform("t1_combat", 60, 15.0);
    private static final ArmorMaterialDef T1_SCOUT_MATERIAL = ArmorMaterialDef.uniform("t1_scout", 60, 13.0);
    private static final ArmorMaterialDef T1_MINER_MATERIAL = ArmorMaterialDef.uniform("t1_miner", 60, 13.0);

    /** T1 Combat, "Soldier's ...": LV, no camo, knockback resistance only. */
    public static final SetDef T1_COMBAT = new SetDef(
        "t1_combat",
        Tier.LV,
        T1_COMBAT_MATERIAL,
        Collections.singletonList("t1_combat"),
        BonusMap.EMPTY,
        PieceDef.builder(ArmorSlot.HELMET, "Soldier's Helmet")
            .bonuses(BonusMap.of(ArmorBonus.KNOCKBACK_RESISTANCE, 0.05))
            .repair(new RepairSpec("plate", "Steel", HEAVY_CLOTH, 0.5, 2))
            .build(),
        PieceDef.builder(ArmorSlot.CHESTPLATE, "Soldier's Vest")
            .bonuses(BonusMap.of(ArmorBonus.KNOCKBACK_RESISTANCE, 0.20))
            .repair(new RepairSpec("plate", "Steel", HEAVY_CLOTH, 0.5, 4))
            .build(),
        PieceDef.builder(ArmorSlot.LEGGINGS, "Soldier's Pants")
            .bonuses(BonusMap.of(ArmorBonus.KNOCKBACK_RESISTANCE, 0.10))
            .repair(new RepairSpec("plate", "Steel", HEAVY_CLOTH, 1.0 / 3.0, 3))
            .build(),
        PieceDef.builder(ArmorSlot.BOOTS, "Soldier's Boots")
            .bonuses(BonusMap.of(ArmorBonus.KNOCKBACK_RESISTANCE, 0.05))
            .repair(new RepairSpec("plate", "Steel", HEAVY_CLOTH, 0.5, 2))
            .build());

    /** T1 Scout, "Bandit ...": Steam/ULV, fast and light, four camos, cloth-only repair. */
    public static final SetDef T1_SCOUT = new SetDef(
        "t1_scout",
        Tier.STEAM,
        T1_SCOUT_MATERIAL,
        Arrays.asList("t1_scout", "t1_scout_forest", "t1_scout_snow", "t1_scout_black"),
        BonusMap.EMPTY,
        PieceDef.builder(ArmorSlot.HELMET, "Bandit Mask")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.125, ArmorBonus.JUMP, 0.02))
            .repair(new RepairSpec(null, null, HEAVY_CLOTH, 0.0, 2))
            .build(),
        PieceDef.builder(ArmorSlot.CHESTPLATE, "Bandit Vest")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.125, ArmorBonus.JUMP, 0.02))
            .repair(new RepairSpec(null, null, HEAVY_CLOTH, 0.0, 4))
            .build(),
        PieceDef.builder(ArmorSlot.LEGGINGS, "Bandit Pants")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.125, ArmorBonus.JUMP, 0.02))
            .repair(new RepairSpec(null, null, HEAVY_CLOTH, 0.0, 3))
            .build(),
        PieceDef.builder(ArmorSlot.BOOTS, "Bandit Boots")
            .bonuses(
                BonusMap.of(
                    ArmorBonus.SPEED,
                    0.125,
                    ArmorBonus.JUMP,
                    0.10,
                    ArmorBonus.FALL_REDUCTION,
                    0.2,
                    ArmorBonus.FREE_FALL_HEIGHT,
                    1.0))
            .repair(new RepairSpec(null, null, HEAVY_CLOTH, 0.0, 2))
            .build());

    /** T1 Miner, "Miner's ...": Steam/ULV, faster digging, four camos. */
    public static final SetDef T1_MINER = new SetDef(
        "t1_miner",
        Tier.STEAM,
        T1_MINER_MATERIAL,
        Arrays.asList("t1_miner", "t1_miner_red", "t1_miner_green", "t1_miner_black"),
        BonusMap.EMPTY,
        PieceDef.builder(ArmorSlot.HELMET, "Miner's Helmet")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.08, ArmorBonus.BREAK_SPEED, 0.05))
            .repair(new RepairSpec("plate", "CastIron", HEAVY_CLOTH, 0.5, 2))
            .build(),
        PieceDef.builder(ArmorSlot.CHESTPLATE, "Miner's Jacket")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.08, ArmorBonus.BREAK_SPEED, 0.05))
            .repair(new RepairSpec("plate", "CastIron", HEAVY_CLOTH, 0.5, 4))
            .build(),
        PieceDef.builder(ArmorSlot.LEGGINGS, "Miner's Pants")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.08, ArmorBonus.BREAK_SPEED, 0.05))
            .repair(new RepairSpec("plate", "CastIron", HEAVY_CLOTH, 1.0 / 3.0, 3))
            .build(),
        PieceDef.builder(ArmorSlot.BOOTS, "Miner's Boots")
            .bonuses(
                BonusMap.of(
                    ArmorBonus.SPEED,
                    0.08,
                    ArmorBonus.JUMP,
                    0.10,
                    ArmorBonus.BREAK_SPEED,
                    0.05,
                    ArmorBonus.FALL_REDUCTION,
                    0.2,
                    ArmorBonus.FREE_FALL_HEIGHT,
                    1.0))
            .repair(new RepairSpec("plate", "CastIron", HEAVY_CLOTH, 0.5, 2))
            .build());

    /** Every set, in registration order. Later waves append here. */
    public static final List<SetDef> ALL = Collections.unmodifiableList(Arrays.asList(T1_SCOUT, T1_MINER, T1_COMBAT));

    private ArmorCatalog() {}

    /**
     * @param id a set id
     * @return the set, or null
     */
    public static SetDef find(String id) {
        for (SetDef s : ALL) {
            if (s.id()
                .equals(id)) {
                return s;
            }
        }
        return null;
    }
}
