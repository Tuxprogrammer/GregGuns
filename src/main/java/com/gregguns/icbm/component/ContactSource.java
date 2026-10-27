package com.gregguns.icbm.component;

/**
 * One tracked missile as the {@code gg_radar} component reads it. Coder A's {@code detect.Contact} has exactly these
 * accessors, so it implements this interface at integration; tests use a fake. No Minecraft types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (getContacts, GGI-01-06)
public interface ContactSource {

    /** @return the missile's id */
    int id();

    /** @return X, blocks */
    double x();

    /** @return Y, blocks */
    double y();

    /** @return Z, blocks */
    double z();

    /** @return velocity X, blocks per tick */
    double vx();

    /** @return velocity Y, blocks per tick */
    double vy();

    /** @return velocity Z, blocks per tick */
    double vz();

    /** @return the missile tier, 1 to 3 */
    int tier();

    /** @return the predicted impact X */
    double impactX();

    /** @return the predicted impact Z */
    double impactZ();

    /**
     * @param centerX the protected center X
     * @param centerZ the protected center Z
     * @param radius  the protected radius, blocks
     * @return true when the predicted impact point is inside the protected circle (D14)
     */
    boolean incoming(double centerX, double centerZ, double radius);
}
