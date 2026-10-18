package com.gregguns.projectile;

/**
 * Where a bullet starts and which way it flies: the shooter's eye, turned by the shot's spread offset, shifted to the
 * firing hand. Plain Java; the launcher feeds it the shooter's numbers and copies the result onto the entity.
 *
 * <p>
 * Angles follow Minecraft: yaw 0 faces +Z (south), yaw -90 faces +X (east), pitch -90 looks straight up.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.2 Spawn position and initial velocity)
public final class ShotGeometry {

    /** Sideways shift of the muzzle from the eye, blocks. */
    public static final double HAND_OFFSET = 0.16;
    /** Downward shift of the muzzle from the eye, blocks. */
    public static final double HAND_DROP = 0.1;

    private ShotGeometry() {}

    /** The start state of one bullet. Mutable only so the maths can hand back several numbers; one per shot. */
    public static final class Spawn {

        /** Start X in blocks. */
        public double x;
        /** Start Y in blocks. */
        public double y;
        /** Start Z in blocks. */
        public double z;
        /** Initial motion X in blocks per tick. */
        public double motionX;
        /** Initial motion Y in blocks per tick. */
        public double motionY;
        /** Initial motion Z in blocks per tick. */
        public double motionZ;
        /** Flight yaw in degrees, projectile convention. */
        public float yaw;
        /** Flight pitch in degrees, positive upwards. */
        public float pitch;
    }

    /**
     * Computes the start state of one bullet.
     *
     * @param eyeX                shooter eye position X
     * @param eyeY                shooter eye position Y
     * @param eyeZ                shooter eye position Z
     * @param headYaw             the way the shooter is looking, degrees
     * @param headPitch           the shooter's pitch, degrees
     * @param yawOffset           spread offset added to the yaw, degrees
     * @param pitchOffset         spread offset added to the pitch, degrees
     * @param leftHand            {@code true} to start from the left hand
     * @param speed               blocks per tick
     * @param forwardTickFraction fraction of one tick's travel to shift the start forward (in-line pellets)
     * @return the start state
     */
    public static Spawn compute(double eyeX, double eyeY, double eyeZ, double headYaw, double headPitch,
        double yawOffset, double pitchOffset, boolean leftHand, double speed, double forwardTickFraction) {
        double yaw = Math.toRadians(headYaw + yawOffset);
        double pitch = Math.toRadians(headPitch + pitchOffset);
        double cosPitch = Math.cos(pitch);
        double dirX = -Math.sin(yaw) * cosPitch;
        double dirY = -Math.sin(pitch);
        double dirZ = Math.cos(yaw) * cosPitch;

        // The right hand sits to the shooter's right, which is (-cos yaw, -sin yaw) in X and Z.
        double side = leftHand ? HAND_OFFSET : -HAND_OFFSET;
        Spawn s = new Spawn();
        s.motionX = dirX * speed;
        s.motionY = dirY * speed;
        s.motionZ = dirZ * speed;
        s.x = eyeX + Math.cos(yaw) * side + s.motionX * forwardTickFraction;
        s.y = eyeY - HAND_DROP + s.motionY * forwardTickFraction;
        s.z = eyeZ + Math.sin(yaw) * side + s.motionZ * forwardTickFraction;
        s.yaw = Ballistics.yawOf(dirX, dirZ);
        s.pitch = Ballistics.pitchOf(dirX, dirY, dirZ);
        return s;
    }
}
