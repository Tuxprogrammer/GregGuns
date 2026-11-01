package com.gregguns.worldgen.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/** A pack without some mod still gets every chest, minus that mod's lines (GGS-11-26, GGS-06-03). */
class LootResolveTest {

    private static LootEntry find(List<LootEntry> lines, String ref) {
        for (LootEntry e : lines) {
            if (e.ref()
                .equals(ref)) {
                return e;
            }
        }
        return null;
    }

    @Test
    void aMissingModsLinesAreSkippedAndTheRestKeepTheirWeights() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.supplyChest(2));
        Predicate<String> noPams = ref -> !ref.startsWith("harvestcraft:") && !ref.startsWith("Forestry:");
        List<LootEntry> kept = LootCategories.resolvable(c, noPams);
        int pams = 0;
        for (LootEntry e : c.entries()) {
            if (!noPams.test(e.ref())) {
                pams++;
            }
        }
        assertTrue(pams > 10, "the supply chest names HarvestCraft and Forestry items");
        assertEquals(
            c.entries()
                .size() - pams,
            kept.size());
        Map<String, Integer> weights = new HashMap<>();
        for (LootEntry e : c.entries()) {
            weights.put(e.ref(), e.weight());
        }
        for (LootEntry e : kept) {
            assertTrue(noPams.test(e.ref()), e.ref());
            assertEquals(weights.get(e.ref()), Integer.valueOf(e.weight()), e.ref());
        }
        assertNotNull(find(kept, "minecraft:bread"));
        assertNull(find(kept, "harvestcraft:beefjerkyItem"));
    }

    @Test
    void aGunWhoseAmmoIsMissingStaysWithoutItsCompanion() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.weaponChest(2));
        assertNotNull(find(c.entries(), "gregguns:thompson_smg").companion(), "the Thompson brings SMG magazines");
        List<LootEntry> kept = LootCategories.resolvable(c, ref -> !ref.equals("gregguns:mag_smg"));
        LootEntry thompson = find(kept, "gregguns:thompson_smg");
        assertNotNull(thompson);
        assertNull(thompson.companion());
        assertNull(find(kept, "gregguns:mag_smg"));
        assertNotNull(find(kept, "gregguns:m4").companion());
    }

    @Test
    void nothingResolvesMeansAnEmptyButValidCategory() {
        LootCategory c = LootCategoryDefs.get(LootCategoryDefs.BARRACKS);
        assertTrue(
            LootCategories.resolvable(c, ref -> false)
                .isEmpty());
        assertFalse(
            LootCategories.resolvable(c, ref -> true)
                .isEmpty());
        assertEquals(c.entries(), LootCategories.resolvable(c, ref -> true));
    }
}
