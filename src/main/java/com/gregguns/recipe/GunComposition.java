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
}
