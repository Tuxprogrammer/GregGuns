package com.gregguns.client.anim;

/** Camo-texture index selection that never goes out of range (GVA-18). */
public final class CamoIndex {

    private CamoIndex() {}

    /**
     * @param raw   the {@code camo} byte from the stack NBT, possibly garbage
     * @param count the number of textures the gun has (at least 1)
     * @return {@code raw} if it is a valid index, else 0
     */
    public static int clamp(int raw, int count) {
        return raw >= 0 && raw < count ? raw : 0;
    }
}
