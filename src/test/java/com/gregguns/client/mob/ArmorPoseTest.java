package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.gregguns.client.armor.BoxArmorModel;

/** D310: armor drawn on an NPC takes the NPC's posed parts, and only while the NPC renders. */
class ArmorPoseTest {

    private static final float EPS = 1.0e-6F;

    @AfterEach
    void clearSource() {
        ArmorPose.end();
    }

    /** Poses a model the way the armor pass does, with a walk and a head turn. */
    private static void pose(ModelBiped m, float limbSwingAmount, float headYaw, float headPitch) {
        m.heldItemRight = 1;
        m.setRotationAngles(0.7F, limbSwingAmount, 12.0F, headYaw, headPitch, 0.0625F, null);
    }

    private static DataBipedModel carrying(String file, boolean aim) {
        DataBipedModel m = DataBipedModel.load(file);
        m.carryingGun = true;
        m.aimedBow = aim;
        return m;
    }

    private static void assertPart(String what, ModelRenderer want, ModelRenderer got) {
        assertEquals(want.rotateAngleX, got.rotateAngleX, EPS, what + " angle x");
        assertEquals(want.rotateAngleY, got.rotateAngleY, EPS, what + " angle y");
        assertEquals(want.rotateAngleZ, got.rotateAngleZ, EPS, what + " angle z");
        assertEquals(want.rotationPointX, got.rotationPointX, EPS, what + " pivot x");
        assertEquals(want.rotationPointY, got.rotationPointY, EPS, what + " pivot y");
        assertEquals(want.rotationPointZ, got.rotationPointZ, EPS, what + " pivot z");
    }

    private static void assertSamePose(ModelBiped want, ModelBiped got) {
        assertPart("head", want.bipedHead, got.bipedHead);
        assertPart("headwear", want.bipedHead, got.bipedHeadwear);
        assertPart("body", want.bipedBody, got.bipedBody);
        assertPart("right arm", want.bipedRightArm, got.bipedRightArm);
        assertPart("left arm", want.bipedLeftArm, got.bipedLeftArm);
        assertPart("right leg", want.bipedRightLeg, got.bipedRightLeg);
        assertPart("left leg", want.bipedLeftLeg, got.bipedLeftLeg);
    }

    @Test
    void hipCarryArmorTakesTheArmsAngleStandingAndWalking() {
        for (float walk : new float[] { 0.0F, 1.0F }) {
            DataBipedModel body = carrying("biped", false);
            pose(body, walk, 0.0F, 0.0F);
            ModelBiped armor = new ModelBiped(1.0F);
            pose(armor, walk, 0.0F, 0.0F);
            // The bug: vanilla's holding pose puts the sleeve somewhere else than the hip-carried arm.
            assertNotEquals(body.bipedRightArm.rotateAngleX, armor.bipedRightArm.rotateAngleX, 1.0e-3F);
            ArmorPose.copy(body, armor);
            assertSamePose(body, armor);
        }
    }

    @Test
    void aimedArmorTakesTheAimWithTheHeadTurn() {
        DataBipedModel body = carrying("biped", true);
        pose(body, 0.4F, 25.0F, -15.0F);
        ModelBiped armor = new ModelBiped(0.5F);
        armor.aimedBow = true;
        pose(armor, 0.4F, 25.0F, -15.0F);
        ArmorPose.copy(body, armor);
        assertSamePose(body, armor);
    }

    @Test
    void customArmorFollowsOnlyWhileAnNpcRenders() {
        DataBipedModel body = carrying("biped", false);
        pose(body, 1.0F, 10.0F, 5.0F);
        BoxArmorModel chest = BoxArmorModel.forPiece("t3_power", 1);
        assertNotNull(chest);
        ArmorPose.during(body, () -> pose(chest, 1.0F, 10.0F, 5.0F));
        assertSamePose(body, chest);

        // Outside an NPC render (a player wearing the same cached model) vanilla's pose stands.
        pose(chest, 1.0F, 10.0F, 5.0F);
        ModelBiped vanilla = new ModelBiped(0.0F);
        pose(vanilla, 1.0F, 10.0F, 5.0F);
        assertSamePose(vanilla, chest);
    }

    @Test
    void theSourceIsClearedWhenTheRenderThrows() {
        DataBipedModel body = carrying("biped", false);
        assertThrows(IllegalStateException.class, () -> ArmorPose.during(body, () -> {
            assertEquals(body, ArmorPose.source());
            throw new IllegalStateException("render failed");
        }));
        assertNull(ArmorPose.source());
    }

    @Test
    void theBodyIsNeverItsOwnFollower() {
        DataBipedModel body = carrying("biped", false);
        pose(body, 0.0F, 30.0F, 0.0F);
        ArmorPose.begin(body);
        ArmorPose.follow(body); // a no-op, not a self-copy that would loop or clobber the rig's headwear
        assertEquals(0.0F, body.bipedHeadwear.rotateAngleY, EPS);
    }

    @Test
    void skeletonLegPivotsAreCopied() {
        DataBipedModel body = carrying("skeleton_soldier", false);
        pose(body, 0.5F, 0.0F, 0.0F);
        ModelBiped armor = new ModelBiped(0.5F);
        pose(armor, 0.5F, 0.0F, 0.0F);
        // The skeleton's legs sit at +-2, vanilla's at +-1.9.
        assertNotEquals(body.bipedRightLeg.rotationPointX, armor.bipedRightLeg.rotationPointX, 1.0e-3F);
        ArmorPose.copy(body, armor);
        assertSamePose(body, armor);
    }
}
