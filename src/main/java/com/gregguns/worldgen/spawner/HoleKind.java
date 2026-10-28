package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.gregguns.entity.npc.NpcMob;

/**
 * The kinds of hole spawner and the NPCs each one may place. Pure: no Minecraft types, so the tables are
 * unit-tested. The Zombie Hole places the zombie NPCs and the Bug Hole places the Alien Bug (PLAN-M8 G11). The two silo
 * posts place the
 * ICBM complex guards (D36).
 *
 * <p>
 * Usage: {@code HoleKind.byLayoutTag("spawner.zombie_hole")}, then {@code kind.pick(NpcRegistry::isRegistered, rng)}.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks), PLAN-M8 G11
public enum HoleKind {

    /** Zombie Hole: the three zombie NPCs, picked evenly among the registered ones. */
    ZOMBIE("zombie_hole", NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER),
    /** Bug Hole: the Alien Bug only. It spawns only when the Alien Bug is registered (config {@code alienBug}). */
    BUG("bug_hole", NpcMob.ALIEN_BUG),
    /** Silo Guard Post (ICBM complexes, D36): the Army Soldier and the Commando. */
    SILO_GUARD("silo_guard", NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO),
    /** Silo Elite Post (ICBM complexes, D36): the Commando and, deeper in, the Stormtrooper. */
    SILO_ELITE("silo_elite", NpcMob.COMMANDO, NpcMob.STORMTROOPER);

    /** Prefix of every spawner layout mark. */
    public static final String TAG_PREFIX = "spawner.";

    private final String blockName;
    private final List<NpcMob> mobs;

    HoleKind(String blockName, NpcMob... mobs) {
        this.blockName = blockName;
        this.mobs = Collections.unmodifiableList(Arrays.asList(mobs));
    }

    /** @return the bare block name, also the lang and texture name, for example {@code zombie_hole} */
    public String blockName() {
        return blockName;
    }

    /**
     * @return the bare name of the block texture. The holes have their own; the two silo posts borrow existing
     *         concrete and hazard-panel tiles until a dedicated texture is approved (D47: generation is a last resort).
     */
    public String textureName() {
        switch (this) {
            case SILO_GUARD:
                return "concrete_hole";
            case SILO_ELITE:
                return "metalpanel_hazard";
            default:
                return blockName;
        }
    }

    /** @return true for the silo guard posts of ICBM complexes (D36) */
    public boolean isSiloPost() {
        return this == SILO_GUARD || this == SILO_ELITE;
    }

    /** @return the layout mark tag, for example {@code spawner.zombie_hole} */
    public String layoutTag() {
        return TAG_PREFIX + blockName;
    }

    /** @return the NPC kinds this hole may place, in a fixed order */
    public List<NpcMob> mobs() {
        return mobs;
    }

    /** @return the entity names of {@link #mobs()}, for counting nearby NPCs of this kind */
    public List<String> mobNames() {
        List<String> names = new ArrayList<>();
        for (NpcMob m : mobs) {
            names.add(m.fullName());
        }
        return names;
    }

    /**
     * @param tag a layout mark tag; may be null
     * @return the kind that tag names, or null (any other spawner tag is ignored)
     */
    public static HoleKind byLayoutTag(String tag) {
        for (HoleKind k : values()) {
            if (k.layoutTag()
                .equals(tag)) {
                return k;
            }
        }
        return null;
    }

    /**
     * @param name a saved kind name ({@link #name()}); may be null
     * @return the kind, or null when unknown
     */
    public static HoleKind byName(String name) {
        for (HoleKind k : values()) {
            if (k.name()
                .equals(name)) {
                return k;
            }
        }
        return null;
    }

    /**
     * @param registered whether an NPC kind is bound and registered this run
     * @param random     the random source
     * @return a registered kind of this hole, evenly chosen, or null when none is registered
     */
    public NpcMob pick(Predicate<NpcMob> registered, Random random) {
        List<NpcMob> open = new ArrayList<>();
        for (NpcMob m : mobs) {
            if (registered.test(m)) {
                open.add(m);
            }
        }
        return open.isEmpty() ? null : open.get(random.nextInt(open.size()));
    }
}
