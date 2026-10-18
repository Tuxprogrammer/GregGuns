package com.gregguns.armor.logic;

/**
 * The GT hazards GregGuns armor can protect against. There is deliberately no {@code SPACE} constant: space protection
 * belongs to GalaxySpace, and GregGuns armor never claims it (GGS-05-12). Mapped to {@code gregtech.api.hazards.Hazard}
 * by the armor item. Plain Java.
 */
// Spec: docs/gregguns-spec/05-armor/hazard-protection.md (GGS-05-12)
public enum HazardFlag {
    /** GT BIOLOGICAL. */
    BIOLOGICAL,
    /** GT FROST. */
    FROST,
    /** GT HEAT. */
    HEAT,
    /** GT RADIOLOGICAL. */
    RADIOLOGICAL,
    /** GT ELECTRICAL. */
    ELECTRICAL,
    /** GT GAS. */
    GAS
}
