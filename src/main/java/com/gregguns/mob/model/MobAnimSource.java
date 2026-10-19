package com.gregguns.mob.model;

/**
 * What a mob entity tells its model that the vanilla fields do not carry. An entity class implements this (it is
 * optional): without it the biped model aims when a gun is in the right hand, and an attack plays over the vanilla
 * swing progress. Pure interface, so entity code on the server may implement it.
 */
// Spec: tools/asset-gen/entity_models.md
public interface MobAnimSource {

    /** @return true while the mob holds its weapon up to fire; drives {@code pose} entries with {@code when: aiming} */
    boolean isAiming();

    /**
     * The progress of the current attack, for {@code pose} entries with {@code when: attacking}.
     *
     * @param partialTicks the render partial tick
     * @return 0 to 1 over the attack, or a negative value when the entity has no attack clock of its own (the model
     *         then uses the vanilla swing progress)
     */
    float attackProgress(float partialTicks);

    /**
     * How far the mob is into its death sequence, for {@code pose} entries with {@code when: dying} and the
     * {@code deathScale} of a {@code spin}.
     *
     * @param partialTicks the render partial tick
     * @return 0 while alive, rising to 1 as the death animation completes
     */
    default float deathProgress(float partialTicks) {
        return 0.0F;
    }
}
