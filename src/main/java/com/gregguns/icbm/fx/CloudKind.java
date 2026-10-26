package com.gregguns.icbm.fx;

/**
 * The kinds of client-side blast cloud (I2). Four gas clouds, each with an abstract color that names no real agent
 * (D56), and the smoke plume of a large conventional blast. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md
public enum CloudKind {

    /** The Chemical warhead: a sickly yellow. */
    CHEMICAL(0.85F, 0.80F, 0.20F, 1.0),
    /** The Debilitation warhead: violet. */
    DEBILITATION(0.55F, 0.30F, 0.80F, 1.0),
    /** The Contagious warhead: a dull green. */
    CONTAGIOUS(0.30F, 0.75F, 0.30F, 1.0),
    /** The Smoke warhead: dense gray smoke, twice the particles. */
    SMOKE(0.55F, 0.55F, 0.55F, 2.0),
    /** The plume of a large conventional blast: a dark column that rises. */
    PLUME(0.20F, 0.20F, 0.20F, 1.0);

    private final float red;
    private final float green;
    private final float blue;
    private final double density;

    CloudKind(float red, float green, float blue, double density) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.density = density;
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

    /** @return the particle count factor against the base rate */
    public double density() {
        return density;
    }

    /** @return true for the gas kinds, which fill a sphere; false for the plume, which is a column */
    public boolean isGas() {
        return this != PLUME;
    }

    /**
     * @param id a wire id
     * @return the kind, or null when the id is out of range
     */
    public static CloudKind byId(int id) {
        CloudKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
