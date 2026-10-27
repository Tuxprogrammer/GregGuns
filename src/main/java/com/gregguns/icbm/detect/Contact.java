package com.gregguns.icbm.detect;

/**
 * One tracked missile in flight, as a radar sees it: a snapshot taken on the server thread, safe to hand to a GUI, a
 * redstone output, a battery or an OpenComputers adapter. It holds no Minecraft types, so it can be tested and handed
 * across threads. The snapshot does not follow the missile; ask {@link Contacts} again for a fresh one.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Contacts), 01-core/golden-dome.md (getContacts)
public final class Contact implements ContactSource {

    private final int id;
    private final int dimension;
    private final double x;
    private final double y;
    private final double z;
    private final double vx;
    private final double vy;
    private final double vz;
    private final int tier;
    private final double impactX;
    private final double impactZ;

    /**
     * @param id        the missile's entity id, unique within its world while it flies
     * @param dimension the dimension id
     * @param x         position X
     * @param y         position Y
     * @param z         position Z
     * @param vx        velocity X, blocks per tick
     * @param vy        velocity Y, blocks per tick
     * @param vz        velocity Z, blocks per tick
     * @param tier      the missile tier, 1 to 3
     * @param impactX   predicted impact X
     * @param impactZ   predicted impact Z
     */
    public Contact(int id, int dimension, double x, double y, double z, double vx, double vy, double vz, int tier,
        double impactX, double impactZ) {
        this.id = id;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.tier = tier;
        this.impactX = impactX;
        this.impactZ = impactZ;
    }

    /** @return the missile's entity id; stable for the whole flight */
    @Override
    public int id() {
        return id;
    }

    /** @return the dimension id */
    @Override
    public int dimension() {
        return dimension;
    }

    /** @return position X */
    @Override
    public double x() {
        return x;
    }

    /** @return position Y */
    @Override
    public double y() {
        return y;
    }

    /** @return position Z */
    @Override
    public double z() {
        return z;
    }

    /** @return velocity X, blocks per tick */
    @Override
    public double vx() {
        return vx;
    }

    /** @return velocity Y, blocks per tick */
    @Override
    public double vy() {
        return vy;
    }

    /** @return velocity Z, blocks per tick */
    @Override
    public double vz() {
        return vz;
    }

    /** @return the missile tier, 1 to 3 */
    @Override
    public int tier() {
        return tier;
    }

    /** @return the predicted impact X */
    @Override
    public double impactX() {
        return impactX;
    }

    /** @return the predicted impact Z */
    @Override
    public double impactZ() {
        return impactZ;
    }

    /** @return the speed, blocks per tick */
    @Override
    public double speed() {
        return Math.sqrt(vx * vx + vy * vy + vz * vz);
    }

    /**
     * @param centerX the protected center X
     * @param centerZ the protected center Z
     * @param radius  the protected radius, blocks
     * @return true when the predicted impact point is inside that circle (D14)
     */
    @Override
    public boolean incoming(double centerX, double centerZ, double radius) {
        return ContactMath.incoming(impactX, impactZ, centerX, centerZ, radius);
    }

    /** @return the flat distance from this contact to a point */
    public double distanceTo(double px, double pz) {
        return ContactMath.flat(x, z, px, pz);
    }

    @Override
    public String toString() {
        return "Contact#" + id
            + "{T"
            + tier
            + " @"
            + (int) x
            + ","
            + (int) y
            + ","
            + (int) z
            + " -> "
            + (int) impactX
            + ","
            + (int) impactZ
            + "}";
    }
}
