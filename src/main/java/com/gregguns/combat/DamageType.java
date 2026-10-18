package com.gregguns.combat;

import java.util.Locale;

/**
 * The ten GregGuns damage types, kept from Techguns ({@code damage-system.md} section 1). A type decides which vanilla
 * flags the {@link GunDamageSource} carries, whether the hit ignores invulnerability frames by default, and which GT
 * hazard guards its secondary effect. Plain Java, no Minecraft types.
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md section 1; docs/design/0004-damage-model.md
public enum DamageType {

    /** Melee hits. No vanilla flag. */
    PHYSICAL(false, null),
    /** Bullets and arrows. Vanilla projectile flag; ignores invulnerability frames. */
    PROJECTILE(true, null),
    /**
     * Flamethrower and fire. Deviation: sets the vanilla fire flag (Techguns used magic), so Fire Resistance applies
     * (fixes GUN-44).
     */
    FIRE(true, HazardKind.HEAT),
    /** Rockets and TNT. Vanilla explosion flag. */
    EXPLOSION(false, null),
    /** Lasers, blasters. Vanilla magic flag. */
    ENERGY(true, null),
    /** Gas and bio weapons. Vanilla magic flag. */
    POISON(true, HazardKind.BIOLOGICAL),
    /** Starve, fall, suffocate. Bypasses armor; the only unblockable type. */
    UNRESISTABLE(false, null),
    /** Cold. Vanilla magic flag. */
    ICE(false, HazardKind.FROST),
    /** Tesla. Vanilla magic flag; ignores invulnerability frames. */
    LIGHTNING(true, HazardKind.ELECTRICAL),
    /** Death ray. Vanilla magic flag; ignores invulnerability frames. */
    RADIATION(true, HazardKind.RADIOLOGICAL);

    /** Prefix of every GregGuns {@code DamageSource.damageType} string. */
    public static final String SOURCE_PREFIX = "gregguns.";

    private final boolean ignoresFramesByDefault;
    private final HazardKind hazard;

    DamageType(boolean ignoresFramesByDefault, HazardKind hazard) {
        this.ignoresFramesByDefault = ignoresFramesByDefault;
        this.hazard = hazard;
    }

    /** @return the lower-case id, for example {@code "energy"} */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the vanilla {@code DamageSource.damageType} string, {@code "gregguns.<id>"} */
    public String sourceName() {
        return SOURCE_PREFIX + id();
    }

    /** @return the lang key of the type's display name, for tooltips */
    public String nameLangKey() {
        return "gregguns.damage_type." + id();
    }

    /** @return the death-message lang key, {@code "death.attack.gregguns.<id>"} (GGS-02-23) */
    public String deathMessageKey() {
        return "death.attack." + sourceName();
    }

    /**
     * Whether hits of this type skip armor entirely (and set vanilla's unblockable flag). Only {@link #UNRESISTABLE}
     * does (GGS-02-21).
     *
     * @return true for the unresistable type
     */
    public boolean bypassesArmor() {
        return this == UNRESISTABLE;
    }

    /**
     * Whether a source of this type ignores (neither checks nor sets) invulnerability frames unless the caller
     * overrides it. Matches the Techguns factory table: every ranged and over-time type does; melee, explosions and
     * ice do not.
     *
     * @return the default
     */
    public boolean ignoresInvulnerabilityFramesByDefault() {
        return ignoresFramesByDefault;
    }

    /** @return the GT hazard that guards this type's secondary effect, or {@code null} when none does */
    public HazardKind hazard() {
        return hazard;
    }
}
