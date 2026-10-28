package com.gregguns.icbm.complex;

/**
 * A real Missile Silo structure (5 x 5 x 12, the controller in the north face) at {@code bounds}, with a 3 x 3 shaft
 * to the sky above its center. The generator stamps the silo's own structure shape into the box.
 */
public final class SiloSlot {

    private final Box bounds;
    private final SurfaceVariant variant;
    private final MissingPart missing;

    SiloSlot(Box bounds, SurfaceVariant variant, MissingPart missing) {
        this.bounds = bounds;
        this.variant = variant;
        this.missing = missing;
    }

    public Box bounds() {
        return bounds;
    }

    public SurfaceVariant variant() {
        return variant;
    }

    public MissingPart missing() {
        return missing;
    }

    public int shaftX() {
        return bounds.centerX();
    }

    public int shaftZ() {
        return bounds.centerZ();
    }
}
