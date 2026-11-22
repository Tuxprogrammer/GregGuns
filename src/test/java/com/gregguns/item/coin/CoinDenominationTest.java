package com.gregguns.item.coin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class CoinDenominationTest {

    @Test
    void theLadderMatchesThePackCoins() {
        int[] values = { 1, 10, 100, 1000, 10000 };
        String[] labels = { "1", "10", "100", "1000", "10K" };
        CoinDenomination[] all = CoinDenomination.values();
        assertEquals(values.length, all.length);
        for (int i = 0; i < all.length; i++) {
            assertEquals(values[i], all[i].value());
            assertEquals(labels[i], all[i].label());
            assertEquals(i, all[i].meta(), "the item damage is the ordinal (world data)");
            assertSame(all[i], CoinDenomination.byMeta(i));
            if (i > 0) {
                assertEquals(all[i].value(), all[i - 1].value() * CoinDenomination.STEP);
                assertSame(all[i - 1], all[i].previous());
                assertSame(all[i], all[i - 1].next());
            }
        }
        assertNull(CoinDenomination.byMeta(-1));
        assertNull(CoinDenomination.byMeta(all.length));
        assertNull(CoinDenomination.ONE.previous());
        assertNull(CoinDenomination.TEN_THOUSAND.next());
    }

    @Test
    void everyDenominationHasItsTexture() {
        for (CoinDenomination d : CoinDenomination.values()) {
            String path = "src/main/resources/assets/gregguns/textures/items/" + d.iconName() + ".png";
            assertTrue(Files.isRegularFile(Paths.get(path)), "missing " + path);
        }
        assertEquals(ItemCoin.NAME, CoinDenomination.ONE.iconName(), "the base icon is the item's texture name");
    }

    @Test
    void exactPicksTheLargestDenominationThatFitsOneStack() {
        check(5, CoinDenomination.ONE, 5);
        check(60, CoinDenomination.TEN, 6);
        check(64, CoinDenomination.ONE, 64);
        check(150, CoinDenomination.TEN, 15);
        check(300, CoinDenomination.HUNDRED, 3);
        check(1500, CoinDenomination.HUNDRED, 15);
        check(2000, CoinDenomination.THOUSAND, 2);
        check(10000, CoinDenomination.TEN_THOUSAND, 1);
        check(640, CoinDenomination.TEN, 64);
        assertNull(CoinDenomination.exact(65), "65 needs two stacks");
        assertNull(CoinDenomination.exact(0));
        assertNull(CoinDenomination.exact(-10));
        assertNull(CoinDenomination.exact(650_000 + 1));
    }

    @Test
    void changePaysASumLargestFirstInWholeStacks() {
        assertEquals(
            "[1 x 1000, 2 x 100, 3 x 10, 4 x 1]",
            CoinDenomination.change(1234)
                .toString());
        assertEquals(
            "[1 x 10K, 2 x 1000, 3 x 100, 4 x 10, 5 x 1]",
            CoinDenomination.change(12345)
                .toString());
        assertEquals(
            "[6 x 10]",
            CoinDenomination.change(60)
                .toString());
        assertEquals(
            "[64 x 10K, 36 x 10K]",
            CoinDenomination.change(1_000_000)
                .toString(),
            "64 coins a stack");
        assertTrue(
            CoinDenomination.change(0)
                .isEmpty());
        assertTrue(
            CoinDenomination.change(-5)
                .isEmpty());
        for (long v : new long[] { 1, 9, 99, 640_001, 1_234_567 }) {
            long sum = 0;
            for (CoinDenomination.Stack s : CoinDenomination.change(v)) {
                sum += s.value();
            }
            assertEquals(v, sum, "change(" + v + ") adds up");
        }
    }

    @Test
    void stacksRejectBadCounts() {
        assertThrows(IllegalArgumentException.class, () -> new CoinDenomination.Stack(CoinDenomination.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new CoinDenomination.Stack(CoinDenomination.ONE, 65));
        assertThrows(IllegalArgumentException.class, () -> new CoinDenomination.Stack(null, 1));
        assertEquals(3000L, new CoinDenomination.Stack(CoinDenomination.THOUSAND, 3).value());
    }

    private static void check(long value, CoinDenomination d, int count) {
        CoinDenomination.Stack s = CoinDenomination.exact(value);
        assertSame(d, s.denomination(), "denomination for " + value);
        assertEquals(count, s.count(), "count for " + value);
        assertEquals(value, s.value());
    }
}
