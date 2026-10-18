package com.gregguns.combat;

/**
 * What a GregGuns hit carries beyond vanilla's {@code DamageSource}: its type, its penetration and a few switches.
 * Shared by {@link RangedDamageSource} and {@link DirectDamageSource}, which cannot share a superclass. The per-hit
 * penetration lives here, so it travels with the source into the {@code LivingHurtEvent} handler.
 *
 * <p>
 * One instance belongs to one source, and a source is meant for one hit: build a fresh one per hit through
 * {@link DamageSources}. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-17)
public final class HitProperties {

    private final DamageType type;
    private final double penetration;
    private boolean ignoreInvulnerabilityFrames;
    private boolean secondaryEffects = true;
    private boolean armourSettled;
    private int savedHurtResistantTime = -1;
    private double penetrationScale = 1.0;

    HitProperties(DamageType type, double penetration) {
        this.type = type;
        this.penetration = Double.isNaN(penetration) ? 0.0 : Math.max(0.0, Math.min(1.0, penetration));
        this.ignoreInvulnerabilityFrames = type.ignoresInvulnerabilityFramesByDefault();
    }

    /** @return the damage type */
    public DamageType type() {
        return type;
    }

    /** @return the penetration of this hit, 0..1 (the share of damage that skips armour before resistances) */
    public double penetration() {
        return penetration;
    }

    /** @return the multiplier on the penetration before resistances (PvP and NPC penetration settings); 1 by default */
    public double penetrationScale() {
        return penetrationScale;
    }

    /** @return true when the hit neither checks nor sets invulnerability frames */
    public boolean ignoresInvulnerabilityFrames() {
        return ignoreInvulnerabilityFrames;
    }

    /** @return true when the hit applies its type's secondary effect (unless hazmat protects) */
    public boolean secondaryEffectsEnabled() {
        return secondaryEffects;
    }

    /** @return true once the damage handler has applied armour, so vanilla must not apply it again */
    boolean isArmourSettled() {
        return armourSettled;
    }

    void setPenetrationScale(double scale) {
        this.penetrationScale = Double.isNaN(scale) ? 1.0 : Math.max(0.0, scale);
    }

    void setArmourSettled(boolean settled) {
        this.armourSettled = settled;
    }

    void setIgnoreInvulnerabilityFrames(boolean ignore) {
        this.ignoreInvulnerabilityFrames = ignore;
    }

    void disableSecondaryEffects() {
        this.secondaryEffects = false;
    }

    int savedHurtResistantTime() {
        return savedHurtResistantTime;
    }

    void setSavedHurtResistantTime(int ticks) {
        this.savedHurtResistantTime = ticks;
    }
}
