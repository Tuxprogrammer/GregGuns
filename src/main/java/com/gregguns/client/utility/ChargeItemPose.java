package com.gregguns.client.utility;

import com.gregguns.utility.charge.ChargeModel;
import com.gregguns.utility.charge.ChargeType;

/**
 * Where a charge sits relative to the origin the item renderer is given, as a pure table. The item renderer asks Forge
 * for the vanilla block pose in every render type (inventory, hand, dropped), which puts the origin at the center of
 * the item as a block-sized cube: spanning -0.5 to 0.5 in the inventory and dropped types, and 0 to 1 in the hand types
 * (Forge shifts the origin to the cube corner there). The charge model, though, is authored inside a cell (0 to 1,
 * hugging the north face), so the renderer moves the middle of the model's boxes onto the origin. Pure: no Minecraft
 * types.
 */
final class ChargeItemPose {

    /** The wall pose the item draws in: plate toward the viewer. */
    static final int ITEM_ORIENTATION = 3;

    private ChargeItemPose() {}

    /**
     * @param type which charge
     * @return the middle of the charge's boxes in the wall pose, in blocks (cell units), as {x, y, z}
     */
    static float[] center(ChargeType type) {
        float[] lo = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
        float[] hi = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
        for (ChargeModel.Box box : ChargeModel.boxes(type, ITEM_ORIENTATION)) {
            for (int a = 0; a < 3; a++) {
                lo[a] = Math.min(lo[a], box.bounds[a]);
                hi[a] = Math.max(hi[a], box.bounds[a + 3]);
            }
        }
        return new float[] { (lo[0] + hi[0]) / 32, (lo[1] + hi[1]) / 32, (lo[2] + hi[2]) / 32 };
    }

    /**
     * @param type       which charge
     * @param cellOrigin true for the hand types, where Forge's block grip puts the origin at the minimum corner of a
     *                   block-sized cell; false for the inventory and dropped types, where it is the cell's middle
     * @return the translation that centers the model on the cell's middle, in blocks, as {x, y, z}
     */
    static float[] translation(ChargeType type, boolean cellOrigin) {
        float[] c = center(type);
        float shift = cellOrigin ? 0.5F : 0F;
        return new float[] { shift - c[0], shift - c[1], shift - c[2] };
    }
}
