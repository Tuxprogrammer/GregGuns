package com.gregguns.client.mob;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobModels;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The biped mode of a data-driven mob model. It is a real {@link ModelBiped} whose six fields
 * ({@code bipedHead}, {@code bipedBody}, {@code bipedRightArm}, {@code bipedLeftArm}, {@code bipedRightLeg},
 * {@code bipedLeftLeg}) are the file's parts named {@code head}, {@code body}, {@code rightArm}, {@code leftArm},
 * {@code rightLeg} and {@code leftLeg}. That is all {@code RenderBiped} needs: held items attach to the right arm,
 * the armor layers are separate vanilla biped models, and {@code aimedBow} and {@code heldItemRight} keep their
 * meaning. The file's animations replace the vanilla pose math: the vanilla walk, aimed-bow and sneak code does not
 * run,
 * so a model gets exactly the motion its file lists. Other parts are children of the six, drawn with them.
 *
 * <p>
 * Client only.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md section 7.1
@SideOnly(Side.CLIENT)
public final class DataBipedModel extends ModelBiped {

    private final ModelRig rig;
    private float partial;

    /** @param def a biped definition (validated to have the six parts as roots) */
    public DataBipedModel(MobModelDef def) {
        super(0.0F, 0.0F, def.textureWidth(), def.textureHeight());
        if (!def.biped()) {
            throw new IllegalArgumentException("model '" + def.name() + "' is not marked biped");
        }
        this.rig = new ModelRig(this, def);
        this.bipedHead = rig.part("head");
        this.bipedBody = rig.part("body");
        this.bipedRightArm = rig.part("rightArm");
        this.bipedLeftArm = rig.part("leftArm");
        this.bipedRightLeg = rig.part("rightLeg");
        this.bipedLeftLeg = rig.part("leftLeg");
        // No hat, ears or cloak: the player-only extras of ModelBiped draw nothing.
        this.bipedHeadwear = new ModelRenderer(this, 0, 0);
        this.bipedEars = new ModelRenderer(this, 0, 0);
        this.bipedCloak = new ModelRenderer(this, 0, 0);
    }

    /**
     * @param name a model name under {@code assets/gregguns/entity_models}
     * @return the model
     */
    public static DataBipedModel load(String name) {
        return new DataBipedModel(MobModels.load(name));
    }

    /** @return the definition this model was built from */
    public MobModelDef def() {
        return rig.def();
    }

    /**
     * Adds a translation, in blocks and in the right arm's frame, applied when a held item is attached to the hand.
     *
     * @param x blocks
     * @param y blocks
     * @param z blocks
     */
    public void setHandOffset(float x, float y, float z) {
        rig.part("rightArm")
            .setHandOffset(x, y, z);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks) {
        this.partial = partialTicks;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scale, Entity entity) {
        rig.pose(
            limbSwing,
            limbSwingAmount,
            ageInTicks,
            netHeadYaw,
            headPitch,
            entity,
            aimedBow,
            heldItemRight != 0,
            onGround,
            partial);
    }
}
