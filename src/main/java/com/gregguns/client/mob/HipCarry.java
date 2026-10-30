package com.gregguns.client.mob;

/**
 * The hip-carry pose of an NPC that holds a gun but has no target. The arm hangs close to the body, tipped forward
 * {@link #ARM_X_DEGREES} so the hand sits at the hip, and the gun is tilted up about the hand by
 * {@link #GUN_LIFT_DEGREES} so it points forward and slightly down instead of along the hanging arm. The arm angle
 * also lives in the model files' {@code carrying} pose ({@code biped.json}, {@code skeleton_soldier.json},
 * {@code super_mutant.json}); a unit test keeps the two in step. Pure; no Minecraft types.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/index.md (idle pose); owner request 2026-10-30
public final class HipCarry {

    /** Right-arm pitch in the carrying pose, degrees (negative tips the arm forward). */
    public static final float ARM_X_DEGREES = -25.0F;
    /** Extra pitch of the gun about the hand, degrees (negative lifts the muzzle). */
    public static final float GUN_LIFT_DEGREES = -45.0F;

    private HipCarry() {}

    /**
     * @return how far below horizontal the muzzle points while carrying, degrees: an arm pitched 90 degrees points
     *         the gun level, and each degree less points it a degree lower
     */
    public static float muzzleDownDegrees() {
        return 90.0F + ARM_X_DEGREES + GUN_LIFT_DEGREES;
    }

    /**
     * @param holdsGun true when the mob holds a gun
     * @param aiming   true when it is aiming
     * @return true when the hip carry applies
     */
    public static boolean applies(boolean holdsGun, boolean aiming) {
        return holdsGun && !aiming;
    }
}
