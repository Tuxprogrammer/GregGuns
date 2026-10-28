package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds a {@link ComplexPlan} deterministically from a world seed and grid cell (D128). The shape: an assembly bay at
 * the center, four wings (N, E, S, W) of corridor with room slots on both sides, a silo cluster in the north-east
 * quadrant linked to the hall, a decontamination airlock and a guard post at each wing mouth, an escape ladder at a
 * wing
 * end, and radar stations on the surface in the south-west quadrant. Pure geometry: it knows nothing of terrain,
 * activity levels or dimensions.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (D128)
public final class ComplexLayout {

    /** The complex is {@code SIZE} x {@code SIZE} blocks (15 chunks). */
    public static final int SIZE = 240;
    /** Floor (y 0), five air cells, ceiling (y 6). */
    public static final int HEIGHT = 7;
    /** The footprint reserved for a real Missile Silo structure. */
    public static final int SILO_SIZE = 5;
    /** The Missile Silo structure is 12 layers tall (the shaft continues above it to the sky). */
    public static final int SILO_HEIGHT = 12;
    public static final int HALL_MIN_X = 102, HALL_MAX_X = 138, HALL_MIN_Z = 106, HALL_MAX_Z = 134;
    public static final int WING_WIDTH = 5;
    public static final int SLOT_PITCH = 16;
    public static final int SLOTS_PER_SIDE = 5;
    /** Wing indices. */
    public static final int N = 0, E = 1, S = 2, W = 3;

    private static final int CORRIDOR_LO = 118, CORRIDOR_HI = 123;
    private static final int MOUTH_GAP = 8;
    private static final int[][] SILO_ORIGINS = { { 152, 92 }, { 184, 92 }, { 152, 62 }, { 184, 62 } };

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
        Random rnd = new Random(seed);
        int id = 0;
        List<Room> rooms = new ArrayList<Room>();
        List<Tunnel> tunnels = new ArrayList<Tunnel>();

        Room hall = new Room(
            id++,
            RoomKind.ASSEMBLY_BAY,
            new Box(HALL_MIN_X, 0, HALL_MIN_Z, HALL_MAX_X, HEIGHT, HALL_MAX_Z),
            -1);
        fillMarks(hall, rnd);

