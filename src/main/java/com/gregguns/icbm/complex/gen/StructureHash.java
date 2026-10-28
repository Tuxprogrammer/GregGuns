package com.gregguns.icbm.complex.gen;

/** SplitMix64 finalizer, for position-derived choices that must not depend on window order. */
final class StructureHash {

    private StructureHash() {}

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
