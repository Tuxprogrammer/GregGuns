package com.gregguns.projectile;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks the targets a chain of lightning jumps to. Pure: no Minecraft types, so every rule is unit-tested.
 *
 * <p>
 * From the point the bolt is at (first the primary target's center), the next target is the nearest eligible
 * candidate within range that the bolt can see; the bolt then moves to that candidate and repeats, up to
 * {@code maxJumps} times. A candidate is never taken twice, and none is taken when {@code maxJumps} is 0.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.6 TeslaProjectile); fixes PFX-08 (Techguns always
// chained at least once, with no alive or line-of-sight check) and GUN-21 (the chain settings were never read).
public final class ChainSelect {

    private ChainSelect() {}

    /** One entity the chain may jump to. */
    public static final class Candidate {

        final double x;
        final double y;
        final double z;
        final boolean alive;
        final boolean friendly;
        final boolean shooter;
        final boolean used;

        /**
         * @param x        center X
         * @param y        center Y
         * @param z        center Z
         * @param alive    false for a dead entity, which is skipped
         * @param friendly true for an ally of the shooter (same team, own pet), which is skipped
         * @param shooter  true for the shooter, who is skipped
         * @param used     true for an entity already hit by this shot (the primary target), which is skipped
         */
        public Candidate(double x, double y, double z, boolean alive, boolean friendly, boolean shooter, boolean used) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.alive = alive;
            this.friendly = friendly;
            this.shooter = shooter;
            this.used = used;
        }

        boolean eligible() {
            return alive && !friendly && !shooter && !used;
        }
    }

    /** Whether the bolt can see a candidate from a point. */
    @FunctionalInterface
    public interface Sight {

        /**
         * @param fromX     the point the bolt jumps from
         * @param fromY     point Y
         * @param fromZ     point Z
         * @param candidate the index in the candidate list
         * @return true when no wall is between the point and the candidate
         */
        boolean clear(double fromX, double fromY, double fromZ, int candidate);
    }

    /**
     * Selects the jump targets in order.
     *
     * @param startX     where the chain starts (the primary target's center)
     * @param startY     start Y
     * @param startZ     start Z
     * @param candidates every entity near the start
     * @param range      the longest single jump, blocks
     * @param maxJumps   how many targets to take at most
     * @param sight      the line-of-sight test
     * @return indices into {@code candidates}, in jump order; empty when {@code maxJumps} is 0 or none qualifies
     */
    public static List<Integer> select(double startX, double startY, double startZ, List<Candidate> candidates,
        double range, int maxJumps, Sight sight) {
        List<Integer> chosen = new ArrayList<>();
        boolean[] taken = new boolean[candidates.size()];
        double cx = startX;
        double cy = startY;
        double cz = startZ;
        for (int jump = 0; jump < maxJumps; jump++) {
            int best = -1;
            double bestSq = range * range;
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                if (taken[i] || !c.eligible()) {
                    continue;
                }
                double dx = c.x - cx;
                double dy = c.y - cy;
                double dz = c.z - cz;
                double sq = dx * dx + dy * dy + dz * dz;
                // Strictly nearer wins, so equal distances keep list order; out of range never qualifies.
                boolean nearer = best < 0 ? sq <= bestSq : sq < bestSq;
                if (nearer && sight.clear(cx, cy, cz, i)) {
                    best = i;
                    bestSq = sq;
                }
            }
            if (best < 0) {
                break;
            }
            taken[best] = true;
            chosen.add(best);
            Candidate c = candidates.get(best);
            cx = c.x;
            cy = c.y;
            cz = c.z;
        }
        return chosen;
    }

    /**
     * The damage the target of a jump takes: the base less a drop per step, never negative. Armor and any
     * friendly-fire factor apply afterwards, to this whole figure (fixes PFX-07, where only the drop was scaled).
     *
     * @param base        the shot's damage
     * @param dropPerJump damage removed per jump
     * @param jumpIndex   0 for the first jump
     * @return the damage, 0 or more
     */
    public static double damageAt(double base, double dropPerJump, int jumpIndex) {
        return Math.max(0.0, base - dropPerJump * (jumpIndex + 1));
    }
}
