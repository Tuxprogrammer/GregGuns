package com.gregguns.client.utility;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Builds a vanilla {@link ModelBase} from the {@link TurretGeometry.Part#BASE} boxes: the Turret base block, drawn as
 * a tile entity render because GT would otherwise draw a casing cube. The root sits at the center of the block's
 * bottom face with vanilla's Y down and front at -z, so the caller draws it under {@code scale(1, -1, -1)}. The unwrap
 * is the same one {@link TurretModel} uses, so the one sheet paints both. A box with a {@code rotY} turns about the
 * vertical through its center. Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/model-notes/turret_base.md,
// docs/gregguns-spec/09-machines-utility/turret.md
@SideOnly(Side.CLIENT)
public final class TurretBaseModel extends ModelBase {

    private final ModelRenderer root;

    /** Builds one piece per base box. */
    public TurretBaseModel() {
        textureWidth = TurretGeometry.TEXTURE_SIZE;
        textureHeight = TurretGeometry.TEXTURE_SIZE;
        root = new ModelRenderer(this, 0, 0);
        for (TurretGeometry.Box box : TurretGeometry.BOXES) {
            if (box.part != TurretGeometry.Part.BASE) {
                continue;
            }
            ModelRenderer piece = new ModelRenderer(this, box.u, box.v);
            // Pivot at the box's footprint center; the box is then laid out around it.
            piece.setRotationPoint(box.x + box.w / 2.0F, 0.0F, -(box.z + box.d / 2.0F));
            piece.addBox(-box.w / 2.0F, -(box.y + box.h), -box.d / 2.0F, box.w, box.h, box.d);
            // Geometry turns x toward +z; the model's z is mirrored, so the angle keeps its sign (see class notes).
            piece.rotateAngleY = (float) Math.toRadians(box.rotY);
            root.addChild(piece);
        }
    }

    /** @param scale the pixel scale, 1/16 */
    public void render(float scale) {
        root.render(scale);
    }
}
