package com.gregguns.client.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import com.gregguns.mob.model.MobModelDef;
import com.gregguns.mob.model.MobModels;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A mob model built from a {@link MobModelDef}: the generic mode, for creatures that are not biped. It builds the
 * part tree, evaluates the file's animations in {@link #setRotationAngles} and draws the root parts. The biped mode is
 * {@link DataBipedModel}.
 *
 * <p>
 * Client only.
 */
// Spec: tools/asset-gen/entity_models.md; docs/techguns-spec/10-entities-npcs/rendering-and-models.md section 4
@SideOnly(Side.CLIENT)
public final class DataMobModel extends ModelBase {

    private final ModelRig rig;
    private float partial;
    /** Set by the renderer while the mob holds its weapon up; the model reads it for {@code when: aiming} poses. */
    public boolean aiming;
    /** Set by the renderer when the mob holds an item. */
    public boolean holding;

    /** @param def the definition; a biped definition also works, it is then drawn without the biped fields */
    public DataMobModel(MobModelDef def) {
        this.rig = new ModelRig(this, def);
    }

    /**
     * @param name a model name under {@code assets/gregguns/entity_models}
     * @return the model
     */
    public static DataMobModel load(String name) {
        return new DataMobModel(MobModels.load(name));
    }

    /** @return the definition this model was built from */
    public MobModelDef def() {
        return rig.def();
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
            aiming,
            holding,
            onGround,
            partial);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        for (RigRenderer r : rig.roots()) {
            r.render(scale);
        }
    }
}
