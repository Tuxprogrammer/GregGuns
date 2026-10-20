package com.gregguns.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BenchTableTest {

    @Test
    void everyNaturaWoodIsATable() {
        for (int meta = 0; meta < BenchTable.NATURA_METAS; meta++) {
            assertTrue(BenchTable.accepts("Natura:Natura.workbench", meta), "meta " + meta);
        }
        assertFalse(BenchTable.accepts("Natura:Natura.workbench", BenchTable.NATURA_METAS));
        assertFalse(BenchTable.accepts("Natura:Natura.workbench", -1));
    }

    @Test
    void theAlternativesAreTablesAtAnyMeta() {
        assertTrue(BenchTable.accepts("minecraft:crafting_table", 0));
        assertTrue(BenchTable.accepts("Avaritia:Dire_Crafting", 0));
    }

    @Test
    void otherBlocksAreNot() {
        assertFalse(BenchTable.accepts(null, 0));
        assertFalse(BenchTable.accepts("minecraft:planks", 0));
        assertFalse(BenchTable.accepts("gregguns:casings", 0));
        assertFalse(BenchTable.accepts("Natura:Natura.planks", 0));
    }

    @Test
    void theShapeCharacterIsT() {
        assertEquals('T', BenchTable.SYMBOL);
    }
}
