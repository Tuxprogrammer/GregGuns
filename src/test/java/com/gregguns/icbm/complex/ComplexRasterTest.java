package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

class ComplexRasterTest {

    private static boolean open(byte c) {
        return Cell.open(c);
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
                    .bounds().minX + 1,
                    p.hall()
                        .bounds().minZ + 1 });
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
                Box b = room.bounds();
                boolean any = false;
                for (int x = b.minX + 1; x < b.maxX - 1 && !any; x++) {
                    for (int z = b.minZ + 1; z < b.maxZ - 1; z++) any |= seen[x][z];
                }
                assertTrue(any, "seed " + s + " " + room.kind());
            }
            for (SiloSlot silo : p.silos()) {
                assertTrue(seen[silo.frontX()][silo.frontZ()], "seed " + s + " silo front");
            }
        }
    }

    /**
     * The owner's layout, checked against the viewer in front of the controller (not the shape's columns): Energy
     * Hatch and Input Bus on the right, Input Hatch and Maintenance Hatch on the left, glass in every other window
     * cell.
     */
    @Test
    void hatchesStandInTheOuterColumnsAndTheRestOfTheWindowIsGlass() {
        int[] rightX = { -1, 0, 1, 0 }, rightZ = { 0, -1, 0, 1 }; // viewer's right, by facing 0 north..3 west
        for (long s = 0; s < 40; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            for (SiloSlot silo : p.silos()) {
                int rx = rightX[silo.facing()], rz = rightZ[silo.facing()];
                int cx = silo.controllerX(), cz = silo.controllerZ();
                String at = "seed " + s + " facing " + silo.facing();
                if (silo.missing() != MissingPart.ENERGY_HATCH) {
                    assertEquals(Cell.HATCH_ENERGY, r.at(cx + rx, 2, cz + rz), at + " energy on the right");
                } else {
                    assertEquals(Cell.SILO_GLASS, r.at(cx + rx, 2, cz + rz), at + " missing energy hatch is glass");
                }
                assertEquals(Cell.HATCH_BUS, r.at(cx + rx, 1, cz + rz), at + " bus under it");
                assertEquals(Cell.HATCH_POWER, r.at(cx - rx, 2, cz - rz), at + " input hatch on the left");
                assertEquals(Cell.HATCH_MAINTENANCE, r.at(cx - rx, 1, cz - rz), at + " maintenance under it");
                assertEquals(Cell.SILO_GLASS, r.at(cx, 1, cz), at + " glass under the controller");
                assertEquals(Cell.SILO_GLASS, r.at(cx, 3, cz), at + " glass over the controller");
                assertEquals(Cell.SILO_GLASS, r.at(cx + rx, 3, cz + rz), at + " glass over the right column");
                assertEquals(Cell.SILO_GLASS, r.at(cx - rx, 3, cz - rz), at + " glass over the left column");
            }
        }
    }

    @Test
    void eachSiloIsMissingExactlyOnePart() {
        for (long s = 0; s < 60; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            for (SiloSlot silo : p.silos()) {
                boolean controller = r.at(silo.controllerX(), 2, silo.controllerZ()) == Cell.CONTROLLER;
                int[] e = silo.hatchCell(Cell.HATCH_ENERGY);
                boolean energy = r.at(e[0], e[1], e[2]) == Cell.HATCH_ENERGY;
                assertEquals(silo.missing() == MissingPart.CONTROLLER, !controller);
                assertEquals(silo.missing() == MissingPart.ENERGY_HATCH, !energy);
                assertEquals(Cell.SILO_PAD, r.at(silo.shaftX(), 0, silo.shaftZ()));
                assertEquals(Cell.AIR, r.at(silo.shaftX(), 5, silo.shaftZ()), "the bore is open");
                // The controller sits in the face toward the gallery: the walkway in front of it is open.
                assertEquals(Cell.AIR, r.at(silo.frontX(), 2, silo.frontZ()), "walkway open");
            }
        }
    }
}
