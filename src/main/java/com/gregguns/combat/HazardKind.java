package com.gregguns.combat;

/**
 * The GregTech hazard categories that matter to GregGuns damage, as plain constants so the damage tables stay free of
 * Minecraft and GregTech types. {@link GregTechHazmat} maps them to {@code gregtech.api.hazards.Hazard}.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GT5-Unofficial: hazmat protection)
public enum HazardKind {
    /** Fire damage. GT: {@code Hazard.HEAT}. */
    HEAT,
    /** Ice damage. GT: {@code Hazard.FROST}. */
    FROST,
    /** Radiation damage. GT: {@code Hazard.RADIOLOGICAL}. */
    RADIOLOGICAL,
    /** Poison damage (the Bio Gun). GT: {@code Hazard.BIOLOGICAL}. */
    BIOLOGICAL,
    /** Lightning damage (Tesla). GT: {@code Hazard.ELECTRICAL}. */
    ELECTRICAL
}
