package com.gregguns.client.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.charge.ChargeModel;
import com.gregguns.utility.charge.ChargeType;

/** The item pose centers each charge on the renderer's origin: no left offset, the same middle as a block. */
class ChargeItemPoseTest {

    @Test
    void modelIsCenteredAcrossAndUpInTheCell() {
        for (ChargeType type : ChargeType.values()) {
            float[] c = ChargeItemPose.center(type);
            assertEquals(0.5F, c[0], 1e-4F, type + " x");
            assertEquals(0.5F, c[1], 1e-4F, type + " y");
            assertTrue(c[2] > 0F && c[2] < 0.5F, type + " z sits on the wall side");
        }
    }

    @Test
    void translationMovesTheBoxMiddleToTheOrigin() {
        for (ChargeType type : ChargeType.values()) {
            float[] t = ChargeItemPose.translation(type, false);
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            for (ChargeModel.Box box : ChargeModel.boxes(type, ChargeItemPose.ITEM_ORIENTATION)) {
                lo = Math.min(lo, box.bounds[0] / 16 + t[0]);
                hi = Math.max(hi, box.bounds[3] / 16 + t[0]);
            }
            assertEquals(-lo, hi, 1e-4F, type + " is symmetric about x = 0");
        }
    }

    @Test
    void chargeFitsInsideABlockCube() {
        for (ChargeType type : ChargeType.values()) {
            float[] t = ChargeItemPose.translation(type, false);
            for (ChargeModel.Box box : ChargeModel.boxes(type, ChargeItemPose.ITEM_ORIENTATION)) {
                for (int a = 0; a < 3; a++) {
                    assertTrue(box.bounds[a] / 16 + t[a] >= -0.5F - 1e-4F, type + " min axis " + a);
                    assertTrue(box.bounds[a + 3] / 16 + t[a] <= 0.5F + 1e-4F, type + " max axis " + a);
                }
            }
        }
    }

    @Test
    void handGripPutsTheModelInsideTheCellAtItsMiddle() {
        for (ChargeType type : ChargeType.values()) {
            float[] t = ChargeItemPose.translation(type, true);
            for (ChargeModel.Box box : ChargeModel.boxes(type, ChargeItemPose.ITEM_ORIENTATION)) {
                for (int a = 0; a < 3; a++) {
                    assertTrue(box.bounds[a] / 16 + t[a] >= -1e-4F, type + " min axis " + a);
                    assertTrue(box.bounds[a + 3] / 16 + t[a] <= 1F + 1e-4F, type + " max axis " + a);
                }
            }
            float[] c = ChargeItemPose.center(type);
            assertEquals(0.5F, c[0] + t[0], 1e-4F);
            assertEquals(0.5F, c[2] + t[2], 1e-4F);
        }
    }
}
