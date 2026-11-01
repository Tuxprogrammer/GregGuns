package com.gregguns.icbm.complex.gen;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.SurfaceVariant;
import com.gregguns.worldgen.HeightProbe;
import com.gregguns.worldgen.PopulateWindow;

/**
 * One silo's mouth as the writer left it (D319): the 5 x 5 shaft footprint, the pad's height, the surface variant and,
 * per column, the top the shaft was written to in that column's own window. The finish pass ({@link SiloMouthKeeper})
 * reads it once every populate that could still write over the mouth has run, and puts the bore and the cap back to
 * what the writer meant, whatever grew there in between.
 *
 * <p>
 * Pure data and rules: {@link #want} says what a cell of the mouth must be, so the order of populates never changes
 * the answer.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Depth, stamp and surface: the mouth finish)
public final class SiloMouth {

    /** A column whose window has not written yet. */
    public static final int UNKNOWN = Integer.MIN_VALUE;
    /** Shaft footprint side. */
    public static final int SIDE = ComplexLayout.SILO_SIZE;
    /** Highest block a mouth's air column reaches. */
    public static final int BUILD_HEIGHT = 255;
    /**
     * Chunks past the populates that may legally write over the footprint (the four chunks each populate has loaded)
     * that must have populated too: a mod that writes a little past its window still lands before the finish.
     */
    public static final int MARGIN_CHUNKS = 1;

    /** What one cell of the mouth must be. */
    public enum Want {
        /** Left as it is: under the pad, the ring's own blocks, or a column with no recorded top. */
        KEEP,
        /** Air, whatever is there (bore, silo interior, a closed mouth's open cells). */
        AIR,
        /** The concrete cap. */
        CAP,
        /** Natural cover (plants, vines, snow, leaves, logs, ice) is removed; anything else stays. */
        CLEAR_COVER
    }

    private final int sx, sz, padY;
    private final SurfaceVariant variant;
    private final int[] tops = new int[SIDE * SIDE];

    /**
     * @param sx      world x of the footprint's west column
     * @param sz      world z of the footprint's north column
     * @param padY    world y of the launch pad (the level floor)
     * @param variant the mouth variant
     */
    public SiloMouth(int sx, int sz, int padY, SurfaceVariant variant) {
        this.sx = sx;
        this.sz = sz;
        this.padY = padY;
        this.variant = variant;
        Arrays.fill(tops, UNKNOWN);
    }

    public int sx() {
        return sx;
    }

    public int sz() {
        return sz;
    }

    public int padY() {
        return padY;
    }

    public SurfaceVariant variant() {
        return variant;
    }

    /** @return whether the column lies in the footprint */
    public boolean contains(int x, int z) {
        return x >= sx && x < sx + SIDE && z >= sz && z < sz + SIDE;
    }

    /**
     * Records the top the writer used in a column.
     *
     * @param top the column's shaft top, or {@link HeightProbe#NONE} for a column under water (left alone)
     * @return whether this changed the record
     */
    public boolean record(int x, int z, int top) {
        int i = (x - sx) * SIDE + (z - sz);
        if (tops[i] == top) return false;
        tops[i] = top;
        return true;
    }

    /** @return the recorded top of a column, {@link #UNKNOWN} or {@link HeightProbe#NONE} */
    public int top(int x, int z) {
        return tops[(x - sx) * SIDE + (z - sz)];
    }

    /** @return whether every column's window has written */
    public boolean complete() {
        for (int t : tops) if (t == UNKNOWN) return false;
        return true;
    }

    /** @return whether the column is one of the nine bore columns */
    public boolean bore(int x, int z) {
        return x > sx && x < sx + SIDE - 1 && z > sz && z < sz + SIDE - 1;
    }

    /** @return whether the bore column carries the concrete cap at its top */
    public boolean capped(int x, int z) {
        return capped(variant, sx, sz, x, z);
    }

    /**
     * Open: no cap. Half open: the west half of the bore (its two western columns). Closed: every bore cell but two
     * of the eight around the center, which stay open to the sky (D17).
     */
    static boolean capped(SurfaceVariant v, int sx, int sz, int x, int z) {
        switch (v) {
            case OPEN:
                return false;
            case HALF_OPEN:
                return x <= sx + 2;
            default:
                return !hole(sx, sz, x, z);
        }
    }

    /** @return whether a closed mouth leaves this bore cell open to the sky (two of the eight non-center cells) */
    static boolean hole(int sx, int sz, int x, int z) {
        int cell = (x - sx - 1) * 3 + (z - sz - 1);
        if (cell == 4) return false;
        long h = StructureHash.mix(sx * 31L + sz);
        int hole1 = (int) Math.floorMod(h, 8), hole2 = (int) Math.floorMod(h >>> 8, 8);
        int idx = cell > 4 ? cell - 1 : cell; // index among the eight non-center cells
        return idx == hole1 || idx == hole2;
    }

    /**
     * What a cell of the mouth must be once the finish has run. A bore column is air from just over the pad to its
     * top, the cap at the top where the variant caps it, and above that nothing natural (a closed mouth's open cells
     * are air to build height). A ring column keeps its concrete and loses any cover over its top.
     */
    public Want want(int x, int y, int z) {
        if (!contains(x, z)) return Want.KEEP;
        int t = top(x, z);
        if (t == UNKNOWN || t == HeightProbe.NONE || y > BUILD_HEIGHT) return Want.KEEP;
        if (!bore(x, z)) return y > t ? Want.CLEAR_COVER : Want.KEEP;
        if (y <= padY) return Want.KEEP;
        if (y < t) return Want.AIR;
        boolean cap = capped(x, z);
        if (y == t) return cap ? Want.CAP : Want.AIR;
        if (variant == SurfaceVariant.CLOSED && !cap) return Want.AIR;
        return Want.CLEAR_COVER;
    }

    /** @return the first chunk x whose populate must have run before the finish */
    public int minChunkX() {
        return reachMin(sx);
    }

    /** @return the last chunk x whose populate must have run before the finish */
    public int maxChunkX() {
        return reachMax(sx);
    }

    public int minChunkZ() {
        return reachMin(sz);
    }

    public int maxChunkZ() {
        return reachMax(sz);
    }

    /**
     * A populate for chunk c may write blocks c*16 to c*16+31 (its four loaded chunks, GGS-11-08), so a column at x is
     * reached by chunks {@code chunkOf(x) - 1} and {@code chunkOf(x)}; one more chunk on each side covers mods that
     * reach past their window.
     */
    private static int reachMin(int s) {
        return PopulateWindow.chunkOf(s) - 1 - MARGIN_CHUNKS;
    }

    private static int reachMax(int s) {
        return PopulateWindow.chunkOf(s + SIDE - 1) + MARGIN_CHUNKS;
    }

    /** Writes the record for the saved ledger. */
    public NBTTagCompound toNbt() {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("x", sx);
        t.setInteger("z", sz);
        t.setInteger("pad", padY);
        t.setByte("v", (byte) variant.ordinal());
        t.setIntArray("tops", tops.clone());
        return t;
    }

    /** @return the record read back, or null for a malformed one */
    public static SiloMouth fromNbt(NBTTagCompound t) {
        int v = t.getByte("v");
        int[] tops = t.getIntArray("tops");
        if (v < 0 || v >= SurfaceVariant.values().length || tops.length != SIDE * SIDE) return null;
        SiloMouth m = new SiloMouth(
            t.getInteger("x"),
            t.getInteger("z"),
            t.getInteger("pad"),
            SurfaceVariant.values()[v]);
        System.arraycopy(tops, 0, m.tops, 0, tops.length);
        return m;
    }
}
