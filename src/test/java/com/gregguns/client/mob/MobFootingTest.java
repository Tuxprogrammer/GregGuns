package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.mob.model.MobBoxDef;
import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobModels;
import com.gregguns.mob.model.MobPartDef;

/**
 * Feet on the ground: the lowest leg point is measured from the model geometry, and the renderer's transform chain
 * puts it on the floor at every scale. {@link #worldFootY} replays that chain (flip Y, the renderer's scale, the foot
 * lift, vanilla's -24 unit and 1/128 block translation) for one model point.
 */
class MobFootingTest {

    /** Vanilla's lift of every living model, in model space before the scale: 1/128 block. */
    private static final double VANILLA_LIFT = 0.0078125;
    private static final float[] MUTANT_SCALES = { 1.35F, 1.50F, 1.65F };

    /**
     * World Y, in blocks above the entity's feet, of a model point at Y = {@code modelY} model units, as
     * {@code RendererLivingEntity.doRender} places it: {@code glScalef(-1, -1, 1)}, then {@code preRenderCallback}
     * ({@code glScalef(scale)} and {@code glTranslatef(0, lift, 0)}), then {@code glTranslatef(0, -1.5 - 1/128, 0)}.
     */
    private static double worldFootY(float scale, float lift, float modelY) {
        return -scale * (lift - 1.5 - VANILLA_LIFT + modelY / 16.0);
    }

    @Test
    void superMutantLegsEndAtTheFootLine() {
        MobModelDef mutant = MobModels.load("super_mutant");
        // Leg pivot at Y = 11, boot 6 + 7 below it: the boot bottom is the foot line, Y = 24.
        assertEquals(24.0F, MobFooting.restFootY(mutant), 1.0e-4F);
        assertEquals(0.0F, MobFooting.groundingTranslation(MobFooting.restFootY(mutant)), 1.0e-6F);
    }

    @Test
    void everyTierStandsOnTheFloor() {
        float foot = MobFooting.restFootY(MobModels.load("super_mutant"));
        float lift = MobFooting.groundingTranslation(foot);
        for (float scale : MUTANT_SCALES) {
            // Vanilla's 1/128 block above the floor, scaled with the model, and nothing else.
            assertEquals(VANILLA_LIFT * scale, worldFootY(scale, lift, foot), 1.0e-5, "scale " + scale);
        }
    }

    @Test
    void theOldTierOffsetsSankTheMutantsByTheirOwnSize() {
        // The removed per-tier offsets (0.55, 0.75, 0.95 blocks, translated by offset / scale after the scale) came
        // from Techguns, which scales about the model origin; with the scale about the feet they sank each tier by
        // almost the whole offset. Kept as the measured cause of the bug report.
        float[] old = { 0.55F, 0.75F, 0.95F };
        for (int i = 0; i < old.length; i++) {
            float s = MUTANT_SCALES[i];
            double y = worldFootY(s, old[i] / s, 24.0F);
            assertEquals(-old[i] + VANILLA_LIFT * s, y, 1.0e-5);
            assertTrue(y < -0.5, "tier " + i + " feet at " + y);
        }
    }

    @Test
    void everyBipedModelGroundsAtEveryScale() {
        for (String name : Arrays.asList("biped", "cyberdemon", "demo_biped", "skeleton_soldier", "super_mutant")) {
            float foot = MobFooting.restFootY(MobModels.load(name));
            float lift = MobFooting.groundingTranslation(foot);
            for (float scale : new float[] { 0.5F, 1.0F, 1.65F, 3.0F }) {
                assertEquals(VANILLA_LIFT * scale, worldFootY(scale, lift, foot), 1.0e-5, name + " at " + scale);
            }
        }
    }

    @Test
    void shortLegsAreLoweredToTheFloor() {
        // Legs ending one unit above the foot line: lowered by 1/16 in the scaled space, on the floor at any scale.
        MobModelDef def = biped(
            leg("rightLeg", null, 11, 0, box(0, 12, 0)),
            leg("leftLeg", null, 11, 0, box(0, 12, 0)));
        assertEquals(23.0F, MobFooting.restFootY(def), 1.0e-4F);
        float lift = MobFooting.groundingTranslation(23.0F);
        assertEquals(1.0F / 16.0F, lift, 1.0e-6F);
        for (float scale : MUTANT_SCALES) {
            assertEquals(VANILLA_LIFT * scale, worldFootY(scale, lift, 23.0F), 1.0e-5);
        }
        assertEquals(-0.5F / 16.0F, MobFooting.groundingTranslation(24.5F), 1.0e-6F);
    }

