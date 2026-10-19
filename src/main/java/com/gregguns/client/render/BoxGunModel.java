package com.gregguns.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.gregguns.client.render.profile.BoxSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A {@link GunModel} built from {@link BoxSpec} data: one {@code ModelRenderer} per box, grouped by part. The boxes
 * are authored with the muzzle toward +X (model space, Y down); {@link #render} turns them a quarter turn about Y so
 * the muzzle points toward -Z, the Z-family convention the transform rows use. Client only.
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (model space, Z-family) and section 8 (texture size).
 */
@SideOnly(Side.CLIENT)
public final class BoxGunModel extends ModelBase implements GunModel {

    private final ModelRenderer[][] byPart;
    private final float scale;
    private final ResourceLocation[] textures;
    private final float[] muzzle;
    private final boolean xFamily;

    /**
     * @param boxes     the boxes, each tagged with its part
     * @param parts     number of parts
     * @param texWidth  model sheet width in texels
     * @param texHeight model sheet height in texels
     * @param scale     GL units per texel
     * @param textures  texture paths, index 0 first
     * @param muzzle    the muzzle in the gun frame, blocks
     */
    public BoxGunModel(BoxSpec[] boxes, int parts, int texWidth, int texHeight, float scale, String[] textures,
        float[] muzzle) {
        this(boxes, parts, texWidth, texHeight, scale, textures, muzzle, false);
    }

    /**
     * As the main constructor, for either model family.
     *
     * @param xFamily true if the muzzle points along +X in the gun frame (the sheet's X-family transform rows), so the
     *                boxes are drawn as authored; false turns them a quarter turn to the Z family
     */
    public BoxGunModel(BoxSpec[] boxes, int parts, int texWidth, int texHeight, float scale, String[] textures,
        float[] muzzle, boolean xFamily) {
        this.xFamily = xFamily;
        this.textureWidth = texWidth;
        this.textureHeight = texHeight;
        this.scale = scale;
        this.muzzle = muzzle.clone();
        this.textures = new ResourceLocation[textures.length];
        for (int i = 0; i < textures.length; i++) {
            this.textures[i] = new ResourceLocation(textures[i]);
        }
        List<List<ModelRenderer>> lists = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            lists.add(new ArrayList<>());
        }
        for (BoxSpec box : boxes) {
            lists.get(box.part())
                .add(build(box, texWidth, texHeight));
        }
        byPart = new ModelRenderer[parts][];
        for (int i = 0; i < parts; i++) {
            byPart[i] = lists.get(i)
                .toArray(new ModelRenderer[0]);
        }
    }

    private ModelRenderer build(BoxSpec box, int texWidth, int texHeight) {
        ModelRenderer r = new ModelRenderer(this, box.u(), box.v());
        r.setTextureSize(texWidth, texHeight);
        boolean tilted = box.tiltZDegrees() != 0.0f || box.tiltXDegrees() != 0.0f || box.tiltYDegrees() != 0.0f;
        float px = tilted ? box.pivotX() : box.x();
        float py = tilted ? box.pivotY() : box.y();
        float pz = tilted ? box.pivotZ() : box.z();
        r.setRotationPoint(px, py, pz);
        r.addBox(box.x() - px, box.y() - py, box.z() - pz, box.w(), box.h(), box.d());
        r.rotateAngleZ = (float) Math.toRadians(box.tiltZDegrees());
        r.rotateAngleX = (float) Math.toRadians(box.tiltXDegrees());
        r.rotateAngleY = (float) Math.toRadians(box.tiltYDegrees());
        return r;
    }

    @Override
    public int parts() {
        return byPart.length;
    }

    @Override
    public ResourceLocation[] textures() {
        return textures.clone();
    }

    @Override
    public void render(int part, ModelState state) {
        if (part < 0 || part >= byPart.length) {
            return;
        }
        GL11.glPushMatrix();
        try {
            if (!xFamily) {
                GL11.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
            }
            for (ModelRenderer r : byPart[part]) {
                r.render(scale);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    @Override
    public boolean muzzleAlongX() {
        return xFamily;
    }

    @Override
    public float[] muzzle() {
        return muzzle.clone();
    }
}
