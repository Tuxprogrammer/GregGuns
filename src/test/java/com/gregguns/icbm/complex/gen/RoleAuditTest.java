package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.palette.BlockRef;
import com.gregguns.icbm.complex.palette.RoomStyle;
import com.gregguns.icbm.worldgen.ActivityLevel;

/**
 * The role audit's core (D267) over whole generated complexes: with the blocks every section's style resolves to,
 * cell by cell as the writer places them, no section's floor, wall and ceiling share a block, in the full pack, with
 * vanilla only and with a partial pack, on every dimension and activity level. And the audit does fail: one block in
 * every role is reported once per section, with its roles.
 */
class RoleAuditTest {

    private static final BlockRef.Lookup ALL = (m, n) -> true;
    private static final BlockRef.Lookup VANILLA = (m, n) -> BlockRef.VANILLA.equals(m);
    /** GregTech and Galacticraft without Chisel or Ztones: every set on its second chain entries. */
    private static final BlockRef.Lookup NO_CHISEL = (m, n) -> !m.equals("chisel") && !m.equals("Ztones");

    /** What the writer places at a role cell of a plan, as a registry name, through the pure palette. */
    private static RoleAudit.Reader reader(ComplexPlan plan, ComplexRaster raster, String dim, ActivityLevel level,
        BlockRef.Lookup lookup) {
        Map<Integer, RoomStyle> styles = new HashMap<>();
        Map<Integer, RoomStyle.Roles> roles = new HashMap<>();
        return (x, y, z) -> {
            byte c = raster.at(x, y, z);
            int id = raster.ownerAt(x, z);
            if (id < 0) return null;
            RoomStyle s = styles.computeIfAbsent(id, k -> PaletteStyles.styleOf(plan, k, dim, level));
            RoomStyle.Roles r = roles.computeIfAbsent(id, k -> s.roles(lookup));
            Box b = plan.sectionBox(id);
            int ix = PaletteStyles.inner(x, b.minX, b.sizeX()), iz = PaletteStyles.inner(z, b.minZ, b.sizeZ());
            int sx = PaletteStyles.span(b.sizeX()), sz = PaletteStyles.span(b.sizeZ());
            BlockRef ref;
            if (c == Cell.WALL) ref = s.wallRef();
            else if (c == Cell.FLOOR) ref = s.floorChoices()
                .get(r.floorIndex).pattern.at(ix, iz, sx, sz);
            else if (c == Cell.CEILING) ref = s.ceilingChoices()
                .get(r.ceilingIndex).pattern.at(ix, iz, sx, sz);
            else return null;
            return RoomStyle.blockId(ref.resolve(lookup));
        };
    }

    @Test
    void generatedComplexesKeepThreeBlocksPerSection() {
        int complexes = 0, sections = 0;
        for (long seed = 0; seed < 12; seed++) {
            ComplexPlan plan = ComplexLayout.generate(seed * 31 + 5, (int) seed, 4);
            ComplexRaster raster = new ComplexRaster(plan);
            for (String dim : new String[] { "Overworld", "moon", "mars", "ceres" }) {
                for (ActivityLevel level : ActivityLevel.values()) {
                    for (BlockRef.Lookup lookup : new BlockRef.Lookup[] { ALL, VANILLA, NO_CHISEL }) {
                        List<RoleAudit.Violation> v = RoleAudit.audit(raster, reader(plan, raster, dim, level, lookup));
                        assertTrue(
                            v.isEmpty(),
                            "seed " + seed
                                + " "
                                + dim
                                + " "
                                + level
                                + ": "
                                + (v.isEmpty() ? ""
                                    : RoleAudit.describeSection(plan, v.get(0).section) + " "
                                        + v.get(0).block
                                        + " "
                                        + v.get(0)
                                            .roles()));
                        complexes++;
                    }
                }
            }
            sections += plan.sectionCount();
        }
        assertTrue(complexes >= 12 * 4 * 3 && sections > 12 * 10, complexes + " complexes, " + sections + " sections");
    }

    @Test
    void oneBlockInEveryRoleIsReportedOncePerSection() {
        ComplexPlan plan = ComplexLayout.generate(77L, 1, 2);
        ComplexRaster raster = new ComplexRaster(plan);
        List<RoleAudit.Violation> v = RoleAudit.audit(raster, (x, y, z) -> {
            byte c = raster.at(x, y, z);
            return c == Cell.WALL || c == Cell.FLOOR || c == Cell.CEILING ? "minecraft:stonebrick" : null;
        });
        assertEquals(plan.sectionCount(), v.size());
        for (RoleAudit.Violation x : v) assertEquals("FLOOR and WALL and CEILING", x.roles());
        String line = RoleAudit.line(
            plan,
            v.get(0),
            "test",
            "probe_set",
            "OPERATIONAL",
            "minecraft:stonebrick",
            (s, role, x, y, z) -> "path " + role);
        assertTrue(line.contains("style set probe_set") && line.contains("level OPERATIONAL"), line);
        assertTrue(line.contains("FLOOR at") && line.contains("WALL at") && line.contains("CEILING at"), line);
        assertTrue(line.contains("written by path " + Cell.FLOOR), line);
    }

    @Test
    void cellsLeftOutDoNotCount() {
        // A floor that shares the wall's block only where another pass wrote (the reader leaves those cells out).
        ComplexPlan plan = ComplexLayout.generate(78L, 2, 2);
        ComplexRaster raster = new ComplexRaster(plan);
        List<RoleAudit.Violation> v = RoleAudit.audit(raster, (x, y, z) -> {
            byte c = raster.at(x, y, z);
            if (c == Cell.WALL) return "wall";
            if (c == Cell.FLOOR) return (x + z) % 7 == 0 ? null : "floor";
            return c == Cell.CEILING ? "ceiling" : null;
        });
        assertTrue(v.isEmpty());
    }

    @Test
    void aWallOfAnotherSectionFacingInCountsAsThisSectionsWall() {
        // Over every plan the audit reads, count the walls a section shows that another section owns: whatever their
        // number, a shared block with them would be reported (the reader below makes every foreign wall the floor's).
        int foreign = 0;
        List<String> found = new ArrayList<>();
        for (long seed = 0; seed < 6; seed++) {
            ComplexPlan plan = ComplexLayout.generate(seed, 3, (int) seed);
            ComplexRaster raster = new ComplexRaster(plan);
            List<RoleAudit.Violation> v = RoleAudit.audit(raster, (x, y, z) -> {
                byte c = raster.at(x, y, z);
                if (c == Cell.FLOOR) return "floor" + raster.ownerAt(x, z);
                if (c == Cell.WALL) return "floor" + (raster.ownerAt(x, z) + 1);
                return c == Cell.CEILING ? "ceiling" + raster.ownerAt(x, z) : null;
            });
            for (RoleAudit.Violation x : v) {
                if (x.cells[1] != null && x.cells[1][3] != x.section) foreign++;
                found.add(x.roles());
            }
        }
        // Each reported pair is FLOOR and WALL, and only through a neighbor's wall.
        for (String r : found) assertEquals("FLOOR and WALL", r);
        assertEquals(found.size(), foreign);
    }
}
