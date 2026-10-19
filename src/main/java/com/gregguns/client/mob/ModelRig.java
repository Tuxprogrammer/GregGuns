package com.gregguns.client.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.jetbrains.annotations.Nullable;

import com.gregguns.mob.model.AnimInput;
import com.gregguns.mob.model.MobAnimSource;
import com.gregguns.mob.model.MobAnimator;
import com.gregguns.mob.model.MobBoxDef;
import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobPartDef;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The parts of one {@link MobModelDef} built as {@link ModelRenderer}s, and the code that poses them each frame. Both
 * {@link DataMobModel} and {@link DataBipedModel} own one, so the two modes share every line of building and
 * animating.
 *
 * <p>
 * Client only.
 */
// Spec: tools/asset-gen/entity_models.md
@SideOnly(Side.CLIENT)
final class ModelRig {

    private final MobModelDef def;
    private final RigRenderer[] parts;
    private final float[][] angles;
    private final AnimInput input = new AnimInput();

    /**
     * @param owner the model the parts belong to; its texture size must already be set
     * @param def   the definition to build
     */
    ModelRig(ModelBase owner, MobModelDef def) {
        this.def = def;
        owner.textureWidth = def.textureWidth();
        owner.textureHeight = def.textureHeight();
        this.parts = new RigRenderer[def.parts()
            .size()];
        this.angles = new float[parts.length][3];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = build(
                owner,
                def.parts()
                    .get(i));
        }
        for (int i = 0; i < parts.length; i++) {
            String parent = def.parts()
                .get(i)
                .parent();
            if (parent != null) {
                parts[def.indexOf(parent)].addChild(parts[i]);
            }
        }
    }

    private static RigRenderer build(ModelBase owner, MobPartDef p) {
        RigRenderer r = new RigRenderer(owner);
        r.glow = p.fullbright();
        for (MobBoxDef b : p.boxes()) {
            r.setTextureOffset(b.u(), b.v());
            // Source: Forge 1.7.10 / vanilla ModelRenderer: the mirror flag must be set before addBox, which reads it.
            r.mirror = b.mirror();
            float[] o = b.offset();
            int[] s = b.size();
            r.addBox(o[0], o[1], o[2], s[0], s[1], s[2], b.inflate());
        }
        r.mirror = false;
        float[] pivot = p.pivot();
        r.setRotationPoint(pivot[0], pivot[1], pivot[2]);
        return r;
    }

    /** @return the definition */
    MobModelDef def() {
        return def;
    }

    /**
     * @param name a part name from the definition
     * @return its renderer
     */
    RigRenderer part(String name) {
        int i = def.indexOf(name);
        if (i < 0) {
            throw new IllegalArgumentException("model '" + def.name() + "' has no part '" + name + "'");
        }
        return parts[i];
    }

    /** @return the renderers whose definition has no parent, in file order */
    java.util.List<RigRenderer> roots() {
        java.util.List<RigRenderer> out = new java.util.ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (def.parts()
                .get(i)
                .parent() == null) {
                out.add(parts[i]);
            }
        }
        return out;
    }

    /**
     * Evaluates the animations and writes the angles onto the parts.
     *
     * @param limbSwing       walk distance
     * @param limbSwingAmount walk speed 0 to 1
     * @param ageInTicks      age with the partial tick
     * @param netHeadYaw      head yaw relative to the body, degrees
     * @param headPitch       head pitch, degrees
     * @param entity          the mob, or null
     * @param aimedFlag       the model's own aiming flag (the renderer sets it when a gun is held)
     * @param holding         whether the right hand holds an item
     * @param swingProgress   the vanilla swing progress
     * @param partialTicks    the render partial tick
     */
    void pose(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
        @Nullable Entity entity, boolean aimedFlag, boolean holding, float swingProgress, float partialTicks) {
        AnimInput in = input;
        in.limbSwing = limbSwing;
        in.limbSwingAmount = limbSwingAmount;
        in.ageInTicks = ageInTicks;
        in.netHeadYaw = netHeadYaw;
        in.headPitch = headPitch;
        in.holding = holding;
        in.aiming = aimedFlag;
        in.attackProgress = swingProgress;
        if (entity instanceof MobAnimSource) {
            MobAnimSource src = (MobAnimSource) entity;
            in.aiming = src.isAiming();
            float p = src.attackProgress(partialTicks);
            if (p >= 0.0F) {
                in.attackProgress = p;
            }
        }
        MobAnimator.evaluate(def, in, angles);
        for (int i = 0; i < parts.length; i++) {
            parts[i].rotateAngleX = angles[i][0];
            parts[i].rotateAngleY = angles[i][1];
            parts[i].rotateAngleZ = angles[i][2];
        }
    }
}
