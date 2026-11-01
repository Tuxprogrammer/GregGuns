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
     * The owner's layout (D299), checked against the viewer in front of the controller (not the shape's columns): two
     * out from the controller, the Input Bus over the Energy Hatch on the right with the Launch Control on top, the
     * Input
     * Hatch over the Maintenance Hatch on the left with concrete on top; clear glass in all eight window cells; a
     * concrete band over the five-wide face.
     */
    @Test
    void hatchColumnsFlankAClearThreeByThreeWindow() {
        int[] rightX = { -1, 0, 1, 0 }, rightZ = { 0, -1, 0, 1 }; // viewer's right, by facing 0 north..3 west
        for (long s = 0; s < 40; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            for (SiloSlot silo : p.silos()) {
                int rx = rightX[silo.facing()], rz = rightZ[silo.facing()];
                int cx = silo.controllerX(), cz = silo.controllerZ();
                String at = "seed " + s + " facing " + silo.facing();
                assertEquals(Cell.HATCH_BUS, r.at(cx + 2 * rx, 2, cz + 2 * rz), at + " bus on the right");
                if (silo.missing() != MissingPart.ENERGY_HATCH) {
                    assertEquals(Cell.HATCH_ENERGY, r.at(cx + 2 * rx, 1, cz + 2 * rz), at + " energy under it");
                } else {
                    assertEquals(
                        Cell.SILO_CONCRETE,
                        r.at(cx + 2 * rx, 1, cz + 2 * rz),
                        at + " a missing energy hatch leaves concrete");
                }
                assertEquals(
                    Cell.SILO_LAUNCH_CONTROL,
                    r.at(cx + 2 * rx, 3, cz + 2 * rz),
                    at + " the silo's own Launch Control tops the right column");
                assertEquals(Cell.HATCH_FUEL, r.at(cx - 2 * rx, 2, cz - 2 * rz), at + " input hatch on the left");
                assertEquals(Cell.HATCH_MAINTENANCE, r.at(cx - 2 * rx, 1, cz - 2 * rz), at + " maintenance under it");
                assertEquals(Cell.SILO_CONCRETE, r.at(cx - 2 * rx, 3, cz - 2 * rz), at + " concrete tops the left");
                for (int a = -1; a <= 1; a++) {
                    for (int y = 1; y <= 3; y++) {
                        if (a == 0 && y == 2) continue;
                        assertEquals(
                            Cell.SILO_GLASS,
                            r.at(cx + a * rx, y, cz + a * rz),
                            at + " glass at window cell " + a + ", " + y);
                    }
                }
                for (int a = -2; a <= 2; a++) {
                    assertEquals(
                        Cell.SILO_CONCRETE,
                        r.at(cx + a * rx, 4, cz + a * rz),
                        at + " concrete over the window at " + a);
                }
            }
        }
    }

    /** The walkway (D299) opens the gallery wall in front of the whole five-wide face, from the floor to y 4. */
    @Test
    void everySiloWalkwayIsFiveWideAndFourHigh() {
        for (long s = 0; s < 40; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            int walkways = 0;
            for (Door d : p.doors()) {
                if (d.kind != Door.Kind.WALKWAY) continue;
                walkways++;
                assertEquals(5, Math.max(d.x1 - d.x0, d.z1 - d.z0) + 1, "seed " + s + " walkway width " + d);
                assertEquals(0, Math.min(d.x1 - d.x0, d.z1 - d.z0), "seed " + s + " walkway depth " + d);
                assertEquals(1, d.y0, "seed " + s + " walkway floor " + d);
                assertEquals(4, d.y1, "seed " + s + " walkway top " + d);
                for (int x = d.x0; x <= d.x1; x++) {
                    for (int z = d.z0; z <= d.z1; z++) {
                        for (int y = d.y0; y <= d.y1; y++) {
                            assertEquals(
                                Cell.AIR,
                                r.at(x, y, z),
                                "seed " + s + " walkway cell " + x + "," + y + "," + z);
                        }
                    }
                }
            }
            assertEquals(
                p.silos()
                    .size(),
                walkways,
                "seed " + s + ": one walkway per silo");
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

    @Test
    void everySiloHasItsOwnLaunchControlOnTopOfTheRightHatchColumn() {
        int col = com.gregguns.icbm.silo.SiloShape.LAUNCH_CONTROL_COLUMN;
        int y = 2 + com.gregguns.icbm.silo.SiloShape.LAUNCH_CONTROL_RISE;
        for (long s = 0; s < 60; s++) {
            ComplexPlan p = ComplexLayout.generate(s, 5, 1);
            ComplexRaster r = new ComplexRaster(p);
            for (SiloSlot silo : p.silos()) {
                assertEquals(Cell.SILO_LAUNCH_CONTROL, r.at(silo.x(col, 0), y, silo.z(col, 0)), "seed " + s);
                assertTrue(Cell.siloMachine(Cell.SILO_LAUNCH_CONTROL));
            }
        }
    }
}
