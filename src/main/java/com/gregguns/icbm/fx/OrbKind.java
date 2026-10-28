package com.gregguns.icbm.fx;

/**
 * The glowing balls of the annihilation warheads (I5): the antimatter flash and the red matter body. Each is one
 * translucent sphere drawn by the client, not particles. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
public enum OrbKind {

    /** The antimatter flash: a white-cyan ball that swells to the blast radius and fades. */
    ANTIMATTER(0.75F, 0.95F, 1.0F, true),
    /** The red matter body: a dark ball with a red rim that follows the carved radius. */
    RED_MATTER(0.85F, 0.05F, 0.05F, false);

    private final float red;
    private final float green;
    private final float blue;
    private final boolean swells;

    OrbKind(float red, float green, float blue, boolean swells) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.swells = swells;
    }

    /** @return red, 0 to 1 */
    public float red() {
        return red;
    }

    /** @return green, 0 to 1 */
    public float green() {
        return green;
    }

    /** @return blue, 0 to 1 */
    public float blue() {
        return blue;
    }

    /** @return true when the ball grows from nothing to its radius over its life (a flash), false when it keeps it */
    public boolean swells() {
        return swells;
    }

    /**
     * @param id a wire id
     * @return the kind, or null when out of range
     */
    public static OrbKind byId(int id) {
        OrbKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
