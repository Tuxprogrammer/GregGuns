package com.gregguns.armor.logic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * An immutable map from {@link ArmorBonus} to an amount. Missing bonuses read as 0. Equality compares amounts, so two
 * maps with the same totals are equal. Plain Java, both sides.
 */
public final class BonusMap {

    /** The map with no bonuses. */
    public static final BonusMap EMPTY = new BonusMap(new EnumMap<>(ArmorBonus.class));

    private final EnumMap<ArmorBonus, Double> values;

    private BonusMap(EnumMap<ArmorBonus, Double> values) {
        this.values = values;
    }

    /**
     * @param pairs alternating bonus and amount, for example {@code of(SPEED, 0.125, JUMP, 0.02)}
     * @return the map; zero amounts are dropped
     */
    public static BonusMap of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs");
        }
        EnumMap<ArmorBonus, Double> m = new EnumMap<>(ArmorBonus.class);
        for (int i = 0; i < pairs.length; i += 2) {
            double amount = ((Number) pairs[i + 1]).doubleValue();
            if (amount != 0.0) {
                m.merge((ArmorBonus) pairs[i], amount, Double::sum);
            }
        }
        return new BonusMap(m);
    }

    /**
     * @param bonus the bonus
     * @return its amount, 0 when absent
     */
    public double get(ArmorBonus bonus) {
        Double v = values.get(bonus);
        return v == null ? 0.0 : v;
    }

    /** @return true when no bonus is set */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** @return a read-only view of the non-zero amounts */
    public Map<ArmorBonus, Double> asMap() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * @param bonus  a bonus
     * @param amount its new amount; 0 removes it
     * @return a copy with the amount replaced
     */
    public BonusMap with(ArmorBonus bonus, double amount) {
        EnumMap<ArmorBonus, Double> m = new EnumMap<>(values);
        if (amount == 0.0) {
            m.remove(bonus);
        } else {
            m.put(bonus, amount);
        }
        return new BonusMap(m);
    }

    /**
     * @param other another map
     * @return the sum of both, per bonus
     */
    public BonusMap plus(BonusMap other) {
        if (other.isEmpty()) {
            return this;
        }
        EnumMap<ArmorBonus, Double> m = new EnumMap<>(values);
        for (Map.Entry<ArmorBonus, Double> e : other.values.entrySet()) {
            m.merge(e.getKey(), e.getValue(), Double::sum);
        }
        return new BonusMap(m);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BonusMap && values.equals(((BonusMap) o).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