    @Test
    void inflateRestRotationAndChildPartsCount() {
        // A child "boot" under the right leg reaches 1 unit lower than the leg, and its 0.5 inflate another 0.5.
        MobPartDef right = leg("rightLeg", null, 11, 0, box(0, 12, 0));
        MobPartDef boot = new MobPartDef(
            "bootR",
            "rightLeg",
            new float[] { 0, 6, 0 },
            new float[] { 0, 0, 0 },
            false,
            Collections.singletonList(
                new MobBoxDef("b", 0, 0, new float[] { -2, 0, -2 }, new int[] { 4, 7, 4 }, 0.5F, false, "x")));
        MobModelDef def = biped(right, leg("leftLeg", null, 11, 0, box(0, 12, 0)), boot);
        assertEquals(24.5F, MobFooting.restFootY(def), 1.0e-4F);
        // A leg tilted 90 degrees about X at rest lies flat: its lowest point is the box's depth edge, not its length.
        MobModelDef flat = biped(
            leg("rightLeg", null, 20, 90, box(0, 12, 0)),
            leg("leftLeg", null, 20, 90, box(0, 12, 0)));
        assertEquals(22.0F, MobFooting.restFootY(flat), 1.0e-3F);
    }

    @Test
    void aHangingPropOutsideTheLegsDoesNotLiftTheMob() {
        MobPartDef coat = new MobPartDef(
            "coat",
            "body",
            new float[] { 0, 0, 0 },
            new float[] { 0, 0, 0 },
            false,
            Collections.singletonList(
                new MobBoxDef("c", 0, 0, new float[] { -4, 0, -2 }, new int[] { 8, 30, 4 }, 0.0F, false, "x")));
        MobModelDef def = biped(
            leg("rightLeg", null, 12, 0, box(0, 12, 0)),
            leg("leftLeg", null, 12, 0, box(0, 12, 0)),
            coat);
        assertEquals(24.0F, MobFooting.restFootY(def), 1.0e-4F);
    }

    @Test
    void midStrideTheBootCornersDipLessThanAHalfUnit() {
        // The walk swings each leg about its hip; the boot's front and back corners then reach a little below the foot
        // line (a box corner on a circle), as on a vanilla biped. The worst case over the swing, measured from the
        // geometry: sqrt(13^2 + 3.2^2) - 13 = 0.39 units, 0.04 blocks at the Elite's 1.65 scale.
        MobModelDef mutant = MobModels.load("super_mutant");
        float worst = 0.0F;
        for (int deg = -60; deg <= 60; deg++) {
            worst = Math.max(worst, MobFooting.footY(mutant, deg) - 24.0F);
        }
        assertEquals(Math.sqrt(13 * 13 + 3.2 * 3.2) - 13, worst, 0.01);
        assertTrue(worst / 16.0F * 1.65F < 0.05F);
    }

    private static MobBoxDef box(float x, int height, float z) {
        return new MobBoxDef(
            "leg",
            0,
            0,
            new float[] { x - 2, 0, z - 2 },
            new int[] { 4, height, 4 },
            0.0F,
            false,
            "x");
    }

    private static MobPartDef leg(String name, String parent, float pivotY, float pitch, MobBoxDef box) {
        return new MobPartDef(
            name,
            parent,
            new float[] { name.startsWith("right") ? -2 : 2, pivotY, 0 },
            new float[] { pitch, 0, 0 },
            false,
            Collections.singletonList(box));
    }

    /** A biped with plain head, body and arms, the two given legs and any extra parts. */
    private static MobModelDef biped(MobPartDef right, MobPartDef left, MobPartDef... extra) {
        List<MobPartDef> parts = new ArrayList<>();
        parts.add(plain("head", 0));
        parts.add(plain("body", 0));
        parts.add(plain("rightArm", 2));
        parts.add(plain("leftArm", 2));
        parts.add(right);
        parts.add(left);
        parts.addAll(Arrays.asList(extra));
        return new MobModelDef("t", 64, 64, true, parts, Collections.emptyList());
    }

    private static MobPartDef plain(String name, float pivotY) {
        return new MobPartDef(
            name,
            null,
            new float[] { 0, pivotY, 0 },
            new float[] { 0, 0, 0 },
            false,
            Collections.singletonList(
                new MobBoxDef("b", 0, 0, new float[] { -1, 0, -1 }, new int[] { 2, 2, 2 }, 0.0F, false, "x")));
    }
}
