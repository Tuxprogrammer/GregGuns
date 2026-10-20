package com.gregguns.entity.npc;

/**
 * Yaw and pitch from an offset, in Minecraft's convention (yaw 0 faces +Z, increasing clockwise seen from above;
 * positive pitch looks down). Pure, no Minecraft types. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 3, step 4: the NPC looks at its target)
public final class AimAngles {

    private AimAngles() {}

    /**
     * @param dx target minus muzzle, east
     * @param dy target minus muzzle, up
     * @param dz target minus muzzle, south
     * @return the yaw in degrees, in [-270, 90]
     */
    public static float yaw(double dx, double dy, double dz) {
        return (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
    }

    /**
     * @param dx target minus muzzle, east
     * @param dy target minus muzzle, up
     * @param dz target minus muzzle, south
     * @return the pitch in degrees: negative when the target is above, positive when below
     */
    public static float pitch(double dx, double dy, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -(Math.atan2(dy, horizontal) * 180.0 / Math.PI);
    }
}
