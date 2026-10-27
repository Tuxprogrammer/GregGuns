package com.gregguns.icbm.emp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Keeps the {@code limit} nearest items out of a stream of candidates, in one pass and without sorting them all: a
 * bounded max-heap, O(n log limit). A dense base can offer thousands of tiles; only the cap survive (D16). Ties keep
 * the earlier candidate, so the result does not depend on hash order when the caller feeds a stable order. No
 * Minecraft types, both sides.
 *
 * @param <T> the candidate type
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (device cap)
public final class NearestPicker<T> {

    private static final class Entry<T> {

        final T item;
        final double distanceSq;
        final long order;

        Entry(T item, double distanceSq, long order) {
            this.item = item;
            this.distanceSq = distanceSq;
            this.order = order;
        }
    }

    private final int limit;
    private final PriorityQueue<Entry<T>> heap;
    private long seen;

    /** @param limit how many to keep; below 1 keeps none */
    public NearestPicker(int limit) {
        this.limit = Math.max(0, limit);
        // Farthest (and latest on a tie) on top, so it is the one to evict.
        this.heap = new PriorityQueue<>(
            Math.max(1, this.limit + 1),
            Comparator.<Entry<T>>comparingDouble(e -> -e.distanceSq)
                .thenComparingLong(e -> -e.order));
    }

    /**
     * @param item       a candidate
     * @param distanceSq its squared distance from the pulse
     */
    public void offer(T item, double distanceSq) {
        Entry<T> e = new Entry<>(item, distanceSq, seen++);
        if (limit == 0) {
            return;
        }
        if (heap.size() < limit) {
            heap.add(e);
        } else if (distanceSq < heap.peek().distanceSq) {
            heap.poll();
            heap.add(e);
        }
    }

    /** @return how many candidates were offered */
    public long offered() {
        return seen;
    }

    /** @return the kept items, nearest first */
    public List<T> nearestFirst() {
        List<Entry<T>> all = new ArrayList<>(heap);
        all.sort(
            Comparator.<Entry<T>>comparingDouble(e -> e.distanceSq)
                .thenComparingLong(e -> e.order));
        List<T> out = new ArrayList<>(all.size());
        for (Entry<T> e : all) {
            out.add(e.item);
        }
        return out;
    }
}
