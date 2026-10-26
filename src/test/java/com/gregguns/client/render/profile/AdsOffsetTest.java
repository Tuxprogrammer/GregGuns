package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.AdsSupport;

/**
 * The ADS table is pinned to the server's list, to the profiles' box names, and to the math that centers the sights.
 */
class AdsOffsetTest {

    @Test
    void tableMatchesTheServersList() {
        Set<String> table = new HashSet<>();
        for (AdsSpec spec : AdsTable.ALL) {
            assertTrue(table.add(spec.gunName()), "listed once: " + spec.gunName());
        }
        assertEquals(AdsSupport.GUNS, table);
    }

    @Test
    void everyListedGunHasAClientDefinitionWithItsSightBoxes() {
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            assertNotNull(def, spec.gunName());
            assertTrue(!def.akimbo(), spec.gunName());
            for (String box : spec.sightBoxes()) {
                boolean found = false;
                for (BoxSpec b : def.boxes()) {
                    found |= b.name()
                        .equals(box);
                }
                assertTrue(found, spec.gunName() + " has box " + box);
            }
        }
    }

    @Test
    void theSightsLandOnTheScreenCenterAtTheirOwnDepth() {
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            double[] shift = AdsTable.shiftOf(def);
            assertNotNull(shift, spec.gunName());
            BoxSpec[] boxes = def.boxes();
            double[] point = AdsOffset.sightPoint(boxes, def.modelScale(), spec);
            int part = AdsOffset.sightPart(boxes, spec);
            double[] before = AdsOffset.eyeOf(def.transforms(), part, point);
            // What the renderer does: translate in the item frame first, then the gun's rows.
            InHandFit.Affine item = InHandFit.firstPersonItemFrame();
            double[] inItem = item.invert()
                .apply(before);
            double[] moved = item
                .apply(new double[] { inItem[0] + shift[0], inItem[1] + shift[1], inItem[2] + shift[2] });
            assertEquals(0.0, moved[0], 1e-9, spec.gunName() + " x");
            assertEquals(0.0, moved[1], 1e-9, spec.gunName() + " y");
            assertEquals(before[2], moved[2], 1e-9, spec.gunName() + " depth is unchanged");
        }
    }

    @Test
    void theSightsStartInFrontOfTheCameraAndTheShiftIsModest() {
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            double[] point = AdsOffset.sightPoint(def.boxes(), def.modelScale(), spec);
            double[] eye = AdsOffset.eyeOf(def.transforms(), AdsOffset.sightPart(def.boxes(), spec), point);
            assertTrue(eye[2] < -0.1, spec.gunName() + " sights are ahead of the camera, z=" + eye[2]);
            // The gun starts in the lower right; it must not need to travel more than a screen's worth.
            assertTrue(Math.hypot(eye[0], eye[1]) < 1.5, spec.gunName() + " offset " + eye[0] + "," + eye[1]);
        }
    }

    @Test
    void sightPointIsTheCenterOfTheNamedBoxesPlusTheNudge() {
        BoxSpec a = new BoxSpec("a", 0, 0, 0, 2, 2, 2, 0f, 0f, 0f, new float[] { 0, 0, 0 }, 0f);
        BoxSpec b = new BoxSpec("b", 0, 0, 0, 2, 2, 2, 10f, 4f, 2f, new float[] { 0, 0, 0 }, 0f);
        AdsSpec spec = new AdsSpec("test", new double[] { 1, 0, 0 }, "a", "b");
        double[] p = AdsOffset.sightPoint(new BoxSpec[] { a, b }, 0.5f, spec);
        // union x 0..12, y 0..6, z 0..4: center 6,3,2 plus nudge 1 on x, times 0.5
        assertEquals(3.5, p[0], 1e-9);
        assertEquals(1.5, p[1], 1e-9);
        assertEquals(1.0, p[2], 1e-9);
    }

    @Test
    void anUnknownBoxNameIsReported() {
        BoxSpec a = new BoxSpec("a", 0, 0, 0, 2, 2, 2, 0f, 0f, 0f, new float[] { 0, 0, 0 }, 0f);
        AdsSpec spec = new AdsSpec("test", new double[3], "missing");
        assertThrows(IllegalArgumentException.class, () -> AdsOffset.sightPoint(new BoxSpec[] { a }, 1f, spec));
    }

    @Test
    void gunsOffTheListHaveNoShift() {
        assertNull(AdsTable.of("bolt_action_rifle"));
        assertNull(AdsTable.shiftOf(ClientGunDefinitions.byName("bolt_action_rifle")));
        assertNull(AdsTable.shiftOf(ClientGunDefinitions.byName("akimbo_pistols")));
    }
}
