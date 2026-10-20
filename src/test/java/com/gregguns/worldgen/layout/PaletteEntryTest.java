package com.gregguns.worldgen.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** WGEN-14: a choice of weight w owns exactly w of the rolls; the first option gets no extra point. */
class PaletteEntryTest {

    private static PaletteEntry entry(int... weights) {
        List<PaletteChoice> list = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            list.add(new PaletteChoice("k" + i, PaletteChoice.KEY_META, RotationKind.NONE, weights[i]));
        }
        return new PaletteEntry(list);
    }

    @Test
    void everyChoiceOwnsExactlyItsWeightInRolls() {
        PaletteEntry e = entry(2, 1);
        assertEquals(3, e.totalWeight());
        assertEquals("k0", e.pick(0).key);
        assertEquals("k0", e.pick(1).key);
        assertEquals("k1", e.pick(2).key);
        int[] owned = new int[2];
        for (int r = 0; r < e.totalWeight(); r++) {
            owned[Integer.parseInt(e.pick(r).key.substring(1))]++;
        }
        assertEquals(2, owned[0], "Techguns gave this option 3 of 4");
        assertEquals(1, owned[1]);
    }

    @Test
    void rollOutsideTheRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> entry(2, 1).pick(3));
    }

    @Test
    void positionKeyedPickIsDeterministicAndRoughlyProportional() {
        PaletteEntry e = entry(2, 1);
        int first = 0;
        int n = 30000;
        for (int i = 0; i < n; i++) {
            PaletteChoice a = e.choose(7L, i, 64 + (i % 5), i * 3);
            PaletteChoice b = e.choose(7L, i, 64 + (i % 5), i * 3);
            assertTrue(a == b, "same inputs must give the same pick");
            if (a.key.equals("k0")) {
                first++;
            }
        }
        double share = first / (double) n;
        assertEquals(2.0 / 3.0, share, 0.02, "share of the weight-2 option");
    }

    @Test
    void variantChangesTheSequence() {
        PaletteEntry e = entry(1, 1);
        int differ = 0;
        for (int i = 0; i < 200; i++) {
            if (e.choose(1L, i, 70, 0) != e.choose(2L, i, 70, 0)) {
                differ++;
            }
        }
        assertTrue(differ > 40, "variants should decorrelate, differed " + differ);
    }

    @Test
    void singleChoiceNeedsNoRoll() {
        PaletteEntry e = entry(5);
        assertEquals("k0", e.choose(0L, -3, 4, 5).key);
    }
}
