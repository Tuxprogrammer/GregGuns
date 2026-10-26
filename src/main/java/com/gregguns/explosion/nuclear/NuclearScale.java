package com.gregguns.explosion.nuclear;

import com.gregguns.explosion.Annihilation;

/**
 * The one nuclear blast's scale rules, shared by the Nuke Launcher (radius 10) and the ICBM nuclear warhead (radius up
 * to 64 at a full casing), D12. Everything the two have in common is a function of the blast radius: the annihilation
 * kill zone and the fallout footprint. Pure, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class NuclearScale {

    /** The fallout disc reaches this many blast radii from ground zero, before the configured cap. */
    public static final double FALLOUT_RADIUS_FACTOR = 1.5;

    private NuclearScale() {}

    /**
     * @param blastRadius the blast radius, blocks
     * @return the radius inside which every living entity dies (D30), the shared {@link Annihilation} share
     */
    public static double killRadius(double blastRadius) {
        return Annihilation.killRadius(blastRadius);
    }

    /**
     * @param blastRadius the blast radius, blocks
     * @param maxRadius   the configured fallout radius cap, blocks
     * @return the radius of the fallout disc, at least 1 and never above {@code maxRadius}
     */
    public static int falloutRadius(double blastRadius, int maxRadius) {
        if (!(blastRadius > 0.0)) {
            return 1;
        }
        return (int) Math.max(1, Math.min(maxRadius, Math.ceil(blastRadius * FALLOUT_RADIUS_FACTOR)));
    }
}
