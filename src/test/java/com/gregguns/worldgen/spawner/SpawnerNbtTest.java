package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.spawn.SpawnerData;

/** The vanilla mob spawner NBT GregGuns writes: settings, even potentials and the GregGuns spawn data. */
class SpawnerNbtTest {

    private static final List<String> GUARDS = Arrays.asList("gregguns.ArmySoldier", "gregguns.Commando");

    @Test
    void writesEveryVanillaFieldFromTheSettings() {
        NBTTagCompound t = SpawnerNbt.build(GUARDS, 0, SpawnerSettings.STANDARD, 3, 10, 64, -20);
        assertEquals("MobSpawner", t.getString("id"));
        assertEquals(10, t.getInteger("x"));
        assertEquals(64, t.getInteger("y"));
        assertEquals(-20, t.getInteger("z"));
        assertEquals(40, t.getShort("Delay"));
        assertEquals(100, t.getShort("MinSpawnDelay"));
        assertEquals(120, t.getShort("MaxSpawnDelay"));
        assertEquals(3, t.getShort("SpawnCount"));
        assertEquals(5, t.getShort("MaxNearbyEntities"));
        assertEquals(24, t.getShort("RequiredPlayerRange"));
        assertEquals(4, t.getShort("SpawnRange"));
        // Vanilla reads these with hasKey(name, 99): any number type; we write shorts as vanilla does.
        assertEquals(
            2,
            t.getTag("SpawnRange")
                .getId());
    }

    @Test
    void potentialsAreEvenAndAllMarked() {
        NBTTagCompound t = SpawnerNbt.build(GUARDS, 0, SpawnerSettings.STANDARD, 3, 10, 64, -20);
        NBTTagList list = t.getTagList("SpawnPotentials", 10);
        assertEquals(2, list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            assertEquals(GUARDS.get(i), e.getString("Type"));
            assertEquals(1, e.getInteger("Weight"));
            assertEquals(3, SpawnerData.minDanger(e.getCompoundTag("Properties")));
            assertTrue(SpawnerData.atOrigin(e.getCompoundTag("Properties"), 10, 64, -20));
        }
        assertEquals(GUARDS, SpawnerNbt.entities(t));
        assertTrue(SpawnerNbt.isMarked(t));
        assertEquals(3, SpawnerNbt.minDanger(t));
        assertTrue(SpawnerData.atOrigin(t.getCompoundTag("SpawnData"), 10, 64, -20));
    }

    @Test
    void entityIdAndSpawnDataNameTheFirstEntry() {
        assertEquals(
            "gregguns.ArmySoldier",
            SpawnerNbt.build(GUARDS, 0, SpawnerSettings.STANDARD, 0, 10, 64, -20)
                .getString("EntityId"));
        assertEquals(
            "gregguns.Commando",
            SpawnerNbt.build(GUARDS, 1, SpawnerSettings.STANDARD, 0, 10, 64, -20)
                .getString("EntityId"));
        assertEquals(
            "gregguns.ArmySoldier",
            SpawnerNbt.build(GUARDS, 6, SpawnerSettings.STANDARD, 0, 10, 64, -20)
                .getString("EntityId"));
        assertEquals(
            "gregguns.Commando",
            SpawnerNbt.build(GUARDS, -1, SpawnerSettings.STANDARD, 0, 10, 64, -20)
                .getString("EntityId"));
        assertTrue(
            SpawnerData.isFromSpawner(
                SpawnerNbt.build(GUARDS, 0, SpawnerSettings.STANDARD, 0, 10, 64, -20)
                    .getCompoundTag("SpawnData")));
    }

    @Test
    void aPlainSpawnerIsNotMarked() {
        NBTTagCompound plain = new NBTTagCompound();
        plain.setString("EntityId", "Zombie");
        assertFalse(SpawnerNbt.isMarked(plain));
        assertEquals(Collections.emptyList(), SpawnerNbt.entities(plain));
        assertEquals(0, SpawnerNbt.minDanger(plain));
    }

    @Test
    void noEntityIsRefused() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SpawnerNbt.build(Collections.<String>emptyList(), 0, SpawnerSettings.STANDARD, 0, 10, 64, -20));
    }
}
