package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Hands out the blocks of one large blast in batches so a nuke-size explosion (radius 10, thousands of blocks) never
 * stalls a server tick. Pure: block positions are {@code int[]{x, y, z}}, no Minecraft types, both sides.
 *
 * <p>
 * Usage: {@code BlastSlicer.sortNearestFirst(list, cx, cy, cz)}, then {@code new BlastSlicer(list, cap)} and call
 * {@link #next()} once per tick until {@link #done()}.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md (nuke scale)
public final class BlastSlicer {

    private final List<int[]> positions;
    private final int cap;
    private int cursor;

    /**
     * @param positions the blocks still to break, in the order they should go
     * @param cap       most blocks returned per {@link #next()} call, at least 1
     */
    public BlastSlicer(List<int[]> positions, int cap) {
        this.positions = new ArrayList<>(positions);
        this.cap = Math.max(1, cap);
    }

    /**
     * Orders blocks by squared distance from the blast centre, so the fireball grows outward over the ticks.
     *
     * @param positions the list to sort in place
     * @param cx        centre X
     * @param cy        centre Y
     * @param cz        centre Z
     */
    public static void sortNearestFirst(List<int[]> positions, double cx, double cy, double cz) {
        Collections.sort(positions, new Comparator<int[]>() {

            @Override
            public int compare(int[] a, int[] b) {
                return Double.compare(dist(a, cx, cy, cz), dist(b, cx, cy, cz));
            }
        });
    }

    private static double dist(int[] p, double cx, double cy, double cz) {
        double dx = p[0] + 0.5 - cx;
        double dy = p[1] + 0.5 - cy;
        double dz = p[2] + 0.5 - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** @return the next batch, at most the cap, empty when finished */
    public List<int[]> next() {
        int end = Math.min(positions.size(), cursor + cap);
        List<int[]> batch = new ArrayList<>(positions.subList(cursor, end));
        cursor = end;
        return batch;
    }

    /** @return true when every block has been handed out */
    public boolean done() {
        return cursor >= positions.size();
    }

    /** @return blocks not yet handed out */
    public int remaining() {
        return positions.size() - cursor;
    }

    /**
     * Keeps the blocks a protection listener allowed (PFX-14: a cancelled {@code BlockEvent.BreakEvent} keeps its
     * block).
     *
     * @param positions candidate blocks
     * @param allowed   asks the listeners about one block; false means cancelled
     * @return the blocks that may be broken, in the same order
     */
    public static List<int[]> keepAllowed(List<int[]> positions, java.util.function.Predicate<int[]> allowed) {
        List<int[]> kept = new ArrayList<>(positions.size());
        for (int[] p : positions) {
            if (allowed.test(p)) {
                kept.add(p);
            }
        }
        return kept;
    }
}
