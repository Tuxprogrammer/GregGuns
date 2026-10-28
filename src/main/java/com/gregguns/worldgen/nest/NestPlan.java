package com.gregguns.worldgen.nest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The shape of one Alien Bug Nest, a pure function of a seed, the box corner and the ground height: a graph of
 * chambers joined by tunnels under an entrance shaft. {@link #cell} answers what belongs at any world position, so a
 * populate window computes its own share without asking another window (WGEN-04); nothing here keeps state between
 * calls and no Minecraft type is used.
 *
 * <p>
 * Rules (structure-notes/alien_bug_nest.md): a main chamber of radius 5, 10 to 16 under the surface and up to 2 blocks
 * off the shaft axis; five satellites every 72 degrees at 15 to 25 blocks; each satellite has an even chance of one
 * child 15 to 25 further out within 45 degrees of its own direction. Every chamber is a shell ball of radius r around
 * an
 * air ball of radius r - 1. Tunnels join linked chambers (shell radius 2 or 3, never wider than either end); a vertical
 * shaft of air radius 2 reaches the surface and is lined with climbable slime; the main chamber has a slime pillar.
 * Each
 * satellite and child holds one bug spawner and piles of eggs on its floor. Air always wins over shells (the "re-carve"
 * step of the notes), so no tunnel wall sticks into a room.
 *
 * <p>
 * Deviation: the notes leave the span "soft" (it falls out of the distances). Here every chamber center is clamped so
 * the whole nest fits a {@value #SIZE} block square box, which the grid needs to plan it.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md; fixes WGEN-04 (position-only cells), WGEN-14
public final class NestPlan {

    /** Side of the square box the nest fits in, in blocks. */
    public static final int SIZE = 61;
    /** Offset of the shaft axis from the box corner. */
    public static final int HALF = SIZE / 2;
    /** Blocks the shaft air reaches above the decided ground, so a hill over the mouth is breached. */
    public static final int SHAFT_EXTRA = 8;
    /** Height of the planned box: from the deepest chamber floor to the top of the shaft. */
    public static final int HEIGHT = 40;
    /** Lowest ground height the nest is built under; below this the deepest chamber could reach the void. */
    public static final int MIN_GROUND_Y = 40;
    /** Blocks above the ground the village scan looks at. */
    public static final int ABOVE = 4;

    /** Position has nothing to write; leave the terrain alone. */
    public static final int NONE = 0;
    /** Air (carved). */
    public static final int AIR = 1;
    /** Resin wall block. */
    public static final int WALL = 2;
    /** Plain sandstone fleck in a wall. */
    public static final int PLAIN = 3;
    /** Egg block (wall fleck or a pile on a chamber floor). */
    public static final int EGG = 4;
    /** Air carved here, and a bug spawner mark belongs on it. */
    public static final int SPAWNER = 5;
    /** Pillar block in the main chamber (resin). */
    public static final int PILLAR = 6;
    /** Slime trail: {@code TRAIL | mask} where mask is the vine side bits (1 south, 2 west, 4 north, 8 east). */
    public static final int TRAIL = 0x10;

    private static final int MAIN_R = 5;
    private static final int SHAFT_AIR = 2;
    private static final int SHAFT_SHELL = 3;
    // dx, dz and vine bit of the four horizontal neighbors: south (+z) 1, west (-x) 2, north (-z) 4, east (+x) 8.
    private static final int[][] SIDES = { { 0, 1, 1 }, { -1, 0, 2 }, { 0, -1, 4 }, { 1, 0, 8 } };

    /** One ball: shell radius {@code r}, air radius {@code r - 1}. */
    public static final class Chamber {

        public final int x;
        public final int y;
        public final int z;
        public final int r;
        /** True for satellites and children: they hold a bug spawner and eggs. */
        public final boolean hive;
        final int index;
        /** Bug spawner column and floor height; unused for the main chamber. */
        int holeX;
        int holeY;
        int holeZ;

        Chamber(int x, int y, int z, int r, boolean hive, int index) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.hive = hive;
            this.index = index;
        }

        int airR() {
            return r - 1;
        }
    }

    /** A round tube between two chamber centers: shell radius {@code r}, air radius {@code r - 1}. */
    static final class Tunnel {

        final int ax;
        final int ay;
        final int az;
        final int bx;
        final int by;
        final int bz;
        final int r;
        final double len2;

        Tunnel(Chamber a, Chamber b, int r) {
            ax = a.x;
            ay = a.y;
            az = a.z;
            bx = b.x;
            by = b.y;
            bz = b.z;
            this.r = r;
            double dx = bx - ax;
            double dy = by - ay;
            double dz = bz - az;
            len2 = dx * dx + dy * dy + dz * dz;
        }

        /** Squared distance from a point to the segment. */
        double dist2(int x, int y, int z) {
            double dx = bx - ax;
            double dy = by - ay;
            double dz = bz - az;
            double t = len2 == 0 ? 0 : ((x - ax) * dx + (y - ay) * dy + (z - az) * dz) / len2;
            t = Math.max(0, Math.min(1, t));
            double ex = ax + t * dx - x;
            double ey = ay + t * dy - y;
            double ez = az + t * dz - z;
            return ex * ex + ey * ey + ez * ez;
        }
    }

    private final long seed;
    private final int anchorX;
    private final int anchorZ;
    private final int groundY;
    private final int centerX;
    private final int centerZ;
    private final List<Chamber> chambers = new ArrayList<>();
    private final List<Tunnel> tunnels = new ArrayList<>();
    private final Chamber main;
    private final int pillarX;
    private final int pillarZ;
    private final int pillarBottom;
    private final int pillarTop;
    private final int minY;
    private final int maxY;

    private NestPlan(long seed, int anchorX, int anchorZ, int groundY) {
        this.seed = seed;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
        this.groundY = groundY;
        this.centerX = anchorX + HALF;
        this.centerZ = anchorZ + HALF;
        Random rnd = new Random(seed ^ 0x6275676E657374L);

        int mainY = groundY - (10 + rnd.nextInt(7));
        main = new Chamber(centerX + rnd.nextInt(5) - 2, mainY, centerZ + rnd.nextInt(5) - 2, MAIN_R, false, 0);
        chambers.add(main);

        double base = rnd.nextDouble() * Math.PI * 2;
        List<Chamber> sats = new ArrayList<>();
        List<Double> satAngle = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = base + i * (Math.PI * 2 / 5);
            Chamber c = ball(rnd, main, angle, mainY + rnd.nextInt(16) - 5, chambers.size());
            sats.add(c);
            satAngle.add(angle);
            chambers.add(c);
            if (i == 0) {
                link(rnd, c, main);
            } else if (rnd.nextBoolean()) {
                link(rnd, c, sats.get(i - 1));
                if (rnd.nextBoolean()) link(rnd, c, main);
            } else {
                link(rnd, c, main);
            }
        }
        for (int i = 0; i < 5; i++) {
            if (!rnd.nextBoolean()) continue;
            Chamber parent = sats.get(i);
            double angle = satAngle.get(i) + (rnd.nextDouble() * 2 - 1) * (Math.PI / 4);
            Chamber c = ball(rnd, parent, angle, parent.y + rnd.nextInt(11) - 5, chambers.size());
            chambers.add(c);
            link(rnd, c, parent);
        }
        for (Chamber c : chambers) {
            if (!c.hive) continue;
            // The hole sits on the floor, in the middle of it.
            int h = c.airR() / 2;
            c.holeX = c.x + rnd.nextInt(2 * h + 1) - h;
            c.holeZ = c.z + rnd.nextInt(2 * h + 1) - h;
            c.holeY = floorY(c, c.holeX, c.holeZ);
        }

        pillarX = centerX + 1;
        pillarZ = centerZ;
        pillarBottom = mainY - 1;
        pillarTop = mainY + main.airR();
        int lo = Integer.MAX_VALUE;
        for (Chamber c : chambers) lo = Math.min(lo, c.y - c.r - 1);
        minY = Math.max(1, lo);
        maxY = groundY + SHAFT_EXTRA;
    }

    /**
     * @param seed    the plan's own seed (the grid's per-plan seed)
     * @param anchorX minimum block X of the {@value #SIZE} square box
     * @param anchorZ minimum block Z of the box
     * @param groundY the decided ground height at the entrance
     * @return the nest
     */
    public static NestPlan generate(long seed, int anchorX, int anchorZ, int groundY) {
        return new NestPlan(seed, anchorX, anchorZ, groundY);
    }

    /** A chamber 15 to 25 from {@code from} along {@code angle}, clamped into the box and under the ground. */
    private Chamber ball(Random rnd, Chamber from, double angle, int y, int index) {
        int dist = 15 + rnd.nextInt(11);
        // Smaller balls more often: radius 3 is likeliest, 5 rarest.
        int r = 3 + Math.min(rnd.nextInt(3), rnd.nextInt(3));
        int x = from.x + (int) Math.round(Math.cos(angle) * dist);
        int z = from.z + (int) Math.round(Math.sin(angle) * dist);
        int reach = HALF - r - 1;
        x = Math.max(centerX - reach, Math.min(centerX + reach, x));
        z = Math.max(centerZ - reach, Math.min(centerZ + reach, z));
        // Top of the shell at least 3 under the ground, and clear of the void.
        y = Math.min(y, groundY - r - 3);
        y = Math.max(y, 8 + r);
        return new Chamber(x, y, z, r, true, index);
    }

    private void link(Random rnd, Chamber a, Chamber b) {
        int r = Math.min(2 + rnd.nextInt(2), Math.min(a.r, b.r));
        tunnels.add(new Tunnel(a, b, r));
    }

    /** World Y of the lowest air cell of a chamber at a column inside its air ball. */
    private static int floorY(Chamber c, int x, int z) {
        int dx = x - c.x;
        int dz = z - c.z;
        int rest = c.airR() * c.airR() - dx * dx - dz * dz;
        int dy = (int) Math.floor(Math.sqrt(Math.max(0, rest)));
        while (dy * dy > rest && dy > 0) dy--;
        return c.y - dy;
    }

    // ---- accessors -------------------------------------------------------------------------------------------

    /** @return every chamber, the main one first */
    public List<Chamber> chambers() {
        return Collections.unmodifiableList(chambers);
    }

    /** @return the number of tunnels between chambers (the shaft is separate) */
    public int tunnelCount() {
        return tunnels.size();
    }

    /** @return the bug spawner positions as {x, y, z}, one per satellite or child chamber */
    public List<int[]> spawners() {
        List<int[]> out = new ArrayList<>();
        for (Chamber c : chambers) {
            if (c.hive) out.add(new int[] { c.holeX, c.holeY, c.holeZ });
        }
        return out;
    }

    public int anchorX() {
        return anchorX;
    }

    public int anchorZ() {
        return anchorZ;
    }

    public int groundY() {
        return groundY;
    }

    /** @return world X of the shaft axis */
    public int shaftX() {
        return centerX;
    }

    /** @return world Z of the shaft axis */
    public int shaftZ() {
        return centerZ;
    }

    /** @return the lowest Y any cell of the nest can occupy */
    public int minY() {
        return minY;
    }

    /** @return the highest Y any cell of the nest can occupy (the top of the shaft air) */
    public int maxY() {
        return maxY;
    }

    // ---- the cell function -----------------------------------------------------------------------------------

    /**
     * What belongs at a world position.
     *
     * @return {@link #NONE}, {@link #AIR}, {@link #WALL}, {@link #PLAIN}, {@link #EGG}, {@link #SPAWNER},
     *         {@link #PILLAR}, or {@link #TRAIL} combined with the vine side mask
     */
    public int cell(int x, int y, int z) {
        if (x < anchorX || x >= anchorX + SIZE || z < anchorZ || z >= anchorZ + SIZE || y < minY || y > maxY) {
            return NONE;
        }
        if (x == pillarX && z == pillarZ && y >= pillarBottom && y <= pillarTop) return PILLAR;
        Chamber in = airChamber(x, y, z);
        if (in != null || tunnelAir(x, y, z) || shaftAir(x, y, z)) {
            return airCell(x, y, z, in);
        }
        if (y > groundY) return NONE;
        return shellCell(x, y, z);
    }

    private int airCell(int x, int y, int z, Chamber in) {
        for (Chamber c : chambers) {
            if (c.hive && c.holeX == x && c.holeY == y && c.holeZ == z) return SPAWNER;
        }
        int mask = trailMask(x, y, z);
        if (mask != 0) return TRAIL | mask;
        if (in != null && in.hive && eggAt(in, x, y, z)) return EGG;
        return AIR;
    }

    /** The first chamber whose air ball holds the cell, or null. Prefers a hive chamber (eggs live there). */
    private Chamber airChamber(int x, int y, int z) {
        Chamber found = null;
        for (Chamber c : chambers) {
            int dx = x - c.x;
            int dy = y - c.y;
            int dz = z - c.z;
            int ar = c.airR();
            if (dx * dx + dy * dy + dz * dz <= ar * ar) {
                if (c.hive) return c;
                found = c;
            }
        }
        return found;
    }

    private boolean tunnelAir(int x, int y, int z) {
        for (Tunnel t : tunnels) {
            int ar = t.r - 1;
            if (t.dist2(x, y, z) <= ar * ar) return true;
        }
        return false;
    }

    private boolean shaftAir(int x, int y, int z) {
        if (y < main.y || y > groundY + SHAFT_EXTRA) return false;
        int dx = x - centerX;
        int dz = z - centerZ;
        return dx * dx + dz * dz <= SHAFT_AIR * SHAFT_AIR;
    }

    private boolean isAir(int x, int y, int z) {
        return airChamber(x, y, z) != null || tunnelAir(x, y, z) || shaftAir(x, y, z);
    }

    /** Shell test for a cell that is not air: which ball or tube holds it, or {@link #NONE}. */
    private int shellCell(int x, int y, int z) {
        boolean chamber = false;
        for (Chamber c : chambers) {
            int dx = x - c.x;
            int dy = y - c.y;
            int dz = z - c.z;
            if (dx * dx + dy * dy + dz * dz <= c.r * c.r) {
                chamber = true;
                break;
            }
        }
        boolean tube = false;
        if (!chamber) {
            for (Tunnel t : tunnels) {
                if (t.dist2(x, y, z) <= t.r * t.r) {
                    tube = true;
                    break;
                }
            }
            if (!tube && y >= main.y) {
                int dx = x - centerX;
                int dz = z - centerZ;
                tube = dx * dx + dz * dz <= SHAFT_SHELL * SHAFT_SHELL;
            }
        }
        if (!chamber && !tube) return NONE;
        // Position hash, not a running random: the same cell gets the same block whichever window writes it.
        int h = (int) ((hash(x, y, z, 1) >>> 33) % (chamber ? 19 : 13));
        if (chamber) return h < 17 ? WALL : (h == 17 ? EGG : PLAIN);
        return h < 12 ? WALL : PLAIN;
    }

    /** True when a cell is a wall cell (any non-air shell), for trail attachment. */
    private boolean isShell(int x, int y, int z) {
        return y <= groundY && !isAir(x, y, z) && shellCell(x, y, z) != NONE;
    }

    /** Vine side bits for a trail at this air cell, or 0. */
    private int trailMask(int x, int y, int z) {
        int mask = 0;
        if (y >= pillarBottom && y <= pillarTop) {
            for (int[] s : SIDES) {
                // The cell on this side of the pillar holds a trail that clings back toward it.
                if (x - s[0] == pillarX && z - s[1] == pillarZ) {
                    mask |= opposite(s[2]);
                }
            }
        }
        if (mask == 0 && y > main.y + main.airR() - 1 && y <= groundY && shaftAir(x, y, z)) {
            for (int[] s : SIDES) {
                if (isShell(x + s[0], y, z + s[1])) mask |= s[2];
            }
        }
        return mask;
    }

    private static int opposite(int bit) {
        // south <-> north, west <-> east
        switch (bit) {
            case 1:
                return 4;
            case 4:
                return 1;
            case 2:
                return 8;
            default:
                return 2;
        }
    }

    /** Egg piles on a hive chamber's floor: a start chance that rises toward the middle, thinning each layer. */
    private boolean eggAt(Chamber c, int x, int y, int z) {
        if (x == c.holeX && z == c.holeZ) return false;
        int dx = x - c.x;
        int dz = z - c.z;
        double dh = Math.sqrt(dx * dx + dz * dz);
        int ar = c.airR();
        int k = y - floorY(c, x, z);
        if (k < 0 || k > ar) return false;
        int start = 15 + (int) (55 * Math.max(0, 1 - dh / Math.max(1, ar)));
        if ((hash(x, c.index, z, 2) >>> 33) % 100 >= start) return false;
        for (int j = 1; j <= k; j++) {
            if ((hash(x, c.index * 64 + j, z, 3) >>> 33) % 100 >= 55) return false;
        }
        return true;
    }

    /** SplitMix64 over the position and the plan seed. */
    private long hash(int x, int y, int z, int salt) {
        long h = seed + salt * 0x9E3779B97F4A7C15L;
        h = mix(h + x * 0xD6E8FEB86659FD93L);
        h = mix(h + y * 0xA0761D6478BD642FL);
        h = mix(h + z * 0xC2B2AE3D27D4EB4FL);
        return h;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
