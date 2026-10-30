package com.gregguns.item.coin;

/**
 * The five GregGuns Coin denominations. The item damage is the ordinal, so the order is world data and never changes.
 * Values and the five-step ladder follow the GTNH pack's coins ({@code dreamcraft:Coin<Kind>} to {@code ...IV}), so a
 * player reads a GregGuns price the same way. Pure data, no Minecraft types, both sides.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md
// Source (pack coin ladder 1/10/100/1000/10K, item suffixes "", I to IV):
// https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/item/NHItemList.java#L104-L108
public enum CoinDenomination {

    /** Worth 1. Gold rim. */
    ONE(1, "1", "coin"),
    /** Worth 10. Green rim. */
    TEN(10, "10", "coin_10"),
    /** Worth 100. Blue rim. */
    HUNDRED(100, "100", "coin_100"),
    /** Worth 1,000. Orange rim. */
    THOUSAND(1000, "1000", "coin_1000"),
    /** Worth 10,000. Black rim. */
    TEN_THOUSAND(10000, "10K", "coin_10000");

    /** How many coins of one denomination make one of the next (the exchange and change recipes). */
    public static final int STEP = 10;

    /** The most coins of one kind a single price stack may hold (one inventory stack). */
    public static final int MAX_STACK = 64;

    private static final CoinDenomination[] BY_META = values();

    private final int value;
    private final String label;
    private final String iconName;

    CoinDenomination(int value, String label, String iconName) {
        this.value = value;
        this.label = label;
        this.iconName = iconName;
    }

    /** @return the coin's worth in single coins */
    public int value() {
        return value;
    }

    /** @return the item damage of this denomination */
    public int meta() {
        return ordinal();
    }

    /** @return the value as the name shows it ("1", "10", "100", "1000", "10K", as the pack writes it) */
    public String label() {
        return label;
    }

    /** @return the bare texture name under {@code textures/items/} ({@code coin}, {@code coin_10}, ...) */
    public String iconName() {
        return iconName;
    }

    /** @return the next larger denomination, or {@code null} for the largest */
    public CoinDenomination next() {
        return ordinal() + 1 < BY_META.length ? BY_META[ordinal() + 1] : null;
    }

    /** @return the next smaller denomination, or {@code null} for the smallest */
    public CoinDenomination previous() {
        return ordinal() > 0 ? BY_META[ordinal() - 1] : null;
    }

    /**
     * @param meta an item damage
     * @return the denomination, or {@code null} for a damage that names none
     */
    public static CoinDenomination byMeta(int meta) {
        return meta >= 0 && meta < BY_META.length ? BY_META[meta] : null;
    }

    /**
     * A price as one stack of one denomination: the largest denomination that divides {@code value} exactly with at
     * most {@link #MAX_STACK} coins. Prices are kept to such values so the Vending Machine can match one stack.
     *
     * @param value a price in single coins
     * @return the stack, or {@code null} if no single stack can pay exactly that much
     */
    public static Stack exact(long value) {
        if (value <= 0) {
            return null;
        }
        for (int i = BY_META.length - 1; i >= 0; i--) {
            CoinDenomination d = BY_META[i];
            if (value % d.value == 0 && value / d.value <= MAX_STACK) {
                return new Stack(d, (int) (value / d.value));
            }
        }
        return null;
    }

    /** A number of coins of one denomination. Immutable. */
    public static final class Stack {

        private final CoinDenomination denomination;
        private final int count;

        /**
         * @param denomination the denomination
         * @param count        how many coins, 1 to {@link #MAX_STACK}
         */
        public Stack(CoinDenomination denomination, int count) {
            if (denomination == null || count < 1 || count > MAX_STACK) {
                throw new IllegalArgumentException("bad coin stack: " + count + " x " + denomination);
            }
            this.denomination = denomination;
            this.count = count;
        }

        /** @return the denomination */
        public CoinDenomination denomination() {
            return denomination;
        }

        /** @return how many coins */
        public int count() {
            return count;
        }

        /** @return the stack's total worth in single coins */
        public long value() {
            return (long) denomination.value * count;
        }

        @Override
        public String toString() {
            return count + " x " + denomination.label;
        }
    }
}
