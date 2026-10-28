package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a {@link ComplexPlan} deterministically from a world seed and grid cell (D128, D229). The shape is grown, not
 * stamped ({@link ComplexGrower}): a central hub (the assembly bay), three or four major halls leaving it (most bend
 * once), a silo gallery of four to ten silos along one or two of them with a shared Launch Control at its far end,
 * then 35 to 40 rooms and minor hallways hanging off halls and off each other, grown into the rock until at least
 * three quarters of the footprint is built. {@link ComplexFurnisher} then puts in the marks. Pure geometry: it knows
 * nothing of terrain, activity levels, dimensions or blocks.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (D128, D229, D230)
public final class ComplexLayout {

    /** The complex is {@code SIZE} x {@code SIZE} blocks (15 chunks). */
    public static final int SIZE = 240;
    /** Height of a minor hallway and the small rooms: floor (y 0), five air layers, ceiling (y 6). */
    public static final int HEIGHT = 7;
    /** @see #HEIGHT */
    public static final int MINOR_HEIGHT = HEIGHT;
    /** Height of a room: floor, seven air layers, ceiling. */
    public static final int ROOM_HEIGHT = 9;
    /** Height of a major hall. */
    public static final int MAJOR_HEIGHT = 11;
    /** Height of the hub and the silo gallery: level with the top of a silo. */
    public static final int HUB_HEIGHT = 12;
    /** @see #HUB_HEIGHT */
    public static final int GALLERY_HEIGHT = 12;
    /** The hub's footprint (D229: twice the I6 hall's area). */
    public static final int HUB_LONG = 51, HUB_SHORT = 40;
    /** Footprint widths, walls included. */
    public static final int MAJOR_WIDTH = 20, MINOR_WIDTH = 5;
    /** The footprint reserved for a real Missile Silo structure. */
    public static final int SILO_SIZE = 5;
    /** The Missile Silo structure is 12 layers tall (the shaft continues above it to the sky). */
    public static final int SILO_HEIGHT = 12;
    /** The share of the footprint that must be built, in percent (D229). */
    public static final int MIN_COVERAGE_PERCENT = 75;

    private ComplexLayout() {}

    /** The seed that all of one cell's randomness derives from. */
    public static long cellSeed(long worldSeed, int cellX, int cellZ) {
        long h = worldSeed ^ 0x5DEECE66DL;
        h = h * 31 + cellX * 0x9E3779B1L;
        h = h * 31 + cellZ * 0x85EBCA77L;
        h ^= (h >>> 29);
        return h * 0xBF58476D1CE4E5B9L;
    }

    public static ComplexPlan generate(long worldSeed, int cellX, int cellZ) {
        return fromSeed(cellSeed(worldSeed, cellX, cellZ), cellX, cellZ);
    }

    /** As {@link #generate} from an already mixed seed (the placement plan's, which includes the dimension). */
    public static ComplexPlan fromSeed(long seed, int cellX, int cellZ) {
        ComplexGrower g = grown(seed);
        Room hub = null;
        List<Room> rooms = new ArrayList<Room>();
        List<Tunnel> tunnels = new ArrayList<Tunnel>();
        for (ComplexGrower.Node n : g.nodes) {
            switch (n.type) {
                case ComplexGrower.HUB:
                    hub = new Room(n.id, RoomKind.ASSEMBLY_BAY, n.box(), -1);
                    break;
                case ComplexGrower.ROOM: {
                    Room r = new Room(n.id, n.kind, n.box(), n.parent);
                    for (Door d : g.doors) {
                        if (d.a == n.parent && d.b == n.id && d.kind == Door.Kind.DOOR) {
                            r.door(d);
                            break;
                        }
                    }
                    rooms.add(r);
                    break;
                }
                default: {
                    Tunnel t = new Tunnel(n.id, n.box(), n.type == ComplexGrower.MAJOR, n.alongX(), n.parent);
                    t.gallery(n.gallery);
                    tunnels.add(t);
                    break;
                }
            }
        }
        ComplexPlan plan = new ComplexPlan(
            seed,
            cellX,
            cellZ,
            hub,
            rooms,
            tunnels,
            g.silos,
            g.doors,
            g.escape,
            g.radars);
        ComplexFurnisher.furnish(plan, g.branchPoints, new Random(seed ^ 0x6F75726E69736831L));
        return plan;
    }

    /** Regrowths tried when a plan misses its targets; each derives its seed from the plan seed and the attempt. */
    static final int ATTEMPTS = 8;

