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
        ComplexPlan p = t.length > 3 ? ComplexLayout.generate(seed, Integer.parseInt(t[2]), Integer.parseInt(t[3]))
            : ComplexLayout.generate(seed, (int) seed, 3);
        int y = t.length > 4 ? Integer.parseInt(t[4]) : 1;
        ComplexRaster raster = new ComplexRaster(p);
        for (Room r : p.rooms()) {
            boolean parentOf = false;
            for (Room c : p.rooms()) parentOf |= c.parent() == r.id() && (section >= 0 ? c.id() == section : c.kind() == kind);
            if (!parentOf && (section >= 0 ? r.id() != section : r.kind() != kind)) continue;
            Box b = r.bounds();
            System.out.println("DEBUG " + r.kind() + " id " + r.id() + " parent " + r.parent() + " " + b + " door side " + r.doorSide() + " y " + y);
            for (Door d : p.doors()) if (d.joins(r.id())) System.out.println("  door " + d);
            for (int z = b.minZ; z < b.maxZ; z++) {
                StringBuilder sb = new StringBuilder(String.format("%4d ", z));
                for (int x = b.minX; x < b.maxX; x++) {
                    char c = Cell.open(raster.at(x, y, z)) ? '.' : '#';
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
