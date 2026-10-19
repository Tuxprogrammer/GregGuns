package com.gregguns.mob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

/** The animation math: each type's formula, evaluated on small models and on the demo. */
class MobAnimatorTest {

    private static final float EPS = 1.0e-5F;

    /** Parts "p" (with the given rest rotation) and "q", plus the given animation JSON entries. */
    private static MobModelDef model(String restRotate, String... anims) {
        String json = "{\"name\":\"t\",\"textureWidth\":16,\"textureHeight\":16,\"parts\":["
            + "{\"name\":\"p\",\"rotate\":"
            + restRotate
            + "},{\"name\":\"q\"}],\"animations\":["
            + String.join(",", anims)
            + "]}";
        try {
            return MobModelDef.parse("t", new StringReader(json));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static float[] eval(MobModelDef def, AnimInput in) {
        float[][] out = new float[def.parts()
            .size()][3];
        MobAnimator.evaluate(def, in, out);
        return out[def.indexOf("p")];
    }

    private static float rad(double deg) {
        return (float) Math.toRadians(deg);
    }

    @Test
    void swingAtZeroLimbSwingEqualsTheAmplitude() {
        MobModelDef def = model("[0,0,0]", "{\"type\":\"swing\",\"part\":\"p\",\"axis\":\"x\",\"amplitude\":80}");
        AnimInput in = new AnimInput();
        in.limbSwing = 0.0F;
        in.limbSwingAmount = 1.0F;
        assertEquals(rad(80), eval(def, in)[0], EPS);
    }

    @Test
    void swingScalesWithAmountAndFollowsTheCosine() {
        MobModelDef def = model(
            "[0,0,0]",
            "{\"type\":\"swing\",\"part\":\"p\",\"axis\":\"z\",\"amplitude\":50,\"speed\":2,\"phase\":30}");
        AnimInput in = new AnimInput();
        in.limbSwing = 0.7F;
        in.limbSwingAmount = 0.5F;
        float expected = rad(50) * 0.5F * (float) Math.cos(0.7 * 2 + Math.toRadians(30));
        assertEquals(expected, eval(def, in)[2], EPS);
        in.limbSwingAmount = 0.0F;
        assertEquals(0.0F, eval(def, in)[2], EPS);
    }

    @Test
    void swingDefaultsToTheVanillaSpeed() {
        MobModelDef def = model("[0,0,0]", "{\"type\":\"swing\",\"part\":\"p\",\"axis\":\"x\",\"amplitude\":80}");
        AnimInput in = new AnimInput();
        in.limbSwing = 1.0F;
        in.limbSwingAmount = 1.0F;
        assertEquals(rad(80) * (float) Math.cos(0.6662), eval(def, in)[0], EPS);
    }

    @Test
    void swingAddsToTheRestRotation() {
        MobModelDef def = model("[10,20,30]", "{\"type\":\"swing\",\"part\":\"p\",\"axis\":\"y\",\"amplitude\":5}");
        AnimInput in = new AnimInput();
        in.limbSwingAmount = 1.0F;
        float[] r = eval(def, in);
        assertEquals(rad(10), r[0], EPS);
        assertEquals(rad(25), r[1], EPS);
        assertEquals(rad(30), r[2], EPS);
    }

    @Test
    void idleIsOffsetPlusSineOfAge() {
        MobModelDef def = model(
            "[0,0,0]",
            "{\"type\":\"idle\",\"part\":\"p\",\"axis\":\"x\",\"amplitude\":10,\"speed\":0.2,\"phase\":90,\"offset\":4}");
        AnimInput in = new AnimInput();
        in.ageInTicks = 5.0F;
        assertEquals(rad(4) + rad(10) * (float) Math.sin(5 * 0.2 + Math.PI / 2), eval(def, in)[0], EPS);
        in.ageInTicks = 0.0F;
        assertEquals(rad(4) + rad(10), eval(def, in)[0], EPS); // sin(90 degrees) = 1
    }

    @Test
    void spinIsConstantRatePerTick() {
        MobModelDef def = model("[0,0,0]", "{\"type\":\"spin\",\"part\":\"p\",\"axis\":\"y\",\"rate\":24}");
        AnimInput in = new AnimInput();
        in.ageInTicks = 7.5F;
        assertEquals(rad(24 * 7.5), eval(def, in)[1], EPS);
    }

    @Test
    void lookFollowsYawAndPitchWithinLimits() {
        MobModelDef def = model(
            "[0,0,0]",
            "{\"type\":\"look\",\"part\":\"p\",\"maxYaw\":40,\"maxPitch\":30,\"pitch\":0.5}");
        AnimInput in = new AnimInput();
        in.netHeadYaw = 25.0F;
        in.headPitch = 20.0F;
        float[] r = eval(def, in);
        assertEquals(rad(25), r[1], EPS);
        assertEquals(rad(10), r[0], EPS);
        in.netHeadYaw = -90.0F;
        in.headPitch = 80.0F;
        r = eval(def, in);
        assertEquals(rad(-40), r[1], EPS);
        assertEquals(rad(15), r[0], EPS);
    }

    @Test
    void aimingPoseSetsAnglesOnlyWhileAiming() {
        MobModelDef def = model(
            "[0,0,0]",
            "{\"type\":\"swing\",\"part\":\"p\",\"axis\":\"x\",\"amplitude\":50}",
            "{\"type\":\"pose\",\"part\":\"p\",\"when\":\"aiming\",\"mode\":\"set\",\"angles\":{\"x\":-90},\"followPitch\":1}");
        AnimInput in = new AnimInput();
        in.limbSwingAmount = 1.0F;
        in.headPitch = 12.0F;
        assertEquals(rad(50), eval(def, in)[0], EPS); // not aiming: the swing stands
        in.aiming = true;
        assertEquals(rad(-90 + 12), eval(def, in)[0], EPS); // aiming: replaced, plus the head pitch
    }

    @Test
    void addPoseAddsAndLeavesOtherAxesAlone() {
        MobModelDef def = model(
            "[1,2,3]",
            "{\"type\":\"pose\",\"part\":\"p\",\"when\":\"holding\",\"angles\":{\"x\":-18}}");
        AnimInput in = new AnimInput();
        in.holding = true;
        float[] r = eval(def, in);
        assertEquals(rad(-17), r[0], EPS);
        assertEquals(rad(2), r[1], EPS);
        assertEquals(rad(3), r[2], EPS);
    }

    @Test
    void attackingPoseUsesItsBlendAndWindow() {
        String sine = "{\"type\":\"pose\",\"part\":\"p\",\"when\":\"attacking\",\"angles\":{\"x\":100},\"blend\":\"sine\"}";
        MobModelDef def = model("[0,0,0]", sine);
        AnimInput in = new AnimInput();
        in.attackProgress = 0.5F;
        assertEquals(rad(100), eval(def, in)[0], EPS); // sin(pi/2) = 1
        in.attackProgress = 0.0F;
        assertEquals(0.0F, eval(def, in)[0], EPS);
        in.attackProgress = 1.0F;
        assertEquals(0.0F, eval(def, in)[0], 1.0e-4F); // sin(pi) = 0
        String cosine = "{\"type\":\"pose\",\"part\":\"p\",\"when\":\"attacking\",\"angles\":{\"x\":100},"
            + "\"blend\":\"cosine\",\"window\":[0.5,1.0]}";
        MobModelDef windowed = model("[0,0,0]", cosine);
        in.attackProgress = 0.25F; // before the window
        assertEquals(0.0F, eval(windowed, in)[0], EPS);
        in.attackProgress = 0.75F; // halfway through the window: 0.5 * (1 - cos(pi/2)) = 0.5
        assertEquals(rad(50), eval(windowed, in)[0], EPS);
        in.attackProgress = 1.0F; // end of the window: fully eased in
        assertEquals(rad(100), eval(windowed, in)[0], EPS);
    }

    @Test
    void demoWalkMatchesVanillaBipedAmplitudes() {
        MobModelDef def = MobModels.load("demo_biped");
        AnimInput in = new AnimInput();
        in.limbSwing = 0.0F;
        in.limbSwingAmount = 1.0F;
        float[][] out = new float[def.parts()
            .size()][3];
        MobAnimator.evaluate(def, in, out);
        assertEquals(1.4F, out[def.indexOf("rightLeg")][0], 1.0e-3F);
        assertEquals(-1.4F, out[def.indexOf("leftLeg")][0], 1.0e-3F);
        assertEquals(-1.0F, out[def.indexOf("rightArm")][0], 1.0e-3F);
        assertEquals(1.0F, out[def.indexOf("leftArm")][0], 1.0e-3F);
        // Standing still: only the idle sway moves the arms, a few degrees.
        in.limbSwingAmount = 0.0F;
        MobAnimator.evaluate(def, in, out);
        assertEquals(0.0F, out[def.indexOf("rightLeg")][0], EPS);
        assertEquals(0.0F, out[def.indexOf("rightArm")][0], 1.0e-2F);
    }

    @Test
    void demoAimedArmsPointForwardAndFollowThePitch() {
        MobModelDef def = MobModels.load("demo_biped");
        AnimInput in = new AnimInput();
        in.aiming = true;
        in.headPitch = 10.0F;
        float[][] out = new float[def.parts()
            .size()][3];
        MobAnimator.evaluate(def, in, out);
        assertEquals(rad(-80), out[def.indexOf("rightArm")][0], EPS);
        assertEquals(rad(-80), out[def.indexOf("leftArm")][0], EPS);
    }

    @Test
    void spinRateIsUnchangedAtDeathProgressZeroAndScaledAboveIt() {
        MobModelDef def = model(
            "[0,0,0]",
            "{\"type\":\"spin\",\"part\":\"p\",\"axis\":\"y\",\"rate\":-24,\"deathScale\":0.5}");
        AnimInput in = new AnimInput();
        in.ageInTicks = 10.0F;
        assertEquals(rad(-240), eval(def, in)[1], EPS);
        in.deathProgress = 0.01F;
        assertEquals(rad(-120), eval(def, in)[1], EPS);
        in.deathProgress = 1.0F;
        assertEquals(rad(-120), eval(def, in)[1], EPS);
    }

    @Test
    void spinWithoutDeathScaleIgnoresDeath() {
        MobModelDef def = model("[0,0,0]", "{\"type\":\"spin\",\"part\":\"p\",\"axis\":\"y\",\"rate\":-24}");
        AnimInput in = new AnimInput();
        in.ageInTicks = 10.0F;
        in.deathProgress = 0.7F;
        assertEquals(rad(-240), eval(def, in)[1], EPS);
    }

    @Test
    void dyingSetPoseBlendsTheRestPitchToTheTargetByDeathProgress() {
        MobModelDef def = model(
            "[25,0,0]",
            "{\"type\":\"pose\",\"part\":\"p\",\"when\":\"dying\",\"mode\":\"set\"," + "\"angles\":{\"x\":0}}");
        AnimInput in = new AnimInput();
        assertEquals(rad(25), eval(def, in)[0], EPS);
        in.deathProgress = 0.5F;
        assertEquals(rad(12.5), eval(def, in)[0], EPS);
        in.deathProgress = 1.0F;
        assertEquals(0.0F, eval(def, in)[0], EPS);
        in.deathProgress = 3.0F; // clamped to 1
        assertEquals(0.0F, eval(def, in)[0], EPS);
    }

    @Test
    void defaultMobAnimSourceHasNoDeathProgress() {
        MobAnimSource src = new MobAnimSource() {

            @Override
            public boolean isAiming() {
                return false;
            }

            @Override
            public float attackProgress(float partialTicks) {
                return -1.0F;
            }
        };
        assertEquals(0.0F, src.deathProgress(0.5F), 0.0F);
    }
}
