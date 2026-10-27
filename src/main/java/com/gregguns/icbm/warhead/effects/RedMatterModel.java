package com.gregguns.icbm.warhead.effects;

/**
 * The numbers and the walk of a red matter body (D10, D132), as pure functions so every bound is unit tested. A body is
 * a ball of annihilation that grows one block of radius per pass, eats the blocks of each new shell once, pulls what is
 * near and annihilates what it touches, then idles when the ball is as big as it will get and ends after a bounded
 * life. No Minecraft types, both sides.
 *
 * <p>
 * Avoids (ICBM red matter, ICBM-02-80 to 02-96): ICBM-02-80 (the whole state is saved and restored), ICBM-02-81 (a
 * body that has nothing left to eat idles and ends), ICBM-02-82 and 02-84 (the radius is an integer capped by the
 * blast radius, never overwritten by a growth rule), ICBM-02-86 and 02-87 (no normalizing a zero vector, no division by
 * a zero distance), ICBM-02-88 and 02-90 (every edit is counted against a cap), ICBM-02-91 (no ray queue: a shell is
 * walked by a cursor, nothing is allocated per visited cell), ICBM-02-93 (sounds only on ignition and collapse).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/red-matter.md
public final class RedMatterModel {

    /** The radius a body starts at, blocks. */
    public static final int START_RADIUS = 2;
    /** Cube cells visited in one scheduler slice. */
    public static final int CELLS_PER_SLICE = 4096;
    /** Blocks one slice may remove. */
    public static final int EDITS_PER_SLICE = 32;
    /** How many edits a charged-machine cell counts for: eating one can set off a chain of GT machine explosions. */
    public static final int MACHINE_EDIT_WEIGHT = 8;
    /** Blocks one body may remove in one server tick, over all its slices. */
    public static final int EDITS_PER_TICK = 384;
    /** Ticks an idle body lingers before it ends (10 s). */
    public static final int IDLE_LINGER_TICKS = 200;
    /** Entities one body pulls or checks in a tick. */
    public static final int ENTITY_CAP = 128;
    /** The farthest an antimatter blast reaches to end a red matter body, blocks (ICBM's 221, rounded down). */
    public static final double ANTIMATTER_REACH = 200.0;
    /** Ticks between announcements of the ball to nearby clients. */
    public static final int ANNOUNCE_PERIOD = 20;
    /** The most speed the pull may give an entity, blocks per tick. */
    public static final double MAX_PULL_SPEED = 1.5;

    /** The body modes, saved as their ordinal. */
    public enum Mode {
        /** Growing and eating shell by shell. */
        SCAN,
        /** Full size, nothing left to scan: no work, only the pull and the kill. */
        IDLE
    }

    private RedMatterModel() {}

    /**
     * @param seconds the configured lifetime, seconds
     * @return the lifetime in ticks, at least 100 (5 s)
     */
    public static int lifetimeTicks(int seconds) {
        return Math.max(100, Math.min(600, seconds) * 20);
    }

    /**
     * @param blastRadius the blast radius at this fill, blocks
     * @return the largest radius the ball grows to: the blast radius, at least {@link #START_RADIUS}
     */
    public static int maxRadius(double blastRadius) {
        if (Double.isNaN(blastRadius) || blastRadius < START_RADIUS) {
            return START_RADIUS;
        }
        return (int) Math.floor(blastRadius);
    }

    /**
     * @param radius a ball radius
     * @return the edge of the cube around it, in cells
     */
    public static int edge(int radius) {
        return 2 * radius + 1;
    }

    /**
     * @param radius a ball radius
     * @return the number of cells in the cube around it
     */
    public static int cubeCells(int radius) {
        long e = edge(radius);
        return (int) Math.min(Integer.MAX_VALUE, e * e * e);
    }

    /**
     * @param dx    cell offset X
     * @param dy    cell offset Y
     * @param dz    cell offset Z
     * @param inner the radius already eaten (cells at or inside it are done)
     * @param outer the radius being eaten now
     * @return true when the cell is in the shell {@code inner < distance <= outer}
     */
    public static boolean inShell(int dx, int dy, int dz, int inner, int outer) {
        long d2 = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        return d2 > (long) inner * inner && d2 <= (long) outer * outer;
    }

    /**
     * Decodes a cube index into offsets, x fastest.
     *
     * @param index  0 to {@code cubeCells(radius) - 1}
     * @param radius the cube's radius
     * @return {dx, dy, dz}
     */
    public static int[] decode(int index, int radius) {
        int[] out = new int[3];
        decodeInto(index, radius, out);
        return out;
    }

    /**
     * Decodes a cube index into a caller-owned array, so a pass visits millions of cells without allocating.
     *
     * @param index  0 to {@code cubeCells(radius) - 1}
     * @param radius the cube's radius
     * @param out    receives dx, dy, dz
     */
    public static void decodeInto(int index, int radius, int[] out) {
        int e = edge(radius);
        out[0] = index % e - radius;
        out[1] = index / e % e - radius;
        out[2] = index / (e * e) - radius;
    }

    /**
     * @param radius the ball radius now
     * @param max    the radius it grows to
     * @return the radius of the next pass, or -1 when the ball is full size and the pass that just ended was its last
     */
    public static int nextRadius(int radius, int max) {
        return radius >= max ? -1 : radius + 1;
    }

    /**
     * @param radius a ball radius
     * @return the radius inside which everything is annihilated: the carved ball itself, at least 1.5 blocks
     */
    public static double killRadius(int radius) {
        return Math.max(1.5, radius);
    }

    /**
     * @param radius a ball radius
     * @return how far the pull reaches, blocks: twice the ball and at least 8 more than the kill radius
     */
    public static double pullRange(int radius) {
        return Math.max(2.0 * radius, killRadius(radius) + 8.0);
    }

    /**
     * The speed added toward the body in one tick to something {@code distance} away. Zero outside the range, at the
     * center (no direction to pull in, ICBM-02-87) and for a NaN; never above {@link #MAX_PULL_SPEED}.
     *
     * @param distance distance to the center, blocks
     * @param radius   the ball radius
     * @return blocks per tick, 0 to {@link #MAX_PULL_SPEED}
     */
    public static double pullSpeed(double distance, int radius) {
        double range = pullRange(radius);
        if (!(distance > 1.0e-3) || distance >= range) {
            return 0.0;
        }
        double falloff = 1.0 - distance / range;
        return Math.min(MAX_PULL_SPEED, falloff * (0.04 + 0.006 * radius));
    }

    /**
     * @param speedX current velocity X
     * @param speedY current velocity Y
     * @param speedZ current velocity Z
     * @return the factor (at most 1) that brings the speed down to {@link #MAX_PULL_SPEED}; 1 for a zero or NaN vector
     */
    public static double speedClamp(double speedX, double speedY, double speedZ) {
        double s = Math.sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ);
        if (!(s > MAX_PULL_SPEED)) {
            return 1.0;
        }
        return MAX_PULL_SPEED / s;
    }
}
