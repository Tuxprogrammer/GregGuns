package com.gregguns.client.utility;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Builds a vanilla {@link ModelBase} from {@link TurretGeometry}: the entity-frame boxes only (the base block is drawn
 * by GT). The conversion from the geometry frame to vanilla model space is: the model's origin sits 24 pixels above
 * the feet with Y down, so a box's model y, relative to its part's pivot, is {@code pivot - y - h} and its model z is
 * {@code -(z + d)} (vanilla's front is -z).
 * Yaw parts stay still in the model: the renderer already turns the whole body by the head yaw. Pitch parts take the
 * entity's pitch. Client only.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (placeholder rendering)
@SideOnly(Side.CLIENT)
public final class TurretModel extends ModelBase {

    private static final float VANILLA_ORIGIN_Y = 24.0F;

    private final ModelRenderer body;
    private final ModelRenderer gunMount;

    /** Builds the parts from the box list. */
    public TurretModel() {
        textureWidth = TurretGeometry.TEXTURE_SIZE;
        textureHeight = TurretGeometry.TEXTURE_SIZE;
        body = new ModelRenderer(this, 0, 0);
        body.setRotationPoint(0.0F, VANILLA_ORIGIN_Y, 0.0F);
        gunMount = new ModelRenderer(this, 0, 0);
        // The pitch pivot, relative to the body's own rotation point at the feet (vanilla Y is down).
        gunMount.setRotationPoint(0.0F, -TurretGeometry.PITCH_PIVOT_Y, 0.0F);
        body.addChild(gunMount);
        for (TurretGeometry.Box box : TurretGeometry.BOXES) {
            if (box.part == TurretGeometry.Part.BASE) {
                continue;
            }
            boolean pitch = box.part == TurretGeometry.Part.PITCH;
            float pivot = pitch ? TurretGeometry.PITCH_PIVOT_Y : 0.0F;
            ModelRenderer piece = new ModelRenderer(this, box.u, box.v);
            piece.addBox(box.x, pivot - box.y - box.h, -(box.z + box.d), box.w, box.h, box.d);
            (pitch ? gunMount : body).addChild(piece);
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scale) {
        gunMount.rotateAngleX = (float) Math.toRadians(headPitch);
        body.render(scale);
    }
}
