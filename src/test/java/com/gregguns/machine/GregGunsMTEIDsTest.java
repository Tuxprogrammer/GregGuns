package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GregGunsMTEIDsTest {

    @Test
    void everyIdIsInsideTheReservedBlockAndUnique() {
        // GGS-08-01 / GGS-08-48
        Set<Integer> seen = new HashSet<>();
        for (int id : GregGunsMTEIDs.all()) {
            assertTrue(id >= GregGunsMTEIDs.BLOCK_FIRST && id <= GregGunsMTEIDs.BLOCK_LAST, "id " + id);
            assertTrue(seen.add(id), "duplicate id " + id);
        }
    }

    @Test
    void theAmmoPressIdsAreTheRostersContiguousRun() {
        // machine-roster.md: 20100 Steam, 20101 LV, 20102 MV, 20103 HV, 20104 EV.
        assertEquals(20100, GregGunsMTEIDs.AMMO_PRESS_STEAM);
        assertEquals(20101, GregGunsMTEIDs.AMMO_PRESS_LV);
        assertEquals(20102, GregGunsMTEIDs.AMMO_PRESS_MV);
        assertEquals(20103, GregGunsMTEIDs.AMMO_PRESS_HV);
        assertEquals(20104, GregGunsMTEIDs.AMMO_PRESS_EV);
    }

    @Test
    void theAmmunitionWorksFollowsThePressRun() {
        // machine-roster.md: the Ammunition Works is 20105.
        assertEquals(20105, GregGunsMTEIDs.AMMO_WORKS);
        assertTrue(
            java.util.Arrays.stream(GregGunsMTEIDs.all())
                .anyMatch(id -> id == GregGunsMTEIDs.AMMO_WORKS));
    }

    @Test
    void theIcbmSubRangeIsReservedInsideTheBlockAndUnused() {
        assertTrue(GregGunsMTEIDs.ICBM_FIRST > GregGunsMTEIDs.BLOCK_FIRST);
        assertEquals(GregGunsMTEIDs.BLOCK_LAST, GregGunsMTEIDs.ICBM_LAST);
        for (int id : GregGunsMTEIDs.all()) {
            assertFalse(id >= GregGunsMTEIDs.ICBM_FIRST && id <= GregGunsMTEIDs.ICBM_LAST, "id " + id + " is ICBM's");
        }
        for (int id : GregGunsMTEIDs.retired()) {
            assertFalse(id >= GregGunsMTEIDs.ICBM_FIRST && id <= GregGunsMTEIDs.ICBM_LAST, "retired id " + id);
        }
    }

    @Test
    void theReservedBlockIs64Ids() {
        assertEquals(64, GregGunsMTEIDs.BLOCK_LAST - GregGunsMTEIDs.BLOCK_FIRST + 1);
    }

    @Test
    void theRetiredIdsAreReservedForeverAndAbsentFromEveryRegistration() throws java.io.IOException {
        // Design record 0012: 20106 and 20107 were the Reaction Chamber (EV, IV), 20108 the Retort. Worlds may still
        // hold
        // them, so they are never reused, never renumbered, and no registration may take one.
        assertArrayEquals(new int[] { 20106, 20107, 20108 }, GregGunsMTEIDs.retired());
        java.util.Set<Integer> retired = new HashSet<>();
        for (int id : GregGunsMTEIDs.retired()) {
            retired.add(id);
            assertTrue(id >= GregGunsMTEIDs.BLOCK_FIRST && id <= GregGunsMTEIDs.BLOCK_LAST, "id " + id);
        }
        for (int id : GregGunsMTEIDs.all()) {
            assertFalse(retired.contains(id), "all() lists the retired id " + id);
        }
        for (java.lang.reflect.Field f : GregGunsMTEIDs.class.getFields()) {
            if (f.getType() == int.class && !f.getName()
                .startsWith("BLOCK_")) {
                try {
                    assertFalse(retired.contains(f.getInt(null)), f.getName() + " takes a retired id");
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        // No source file may name a retired id as a number; only GregGunsMTEIDs holds them, and MtePlaceholders reads
        // them through retired() to register inert stand-ins (the only registration allowed to use them).
        java.util.regex.Pattern literal = java.util.regex.Pattern.compile("\b2010[678]\b");
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files
            .walk(java.nio.file.Paths.get("src", "main", "java"))) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) files.filter(
                x -> x.toString()
                    .endsWith(".java"))::iterator) {
                if (p.getFileName()
                    .toString()
                    .equals("GregGunsMTEIDs.java")) {
                    continue;
                }
                String text = new String(java.nio.file.Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8);
                assertFalse(
                    literal.matcher(text)
                        .find(),
                    p.getFileName() + " names a retired MTE id");
            }
        }
    }

    @Test
    void theRetiredIdsGetInertPlaceholdersAndNothingElse() {
        assertEquals(java.util.Arrays.asList(20106, 20107, 20108), MtePlaceholders.retiredMissing(i -> false));
        assertTrue(
            MtePlaceholders.retiredMissing(i -> true)
                .isEmpty());
        assertFalse(
            MtePlaceholders.missing(i -> false)
                .contains(20106));
    }

    @Test
    void theBuiltMachinesAreTheContiguousRunFromTheBlockStart() {
        // M6 utility core: every built or reserved-for-M6 machine is recorded, 20100 to 20124, so the pack-side
        // MetaTileEntityIDs rows (PACK-CHANGES.md) cover exactly these ids.
        int[] ids = GregGunsMTEIDs.all()
            .clone();
        java.util.Arrays.sort(ids);
        assertEquals(22, ids.length);
        int next = GregGunsMTEIDs.BLOCK_FIRST;
        for (int id : ids) {
            if (next >= 20106 && next <= 20108) {
                next = 20109;
            }
            assertEquals(next, id);
            next++;
        }
    }

    @Test
    void theUtilityMachinesUseTheRostersIds() {
        // machine-roster.md (GGS-08-48): Turret MV to IV 20116 to 20119, Rampart 20120, Gun Mount Hatch MV to IV
        // 20121 to 20124. Each tier run is contiguous in tier order.
        assertEquals(20116, GregGunsMTEIDs.TURRET_MV);
        assertEquals(GregGunsMTEIDs.TURRET_MV + 1, GregGunsMTEIDs.TURRET_HV);
        assertEquals(GregGunsMTEIDs.TURRET_HV + 1, GregGunsMTEIDs.TURRET_EV);
        assertEquals(GregGunsMTEIDs.TURRET_EV + 1, GregGunsMTEIDs.TURRET_IV);
        assertEquals(20120, GregGunsMTEIDs.RAMPART);
        assertEquals(20121, GregGunsMTEIDs.GUN_MOUNT_HATCH_MV);
        assertEquals(GregGunsMTEIDs.GUN_MOUNT_HATCH_MV + 1, GregGunsMTEIDs.GUN_MOUNT_HATCH_HV);
        assertEquals(GregGunsMTEIDs.GUN_MOUNT_HATCH_HV + 1, GregGunsMTEIDs.GUN_MOUNT_HATCH_EV);
        assertEquals(20124, GregGunsMTEIDs.GUN_MOUNT_HATCH_IV);
    }
}
