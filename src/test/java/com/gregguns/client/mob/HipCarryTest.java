package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.mob.model.AnimInput;
import com.gregguns.mob.model.MobAnimator;
import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobModels;

/** The hip-carry pose math and its agreement with the shipped model files. */
class HipCarryTest {

    @Test
    void gunPointsForwardAndSlightlyDown() {
        float down = HipCarry.muzzleDownDegrees();
        assertTrue(down > 0.0F && down <= 30.0F, "muzzle should point forward and slightly down: " + down);
    }

    @Test
    void appliesOnlyToAnArmedMobThatIsNotAiming() {
        assertTrue(HipCarry.applies(true, false));
        assertFalse(HipCarry.applies(true, true));
        assertFalse(HipCarry.applies(false, false));
    }

    @Test
    void modelFilesUseTheSameArmAngleAndNoSwingWhileCarrying() {
        for (String name : new String[] { "biped", "skeleton_soldier", "super_mutant" }) {
            MobModelDef def = MobModels.load(name);
            AnimInput in = new AnimInput();
            in.holding = true;
            in.carrying = true;
            in.limbSwingAmount = 1.0F;
            for (float swing : new float[] { 0.0F, 0.9F, 2.1F }) {
                in.limbSwing = swing;
                float[][] out = new float[def.parts()
                    .size()][3];
                MobAnimator.evaluate(def, in, out);
                float x = out[def.indexOf("rightArm")][0];
                assertEquals((float) Math.toRadians(HipCarry.ARM_X_DEGREES), x, 1.0e-4F, name + " swing " + swing);
            }
        }
    }
}
