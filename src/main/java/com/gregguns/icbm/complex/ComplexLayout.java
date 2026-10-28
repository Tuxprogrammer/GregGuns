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
            radars.add(new int[] { 24 + rnd.nextInt(72), 150 + rnd.nextInt(66) });
        }
        return new ComplexPlan(seed, cellX, cellZ, hall, rooms, tunnels, silos, esc[0], esc[1], radars);
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
        tunnels.add(new Tunnel(new Box(138, 0, 106, 142, HEIGHT, 111), -1));
        tunnels.add(new Tunnel(new Box(140, 0, minZ, 145, HEIGHT, 111), -1));
        for (int r = 0; r < 2; r++) {
            if (!row[r]) continue;
            int oz = SILO_ORIGINS[r * 2][1];
            tunnels.add(new Tunnel(new Box(145, 0, oz - 6, 196, HEIGHT, oz), -1));
        }
    }

    private static void addTunnelMarks(Tunnel t, int wing, Random rnd) {
        Box b = t.bounds();
        boolean alongZ = wing == N || wing == S;
        int len = alongZ ? b.sizeZ() : b.sizeX();
        int mid = alongZ ? b.centerX() : b.centerZ();
        for (int p = 6; p < len; p += 8) {
            int x = alongZ ? mid : (wing == E ? b.minX + p : b.maxX - 1 - p);
            int z = alongZ ? (wing == S ? b.minZ + p : b.maxZ - 1 - p) : mid;
            t.add(MarkKind.LIGHT, x, HEIGHT - 2, z, "emergency");
        }
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

    /** Puts the marks of a room by kind. Interior cells only (one block in from the bounds). */
    private static void fillMarks(Room r, Random rnd) {
        Box b = r.bounds();
        int ix0 = b.minX + 1, ix1 = b.maxX - 2, iz0 = b.minZ + 1, iz1 = b.maxZ - 2;
        int cx = b.centerX(), cz = b.centerZ();
        // Lights: the four inner corners and the center.
        r.add(MarkKind.LIGHT, ix0, HEIGHT - 2, iz0, "room");
        r.add(MarkKind.LIGHT, ix1, HEIGHT - 2, iz0, "room");
        r.add(MarkKind.LIGHT, ix0, HEIGHT - 2, iz1, "room");
        r.add(MarkKind.LIGHT, ix1, HEIGHT - 2, iz1, "room");
        r.add(MarkKind.LIGHT, cx, HEIGHT - 2, cz, "room");
        r.add(
            MarkKind.SIGN,
            cx,
            3,
            b.minZ,
            "icbm.complex.sign." + r.kind()
                .name()
                .toLowerCase(java.util.Locale.ROOT));
        // Damage cells scale with the area.
        int area = b.sizeX() * b.sizeZ();
        for (int i = 0, n = Math.max(3, area / 14); i < n; i++) {
            int x = b.minX + rnd.nextInt(b.sizeX()), z = b.minZ + rnd.nextInt(b.sizeZ());
            int pick = rnd.nextInt(3);
            r.add(
                MarkKind.DAMAGE_CELL,
                x,
                pick == 0 ? 0 : pick == 1 ? HEIGHT - 1 : 1 + rnd.nextInt(HEIGHT - 2),
                z,
                pick == 0 ? "floor" : pick == 1 ? "roof" : "wall");
        }
        switch (r.kind()) {
            case ASSEMBLY_BAY:
                for (int i = 0; i < 4; i++) r.add(MarkKind.LOOT_CHEST, ix0 + 2 + i * 8, 1, iz0, LootKeys.SALVAGE);
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.WORKBENCH.name());
                r.add(MarkKind.SPAWNER, ix0 + 2, 1, iz1 - 2, "room");
                r.add(MarkKind.SPAWNER, ix1 - 2, 1, iz1 - 2, "room");
                break;
            case LAUNCH_CONTROL:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.LAUNCH_CONTROL.name());
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.CONTROL);
                r.add(MarkKind.SPAWNER, ix1, 1, iz1, "room");
                break;
            case RADAR_TRACKING:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.RADAR.name());
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.CONTROL);
                break;
            case COMMS:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.COMMS.name());
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.COMMS);
                break;
            case COMMANDER_OFFICE:
                r.add(MarkKind.LOOT_CHEST, ix1, 1, iz1, LootKeys.SAFE);
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.STOCKPILE);
                break;
            case WARHEAD_MAGAZINE:
                for (int i = 0; i < 4; i++)
                    r.add(MarkKind.MACHINE, ix0 + 1 + i * 3, 1, cz, MachineRole.MAGAZINE_CASING.name());
                r.add(MarkKind.RUBBLE, cx, 1, iz0, "fallout");
                r.add(MarkKind.RUBBLE, cx + 2, 1, iz1, "fallout");
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.MAGAZINE);
                r.add(MarkKind.SPAWNER, ix1, 1, iz1, "room");
                break;
            case FUEL_DEPOT:
                r.add(MarkKind.MACHINE, ix0 + 1, 1, cz, MachineRole.FUEL_TANK.name());
                r.add(MarkKind.MACHINE, ix1 - 1, 1, cz, MachineRole.FUEL_TANK.name());
                r.add(MarkKind.LOOT_CHEST, cx, 1, iz1, LootKeys.FUEL);
                break;
            case GENERATOR_HALL:
                for (int i = 0; i < 3; i++)
                    r.add(MarkKind.MACHINE, ix0 + 2 + i * 4, 1, cz, MachineRole.GENERATOR.name());
                r.add(MarkKind.MACHINE, ix1, 1, iz0, MachineRole.BATTERY_BUFFER.name());
                r.add(MarkKind.SPAWNER, ix0, 1, iz1, "room");
                break;
            case AIR_HANDLING:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.AIR_HANDLER.name());
                break;
            case PUMP_ROOM:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.PUMP.name());
                for (int x = ix0; x <= ix1; x += 2) r.add(MarkKind.FLOOD_CELL, x, 1, cz + 1, "flood");
                break;
            case WORKSHOP:
                r.add(MarkKind.MACHINE, cx, 1, cz, MachineRole.WORKBENCH.name());
                r.add(MarkKind.MACHINE, ix0, 1, iz0, MachineRole.NASA_WORKBENCH.name());
                r.add(MarkKind.LOOT_CHEST, ix1, 1, iz1, LootKeys.WORKSHOP);
                break;
            case BUNKS:
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.BUNKS);
                r.add(MarkKind.LOOT_CHEST, ix1, 1, iz1, LootKeys.BUNKS);
                r.add(MarkKind.SPAWNER, cx, 1, cz, "room");
                break;
            case STOCKPILE:
                for (int i = 0; i < 3; i++) r.add(MarkKind.LOOT_CHEST, ix0 + 1 + i * 4, 1, iz1, LootKeys.STOCKPILE);
                break;
            case MESS_GALLEY:
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.GALLEY);
                break;
            case INFIRMARY:
                r.add(MarkKind.LOOT_CHEST, ix0, 1, iz1, LootKeys.MEDICAL);
                break;
            case ARMORY:
                r.add(MarkKind.ITEM_FRAME_WALL, ix0, 2, b.minZ, LootKeys.ARMORY);
                r.add(MarkKind.ITEM_FRAME_WALL, ix1, 2, b.minZ, LootKeys.ARMORY);
                r.add(MarkKind.LOOT_CHEST, cx, 1, iz1, LootKeys.ARMORY);
                r.add(MarkKind.SPAWNER, ix1, 1, iz1, "room");
                break;
            case GUARD_POST:
                r.add(MarkKind.SPAWNER, cx, 1, cz, "post");
                break;
            default:
                break;
        }
    }
}
