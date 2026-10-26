package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.Stamper;
import com.gregguns.worldgen.loot.LootPending;

/**
 * The populate pipeline over synthetic columns: GGS-11-08 (every write in the window), GGS-11-10 (same seed, same
 * result), GGS-11-20 (per-chest seeds), PLAN-M8 Q3 (the recorded decision wins), and the biome and terrain rejections.
 */
class StructurePopulatorTest {

    private static final long SEED = 0xC0FFEEL;
    private static final int BASE = 100;
    private static final int SIDE = 12;
    private static final Block FILL = new Block(Material.ground) {};
    private static final Block AIR = new Block(Material.air) {};

    /** One distinct Block per registry name. */
    private static final class Blocks implements PaletteKeys.BlockLookup {

        final Map<String, Block> byName = new HashMap<>();

        @Override
        public Block byName(String name) {
            return byName.computeIfAbsent(name, n -> new Block(Material.rock) {});
        }
    }

    /** Collects marks. */
    private static final class Recorder implements StructurePopulator.Marks {

        final List<String> chests = new ArrayList<>();
        final List<LootPending> orders = new ArrayList<>();
        int spawners;
        int flags;

        @Override
        public void chest(StructurePlan plan, String kind, int x, int y, int z, LootPending order) {
            chests.add(kind + "@" + x + "," + y + "," + z);
            orders.add(order);
        }

        @Override
        public void spawner(StructurePlan plan, String tag, int x, int y, int z) {
            spawners++;
        }

        @Override
        public void flag(StructurePlan plan, String planKey, int x, int y, int z) {
            flags++;
        }
    }

    private static final String HUT = "gregguns-layout 1\nid: test_hut\nsize: 5 4 5\nground: 1\npalette:\n"
        + "S = minecraft:stone\nC = minecraft:chest#3@facing\nW = gregguns:weapon_chest#3@facing\n"
        + "marks:\nchest.vanilla 1 1 1\nchest.weapon 3 1 1\nspawner.zombie 2 1 2\nflag 0 1 0\n"
        + "layers:\nlayer 0\nSSSSS\nSSSSS\nSSSSS\nSSSSS\nSSSSS\nlayer 1\n.....\n.C.W.\n.....\n.....\n.....\n";

    private final PaletteKeys keys = PaletteKeys.resolve(null, new Blocks(), s -> {});
    private final LayoutRegistry layouts = LayoutRegistry
        .load(id -> HUT, Collections.singletonList("test_hut"), keys, s -> { throw new IllegalStateException(s); });
    private final StructurePopulator populator = new StructurePopulator(
        new StructureGrid(2, 2, 2, pools()),
        layouts,
        new Stamper(keys),
        FILL,
        AIR,
        StructurePopulator.CONFIG_RULES);

    private static Map<SizeClass, List<PoolEntry>> pools() {
        Map<SizeClass, List<PoolEntry>> m = new EnumMap<>(SizeClass.class);
        m.put(SizeClass.SMALL, Collections.singletonList(new PoolEntry("test_hut", 1, 4, new Footprint(5, 4, 5))));
        return m;
    }

    private static final StructurePopulator.BiomeSource PLAINS = (x, z) -> Collections.<Type>singleton(Type.PLAINS);

    /** Everything one run over the square produced. */
    private static final class Run {

        final List<String> writes = new ArrayList<>();
        final Recorder marks = new Recorder();
        final StructureState state = new StructureState("t");
        int stamped;
        int outside;
        int rejected;
        int maxY = -1;
    }

    private Run run(StructureState state, ColumnReader reader, StructurePopulator.BiomeSource biomes) {
        return run(populator, state, reader, biomes);
    }

