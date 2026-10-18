package com.gregguns.armor.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;

/**
 * Every armor set GregGuns defines, as data. To add a set (T2, T3, powered), add a {@link SetDef} here and list it in
 * {@link #ALL}; items, lang keys (see the dev self-check), tooltips, bonuses, hazard flags and layer textures follow
 * from the data. Plain Java, both sides.
 *
 * <p>
 * M3 wave A: the three T1 sets; wave B: T2 Combat, Commando, Riot, Beret, T3 Combat, HEV and steam armor. Stats are the
 * Techguns values ({@code armor-catalog.md} 5.1 to 5.3). Deviations:
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

    // ---- M3 wave B: T2 and T3 sets and steam armor ---------------------------------------------------------------

    /** Registry name of the Treated Leather the Ranger Veteran set is repaired with (RC-22). */
    public static final String TREATED_LEATHER = "part_treated_leather";
    /** Registry name of the Steam Armor Plating the steam set is built and repaired with (RC-23). */
    public static final String STEAM_ARMOR_PLATING = "part_steam_armor_plating";

    private static final double[] REPAIR_RATIO = { 0.5, 0.5, 1.0 / 3.0, 0.5 };
    private static final int[] REPAIR_MATS = { 2, 4, 3, 2 };

    /** The repair recipe of a slot with the Techguns ratio and part count (armor-catalog 5.5 to 5.11). */
    private static RepairSpec repairFor(ArmorSlot slot, String metalPrefix, String metalMaterial, String cloth) {
        return new RepairSpec(
            metalPrefix,
            metalMaterial,
            cloth,
            REPAIR_RATIO[slot.ordinal()],
            REPAIR_MATS[slot.ordinal()]);
    }

    private static final ArmorMaterialDef T2_COMBAT_MATERIAL = ArmorMaterialDef.uniform("t2_combat", 72, 19.0);
    private static final ArmorMaterialDef T2_COMMANDO_MATERIAL = ArmorMaterialDef.uniform("t2_commando", 72, 18.0)
        .with(DamageType.EXPLOSION, 15.0)
        .with(DamageType.ENERGY, 15.0)
        .with(DamageType.FIRE, 15.0)
        .with(DamageType.ICE, 15.0)
        .with(DamageType.LIGHTNING, 15.0)
        .with(DamageType.POISON, 10.0)
        .with(DamageType.RADIATION, 5.0);
    private static final ArmorMaterialDef T2_RIOT_MATERIAL = ArmorMaterialDef.uniform("t2_riot", 96, 20.0)
        .with(DamageType.EXPLOSION, 18.0)
        .with(DamageType.ENERGY, 18.0)
        .with(DamageType.FIRE, 18.0)
        .with(DamageType.ICE, 17.0)
        .with(DamageType.LIGHTNING, 17.0)
        .with(DamageType.POISON, 16.0)
        .with(DamageType.RADIATION, 16.0)
        .withPenetrationResistance(0.10);
    private static final ArmorMaterialDef T2_BERET_MATERIAL = ArmorMaterialDef.uniform("t2_beret", 60, 8.0);
    private static final ArmorMaterialDef T3_COMBAT_MATERIAL = ArmorMaterialDef.uniform("t3_combat", 240, 22.0)
        .with(DamageType.EXPLOSION, 18.0)
        .with(DamageType.ENERGY, 18.0)
        .with(DamageType.FIRE, 18.0)
        .with(DamageType.ICE, 18.0)
        .with(DamageType.LIGHTNING, 18.0)
        .with(DamageType.POISON, 10.0)
        .with(DamageType.RADIATION, 6.0)
        .withPenetrationResistance(0.15);
    private static final ArmorMaterialDef T3_HEV_MATERIAL = ArmorMaterialDef.uniform("t3_hev", 300, 21.0)
        .with(DamageType.EXPLOSION, 21.0)
        .with(DamageType.ENERGY, 21.0)
        .with(DamageType.FIRE, 21.0)
        .with(DamageType.ICE, 21.0)
        .with(DamageType.LIGHTNING, 21.0)
        .with(DamageType.POISON, 21.0)
        .with(DamageType.RADIATION, 21.0)
        .withPenetrationResistance(0.15);
    private static final ArmorMaterialDef STEAM_MATERIAL = ArmorMaterialDef.uniform("steam", 200, 20.0)
        .with(DamageType.EXPLOSION, 18.0)
        .with(DamageType.ENERGY, 16.0)
        .with(DamageType.FIRE, 18.0)
        .with(DamageType.ICE, 16.0)
        .with(DamageType.LIGHTNING, 16.0)
        .with(DamageType.POISON, 10.0)
        .with(DamageType.RADIATION, 6.0)
        .withPenetrationResistance(0.10);

    private static final double[] T2_COMBAT_KB = { 0.10, 0.25, 0.15, 0.10 };
    private static final double[] T2_COMMANDO_KB = { 0.05, 0.20, 0.10, 0.05 };
    private static final double[] T2_RIOT_KB = { 0.125, 0.275, 0.175, 0.125 };
    private static final double[] T2_RIOT_SPEED = { 0.12, 0.12, 0.13, 0.13 };
    private static final double[] T3_COMBAT_KB = { 0.15, 0.30, 0.20, 0.15 };
    private static final double[] STEAM_KB = { 0.15, 0.30, 0.20, 0.15 };
    private static final double[] HEV_KB = { 0.10, 0.25, 0.15, 0.10 };
    private static final double[] HEV_JUMP = { 0.30, 0.03, 0.03, 0.15 };
    private static final double[] STEAM_HEALTH = { 1.0, 2.0, 1.0, 1.0 };
    private static final double[] STEAM_JUMP = { 0.03, 0.03, 0.03, 0.15 };

    /** T2 Combat, "Combat ...": MV, six camos, speed and knockback resistance. */
    public static final SetDef T2_COMBAT = new SetDef(
        "t2_combat",
        Tier.MV,
        T2_COMBAT_MATERIAL,
        Arrays.asList(
            "t2_combat",
            "t2_combat_wood",
            "t2_combat_desert",
            "t2_combat_arctic",
            "t2_combat_swat",
            "t2_combat_security"),
        BonusMap.EMPTY,
        combatPiece(ArmorSlot.HELMET, "Combat Helmet", 0.0),
        combatPiece(ArmorSlot.CHESTPLATE, "Combat Vest", 0.0),
        combatPiece(ArmorSlot.LEGGINGS, "Combat Pants", 0.0),
        combatPiece(ArmorSlot.BOOTS, "Combat Boots", 0.10));

    private static PieceDef combatPiece(ArmorSlot slot, String name, double jump) {
        return PieceDef.builder(slot, name)
            .bonuses(
                BonusMap.of(
                    ArmorBonus.SPEED,
                    0.10,
                    ArmorBonus.JUMP,
                    jump,
                    ArmorBonus.KNOCKBACK_RESISTANCE,
                    T2_COMBAT_KB[slot.ordinal()]))
            .repair(repairFor(slot, "plate", "Aluminium", HEAVY_CLOTH))
            .build();
    }

    /**
     * T2 Commando, "Commando ...": MV, no camo, no repair. Underwater mining, gun accuracy, oxygen gear on the helmet,
     * rubber insulation (partial ELECTRICAL).
     */
    public static final SetDef T2_COMMANDO = new SetDef(
        "t2_commando",
        Tier.MV,
        T2_COMMANDO_MATERIAL,
        Collections.singletonList("t2_commando"),
        BonusMap.EMPTY,
        commandoPiece(ArmorSlot.HELMET, "Commando Headgear", BonusMap.of(ArmorBonus.OXYGEN_GEAR, 1.0)),
        commandoPiece(ArmorSlot.CHESTPLATE, "Commando Vest", BonusMap.EMPTY),
        commandoPiece(ArmorSlot.LEGGINGS, "Commando Pants", BonusMap.EMPTY),
        commandoPiece(
            ArmorSlot.BOOTS,
            "Commando Boots",
            BonusMap.of(ArmorBonus.JUMP, 0.10, ArmorBonus.FALL_REDUCTION, 0.2, ArmorBonus.FREE_FALL_HEIGHT, 1.0)));

    private static PieceDef commandoPiece(ArmorSlot slot, String name, BonusMap extra) {
        return PieceDef.builder(slot, name)
            .bonuses(
                BonusMap
                    .of(
                        ArmorBonus.SPEED,
                        0.10,
                        ArmorBonus.BREAK_SPEED_WATER,
                        1.25,
                        ArmorBonus.GUN_ACCURACY,
                        0.05,
                        ArmorBonus.KNOCKBACK_RESISTANCE,
                        T2_COMMANDO_KB[slot.ordinal()])
                    .plus(extra))
            .partialHazards(HazardFlag.ELECTRICAL)
            .build();
    }

    /** T2 Riot, "Ranger Veteran ...": MV, upgraded from a Combat piece, lead lining (partial RADIOLOGICAL). */
    public static final SetDef T2_RIOT = new SetDef(
        "t2_riot",
        Tier.MV,
        T2_RIOT_MATERIAL,
        Collections.singletonList("t2_riot"),
        BonusMap.EMPTY,
        riotPiece(ArmorSlot.HELMET, "Ranger Veteran Helmet", 0.0),
        riotPiece(ArmorSlot.CHESTPLATE, "Ranger Veteran Jacket", 0.0),
        riotPiece(ArmorSlot.LEGGINGS, "Ranger Veteran Pants", 0.0),
        riotPiece(ArmorSlot.BOOTS, "Ranger Veteran Boots", 0.15));

    private static PieceDef riotPiece(ArmorSlot slot, String name, double jump) {
        return PieceDef.builder(slot, name)
            .bonuses(
                BonusMap.of(
                    ArmorBonus.SPEED,
                    T2_RIOT_SPEED[slot.ordinal()],
                    ArmorBonus.JUMP,
                    jump,
                    ArmorBonus.KNOCKBACK_RESISTANCE,
                    T2_RIOT_KB[slot.ordinal()]))
            .partialHazards(HazardFlag.RADIOLOGICAL)
            .repair(repairFor(slot, "plate", "Aluminium", TREATED_LEATHER))
            .build();
    }

    /** The Beret: a one-piece MV set, registry name {@code t2_beret}, three colors on one sheet each. */
    public static final SetDef T2_BERET = new SetDef(
        "t2_beret",
        Tier.MV,
        T2_BERET_MATERIAL,
        Arrays.asList("t2_beret", "t2_beret_black", "t2_beret_green"),
        SetDef.TextureLayout.SINGLE,
        PowerSpec.NONE,
        BonusMap.EMPTY,
        PieceDef.builder(ArmorSlot.HELMET, "Beret")
            .registryName("t2_beret")
            .bonuses(BonusMap.of(ArmorBonus.SPEED, 0.10))
            .repair(new RepairSpec(null, null, HEAVY_CLOTH, 0.0, 2))
            .build());

    /** T3 Combat, "Advanced Combat ...": EV, three camos, penetration resistance 15%. */
    public static final SetDef T3_COMBAT = new SetDef(
        "t3_combat",
        Tier.EV,
        T3_COMBAT_MATERIAL,
        Arrays.asList("t3_combat", "t3_combat_silver", "t3_combat_green"),
        BonusMap.EMPTY,
        advancedPiece(ArmorSlot.HELMET, "Advanced Combat Helmet", 0.0),
        advancedPiece(ArmorSlot.CHESTPLATE, "Advanced Combat Chestplate", 0.0),
        advancedPiece(ArmorSlot.LEGGINGS, "Advanced Combat Leggings", 0.0),
        advancedPiece(ArmorSlot.BOOTS, "Advanced Combat Boots", 0.10));

    private static PieceDef advancedPiece(ArmorSlot slot, String name, double jump) {
        return PieceDef.builder(slot, name)
            .bonuses(
                BonusMap.of(
                    ArmorBonus.SPEED,
                    0.10,
                    ArmorBonus.JUMP,
                    jump,
                    ArmorBonus.KNOCKBACK_RESISTANCE,
                    T3_COMBAT_KB[slot.ordinal()]))
            .repair(repairFor(slot, "plate", "EpoxidFiberReinforced", HEAVY_CLOTH))
            .build();
    }

    /**
     * Steam armor: EV, four camos on single sheets, powered by compressed air in the chestplate. Every bonus is
     * powered; heat insulation (partial HEAT) is not.
     */
    public static final SetDef STEAM = new SetDef(
        "steam",
        Tier.EV,
        STEAM_MATERIAL,
        Arrays.asList("steam", "steam_rusty", "steam_iron", "steam_hazard"),
        SetDef.TextureLayout.SINGLE,
        PowerSpec.steam(3600),
        BonusMap.EMPTY,
        steamPiece(ArmorSlot.HELMET, "Steam Helmet", BonusMap.EMPTY),
        steamPiece(ArmorSlot.CHESTPLATE, "Steam Chestplate", BonusMap.EMPTY),
        steamPiece(ArmorSlot.LEGGINGS, "Steam Legplates", BonusMap.EMPTY),
        steamPiece(
            ArmorSlot.BOOTS,
            "Steam Boots",
            BonusMap
                .of(ArmorBonus.STEP_ASSIST, 1.0, ArmorBonus.FALL_REDUCTION, 0.2, ArmorBonus.FREE_FALL_HEIGHT, 1.0)));

    private static PieceDef steamPiece(ArmorSlot slot, String name, BonusMap extra) {
        return PieceDef.builder(slot, name)
            .poweredBonuses(
                BonusMap
                    .of(
                        ArmorBonus.SPEED,
                        0.05,
                        ArmorBonus.JUMP,
                        STEAM_JUMP[slot.ordinal()],
                        ArmorBonus.BREAK_SPEED,
                        0.05,
                        ArmorBonus.EXTRA_HEALTH,
                        STEAM_HEALTH[slot.ordinal()],
                        ArmorBonus.KNOCKBACK_RESISTANCE,
                        STEAM_KB[slot.ordinal()])
                    .plus(extra))
            .partialHazards(HazardFlag.HEAT)
            .repair(repairFor(slot, null, null, STEAM_ARMOR_PLATING))
            .build();
    }

    /**
     * T3 Hazardous Environment ("HEV"): EV, three camos, an IC2 electric chestplate. Gas, biological and radiological
     * cover is passive; heat, frost and electrical cover and every bonus need a charged chestplate.
     */
    public static final SetDef T3_HEV = new SetDef(
        "t3_hev",
        Tier.EV,
        T3_HEV_MATERIAL,
        Arrays.asList("t3_hev", "t3_hev_silver", "t3_hev_black"),
        SetDef.TextureLayout.DOUBLE,
        PowerSpec.electric(10000, 40.0),
        BonusMap.EMPTY,
        hevPiece(ArmorSlot.HELMET, "Hazardous Environment Helmet", BonusMap.of(ArmorBonus.OXYGEN_GEAR, 1.0)),
        hevPiece(ArmorSlot.CHESTPLATE, "Hazardous Environment Chestplate", BonusMap.of(ArmorBonus.COOLING_SYSTEM, 1.0)),
        hevPiece(ArmorSlot.LEGGINGS, "Hazardous Environment Leggings", BonusMap.EMPTY),
        hevPiece(
            ArmorSlot.BOOTS,
            "Hazardous Environment Boots",
            BonusMap
                .of(ArmorBonus.STEP_ASSIST, 1.0, ArmorBonus.FALL_REDUCTION, 0.5, ArmorBonus.FREE_FALL_HEIGHT, 3.0)));

    private static PieceDef hevPiece(ArmorSlot slot, String name, BonusMap extra) {
        return PieceDef.builder(slot, name)
            .poweredBonuses(
                BonusMap
                    .of(
                        ArmorBonus.SPEED,
                        0.10,
                        ArmorBonus.SPEED_WATER,
                        1.25,
                        ArmorBonus.JUMP,
                        HEV_JUMP[slot.ordinal()],
                        ArmorBonus.BREAK_SPEED,
                        0.10,
                        ArmorBonus.BREAK_SPEED_WATER,
                        1.25,
                        ArmorBonus.KNOCKBACK_RESISTANCE,
                        HEV_KB[slot.ordinal()])
                    .plus(extra))
            .partialHazards(HazardFlag.BIOLOGICAL, HazardFlag.GAS, HazardFlag.RADIOLOGICAL)
            .poweredPartialHazards(HazardFlag.HEAT, HazardFlag.FROST, HazardFlag.ELECTRICAL)
            .repair(repairFor(slot, "plate", "EpoxidFiberReinforced", HEAVY_CLOTH))
            .build();
    }

    /** Every set, in registration order. Later waves append here. */
    public static final List<SetDef> ALL = Collections.unmodifiableList(
        Arrays.asList(
            T1_SCOUT,
            T1_MINER,
            T1_COMBAT,
            T2_COMBAT,
            T2_COMMANDO,
            T2_RIOT,
            T2_BERET,
            T3_COMBAT,
            STEAM,
            T3_HEV));

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
