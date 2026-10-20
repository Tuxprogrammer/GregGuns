package com.gregguns.entity.npc.loot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

/**
 * What an NPC may carry and wear, as data, and the pure roll that picks it. No Minecraft types: weapons are gun
 * registry names, armor is a set id plus slot index plus camo index, so the roll is unit-testable with a seeded
 * {@link Random} (GGS-10-15).
 *
 * <p>
 * Weapon: a weighted pool; an entry joins the pool at its {@code minDifficulty} (the Pigman Soldier's pool grows with
 * the spawn danger). Guns that are not registered yet are dropped from the pool by the {@code gunExists} filter, so a
 * mob never rolls an empty hand because its first choice does not exist. Armor: per slot a chance and one or more set
 * choices; a camo is fixed, drawn per piece, or drawn once for the whole roll (the shared camo of the Miner sets).
 * Armor slots use vanilla's {@code armorType}: 0 helmet to 3 boots.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Armor and Weapon of each mob), GGS-10-17
public final class EquipmentProfile {

    private final List<Weapon> weapons;
    private final List<ArmorRule> armor;
    private final int sharedCamoCount;

    private EquipmentProfile(List<Weapon> weapons, List<ArmorRule> armor, int sharedCamoCount) {
        this.weapons = Collections.unmodifiableList(weapons);
        this.armor = Collections.unmodifiableList(armor);
        this.sharedCamoCount = sharedCamoCount;
    }

    /** @return a builder */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the weapon pool */
    public List<Weapon> weapons() {
        return weapons;
    }

    /** @return the armor rules */
    public List<ArmorRule> armor() {
        return armor;
    }

    /** A weapon in the pool. */
    public static final class Weapon {

        private final String gun;
        private final int weight;
        private final int minDifficulty;

        Weapon(String gun, int weight, int minDifficulty) {
            this.gun = gun;
            this.weight = weight;
            this.minDifficulty = minDifficulty;
        }

        /** @return the gun or held item registry name, without the mod id */
        public String gun() {
            return gun;
        }

        /** @return the pool weight */
        public int weight() {
            return weight;
        }

        /** @return the smallest difficulty at which it is in the pool */
        public int minDifficulty() {
            return minDifficulty;
        }
    }

    /** One armor slot's rule. */
    public static final class ArmorRule {

        private final int slot;
        private final int percent;
        private final List<String> sets;
        private final int camoMode;
        private final int camoArg;

        ArmorRule(int slot, int percent, List<String> sets, int camoMode, int camoArg) {
            this.slot = slot;
            this.percent = percent;
            this.sets = Collections.unmodifiableList(sets);
            this.camoMode = camoMode;
            this.camoArg = camoArg;
        }

        /** @return the armor slot, 0 helmet to 3 boots */
        public int slot() {
            return slot;
        }

        /** @return the chance this slot is filled, 0 to 100 */
        public int percent() {
            return percent;
        }

        /** @return the set ids it may use, one picked with equal chance */
        public List<String> sets() {
            return sets;
        }
    }

    /** A rolled armor piece. */
    public static final class ArmorPick {

        private final int slot;
        private final String setId;
        private final int camo;

        ArmorPick(int slot, String setId, int camo) {
            this.slot = slot;
            this.setId = setId;
            this.camo = camo;
        }

        /** @return the armor slot, 0 helmet to 3 boots */
        public int slot() {
            return slot;
        }

        /** @return the set id, for example {@code t1_combat} */
        public String setId() {
            return setId;
        }

        /** @return the camo index */
        public int camo() {
            return camo;
        }
    }

    /** The result of one roll. */
    public static final class Gear {

        private final String weapon;
        private final List<ArmorPick> armor;

        Gear(@Nullable String weapon, List<ArmorPick> armor) {
            this.weapon = weapon;
            this.armor = Collections.unmodifiableList(armor);
        }

        /** @return the gun name, or null for an empty hand */
        public @Nullable String weapon() {
            return weapon;
        }

        /** @return the armor pieces */
        public List<ArmorPick> armor() {
            return armor;
        }
    }

    /**
     * Rolls one set of gear. Draw order is fixed (weapon, shared camo, then slots helmet to boots), so one seed gives
     * one result.
     *
     * @param rand       the entity's own random source
     * @param difficulty the spawn danger 0 to 5 (eggs pass 0 to 3)
     * @param gunExists  filter for weapons whose item is registered
     * @param camoCount  the camo count of a set id, to bound a drawn camo
     * @return the gear
     */
    public Gear roll(Random rand, int difficulty, Predicate<String> gunExists, ToIntFunction<String> camoCount) {
        String weapon = null;
        int total = 0;
        List<Weapon> pool = new ArrayList<>();
        for (Weapon w : weapons) {
            if (difficulty >= w.minDifficulty && gunExists.test(w.gun)) {
                pool.add(w);
                total += w.weight;
            }
        }
        if (total > 0) {
            int r = rand.nextInt(total);
            for (Weapon w : pool) {
                r -= w.weight;
                if (r < 0) {
                    weapon = w.gun;
                    break;
                }
            }
        }
        int shared = sharedCamoCount > 0 ? rand.nextInt(sharedCamoCount) : 0;
        List<ArmorPick> picks = new ArrayList<>();
        for (ArmorRule rule : armor) {
            if (rule.percent < 100 && rand.nextInt(100) >= rule.percent) {
                continue;
            }
            String set = rule.sets.size() == 1 ? rule.sets.get(0) : rule.sets.get(rand.nextInt(rule.sets.size()));
            int camo;
            int at = set.indexOf('@');
            if (at >= 0) {
                // "set@N" pins the camo of this choice (a helmet that is either a Combat helmet or a green beret).
                camo = Integer.parseInt(set.substring(at + 1));
                set = set.substring(0, at);
            } else if (rule.camoMode == Builder.CAMO_SHARED) {
                camo = shared;
            } else if (rule.camoMode == Builder.CAMO_RANDOM) {
                camo = 1 + rand.nextInt(Math.max(1, Math.min(rule.camoArg, camoCount.applyAsInt(set) - 1)));
            } else {
                camo = rule.camoArg;
            }
            picks.add(new ArmorPick(rule.slot, set, camo));
        }
        return new Gear(weapon, picks);
    }

    /** Builds a profile. */
    public static final class Builder {

        static final int CAMO_FIXED = 0;
        static final int CAMO_SHARED = 1;
        static final int CAMO_RANDOM = 2;

        private final List<Weapon> weapons = new ArrayList<>();
        private final List<ArmorRule> armor = new ArrayList<>();
        private int sharedCamoCount;

        /**
         * @param gun    gun registry name
         * @param weight pool weight
         * @return this
         */
        public Builder weapon(String gun, int weight) {
            return weapon(gun, weight, 0);
        }

        /**
         * @param gun           gun registry name
         * @param weight        pool weight
         * @param minDifficulty difficulty at which the gun joins the pool
         * @return this
         */
        public Builder weapon(String gun, int weight, int minDifficulty) {
            weapons.add(new Weapon(gun, weight, minDifficulty));
            return this;
        }

        /**
         * Sets how many camos the shared draw spans; one camo in {@code 0..count-1} is drawn per roll for every rule
         * built with {@link #sharedCamo}.
         *
         * @param count the camo count
         * @return this
         */
        public Builder sharedCamoOf(int count) {
            sharedCamoCount = count;
            return this;
        }

        /**
         * @param slot    0 helmet to 3 boots
         * @param percent chance the slot is filled
         * @param camo    the fixed camo index
         * @param sets    set ids, one picked with equal chance
         * @return this
         */
        public Builder armor(int slot, int percent, int camo, String... sets) {
            armor.add(new ArmorRule(slot, percent, Arrays.asList(sets), CAMO_FIXED, camo));
            return this;
        }

        /**
         * Armor whose camo is the roll's shared draw.
         *
         * @param slot    0 helmet to 3 boots
         * @param percent chance the slot is filled
         * @param set     the set id
         * @return this
         */
        public Builder sharedCamo(int slot, int percent, String set) {
            armor.add(new ArmorRule(slot, percent, Collections.singletonList(set), CAMO_SHARED, 0));
            return this;
        }

        /**
         * Armor whose camo is drawn per piece from {@code 1..maxCamo}.
         *
         * @param slot    0 helmet to 3 boots
         * @param percent chance the slot is filled
         * @param maxCamo the largest camo index
         * @param set     the set id
         * @return this
         */
        public Builder randomCamo(int slot, int percent, int maxCamo, String set) {
            armor.add(new ArmorRule(slot, percent, Collections.singletonList(set), CAMO_RANDOM, maxCamo));
            return this;
        }

        /** @return the profile */
        public EquipmentProfile build() {
            return new EquipmentProfile(new ArrayList<>(weapons), new ArrayList<>(armor), sharedCamoCount);
        }
    }
}
