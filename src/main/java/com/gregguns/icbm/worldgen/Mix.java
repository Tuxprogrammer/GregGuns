package com.gregguns.icbm.worldgen;

/** The splitmix64 finalizer, shared by every seeded roll in the complex so no roll depends on call order. Pure. */
final class Mix {

    private Mix() {}

    /** @return a well-mixed value of {@code z} */
    static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** @return a mixed value of the seed and up to four salts */
    static long of(long seed, long a, long b, long c) {
        long h = mix(seed);
        h = mix(h ^ a * 0xC2B2AE3D27D4EB4FL);
        h = mix(h ^ b * 0x165667B19E3779F9L);
        return mix(h ^ c * 0x27D4EB2F165667C5L);
    }

    /** @return a value 0 (inclusive) to 1 (exclusive) from the hash */
    static double unit(long h) {
        return (h >>> 11) * (1.0 / (1L << 53));
    }
}
