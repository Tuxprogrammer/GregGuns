package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.gregguns.api.Tier;

/**
 * The tier self-check for structure loot: no line, and no drawn stack, is above its category's ceiling
 * (GGS-11-21, GGS-11-22). Pure; the in-game side feeds in the real item tier.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-21, GGS-11-22, Tests)
public final class LootTierCheck {

    private LootTierCheck() {}

    /**
     * @param category   a category
     * @param actualTier the item's own declared tier by reference ({@code Tiered.getTier()} in game), or null for a
     *                   reference that is not a GregGuns item; the function itself may be null to skip this half
     * @return one message per line whose declared tier, or real item tier, is above the ceiling, or whose declared
     *         tier is below the real one
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
}
