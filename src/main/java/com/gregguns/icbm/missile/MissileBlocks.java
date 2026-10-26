package com.gregguns.icbm.missile;

/**
 * Holds the registered missile body blocks. Set once by {@link MissileRegistration}; null before it runs. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md
public final class MissileBlocks {

    private static final BlockMissileBody[] BODIES = new BlockMissileBody[MissileTier.values().length];
    private static BlockMissileFiller filler;

    private MissileBlocks() {}

    static void set(BlockMissileBody[] bodies, BlockMissileFiller fill) {
        System.arraycopy(bodies, 0, BODIES, 0, BODIES.length);
        filler = fill;
    }

    /** @return the body block of a tier; null before registration */
    public static BlockMissileBody body(MissileTier tier) {
        return BODIES[tier.ordinal()];
    }

    /** @return the filler block; null before registration */
    public static BlockMissileFiller filler() {
        return filler;
    }

    /** @return true once the content is registered */
    public static boolean registered() {
        return filler != null;
    }
}
