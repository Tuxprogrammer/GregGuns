package com.gregguns.armor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorItemOrder.Kind;
import com.gregguns.armor.ArmorItemOrder.Slot;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;

class ArmorItemOrderTest {

    private final List<Slot> order = ArmorItemOrder.build();

    @Test
    void tierNeverDecreasesDownTheList() {
        Tier previous = Tier.STEAM;
        for (Slot slot : order) {
            assertTrue(
                slot.tier()
                    .ordinal() >= previous.ordinal(),
                slot.name() + " is out of tier order");
            previous = slot.tier();
        }
    }

    @Test
    void everyItemAppearsOnce() {
        Set<String> seen = new HashSet<>();
        for (Slot slot : order) {
            assertTrue(seen.add(slot.name()), "duplicate " + slot.name());
        }
    }

    @Test
    void everySetHasItsPiecesContiguousAndInSlotOrder() {
        for (SetDef set : ArmorCatalog.ALL) {
            int first = -1;
            int expected = 0;
            for (int i = 0; i < order.size(); i++) {
                Slot slot = order.get(i);
                if (slot.set() != set) {
                    continue;
                }
                if (first < 0) {
                    first = i;
                }
                assertEquals(first + expected, i, set.id() + " pieces are not contiguous");
                expected++;
            }
            assertEquals(set.pieceCount(), expected, set.id() + " is missing pieces");
            ArmorSlot previous = null;
            for (Slot slot : order.subList(first, first + expected)) {
                assertEquals(set.pieceName(slot.armorSlot()), slot.name());
                assertTrue(
                    previous == null || slot.armorSlot()
                        .ordinal() > previous.ordinal());
                previous = slot.armorSlot();
            }
        }
    }

    @Test
    void setsKeepCatalogOrderWithinATier() {
        int last = -1;
        Tier tier = null;
        for (Slot slot : order) {
            if (slot.kind() != Kind.ARMOR_PIECE) {
                continue;
            }
            int index = ArmorCatalog.ALL.indexOf(slot.set());
            if (slot.tier() == tier) {
                assertTrue(index >= last, slot.name() + " breaks catalog order");
            }
            tier = slot.tier();
            last = index;
        }
    }

    @Test
    void armorThenAccessoriesThenPartsInsideATier() {
        for (Tier tier : Tier.values()) {
            int rank = 0;
            for (Slot slot : order) {
                if (slot.tier() != tier) {
                    continue;
                }
                int r = slot.kind() == Kind.ARMOR_PIECE ? 0 : slot.kind() == Kind.PART ? 2 : 1;
                assertTrue(r >= rank, slot.name() + " is out of group order in " + tier);
                rank = r;
            }
        }
    }

    @Test
    void ownPartsAreAllPlaced() {
        Set<String> placed = new HashSet<>();
        for (Slot slot : order) {
            if (slot.kind() == Kind.PART) {
                placed.add(slot.name());
            }
        }
        assertEquals(new HashSet<>(ArmorParts.NAMES), placed);
    }
}
