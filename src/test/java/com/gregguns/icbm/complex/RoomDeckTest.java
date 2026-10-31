package com.gregguns.icbm.complex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** D307: rooms are dealt like a deck, so no kind repeats while a kind that fits is still missing. */
class RoomDeckTest {

    private static final int SEEDS = 1000;

    private static int lateral(Room r) {
        int s = r.doorSide();
        return s == 0 || s == 2 ? r.bounds()
            .sizeX()
            : r.bounds()
                .sizeZ();
    }

    private static int depth(Room r) {
        int s = r.doorSide();
        return s == 0 || s == 2 ? r.bounds()
            .sizeZ()
            : r.bounds()
                .sizeX();
    }

    @Test
    void theDeckDealsEveryKindOnceBeforeAnyRepeats() {
        RoomDeck deck = new RoomDeck(new Random(5), 60);
        Map<RoomKind, Integer> n = new EnumMap<RoomKind, Integer>(RoomKind.class);
        List<RoomKind> order = new ArrayList<RoomKind>();
        while (!deck.empty()) {
            RoomKind k = deck.candidates()
                .get(0);
            deck.deal(k);
            order.add(k);
            n.merge(k, 1, Integer::sum);
            int min = Integer.MAX_VALUE, max = 0;
            for (RoomKind o : RoomKind.values()) {
                if (!RoomDeck.dealt(o) || o.mandatory) continue;
                int c = n.getOrDefault(o, 0);
                min = Math.min(min, c);
                max = Math.max(max, c);
            }
            assertTrue(max - min <= 1, "optional kinds never differ by two: " + order);
        }
        for (RoomKind k : RoomKind.values()) {
            if (RoomDeck.dealt(k) && k.mandatory) assertEquals(1, (int) n.get(k), k + " once");
        }
    }

    @Test
    void theMandatoryKindsTakeTheLastSlotsWhenTheDeckRunsShort() {
        RoomDeck deck = new RoomDeck(new Random(9), 6);
        List<RoomKind> got = new ArrayList<RoomKind>();
        while (!deck.empty()) {
            // A slot that only takes the deck's last card: the mandatory ones must still come.
            List<RoomKind> c = deck.candidates();
            RoomKind k = c.get(c.size() - 1);
            deck.deal(k);
            got.add(k);
        }
        for (RoomKind k : RoomKind.values()) {
            if (RoomDeck.dealt(k) && k.mandatory) assertTrue(got.contains(k), k + " dealt: " + got);
        }
    }

    @Test
    void noOptionalKindRepeatsWhileAFittingKindIsMissing() {
        Map<RoomKind, Integer> total = new EnumMap<RoomKind, Integer>(RoomKind.class);
        int repeatsAll = 0;
        for (long s = 0; s < SEEDS; s++) {
            ComplexPlan p = ComplexLayout.generate(s, (int) s, 3);
            Map<RoomKind, Integer> n = new EnumMap<RoomKind, Integer>(RoomKind.class);
            for (Room r : p.rooms()) {
                n.merge(r.kind(), 1, Integer::sum);
                total.merge(r.kind(), 1, Integer::sum);
                if (r.kind() == RoomKind.SHOWERS_LAUNDRY) {
                    assertTrue(lateral(r) <= 18 && depth(r) <= 16, "seed " + s + " showers " + r.bounds());
                }
                assertTrue(
                    r.kind()
                        .fits(lateral(r), depth(r)),
                    "seed " + s + " " + r.kind() + " " + lateral(r) + " x " + depth(r));
            }
            boolean repeat = false;
            for (RoomKind k : RoomKind.values()) {
                if (!RoomDeck.dealt(k)) continue;
                if (k.mandatory) assertEquals(1, (int) n.getOrDefault(k, 0), "seed " + s + " " + k);
                else if (n.getOrDefault(k, 0) > 1) repeat = true;
            }
            if (!repeat) continue;
            repeatsAll++;
            for (RoomKind k : RoomKind.values()) {
                if (!RoomDeck.dealt(k) || k.mandatory || n.getOrDefault(k, 0) > 0) continue;
                // A missing kind with no minimum size would have fit any slot a repeat took.
                boolean sized = k.minLateral > 0 || k.minDepth > 0;
                // A sized kind (the farm, the range) may be missing: no slot was big enough when it was dealt.
                assertTrue(sized, "seed " + s + ": a kind repeats while " + k + " is missing " + n);
            }
        }
        System.out.println("RoomDeckTest per-kind rooms over " + SEEDS + " plans: " + total);
        System.out.println("RoomDeckTest plans with a repeat: " + repeatsAll + " of " + SEEDS);
        for (RoomKind k : RoomKind.values()) {
            if (RoomDeck.dealt(k)) assertTrue(total.getOrDefault(k, 0) > SEEDS / 4, k + " is dealt: " + total);
        }
    }
}
