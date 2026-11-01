package com.gregguns.worldgen.loot;

import java.util.function.Predicate;

import com.gregguns.api.Tier;

/**
 * Exact odds of one chest drawn from a category at a richness, from the weights alone (no sampling): the chance per
 * pick of a line group, the expected stacks per chest, and the chance that a chest holds at least one. Test helper.
 */
final class LootOdds {

    private final LootCategory category;
    private final int richness;
    private final double total;

    LootOdds(LootCategory category, int richness) {
        this.category = category;
        this.richness = richness;
        Tier top = LootDraw.topTier(category);
        double t = 0;
        for (LootEntry e : category.entries()) {
            t += LootDraw.weight(e, top, richness);
        }
        this.total = t;
    }

    /** @return the chance that one pick lands on a line the test accepts */
    double perPick(Predicate<LootEntry> lines) {
        Tier top = LootDraw.topTier(category);
        double w = 0;
        for (LootEntry e : category.entries()) {
            if (lines.test(e)) {
                w += LootDraw.weight(e, top, richness);
            }
        }
        return w / total;
    }

    /** @return the expected number of picks per chest */
    double picks() {
        int lo = category.minItems() + LootDraw.clamp(richness);
        int hi = category.maxItems() + LootDraw.clamp(richness);
        return (lo + hi) / 2.0;
    }

    /** @return the chance that a chest holds at least one pick the test accepts */
    double chestHas(Predicate<LootEntry> lines) {
        double p = perPick(lines);
        int lo = category.minItems() + LootDraw.clamp(richness);
        int hi = category.maxItems() + LootDraw.clamp(richness);
        double none = 0;
        for (int n = lo; n <= hi; n++) {
            none += Math.pow(1 - p, n);
        }
        return 1 - none / (hi - lo + 1);
    }

    /** @return the expected gun stacks per chest */
    double guns() {
        return picks() * perPick(e -> Arsenal.GUN.equals(e.pool()));
    }

    /** @return the expected ammo stacks per chest: ammo picks plus the ammo every gun brings along */
    double ammo() {
        return picks() * (perPick(e -> Arsenal.AMMO.equals(e.pool()))
            + perPick(e -> Arsenal.GUN.equals(e.pool()) && e.companion() != null));
    }

    /** @return the chance that a chest holds a gun */
    double chestHasGun() {
        return chestHas(e -> Arsenal.GUN.equals(e.pool()));
    }

    /** @return the chance that a chest holds ammo (an ammo pick, or a gun that brings its ammo) */
    double chestHasAmmo() {
        return chestHas(e -> Arsenal.AMMO.equals(e.pool()) || Arsenal.GUN.equals(e.pool()) && e.companion() != null);
    }

    /** @return the chance per pick of a GregGuns line (any reference in the gregguns namespace) */
    double gregGunsPerPick() {
        return perPick(
            e -> e.ref()
                .startsWith("gregguns:"));
    }
}
