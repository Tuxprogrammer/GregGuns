package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.gregguns.entity.npc.NpcMob;

/**
 * The kinds of GregGuns spawner: which layout mark each one answers, which NPCs its vanilla mob spawner spawns, and its
 * {@link SpawnerSettings}. Worldgen writes a {@code minecraft:mob_spawner} for each mark (see {@link VanillaSpawners}).
 * Pure: no Minecraft types, so the tables are unit-tested.
 *
 * <p>
 * The zombie spawner of the Big Bunker spawns the three zombie NPCs and the bug spawner of the Alien Bug Nest spawns
 * the Alien Bug (PLAN-M8 G11). The two silo posts spawn the ICBM complex guards (D36).
 *
 * <p>
 * Usage: {@code SpawnerKind.byLayoutTag("spawner.zombie_hole")}, then
 * {@code kind.registered(NpcRegistry::isRegistered)}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks), PLAN-M8 G11
public enum SpawnerKind {

    /** Big Bunker spawner: the three zombie NPCs, weighted evenly. */
    ZOMBIE("zombie_hole", NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER),
    /** Alien Bug Nest spawner: the Alien Bug only. */
    BUG("bug_hole", NpcMob.ALIEN_BUG),
    /** Silo guard post (ICBM complexes, D36): the Army Soldier and the Commando. */
    SILO_GUARD("silo_guard", NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO),
    /** Silo elite post (ICBM complexes, D36): the Commando and, deeper in, the Stormtrooper. */
    SILO_ELITE("silo_elite", NpcMob.COMMANDO, NpcMob.STORMTROOPER);

    /** Prefix of every spawner layout mark. */
    public static final String TAG_PREFIX = "spawner.";

    private final String markName;
    private final List<NpcMob> mobs;

    SpawnerKind(String markName, NpcMob... mobs) {
        this.markName = markName;
        this.mobs = Collections.unmodifiableList(Arrays.asList(mobs));
    }

    /** @return the bare mark name, for example {@code zombie_hole} */
    public String markName() {
        return markName;
    }

    /** @return true for the silo posts of ICBM complexes (D36) */
    public boolean isSiloPost() {
        return this == SILO_GUARD || this == SILO_ELITE;
    }

    /** @return the layout mark tag, for example {@code spawner.zombie_hole} */
    public String layoutTag() {
        return TAG_PREFIX + markName;
    }

    /** @return the settings written into this kind's spawners */
    public SpawnerSettings settings() {
        return SpawnerSettings.STANDARD;
    }

    /** @return the NPC kinds this spawner may spawn, in a fixed order */
    public List<NpcMob> mobs() {
        return mobs;
    }

    /** @return the entity names of {@link #mobs()}, as {@code EntityList} knows them */
    public List<String> mobNames() {
        List<String> names = new ArrayList<>();
        for (NpcMob m : mobs) {
            names.add(m.fullName());
        }
        return names;
    }

    /**
     * A spawner must never name an entity that does not exist this run: the vanilla client renderer would fail on it.
     *
     * @param registered whether an NPC kind is bound and registered this run
     * @return the registered kinds of this spawner, in {@link #mobs()} order; empty when none is (no spawner is placed)
     */
    public List<NpcMob> registered(Predicate<NpcMob> registered) {
        List<NpcMob> open = new ArrayList<>();
        for (NpcMob m : mobs) {
            if (registered.test(m)) {
                open.add(m);
            }
        }
        return open;
    }

    /**
     * @param tag a layout mark tag; may be null
     * @return the kind that tag names, or null (any other spawner tag is ignored)
     */
    public static SpawnerKind byLayoutTag(String tag) {
        for (SpawnerKind k : values()) {
            if (k.layoutTag()
                .equals(tag)) {
                return k;
            }
        }
        return null;
    }
}
