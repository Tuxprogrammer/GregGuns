package com.gregguns.worldgen.layout;

/**
 * How a block's metadata turns when a layout is stamped rotated. A layout is authored facing rotation 0; rotation
 * {@code q} turns it {@code q} quarter turns clockwise seen from above (east becomes south). Most blocks are
 * rotation-neutral ({@link #NONE}); the ones that carry a facing in their metadata name a kind in the palette
 * ({@code @stairs}) so the facing follows the structure (WGEN-31 was a door that did not).
 *
 * <p>
 * Each cycle lists the facing values in clockwise order, so rotating by {@code q} is "move {@code q} places along
 * the cycle". Bits outside the mask (open flags, upside-down bits, hinge) are kept. A value not in the cycle (for
 * example a piston pointing up) is left alone.
 */
public enum RotationKind {

    /** Rotation-neutral: metadata is used as authored. */
    NONE("none", 0, null),
    /** Chests, furnaces, ladders, wall signs, dispensers: 2 north, 5 east, 3 south, 4 west. */
    FACING("facing", 7, new int[] { 2, 5, 3, 4 }),
    /** Stairs: 0 east, 2 south, 1 west, 3 north; bit 4 (upside down) kept. */
    STAIRS("stairs", 3, new int[] { 0, 2, 1, 3 }),
    /** Wall torches: 1 east, 3 south, 2 west, 4 north. */
    TORCH("torch", 7, new int[] { 1, 3, 2, 4 }),
    /** Doors (either half's low bits): 0 west, 1 north, 2 east, 3 south; open and half bits kept. */
    DOOR("door", 3, new int[] { 0, 1, 2, 3 }),
    /** Trapdoors: 0 north, 3 east, 1 south, 2 west; open and top bits kept. */
    TRAPDOOR("trapdoor", 3, new int[] { 0, 3, 1, 2 }),
    /** Log-like axis in bits 4 and 8: x and z swap on an odd number of quarter turns. */
    AXIS("axis", 12, null);

    private final String token;
    private final int mask;
    private final int[] cycle;

    RotationKind(String token, int mask, int[] cycle) {
        this.token = token;
        this.mask = mask;
        this.cycle = cycle;
    }

    /** The name used after {@code @} in a palette entry. */
    public String token() {
        return token;
    }

    /** Looks a kind up by its palette token; null when unknown. */
    public static RotationKind byToken(String token) {
        for (RotationKind k : values()) {
            if (k.token.equals(token)) {
                return k;
            }
        }
        return null;
    }

    /** Rotates {@code meta} by {@code quarterTurns} (any int; taken modulo 4) clockwise. */
    public int rotate(int meta, int quarterTurns) {
        int q = quarterTurns & 3;
        if (q == 0 || this == NONE) {
            return meta;
        }
        if (this == AXIS) {
            int axis = meta & mask;
            if ((q & 1) == 0 || axis == 0 || axis == 12) {
                return meta;
            }
            return (meta & ~mask) | (axis == 4 ? 8 : 4);
        }
        int v = meta & mask;
        for (int i = 0; i < 4; i++) {
            if (cycle[i] == v) {
                return (meta & ~mask) | cycle[(i + q) & 3];
            }
        }
        return meta;
    }
}
