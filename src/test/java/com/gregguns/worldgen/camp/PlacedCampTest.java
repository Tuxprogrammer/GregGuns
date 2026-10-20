package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.FlagPos;

/** Rotation of the flag, spawn points and rectangles, and the cap formula (WGEN-10, WGEN-11). */
class PlacedCampTest {

    private static final int AX = 1000;
    private static final int AY = 70;
    private static final int AZ = -500;

    /** The documented table, written out by hand: offset of local (lx, lz) from the anchor. */
    private static int[] expectedOffset(int rot, int lx, int lz, int sx, int sz) {
        switch (rot) {
            case 0:
                return new int[] { lx, lz };
            case 1:
                return new int[] { sz - 1 - lz, lx };
            case 2:
                return new int[] { sx - 1 - lx, sz - 1 - lz };
            default:
                return new int[] { lz, sx - 1 - lx };
        }
    }

    @Test
    void flagIsRotatedWithTheFootprintForAllFourRotations() {
        for (long seed = 0; seed < 40; seed++) {
            CampPlan plan = CampPlan.generate(seed);
            for (int rot = 0; rot < 4; rot++) {
                PlacedCamp camp = plan.place(AX, AY, AZ, rot);
                int[] off = expectedOffset(rot, plan.flagX(), plan.flagZ(), plan.sizeX(), plan.sizeZ());
                FlagPos f = camp.flagPos();
                assertEquals(new FlagPos(AX + off[0], AY, AZ + off[1]), f, "seed " + seed + " rot " + rot);
                assertEquals(off[0], camp.flagOffsetX());
                assertEquals(off[1], camp.flagOffsetZ());
                assertEquals(0, camp.flagOffsetY());
                assertTrue(off[0] >= 0 && off[0] < camp.footprintX() && off[1] >= 0 && off[1] < camp.footprintZ());
                // And the flag really is at the middle of the rotated flag leaf.
                CampLeaf l = plan.flagLeaf();
                int[] box = camp.worldRect(l.x, l.z, l.w, l.d);
                assertTrue(f.x >= box[0] && f.x <= box[2] && f.z >= box[1] && f.z <= box[3]);
            }
        }
    }

    @Test
    void footprintSwapsForOddRotations() {
        CampPlan plan = CampPlan.generate(7, 40, 60);
        assertEquals(
            40,
            plan.place(0, 0, 0, 0)
                .footprintX());
        assertEquals(
            60,
            plan.place(0, 0, 0, 1)
                .footprintX());
        assertEquals(
            40,
            plan.place(0, 0, 0, 2)
                .footprintX());
        assertEquals(
            40,
            plan.place(0, 0, 0, 3)
                .footprintZ());
        assertEquals(
            60,
            plan.place(0, 0, 0, 3)
                .footprintX());
    }

    @Test
    void localAndWorldMapsAreInverses() {
        CampPlan plan = CampPlan.generate(9, 45, 33);
        for (int rot = 0; rot < 4; rot++) {
            PlacedCamp camp = plan.place(AX, AY, AZ, rot);
            for (int lx = 0; lx < plan.sizeX(); lx += 7) {
                for (int lz = 0; lz < plan.sizeZ(); lz += 5) {
                    int wx = camp.worldX(lx, lz);
                    int wz = camp.worldZ(lx, lz);
                    assertEquals(lx, camp.localX(wx, wz), "rot " + rot);
                    assertEquals(lz, camp.localZ(wx, wz), "rot " + rot);
                    assertTrue(wx >= AX && wx < AX + camp.footprintX() && wz >= AZ && wz < AZ + camp.footprintZ());
                }
            }
        }
    }

    @Test
    void spawnOffsetsRotateLikeTheFlag() {
        CampPlan plan = CampPlan.generate(11);
        int[] local = plan.spawnPoints();
        for (int rot = 0; rot < 4; rot++) {
            int[] out = plan.place(AX, AY, AZ, rot)
                .spawnOffsets();
            assertEquals(local.length, out.length);
            for (int i = 0; i < local.length; i += 3) {
                int[] off = expectedOffset(rot, local[i], local[i + 2], plan.sizeX(), plan.sizeZ());
                assertEquals(off[0], out[i]);
                assertEquals(local[i + 1], out[i + 1]);
                assertEquals(off[1], out[i + 2]);
            }
        }
    }

    @Test
    void flagSpecCarriesTheRotatedFootprintOffsetsNamesAndCap() {
        CampPlan plan = CampPlan.generate(13, 50, 34);
        PlacedCamp camp = plan.place(AX, AY, AZ, 1);
        FlagSpec spec = camp.flagSpec(0.75F);
        assertEquals(34, spec.sizeX);
        assertEquals(50, spec.sizeZ);
        assertEquals(camp.flagPos().x, spec.x);
        assertEquals(camp.flagPos().x - AX, spec.offsetX);
        assertEquals(camp.flagPos().z - AZ, spec.offsetZ);
        assertEquals(CampPlan.BASE_NPC_NAMES, spec.names());
        assertEquals(31, spec.cap, "floor(((50 + 34) / 2) * 0.75) = 31");
    }

    @Test
    void boundsCoverTheRoadTailsAndWindowsOverlapAccordingly() {
        PlacedCamp camp = CampPlan.generate(2, 40, 40)
            .place(0, 64, 0, 0);
        assertEquals(-3, camp.boundsMinX());
        assertEquals(42, camp.boundsMaxX());
        assertTrue(camp.overlaps(new com.gregguns.worldgen.PopulateWindow(0, 0)));
        assertTrue(!camp.overlaps(new com.gregguns.worldgen.PopulateWindow(10, 10)));
    }

    @Test
    void capFormula() {
        assertEquals(24, CampCap.cap(32, 32, 0.75F));
        assertEquals(59, CampCap.cap(79, 79, 0.75F));
        assertEquals(24, CampCap.cap(33, 32, 0.75F), "floor(32.5 * 0.75) = 24");
        assertEquals(30, CampCap.cap(50, 50, 0.6F));
        assertEquals(0, CampCap.cap(50, 50, 0F));
        assertEquals(
            24,
            CampPlan.generate(1, 32, 32)
                .cap(0.75F));
    }
}