        // Slot assignment: the mandatory kinds go to random slots first, the rest fill from the optional pool.
        int slotCount = 4 * 2 * SLOTS_PER_SIDE;
        List<RoomKind> kinds = new ArrayList<RoomKind>();
        List<RoomKind> optional = new ArrayList<RoomKind>();
        for (RoomKind k : RoomKind.values()) {
            if (k == RoomKind.ASSEMBLY_BAY || k == RoomKind.DECON_AIRLOCK || k == RoomKind.GUARD_POST) continue;
            if (k.mandatory) kinds.add(k);
            else optional.add(k);
        }
        // 60-85% of the slots are used; the remainder stay solid rock.
        int used = slotCount * (60 + rnd.nextInt(26)) / 100;
        Collections.shuffle(optional, rnd);
        int oi = 0;
        while (kinds.size() < used) {
            kinds.add(optional.get(oi++ % optional.size()));
        }
        Collections.shuffle(kinds, rnd);
        boolean[] taken = new boolean[slotCount];
        int placed = 0;
        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < slotCount; i++) order.add(i);
        Collections.shuffle(order, rnd);
        for (int s : order) {
            if (placed >= kinds.size()) break;
            taken[s] = true;
            RoomKind k = kinds.get(placed++);
            int wing = s / (2 * SLOTS_PER_SIDE);
            int side = (s / SLOTS_PER_SIDE) % 2;
            int slot = s % SLOTS_PER_SIDE;
            Room r = new Room(id++, k, slotBox(wing, side, slot, k), wing);
            fillMarks(r, rnd);
            rooms.add(r);
        }

        // Corridors and the mouth features.
        for (int w = 0; w < 4; w++) {
            Tunnel t = new Tunnel(wingBox(w), w);
            addTunnelMarks(t, w, rnd);
            tunnels.add(t);
            Box post = mouthBox(w, RoomKind.GUARD_POST, 0);
            Room g = new Room(id++, RoomKind.GUARD_POST, post, w);
            fillMarks(g, rnd);
            rooms.add(g);
        }
        // One decontamination airlock between the hall and a randomly picked wing mouth.
        int dw = rnd.nextInt(4);
        Room decon = new Room(id++, RoomKind.DECON_AIRLOCK, mouthBox(dw, RoomKind.DECON_AIRLOCK, 1), dw);
        fillMarks(decon, rnd);
        rooms.add(decon);

        // Silos: usually 1, rarely 2, at most 4.
        int roll = rnd.nextInt(100);
        int siloCount = roll < 78 ? 1 : roll < 93 ? 2 : roll < 98 ? 3 : 4;
        List<SiloSlot> silos = new ArrayList<SiloSlot>();
        for (int i = 0; i < siloCount; i++) {
            int ox = SILO_ORIGINS[i][0], oz = SILO_ORIGINS[i][1];
            SurfaceVariant v = SurfaceVariant.values()[rnd.nextInt(3)];
            MissingPart m = MissingPart.values()[rnd.nextInt(2)];
            silos.add(new SiloSlot(new Box(ox, 0, oz, ox + SILO_SIZE, SILO_HEIGHT, oz + SILO_SIZE), v, m));
        }
        addSiloLinks(tunnels, silos);

        // Escape ladder at the far end of a random wing.
        int ew = rnd.nextInt(4);
        int[] esc = escapePos(ew);

        List<int[]> radars = new ArrayList<int[]>();
        int radarCount = rnd.nextInt(3);
        for (int i = 0; i < radarCount; i++) {
            // Stations stand at least RADAR_SPACING apart on one axis, so two huts never share cells.
            int[] at;
            int tries = 0;
            do {
                at = new int[] { 24 + rnd.nextInt(72), 150 + rnd.nextInt(66) };
            } while (tries++ < 16 && tooClose(radars, at));
            if (tooClose(radars, at)) continue;
            radars.add(at);
        }
        return new ComplexPlan(seed, cellX, cellZ, hall, rooms, tunnels, silos, esc[0], esc[1], ladderMeta(ew), radars);
    }

    private static final int RADAR_SPACING = 12;

    private static boolean tooClose(List<int[]> radars, int[] at) {
        for (int[] o : radars) {
            if (Math.abs(o[0] - at[0]) < RADAR_SPACING && Math.abs(o[1] - at[1]) < RADAR_SPACING) return true;
        }
        return false;
    }

    /** The room box of a slot: {@code side} 0 is the low-coordinate side of the corridor, 1 the high side. */
    static Box slotBox(int wing, int side, int slot, RoomKind k) {
        int off = MOUTH_GAP + 2 + slot * SLOT_PITCH; // clear of the mouth annex
        int len = k.length, dep = k.depth;
        int a0 = side == 0 ? CORRIDOR_LO - dep : CORRIDOR_HI; // across-wing range start
        int a1 = a0 + dep;
        switch (wing) {
            case N:
                return new Box(a0, 0, HALL_MIN_Z - off - len, a1, HEIGHT, HALL_MIN_Z - off);
            case S:
                return new Box(a0, 0, HALL_MAX_Z + off, a1, HEIGHT, HALL_MAX_Z + off + len);
            case E:
                return new Box(HALL_MAX_X + off, 0, a0, HALL_MAX_X + off + len, HEIGHT, a1);
            default:
                return new Box(HALL_MIN_X - off - len, 0, a0, HALL_MIN_X - off, HEIGHT, a1);
        }
    }

    /** The corridor of a wing, from the hall wall to the far end. */
    static Box wingBox(int wing) {
        int far = MOUTH_GAP + 2 + SLOTS_PER_SIDE * SLOT_PITCH;
        switch (wing) {
            case N:
                return new Box(CORRIDOR_LO, 0, HALL_MIN_Z - far, CORRIDOR_HI, HEIGHT, HALL_MIN_Z);
            case S:
                return new Box(CORRIDOR_LO, 0, HALL_MAX_Z, CORRIDOR_HI, HEIGHT, HALL_MAX_Z + far);
            case E:
                return new Box(HALL_MAX_X, 0, CORRIDOR_LO, HALL_MAX_X + far, HEIGHT, CORRIDOR_HI);
            default:
                return new Box(HALL_MIN_X - far, 0, CORRIDOR_LO, HALL_MIN_X, HEIGHT, CORRIDOR_HI);
        }
    }

    /** A small annex beside the corridor mouth (guard post on the low side, airlock on the high side). */
    private static Box mouthBox(int wing, RoomKind k, int side) {
        int a0 = side == 0 ? CORRIDOR_LO - k.depth : CORRIDOR_HI;
        int a1 = a0 + k.depth;
        int off = 1;
        switch (wing) {
            case N:
                return new Box(a0, 0, HALL_MIN_Z - off - k.length, a1, HEIGHT, HALL_MIN_Z - off);
            case S:
                return new Box(a0, 0, HALL_MAX_Z + off, a1, HEIGHT, HALL_MAX_Z + off + k.length);
            case E:
                return new Box(HALL_MAX_X + off, 0, a0, HALL_MAX_X + off + k.length, HEIGHT, a1);
            default:
                return new Box(HALL_MIN_X - off - k.length, 0, a0, HALL_MIN_X - off, HEIGHT, a1);
        }
    }

    /** Ladder metadata that clings to the wing end's wall: the wall lies behind the ladder, away from the corridor. */
    private static int ladderMeta(int wing) {
        switch (wing) {
            case N:
                return 3; // wall at -Z
            case S:
                return 2; // wall at +Z
            case E:
                return 4; // wall at +X
            default:
                return 5; // wall at -X
        }
    }

    private static int[] escapePos(int wing) {
        Box b = wingBox(wing);
        switch (wing) {
            case N:
                return new int[] { b.centerX(), b.minZ + 1 };
            case S:
                return new int[] { b.centerX(), b.maxZ - 2 };
            case E:
                return new int[] { b.maxX - 2, b.centerZ() };
            default:
                return new int[] { b.minX + 1, b.centerZ() };
        }
    }

    /**
     * Links the silo cluster to the hall: a stub off the hall's east wall, a trunk running north, and one row corridor
     * per silo row along the silos' front (north) faces, where the controller is.
     */
    private static void addSiloLinks(List<Tunnel> tunnels, List<SiloSlot> silos) {
        int minZ = 106;
        boolean[] row = new boolean[2];
        for (SiloSlot s : silos) {
            int r = s.bounds().minZ == SILO_ORIGINS[0][1] ? 0 : 1;
            row[r] = true;
            minZ = Math.min(minZ, s.bounds().minZ - 6);
        }
        Tunnel stub = new Tunnel(new Box(138, 0, 106, 142, HEIGHT, 111), -1);
        lamp(stub);
        tunnels.add(stub);
        Tunnel trunk = new Tunnel(new Box(140, 0, minZ, 145, HEIGHT, 111), -1);
        lamp(trunk);
        // D36: guards thicken with depth; the trunk's far end and each silo's walkway hold a "deep" post.
        trunk.add(MarkKind.SPAWNER, 142, 1, minZ + 2, "deep");
        tunnels.add(trunk);
        for (int r = 0; r < 2; r++) {
            if (!row[r]) continue;
            int oz = SILO_ORIGINS[r * 2][1];
            Tunnel rowTunnel = new Tunnel(new Box(145, 0, oz - 6, 196, HEIGHT, oz), -1);
            lamp(rowTunnel);
            for (SiloSlot s : silos) {
                if (s.bounds().minZ == oz) rowTunnel.add(MarkKind.SPAWNER, s.bounds().minX + 2, 1, oz - 3, "deep");
            }
            tunnels.add(rowTunnel);
        }
    }

    /** Lights a link tunnel the way {@link #lampCells} does for every other box. */
    private static void lamp(Tunnel t) {
        for (int[] c : lampCells(t.bounds())) t.add(MarkKind.LIGHT, c[0], HEIGHT - 2, c[1], "emergency");
    }

    /**
     * Horizontal reach of one lamp on the walking floor. A lamp hangs in the top air row, four cells over the floor,
     * and block light falls one level per cell (15 at the lamp), so a floor cell {@code h} cells away sideways reads
     * {@code 15 - 4 - h}: within {@value} cells it is 8 or more.
     */
    public static final int LAMP_REACH = 3;

    /**
     * The lamp cells of a box's ceiling, as {x, z} pairs, so that every interior floor cell lies within
     * {@link #LAMP_REACH} cells (Manhattan) of a lamp and reads block light 8 or more once the lamps are lit (D217).
     * A diagonal lattice with the density of a perfect tiling of reach-3 diamonds (one lamp per 25 cells), then a
     * lamp for every cell near an edge the lattice leaves short. Deterministic, and it draws no random numbers.
     *
     * @param b a room, hall or tunnel box (its interior is one cell in from each side)
     * @return the lamp cells
     */
    public static List<int[]> lampCells(Box b) {
        int x0 = b.minX + 1, x1 = b.maxX - 2, z0 = b.minZ + 1, z1 = b.maxZ - 2;
        List<int[]> out = new ArrayList<int[]>();
        boolean[][] lit = new boolean[x1 - x0 + 1][z1 - z0 + 1];
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (Math.floorMod(x + 7 * z, 25) == 0) light(out, lit, x0, z0, x, z);
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!lit[x - x0][z - z0]) light(out, lit, x0, z0, Math.min(x + 2, x1), Math.min(z + 1, z1));
            }
        }
        return out;
    }

    private static void light(List<int[]> out, boolean[][] lit, int x0, int z0, int x, int z) {
        out.add(new int[] { x, z });
        for (int i = 0; i < lit.length; i++) {
            for (int j = 0; j < lit[i].length; j++) {
                if (Math.abs(x0 + i - x) + Math.abs(z0 + j - z) <= LAMP_REACH) lit[i][j] = true;
            }
        }
    }

    private static void addTunnelMarks(Tunnel t, int wing, Random rnd) {
        Box b = t.bounds();
        boolean alongZ = wing == N || wing == S;
        int mid = alongZ ? b.centerX() : b.centerZ();
        for (int[] c : lampCells(b)) t.add(MarkKind.LIGHT, c[0], HEIGHT - 2, c[1], "emergency");
        int sx = alongZ ? mid : (wing == E ? b.minX + 1 : b.maxX - 2);
        int sz = alongZ ? (wing == S ? b.minZ + 1 : b.maxZ - 2) : mid;
        t.add(MarkKind.BLAST_DOOR, sx, 1, sz, "mouth");
        t.add(MarkKind.SIGN, sx, 3, sz, "icbm.complex.sign.wing." + wing);
        int d = 3 + rnd.nextInt(4);
        for (int i = 0; i < d; i++) {
            int x = alongZ ? mid + (rnd.nextBoolean() ? -1 : 1) : b.minX + rnd.nextInt(b.sizeX());
            int z = alongZ ? b.minZ + rnd.nextInt(b.sizeZ()) : mid + (rnd.nextBoolean() ? -1 : 1);
            t.add(MarkKind.DAMAGE_CELL, x, HEIGHT - 1, z, "roof");
        }
    }

    /**
     * Cells between two frames of a row on an armory wall. A long rifle in a frame draws about three blocks wide, so
     * frames three cells apart leave every gun its own stretch of wall.
     */
    public static final int FRAME_STRIDE = 3;

    /**
     * Two rows of item frames on the armory's far wall (the wall opposite its doorway; see
     * {@code ComplexRaster.roomDoor}), one frame every {@link #FRAME_STRIDE} cells. The upper row starts one cell
     * further along, so no frame sits directly above another and the guns of the two rows stagger.
     */
    private static void addFrameWall(Room r, int ix0, int ix1, int iz0, int iz1) {
        boolean alongZ = r.wing() == N || r.wing() == S;
        Box b = r.bounds();
        boolean doorOnHighSide = alongZ ? b.maxX <= CORRIDOR_LO : b.maxZ <= CORRIDOR_LO;
        if (alongZ) {
            int x = doorOnHighSide ? ix0 : ix1;
            int facing = doorOnHighSide ? 3 : 1;
            for (int y = 2; y <= 3; y++) {
                int shift = y - 2;
                r.add(
                    MarkKind.ITEM_FRAME_WALL,
                    x,
                    y,
                    iz0 + shift,
                    LootKeys.ARMORY,
                    facing,
                    (iz1 - iz0 - shift) / FRAME_STRIDE + 1);
            }
        } else {
            int z = doorOnHighSide ? iz0 : iz1;
            int facing = doorOnHighSide ? 0 : 2;
            for (int y = 2; y <= 3; y++) {
                int shift = y - 2;
                r.add(
                    MarkKind.ITEM_FRAME_WALL,
                    ix0 + shift,
                    y,
                    z,
                    LootKeys.ARMORY,
                    facing,
                    (ix1 - ix0 - shift) / FRAME_STRIDE + 1);
            }
        }
    }

    /**
     * A room's own frame, so furnishing never depends on which wing or side the room sits on (item 0 of the I6 fix
     * round). {@code a} runs along the door wall ("lateral", the wing's direction, the long side); {@code d} is the
     * depth from the back wall: row 0 lies against the wall opposite the door, row {@code D - 1} is the row just
     * inside the door wall. The door itself is the one {@link ComplexRaster} cuts.
     */
    static final class Frame {

        final Box b;
        final boolean alongZ;
        /** True when the door is in the high-coordinate wall (+X for N and S wings, +Z for E and W). */
        final boolean doorHigh;
        /** Interior cell counts: along the door wall, and from the back wall to the door wall. */
        final int lateral, depth;
        /** First and last lateral index of the doorway. */
        final int doorLo, doorHi;

        Frame(Room r) {
            b = r.bounds();
            alongZ = r.wing() == N || r.wing() == S;
            doorHigh = alongZ ? b.maxX <= CORRIDOR_LO : b.maxZ <= CORRIDOR_LO;
            lateral = (alongZ ? b.sizeZ() : b.sizeX()) - 2;
            depth = (alongZ ? b.sizeX() : b.sizeZ()) - 2;
            int c = (alongZ ? b.centerZ() : b.centerX()) - (alongZ ? b.minZ : b.minX) - 1;
            doorLo = c - 1;
            doorHi = c;
        }

        int x(int a, int d) {
            if (!alongZ) return b.minX + 1 + a;
            return doorHigh ? b.minX + 1 + d : b.maxX - 2 - d;
        }

        int z(int a, int d) {
            if (alongZ) return b.minZ + 1 + a;
            return doorHigh ? b.minZ + 1 + d : b.maxZ - 2 - d;
        }

        /** True for the two interior cells straight inside the doorway, which stay clear. */
        boolean blocksDoor(int a, int d) {
            return d >= depth - 1 && a >= doorLo - 1 && a <= doorHi + 1;
        }

        /** Hanging direction (0 south, 1 west, 2 north, 3 east) of something on the door wall, facing into the room. */
        int doorWallFacing() {
            return alongZ ? (doorHigh ? 1 : 3) : (doorHigh ? 2 : 0);
        }
    }

    private static void put(Room r, Frame f, MarkKind kind, int a, int d, String key) {
        if (f.blocksDoor(a, d)) {
            throw new IllegalStateException(
                r.kind() + " mark would block the doorway at lateral " + a + ", depth " + d);
        }
        r.add(kind, f.x(a, d), 1, f.z(a, d), key);
    }

    /** The sign by the door: on the door wall beside the doorway, facing into the room. */
    private static void doorSign(Room r, Frame f) {
        int a = f.doorHi + 2 <= f.lateral - 1 ? f.doorHi + 2 : f.doorHi + 1;
        // The door wall's own row: one step further than depth - 1.
        int x = f.alongZ ? (f.doorHigh ? f.b.maxX - 1 : f.b.minX) : f.x(a, 0);
        int z = f.alongZ ? f.z(a, 0) : (f.doorHigh ? f.b.maxZ - 1 : f.b.minZ);
        r.add(
            MarkKind.SIGN,
            x,
            3,
            z,
            "icbm.complex.sign." + r.kind()
                .name()
                .toLowerCase(java.util.Locale.ROOT),
            f.doorWallFacing(),
            1);
    }

    /** A "wall" damage cell on the room's perimeter shell, clear of every doorway. */
    private static void wallDamage(Room r, Random rnd) {
        Box b = r.bounds();
        for (int tries = 0; tries < 8; tries++) {
            int y = 1 + rnd.nextInt(HEIGHT - 2);
            boolean onX = rnd.nextBoolean();
            int x, z;
            if (onX) {
                x = rnd.nextBoolean() ? b.minX : b.maxX - 1;
                z = b.minZ + 1 + rnd.nextInt(b.sizeZ() - 2);
            } else {
                z = rnd.nextBoolean() ? b.minZ : b.maxZ - 1;
                x = b.minX + 1 + rnd.nextInt(b.sizeX() - 2);
            }
            if (y <= 3 && nearDoor(r, x, z)) continue;
            r.add(MarkKind.DAMAGE_CELL, x, y, z, "wall");
            return;
        }
        // Every try hit a doorway: a hole high in the wall is always clear.
        r.add(MarkKind.DAMAGE_CELL, b.minX, HEIGHT - 2, b.minZ + 1, "wall");
    }

    /** True when a shell cell is within a cell of a doorway (the hall has several; rooms have one). */
    private static boolean nearDoor(Room r, int x, int z) {
        if (r.wing() >= 0) {
            Frame f = new Frame(r);
            boolean onDoorWall = f.alongZ ? x == (f.doorHigh ? f.b.maxX - 1 : f.b.minX)
                : z == (f.doorHigh ? f.b.maxZ - 1 : f.b.minZ);
            if (!onDoorWall) return false;
            int a = f.alongZ ? z - f.b.minZ - 1 : x - f.b.minX - 1;
            return a >= f.doorLo - 1 && a <= f.doorHi + 1;
        }
        // The hall: wing doors at the middle of each side, the silo stub on the east wall's north end.
        Box b = r.bounds();
        boolean northSouth = z == b.minZ || z == b.maxZ - 1;
        boolean eastWest = x == b.minX || x == b.maxX - 1;
        if (northSouth && x >= CORRIDOR_LO - 1 && x <= CORRIDOR_HI + 1) return true;
        if (eastWest && z >= CORRIDOR_LO - 1 && z <= CORRIDOR_HI + 1) return true;
        return x == b.maxX - 1 && z <= 111;
    }

    /** Puts the marks of a room by kind. Interior cells only (one block in from the bounds). */
    private static void fillMarks(Room r, Random rnd) {
        Box b = r.bounds();
        int ix0 = b.minX + 1, ix1 = b.maxX - 2, iz0 = b.minZ + 1, iz1 = b.maxZ - 2;
        int cx = b.centerX(), cz = b.centerZ();
        for (int[] c : lampCells(b)) r.add(MarkKind.LIGHT, c[0], HEIGHT - 2, c[1], "room");
        // Damage cells scale with the area.
        int area = b.sizeX() * b.sizeZ();
        for (int i = 0, n = Math.max(3, area / 14); i < n; i++) {
            int pick = rnd.nextInt(3);
            if (pick == 2) {
                wallDamage(r, rnd);
                continue;
            }
            int x = b.minX + rnd.nextInt(b.sizeX()), z = b.minZ + rnd.nextInt(b.sizeZ());
            r.add(MarkKind.DAMAGE_CELL, x, pick == 0 ? 0 : HEIGHT - 1, z, pick == 0 ? "floor" : "roof");
        }
        if (r.wing() < 0) {
            fillHall(r);
            return;
        }
        Frame f = new Frame(r);
        doorSign(r, f);
        int L = f.lateral, D = f.depth, ma = L / 2, md = D / 2, last = L - 1;
        switch (r.kind()) {
            case LAUNCH_CONTROL:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.LAUNCH_CONTROL.name());
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.CONTROL);
                put(r, f, MarkKind.SPAWNER, last, 0, "room");
                break;
            case RADAR_TRACKING:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.RADAR.name());
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.CONTROL);
                break;
            case COMMS:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.COMMS.name());
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.COMMS);
                break;
            case COMMANDER_OFFICE:
                put(r, f, MarkKind.LOOT_CHEST, last, 0, LootKeys.SAFE);
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.STOCKPILE);
                break;
            case WARHEAD_MAGAZINE:
                for (int i = 0; i < 4; i++)
                    put(r, f, MarkKind.MACHINE, 1 + i * 3, md, MachineRole.MAGAZINE_CASING.name());
                put(r, f, MarkKind.RUBBLE, ma - 3, D - 2, "fallout");
                put(r, f, MarkKind.RUBBLE, ma + 2, 0, "fallout");
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.MAGAZINE);
                put(r, f, MarkKind.SPAWNER, last, 0, "room");
                break;
            case FUEL_DEPOT:
                put(r, f, MarkKind.MACHINE, 1, md, MachineRole.FUEL_TANK.name());
                put(r, f, MarkKind.MACHINE, last - 1, md, MachineRole.FUEL_TANK.name());
                put(r, f, MarkKind.LOOT_CHEST, ma, 0, LootKeys.FUEL);
                break;
            case GENERATOR_HALL:
                for (int i = 0; i < 3; i++) put(r, f, MarkKind.MACHINE, 2 + i * 4, md, MachineRole.GENERATOR.name());
                put(r, f, MarkKind.MACHINE, last, 0, MachineRole.BATTERY_BUFFER.name());
                put(r, f, MarkKind.SPAWNER, 0, 0, "room");
                break;
            case AIR_HANDLING:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.AIR_HANDLER.name());
                break;
            case PUMP_ROOM:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.PUMP.name());
                for (int a = 0; a <= last; a += 2) put(r, f, MarkKind.FLOOD_CELL, a, md + 1, "flood");
                break;
            case WORKSHOP:
                put(r, f, MarkKind.MACHINE, ma, md, MachineRole.WORKBENCH.name());
                // The NASA Workbench is 3x3: its mark is the center, with the cells around it free and a free row
                // between it and the back and side walls.
                put(r, f, MarkKind.MACHINE, 2, 2, MachineRole.NASA_WORKBENCH.name());
                put(r, f, MarkKind.LOOT_CHEST, last, 0, LootKeys.WORKSHOP);
                break;
            case BUNKS:
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.BUNKS);
                put(r, f, MarkKind.LOOT_CHEST, last, 0, LootKeys.BUNKS);
                put(r, f, MarkKind.SPAWNER, ma, md, "room");
                break;
            case STOCKPILE:
                for (int i = 0; i < 3; i++) put(r, f, MarkKind.LOOT_CHEST, 1 + i * 4, 0, LootKeys.STOCKPILE);
                break;
            case MESS_GALLEY:
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.GALLEY);
                break;
            case INFIRMARY:
                put(r, f, MarkKind.LOOT_CHEST, 0, 0, LootKeys.MEDICAL);
                break;
            case ARMORY:
                addFrameWall(r, ix0, ix1, iz0, iz1);
                put(r, f, MarkKind.LOOT_CHEST, ma, 0, LootKeys.ARMORY);
                put(r, f, MarkKind.SPAWNER, last, 0, "room");
                break;
            case GUARD_POST:
                put(r, f, MarkKind.SPAWNER, ma, md, "post");
                break;
            default:
                break;
        }
    }

    /**
     * The assembly bay (the hall): four wing doors at the middle of its sides and the silo stub at the north end of
     * its east wall. Chests line the north wall to either side of the north doorway; the cells inside every doorway
     * stay clear.
     */
    private static void fillHall(Room r) {
        Box b = r.bounds();
        int ix0 = b.minX + 1, ix1 = b.maxX - 2, iz0 = b.minZ + 1, iz1 = b.maxZ - 2;
        // The sign hangs on the north wall beside the north doorway, facing south into the hall.
        r.add(MarkKind.SIGN, CORRIDOR_HI + 4, 3, b.minZ, "icbm.complex.sign.assembly_bay", 0, 1);
        r.add(MarkKind.LOOT_CHEST, ix0 + 2, 1, iz0, LootKeys.SALVAGE);
        r.add(MarkKind.LOOT_CHEST, ix0 + 6, 1, iz0, LootKeys.SALVAGE);
        r.add(MarkKind.LOOT_CHEST, ix1 - 6, 1, iz0, LootKeys.SALVAGE);
        r.add(MarkKind.LOOT_CHEST, ix1 - 2, 1, iz0, LootKeys.SALVAGE);
        r.add(MarkKind.MACHINE, b.centerX(), 1, b.centerZ(), MachineRole.WORKBENCH.name());
        r.add(MarkKind.SPAWNER, ix0 + 2, 1, iz1 - 2, "room");
        r.add(MarkKind.SPAWNER, ix1 - 2, 1, iz1 - 2, "room");
    }
}
