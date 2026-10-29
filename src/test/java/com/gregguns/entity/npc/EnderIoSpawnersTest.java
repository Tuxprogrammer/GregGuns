package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.spawner.SpawnerKind;

/** D273: which NPCs GregGuns asks EnderIO to keep off its Broken Spawner drop and break burst. */
class EnderIoSpawnersTest {

    @Test
    void withoutEnderIoNothingIsSent() {
        assertTrue(
            EnderIoSpawners.blacklist(false, true, EnumSet.allOf(NpcMob.class))
                .isEmpty());
    }

    @Test
    void theCompatSwitchTurnsItOff() {
        assertTrue(
            EnderIoSpawners.blacklist(true, false, EnumSet.allOf(NpcMob.class))
                .isEmpty());
    }

    @Test
    void everySpawnerMobIsBlacklisted() {
        List<String> names = EnderIoSpawners.blacklist(true, true, EnumSet.allOf(NpcMob.class));
        for (SpawnerKind kind : SpawnerKind.values()) {
            for (String name : kind.mobNames()) {
                assertTrue(names.contains(name), kind + " mob " + name + " is not blacklisted");
            }
        }
    }

    @Test
    void oneFullNamePerRegisteredNpcInOrder() {
        List<String> names = EnderIoSpawners
            .blacklist(true, true, Arrays.asList(NpcMob.COMMANDO, NpcMob.ALIEN_BUG, NpcMob.ZOMBIE_SOLDIER));
        assertEquals(
            Arrays.asList(NpcMob.COMMANDO.fullName(), NpcMob.ALIEN_BUG.fullName(), NpcMob.ZOMBIE_SOLDIER.fullName()),
            names);
        assertTrue(
            names.get(0)
                .startsWith("gregguns."));
    }

    @Test
    void theImcKeyMatchesEnderIo() {
        assertEquals("EnderIO", EnderIoSpawners.MOD_ID);
        assertEquals("poweredSpawner:blacklist:add", EnderIoSpawners.BLACKLIST_ADD);
    }
}
