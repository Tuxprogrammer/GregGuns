package com.gregguns.icbm.complex.blueprint;

/**
 * A direction in a room's own frame (D208): lateral {@code a} runs along the door wall, depth {@code d} runs from the
 * back wall (d 0) to the door wall (d {@code depth - 1}). {@link #AUTO} lets the kit choose when it commits the room
 * (see {@link Kit#commit()}).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D305
public enum Dir {

    /** Toward the door wall: +d. */
    TO_DOOR(0, 1),
    /** Toward the back wall: -d. */
    TO_BACK(0, -1),
    /** Along +a. */
    PLUS_A(1, 0),
    /** Along -a. */
    MINUS_A(-1, 0),
    /** Chosen by the kit at commit time. */
    AUTO(0, 0);

    /** The four real directions, in the order the kit tries them. */
    static final Dir[] REAL = { TO_DOOR, TO_BACK, PLUS_A, MINUS_A };

    /** One step in this direction. */
    public final int da, dd;

    Dir(int da, int dd) {
        this.da = da;
        this.dd = dd;
    }

    /** @return the opposite direction ({@link #AUTO} stays) */
    public Dir opposite() {
        switch (this) {
            case TO_DOOR:
                return TO_BACK;
            case TO_BACK:
                return TO_DOOR;
            case PLUS_A:
                return MINUS_A;
            case MINUS_A:
                return PLUS_A;
            default:
                return AUTO;
        }
    }

    /** @return the direction seen in a module mirrored across its columns: {@link #PLUS_A} and {@link #MINUS_A} swap */
    public Dir mirrored() {
        return this == PLUS_A ? MINUS_A : this == MINUS_A ? PLUS_A : this;
    }

    /**
     * @param side the room's door side (0 north, 1 east, 2 south, 3 west)
     * @return the world facing of this direction, as Minecraft's hanging direction (0 south, 1 west, 2 north, 3 east)
     */
    public int world(int side) {
        // Side 0 (north door): d grows to the north; side 1 east, 2 south, 3 west. a runs +x (sides 0, 2) or +z.
        int toDoor = new int[] { 2, 3, 0, 1 }[side & 3];
        int plusA = side == 0 || side == 2 ? 3 : 0;
        switch (this) {
            case TO_BACK:
                return (toDoor + 2) & 3;
            case PLUS_A:
                return plusA;
            case MINUS_A:
                return (plusA + 2) & 3;
            default:
                return toDoor;
        }
    }
}
