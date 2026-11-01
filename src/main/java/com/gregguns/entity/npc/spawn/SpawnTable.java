package com.gregguns.entity.npc.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.NpcMob;

/**
 * The natural-spawn table: which mobs may spawn naturally in which {@link SpawnRealm}, from which danger level, at what
 * weight and in what group size. Built from a {@link Settings} snapshot (so tests need no config) and a "is this mob
 * bound to a class yet" predicate (so a mob that is not implemented is skipped, never a crash). A mob with a disabled
 * flag or weight 0 has no entry, so it is neither registered with the spawner nor selectable (NPC-14, NPC-15). Pure.
 *
 * <p>
 * The Overworld rows are Techguns' with the weights halved (design 0005). The Nether and End rows are GregGuns' own
 * (D353): the Zombie Pigman Soldier and the three Super Mutants by danger, the Cyberdemon as a rare boss, and Commando
 * squads in the End.
 *
 * <p>
 * Usage: {@code SpawnTable t = SpawnTable.build(Settings.fromConfig(), NpcRegistry::isRegistered);
 * t.allows(NpcMob.BANDIT, SpawnRealm.OVERWORLD, danger)}. Both sides; the server uses it.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (1.4 spawn tables), docs/gregguns-spec/10-entities-npcs/
// spawning.md (GGS-10-24, GGS-10-26, GGS-10-41)
// Deviation: Techguns spawned the Zombie Pigman Soldier and the Cyberdemon in the Nether at its own weights and the
// Super Mutants and Commandos nowhere; GregGuns' Nether and End tables are D353's.
public final class SpawnTable {

    /** Group size of every natural spawn unless its row says otherwise. */
    public static final int GROUP_MIN = 1;
    /** Largest group of every natural spawn unless its row says otherwise. */
    public static final int GROUP_MAX = 2;

    /** One natural-spawn entry. */
    public static final class Entry {

        private final NpcMob mob;
        private final SpawnRealm realm;
        private final int level;
        private final int weight;
        private final int groupMin;
        private final int groupMax;
        private final boolean boss;

        Entry(NpcMob mob, SpawnRealm realm, int level, int weight, int groupMin, int groupMax, boolean boss) {
            this.mob = mob;
            this.realm = realm;
            this.level = level;
            this.weight = weight;
            this.groupMin = groupMin;
            this.groupMax = groupMax;
            this.boss = boss;
        }

        /** @return the mob */
        public NpcMob mob() {
            return mob;
        }

        /** @return the realm whose biomes and dimensions it spawns in */
        public SpawnRealm realm() {
            return realm;
        }

        /** @return the lowest danger level at which it may spawn */
        public int level() {
            return level;
        }

        /** @return the spawn weight, always above 0 */
        public int weight() {
            return weight;
        }

        /** @return the smallest group the vanilla spawner places */
        public int groupMin() {
            return groupMin;
        }

        /** @return the largest group the vanilla spawner places */
        public int groupMax() {
            return groupMax;
        }

        /** @return true for a boss: none spawns while another of its kind is within the boss spacing */
        public boolean boss() {
            return boss;
        }
    }

    /** A snapshot of the config the table reads. */
    public static final class Settings {

        public boolean zombies = true;
        public boolean military = true;
        public boolean bandits = true;
        public boolean miniBosses = true;
        public boolean mutants = true;
        public boolean cyberdemon = true;
        public int weightZombieFarmer = 100;
        public int weightZombieMiner = 100;
        public int weightZombieSoldier = 50;
        public int weightSkeletonSoldier = 50;
        public int weightBandit = 25;
        public int weightPsychoSteve = 1;
        public int weightNetherPigmanSoldier = 15;
        public int weightNetherMutantBasic = 6;
        public int weightNetherMutantHeavy = 3;
        public int weightNetherMutantElite = 2;
        public int weightNetherCyberdemon = 1;
        public int weightEndCommando = 1;

        /** @return a snapshot of {@link NpcConfig} */
        public static Settings fromConfig() {
            Settings s = new Settings();
            s.zombies = NpcConfig.zombies;
            s.military = NpcConfig.military;
            s.bandits = NpcConfig.bandits;
            s.miniBosses = NpcConfig.miniBosses;
            s.mutants = NpcConfig.mutants;
            s.cyberdemon = NpcConfig.cyberdemon;
            s.weightZombieFarmer = NpcConfig.weightZombieFarmer;
            s.weightZombieMiner = NpcConfig.weightZombieMiner;
            s.weightZombieSoldier = NpcConfig.weightZombieSoldier;
            s.weightSkeletonSoldier = NpcConfig.weightSkeletonSoldier;
            s.weightBandit = NpcConfig.weightBandit;
            s.weightPsychoSteve = NpcConfig.weightPsychoSteve;
            s.weightNetherPigmanSoldier = NpcConfig.weightNetherPigmanSoldier;
            s.weightNetherMutantBasic = NpcConfig.weightNetherMutantBasic;
            s.weightNetherMutantHeavy = NpcConfig.weightNetherMutantHeavy;
            s.weightNetherMutantElite = NpcConfig.weightNetherMutantElite;
            s.weightNetherCyberdemon = NpcConfig.weightNetherCyberdemon;
            s.weightEndCommando = NpcConfig.weightEndCommando;
            return s;
        }
    }

    /** Nether danger from which Super Mutant Basics appear: about 62 Nether blocks out with the default distances. */
    public static final int NETHER_LEVEL_MUTANT_BASIC = 2;
    /** Nether danger of the Super Mutant Heavy: about 125 Nether blocks out. */
    public static final int NETHER_LEVEL_MUTANT_HEAVY = 3;
    /** Nether danger of the Super Mutant Elite and the Cyberdemon: about 312 Nether blocks out. */
    public static final int NETHER_LEVEL_DEEP = 4;
    /** Largest Commando squad in the End. */
    public static final int END_SQUAD_MAX = 3;

    private final List<Entry> entries;

    private SpawnTable(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * @param s     the settings
     * @param bound true for a mob whose class is registered
     * @return the table: the Overworld rows in Techguns order (danger 0 first), then the Nether rows, then the End
     */
    public static SpawnTable build(Settings s, Predicate<NpcMob> bound) {
        List<Entry> out = new ArrayList<>();
        SpawnRealm ow = SpawnRealm.OVERWORLD;
        add(out, NpcMob.ZOMBIE_FARMER, ow, 0, s.zombies, s.weightZombieFarmer, bound);
        add(out, NpcMob.ZOMBIE_MINER, ow, 0, s.zombies, s.weightZombieMiner, bound);
        add(out, NpcMob.ZOMBIE_SOLDIER, ow, 1, s.zombies, s.weightZombieSoldier, bound);
        add(out, NpcMob.SKELETON_SOLDIER, ow, 1, s.military, s.weightSkeletonSoldier, bound);
        add(out, NpcMob.PSYCHO_STEVE, ow, 1, s.miniBosses, s.weightPsychoSteve, bound);
        add(out, NpcMob.BANDIT, ow, 2, s.bandits, s.weightBandit, bound);
        SpawnRealm nether = SpawnRealm.NETHER;
        // The Nether danger is never below 1, so the Pigman Soldier is open everywhere there.
        add(out, NpcMob.ZOMBIE_PIGMAN_SOLDIER, nether, 0, s.zombies, s.weightNetherPigmanSoldier, bound);
        add(
            out,
            NpcMob.SUPER_MUTANT_BASIC,
            nether,
            NETHER_LEVEL_MUTANT_BASIC,
            s.mutants,
            s.weightNetherMutantBasic,
            bound);
        add(
            out,
            NpcMob.SUPER_MUTANT_HEAVY,
            nether,
            NETHER_LEVEL_MUTANT_HEAVY,
            s.mutants,
            s.weightNetherMutantHeavy,
            bound);
        add(out, NpcMob.SUPER_MUTANT_ELITE, nether, NETHER_LEVEL_DEEP, s.mutants, s.weightNetherMutantElite, bound);
        if (s.cyberdemon && s.weightNetherCyberdemon > 0 && bound.test(NpcMob.CYBER_DEMON)) {
            out.add(new Entry(NpcMob.CYBER_DEMON, nether, NETHER_LEVEL_DEEP, s.weightNetherCyberdemon, 1, 1, true));
        }
        if (s.military && s.weightEndCommando > 0 && bound.test(NpcMob.COMMANDO)) {
            out.add(new Entry(NpcMob.COMMANDO, SpawnRealm.END, 0, s.weightEndCommando, 2, END_SQUAD_MAX, false));
        }
        return new SpawnTable(Collections.unmodifiableList(out));
    }

    private static void add(List<Entry> out, NpcMob mob, SpawnRealm realm, int level, boolean flag, int weight,
        Predicate<NpcMob> bound) {
        if (flag && weight > 0 && bound.test(mob)) {
            out.add(new Entry(mob, realm, level, weight, GROUP_MIN, GROUP_MAX, false));
        }
    }

    /**
     * @return every natural-spawn entry of every realm (the ones to register with the spawner)
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * @param realm a realm
     * @return its entries, in table order
     */
    public List<Entry> entries(SpawnRealm realm) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.realm == realm) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * @param mob   a kind
     * @param realm a realm
     * @return its entry in that realm, or null if it does not spawn naturally there
     */
    public Entry entry(NpcMob mob, SpawnRealm realm) {
        for (Entry e : entries) {
            if (e.mob == mob && e.realm == realm) {
                return e;
            }
        }
        return null;
    }

    /**
     * @param mob a kind
     * @return its Overworld entry, or null
     */
    public Entry entry(NpcMob mob) {
        return entry(mob, SpawnRealm.OVERWORLD);
    }

    /**
     * @param realm  a realm
     * @param danger the spawn danger, 0 to 5
     * @return the entries of that realm open at that danger: tables 0 through {@code danger}
     */
    public List<Entry> open(SpawnRealm realm, int danger) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.realm == realm && e.level <= danger) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * @param danger the spawn danger, 0 to 5
     * @return the Overworld entries open at that danger
     */
    public List<Entry> open(int danger) {
        return open(SpawnRealm.OVERWORLD, danger);
    }

    /**
     * @param mob    a kind
     * @param realm  a realm
     * @param danger the spawn danger
     * @return true if the mob has an entry in that realm open at that danger
     */
    public boolean allows(NpcMob mob, SpawnRealm realm, int danger) {
        Entry e = entry(mob, realm);
        return e != null && e.level <= danger;
    }

    /**
     * @param mob    a kind
     * @param danger the spawn danger
     * @return true if the mob has an Overworld entry open at that danger
     */
    public boolean allows(NpcMob mob, int danger) {
        return allows(mob, SpawnRealm.OVERWORLD, danger);
    }

    /**
     * Draws one mob from the entries of a realm open at a danger, by weight.
     *
     * @param realm  the realm
     * @param danger the spawn danger
     * @param rnd    the random source
     * @return the mob, or null when nothing is open
     */
    public NpcMob pick(SpawnRealm realm, int danger, Random rnd) {
        List<Entry> open = open(realm, danger);
        int[] w = new int[open.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = open.get(i).weight;
        }
        int i = WeightedPick.pick(w, rnd);
        return i < 0 ? null : open.get(i).mob;
    }

    /**
     * @param danger the spawn danger
     * @param rnd    the random source
     * @return an Overworld mob drawn by weight, or null when nothing is open
     */
    public NpcMob pick(int danger, Random rnd) {
        return pick(SpawnRealm.OVERWORLD, danger, rnd);
    }
}