    /**
     * Grows the plan's geometry. A growth that misses the coverage target (with a margin), or the room, silo or
     * guard-post count, is grown again from a seed mixed from the plan seed and the attempt number, and the best
     * attempt
     * is kept, so the result is still a pure function of the seed (D230).
     */
    static ComplexGrower grown(long seed) {
        ComplexGrower best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            long s = attempt == 0 ? seed : seed ^ (0x9E3779B97F4A7C15L * attempt);
            ComplexGrower g = new ComplexGrower(s);
            try {
                g.grow();
            } catch (IllegalStateException e) {
                continue; // no hall long enough for the gallery: grow again
            }
            int cover = g.coveredColumns() * 100 / (SIZE * SIZE);
            int rooms = g.roomCount();
            int silos = g.silos.size();
            int posts = g.posts();
            boolean ok = rooms >= MIN_ROOMS && rooms <= MAX_ROOMS
                && cover >= MIN_COVERAGE_PERCENT + 1
                && silos >= MIN_SILOS
                && posts >= MIN_POSTS;
            if (ok) return g;
            int score = cover - 10 * Math.max(0, MIN_ROOMS - rooms)
                - 100 * Math.max(0, MIN_SILOS - silos)
                - 10 * Math.max(0, MIN_POSTS - posts);
            if (score > bestScore) {
                bestScore = score;
                best = g;
            }
        }
        if (best == null) throw new IllegalStateException("no complex grew from seed " + seed);
        return best;
    }

    /** The silo count: four to ten (D229). */
    public static final int MIN_SILOS = 4, MAX_SILOS = 10;

    /** The fewest guard-post rooms a plan has (D233: three to five). */
    public static final int MIN_POSTS = 3;

    /** The room count (the hub not counted), D229. */
    public static final int MIN_ROOMS = 35, MAX_ROOMS = 40;

    /**
     * Cells between two frames of a row on an armory wall. A long rifle in a frame draws about three blocks wide, so
     * frames three cells apart leave every gun its own stretch of wall.
     */
    public static final int FRAME_STRIDE = 3;

    /**
     * A room's own frame, so furnishing never depends on where the room sits or which way it faces (D208). {@code a}
     * runs along the door wall ("lateral"); {@code d} is the depth from the back wall: row 0 lies against the wall
     * opposite the door, row {@code depth - 1} is the row just inside the door wall.
     */
    public static final class Frame {

        public final Box b;
        /** The side of the room the door is in: 0 north, 1 east, 2 south, 3 west. */
        public final int side;
        /** Interior cell counts: along the door wall, and from the back wall to the door wall. */
        public final int lateral, depth;
        /** First and last lateral index of the doorway. */
        public final int doorLo, doorHi;

        public Frame(Room r) {
            b = r.bounds();
            side = r.doorSide();
            boolean nsDoor = side == 0 || side == 2;
            lateral = (nsDoor ? b.sizeX() : b.sizeZ()) - 2;
            depth = (nsDoor ? b.sizeZ() : b.sizeX()) - 2;
            Door d = r.door();
            doorLo = nsDoor ? d.x0 - b.minX - 1 : d.z0 - b.minZ - 1;
            doorHi = nsDoor ? d.x1 - b.minX - 1 : d.z1 - b.minZ - 1;
        }

        public int x(int a, int d) {
            switch (side) {
                case 0:
                case 2:
                    return b.minX + 1 + a;
                case 1:
                    return b.minX + 1 + d;
                default:
                    return b.maxX - 2 - d;
            }
        }

        public int z(int a, int d) {
            switch (side) {
                case 1:
                case 3:
                    return b.minZ + 1 + a;
                case 2:
                    return b.minZ + 1 + d;
                default:
                    return b.maxZ - 2 - d;
            }
        }

        /** @return x of the door wall's cell at lateral {@code a} */
        public int wallX(int a) {
            return side == 1 ? b.maxX - 1 : side == 3 ? b.minX : x(a, 0);
        }

        /** @return z of the door wall's cell at lateral {@code a} */
        public int wallZ(int a) {
            return side == 2 ? b.maxZ - 1 : side == 0 ? b.minZ : z(a, 0);
        }

        /** @return x of the back wall's cell at lateral {@code a} */
        public int backWallX(int a) {
            return side == 1 ? b.minX : side == 3 ? b.maxX - 1 : x(a, 0);
        }

        /** @return z of the back wall's cell at lateral {@code a} */
        public int backWallZ(int a) {
            return side == 2 ? b.minZ : side == 0 ? b.maxZ - 1 : z(a, 0);
        }

        /** True for the cells straight inside the doorway and beside it, which stay clear. */
        public boolean blocksDoor(int a, int d) {
            return d >= depth - 2 && a >= doorLo - 1 && a <= doorHi + 1;
        }

        /** Hanging direction (0 south, 1 west, 2 north, 3 east) of something on the door wall, facing into the room. */
        public int doorWallFacing() {
            return ComplexFurnisher.inward(side);
        }

        /** Hanging direction of something on the back wall, facing into the room (toward the door). */
        public int backFacing() {
            return ComplexFurnisher.inward((side + 2) & 3);
        }
    }
}
