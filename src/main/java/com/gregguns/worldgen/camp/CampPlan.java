package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The pure result of subdividing a Military Base: footprint size, leaves with their parts, roads, the flag spot, the
 * spawn points and the NPC names. All coordinates are camp-local (x east, z south, origin at the footprint's minimum
 * corner, y 0 is the first air block above the ground). No Minecraft types and no world access: the same seed always
 * gives the same plan, which is what lets any populate window rebuild its slice (GGS-11-10).
 *
 * <p>
 * Rules (camp_parts.md): the footprint is 32 to 79 on each axis; the usable area is the footprint inset by 2; it
 * splits along its longer axis into 2 parts at the root and 2 or more below; parts get random weights 1 to 2 and a
 * road lane separates neighbors. A base whose short side is under {@link #ABORT_BELOW} aborts ({@link #generate}
 * returns null). The flag sits at the center of the leaf nearest the footprint center and that leaf holds no building.
 * Every other leaf rolls a corner, border or regular part (or the cobblestone fallback).
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/camp_parts.md (Base rules); WGEN-32 (camo is chosen at write
// time from the base center's biome types, not here)
public final class CampPlan {

    public static final int MIN_SIDE = 32;
    public static final int MAX_SIDE = 79;
    /** A footprint with a side below this is not built. */
    public static final int ABORT_BELOW = 5;
    /** The usable area starts this far inside the footprint. */
    public static final int INSET = 2;
    /** Root roads run this many blocks past the fence. */
    public static final int ROAD_TAIL = 3;
    /** Chance that each extra child (beyond two) is added to a split, below the root. */
    private static final double EXTRA_CHILD_CHANCE = 0.3;

    private static final long SIZE_SALT = 0x5DEECE66DL;
    private static final long BSP_SALT = 0x2545F4914F6CDD1DL;

    /** The NPC registry names the flag spawns for a base (N8 matches them through {@code NpcMob.byName}). */
    public static final List<String> BASE_NPC_NAMES = Collections.singletonList("ArmySoldier");

    private final long seed;
    private final int sizeX;
    private final int sizeZ;
    private final List<CampLeaf> leaves;
    private final List<CampRoad> roads;
    private final int flagLeafIndex;
    private final int flagX;
    private final int flagZ;
    private final int[] spawnPoints;

    private CampPlan(long seed, int sizeX, int sizeZ, List<CampLeaf> leaves, List<CampRoad> roads, int flagLeafIndex,
        int flagX, int flagZ, int[] spawnPoints) {
        this.seed = seed;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.leaves = Collections.unmodifiableList(leaves);
        this.roads = Collections.unmodifiableList(roads);
        this.flagLeafIndex = flagLeafIndex;
        this.flagX = flagX;
        this.flagZ = flagZ;
        this.spawnPoints = spawnPoints;
    }

    /** The footprint a seed gives: {@code {sizeX, sizeZ}}, each 32..79. The generator needs it before the plan. */
    public static int[] sizeFor(long seed) {
        Random r = new Random(seed ^ SIZE_SALT);
        int range = MAX_SIDE - MIN_SIDE + 1;
        int sx = MIN_SIDE + r.nextInt(range);
        int sz = MIN_SIDE + r.nextInt(range);
        return new int[] { sx, sz };
    }

    /** The plan for a seed at the size {@link #sizeFor} gives. */
    public static CampPlan generate(long seed) {
        int[] size = sizeFor(seed);
        return generate(seed, size[0], size[1]);
    }

    /**
     * @return the plan, or null when either side is below {@link #ABORT_BELOW}
     */
    public static CampPlan generate(long seed, int sizeX, int sizeZ) {
        if (sizeX < ABORT_BELOW || sizeZ < ABORT_BELOW) {
            return null;
        }
        return new Builder(seed, sizeX, sizeZ).build();
    }

    public long seed() {
        return seed;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public List<CampLeaf> leaves() {
        return leaves;
    }

    public List<CampRoad> roads() {
        return roads;
    }

    public CampLeaf flagLeaf() {
        return leaves.get(flagLeafIndex);
    }

    /** Camp-local flag column. The flag sits at local y 0. */
    public int flagX() {
        return flagX;
    }

    public int flagZ() {
        return flagZ;
    }

    /** Flat (x, y, z) triples, camp-local, one per road center. */
    public int[] spawnPoints() {
        return spawnPoints.clone();
    }

    /** The NPC cap for this base at a cap factor; see {@link CampCap}. */
    public int cap(float factor) {
        return CampCap.cap(sizeX, sizeZ, factor);
    }

    /** True for a perimeter column that holds fence, i.e. not an opening a root road makes. */
    public boolean isFence(int lx, int lz) {
        if (lx < 0 || lz < 0 || lx >= sizeX || lz >= sizeZ) {
            return false;
        }
        if (lx != 0 && lz != 0 && lx != sizeX - 1 && lz != sizeZ - 1) {
            return false;
        }
        for (CampRoad r : roads) {
            if (r.isRoot() && r.contains(lx, lz)) {
                return false;
            }
        }
        return true;
    }

    /** Puts the plan in the world: anchor is the rotated footprint's minimum corner. */
    public PlacedCamp place(int anchorX, int anchorY, int anchorZ, int rotation) {
        return new PlacedCamp(this, anchorX, anchorY, anchorZ, rotation);
    }

    /** The subdivision, written as a class so the plan itself stays immutable. */
    private static final class Builder {

        private final long seed;
        private final int sx;
        private final int sz;
        private final Random rng;
        private final int minSide;
        private final int maxSide;
        private final int rootRoad;
        private final List<CampLeaf> leaves = new ArrayList<>();
        private final List<CampRoad> roads = new ArrayList<>();

        Builder(long seed, int sx, int sz) {
            this.seed = seed;
            this.sx = sx;
            this.sz = sz;
            this.rng = new Random(seed ^ BSP_SALT);
            int shortSide = Math.min(sx, sz);
            this.minSide = shortSide < 10 ? 5 : 7;
            this.maxSide = shortSide <= 45 ? 15 : shortSide / 3;
            this.rootRoad = Math.max(1, Math.min(5, Math.max(sx, sz) / 8));
        }

        CampPlan build() {
            split(INSET, INSET, sx - 2 * INSET, sz - 2 * INSET, 0, 0);
            int flagIdx = nearestToCenter();
            List<CampLeaf> done = new ArrayList<>(leaves.size());
            for (int i = 0; i < leaves.size(); i++) {
                CampLeaf leaf = leaves.get(i);
                done.add(i == flagIdx ? leaf.with(null, 0, true) : assign(leaf));
            }
            CampLeaf fl = done.get(flagIdx);
            int fx = fl.x + fl.w / 2;
            int fz = fl.z + fl.d / 2;
            return new CampPlan(seed, sx, sz, done, roads, flagIdx, fx, fz, spawnPoints(fx, fz));
        }

        private int nearestToCenter() {
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < leaves.size(); i++) {
                CampLeaf l = leaves.get(i);
                double dx = l.centerX() - sx / 2.0;
                double dz = l.centerZ() - sz / 2.0;
                double dist = dx * dx + dz * dz;
                if (dist < bestD) {
                    bestD = dist;
                    best = i;
                }
            }
            return best;
        }

        private int[] spawnPoints(int fx, int fz) {
            if (roads.isEmpty()) {
                return new int[] { fx, 0, fz }; // an unsplit base has no road; stand at the flag
            }
            int[] out = new int[roads.size() * 3];
            for (int i = 0; i < roads.size(); i++) {
                CampRoad r = roads.get(i);
                out[i * 3] = Math.max(0, Math.min(sx - 1, r.x + r.w / 2));
                out[i * 3 + 1] = 0;
                out[i * 3 + 2] = Math.max(0, Math.min(sz - 1, r.z + r.d / 2));
            }
            return out;
        }

        private boolean shouldSplit(int x, int z, int w, int d, int depth) {
            int longer = Math.max(w, d);
            int roadW = Math.max(1, rootRoad - depth);
            if (longer < 2 * minSide + roadW) {
                return false; // a part this small never splits
            }
            if (depth == 0 || longer > maxSide) {
                return true;
            }
            double p = (longer - minSide) / (double) Math.max(1, maxSide - minSide);
            double dx = (x + w / 2.0 - sx / 2.0) / (sx / 2.0);
            double dz = (z + d / 2.0 - sz / 2.0) / (sz / 2.0);
            double dist = Math.min(1.0, Math.sqrt(dx * dx + dz * dz));
            p = Math.min(1.0, p * (1.0 + 0.5 * (1.0 - dist))); // parts near the center split more
            return rng.nextDouble() < p;
        }

        private void split(int x, int z, int w, int d, int depth, int facing) {
            if (!shouldSplit(x, z, w, d, depth)) {
                leaves.add(new CampLeaf(x, z, w, d, facing, null, 0, false, leaves.size()));
                return;
            }
            boolean alongX = w > d || w == d && rng.nextBoolean();
            int length = alongX ? w : d;
            int roadW = Math.max(1, rootRoad - depth);
            int maxChildren = (length + roadW) / (minSide + roadW);
            int n = 2;
            if (depth > 0) {
                while (n < maxChildren && rng.nextDouble() < EXTRA_CHILD_CHANCE) {
                    n++;
                }
            }
            double[] weight = new double[n];
            double sum = 0;
            for (int i = 0; i < n; i++) {
                weight[i] = 1.0 + rng.nextDouble();
                sum += weight[i];
            }
            int total = length - (n - 1) * roadW;
            int remain = total - n * minSide;
            int[] len = new int[n];
            int used = 0;
            for (int i = 0; i < n; i++) {
                len[i] = minSide + (int) (remain * weight[i] / sum);
                used += len[i];
            }
            len[n - 1] += total - used; // rounding leftover goes to the last child

            int cur = alongX ? x : z;
            for (int i = 0; i < n; i++) {
                boolean last = i == n - 1;
                // Buildings face the road between them: all but the last face onward, the last faces back.
                int childFacing = alongX ? (last ? 1 : 3) : (last ? 2 : 0);
                if (alongX) {
                    split(cur, z, len[i], d, depth + 1, childFacing);
                } else {
                    split(x, cur, w, len[i], depth + 1, childFacing);
                }
                if (!last) {
                    int roadAt = cur + len[i];
                    roads.add(road(alongX, roadAt, roadW, x, z, w, d, depth));
                }
                cur += len[i] + roadW;
            }
        }

        private CampRoad road(boolean alongX, int at, int roadW, int x, int z, int w, int d, int depth) {
            if (alongX) {
                // Root roads cross the whole footprint and run past the fence.
                int z0 = depth == 0 ? -ROAD_TAIL : z;
                int len = depth == 0 ? sz + 2 * ROAD_TAIL : d;
                return new CampRoad(at, z0, roadW, len, depth);
            }
            int x0 = depth == 0 ? -ROAD_TAIL : x;
            int len = depth == 0 ? sx + 2 * ROAD_TAIL : w;
            return new CampRoad(x0, at, len, roadW, depth);
        }

        private CampLeaf assign(CampLeaf leaf) {
            boolean west = leaf.x <= INSET;
            boolean east = leaf.x + leaf.w >= sx - INSET;
            boolean north = leaf.z <= INSET;
            boolean south = leaf.z + leaf.d >= sz - INSET;
            boolean xEdge = west || east;
            boolean zEdge = north || south;
            int smallSide = Math.min(leaf.w, leaf.d);

            if (xEdge && zEdge && smallSide >= 3) {
                double chance = ((sx + sz) / 2.0 - 20.0) / 40.0;
                if (rng.nextDouble() < chance) {
                    int size = smallSide >= 4 && rng.nextBoolean() ? 4 : 3;
                    return leaf.with(CampPart.TOWER, size, false);
                }
            }
            if (xEdge || zEdge) {
                // An edge on the x axis (west or east) reads the base's x size; on z, its z size; a corner leaf that
                // missed its tower roll takes the larger.
                int side = xEdge && zEdge ? Math.max(sx, sz) : xEdge ? sx : sz;
                if (rng.nextDouble() < (side - 30) / 50.0) {
                    CampPart p = pick(CampPart.borderParts(), leaf);
                    if (p != null) {
                        return leaf.with(p, 0, false);
                    }
                }
            }
            return leaf.with(pick(CampPart.regularParts(), leaf), 0, false); // null: the cobblestone fallback
        }

        private CampPart pick(List<CampPart> pool, CampLeaf leaf) {
            List<CampPart> fit = new ArrayList<>();
            for (CampPart p : pool) {
                if (p.fits(leaf.w, leaf.d)) {
                    fit.add(p);
                }
            }
            return fit.isEmpty() ? null : fit.get(rng.nextInt(fit.size()));
        }
    }
}
