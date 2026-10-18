package com.gregguns.combat;

import net.minecraft.entity.EntityLivingBase;

import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.HazardProtection;

/**
 * Asks GregTech whether an entity wears full hazmat for a hazard. GregTech is a hard dependency, so no guard is needed.
 * Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (GGS-02-22)
public final class GregTechHazmat {

    private GregTechHazmat() {}

    /**
     * @param kind a GregGuns hazard
     * @return the matching GT hazard
     */
    public static Hazard toGt(HazardKind kind) {
        switch (kind) {
            case HEAT:
                return Hazard.HEAT;
            case FROST:
                return Hazard.FROST;
            case RADIOLOGICAL:
                return Hazard.RADIOLOGICAL;
            case BIOLOGICAL:
                return Hazard.BIOLOGICAL;
            case ELECTRICAL:
                return Hazard.ELECTRICAL;
            default:
                throw new IllegalArgumentException("Unmapped hazard " + kind);
        }
    }

    /**
     * @param entity the target
     * @param kind   the hazard of the hit's secondary effect
     * @return true when the entity wears full GT hazmat against it
     */
    public static boolean isProtected(EntityLivingBase entity, HazardKind kind) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L44-L60
        // (all four armor slots protect, or one slot protects fully; the Hazmat enchantment counts too)
        return HazardProtection.isWearingFullHazmatAgainst(entity, toGt(kind));
    }
}
