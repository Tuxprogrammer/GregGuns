package com.gregguns.client.projectile;

/**
 * Pure math for moving a beam's start from the shooter's eye to the gun's muzzle in first person. The server spawns a
 * beam at the shot origin (the eye), so seen from the shooter's own camera it starts at or behind the near plane and
 * is drawn straight over the held gun. The client shifts the start to the muzzle and keeps the end where the hit
 * test put it. No Minecraft types; angles in degrees, positions in blocks.
 */
public final class BeamOrigin {

    /** A beam whose start is within this many blocks of the camera belongs to the viewer's own shot. */
    public static final double OWN_SHOT_RADIUS = 1.6;
    /** Beams shorter than this after the shift are not drawn (the target is closer than the muzzle). */
    public static final double MIN_LENGTH = 0.05;

    private BeamOrigin() {}

    /**
     * Converts a camera-space offset into a world offset.
     *
     * @param eye      {@code (x right, y up, z)} in OpenGL eye space, where negative z is forward
     * @param yawDeg   camera yaw, Minecraft convention (0 looks along +Z)
     * @param pitchDeg camera pitch (positive looks down)
     * @return the world-space offset {@code (x, y, z)}
     */
    public static double[] eyeToWorld(double[] eye, double yawDeg, double pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        // up = right x forward
        double ux = 0.0 * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - 0.0 * fx;
        double back = -eye[2];
        return new double[] { eye[0] * rx + eye[1] * ux + back * fx, eye[0] * 0.0 + eye[1] * uy + back * fy,
            eye[0] * rz + eye[1] * uz + back * fz };
    }

    /** @return true if a beam starting at {@code start} was fired from the camera at {@code cam} */
    public static boolean isOwnShot(double[] start, double[] cam) {
        double dx = start[0] - cam[0];
        double dy = start[1] - cam[1];
        double dz = start[2] - cam[2];
        return dx * dx + dy * dy + dz * dz <= OWN_SHOT_RADIUS * OWN_SHOT_RADIUS;
    }

    /**
     * Re-aims a beam so it starts at the muzzle and still ends at its original end point.
     *
     * @param start  original start (the eye), world
     * @param dir    original unit direction, world
     * @param length original length
     * @param muzzle the muzzle in world space
     * @return {@code {startX, startY, startZ, dirX, dirY, dirZ, length}}, or null if the beam would be shorter than
     *         {@link #MIN_LENGTH}
     */
    public static double[] fromMuzzle(double[] start, double[] dir, double length, double[] muzzle) {
        double ex = start[0] + dir[0] * length - muzzle[0];
        double ey = start[1] + dir[1] * length - muzzle[1];
        double ez = start[2] + dir[2] * length - muzzle[2];
        double len = Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (len < MIN_LENGTH) {
            return null;
        }
        return new double[] { muzzle[0], muzzle[1], muzzle[2], ex / len, ey / len, ez / len, len };
    }
}
