package com.gregguns.icbm.defense;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A radar's address as a data stick carries it (D13, D14): dimension and block position. A radar writes it; an
 * air-defense battery reads it to find the radar it listens to. A tag that is missing or damaged reads as no link,
 * never
 * as the world origin. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (Radar link)
public final class RadarLink {

    /** The tag the link lives under on the stick. */
    public static final String TAG = "gregguns_icbm_radar";
    private static final String K_DIM = "dim";
    private static final String K_X = "x";
    private static final String K_Y = "y";
    private static final String K_Z = "z";

    private final int dim;
    private final int x;
    private final int y;
    private final int z;

    /**
     * @param dim dimension id
     * @param x   block X
     * @param y   block Y
     * @param z   block Z
     */
    public RadarLink(int dim, int x, int y, int z) {
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** @return the dimension id */
    public int dim() {
        return dim;
    }

    /** @return block X */
    public int x() {
        return x;
    }

    /** @return block Y */
    public int y() {
        return y;
    }

    /** @return block Z */
    public int z() {
        return z;
    }

    /** @param stickTag the stick's tag, created by the caller if the stack had none */
    public void write(NBTTagCompound stickTag) {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger(K_DIM, dim);
        t.setInteger(K_X, x);
        t.setInteger(K_Y, y);
        t.setInteger(K_Z, z);
        stickTag.setTag(TAG, t);
    }

    /**
     * @param stickTag a stick's tag, may be null
     * @return the link, or null when there is none or it is malformed
     */
    public static RadarLink read(NBTTagCompound stickTag) {
        if (stickTag == null || !stickTag.hasKey(TAG, 10)) {
            return null;
        }
        NBTTagCompound t = stickTag.getCompoundTag(TAG);
        if (!t.hasKey(K_DIM, 3) || !t.hasKey(K_X, 3) || !t.hasKey(K_Y, 3) || !t.hasKey(K_Z, 3)) {
            return null;
        }
        return new RadarLink(t.getInteger(K_DIM), t.getInteger(K_X), t.getInteger(K_Y), t.getInteger(K_Z));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RadarLink)) {
            return false;
        }
        RadarLink l = (RadarLink) o;
        return dim == l.dim && x == l.x && y == l.y && z == l.z;
    }

    @Override
    public int hashCode() {
        return ((dim * 31 + x) * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
