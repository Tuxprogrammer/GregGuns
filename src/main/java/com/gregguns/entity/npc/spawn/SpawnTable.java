package com.gregguns.entity.npc.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.NpcMob;

/**
 * The natural-spawn table: which mobs may spawn naturally, from which danger level, and at what weight. Built from a
 * {@link Settings} snapshot (so tests need no config) and a "is this mob bound to a class yet" predicate (so a mob
 * that is not implemented is skipped, never a crash). A mob with a disabled faction flag or weight 0 has no entry, so
 * it is neither registered with the spawner nor selectable (NPC-14, NPC-15). Pure.
 *
 * <p>
 * Usage: {@code SpawnTable t = SpawnTable.build(Settings.fromConfig(), NpcRegistry::isRegistered);
 * t.allows(NpcMob.BANDIT, danger)}. Both sides; the server uses it.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (1.4 spawn tables), docs/gregguns-spec/10-entities-npcs/
// spawning.md (GGS-10-24, GGS-10-26)
// Deviation: Nether-only mobs (Zombie Pigman Soldier, Cyberdemon) have no natural spawn (design 0005, plan M7 5.1).
public final class SpawnTable {

    /** One natural-spawn entry. */
    public static final class Entry {

        private final NpcMob mob;
        private final int level;
        private final int weight;

        Entry(NpcMob mob, int level, int weight) {
            this.mob = mob;
            this.level = level;
            this.weight = weight;
        }

        /** @return the mob */
        public NpcMob mob() {
            return mob;
        }

        /** @return the lowest danger level at which it may spawn */
        public int level() {
            return level;
        }

        /** @return the spawn weight, always above 0 */
        public int weight() {
            return weight;
        }
    }

    /** A snapshot of the config the table reads. */
    public static final class Settings {

        public boolean zombies = true;
        public boolean military = true;
        public boolean bandits = true;
        public boolean miniBosses = true;
        public int weightZombieFarmer = 100;
        public int weightZombieMiner = 100;
        public int weightZombieSoldier = 50;
        public int weightSkeletonSoldier = 50;
        public int weightBandit = 25;
        public int weightPsychoSteve = 1;

        /** @return a snapshot of {@link NpcConfig} */
        public static Settings fromConfig() {
            Settings s = new Settings();
            s.zombies = NpcConfig.zombies;
            s.military = NpcConfig.military;
            s.bandits = NpcConfig.bandits;
            s.miniBosses = NpcConfig.miniBosses;
            s.weightZombieFarmer = NpcConfig.weightZombieFarmer;
            s.weightZombieMiner = NpcConfig.weightZombieMiner;
            s.weightZombieSoldier = NpcConfig.weightZombieSoldier;
            s.weightSkeletonSoldier = NpcConfig.weightSkeletonSoldier;
            s.weightBandit = NpcConfig.weightBandit;
            s.weightPsychoSteve = NpcConfig.weightPsychoSteve;
            return s;
        }
    }

    private final List<Entry> entries;

    private SpawnTable(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * @param s     the settings
     * @param bound true for a mob whose class is registered
     * @return the table, in Techguns order (danger 0 first)
     */
    public static SpawnTable build(Settings s, Predicate<NpcMob> bound) {
        List<Entry> out = new ArrayList<>();
        add(out, NpcMob.ZOMBIE_FARMER, 0, s.zombies, s.weightZombieFarmer, bound);
        add(out, NpcMob.ZOMBIE_MINER, 0, s.zombies, s.weightZombieMiner, bound);
        add(out, NpcMob.ZOMBIE_SOLDIER, 1, s.zombies, s.weightZombieSoldier, bound);
        add(out, NpcMob.SKELETON_SOLDIER, 1, s.military, s.weightSkeletonSoldier, bound);
        add(out, NpcMob.PSYCHO_STEVE, 1, s.miniBosses, s.weightPsychoSteve, bound);
        add(out, NpcMob.BANDIT, 2, s.bandits, s.weightBandit, bound);
        return new SpawnTable(Collections.unmodifiableList(out));
    }

    private static void add(List<Entry> out, NpcMob mob, int level, boolean flag, int weight, Predicate<NpcMob> bound) {
        if (flag && weight > 0 && bound.test(mob)) {
            out.add(new Entry(mob, level, weight));
        }
    }

    /**
     * @return every natural-spawn entry (the ones to register with the spawner)
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * @param mob a kind
     * @return its entry, or null if it does not spawn naturally
     */
    public Entry entry(NpcMob mob) {
        for (Entry e : entries) {
            if (e.mob == mob) {
                return e;
            }
        }
        return null;
    }

    /**
     * @param danger the spawn danger, 0 to 5
     * @return the entries open at that danger: tables 0 through {@code danger}
     */
    public List<Entry> open(int danger) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.level <= danger) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * @param mob    a kind
     * @param danger the spawn danger
     * @return true if the mob has an entry open at that danger
     */
    public boolean allows(NpcMob mob, int danger) {
        Entry e = entry(mob);
        return e != null && e.level <= danger;
    }

    /**
     * Draws one mob from the entries open at a danger, by weight.
     *
     * @param danger the spawn danger
     * @param rnd    the random source
     * @return the mob, or null when nothing is open
     */
    public NpcMob pick(int danger, Random rnd) {
        List<Entry> open = open(danger);
        int[] w = new int[open.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = open.get(i).weight;
        }
        int i = WeightedPick.pick(w, rnd);
        return i < 0 ? null : open.get(i).mob;
    }
}
