package com.gregguns.icbm.complex;

/**
 * A {@link ComplexPlan} rasterized to cells, in complex-local coordinates: x and z in [0, SIZE), y in [0, LEVELS).
 * Shells first, then interiors, then doors, so every box is sealed except where a door is cut. Everything above the
 * level (silo shafts, the escape ladder) is the placer's job because it depends on the ground height.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md
public final class ComplexRaster {

    /** Layers held: the silo is the tallest piece. */
    public static final int LEVELS = ComplexLayout.SILO_HEIGHT;
    private static final int S = ComplexLayout.SIZE;

    private final byte[] cells = new byte[S * LEVELS * S];
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

    private void set(int x, int y, int z, byte c) {
        cells[(x * LEVELS + y) * S + z] = c;
    }

    private void shell(Box b) {
        for (int x = b.minX; x < b.maxX; x++) for (int z = b.minZ; z < b.maxZ; z++) for (int y = 0; y < b.maxY; y++) {
            if (at(x, y, z) == Cell.NONE) set(x, y, z, Cell.WALL);
        }
    }

    private void interior(Box b) {
        for (int x = b.minX + 1; x < b.maxX - 1; x++) for (int z = b.minZ + 1; z < b.maxZ - 1; z++) {
            for (int y = 1; y < b.maxY - 1; y++) set(x, y, z, Cell.AIR);
        }
    }

    /** Opens a 3-high doorway (y 1 to 3) over the inclusive x and z ranges. */
    private void door(int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) for (int y = 1; y <= 3; y++) set(x, y, z, Cell.AIR);
    }

    private void build() {
        java.util.List<Box> boxes = new java.util.ArrayList<Box>();
        boxes.add(
            plan.hall()
                .bounds());
        for (Room r : plan.rooms()) boxes.add(r.bounds());
        for (Tunnel t : plan.tunnels()) boxes.add(t.bounds());
        for (Box b : boxes) shell(b);
        for (Box b : boxes) interior(b);
        // Wing corridors open into the hall; rooms open onto their corridor.
        for (Tunnel t : plan.tunnels()) {
            if (t.wing() >= 0) wingDoor(t.bounds(), t.wing());
        }
        for (Room r : plan.rooms()) if (r.wing() >= 0) roomDoor(r.bounds(), r.wing());
        // The silo cluster: stub to the hall, row corridors to the trunk.
        door(137, 138, 107, 109);
        for (Tunnel t : plan.tunnels()) {
            Box b = t.bounds();
            if (t.wing() < 0 && b.minX == 145) door(144, 145, b.minZ + 2, b.maxZ - 3);
        }
        for (SiloSlot s : plan.silos()) silo(s);
        for (Tunnel t : plan.tunnels()) if (t.wing() >= 0) rails(t.bounds(), t.wing());
        // The escape ladder cell inside the level.
        set(plan.escapeX(), 1, plan.escapeZ(), Cell.LADDER);
        for (int y = 2; y < ComplexLayout.HEIGHT; y++) set(plan.escapeX(), y, plan.escapeZ(), Cell.LADDER);
    }

    private void wingDoor(Box b, int wing) {
        int mid = (wing == ComplexLayout.N || wing == ComplexLayout.S) ? b.centerX() : b.centerZ();
        switch (wing) {
            case ComplexLayout.N:
                door(mid - 1, mid + 1, b.maxZ - 1, b.maxZ);
                break;
            case ComplexLayout.S:
                door(mid - 1, mid + 1, b.minZ - 1, b.minZ);
                break;
            case ComplexLayout.E:
                door(b.minX - 1, b.minX, mid - 1, mid + 1);
                break;
            default:
                door(b.maxX - 1, b.maxX, mid - 1, mid + 1);
                break;
        }
    }

    private void roomDoor(Box r, int wing) {
        boolean alongZ = wing == ComplexLayout.N || wing == ComplexLayout.S;
        if (alongZ) {
            int cz = r.centerZ();
            if (r.maxX <= 118) door(r.maxX - 1, r.maxX, cz - 1, cz);
            else door(r.minX - 1, r.minX, cz - 1, cz);
        } else {
            int cx = r.centerX();
            if (r.maxZ <= 118) door(cx - 1, cx, r.maxZ - 1, r.maxZ);
            else door(cx - 1, cx, r.minZ - 1, r.minZ);
        }
    }

    private void rails(Box b, int wing) {
        boolean alongZ = wing == ComplexLayout.N || wing == ComplexLayout.S;
        byte c = alongZ ? Cell.RAIL_NS : Cell.RAIL_EW;
        int mid = alongZ ? b.centerX() : b.centerZ();
        if (alongZ) for (int z = b.minZ + 1; z < b.maxZ - 1; z++) set(mid, 1, z, c);
        else for (int x = b.minX + 1; x < b.maxX - 1; x++) set(x, 1, mid, c);
    }

    private void silo(SiloSlot s) {
        String[][] piece = com.gregguns.icbm.silo.SiloShape.piece();
        int ox = s.bounds().minX, oz = s.bounds().minZ;
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
            set(ox + c, 11 - r, oz + a, cell);
        }
        // Hatches around the controller at (ox+2, y 2, oz): bus and maintenance beside it, energy and power below.
        set(ox + 1, 2, oz, Cell.HATCH_BUS);
        set(ox + 3, 2, oz, Cell.HATCH_MAINTENANCE);
        set(ox + 3, 1, oz, Cell.HATCH_POWER);
        if (s.missing() != MissingPart.ENERGY_HATCH) set(ox + 2, 1, oz, Cell.HATCH_ENERGY);
        // The walkway in front of the controller (the row corridor's south wall is opened).
        door(ox + 1, ox + 3, oz - 1, oz - 1);
    }
}
