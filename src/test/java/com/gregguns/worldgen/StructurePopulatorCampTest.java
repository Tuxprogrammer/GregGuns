package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.camp.CampPlaceholders;
import com.gregguns.worldgen.camp.CampPlacer;
import com.gregguns.worldgen.camp.CampPlan;
import com.gregguns.worldgen.camp.CarrierFlag;
import com.gregguns.worldgen.camp.FlagSpec;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * The Military Base through the populator (G9 wiring of G10): sized from its own plan, every overlapping window
 * written (road tails included), exactly one flag recorded, and the carrier flag spec computed from the layout.
 */
class StructurePopulatorCampTest {

    private static final long SEED = 0x5EEDL;
    private static final Block FLAG = new Block(Material.rock) {};
    private static final Block FILL = new Block(Material.ground) {};
    private static final Block AIR = new Block(Material.air) {};

    private static final class Blocks implements PaletteKeys.BlockLookup {

        final Map<String, Block> byName = new HashMap<>();

        @Override
        public Block byName(String name) {
            return byName.computeIfAbsent(name, n -> new Block(Material.rock) {});
        }
    }

    /** Captures every write of a run; counts spawns. */
    private static final class Capture implements WindowedSink.Target {

        final List<String> blocks = new ArrayList<>();
        final List<TileEntity> tiles = new ArrayList<>();
        int spawns;

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            blocks.add(x + "," + y + "," + z + "=" + System.identityHashCode(block) + ":" + meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            tiles.add(tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            spawns++;
        }
    }

    /** Feeds both a per-window RecordingWorld (window checks) and the run's capture. */
    private static final class Tee implements WindowedSink.Target {

        private final WindowedSink.Target a;
        private final WindowedSink.Target b;

        Tee(WindowedSink.Target a, WindowedSink.Target b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            a.setBlock(x, y, z, block, meta);
            b.setBlock(x, y, z, block, meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            a.setTile(x, y, z, tile);
            b.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            a.spawn(entity, x, y, z);
            b.spawn(entity, x, y, z);
        }
    }

    private static final class NoMarks implements StructurePopulator.Marks {

        @Override
        public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {}

        @Override
        public void spawner(StructurePlan plan, String tag, int x, int y, int z) {}

        @Override
        public void flag(StructurePlan plan, String planKey, int x, int y, int z) {}
    }

    private final PaletteKeys keys = PaletteKeys.resolve(null, new Blocks(), s -> {});
    private final LayoutRegistry empty = LayoutRegistry.load(id -> {
        throw new IOException("none");
    }, Collections.<String>emptyList(), keys, s -> {});

    private StructureGrid grid() {
        return new StructureGrid(16, 16, 8, StructureCatalog.pools(empty, s -> s == StructureId.MILITARY_BASE, true));
    }

    private StructurePopulator populator() {
        return new StructurePopulator(
            grid(),
            empty,
            new Stamper(keys),
            FILL,
            AIR,
            StructurePopulator.CONFIG_RULES,
            new CampPlacer(keys, CampPlaceholders.all()),
            FLAG,
            0.75F);
    }

    private StructurePlan findPlan(long worldSeed) {
        StructureGrid g = grid();
        for (int cx = 20; cx < 40; cx++) {
            Optional<StructurePlan> p = g.planAt(worldSeed, 0, SizeClass.BIG, cx, 20);
            if (p.isPresent()) return p.get();
        }
        throw new AssertionError("no plan in the scanned cells");
    }

    private static final class Pass {

        final StructureState state = new StructureState("t");
        final Capture capture = new Capture();
        final List<String> perWindow = new ArrayList<>();
        int outside;
        int stamped;
        int flagTiles;
    }

    private Pass run(StructurePlan plan) {
        Pass pass = new Pass();
        StructurePopulator populator = populator();
        int c0x = Math.floorDiv(plan.anchorX() - 8 - 4, 16);
        int c1x = Math.floorDiv(plan.maxX() - 8 + 4, 16) + 1;
        int c0z = Math.floorDiv(plan.anchorZ() - 8 - 4, 16);
        int c1z = Math.floorDiv(plan.maxZ() - 8 + 4, 16) + 1;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                pass.stamped += populator.populate(
                    SEED,
                    0,
                    world.window(),
                    new ClippingSink(world.window(), new Tee(world, pass.capture)),
                    GridColumns.flat(64),
                    (x, z) -> Collections.<Type>singleton(Type.PLAINS),
                    pass.state,
                    new NoMarks());
                pass.outside += world.writesOutsideWindow()
                    .size();
                pass.perWindow.add(cx + "," + cz + ":" + world.writes.size());
                assertTrue(
                    world.foreignChunkRequests()
                        .isEmpty());
            }
        }
        for (TileEntity t : pass.capture.tiles) {
            if (t instanceof TileCampFlag) pass.flagTiles++;
        }
        return pass;
    }

