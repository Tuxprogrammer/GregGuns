package com.gregguns.entity.npc.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.ammo.AmmoType;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.part.PartCatalog;

class NpcLootTest {

    @Test
    void doesNotReproduceNPC04_countNeverBelowOne() {
        LootDrop obsidian = LootDrop.range("ore:ingotSteel", 1, 2);
        Random rand = new Random(4);
        for (int i = 0; i < 5000; i++) {
            assertTrue(obsidian.count(rand) >= 1);
        }
        assertThrows(IllegalArgumentException.class, () -> new LootDrop("x:y", 0, 1));
    }

    @Test
    void doesNotReproduceNPC05_rangedCountsVary() {
        LootDrop redstone = LootDrop.range("minecraft:redstone", 1, 3);
        Random rand = new Random(5);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(redstone.count(rand));
        }
        assertEquals(3, seen.size());
    }

    @Test
    void rollsAreZeroToTwoPlusLooting() {
        Random rand = new Random(1);
        int max = 0;
        for (int i = 0; i < 2000; i++) {
            int n = LootTable.rolls(rand, 2);
            assertTrue(n >= 0 && n <= 4);
            max = Math.max(max, n);
        }
        assertEquals(4, max);
    }

    @Test
    void emptySlotsDropNothingAndWeightsFollowSlotCounts() {
        LootTable t = NpcProfiles.get("Commando")
            .table();
        assertNotNull(t);
        assertEquals(
            10,
            t.slots()
                .size());
        Map<String, Integer> seen = new HashMap<>();
        int nothing = 0;
        Random rand = new Random(2);
        for (int i = 0; i < 10000; i++) {
            LootDrop d = t.pick(rand);
            if (d == null) {
                nothing++;
            } else {
                seen.merge(d.ref(), 1, Integer::sum);
            }
        }
        assertTrue(nothing > 800 && nothing < 1200, "about 10% nothing: " + nothing);
        assertTrue(seen.get("ore:plateRubber") > 1700, "rubber fills two slots");
    }

    /** D349: Techguns' nine slots plus ten of energy-gun components, since both its guns are energy guns. */
    @Test
    void stormtrooperTableAddsEnergyGunComponents() {
        LootTable t = NpcProfiles.get("Stormtrooper")
            .table();
        assertNotNull(t);
        assertEquals(
            19,
            t.slots()
                .size());
        Set<String> refs = refs(t);
        for (String part : new String[] { "gregguns:part_barrel_laser", "gregguns:part_laser_focus",
            "gregguns:part_receiver_carbon", "gregguns:part_receiver_hardened", "gregguns:part_stock_carbon",
            "gt:plate.TungstenSteel", "gt:screw.TungstenSteel", "gt:plate.Titanium" }) {
            assertTrue(refs.contains(part), part + " in " + refs);
        }
        Random rand = new Random(9);
        for (int i = 0; i < 2000; i++) {
            assertNotNull(t.pick(rand), "every Stormtrooper slot drops something");
        }
    }

    /** D349: the Outcast (LuV) has a table: ammo for each of its guns, energy-gun parts and LuV metals. */
    @Test
    void outcastTablePaysForEveryGunAndInLuvMetals() {
        LootTable t = NpcProfiles.get("Outcast")
            .table();
        assertNotNull(t);
        Set<String> refs = refs(t);
        for (String want : new String[] { "gregguns:ammo_rifle_bullet", "gregguns:ammo_rocket",
            "gregguns:tank_fuel~full", "gregguns:tank_bio~full", "gregguns:part_barrel_laser",
            "gregguns:part_power_plating", "ore:plateRhodium-PlatedPalladium", "gt:ingot.HSSG" }) {
            assertTrue(refs.contains(want), want + " in " + refs);
        }
    }

    /**
     * D349: the Super Mutants keep their cybernetic parts and drop ammo for their Rocket Launcher, AK-47 and shotgun.
     */
    @Test
    void superMutantsKeepCyberneticPartsAndDropTheirAmmo() {
        for (String id : new String[] { "SuperMutantBasic", "SuperMutantHeavy", "SuperMutantElite" }) {
            LootTable t = NpcProfiles.get(id)
                .table();
            int cybernetic = 0;
            for (LootDrop d : t.slots()) {
                cybernetic += d != null && d.ref()
                    .equals("gregguns:part_cybernetic") ? 1 : 0;
            }
            assertEquals(15, cybernetic, id);
            Set<String> refs = refs(t);
            assertTrue(refs.contains("gregguns:ammo_rocket"), id + " " + refs);
            assertTrue(refs.contains("gregguns:ammo_rifle_bullet"), id + " " + refs);
            assertTrue(refs.contains("gregguns:ammo_shotgun_shell"), id + " " + refs);
        }
    }

    /**
     * D349: every mob that carries a gun with loose rounds or a tank also has that ammo in its table or fixed drops
     * (the
     * Cyberdemon's Nether Charge excepted, an owner call), and a mob whose guns are all energy guns has GregGuns
     * energy-gun components in its table.
     */
    @Test
    void everyGunCarrierDropsItsAmmoOrComponents() {
        Map<String, GunDefinition> defs = new HashMap<>();
        for (GunDefinition d : GunDefinitions.ALL) {
            defs.put(d.name(), d);
        }
        for (NpcProfile p : NpcProfiles.all()
            .values()) {
            if (p.equipment() == null || p.id()
                .equals("CyberDemon")) {
                continue;
            }
            Set<String> refs = p.table() == null ? new HashSet<>() : refs(p.table());
            for (LootDrop d : p.fixedDrops()) {
                refs.add(d.ref());
            }
            Set<String> bare = new HashSet<>();
            for (String r : refs) {
                bare.add(r.replace("~full", ""));
            }
            boolean allEnergy = true;
            for (EquipmentProfile.Weapon w : p.equipment()
                .weapons()) {
                GunDefinition def = defs.get(w.gun());
                if (def == null) {
                    continue;
                }
                if (def.stats()
                    .isEnergy()) {
                    continue;
                }
                allEnergy = false;
                String ammo = "gregguns:" + def.stats()
                    .looseAmmoType();
                assertTrue(bare.contains(ammo), p.id() + " carries " + w.gun() + " but drops no " + ammo);
            }
            if (allEnergy) {
                assertTrue(
                    refs.contains("gregguns:part_barrel_laser"),
                    p.id() + " carries only energy guns but drops no components: " + refs);
            }
        }
    }

    private static Set<String> refs(LootTable t) {
        Set<String> out = new HashSet<>();
        for (LootDrop d : t.slots()) {
            if (d != null) {
                out.add(d.ref());
            }
        }
        return out;
    }

    @Test
    void sameSeedSameDrops() {
        LootTable t = NpcProfiles.get("Bandit")
            .table();
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        for (StringBuilder out : new StringBuilder[] { a, b }) {
            Random rand = new Random(77);
            for (int i = 0; i < 50; i++) {
                LootDrop d = t.pick(rand);
                out.append(d == null ? "-" : d.ref() + d.count(rand))
                    .append(',');
            }
        }
        assertEquals(a.toString(), b.toString());
    }

    @Test
    void pigmanWeaponPoolGrowsWithDifficulty() {
        EquipmentProfile eq = NpcProfiles.get("ZombiePigmanSoldier")
            .equipment();
        Set<String> d0 = new HashSet<>();
        Set<String> d1 = new HashSet<>();
        Set<String> d2 = new HashSet<>();
        Random rand = new Random(3);
        for (int i = 0; i < 3000; i++) {
            d0.add(
                eq.roll(rand, 0, g -> true, s -> 1)
                    .weapon());
            d1.add(
                eq.roll(rand, 1, g -> true, s -> 1)
                    .weapon());
            d2.add(
                eq.roll(rand, 5, g -> true, s -> 1)
                    .weapon());
        }
        assertEquals(2, d0.size());
        assertEquals(4, d1.size());
        assertEquals(8, d2.size());
    }

    @Test
    void missingGunsAreNeverRolled() {
        EquipmentProfile eq = NpcProfiles.get("SuperMutantBasic")
            .equipment();
        Random rand = new Random(9);
        for (int i = 0; i < 500; i++) {
            String w = eq.roll(rand, 0, g -> !g.equals("laser_rifle"), s -> 1)
                .weapon();
            assertNotNull(w);
            assertFalse(w.equals("laser_rifle"));
        }
        assertNull(
            eq.roll(rand, 0, g -> false, s -> 1)
                .weapon());
    }

    @Test
    void farmerSharesOneCamoAcrossPieces() {
        EquipmentProfile eq = NpcProfiles.get("ZombieFarmer")
            .equipment();
        Random rand = new Random(11);
        Set<Integer> camos = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            EquipmentProfile.Gear g = eq.roll(rand, 0, x -> true, s -> 4);
            int first = g.armor()
                .get(0)
                .camo();
            for (EquipmentProfile.ArmorPick p : g.armor()) {
                assertEquals(first, p.camo());
                assertEquals("t1_miner", p.setId());
            }
            assertEquals(
                1,
                g.armor()
                    .get(0)
                    .slot());
            camos.add(first);
        }
        assertEquals(4, camos.size());
    }

    @Test
    void armySoldierHelmetIsCombatOrGreenBeret() {
        EquipmentProfile eq = NpcProfiles.get("ArmySoldier")
            .equipment();
        Random rand = new Random(13);
        int helmets = 0;
        Set<String> kinds = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            for (EquipmentProfile.ArmorPick p : eq.roll(rand, 0, x -> true, s -> 3)
                .armor()) {
                if (p.slot() == 0) {
                    helmets++;
                    kinds.add(p.setId() + "@" + p.camo());
                }
            }
        }
        assertTrue(helmets > 2400 && helmets < 2900, "about 66%: " + helmets);
        assertEquals(new HashSet<>(java.util.Arrays.asList("t2_combat@0", "t2_beret@2")), kinds);
    }

    @Test
    void sameSeedSameGear() {
        EquipmentProfile eq = NpcProfiles.get("Bandit")
            .equipment();
        EquipmentProfile.Gear a = eq.roll(new Random(5), 2, x -> true, s -> 4);
        EquipmentProfile.Gear b = eq.roll(new Random(5), 2, x -> true, s -> 4);
        assertEquals(a.weapon(), b.weapon());
        assertEquals(
            a.armor()
                .size(),
            b.armor()
                .size());
    }

    @Test
    void bossesDropFixedItemsAndNoRareDrop() {
        NpcProfile steve = NpcProfiles.get("PsychoSteve");
        assertFalse(steve.rareDrop());
        assertEquals(
            2,
            steve.fixedDrops()
                .size());
        assertEquals(
            "gregguns:chainsaw",
            steve.fixedDrops()
                .get(0)
                .ref());
        LootDrop fuel = steve.fixedDrops()
            .get(1);
        assertEquals(4, fuel.min());
        assertEquals(8, fuel.max());
        assertEquals(
            Tier.IV,
            NpcProfiles.get("DictatorDave")
                .ceiling());
    }

    /** GGS-10-21: everything a mob can hold, wear or drop is at or below its ceiling. */
    @Test
    void ceilingsHold() {
        Map<String, Tier> gunTiers = new HashMap<>();
        for (GunDefinition d : GunDefinitions.ALL) {
            gunTiers.put(d.name(), d.tier());
        }
        for (NpcProfile p : NpcProfiles.all()
            .values()) {
            if (p.equipment() != null) {
                for (EquipmentProfile.Weapon w : p.equipment()
                    .weapons()) {
                    checkGun(p, w.gun(), gunTiers);
                }
                for (EquipmentProfile.ArmorRule r : p.equipment()
                    .armor()) {
                    for (String id : r.sets()) {
                        SetDef set = ArmorCatalog.find(id.contains("@") ? id.substring(0, id.indexOf('@')) : id);
                        assertNotNull(set, p.id() + " armor set " + id);
                        assertTrue(
                            set.tier()
                                .ordinal()
                                <= p.ceiling()
                                    .ordinal(),
                            p.id() + " wears " + set.id() + " above " + p.ceiling());
                    }
                }
            }
            List<LootDrop> drops = new ArrayList<>(p.fixedDrops());
            if (p.table() != null) {
                drops.addAll(
                    p.table()
                        .slots());
            }
            for (LootDrop d : drops) {
                if (d == null) {
                    continue;
                }
                checkGun(p, d.ref(), gunTiers);
                Tier t = ownItemTier(d.ref());
                // The one accepted exception (D349): the Attack Helicopter's Techguns fixed drop of Cybernetic Parts
                // (per-mob.md section 16), EV over its HV.
                boolean accepted = p.id()
                    .equals("AttackHelicopter")
                    && d.ref()
                        .equals("gregguns:part_cybernetic");
                if (t != null && !accepted) {
                    assertTrue(
                        t.ordinal() <= p.ceiling()
                            .ordinal(),
                        p.id() + " drops " + d.ref() + " (" + t + ") above " + p.ceiling());
                }
            }
        }
    }

    /** @return the tier of a GregGuns round, part or tank reference, or null for anything else */
    private static Tier ownItemTier(String ref) {
        if (!ref.startsWith("gregguns:")) {
            return null;
        }
        String name = ref.substring("gregguns:".length())
            .replace("~full", "");
        int hash = name.indexOf('#');
        if (hash >= 0) {
            name = name.substring(0, hash);
        }
        for (AmmoType a : AmmoTypes.allAmmo()
            .values()) {
            if (a.roundItemName()
                .equals(name)) {
                return a.tier();
            }
        }
        return PartCatalog.tierOf(name);
    }

    private static void checkGun(NpcProfile p, String ref, Map<String, Tier> gunTiers) {
        String name = ref.startsWith("gregguns:") ? ref.substring("gregguns:".length()) : ref;
        Tier t = gunTiers.get(name);
        if (t != null) {
            assertTrue(
                t.ordinal() <= p.ceiling()
                    .ordinal(),
                p.id() + " has " + name + " (" + t + ") above " + p.ceiling());
        }
    }

    @Test
    void everyArmorSetInTheDataExists() {
        for (NpcProfile p : NpcProfiles.all()
            .values()) {
            if (p.equipment() == null) {
                continue;
            }
            for (EquipmentProfile.ArmorRule r : p.equipment()
                .armor()) {
                for (String id : r.sets()) {
                    String set = id.contains("@") ? id.substring(0, id.indexOf('@')) : id;
                    assertNotNull(ArmorCatalog.find(set), set);
                }
            }
        }
    }

    @Test
    void infernalBandsFollowModifierCount() {
        assertEquals(InfernalBands.ELITE, InfernalBands.forModifiers(2));
        assertEquals(InfernalBands.ELITE, InfernalBands.forModifiers(5));
        assertEquals(InfernalBands.ULTRA, InfernalBands.forModifiers(6));
        assertEquals(InfernalBands.ULTRA, InfernalBands.forModifiers(10));
        assertEquals(InfernalBands.INFERNAL, InfernalBands.forModifiers(11));
        assertEquals(1, InfernalBands.rolls(2));
        assertEquals(1, InfernalBands.rolls(5));
        assertEquals(2, InfernalBands.rolls(10));
        assertEquals(3, InfernalBands.rolls(15));
    }

    @Test
    void infernalCeilingsNeverExceedHvOrTheMob() {
        assertEquals(Tier.LV, InfernalBands.ELITE.ceilingFor(Tier.HV, false));
        assertEquals(Tier.MV, InfernalBands.ULTRA.ceilingFor(Tier.HV, false));
        assertEquals(Tier.MV, InfernalBands.INFERNAL.ceilingFor(Tier.HV, false));
        assertEquals(Tier.HV, InfernalBands.INFERNAL.ceilingFor(Tier.EV, true));
        assertEquals(Tier.STEAM, InfernalBands.INFERNAL.ceilingFor(Tier.STEAM, true));
        for (InfernalBands b : InfernalBands.values()) {
            assertTrue(
                b.rareCeiling()
                    .ordinal() <= Tier.HV.ordinal());
        }
    }
}
