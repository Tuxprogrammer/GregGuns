package com.gregguns.core.dump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class DumpRowTest {

    private static DumpRow row() {
        return new DumpRow("machine", "gt.recipe.assembler", "gregguns");
    }

    /** Declaration order and split stacks do not change the canonical input key. */
    @Test
    void inputOrderAndSplitStacksNormalize() {
        DumpRow a = row();
        a.addInput(DumpRow.itemKey("gregtech:gt.metaitem.01", 17), 2);
        a.addInput(DumpRow.oreKey("plateSteel"), 1);
        DumpRow b = row();
        b.addInput(DumpRow.oreKey("plateSteel"), 1);
        b.addInput(DumpRow.itemKey("gregtech:gt.metaitem.01", 17), 1);
        b.addInput(DumpRow.itemKey("gregtech:gt.metaitem.01", 17), 1);
        assertEquals(a.inputKey(), b.inputKey());
    }

    /** The integrated circuit is lifted out of the inputs. */
    @Test
    void circuitIsLiftedOut() {
        DumpRow a = row();
        a.addInput(DumpRow.itemKey(DumpRow.CIRCUIT_ITEM, 3), 1);
        a.addInput(DumpRow.oreKey("dustIron"), 1);
        assertEquals(3, a.circuit);
        assertEquals(
            1,
            a.inputs()
                .size());
        DumpRow b = row();
        b.addInput(DumpRow.oreKey("dustIron"), 1);
        assertNotEquals(a.inputKey(), b.inputKey());
    }

    /** Wildcard meta prints as star, fluids are keyed apart from items. */
    @Test
    void wildcardAndFluidKeys() {
        assertEquals("minecraft:stick:*", DumpRow.itemKey("minecraft:stick", 32767));
        DumpRow a = row();
        a.addFluid("water", 500);
        a.addFluid("water", 500);
        assertEquals(
            1000,
            (int) a.fluids()
                .get("fluid:water"));
    }

    /** The JSON line carries the row shape and escapes quotes. */
    @Test
    void jsonShape() {
        DumpRow a = new DumpRow("shaped", "crafting", "gregguns");
        a.addInput("a\"b:0", 1);
        a.addOutput("gregguns:x:0", 2);
        a.eut = 30;
        a.grid = Arrays.asList(Arrays.asList("a\"b:0", ""));
        String j = a.toJson();
        assertTrue(j.contains("\"inputs\":[{\"k\":\"a\\\"b:0\",\"n\":1}]"), j);
        assertTrue(j.contains("\"outputs\":[{\"k\":\"gregguns:x:0\",\"n\":2}]"), j);
        assertTrue(j.contains("\"eut\":30"), j);
        assertTrue(j.contains("\"grid\":[[\"a\\\"b:0\",\"\"]]"), j);
    }
}
