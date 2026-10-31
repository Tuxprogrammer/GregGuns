package com.gregguns.icbm.complex;

import org.junit.jupiter.api.Test;

/** Temporary: prints one room's raster at foot height with its marks, for debugging. */
class DebugRoomDumpTest {

    @Test
    void dump() {
        String spec = System.getenv("GG_DEBUG_ROOM");
        if (spec == null) return;
        String[] t = spec.split(",");
        long seed = Long.parseLong(t[0]);
        int section = t[1].startsWith("#") ? Integer.parseInt(t[1].substring(1)) : -1;
        RoomKind kind = section >= 0 ? null : RoomKind.valueOf(t[1]);
        ComplexPlan p = ComplexLayout.generate(seed, (int) seed, 3);
        ComplexRaster raster = new ComplexRaster(p);
        for (Room r : p.rooms()) {
            if (section >= 0 ? r.id() != section : r.kind() != kind) continue;
            Box b = r.bounds();
            System.out.println("DEBUG " + r.kind() + " " + b + " door side " + r.doorSide());
            for (int z = b.minZ; z < b.maxZ; z++) {
                StringBuilder sb = new StringBuilder(String.format("%4d ", z));
                for (int x = b.minX; x < b.maxX; x++) {
                    char c = Cell.open(raster.at(x, 1, z)) ? '.' : '#';
                    for (Mark m : r.marks()) {
                        if (m.x() != x || m.z() != z) continue;
                        if (m.y() == 1 && m.kind() == MarkKind.LOOT_CHEST) c = 'C';
                        else if (m.y() == 1 && m.kind() == MarkKind.MACHINE) c = 'M';
                        else if (m.y() == 1 && m.kind() == MarkKind.GUN_DISPLAY) c = 'D';
                        else if (m.y() == 1 && m.kind() == MarkKind.SPAWNER) c = 'G';
                        else if (m.y() == 1 && m.kind() == MarkKind.FURNITURE && c == '.') c = 'f';
                    }
                    sb.append(c);
                }
                System.out.println(sb);
            }
        }
    }
}
