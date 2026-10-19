package com.gregguns.client.accessory;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A worn accessory model built from an {@link AccessoryDef}: one {@code ModelRenderer} per box, the hinged parts
 * grouped under a hinge renderer so a folding glider can turn its outer wings. Client only.
 *
 * <p>
 * The model is drawn in the frame of the body part it is mounted on (the head or the torso), at the def's scale; the
 * caller sets up that frame ({@code ModelRenderer.postRender}) and binds the texture.
 */
@SideOnly(Side.CLIENT)
public final class AccessoryModel extends ModelBase {

    private static final String TEXTURE_DIR = "gregguns:textures/models/armor/";

    private final AccessoryDef def;
    private final ModelRenderer root;
    private final ModelRenderer[] hinges;

    /** @param def the model's boxes and sheet */
    public AccessoryModel(AccessoryDef def) {
        this.def = def;
        this.textureWidth = def.textureWidth();
        this.textureHeight = def.textureHeight();
        this.root = new ModelRenderer(this, 0, 0);
        this.root.setTextureSize(textureWidth, textureHeight);
        this.root.setRotationPoint(0.0f, 0.0f, 0.0f);
        this.hinges = new ModelRenderer[def.parts()];
        for (int p = 1; p < hinges.length; p++) {
            float[] pivot = def.hinge(p);
            ModelRenderer hinge = new ModelRenderer(this, 0, 0);
            hinge.setTextureSize(textureWidth, textureHeight);
            hinge.setRotationPoint(pivot[0], pivot[1], pivot[2]);
            root.addChild(hinge);
            hinges[p] = hinge;
        }
        for (AccessoryBox box : def.boxes()) {
            float[] pivot = box.pivot();
            float[] rot = box.rotationDegrees();
            ModelRenderer r = new ModelRenderer(this, box.u(), box.v());
            r.setTextureSize(textureWidth, textureHeight);
            float[] base = box.part() == 0 ? new float[3] : def.hinge(box.part());
            r.setRotationPoint(pivot[0] - base[0], pivot[1] - base[1], pivot[2] - base[2]);
            r.addBox(box.x() - pivot[0], box.y() - pivot[1], box.z() - pivot[2], box.w(), box.h(), box.d());
            r.rotateAngleX = (float) Math.toRadians(rot[0]);
            r.rotateAngleY = (float) Math.toRadians(rot[1]);
            r.rotateAngleZ = (float) Math.toRadians(rot[2]);
            (box.part() == 0 ? root : hinges[box.part()]).addChild(r);
        }
    }

    /** @return the model definition this was built from */
    public AccessoryDef def() {
        return def;
    }

    /** @return the root renderer, so a biped armor model can hang the accessory under its head */
    public ModelRenderer root() {
        return root;
    }

    /**
     * @param textureIndex index into {@link AccessoryDef#textures()}; out of range gives the default
     * @return the sheet to bind
     */
    public ResourceLocation texture(int textureIndex) {
        int i = textureIndex >= 0 && textureIndex < def.textures()
            .size() ? textureIndex : 0;
        return new ResourceLocation(
            TEXTURE_DIR + def.textures()
                .get(i) + ".png");
    }

    /**
     * Stows or deploys the hinged parts. The glider's wings are flat and never turn; they pop in and out, so the folded
     * pack alone remains when stowed.
     *
     * @param fold 0 is deployed (as authored), 1 is stowed; the wings show while it is under one half
     */
    public void setFold(float fold) {
        for (int p = 1; p < hinges.length; p++) {
            hinges[p].showModel = fold < 0.5f;
        }
    }

    /**
     * Draws the model at the current GL transform, at its own scale.
     */
    public void render() {
        root.render(def.scale());
    }
}
