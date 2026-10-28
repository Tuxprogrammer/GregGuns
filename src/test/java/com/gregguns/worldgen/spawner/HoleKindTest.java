package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcMob;

/** Which NPCs each hole places and how layout tags map to kinds. */
class HoleKindTest {

    @Test
    void zombieHolePlacesTheThreeZombieNpcs() {
        assertEquals(
            Arrays.asList(NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER),
            HoleKind.ZOMBIE.mobs());
    }

    @Test
    void bugHolePlacesOnlyTheAlienBug() {
        assertEquals(Arrays.asList(NpcMob.ALIEN_BUG), HoleKind.BUG.mobs());
        assertEquals(Arrays.asList("gregguns.AlienBug"), HoleKind.BUG.mobNames());
    }

    @Test
    void layoutTagsMapToKinds() {
        assertSame(HoleKind.ZOMBIE, HoleKind.byLayoutTag("spawner.zombie_hole"));
        assertSame(HoleKind.BUG, HoleKind.byLayoutTag("spawner.bug_hole"));
        assertNull(HoleKind.byLayoutTag("spawner.other"));
        assertNull(HoleKind.byLayoutTag(null));
        for (HoleKind k : HoleKind.values()) {
            assertSame(k, HoleKind.byLayoutTag(k.layoutTag()));
            assertSame(k, HoleKind.byName(k.name()));
        }
        assertNull(HoleKind.byName("nope"));
    }

    @Test
    void pickSkipsUnregisteredKinds() {
        Random rand = new Random(3);
        for (int i = 0; i < 50; i++) {
            NpcMob m = HoleKind.ZOMBIE.pick(mob -> mob != NpcMob.ZOMBIE_MINER, rand);
            assertNotNull(m);
            assertEquals(false, m == NpcMob.ZOMBIE_MINER);
        }
        assertNull(HoleKind.BUG.pick(mob -> false, rand));
        assertSame(NpcMob.ALIEN_BUG, HoleKind.BUG.pick(mob -> true, rand));
    }

    @Test
    void blockNamesAreDistinct() {
        assertEquals("zombie_hole", HoleKind.ZOMBIE.blockName());
        assertEquals("bug_hole", HoleKind.BUG.blockName());
    }

    @Test
    void siloPostsPlaceTheComplexGuards() {
        assertEquals(Arrays.asList(NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO), HoleKind.SILO_GUARD.mobs());
        assertEquals(Arrays.asList(NpcMob.COMMANDO, NpcMob.STORMTROOPER), HoleKind.SILO_ELITE.mobs());
        assertSame(HoleKind.SILO_GUARD, HoleKind.byLayoutTag("spawner.silo_guard"));
        assertSame(HoleKind.SILO_ELITE, HoleKind.byLayoutTag("spawner.silo_elite"));
        assertEquals(true, HoleKind.SILO_GUARD.isSiloPost());
        assertEquals(false, HoleKind.ZOMBIE.isSiloPost());
        assertEquals("zombie_hole", HoleKind.ZOMBIE.textureName());
        assertEquals("concrete_hole", HoleKind.SILO_GUARD.textureName());
    }
}
