package com.gregguns.entity.npc.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

/** The GregGuns spawner key an NPC reads from vanilla SpawnData, its danger floor, its origin and the spawn box. */
class SpawnerDataTest {

    @Test
    void spawnDataRoundTripsTheFloor() {
        NBTTagCompound d = SpawnerData.spawnData(4, 0, 64, 0);
        assertTrue(SpawnerData.isFromSpawner(d));
        assertEquals(4, SpawnerData.minDanger(d));
    }

    @Test
    void theFloorIsClampedToTheDangerRange() {
        assertEquals(5, SpawnerData.minDanger(SpawnerData.spawnData(9, 0, 64, 0)));
        assertEquals(0, SpawnerData.minDanger(SpawnerData.spawnData(-2, 0, 64, 0)));
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

    @Test
    void theOriginPinsTheSpawnerToItsBlock() {
        NBTTagCompound d = SpawnerData.spawnData(5, 100, 40, -7);
        assertTrue(SpawnerData.hasOrigin(d));
        assertTrue(SpawnerData.atOrigin(d, 100, 40, -7));
        // A dolly carries the NBT to another block: the origin no longer matches.
        assertFalse(SpawnerData.atOrigin(d, 101, 40, -7));
        assertFalse(SpawnerData.atOrigin(d, 100, 41, -7));
        assertFalse(SpawnerData.atOrigin(d, 100, 40, -6));
        assertFalse(SpawnerData.atOrigin(null, 100, 40, -7));
    }

    @Test
    void aKeyWithoutAnOriginIsNeverHonored() {
        NBTTagCompound inner = new NBTTagCompound();
        inner.setInteger(SpawnerData.MIN_DANGER, 5);
        inner.setInteger(SpawnerData.ORIGIN_X, 0);
        inner.setInteger(SpawnerData.ORIGIN_Y, 64);
        NBTTagCompound d = new NBTTagCompound();
        d.setTag(SpawnerData.KEY, inner);
        assertTrue(SpawnerData.isFromSpawner(d));
        assertFalse(SpawnerData.hasOrigin(d));
        assertFalse(SpawnerData.atOrigin(d, 0, 64, 0));
        assertFalse(SpawnerData.originNear(d, 0.5, 64, 0.5));
    }

    @Test
    void originNearUsesTheVanillaSpawnBox() {
        NBTTagCompound d = SpawnerData.spawnData(3, 10, 64, 10);
        assertTrue(SpawnerData.originNear(d, 13.9, 65, 6.1));
        assertFalse(SpawnerData.originNear(d, 14.5, 64, 10));
        assertFalse(SpawnerData.originNear(d, 10, 66, 10));
        // An NPC placed by a spawner 20 blocks from its origin is not near it.
        assertFalse(SpawnerData.originNear(d, 30.5, 64, 10.5));
    }
}
