package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The order the NPC spawn egg's subtypes sit in the creative tab and NEI. The egg is one item whose damage value is the
 * entity ID, and those IDs are world data, so only the list order changes, never an ID. The order reads as a roster:
 * the undead (zombies, then the skeleton), the Alien Bug, the human factions from the weakest (Bandit, Outcast) to the
 * best equipped (Army Soldier, Commando, Stormtrooper), the Super Mutants Basic to Elite, then the bosses, then the
 * machines (turret, helicopter), and the dev-only test NPC last. A mob missing from {@link #ROSTER} is appended in ID
 * order, so a new {@link NpcMob} still shows up; the unit test asks for it to be placed. Pure data.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/index.md (spawn egg order); owner request 2026-10-20
public final class NpcEggOrder {

    /** The roster, group by group. */
    static final List<NpcMob> ROSTER = Collections.unmodifiableList(
        Arrays.asList(
            NpcMob.ZOMBIE_SOLDIER,
            NpcMob.ZOMBIE_FARMER,
            NpcMob.ZOMBIE_MINER,
            NpcMob.ZOMBIE_PIGMAN_SOLDIER,
            NpcMob.SKELETON_SOLDIER,
            NpcMob.ALIEN_BUG,
            NpcMob.BANDIT,
            NpcMob.OUTCAST,
            NpcMob.ARMY_SOLDIER,
            NpcMob.COMMANDO,
            NpcMob.STORMTROOPER,
            NpcMob.SUPER_MUTANT_BASIC,
            NpcMob.SUPER_MUTANT_HEAVY,
            NpcMob.SUPER_MUTANT_ELITE,
            NpcMob.PSYCHO_STEVE,
            NpcMob.DICTATOR_DAVE,
            NpcMob.CYBER_DEMON,
            NpcMob.SIMPLE_TURRET,
            NpcMob.ATTACK_HELICOPTER,
            NpcMob.NPC_TEST));

    private NpcEggOrder() {}

    /** @return every {@link NpcMob} exactly once: the roster first, then any unlisted mob in ID order */
    public static List<NpcMob> all() {
        List<NpcMob> out = new ArrayList<>(ROSTER);
        for (NpcMob mob : NpcMob.values()) {
            if (!out.contains(mob)) {
                out.add(mob);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
