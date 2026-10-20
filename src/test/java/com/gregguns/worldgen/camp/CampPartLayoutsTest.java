package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Rotations;
import com.gregguns.worldgen.loot.LootCategoryDefs;

/** The shipped Military Base part layouts (camp_PART_TIER[_color].txt): they load, fit and are sane. */
class CampPartLayoutsTest {

    private static final Map<String, Block> BLOCKS = new HashMap<>();
    private static final PaletteKeys KEYS = PaletteKeys
        .resolve(null, name -> BLOCKS.computeIfAbsent(name, n -> new Block(Material.rock) {}), s -> {});
    private static final List<String> ERRORS = new ArrayList<>();
    private static final Set<String> SHIPPED = new HashSet<>();
    private static LayoutRegistry registry;

    @BeforeAll
    static void load() {
        for (String id : PartLayouts.FromRegistry.allIds()) {
            if (CampPartLayoutsTest.class.getResource(LayoutRegistry.ClasspathSource.STRUCTURES + id + ".txt")
                != null) {
                SHIPPED.add(id);
            }
        }
        registry = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            SHIPPED,
            KEYS,
            ERRORS::add);
    }

    private static Layout layout(CampPart p, int tier) {
        Layout l = registry.get(p.layoutId(tier));
        assertNotNull(l, p.layoutId(tier));
        return l;
    }

    /** Towers ship two sizes (3 and 4 square); the placer can never pick a third. */
    private static int tiers(CampPart p) {
        return p == CampPart.TOWER ? 2 : CampPart.TIERS;
    }

    @Test
    void everyShippedIdLoadsWithNoErrors() {
        assertTrue(ERRORS.isEmpty(), ERRORS.toString());
        for (String id : SHIPPED) {
            assertNotNull(registry.get(id), id);
        }
    }

    @Test
    void everyPartShipsEveryTierItCanUse() {
        for (CampPart p : CampPart.values()) {
            for (int t = 1; t <= tiers(p); t++) {
                assertTrue(SHIPPED.contains(p.layoutId(t)), p.layoutId(t));
            }
        }
    }

    @Test
    void colorVariantsExistOnlyWhereCamoNetsAreAndKeepTheSize() {
        for (String id : SHIPPED) {
            for (CampColor c : CampColor.values()) {
                String suffix = "_" + PartLayouts.suffix(c);
                if (!id.endsWith(suffix)) {
                    continue;
                }
                String plain = id.substring(0, id.length() - suffix.length());
                assertTrue(plain.startsWith("camp_tent_") || plain.equals("camp_props_3"), id);
                Layout a = registry.get(id);
                Layout b = registry.get(plain);
                assertEquals(b.sizeX(), a.sizeX(), id);
                assertEquals(b.sizeY(), a.sizeY(), id);
                assertEquals(b.sizeZ(), a.sizeZ(), id);
            }
        }
    }

    @Test
    void tierOneFitsTheSmallestLeafItsPartIsPickedFor() {
        for (CampPart p : CampPart.values()) {
            Layout l = layout(p, 1);
            boolean fits = l.sizeX() <= p.minW() && l.sizeZ() <= p.minD()
                || l.sizeX() <= p.minD() && l.sizeZ() <= p.minW();
            assertTrue(fits, p + " tier 1 is " + l.sizeX() + "x" + l.sizeZ());
        }
    }

    @Test
    void biggerTiersAreBiggerAndStayInsideTheNotesRanges() {
        for (CampPart p : CampPart.values()) {
            long prev = 0;
            for (int t = 1; t <= tiers(p); t++) {
                Layout l = layout(p, t);
                long area = (long) l.sizeX() * l.sizeZ();
                assertTrue(area > prev, l.id() + " area " + area);
                prev = area;
                assertTrue(l.sizeX() <= 16 && l.sizeZ() <= 16, l.id());
            }
        }
        assertEquals(4, layout(CampPart.TOWER, 2).sizeX());
        assertEquals(7, layout(CampPart.BARRACKS, 1).sizeZ(), "5 x 5 building plus a two-block ramp");
        assertEquals(7, layout(CampPart.HELIPAD, 1).sizeX());
    }

    @Test
    void fixedHeightsMatchTheNotes() {
        for (int t = 1; t <= 3; t++) {
            assertEquals(7, layout(CampPart.BARRACKS, t).sizeY());
            assertEquals(4, layout(CampPart.CONTAINERS, t).sizeY(), "layer 0 plus 3 high");
            assertTrue(layout(CampPart.TANK_FARM, t).sizeY() >= 6, "at least 5 above the ground layer");
        }
        assertEquals(8, layout(CampPart.TOWER, 1).sizeY());
        int h = layout(CampPart.TOWER, 2).sizeY();
        assertTrue(h >= 10 && h <= 12);
    }

    @Test
    void fourRotationsKeepTheFootprintAndKeepEveryMarkInside() {
        for (String id : SHIPPED) {
            Layout l = registry.get(id);
            for (int q = 0; q < 4; q++) {
                int w = l.footprintX(q);
                int d = l.footprintZ(q);
                assertEquals(q % 2 == 0 ? l.sizeX() : l.sizeZ(), w, id);
                assertEquals(q % 2 == 0 ? l.sizeZ() : l.sizeX(), d, id);
                for (Layout.Mark m : l.marks()) {
                    int ox = Rotations.offsetX(q, m.x, m.z, l.sizeX(), l.sizeZ());
                    int oz = Rotations.offsetZ(q, m.x, m.z, l.sizeX(), l.sizeZ());
                    assertTrue(ox >= 0 && ox < w && oz >= 0 && oz < d, id + " mark " + m.tag + " q" + q);
                }
            }
        }
    }

    @Test
    void chestMarksUseValidLootCategories() {
        Set<String> seen = new HashSet<>();
        for (String id : SHIPPED) {
            for (Layout.Mark m : registry.get(id)
                .marks()) {
                if (!m.tag.startsWith("chest.")) {
                    continue;
                }
                seen.add(m.tag);
                String category;
                switch (m.tag) {
                    case "chest.reinforced":
                        category = LootCategoryDefs.REINFORCEDCHEST_T2;
                        break;
                    case "chest.weapon":
                        category = LootCategoryDefs.WEAPONCHEST_T2;
                        break;
                    case "chest.barracks":
                        category = LootCategoryDefs.BARRACKS;
                        break;
                    case "chest.vanilla":
                        category = null; // the factory's standalone mark; the LV ceiling is the host's call
                        break;
                    default:
                        throw new AssertionError(id + ": unknown chest mark " + m.tag);
                }
                if (category != null) {
                    assertNotNull(LootCategoryDefs.get(category), category);
                }
            }
        }
        assertTrue(
            seen.contains("chest.reinforced") && seen.contains("chest.weapon") && seen.contains("chest.barracks"));
    }

    private static int count(Layout l, String tag) {
        int n = 0;
        for (Layout.Mark m : l.marks()) {
            n += m.tag.equals(tag) ? 1 : 0;
        }
        return n;
    }

    @Test
    void lootAndSoldiersAreWhereTheNotesPutThem() {
        CampPart[] noLoot = { CampPart.SMALL_BUNKER, CampPart.CONTAINERS, CampPart.TANK_FARM, CampPart.TENT,
            CampPart.HELIPAD, CampPart.TOWER };
        for (int t = 1; t <= 3; t++) {
            assertTrue(count(layout(CampPart.CLOSED_BUNKER, t), "chest.weapon") >= 1);
            for (CampPart none : noLoot) {
                if (t <= tiers(none)) {
                    for (Layout.Mark m : layout(none, t).marks()) {
                        assertTrue(!m.tag.startsWith("chest."), none + " " + t);
                    }
                }
            }
            Layout b = layout(CampPart.BARRACKS, t);
            assertTrue(count(b, "chest.barracks") >= 3 && count(b, "spawn.npc") >= 1, b.id());
            assertTrue(count(layout(CampPart.FACTORY, t), "chest.vanilla") >= 1);
            assertTrue(count(layout(CampPart.HELIPAD, t), "spawn.npc") >= 1);
        }
        assertTrue(count(layout(CampPart.CLOSED_BUNKER, 3), "chest.reinforced") >= 2);
        assertTrue(count(layout(CampPart.TOWER, 1), "spawn.npc") >= 1);
        assertTrue(count(layout(CampPart.TOWER, 2), "spawn.npc") >= 1);
        assertTrue(count(layout(CampPart.PROPS, 2), "spawn.npc") >= 1);
    }

    /** Mirrors the placer's rule: some orientation holds a layout, and a tower layout is no wider than its hint. */
    private static boolean fits(CampLeaf leaf, List<Layout> choices) {
        for (Layout l : choices) {
            for (int q = 0; q < 2; q++) {
                int fw = l.footprintX(q);
                int fd = l.footprintZ(q);
                boolean ok = fw <= leaf.w && fd <= leaf.d;
                if (leaf.part == CampPart.TOWER) {
                    ok &= fw <= leaf.sizeHint && fd <= leaf.sizeHint;
                }
                if (ok) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void campsAtThreeSeedsFillEveryPartLeafWithARealLayout() {
        PartLayouts real = new PartLayouts.FromRegistry(registry);
        int partLeaves = 0;
        int fallback = 0;
        int noPart = 0;
        for (long seed : new long[] { 6, 11, 29 }) {
            CampPlan plan = CampPlan.generate(seed);
            for (CampLeaf leaf : plan.leaves()) {
                if (leaf.flagLeaf) {
                    continue;
                }
                if (leaf.part == null) {
                    noPart++;
                    continue;
                }
                partLeaves++;
                for (CampColor c : CampColor.values()) {
                    if (!fits(leaf, real.choices(leaf.part, c))) {
                        fallback++;
                    }
                }
            }
            // And an actual placement: every window clipped, nothing written twice.
            PlacedCamp camp = plan.place(0, 70, 0, (int) (seed & 3));
            Set<String> written = new HashSet<>();
            CampPlacer placer = new CampPlacer(KEYS, real);
            int c0x = Math.floorDiv(camp.boundsMinX() - 8, 16);
            int c1x = Math.floorDiv(camp.boundsMaxX() - 8, 16);
            int c0z = Math.floorDiv(camp.boundsMinZ() - 8, 16);
            int c1z = Math.floorDiv(camp.boundsMaxZ() - 8, 16);
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) {
                    RecordingWorld world = new RecordingWorld(cx, cz);
                    ClippingSink sink = new ClippingSink(world.window(), world);
                    placer.write(sink, camp, world.window(), CampColor.WOODLAND, (t, x, y, z) -> {});
                    for (RecordingWorld.Write w : world.writes) {
                        assertTrue(written.add(w.x + "," + w.y + "," + w.z), "double write");
                    }
                    assertEquals(0, sink.rejectedWrites());
                }
            }
        }
        System.out.println(
            "camp fallback check: partLeaves=" + partLeaves
                + " cobbleFallbacks="
                + fallback
                + " leavesWithNoPartThatFits="
                + noPart);
        assertEquals(0, fallback, "a leaf given a part must fit one of its layouts");
    }
}
