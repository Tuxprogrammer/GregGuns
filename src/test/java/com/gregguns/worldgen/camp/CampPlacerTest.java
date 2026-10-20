package com.gregguns.worldgen.camp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.RecordingWorld;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.layout.PaletteKeys;

/** Stamps placeholder camps through every window they touch: in-window only, no overlap, rotation-invariant. */
class CampPlacerTest {

    private final Map<String, Block> blocks = new HashMap<>();
    private final PaletteKeys keys = PaletteKeys
        .resolve(null, name -> blocks.computeIfAbsent(name, n -> new Block(Material.rock) {}), s -> {});

    /** What one full pass over a camp produced. */
    private static final class Pass {

        final Set<String> columns = new HashSet<>();
        int writes;
        int duplicates;
        int rejected;
        int chunkRequests;
        final List<String> marks = new ArrayList<>();
    }

    private Pass run(PlacedCamp camp, PartLayouts layouts, CampColor color) {
        CampPlacer placer = new CampPlacer(keys, layouts);
        Pass pass = new Pass();
        int c0x = Math.floorDiv(camp.boundsMinX() - 8, 16);
        int c1x = Math.floorDiv(camp.boundsMaxX() - 8, 16);
        int c0z = Math.floorDiv(camp.boundsMinZ() - 8, 16);
        int c1z = Math.floorDiv(camp.boundsMaxZ() - 8, 16);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                ClippingSink sink = new ClippingSink(world.window(), world);
                placer.write(sink, camp, world.window(), color, (tag, x, y, z) -> {
                    assertTrue(world.window().contains(x, z), "mark outside its window");
                    pass.marks.add(tag + "@" + x + "," + y + "," + z);
                });
                for (RecordingWorld.Write w : world.writes) {
                    assertTrue(world.window().contains(w.x, w.z), "write outside the window");
                    if (!pass.columns.add(w.x + "," + w.y + "," + w.z)) {
                        pass.duplicates++;
                    }
                }
                pass.writes += world.writes.size();
                pass.rejected += sink.rejectedWrites();
                pass.chunkRequests += world.chunkRequests.size();
            }
        }
        return pass;
    }

    @Test
    void everyWriteIsInsideItsWindowAndNothingIsWrittenTwice() {
        for (long seed = 0; seed < 12; seed++) {
            for (int rot = 0; rot < 4; rot++) {
                PlacedCamp camp = CampPlan.generate(seed)
                    .place(-37, 70, 91, rot);
                Pass p = run(camp, CampPlaceholders.all(), CampColor.WOODLAND);
                assertEquals(0, p.rejected, "the stamper never asks for a clipped write: seed " + seed);
                assertEquals(0, p.duplicates, "windows tile the plane: seed " + seed + " rot " + rot);
                assertEquals(0, p.chunkRequests);
                assertTrue(p.writes > 1000, "a camp is a lot of blocks");
            }
        }
    }

    @Test
    void rotationOnlyMovesTheCampNeverChangesWhatIsBuilt() {
        for (long seed = 0; seed < 8; seed++) {
            CampPlan plan = CampPlan.generate(seed);
            int base = run(plan.place(0, 70, 0, 0), CampPlaceholders.all(), CampColor.DESERT).writes;
            List<String> marks0 = sortedMarkTags(plan.place(0, 70, 0, 0));
            for (int rot = 1; rot < 4; rot++) {
                PlacedCamp camp = plan.place(0, 70, 0, rot);
                assertEquals(base, run(camp, CampPlaceholders.all(), CampColor.DESERT).writes, "rot " + rot);
                assertEquals(marks0, sortedMarkTags(camp), "chest marks survive rotation");
            }
        }
    }

    private List<String> sortedMarkTags(PlacedCamp camp) {
        List<String> tags = new ArrayList<>();
        for (String m : run(camp, CampPlaceholders.all(), CampColor.WOODLAND).marks) {
            tags.add(m.substring(0, m.indexOf('@')));
        }
        java.util.Collections.sort(tags);
        return tags;
    }

    @Test
    void theFlagLeafHoldsNoBuildingAndAskForPartsUsesTheCampColor() {
        PlacedCamp camp = CampPlan.generate(4)
            .place(0, 70, 0, 0);
        List<String> asked = new ArrayList<>();
        run(camp, CampPlaceholders.recording(asked), CampColor.SNOW);
        assertTrue(!asked.isEmpty());
        for (String s : asked) {
            assertTrue(s.endsWith(":SNOW"), s);
        }
        int parts = 0;
        for (CampLeaf l : camp.plan()
            .leaves()) {
            parts += l.part != null ? 1 : 0;
        }
        // Each non-flag leaf with a part asks once per window it touches, so at least once per part leaf.
        assertTrue(asked.size() >= parts);
    }

    @Test
    void withNoLayoutsTheLeavesFallBackToFlatCobblestoneAndTheCampStillBuilds() {
        PlacedCamp camp = CampPlan.generate(6)
            .place(0, 70, 0, 3);
        Pass p = run(camp, (part, color) -> new ArrayList<>(), CampColor.WOODLAND);
        assertEquals(0, p.duplicates);
        assertTrue(p.writes > 500, "roads, fence and fallback layers still go down");
        assertTrue(p.marks.isEmpty());
    }

    @Test
    void windowsOutsideTheCampWriteNothing() {
        PlacedCamp camp = CampPlan.generate(6)
            .place(0, 70, 0, 0);
        RecordingWorld world = new RecordingWorld(40, 40);
        new CampPlacer(keys, CampPlaceholders.all())
            .write(new ClippingSink(world.window(), world), camp, new PopulateWindow(40, 40), CampColor.WOODLAND, (
                t, x, y, z) -> {});
        assertTrue(world.writes.isEmpty());
    }

    @Test
    void registryIdsListEveryTierAndColor() {
        List<String> ids = PartLayouts.FromRegistry.allIds();
        assertTrue(ids.contains("camp_barracks_1"));
        assertTrue(ids.contains("camp_tent_2_snow"));
        assertTrue(ids.contains("camp_tower_3_desert"));
        assertEquals(CampPart.values().length * CampPart.TIERS * (1 + CampColor.values().length), ids.size());
    }
}
