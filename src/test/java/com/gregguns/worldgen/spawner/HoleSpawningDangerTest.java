package com.gregguns.worldgen.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** How a Ross128 structure's rolled ceiling raises the danger of its hole NPCs (design record 0019). */
class HoleSpawningDangerTest {

    @Test
    void anUnrolledHoleKeepsThePlaceDanger() {
        assertEquals(0, HoleSpawning.danger(0, 0));
        assertEquals(3, HoleSpawning.danger(3, 0));
    }

    @Test
    void aRolledHoleRaisesButNeverLowersTheDanger() {
        assertEquals(1, HoleSpawning.danger(0, 2), "MV ceiling lifts danger 0 to 1");
        assertEquals(5, HoleSpawning.danger(0, 9), "UHV ceiling");
        assertEquals(4, HoleSpawning.danger(4, 3), "a more dangerous place keeps its own danger");
    }
}
