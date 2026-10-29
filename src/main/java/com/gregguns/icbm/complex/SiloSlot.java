package com.gregguns.icbm.complex;

/**
 * A real Missile Silo structure (5 x 5 x 12) at {@code bounds}, its controller in the face toward the gallery hall,
 * with a 3 x 3 shaft to the sky above its center. The generator stamps the silo's own structure shape, turned to face
 * the hall, into the box.
 */
public final class SiloSlot {

    private final Box bounds;
    private final SurfaceVariant variant;
    private final MissingPart missing;
    private final int facing;

    SiloSlot(Box bounds, SurfaceVariant variant, MissingPart missing, int facing) {
        this.bounds = bounds;
        this.variant = variant;
        this.missing = missing;
        this.facing = facing;
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

    /** @return the way the controller faces (0 north, 1 east, 2 south, 3 west): toward the gallery hall */
    public int facing() {
        return facing;
    }

    public int shaftX() {
        return bounds.centerX();
    }

    public int shaftZ() {
        return bounds.centerZ();
    }

    /**
     * Complex-local x of a cell of the silo's piece.
     *
     * @param column 0 to 4 across the front
     * @param aisle  0 (the controller's face) to 4 (the back)
     */
    public int x(int column, int aisle) {
        switch (facing) {
            case 0:
                return bounds.minX + column;
            case 2:
                return bounds.minX + 4 - column;
            case 1:
                return bounds.minX + 4 - aisle;
            default:
                return bounds.minX + aisle;
        }
    }

    /** Complex-local z of a cell of the silo's piece; see {@link #x}. */
    public int z(int column, int aisle) {
        switch (facing) {
            case 0:
                return bounds.minZ + aisle;
            case 2:
                return bounds.minZ + 4 - aisle;
            case 1:
                return bounds.minZ + column;
            default:
                return bounds.minZ + 4 - column;
        }
    }

    /** @return complex-local x of the controller (y 2) */
    public int controllerX() {
        return x(2, 0);
    }

    /** @return complex-local z of the controller (y 2) */
    public int controllerZ() {
        return z(2, 0);
    }

    /** @return x of the walkway cell straight in front of the controller (in the hall's wall) */
    public int frontX() {
        return controllerX() + (facing == 1 ? 1 : facing == 3 ? -1 : 0);
    }

    /** @return z of the walkway cell straight in front of the controller */
    public int frontZ() {
        return controllerZ() + (facing == 2 ? 1 : facing == 0 ? -1 : 0);
    }
}
