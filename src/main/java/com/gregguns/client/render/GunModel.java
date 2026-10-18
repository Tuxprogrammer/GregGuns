package com.gregguns.client.render;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A gun's 3D model as the item renderer sees it. The renderer sets up every transform and binds the texture, then
 * asks the model to draw one part. Implementations draw their boxes only (no transforms of their own beyond the
 * fixed orientation of the model), so the renderer controls all motion. Client only.
 *
 * <p>
 * Spec: docs/techguns-spec/03-gun-visuals-audio/rendering-framework.md sections 1, 2 and 10 (parts, ammo-dependent
 * geometry).
 */
@SideOnly(Side.CLIENT)
public interface GunModel {

    /** @return how many parts the model has; part indices run from 0 */
    int parts();

    /**
     * @return the textures, index 0 first; further entries are camo variants. Never empty. The renderer clamps the
     *         stack's camo index into range (GVA-18).
     */
    ResourceLocation[] textures();

    /**
     * Draws one part. The matrix stack is already set up; the texture is bound.
     *
     * @param part  part index, from 0 to {@code parts() - 1}
     * @param state ammo, reload and fire progress and the render type, for ammo-dependent geometry
     */
    void render(int part, ModelState state);

    /**
     * @return the muzzle in the gun frame (the frame the model is drawn in, before the model's own orientation), as
     *         {x, y, z} in blocks, for the third-person flash
     */
    float[] muzzle();

    /**
     * @return true for an X-family model, whose barrel points along +X in the gun frame instead of -Z; the
     *         third-person muzzle flash is drawn along that axis
     */
    default boolean muzzleAlongX() {
        return false;
    }
}
