package com.gregguns.icbm.detect;

/**
 * A read-only view of one tracked missile contact. {@link Contact} implements it; adapters (the OpenComputers
 * component) and tests can implement it over other data. The accessor names are stable.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Contacts)
public interface ContactSource {

    /** @return the missile's entity id */
    int id();

    /** @return the dimension id */
    int dimension();

    /** @return position X */
    double x();

    /** @return position Y */
    double y();

    /** @return position Z */
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

    /** @return the speed, blocks per tick */
    double speed();

    /**
     * @param centerX the protected center X
     * @param centerZ the protected center Z
     * @param radius  the protected radius
     * @return true when the predicted impact is inside the circle (D14)
     */
    boolean incoming(double centerX, double centerZ, double radius);
}
