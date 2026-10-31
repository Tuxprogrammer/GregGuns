package com.gregguns.client.mob;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Makes the armor drawn on a GregGuns biped NPC take the NPC's own pose (D310). The body model is posed by its file
 * (hip carry, aim, walk, attack, death; see {@link DataBipedModel}), but vanilla poses every armor model with its own
 * {@code ModelBiped} math when the armor pass renders it. Left alone, a sleeve hangs at vanilla's holding angle while
 * the arm under it is at the file's, and the bare arm shows below the sleeve.
 *
 * <p>
 * While {@link RenderDataBiped} draws an NPC, the NPC's body model is the <em>source</em>. Every armor model that
 * calls {@link #follow} at the end of its {@code setRotationAngles} then copies the source's six parts (angles and
 * pivots, the head also onto the headwear), so it wins over vanilla's math. The vanilla armor layers of the renderer,
 * {@link com.gregguns.client.armor.BoxArmorModel} and the mask helmets follow; outside an NPC render (a player
 * wearing the same cached models) there is no source and {@code follow} does nothing.
 *
 * <p>
 * Not covered, by design: a third-party armor model with its own pose code (NPCs only wear GregGuns armor), and the
 * non-biped NPC models, which wear no armor.
 *
 * <p>
 * Rendering is single-threaded on the client thread, so the source is a plain static, always cleared by
 * {@link #end}. Client only.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/index.md (Rendering: armor follows the posed model)
@SideOnly(Side.CLIENT)
public final class ArmorPose {

    private static @Nullable ModelBiped source;

    private ArmorPose() {}

    /**
     * Copies the six biped parts' rotation angles and rotation points, and the head onto the headwear.
     *
     * @param from the posed model
     * @param to   the model that takes the pose
     */
    public static void copy(ModelBiped from, ModelBiped to) {
        part(from.bipedHead, to.bipedHead);
        part(from.bipedHead, to.bipedHeadwear);
        part(from.bipedBody, to.bipedBody);
        part(from.bipedRightArm, to.bipedRightArm);
        part(from.bipedLeftArm, to.bipedLeftArm);
        part(from.bipedRightLeg, to.bipedRightLeg);
        part(from.bipedLeftLeg, to.bipedLeftLeg);
    }

    private static void part(ModelRenderer from, ModelRenderer to) {
        to.rotateAngleX = from.rotateAngleX;
        to.rotateAngleY = from.rotateAngleY;
        to.rotateAngleZ = from.rotateAngleZ;
        to.rotationPointX = from.rotationPointX;
        to.rotationPointY = from.rotationPointY;
        to.rotationPointZ = from.rotationPointZ;
    }

    /**
     * Starts an NPC render: armor drawn until {@link #end} follows this model. Pair it with {@code end} in a
     * {@code finally}, or use {@link #during}.
     *
     * @param posed the NPC's body model
     */
    public static void begin(ModelBiped posed) {
        source = posed;
    }

    /** Ends the NPC render: armor models pose themselves again. */
    public static void end() {
        source = null;
    }

    /**
     * Runs a render with {@code posed} as the source, and clears the source however the render ends.
     *
     * @param posed  the NPC's body model
     * @param render the render
     */
    public static void during(ModelBiped posed, Runnable render) {
        begin(posed);
        try {
            render.run();
        } finally {
            end();
        }
    }

    /** @return the model armor follows now, or null outside an NPC render */
    public static @Nullable ModelBiped source() {
        return source;
    }

    /**
     * Called by an armor model at the end of its {@code setRotationAngles}: takes the NPC's pose when an NPC is being
     * drawn, and does nothing otherwise.
     *
     * @param self the armor model
     */
    public static void follow(ModelBiped self) {
        ModelBiped from = source;
        if (from != null && from != self) {
            copy(from, self);
        }
    }
}
