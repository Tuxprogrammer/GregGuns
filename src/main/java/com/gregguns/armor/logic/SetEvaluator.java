package com.gregguns.armor.logic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds up the bonuses of what a wearer has on and detects complete sets. Stateless: the same worn pieces always give
 * the same result, so equipping, unequipping, death and dimension change need no bookkeeping beyond comparing the
 * result with what is applied ({@link BonusLifecycle}). Plain Java, both sides.
 *
 * <p>
 * Rules: a broken piece (see {@link ArmorProtection#isBroken}) contributes nothing and does not count toward a set.
 * Piece bonuses add up across sets. A set is complete when it has as many intact worn pieces as it defines; a complete
 * set adds its {@link SetDef#fullSetBonus()} once.
 */
// Spec: docs/techguns-spec/05-armor/set-bonuses.md section 2 (aggregation); Deviation: Techguns has no full-set
// bonuses; GregGuns adds the hook for later sets and leaves it empty for T1.
public final class SetEvaluator {

    /** One worn piece, as the evaluator needs it. */
    public static final class Worn {

        private final SetDef set;
        private final PieceDef piece;
        private final boolean broken;
        private final boolean powered;

        /**
         * @param set    the piece's set
         * @param piece  the piece
         * @param broken whether the stack is broken
         */
        public Worn(SetDef set, PieceDef piece, boolean broken) {
            this(set, piece, broken, false);
        }

        /**
         * @param set     the piece's set
         * @param piece   the piece
         * @param broken  whether the stack is broken
         * @param powered whether the set's chestplate is worn, intact and charged; switches on the piece's
         *                {@link PieceDef#poweredBonuses()}
         */
        public Worn(SetDef set, PieceDef piece, boolean broken, boolean powered) {
            this.set = set;
            this.piece = piece;
            this.broken = broken;
            this.powered = powered;
        }
    }

    /** What a wearer has on. */
    public static final class Result {

        private final BonusMap bonuses;
        private final Set<String> completeSets;
        private final Map<String, Integer> intactPieces;

        Result(BonusMap bonuses, Set<String> completeSets, Map<String, Integer> intactPieces) {
            this.bonuses = bonuses;
            this.completeSets = Collections.unmodifiableSet(completeSets);
            this.intactPieces = Collections.unmodifiableMap(intactPieces);
        }

        /** @return the summed piece bonuses plus the full-set bonuses */
        public BonusMap bonuses() {
            return bonuses;
        }

        /** @return the ids of the sets worn complete and intact */
        public Set<String> completeSets() {
            return completeSets;
        }

        /**
         * @param setId a set id
         * @return how many intact pieces of the set are worn
         */
        public int intactPieces(String setId) {
            Integer n = intactPieces.get(setId);
            return n == null ? 0 : n;
        }
    }

    private SetEvaluator() {}

    /**
     * @param worn the worn GregGuns pieces, any order, at most one per slot
     * @return the summed bonuses and the complete sets
     */
    public static Result evaluate(List<Worn> worn) {
        BonusMap total = BonusMap.EMPTY;
        Map<String, Integer> counts = new HashMap<>();
        Map<String, SetDef> sets = new HashMap<>();
        for (Worn w : worn) {
            if (w.broken) {
                continue;
            }
            total = total.plus(w.piece.bonuses());
            if (w.powered) {
                total = total.plus(w.piece.poweredBonuses());
            }
            counts.merge(w.set.id(), 1, Integer::sum);
            sets.put(w.set.id(), w.set);
        }
        Set<String> complete = new HashSet<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            SetDef set = sets.get(e.getKey());
            if (e.getValue() == set.pieceCount()) {
                complete.add(set.id());
                total = total.plus(set.fullSetBonus());
            }
        }
        return new Result(total, complete, counts);
    }
}