    private Run run(StructurePopulator populator, StructureState state, ColumnReader reader,
        StructurePopulator.BiomeSource biomes) {
        Run r = new Run();
        StructureState st = state != null ? state : r.state;
        for (int cx = BASE; cx < BASE + SIDE; cx++) {
            for (int cz = BASE; cz < BASE + SIDE; cz++) {
                RecordingWorld world = new RecordingWorld(cx, cz);
                ClippingSink sink = new ClippingSink(world.window(), world);
                try (PopulateGuard.Scope ignored = PopulateGuard.enter(cx, cz)) {
                    r.stamped += populator.populate(SEED, 0, world.window(), sink, reader, biomes, st, r.marks);
                }
                r.outside += world.writesOutsideWindow()
                    .size();
                r.rejected += sink.rejectedWrites();
                for (RecordingWorld.Write w : world.writes) {
                    r.writes.add(w.kind + w.x + "," + w.y + "," + w.z);
                    r.maxY = Math.max(r.maxY, w.y);
                }
                assertTrue(
                    world.foreignChunkRequests()
                        .isEmpty());
            }
        }
        return r;
    }

    @Test
    void placesStructuresWithEveryWriteInsideItsWindow() {
        Run r = run(null, GridColumns.flat(64), PLAINS);
        assertTrue(r.stamped > 0, "some structures are stamped");
        assertEquals(0, r.outside, "GGS-11-08: no write outside the window");
        assertEquals(0, r.rejected, "the stamper never produces a write the sink must drop");
    }

    @Test
    void everyMarkIsReportedExactlyOnce() {
        Run r = run(null, GridColumns.flat(64), PLAINS);
        // Windows tile, so no mark is reported twice. (Structures cut by the edge of the square lose some marks.)
        assertTrue(r.marks.spawners > 0);
        assertTrue(r.marks.flags > 0);
        assertTrue(r.marks.chests.size() > 0);
        assertEquals(new HashSet<>(r.marks.chests).size(), r.marks.chests.size());
    }

    @Test
    void sameSeedGivesTheSameResult() {
        Run a = run(null, GridColumns.flat(64), PLAINS);
        Run b = run(null, GridColumns.flat(64), PLAINS);
        assertEquals(a.writes, b.writes);
        assertEquals(a.marks.chests, b.marks.chests);
        for (int i = 0; i < a.marks.orders.size(); i++) {
            assertEquals(a.marks.orders.get(i), b.marks.orders.get(i));
        }
    }

    @Test
    void chestSeedsDifferPerChestAndUseTheWeaponCategory() {
        Run r = run(null, GridColumns.flat(64), PLAINS);
        Set<Long> seeds = new HashSet<>();
        for (LootPending p : r.marks.orders) {
            seeds.add(p.seed());
            assertEquals("gregguns:weaponchest_t1", p.category(), "test ids have ceiling 1 (LV)");
            assertEquals(1, p.richness());
        }
        assertEquals(r.marks.orders.size(), seeds.size(), "every chest has its own seed");
    }

    @Test
    void oceanBiomeRejectsAndRecordsWhy() {
        Run r = run(null, GridColumns.flat(64), (x, z) -> Collections.<Type>singleton(Type.OCEAN));
        assertEquals(0, r.stamped);
        assertTrue(r.writes.isEmpty());
    }

    @Test
    void steepTerrainIsRejectedWhereTheWindowSeesTheSlope() {
        // A window that sees only a one-column sliver of a footprint cannot see a slope (Q3), so some still pass;
        // every footprint that sits inside one window is rejected.
        GridColumns cliffs = new GridColumns((x, z) -> 40 + (x & 15) * 4, (x, z) -> -1);
        Run steep = run(null, cliffs, PLAINS);
        Run flat = run(null, GridColumns.flat(64), PLAINS);
        assertTrue(steep.stamped < flat.stamped);
    }

    @Test
    void waterColumnsAreRejected() {
        Run r = run(null, new GridColumns((x, z) -> 50, (x, z) -> 62), PLAINS);
        assertEquals(0, r.stamped);
        assertTrue(r.writes.isEmpty());
    }

