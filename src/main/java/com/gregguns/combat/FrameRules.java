package com.gregguns.combat;

/**
 * Pure rule for when vanilla's {@code attackEntityFrom} drops a hit after the {@code LivingAttackEvent}. The combat
 * handler clears a target's invulnerability frames only for a hit that will actually apply, so a rejected hit does not
 * leave the frames cleared (the target would then take the next hit at full value).
 *
 * <p>
 * Evidence (Forge 1.7.10 {@code EntityLivingBase.attackEntityFrom} and {@code EntityPlayer.attackEntityFrom}): after
 * {@code ForgeHooks.onLivingAttack} the method returns false when the entity is invulnerable, when it is a player in
 * creative (and the source cannot harm in creative), when its health is zero or below, and when the source is fire
 * damage and the entity has Fire Resistance. Fire-immune entities are <em>not</em> rejected at this point (vanilla
 * guards fire at the fire source, {@code dealFireDamage}), so they are not a case here. Anything else that still drops
 * the hit (for example peaceful difficulty for a difficulty-scaled source on a player) is caught by the restore in
 * {@link DamageSources#hit}.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md ("Invulnerability frames")
final class FrameRules {

    private FrameRules() {}

    /**
     * @param invulnerable         the entity's {@code isEntityInvulnerable()}
     * @param creativeProtected    a player whose abilities disable damage and the source cannot harm in creative
     * @param healthDepleted       the entity's health is zero or below (dead or dying)
     * @param fireVersusResistance the source is fire damage and the entity has Fire Resistance
     * @return true when vanilla will reject the hit, so the frames must be left alone
     */
    static boolean vanillaRejects(boolean invulnerable, boolean creativeProtected, boolean healthDepleted,
        boolean fireVersusResistance) {
        return invulnerable || creativeProtected || healthDepleted || fireVersusResistance;
    }
}
