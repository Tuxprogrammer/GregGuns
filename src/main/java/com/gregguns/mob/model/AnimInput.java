package com.gregguns.mob.model;

/**
 * The per-frame inputs of the animation evaluator: the arguments of {@code ModelBase.setRotationAngles} plus the
 * three state flags a pose reads. A plain mutable holder so the client model can refill one instance every frame
 * without allocating, and tests can set exactly the values they want.
 */
// Spec: tools/asset-gen/entity_models.md
public final class AnimInput {

    /** Walk distance, the {@code limbSwing} of the renderer. */
    public float limbSwing;
    /** Walk speed from 0 (standing) to 1, the {@code limbSwingAmount}. */
    public float limbSwingAmount;
    /** Age in ticks, with the partial tick folded in. */
    public float ageInTicks;
    /** Head yaw relative to the body, degrees. */
    public float netHeadYaw;
    /** Head pitch, degrees. */
    public float headPitch;
    /** Whether the mob is aiming a gun. */
    public boolean aiming;
    /** Whether the mob holds an item in its right hand. */
    public boolean holding;
    /** Attack progress 0 to 1, or 0 when not attacking. */
    public float attackProgress;
}
