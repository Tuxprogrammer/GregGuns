package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.mob.model.MobModels;

/** The biped mode wires the file's parts into the six {@code ModelBiped} fields the vanilla renderer uses. */
class DataBipedModelTest {

    @Test
    void sixBipedFieldsAreTheFilesParts() {
        DataBipedModel m = DataBipedModel.load("demo_biped");
        assertNotNull(m.bipedHead);
        assertNotNull(m.bipedBody);
        assertNotNull(m.bipedRightArm);
        assertNotNull(m.bipedLeftArm);
        assertNotNull(m.bipedRightLeg);
        assertNotNull(m.bipedLeftLeg);
        assertEquals(1, m.bipedHead.cubeList.size());
        assertEquals(1, m.bipedBody.cubeList.size());
        assertEquals(-5.0F, m.bipedRightArm.rotationPointX, 0.0F);
        assertEquals(5.0F, m.bipedLeftArm.rotationPointX, 0.0F);
        assertEquals(12.0F, m.bipedRightLeg.rotationPointY, 0.0F);
        assertEquals(64, m.textureWidth);
        assertEquals(32, m.textureHeight);
        assertNotSame(m.bipedRightArm, m.bipedLeftArm);
        assertNotSame(m.bipedRightLeg, m.bipedLeftLeg);
        assertTrue(m.bipedHead instanceof RigRenderer);
    }

    @Test
    void visorIsAFullbrightChildOfTheHead() {
        DataBipedModel m = DataBipedModel.load("demo_biped");
        assertEquals(1, m.bipedHead.childModels.size());
        assertTrue(((RigRenderer) m.bipedHead.childModels.get(0)).glow);
        assertTrue(!((RigRenderer) m.bipedHead).glow);
    }

    @Test
    void setRotationAnglesPosesThePartsLikeVanilla() {
        DataBipedModel m = DataBipedModel.load("demo_biped");
        m.setRotationAngles(0.0F, 1.0F, 0.0F, 30.0F, -10.0F, 0.0625F, null);
        assertEquals(1.4F, m.bipedRightLeg.rotateAngleX, 1.0e-3F);
        assertEquals(-1.4F, m.bipedLeftLeg.rotateAngleX, 1.0e-3F);
        assertEquals((float) Math.toRadians(30), m.bipedHead.rotateAngleY, 1.0e-5F);
        assertEquals((float) Math.toRadians(-10), m.bipedHead.rotateAngleX, 1.0e-5F);
    }

    @Test
    void aimedBowRaisesTheArms() {
        DataBipedModel m = DataBipedModel.load("demo_biped");
        m.aimedBow = true;
        m.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, null);
        assertEquals((float) Math.toRadians(-90), m.bipedRightArm.rotateAngleX, 1.0e-5F);
        assertEquals((float) Math.toRadians(-90), m.bipedLeftArm.rotateAngleX, 1.0e-5F);
    }

    @Test
    void handOffsetIsStoredOnTheRightArm() {
        DataBipedModel m = DataBipedModel.load("demo_biped");
        m.setHandOffset(0.1F, 0.2F, 0.3F);
        RigRenderer arm = (RigRenderer) m.bipedRightArm;
        assertEquals(0.1F, arm.handX, 0.0F);
        assertEquals(0.2F, arm.handY, 0.0F);
        assertEquals(0.3F, arm.handZ, 0.0F);
    }

    @Test
    void genericModeBuildsTheSamePartsWithoutBipedFields() {
        DataMobModel m = new DataMobModel(MobModels.load("demo_biped"));
        assertEquals(
            7,
            m.def()
                .parts()
                .size());
    }
}
