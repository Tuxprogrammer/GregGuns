package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.layout.LayoutRegistry;
import com.gregguns.worldgen.layout.PaletteKeys;

/** Pool building and the size-class table (GGS-11-15, PLAN-M8 section 2). */
class StructureCatalogTest {

    private static final String TEXT = "gregguns-layout 1\nid: %s\nsize: 3 2 4\nground: 0\npalette:\nS = minecraft:stone\n"
        + "layers:\nlayer 0\nSSS\nSSS\nSSS\nSSS\n";

    private final PaletteKeys keys = PaletteKeys.resolve(null, n -> new Block(Material.rock) {}, s -> {});

    private LayoutRegistry registry(String... ids) {
        List<String> have = Arrays.asList(ids);
        return LayoutRegistry.load(id -> {
            if (!have.contains(id)) throw new IOException("none");
            return String.format(TEXT, id);
        }, Arrays.asList("factory_small", "big_bunker", "submarine", "gas_station"), keys, s -> {});
    }

    @Test
    void noLayoutsMeansEmptyPools() {
        Map<SizeClass, List<PoolEntry>> pools = StructureCatalog.pools(registry(), s -> true);
        for (SizeClass c : SizeClass.values()) {
            assertTrue(
                pools.get(c)
                    .isEmpty(),
                c.name());
        }
    }

    @Test
    void loadedAndEnabledStructuresLandInTheirClass() {
        Map<SizeClass, List<PoolEntry>> pools = StructureCatalog
            .pools(registry("factory_small", "big_bunker", "submarine"), s -> true);
        assertEquals(
            1,
            pools.get(SizeClass.SMALL)
                .size());
        assertEquals(
            "factory_small",
            pools.get(SizeClass.SMALL)
                .get(0)
                .id());
        assertEquals(
            "big_bunker",
            pools.get(SizeClass.MEDIUM)
                .get(0)
                .id());
        assertEquals(
            "submarine",
            pools.get(SizeClass.BIG)
                .get(0)
                .id());
        Footprint f = pools.get(SizeClass.SMALL)
            .get(0)
            .footprint();
        assertEquals(3, f.sizeX());
        assertEquals(2, f.sizeY());
        assertEquals(4, f.sizeZ());
    }

    @Test
    void disabledStructuresAreLeftOut() {
        Map<SizeClass, List<PoolEntry>> pools = StructureCatalog
            .pools(registry("factory_small", "gas_station"), s -> s != StructureId.GAS_STATION);
        assertEquals(
            1,
            pools.get(SizeClass.SMALL)
                .size());
        assertEquals(
            "factory_small",
            pools.get(SizeClass.SMALL)
                .get(0)
                .id());
    }

    @Test
    void everyStructureHasAClassAndOnlyShipsFloat() {
        for (StructureId s : StructureId.values()) {
            assertEquals(s == StructureId.AIRCRAFT_CARRIER || s == StructureId.SUBMARINE, StructureCatalog.isWater(s));
            assertEquals(s, StructureCatalog.byId(s.id()));
        }
        assertEquals(SizeClass.SMALL, StructureCatalog.sizeClass(StructureId.GAS_STATION));
        assertEquals(SizeClass.MEDIUM, StructureCatalog.sizeClass(StructureId.ALIEN_BUG_NEST));
        assertEquals(SizeClass.BIG, StructureCatalog.sizeClass(StructureId.MILITARY_BASE));
        assertNull(StructureCatalog.byId("test_hut"));
    }

    @Test
    void idsToLoadSkipsDisabled() {
        List<String> ids = StructureCatalog.idsToLoad(s -> s.enabledByDefault());
        assertFalse(ids.contains("alien_bug_nest"));
        assertTrue(ids.contains("big_bunker"));
    }
}
