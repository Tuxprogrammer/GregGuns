package com.gregguns.gun.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Power Hammer's shockwave as pure geometry: which block cells a charged release reaches and how hard it hits.
 * No Minecraft types; both sides.
 *
 * <p>
 * The wave is a cube {@link #SIZE} blocks on a side (the 4 x 4 collision box of the original) carried along the look
 * direction for {@link #REACH} blocks. It stops where it meets a solid block, and breaks every cell of the cube around
 * that point, so a wall is broken a little way in and a free swing breaks the cells at the end of the reach.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Power Hammer, section 4);
// docs/techguns-spec/04-projectiles-fx/projectiles.md (3.11 PowerHammerProjectile)
// Deviation: the original hammer blast never broke blocks (only the left-click mining did); the GregGuns shockwave
// breaks the cells of the box, through BlockBreakGuard, at one round of air per block.
public final class Shockwave {

    /** Side of the cube, blocks. */
    public static final int SIZE = 4;
    /** How far the wave travels from the eye, blocks. */
    public static final double REACH = 3.0;
    /** The wave center is never nearer the eye than this, so the box starts at the player. */
    public static final double MIN_CENTER = SIZE / 2.0;
    /** Damage per round of air spent, and the least damage of any release. */
    public static final double DAMAGE_PER_UNIT = 2.5;

    private Shockwave() {}

    /**
     * @param units rounds of air the release spent, 1 to 10
     * @return the raw damage to every entity in the box: {@code units x 2.5}, never under 2.5
     */
    public static double damage(int units) {
        return Math.max(1, units) * DAMAGE_PER_UNIT;
    }

    /**
     * Where the center of the cube sits.
     *
     * @param hitDistance distance from the eye to the first solid block on the way, or a negative number for none
     * @return the distance from the eye along the look direction, {@link #MIN_CENTER} to {@link #REACH}
     */
    public static double centerDistance(double hitDistance) {
        double d = hitDistance < 0 ? REACH : Math.min(REACH, hitDistance);
        return Math.max(MIN_CENTER, d);
    }

    /**
     * The block cells of the cube, nearest to the eye first (so a tank running dry loses the far ones).
     *
     * @param eyeX     eye X
     * @param eyeY     eye Y
     * @param eyeZ     eye Z
     * @param lookX    look direction X (unit length)
     * @param lookY    look direction Y
     * @param lookZ    look direction Z
     * @param distance the cube center's distance along the look, from {@link #centerDistance}
     * @return {@code SIZE^3} cells as {x, y, z}
     */
    public static List<int[]> cells(double eyeX, double eyeY, double eyeZ, double lookX, double lookY, double lookZ,
        double distance) {
        double cx = eyeX + lookX * distance;
        double cy = eyeY + lookY * distance;
        double cz = eyeZ + lookZ * distance;
        double half = SIZE / 2.0;
        int x0 = (int) Math.ceil(cx - half - 0.5);
        int y0 = (int) Math.ceil(cy - half - 0.5);
        int z0 = (int) Math.ceil(cz - half - 0.5);
        List<int[]> out = new ArrayList<>(SIZE * SIZE * SIZE);
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dy = 0; dy < SIZE; dy++) {
                for (int dz = 0; dz < SIZE; dz++) {
                    out.add(new int[] { x0 + dx, y0 + dy, z0 + dz });
                }
            }
        }
        out.sort(
            Comparator.comparingDouble(c -> sq(c[0] + 0.5 - eyeX) + sq(c[1] + 0.5 - eyeY) + sq(c[2] + 0.5 - eyeZ)));
        return out;
    }

    /**
     * The cube as a box for the entity hit test.
     *
     * @param eyeX     eye X
     * @param eyeY     eye Y
     * @param eyeZ     eye Z
     * @param lookX    look direction X
     * @param lookY    look direction Y
     * @param lookZ    look direction Z
     * @param distance the cube center's distance along the look
     * @return {minX, minY, minZ, maxX, maxY, maxZ}
     */
    public static double[] box(double eyeX, double eyeY, double eyeZ, double lookX, double lookY, double lookZ,
        double distance) {
        double half = SIZE / 2.0;
        double cx = eyeX + lookX * distance;
        double cy = eyeY + lookY * distance;
        double cz = eyeZ + lookZ * distance;
        return new double[] { cx - half, cy - half, cz - half, cx + half, cy + half, cz + half };
    }

    private static double sq(double v) {
        return v * v;
    }
}
