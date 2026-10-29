package com.gregguns.icbm.complex;

/**
 * A {@link ComplexPlan} rasterized to cell roles, in complex-local coordinates: x and z in [0, SIZE), y in [0, LEVELS).
 * Each section writes its own shell (perimeter walls, floor, ceiling) and interior; sections touch but never overlap,
 * so a wall between two of them is two blocks thick and each side shows its own room's wall. Then doors, door frames,
 * windows, rails, lamps, furniture and the silos. Every column also records the section that owns it, so the writer
 * resolves a role through that section's palette style. Everything above the level (silo shafts, the escape ladder) is
 * the placer's job because it depends on the ground height.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Raster)
public final class ComplexRaster {

    /** Layers held: the silo, the hub and the gallery are the tallest pieces. */
    public static final int LEVELS = ComplexLayout.SILO_HEIGHT;
    private static final int S = ComplexLayout.SIZE;

    private final byte[] cells = new byte[S * LEVELS * S];
    /** Section id + 1 per column; 0 for rock and silos. */
    private final short[] owner = new short[S * S];
    private final ComplexPlan plan;

    public ComplexRaster(ComplexPlan plan) {
        this.plan = plan;
        build();
    }

    public ComplexPlan plan() {
        return plan;
    }

    public byte at(int x, int y, int z) {
        if (x < 0 || x >= S || z < 0 || z >= S || y < 0 || y >= LEVELS) return Cell.NONE;
        return cells[(x * LEVELS + y) * S + z];
    }

    /** @return the id of the section that owns the column, or -1 for rock and silos */
    public int ownerAt(int x, int z) {
        if (x < 0 || x >= S || z < 0 || z >= S) return -1;
        return owner[x * S + z] - 1;
    }

    /** @return how many columns hold anything (a section or a silo): the built footprint seen from above */
    public int coveredColumns() {
        int n = 0;
        for (int x = 0; x < S; x++) {
            for (int z = 0; z < S; z++) {
                for (int y = 0; y < LEVELS; y++) {
                    if (at(x, y, z) != Cell.NONE) {
                        n++;
                        break;
                    }
                }
            }
        }
        return n;
    }

    private void set(int x, int y, int z, byte c) {
        if (x < 0 || x >= S || z < 0 || z >= S || y < 0 || y >= LEVELS) return;
        cells[(x * LEVELS + y) * S + z] = c;
    }

    private void section(int id, Box b) {
        int h = b.sizeY();
        for (int x = b.minX; x < b.maxX; x++) {
            for (int z = b.minZ; z < b.maxZ; z++) {
                owner[x * S + z] = (short) (id + 1);
                boolean edge = x == b.minX || x == b.maxX - 1 || z == b.minZ || z == b.maxZ - 1;
                for (int y = 0; y < h; y++) {
                    byte c = edge ? Cell.WALL : y == 0 ? Cell.FLOOR : y == h - 1 ? Cell.CEILING : Cell.AIR;
                    set(x, y, z, c);
                }
            }
        }
    }

    private void build() {
        section(
            plan.hall()
                .id(),
            plan.hall()
                .bounds());
        for (Room r : plan.rooms()) section(r.id(), r.bounds());
        for (Tunnel t : plan.tunnels()) section(t.id(), t.bounds());
        for (Door d : plan.doors()) open(d);
        for (Door d : plan.doors()) if (d.kind == Door.Kind.DOOR || d.kind == Door.Kind.PORTAL) frame(d);
        for (SiloSlot s : plan.silos()) silo(s);
        for (Mark m : plan.allMarks()) {
            if (m.kind() == MarkKind.LIGHT) {
                set(m.x(), m.y(), m.z(), Cell.LAMP);
            } else if (m.kind() == MarkKind.FURNITURE) {
                set(m.x(), m.y(), m.z(), furniture(m.key()));
            }
        }
        // After the lamps: a rail needs a solid top under it, and a lit floor tile (glowstone) is not one.
        for (Tunnel t : plan.tunnels()) if (t.major()) rails(t);
        // The escape ladder inside the level, up through its section's ceiling.
        for (int y = 1; y < plan.escapeTop(); y++) set(plan.escapeX(), y, plan.escapeZ(), Cell.LADDER);
    }

    private static byte furniture(String key) {
        // A small room's furniture piece (D259): its style cube, or open where it has none.
        FurniturePiece piece = FurniturePiece.of(key);
        if (piece != null) return piece.fallbackRole();
        switch (key) {
            case "TABLE":
                return Cell.FURN_TABLE;
            case "CONSOLE":
                return Cell.FURN_CONSOLE;
            case "BUNK":
                return Cell.FURN_BUNK;
            case "BASIN":
                return Cell.BASIN;
            default:
                return Cell.FURN_SHELF;
        }
    }

    /** Cuts a door through both wall layers (clear glass for a window). */
    private void open(Door d) {
        byte c = d.kind == Door.Kind.WINDOW ? Cell.WINDOW : Cell.AIR;
        for (int x = d.x0; x <= d.x1; x++) {
            for (int z = d.z0; z <= d.z1; z++) for (int y = d.y0; y <= d.y1; y++) set(x, y, z, c);
        }
    }

    /** Jambs either side of the opening and a lintel over it, in both wall layers: the DOOR role. */
    private void frame(Door d) {
        boolean xs = d.inXWall();
        int lo = xs ? d.x0 : d.z0, hi = xs ? d.x1 : d.z1;
        int t0 = xs ? d.z0 : d.x0, t1 = xs ? d.z1 : d.x1;
        for (int t = t0; t <= t1; t++) {
            for (int y = d.y0; y <= d.y1 + 1; y++) {
                frameCell(xs, lo - 1, t, y);
                frameCell(xs, hi + 1, t, y);
            }
            for (int a = lo; a <= hi; a++) frameCell(xs, a, t, d.y1 + 1);
        }
    }

    private void frameCell(boolean xs, int a, int t, int y) {
        int x = xs ? a : t, z = xs ? t : a;
        if (at(x, y, z) == Cell.WALL) set(x, y, z, Cell.DOOR_FRAME);
    }

    /** Offsets from a hall's centerline tried for its rail line, nearest first. */
    private static final int[] RAIL_OFFSETS = { 0, 1, -1, 2, -2 };

    /**
     * A rail down a major hall, on the line nearest its center that crosses no floor light tile: a rail on glowstone
     * drops off at the first block update next to it. Rails go only over a FLOOR cell, so a line with no clear choice
     * leaves a gap over each tile rather than a rail that breaks.
     */
    private void rails(Tunnel t) {
        Box b = t.bounds();
        boolean ew = t.alongX();
        int lo = ew ? b.minX + 1 : b.minZ + 1, hi = ew ? b.maxX - 1 : b.maxZ - 1;
        int center = ew ? b.centerZ() : b.centerX();
        int line = center;
        for (int off : RAIL_OFFSETS) {
            boolean clear = true;
            for (int a = lo; a < hi && clear; a++) {
                if (at(ew ? a : center + off, 0, ew ? center + off : a) == Cell.LAMP) clear = false;
            }
            if (clear) {
                line = center + off;
                break;
            }
        }
        for (int a = lo; a < hi; a++) {
            int x = ew ? a : line, z = ew ? line : a;
            if (at(x, 0, z) == Cell.FLOOR && at(x, 1, z) == Cell.AIR) set(x, 1, z, ew ? Cell.RAIL_EW : Cell.RAIL_NS);
        }
    }

    /** The silo's structure piece, turned so its controller faces the gallery. */
    private void silo(SiloSlot s) {
        String[][] piece = com.gregguns.icbm.silo.SiloShape.piece();
        for (int a = 0; a < 5; a++) for (int r = 0; r < 12; r++) for (int c = 0; c < 5; c++) {
            char ch = piece[a][r].charAt(c);
            byte cell;
            switch (ch) {
                case 'C':
                case 'F':
                    cell = Cell.SILO_CONCRETE;
                    break;
                case 'G':
                    cell = Cell.SILO_GLASS;
                    break;
                case 'B':
                    cell = Cell.SILO_BEAM;
                    break;
                case 'P':
                    cell = Cell.SILO_PAD;
                    break;
                case '~':
                    cell = s.missing() == MissingPart.CONTROLLER ? Cell.SILO_CONCRETE : Cell.CONTROLLER;
                    break;
                default:
                    cell = (a >= 1 && a <= 3 && c >= 1 && c <= 3) ? Cell.AIR : Cell.SILO_CONCRETE;
                    break;
            }
            set(s.x(c, a), 11 - r, s.z(c, a), cell);
        }
        // Hatches around the controller (column 2, y 2, front aisle): bus and maintenance beside it, energy and power
        // below.
        set(s.x(1, 0), 2, s.z(1, 0), Cell.HATCH_BUS);
        set(s.x(3, 0), 2, s.z(3, 0), Cell.HATCH_MAINTENANCE);
        set(s.x(3, 0), 1, s.z(3, 0), Cell.HATCH_POWER);
        if (s.missing() != MissingPart.ENERGY_HATCH) set(s.x(2, 0), 1, s.z(2, 0), Cell.HATCH_ENERGY);
    }
}