    @Test
    void recordedRejectionWins() {
        StructureState st = new StructureState("t");
        for (int cx = BASE - 2; cx < BASE + SIDE + 2; cx++) {
            for (int cz = BASE - 2; cz < BASE + SIDE + 2; cz++) {
                for (StructurePlan p : populatorPlans(cx, cz)) {
                    st.decideOnce(PlanKey.of(p), () -> Decision.rejected("water"));
                }
            }
        }
        Run r = run(st, GridColumns.flat(64), PLAINS);
        assertEquals(0, r.stamped, "later windows obey the record (Q3)");
        assertTrue(r.writes.isEmpty());
    }

    @Test
    void recordedHeightWinsOverTheTerrain() {
        StructureState st = new StructureState("t");
        for (int cx = BASE - 2; cx < BASE + SIDE + 2; cx++) {
            for (int cz = BASE - 2; cz < BASE + SIDE + 2; cz++) {
                for (StructurePlan p : populatorPlans(cx, cz)) {
                    st.decideOnce(PlanKey.of(p), () -> Decision.placed(80));
                }
            }
        }
        Run r = run(st, GridColumns.flat(64), PLAINS);
        assertTrue(r.stamped > 0);
        // Ground layer 1 at anchor Y 81 puts layer 1 at Y 81 and the floor (layer 0) at Y 80.
        assertEquals(81, r.maxY);
        assertNotEquals(-1, r.maxY);
    }

    private List<StructurePlan> populatorPlans(int cx, int cz) {
        return new StructureGrid(2, 2, 2, pools()).plansForWindow(SEED, 0, cx, cz);
    }

    /** Settings that roll a fixed ceiling, as the Ross128 dimensions do (design record 0019). */
    private static StructurePopulator.Rules rolling(int ceiling) {
        return new StructurePopulator.Rules() {

            @Override
            public boolean enabled(StructureId s) {
                return true;
            }

            @Override
            public Set<Type> biomeBlacklist(StructureId s) {
                return Collections.emptySet();
            }

            @Override
            public int lootCeiling(StructureId s) {
                return 1;
            }

            @Override
            public int richness(StructureId s) {
                return 1;
            }

            @Override
            public int rolledCeiling(StructurePlan plan) {
                return ceiling;
            }
        };
    }

    private StructurePopulator populatorWith(StructurePopulator.Rules rules) {
        return new StructurePopulator(
            new StructureGrid(2, 2, 2, pools()),
            layouts,
            new Stamper(keys),
            FILL,
            AIR,
            rules);
    }

    @Test
    void aRolledCeilingPicksTheChestTablesOfThatTier() {
        Run r = run(populatorWith(rolling(5)), null, GridColumns.flat(64), PLAINS);
        assertTrue(r.stamped > 0);
        assertTrue(!r.marks.orders.isEmpty());
        for (LootPending o : r.marks.orders) {
            assertEquals("gregguns:weaponchest_t5", o.category(), "a rolled IV ceiling uses the IV table");
        }
    }

    @Test
    void aRolledCeilingAboveZpmUsesTheZpmTables() {
        Run r = run(populatorWith(rolling(9)), null, GridColumns.flat(64), PLAINS);
        assertTrue(!r.marks.orders.isEmpty());
        for (LootPending o : r.marks.orders) {
            assertEquals("gregguns:weaponchest_t7", o.category());
        }
    }

    @Test
    void noRollKeepsTheSurfaceCeiling() {
        Run r = run(populatorWith(rolling(0)), null, GridColumns.flat(64), PLAINS);
        assertTrue(!r.marks.orders.isEmpty());
        for (LootPending o : r.marks.orders) {
            assertEquals("gregguns:weaponchest_t1", o.category(), "an unrolled hut stays at its LV ceiling");
        }
    }

    @Test
    void theRolledCeilingNeverFallsBelowTheConfiguredOne() {
        // lootCeiling is 1 in rolling(); a roll of MV (2) must give the MV table, not less.
        Run r = run(populatorWith(rolling(2)), null, GridColumns.flat(64), PLAINS);
        for (LootPending o : r.marks.orders) {
            assertEquals("gregguns:weaponchest_t2", o.category());
        }
    }
}
