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

    /** Plays the pose back at blend 1 the way the renderer's GL calls do, on a point in eye space. */
    private static double[] aimed(AdsPose pose, double[] eyePoint) {
        InHandFit.Affine item = InHandFit.firstPersonItemFrame();
        double[] p = item.invert()
            .apply(eyePoint);
        double[] pivot = pose.pivot();
        double[] axis = pose.axis();
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            r[i] = p[i] - pivot[i];
        }
        double c = Math.cos(Math.toRadians(pose.angleDegrees()));
        double s = Math.sin(Math.toRadians(pose.angleDegrees()));
        double dot = axis[0] * r[0] + axis[1] * r[1] + axis[2] * r[2];
        double[] cross = { axis[1] * r[2] - axis[2] * r[1], axis[2] * r[0] - axis[0] * r[2],
            axis[0] * r[1] - axis[1] * r[0] };
        double[] shift = pose.shift();
        double[] q = new double[3];
        for (int i = 0; i < 3; i++) {
            q[i] = pivot[i] + r[i] * c + cross[i] * s + axis[i] * dot * (1 - c) + shift[i];
        }
        return item.apply(q);
    }

    @Test
    void theSightsLandOnTheScreenCenterAtTheirOwnDepthWithTheBarrelStraightAhead() {
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            AdsPose pose = AdsTable.poseOf(def);
            assertNotNull(pose, spec.gunName());
            BoxSpec[] boxes = def.boxes();
            double[] point = AdsOffset.sightPoint(boxes, def.modelScale(), spec);
            int part = AdsOffset.sightPart(boxes, spec);
            double[] before = AdsOffset.eyeOf(def.transforms(), part, point, def.xFamily());
            double[] moved = aimed(pose, before);
            assertEquals(0.0, moved[0], 1e-9, spec.gunName() + " x");
            assertEquals(0.0, moved[1], 1e-9, spec.gunName() + " y");
            assertEquals(before[2], moved[2], 1e-9, spec.gunName() + " depth is unchanged");
            // A point one texel further along the barrel lies straight ahead of the sights: no sideways or vertical
            // drift, so the gun runs down the line of sight.
            double[] aheadBefore = AdsOffset.eyeOf(
                def.transforms(),
                part,
                new double[] { point[0] + 1.0, point[1], point[2] },
                def.xFamily());
            double[] ahead = aimed(pose, aheadBefore);
            assertEquals(0.0, ahead[0], 1e-9, spec.gunName() + " barrel x");
            assertEquals(0.0, ahead[1], 1e-9, spec.gunName() + " barrel y");
            assertTrue(ahead[2] < moved[2], spec.gunName() + " the muzzle is further from the eye than the sights");
        }
    }

    @Test
    void theQuarterTurnOfAZFamilyModelIsPartOfTheSightPosition() {
        // Regression: the Golden Gun (and the 9mm Pistol) sat left of center because the sights were placed in the
        // model's authored axes while the renderer turns a Z-family model a quarter turn about Y.
        ClientGunDefinition def = ClientGunDefinitions.byName("golden_gun");
        assertTrue(!def.xFamily());
        AdsSpec spec = AdsTable.of("golden_gun");
        double[] point = AdsOffset.sightPoint(def.boxes(), def.modelScale(), spec);
        int part = AdsOffset.sightPart(def.boxes(), spec);
        double[] turned = AdsOffset.eyeOf(def.transforms(), part, point, false);
        double[] authored = AdsOffset.eyeOf(def.transforms(), part, point, true);
        assertTrue(Math.abs(turned[0] - authored[0]) + Math.abs(turned[2] - authored[2]) > 0.05);
    }

    @Test
    void theLmgIsTurnedToLieAlongTheViewNotDraggedAcrossIt() {
        // Regression: translating only, the slanted LMG's buttstock came across the camera.
        AdsPose pose = AdsTable.poseOf(ClientGunDefinitions.byName("lmg"));
        assertTrue(pose.angleDegrees() > 5.0, "the LMG needs a turn, got " + pose.angleDegrees());
    }

    @Test
    void theSightsStartInFrontOfTheCameraAndTheShiftIsModest() {
        for (AdsSpec spec : AdsTable.ALL) {
            ClientGunDefinition def = ClientGunDefinitions.byName(spec.gunName());
            double[] point = AdsOffset.sightPoint(def.boxes(), def.modelScale(), spec);
            double[] eye = AdsOffset.eyeOf(def.transforms(), AdsOffset.sightPart(def.boxes(), spec), point, def.xFamily());
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
        assertNull(AdsTable.poseOf(ClientGunDefinitions.byName("bolt_action_rifle")));
        assertNull(AdsTable.poseOf(ClientGunDefinitions.byName("akimbo_pistols")));
    }
}
