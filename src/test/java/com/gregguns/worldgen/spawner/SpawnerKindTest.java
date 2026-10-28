package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** Which NPCs each spawner kind spawns and how layout tags map to kinds. */
class SpawnerKindTest {

    @Test
    void zombieSpawnerSpawnsTheThreeZombieNpcs() {
        assertEquals(
            Arrays.asList(NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER),
            SpawnerKind.ZOMBIE.mobs());
    }

    @Test
    void bugSpawnerSpawnsOnlyTheAlienBug() {
        assertEquals(Arrays.asList(NpcMob.ALIEN_BUG), SpawnerKind.BUG.mobs());
        assertEquals(Arrays.asList("gregguns.AlienBug"), SpawnerKind.BUG.mobNames());
    }

    @Test
    void layoutTagsMapToKinds() {
        assertSame(SpawnerKind.ZOMBIE, SpawnerKind.byLayoutTag("spawner.zombie_hole"));
        assertSame(SpawnerKind.BUG, SpawnerKind.byLayoutTag("spawner.bug_hole"));
        assertNull(SpawnerKind.byLayoutTag("spawner.other"));
        assertNull(SpawnerKind.byLayoutTag(null));
        for (SpawnerKind k : SpawnerKind.values()) {
            assertSame(k, SpawnerKind.byLayoutTag(k.layoutTag()));
            assertSame(SpawnerSettings.STANDARD, k.settings());
        }
    }

    @Test
    void registeredKeepsOrderAndSkipsUnregisteredKinds() {
        assertEquals(
            Arrays.asList(NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER),
            SpawnerKind.ZOMBIE.registered(mob -> mob != NpcMob.ZOMBIE_MINER));
        assertEquals(Collections.emptyList(), SpawnerKind.BUG.registered(mob -> false));
        assertEquals(SpawnerKind.SILO_ELITE.mobs(), SpawnerKind.SILO_ELITE.registered(mob -> true));
    }

    @Test
    void siloPostsSpawnTheComplexGuards() {
        assertEquals(Arrays.asList(NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO), SpawnerKind.SILO_GUARD.mobs());
        assertEquals(Arrays.asList(NpcMob.COMMANDO, NpcMob.STORMTROOPER), SpawnerKind.SILO_ELITE.mobs());
        assertSame(SpawnerKind.SILO_GUARD, SpawnerKind.byLayoutTag("spawner.silo_guard"));
        assertSame(SpawnerKind.SILO_ELITE, SpawnerKind.byLayoutTag("spawner.silo_elite"));
        assertTrue(SpawnerKind.SILO_GUARD.isSiloPost());
        assertEquals(false, SpawnerKind.ZOMBIE.isSiloPost());
    }
}
