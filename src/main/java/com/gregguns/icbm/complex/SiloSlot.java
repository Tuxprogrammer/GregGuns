package com.gregguns.icbm.complex;

/**
 * A real Missile Silo structure placed at {@code origin} (its footprint box in complex-local coordinates), with a shaft
 * to the sky at its center. The generator stamps the silo's own structure definition into the box, so the footprint
 * here is a reservation, sized {@link ComplexLayout#SILO_SIZE} until the structure's real size is wired in.
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
