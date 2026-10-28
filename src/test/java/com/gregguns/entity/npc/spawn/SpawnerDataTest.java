package com.gregguns.entity.npc.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The GregGuns spawner key an NPC reads from vanilla SpawnData, its danger floor and the spawn box. */
class SpawnerDataTest {

    @Test
    void spawnDataRoundTripsTheFloor() {
        NBTTagCompound d = SpawnerData.spawnData(4);
        assertTrue(SpawnerData.isFromSpawner(d));
        assertEquals(4, SpawnerData.minDanger(d));
    }

    @Test
    void theFloorIsClampedToTheDangerRange() {
        assertEquals(5, SpawnerData.minDanger(SpawnerData.spawnData(9)));
        assertEquals(0, SpawnerData.minDanger(SpawnerData.spawnData(-2)));
    }

    @Test
    void anOrdinaryEntityTagIsNotFromASpawner() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("GGDanger", 3);
        assertFalse(SpawnerData.isFromSpawner(tag));
        assertFalse(SpawnerData.isFromSpawner(null));
        assertEquals(0, SpawnerData.minDanger(tag));
        tag.setString(SpawnerData.KEY, "not a compound");
        assertFalse(SpawnerData.isFromSpawner(tag));
    }

    @Test
    void theSpawnerFloorRaisesButNeverLowersThePlaceDanger() {
        assertEquals(0, SpawnerData.danger(0, 0));
        assertEquals(3, SpawnerData.danger(3, 0));
        assertEquals(5, SpawnerData.danger(0, 5));
        assertEquals(4, SpawnerData.danger(4, 2));
        assertEquals(5, SpawnerData.danger(7, 0));
    }

    @Test
    void spawnBoxMatchesVanillaSpread() {
        assertTrue(SpawnerData.inSpawnBox(0, 0, 0));
        assertTrue(SpawnerData.inSpawnBox(-3.99, -1, 3.99));
        assertTrue(SpawnerData.inSpawnBox(4, 1, -4));
        assertFalse(SpawnerData.inSpawnBox(4.01, 0, 0));
        assertFalse(SpawnerData.inSpawnBox(0, 2, 0));
        assertFalse(SpawnerData.inSpawnBox(0, -1.5, 0));
    }
}
