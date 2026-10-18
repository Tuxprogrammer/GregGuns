package com.gregguns.recipe;

/**
 * Material amounts of gun assembly recipes, derived from the recipe rows so GT recycling returns what went in. The
 * callers pass GT's own per-prefix amounts. Spec: docs/gregguns-spec/02-guns/recipes.md. Pure, both sides.
 */
public final class GunComposition {

    private GunComposition() {}

    /**
     * Steel in the pistol's directly consumed GT inputs (GR-01: 2 plates, 4 screws, 1 small spring), excluding the
     * barrel and mechanical parts, which carry their own item data.
     *
     * @param plate       one plate's material amount
     * @param screw       one screw's material amount
     * @param springSmall one small spring's material amount
     * @return the steel in those inputs
     */
    public static long pistolDirectSteel(long plate, long screw, long springSmall) {
        return 2 * plate + 4 * screw + springSmall;
    }

    /**
     * Steel in the Sawed Off's directly consumed GT inputs (GR-03: 1 plate, 2 screws, 1 small spring), excluding the
     * two Iron Barrels, which carry their own item data.
     *
     * @param plate       steel in one plate
     * @param screw       steel in one screw
     * @param springSmall steel in one small spring
     * @return steel units
     */
    public static long sawedOffDirectSteel(long plate, long screw, long springSmall) {
        return plate + 2 * screw + springSmall;
    }

    /**
     * Steel in the Revolver's directly consumed GT inputs (GR-02: 2 plates, 4 screws, 1 small spring), excluding the
     * parts, which carry their own item data. Same figures as the pistol's, so the same sum.
     *
     * @param plate       steel in one plate
     * @param screw       steel in one screw
     * @param springSmall steel in one small spring
     * @return steel units
     */
    public static long revolverDirectSteel(long plate, long screw, long springSmall) {
        return pistolDirectSteel(plate, screw, springSmall);
    }

    /**
     * Steel in the Bolt Action Rifle's directly consumed GT inputs (GR-04: 2 plates, 4 screws, 1 small spring; the lens
     * is glass). Same figures as the pistol's, so the same sum.
     */
    public static long boltActionDirectSteel(long plate, long screw, long springSmall) {
        return pistolDirectSteel(plate, screw, springSmall);
    }

    /** Iron in an Iron Receiver (RC-13): three plates. */
    public static long ironReceiverIron(long plate) {
        return 3 * plate;
    }

    /** Steel in an Iron Receiver (RC-13): the two screws. */
    public static long ironReceiverSteel(long screw) {
        return 2 * screw;
    }

    /** Iron in an Iron Barrel (RC-04): one long rod and two rings. */
    public static long ironBarrelIron(long stickLong, long ring) {
        return stickLong + 2 * ring;
    }

    /** Steel in an Iron Barrel (RC-04): the two screws. */
    public static long ironBarrelSteel(long screw) {
        return 2 * screw;
    }

    /** Bronze in Mechanical Parts (RC-03): four small gears and one small spring. */
    public static long mechanical1Bronze(long gearGtSmall, long springSmall) {
        return 4 * gearGtSmall + springSmall;
    }
}
