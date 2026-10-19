package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

class NpcGunProfileTest {

    @AfterEach
    void cleanUp() {
        NpcGunProfile.clearRegistered();
    }

    private static void expect(String gun, double range, int time, int burst, int delay) {
        NpcGunProfile p = NpcGunProfile.forGun(gun);
        assertNotNull(p, gun);
        assertEquals(new NpcGunProfile(range, time, burst, delay), p, gun);
    }

    /** The Techguns table (ai.md section 3) for the guns that exist in M2. */
    @Test
    void m2GunsMatchTheSpecTable() {
        expect("handcannon", 12, 60, 0, 0);
        expect("pistol_9mm", 24, 30, 3, 10);
        expect("sawed_off", 12, 60, 2, 20);
        expect("bolt_action_rifle", 36, 60, 0, 0);
        expect("m4", 24, 30, 3, 3);
        expect("ak47", 24, 30, 3, 3);
        expect("flamethrower", 12, 20, 5, 5);
        expect("combat_shotgun", 12, 30, 0, 0);
        expect("rocket_launcher", 24, 80, 0, 0);
        expect("thompson", 18, 45, 3, 3);
        expect("revolver", 18, 90, 6, 20);
        expect("as50", 36, 30, 0, 0);
        expect("lmg", 24, 40, 6, 3);
        expect("golden_revolver", 24, 60, 6, 15);
    }

    @Test
    void everyDefinedGunHasAProfile() {
        for (GunDefinition def : GunDefinitions.ALL) {
            assertNotNull(NpcGunProfile.forGun(def.name()), def.name());
        }
    }

    @Test
    void unknownGunFallsBackToDefault() {
        assertNull(NpcGunProfile.forGun("laser_gun"));
        assertSame(NpcGunProfile.DEFAULT, NpcGunProfile.forGunOrDefault("laser_gun"));
        assertEquals(new NpcGunProfile(15, 60, 0, 0), NpcGunProfile.DEFAULT);
    }

    @Test
    void laterGunsRegisterAndOverride() {
        NpcGunProfile laser = new NpcGunProfile(24, 30, 0, 0);
        NpcGunProfile.register("laser_gun", laser);
        assertSame(laser, NpcGunProfile.forGun("laser_gun"));
        NpcGunProfile tuned = new NpcGunProfile(20, 30, 0, 0);
        NpcGunProfile.register("m4", tuned);
        assertSame(tuned, NpcGunProfile.forGun("m4"));
    }

    @Test
    void varianceIsIntegerThird() {
        assertEquals(10, new NpcGunProfile(24, 30, 3, 3).variance());
        assertEquals(15, new NpcGunProfile(18, 45, 3, 3).variance());
        assertEquals(26, new NpcGunProfile(18, 80, 0, 0).variance());
    }

    @Test
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new NpcGunProfile(0, 30, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new NpcGunProfile(10, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new NpcGunProfile(10, 30, -1, 0));
    }
}
