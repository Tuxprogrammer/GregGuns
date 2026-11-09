package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;

/**
 * The tier self-check for structure loot: no line, and no drawn stack, is above its category's ceiling
 * (GGS-11-21, GGS-11-22), and no line's whole crafting chain is above the tier it declares (D363). Pure; the in-game
 * side feeds in the real item tier and the {@link ObtainTier} answer.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21, GGS-11-22, Loot tier cap, Tests)
public final class LootTierCheck {

    private LootTierCheck() {}

    /**
     * @param category   a category
     * @param actualTier the item's own declared tier by reference ({@code Tiered.getTier()} in game), or null for a
     *                   reference that is not a GregGuns item; the function itself may be null to skip this half
     * @return one message per line whose declared tier, or real item tier, is above the ceiling, or whose declared
     *         tier is below the real one, or whose companion (a gun's ammo) is really above the ceiling
     */
    public static List<String> violations(LootCategory category, @Nullable Function<String, Tier> actualTier) {
        List<String> bad = new ArrayList<>();
        int ceiling = category.ceiling()
            .ordinal();
        for (LootEntry e : category.entries()) {
            if (e.tier()
                .ordinal() > ceiling) {
                bad.add(category.name() + ": " + e.ref() + " declares " + e.tier() + ", above " + category.ceiling());
            }
            Tier real = actualTier == null ? null : actualTier.apply(e.ref());
            if (real != null && real.ordinal() > ceiling) {
                bad.add(category.name() + ": " + e.ref() + " is really " + real + ", above " + category.ceiling());
            }
            if (real != null && real.ordinal() > e.tier()
                .ordinal()) {
                bad.add(category.name() + ": " + e.ref() + " declares " + e.tier() + " but is really " + real);
            }
            Tier companion = actualTier == null || e.companion() == null ? null
                : actualTier.apply(
                    e.companion()
                        .ref());
            if (companion != null && companion.ordinal() > ceiling) {
                bad.add(
                    category.name() + ": "
                        + e.ref()
                        + " brings "
                        + e.companion()
                            .ref()
                        + ", really "
                        + companion
                        + ", above "
                        + category.ceiling());
            }
        }
        return bad;
    }

    /**
     * Draws chests and reports every stack above the ceiling. This is the "1000 chests per structure" check.
     *
     * @param category a category
     * @param richness 0..2
     * @param chests   how many chests to draw
     * @param seed     the seed
     * @return one message per offending stack; empty when the ceiling holds
     */
    public static List<String> drawViolations(LootCategory category, int richness, int chests, long seed) {
        List<String> bad = new ArrayList<>();
        Random rand = new Random(seed);
        int ceiling = category.ceiling()
            .ordinal();
        for (int c = 0; c < chests; c++) {
            for (LootDraw.Roll roll : LootDraw.draw(category, richness, rand)) {
                if (roll.entry()
                    .tier()
                    .ordinal() > ceiling) {
                    bad.add(
                        "chest " + c
                            + ": "
                            + roll.entry()
                                .ref()
                            + " above "
                            + category.ceiling());
                }
            }
        }
        return bad;
    }

    /**
     * The obtain-tier audit of a table (D363): a line is only as low as the crafting chain of its item.
     *
     * @param label      what the lines belong to, for the messages
     * @param entries    the lines
     * @param obtainTier a reference's obtain tier (a GT voltage index), {@link ObtainTierRules#UNKNOWN} when nobody
     *                   vouched for it, or a negative number for a reference this pack does not have (skipped)
     * @return one message per line, or companion, with no known obtain tier or one above the line's declared tier
     */
    public static List<String> obtainViolations(String label, List<LootEntry> entries,
        ToIntFunction<String> obtainTier) {
        return obtainViolations(
            label,
            entries,
            e -> e.tier() == null ? Integer.MAX_VALUE
                : e.tier()
                    .voltageIndex(),
            obtainTier);
    }

    /**
     * The audit of {@link #obtainViolations(String, List, ToIntFunction)} for a table whose lines declare their tier
     * some other way (a silo ladder rung above ZPM declares its own voltage).
     *
     * @param label        what the lines belong to, for the messages
     * @param entries      the lines
     * @param declaredTier a line's declared tier (a GT voltage index)
     * @param obtainTier   a reference's obtain tier, as for the plain audit
     * @return one message per line, or companion, with no known obtain tier or one above the line's declared tier
     */
    public static List<String> obtainViolations(String label, List<LootEntry> entries,
        ToIntFunction<LootEntry> declaredTier, ToIntFunction<String> obtainTier) {
        List<String> bad = new ArrayList<>();
        for (LootEntry e : entries) {
            int declared = declaredTier.applyAsInt(e);
            check(label, e.ref(), declared, obtainTier, bad);
            if (e.companion() != null) {
                check(
                    label,
                    e.companion()
                        .ref(),
                    declared,
                    obtainTier,
                    bad);
            }
        }
        return bad;
    }

    /** @see #obtainViolations(String, List, ToIntFunction) */
    public static List<String> obtainViolations(LootCategory category, ToIntFunction<String> obtainTier) {
        return obtainViolations(category.name(), category.entries(), obtainTier);
    }

    private static void check(String label, String ref, int declared, ToIntFunction<String> obtainTier,
        List<String> bad) {
        int t = obtainTier.applyAsInt(ref);
        if (t < 0) {
            return;
        }
        if (t == ObtainTierRules.UNKNOWN) {
            bad.add(label + ": " + ref + " has no known obtain tier, so it never rolls");
        } else if (t > declared) {
            bad.add(
                label + ": "
                    + ref
                    + " declares "
                    + ObtainTierRules.name(declared)
                    + " but its chain needs "
                    + ObtainTierRules.name(t));
        }
    }
}