    @Test
    void theBaseFootprintComesFromItsOwnSeed() {
        StructurePlan plan = findPlan(SEED);
        assertEquals(StructureId.MILITARY_BASE.id(), plan.id());
        int[] size = CampPlan.sizeFor(plan.shapeSeed());
        assertEquals(
            size[0],
            plan.footprint()
                .sizeX());
        assertEquals(
            size[1],
            plan.footprint()
                .sizeZ());
        assertEquals(CampPlan.ROAD_TAIL, plan.apron());
    }

    @Test
    void roadTailOnlyWindowsStillFindThePlan() {
        StructureGrid g = grid();
        for (long seed = 1; seed < 400; seed++) {
            StructurePlan plan = findPlan(seed);
            // A window whose last block is 1 to 3 blocks west of the box touches only the road tails.
            int k = Math.floorMod(plan.anchorX() - 23, 16);
            if (k < 1 || k > 3) continue;
            int cx = Math.floorDiv(plan.anchorX() - 23, 16);
            int cz = Math.floorDiv(plan.anchorZ() + 20 - 8, 16);
            boolean found = false;
            for (StructurePlan p : g.plansForWindow(seed, 0, cx, cz)) {
                found |= p.cellX() == plan.cellX() && p.cellZ() == plan.cellZ();
            }
            assertTrue(found, "an apron-only window must still list the plan");
            return;
        }
        throw new AssertionError("no seed with an apron-only window");
    }

    @Test
    void placesOneFlagRecordedAndWritesStayInWindow() {
        StructurePlan plan = findPlan(SEED);
        Pass pass = run(plan);
        assertTrue(pass.stamped > 0, "the camp wrote blocks");
        assertEquals(0, pass.outside, "GGS-11-08: no write outside its window");
        assertEquals(1, pass.flagTiles, "exactly one flag tile");
        assertEquals(0, pass.capture.spawns, "no NPC at generation (Q12)");
        Decision d = pass.state.get(PlanKey.of(plan));
        assertNotNull(d);
        assertTrue(d.isPlaced());
        assertEquals(1, d.flags().size(), "StructureState recorded the flag");
        TileCampFlag tile = null;
        for (TileEntity t : pass.capture.tiles) {
            if (t instanceof TileCampFlag) tile = (TileCampFlag) t;
        }
        assertNotNull(tile);
        assertTrue(tile.isInitialized());
        assertEquals(Arrays.asList("ArmySoldier"), tile.npcNames());
    }

    @Test
    void sameSeedSameResult() {
        StructurePlan plan = findPlan(SEED);
        Pass a = run(plan);
        Pass b = run(plan);
        assertEquals(a.capture.blocks, b.capture.blocks);
        assertEquals(a.perWindow, b.perWindow);
    }

    @Test
    void carrierFlagSpecHasCapNamesAndNineSpawnOffsets() {
        PaletteKeys k = PaletteKeys.resolve(null, new Blocks(), s -> {});
        List<String> errors = new ArrayList<>();
        LayoutRegistry reg = LayoutRegistry.load(
            new LayoutRegistry.ClasspathSource(LayoutRegistry.ClasspathSource.STRUCTURES),
            Collections.singletonList("aircraft_carrier"),
            k,
            errors::add);
        Layout l = reg.get("aircraft_carrier");
        assertNotNull(l, errors.toString());
        for (int rot = 0; rot < 4; rot++) {
            StructurePlan plan = new StructurePlan(
                "aircraft_carrier",
                SizeClass.BIG,
                0,
                3,
                3,
                1000,
                -2000,
                rot,
                0,
                new Footprint(l.sizeX(), l.sizeY(), l.sizeZ()),
                7L);
            FlagSpec spec = CarrierFlag.specFor(reg, plan, 62, 1010, 70, -1990);
            assertNotNull(spec);
            assertEquals(9, spec.cap);
            assertEquals(Arrays.asList("ArmySoldier", "Commando"), spec.names());
            int[] sp = spec.spawnPoints();
            assertEquals(27, sp.length, "nine spawn.npc marks");
            assertEquals(plan.sizeX(), spec.sizeX);
            assertEquals(plan.sizeZ(), spec.sizeZ);
            assertEquals(10, spec.offsetX);
            assertEquals(10, spec.offsetZ);
            for (int i = 0; i < 27; i += 3) {
                assertTrue(sp[i] >= 0 && sp[i] < plan.sizeX(), "offset x inside the box at rotation " + rot);
                assertTrue(sp[i + 2] >= 0 && sp[i + 2] < plan.sizeZ(), "offset z inside the box at rotation " + rot);
            }
        }
    }
}
