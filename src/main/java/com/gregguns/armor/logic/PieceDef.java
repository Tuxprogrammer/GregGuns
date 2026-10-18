package com.gregguns.armor.logic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One armor piece of a set: slot, English name, its own bonuses, hazard flags and repair recipe. Plain Java, both
 * sides. Registry name and lang key come from the set ({@link SetDef#pieceName}).
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md section 5 (piece catalog)
public final class PieceDef {

    private final ArmorSlot slot;
    private final String englishName;
    private final BonusMap bonuses;
    private final RepairSpec repair;
    private final BonusMap poweredBonuses;
    private final String registryName;
    private final Set<HazardFlag> poweredPartialHazards;
    private final Set<HazardFlag> partialHazards;
    private final Set<HazardFlag> fullHazards;

    private PieceDef(Builder b) {
        this.slot = b.slot;
        this.englishName = b.englishName;
        this.bonuses = b.bonuses;
        this.repair = b.repair;
        this.poweredBonuses = b.poweredBonuses;
        this.registryName = b.registryName;
        this.poweredPartialHazards = Collections.unmodifiableSet(EnumSet.copyOf(b.poweredPartial));
        this.partialHazards = Collections.unmodifiableSet(EnumSet.copyOf(b.partial));
        this.fullHazards = Collections.unmodifiableSet(EnumSet.copyOf(b.full));
    }

    /**
     * @param slot        the slot
     * @param englishName the en_US name; the lang file must carry the same text
     * @return a builder
     */
    public static Builder builder(ArmorSlot slot, String englishName) {
        return new Builder(slot, englishName);
    }

    /** Builds a {@link PieceDef}. */
    public static final class Builder {

        private final ArmorSlot slot;
        private final String englishName;
        private BonusMap bonuses = BonusMap.EMPTY;
        private RepairSpec repair;
        private BonusMap poweredBonuses = BonusMap.EMPTY;
        private String registryName;
        private final EnumSet<HazardFlag> poweredPartial = EnumSet.noneOf(HazardFlag.class);
        private final EnumSet<HazardFlag> partial = EnumSet.noneOf(HazardFlag.class);
        private final EnumSet<HazardFlag> full = EnumSet.noneOf(HazardFlag.class);

        private Builder(ArmorSlot slot, String englishName) {
            this.slot = slot;
            this.englishName = englishName;
        }

        /**
         * @param b the piece's own bonuses
         * @return this
         */
        public Builder bonuses(BonusMap b) {
            this.bonuses = b;
            return this;
        }

        /**
         * @param r the repair recipe; null makes the piece unrepairable on an anvil
         * @return this
         */
        public Builder repair(RepairSpec r) {
            this.repair = r;
            return this;
        }

        /**
         * @param b bonuses that only count while the set's chestplate is powered ({@link SetDef#power()})
         * @return this
         */
        public Builder poweredBonuses(BonusMap b) {
            this.poweredBonuses = b;
            return this;
        }

        /**
         * @param name the registry name, for a single-piece set whose item is not called {@code <set>_<slot>} (the
         *             beret); null for the default
         * @return this
         */
        public Builder registryName(String name) {
            this.registryName = name;
            return this;
        }

        /**
         * @param flags partial hazards that only count while the set's chestplate is powered
         * @return this
         */
        public Builder poweredPartialHazards(HazardFlag... flags) {
            Collections.addAll(poweredPartial, flags);
            return this;
        }

        /**
         * @param flags hazards the piece protects against only together with the other pieces (GT partial)
         * @return this
         */
        public Builder partialHazards(HazardFlag... flags) {
            Collections.addAll(partial, flags);
            return this;
        }

        /**
         * @param flags hazards the piece protects against on its own (GT protectsAgainstFully)
         * @return this
         */
        public Builder fullHazards(HazardFlag... flags) {
            Collections.addAll(full, flags);
            return this;
        }

        /** @return the piece */
        public PieceDef build() {
            return new PieceDef(this);
        }
    }

    /** @return the slot */
    public ArmorSlot slot() {
        return slot;
    }

    /** @return the en_US display name */
    public String englishName() {
        return englishName;
    }

    /** @return the piece's own bonuses */
    public BonusMap bonuses() {
        return bonuses;
    }

    /** @return the bonuses that need a powered chestplate; empty for an unpowered set */
    public BonusMap poweredBonuses() {
        return poweredBonuses;
    }

    /** @return the explicit registry name, or null when it is {@code <set>_<slot>} */
    public String registryName() {
        return registryName;
    }

    /** @return partial hazards that count only while the chestplate is powered */
    public Set<HazardFlag> poweredPartialHazards() {
        return poweredPartialHazards;
    }

    /** @return the repair recipe, or null */
    public RepairSpec repair() {
        return repair;
    }

    /** @return hazards that count toward GT's full-set rule */
    public Set<HazardFlag> partialHazards() {
        return partialHazards;
    }

    /** @return hazards the piece covers alone */
    public Set<HazardFlag> fullHazards() {
        return fullHazards;
    }
}
