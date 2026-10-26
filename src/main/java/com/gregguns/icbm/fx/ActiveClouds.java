package com.gregguns.icbm.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The clouds a client is animating: a bounded list that refreshes a cloud it already has (the server re-announces a
 * running cloud, ID by place and kind) and drops the oldest when full. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Cost bounds)
public final class ActiveClouds {

    /** One running cloud. */
    public static final class Cloud {

        public final CloudKind kind;
        public final double x;
        public final double y;
        public final double z;
        public final int radius;
        public final int life;
        public int age;
        public int ticksLeft;

        Cloud(CloudKind kind, double x, double y, double z, int radius, int life, int ticksLeft) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.life = life;
            this.ticksLeft = ticksLeft;
            this.age = life - ticksLeft;
        }
    }

    private final List<Cloud> clouds = new ArrayList<>();

    /**
     * Adds a cloud, or refreshes the one already at that place with that kind.
     *
     * @param kind      the kind
     * @param x         center X
     * @param y         center Y
     * @param z         center Z
     * @param radius    blocks
     * @param life      the cloud's whole length, ticks
     * @param ticksLeft ticks it still has
     */
    public void add(CloudKind kind, double x, double y, double z, int radius, int life, int ticksLeft) {
        for (Cloud c : clouds) {
            if (c.kind == kind && Math.abs(c.x - x) < 1.0 && Math.abs(c.y - y) < 1.0 && Math.abs(c.z - z) < 1.0) {
                c.ticksLeft = Math.max(c.ticksLeft, ticksLeft);
                return;
            }
        }
        if (clouds.size() >= CloudPlan.MAX_ACTIVE) {
            clouds.remove(0);
        }
        clouds.add(new Cloud(kind, x, y, z, radius, life, ticksLeft));
    }

    /** Ages every cloud by one tick and drops the ones that ended. */
    public void tick() {
        for (Iterator<Cloud> it = clouds.iterator(); it.hasNext();) {
            Cloud c = it.next();
            c.age++;
            if (--c.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    /** @return the running clouds; a read-only view */
    public List<Cloud> clouds() {
        return java.util.Collections.unmodifiableList(clouds);
    }

    /** Forgets every cloud (a dimension change or a disconnect). */
    public void clear() {
        clouds.clear();
    }

    /** @return how many clouds run */
    public int size() {
        return clouds.size();
    }
}
