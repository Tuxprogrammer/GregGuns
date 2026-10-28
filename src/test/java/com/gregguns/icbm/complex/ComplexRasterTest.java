package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

class ComplexRasterTest {

    private static boolean open(byte c) {
        return c == Cell.AIR || c == Cell.RAIL_NS || c == Cell.RAIL_EW || c == Cell.LADDER;
    }

    /** Flood fill from the hall floor level: every room interior and every silo walkway is reachable on foot. */
    @Test
    void everyRoomAndEverySiloFrontIsReachableFromTheHall() {
        for (long s = 0; s < 40; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 2, 2);
            ComplexRaster r = new ComplexRaster(p);
            boolean[][] seen = new boolean[ComplexLayout.SIZE][ComplexLayout.SIZE];
            Deque<int[]> q = new ArrayDeque<int[]>();
            q.add(
                new int[] { p.hall()
                    .bounds()
                    .centerX(),
                    p.hall()
                        .bounds()
                        .centerZ() });
            seen[q.peek()[0]][q.peek()[1]] = true;
            int[][] d = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] k : d) {
                    int x = c[0] + k[0], z = c[1] + k[1];
                    if (x < 0 || z < 0 || x >= ComplexLayout.SIZE || z >= ComplexLayout.SIZE || seen[x][z]) continue;
                    if (!open(r.at(x, 2, z))) continue;
                    seen[x][z] = true;
                    q.add(new int[] { x, z });
                }
            }
            for (Room room : p.rooms()) {
                assertTrue(
                    seen[room.bounds()
                        .centerX()][room.bounds()
                            .centerZ()],
                    "seed " + s + " " + room.kind());
            }
            for (SiloSlot silo : p.silos()) {
                assertTrue(seen[silo.bounds().minX + 2][silo.bounds().minZ - 1], "seed " + s + " silo front");
            }
        }
    }

    @Test
    void eachSiloIsMissingExactlyOnePart() {
        for (long s = 0; s < 60; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            for (SiloSlot silo : p.silos()) {
                int ox = silo.bounds().minX, oz = silo.bounds().minZ;
                boolean controller = r.at(ox + 2, 2, oz) == Cell.CONTROLLER;
                boolean energy = r.at(ox + 2, 1, oz) == Cell.HATCH_ENERGY;
                assertEquals(silo.missing() == MissingPart.CONTROLLER, !controller);
                assertEquals(silo.missing() == MissingPart.ENERGY_HATCH, !energy);
                assertEquals(Cell.SILO_PAD, r.at(ox + 2, 0, oz + 2));
                assertEquals(Cell.AIR, r.at(ox + 2, 5, oz + 2), "the bore is open");
            }
        }
    }
}
