package com.gregguns.entity.npc.loot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;

/**
 * The loot and equipment data of every M7 mob that can be built before the M5 energy guns: Zombie Farmer, Miner and
 * Soldier, Skeleton Soldier, Bandit, Army Soldier, Commando, Zombie Pigman Soldier, Psycho Steve, Dictator Dave and the
 * three Super Mutants. Outcast, Stormtrooper and Cyberdemon wait for their guns (PLAN-M7 section 5, answer 5). Keys
 * are the registry names without the mod id.
 *
 * <p>
 * Item references: {@code mod:name} for a registered item, {@code ore:Name} for an ore dictionary entry (GT first, so
 * steel and rubber come from GregTech). A weapon that holds an item with a colon is a vanilla tool (hoe, shovel,
 * pick); a bare name is a GregGuns gun.
 *
 * <p>
 * Deviations from Techguns, all by design: counts are uniform integer draws and never zero (NPC-04, NPC-05; the
 * Stormtrooper redstone is not built yet); the {@code RestrictedLoottables} variant is not carried; the Commando's
 * silenced M4 is the M4 until a silenced variant exists; "obsidian steel ingot" is a GT steel ingot; Psycho Steve's
 * HV Chainsaw is accepted as his ceiling (design 0005 amendment, mob-drops-nei.md open question 1).
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (each mob's Armor, Weapon, Loot), BUGS NPC-04/05
public final class NpcProfiles {

    private static final String G = "gregguns:";
    private static final LootDrop HEAVY_CLOTH = LootDrop.one(G + "part_heavy_cloth");
    private static final LootDrop IRON = LootDrop.one("minecraft:iron_ingot");
    private static final LootDrop GUNPOWDER = LootDrop.one("minecraft:gunpowder");
    private static final LootDrop SHELLS = LootDrop.range(G + "ammo_shotgun_shell", 1, 8);
    private static final LootDrop RIFLE = LootDrop.range(G + "ammo_rifle_bullet", 1, 3);
    private static final LootDrop NINE_MM = LootDrop.range(G + "ammo_9mm_bullet", 1, 3);
    private static final LootDrop STEEL_INGOT = LootDrop.one("ore:ingotSteel");
    private static final LootDrop CYBERNETIC = LootDrop.one(G + "part_cybernetic");

    private static final Map<String, NpcProfile> ALL = build();

    private NpcProfiles() {}

    /**
     * @param id a mob's registry name without the mod id
     * @return its profile, or null when the mob has none (yet)
     */
    public static @Nullable NpcProfile get(String id) {
        return ALL.get(id);
    }

    /** @return every profile by registry name, in a stable order */
    public static Map<String, NpcProfile> all() {
        return ALL;
    }

    private static Map<String, NpcProfile> build() {
        Map<String, NpcProfile> m = new LinkedHashMap<>();
        add(
            m,
            NpcProfile.builder("ZombieSoldier", Tier.MV)
                .table(
                    LootTable.builder()
                        .slot(HEAVY_CLOTH)
                        .slot(IRON)
                        .slot(GUNPOWDER)
                        .slot(SHELLS)
                        .slot(NINE_MM)
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("revolver", 1)
                        .weapon("thompson_smg", 1)
                        .weapon("minecraft:iron_shovel", 1)
                        .weapon("minecraft:stone_shovel", 1)
                        .armor(0, 50, 0, "t1_combat")
                        .armor(1, 50, 0, "t1_combat")
                        .armor(2, 50, 0, "t1_combat")
                        .armor(3, 50, 0, "t1_combat")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("ZombieFarmer", Tier.STEAM)
                .table(
                    LootTable.builder()
                        .slot(HEAVY_CLOTH)
                        .slot(LootDrop.range("minecraft:wheat_seeds", 1, 3))
                        .slot(GUNPOWDER)
                        .slot(LootDrop.range("minecraft:wheat", 1, 3))
                        .slot(LootDrop.range("minecraft:leather", 1, 3))
                        .slot(LootDrop.range("minecraft:bread", 1, 2))
                        .slot(LootDrop.range("minecraft:apple", 1, 2))
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("minecraft:wooden_hoe", 1)
                        .weapon("minecraft:iron_hoe", 1)
                        .weapon("minecraft:stone_hoe", 1)
                        .weapon("handcannon", 1)
                        .sharedCamoOf(4)
                        .sharedCamo(1, 100, "t1_miner")
                        .sharedCamo(2, 50, "t1_miner")
                        .sharedCamo(3, 50, "t1_miner")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("ZombieMiner", Tier.STEAM)
                .table(
                    LootTable.builder()
                        .slot(HEAVY_CLOTH)
                        .slot(LootDrop.one("minecraft:coal"))
                        .slot(GUNPOWDER)
                        .slot(IRON)
                        .slot(LootDrop.one("minecraft:redstone"))
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("minecraft:wooden_pickaxe", 1)
                        .weapon("minecraft:iron_pickaxe", 1)
                        .weapon("minecraft:stone_pickaxe", 1)
                        .weapon("handcannon", 1)
                        .sharedCamoOf(4)
                        .sharedCamo(0, 100, "t1_miner")
                        .sharedCamo(1, 50, "t1_miner")
                        .sharedCamo(2, 50, "t1_miner")
                        .sharedCamo(3, 50, "t1_miner")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("ZombiePigmanSoldier", Tier.HV)
                .table(
                    LootTable.builder()
                        .slot(LootDrop.one("ore:plateSteel"))
                        .slot(LootDrop.range("ore:ingotSteel", 1, 2))
                        .slot(LootDrop.range("minecraft:gold_ingot", 1, 3))
                        .slot(LootDrop.range("minecraft:gunpowder", 1, 5))
                        .slot(LootDrop.range(G + "ammo_shotgun_shell", 1, 7))
                        .slot(RIFLE)
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("thompson_smg", 3, 0)
                        .weapon("revolver", 2, 0)
                        .weapon("ak47", 2, 1)
                        .weapon("pistol_9mm", 2, 1)
                        .weapon("flamethrower", 2, 2)
                        .weapon("combat_shotgun", 2, 2)
                        .weapon("lmg", 1, 2)
                        .weapon("rocket_launcher", 1, 2)
                        .armor(0, 100, 3, "t2_combat")
                        .armor(1, 50, 3, "t2_combat")
                        .armor(2, 50, 3, "t2_combat")
                        .armor(3, 50, 3, "t2_combat")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("SkeletonSoldier", Tier.MV)
                .table(
                    LootTable.builder()
                        .slot(LootDrop.range("minecraft:bone", 1, 2))
                        .slot(LootDrop.range("minecraft:coal", 1, 3))
                        .slot(GUNPOWDER)
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("revolver", 1)
                        .weapon("thompson_smg", 1)
                        .weapon("handcannon", 1)
                        .armor(0, 100, 0, "t1_combat", "t1_scout")
                        .armor(3, 100, 0, "t1_combat", "t1_scout")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("ArmySoldier", Tier.MV)
                .table(
                    LootTable.builder()
                        .slot(HEAVY_CLOTH)
                        .slot(IRON)
                        .slot(GUNPOWDER)
                        .slot(SHELLS)
                        .slot(RIFLE)
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("m4", 1)
                        .weapon("combat_shotgun", 1)
                        .weapon("bolt_action_rifle", 1)
                        .armor(0, 66, 0, "t2_combat@0", "t2_beret@2")
                        .armor(1, 50, 0, "t2_combat")
                        .armor(2, 50, 0, "t2_combat")
                        .armor(3, 50, 0, "t2_combat")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("Bandit", Tier.MV)
                .table(
                    LootTable.builder()
                        .slot(HEAVY_CLOTH)
                        .slot(IRON)
                        .slot(GUNPOWDER)
                        .slot(SHELLS)
                        .slot(RIFLE)
                        .slot(LootDrop.range(G + "ammo_9mm_bullet", 1, 4))
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("pistol_9mm", 1)
                        .weapon("ak47", 1)
                        .weapon("bolt_action_rifle", 1)
                        .weapon("sawed_off", 1)
                        .weapon("thompson_smg", 1)
                        .weapon("revolver", 1)
                        .armor(0, 50, 0, "t1_scout")
                        .armor(1, 100, 0, "t1_scout")
                        .armor(2, 100, 0, "t1_scout")
                        .armor(3, 100, 0, "t1_scout")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("Commando", Tier.MV)
                .table(
                    LootTable.builder()
                        .slot(STEEL_INGOT)
                        .nothing(1)
                        .slots(LootDrop.one("ore:plateRubber"), 2)
                        .slots(GUNPOWDER, 2)
                        .slots(RIFLE, 2)
                        .slot(LootDrop.one(G + "explosive_charge"))
                        .slot(LootDrop.one(G + "explosive_charge#1"))
                        .build())
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("m4", 1)
                        .armor(0, 100, 0, "t2_commando")
                        .armor(1, 100, 0, "t2_commando")
                        .armor(2, 100, 0, "t2_commando")
                        .armor(3, 100, 0, "t2_commando")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("PsychoSteve", Tier.HV)
                .fixed(LootDrop.one(G + "chainsaw"))
                .fixed(LootDrop.range(G + "tank_fuel", 4, 8))
                .rareDrop(false)
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("chainsaw", 1)
                        .armor(0, 100, 1, "t1_miner")
                        .armor(1, 100, 1, "t1_miner")
                        .armor(2, 100, 1, "t1_miner")
                        .armor(3, 100, 1, "t1_miner")
                        .build())
                .build());
        add(
            m,
            NpcProfile.builder("DictatorDave", Tier.IV)
                .fixed(LootDrop.one(G + "golden_revolver"))
                .fixed(LootDrop.range(G + "ammo_9mm_bullet", 8, 16))
                .rareDrop(false)
                .equipment(
                    EquipmentProfile.builder()
                        .weapon("golden_revolver", 1)
                        .armor(0, 100, 0, "t2_beret")
                        .armor(1, 100, 0, "t1_combat")
                        .armor(2, 100, 2, "t1_scout")
                        .armor(3, 100, 0, "t1_combat")
                        .build())
                .build());
        // Attack Helicopter: fixed drops only (per-mob.md section 16), no gear. Ranges are the spec's 16 + nextInt(16)
        // rifle bullets, 8 + nextInt(8) rockets, 2 + nextInt(2) cybernetic parts and 4 + nextInt(4) iron blocks.
        add(
            m,
            NpcProfile.builder("AttackHelicopter", Tier.HV)
                .fixed(LootDrop.range(G + "ammo_rifle_bullet", 16, 31))
                .fixed(LootDrop.range(G + "ammo_rocket", 8, 15))
                .fixed(LootDrop.range(G + "part_cybernetic", 2, 3))
                .fixed(LootDrop.range("minecraft:iron_block", 4, 7))
                .rareDrop(false)
                .build());
        for (String mutant : new String[] { "SuperMutantBasic", "SuperMutantHeavy", "SuperMutantElite" }) {
            add(
                m,
                NpcProfile.builder(mutant, Tier.EV)
                    .table(
                        LootTable.builder()
                            .slot(LootDrop.one("minecraft:ghast_tear"))
                            .slots(LootDrop.one("minecraft:blaze_rod"), 4)
                            .slots(CYBERNETIC, 15)
                            .build())
                    .equipment(
                        EquipmentProfile.builder()
                            .weapon("rocket_launcher", 1)
                            .weapon("ak47", 1)
                            .weapon("combat_shotgun", 1)
                            .weapon("laser_gun", 2)
                            .build())
                    .build());
        }
        return Collections.unmodifiableMap(m);
    }

    private static void add(Map<String, NpcProfile> m, NpcProfile p) {
        m.put(p.id(), p);
    }
}
