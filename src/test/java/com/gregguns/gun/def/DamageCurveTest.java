package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

/** The damage curve of design 0016: every tier from HV up out-damages the one below it, and has a floor. */
// Spec: docs/design/0016-damage-balance-pass.md
class DamageCurveTest {

    /** Explosive launchers (blast damage, balanced separately). */
    private static final Set<String> EXPLOSIVE = new HashSet<>(
        Arrays.asList("grenade_launcher", "rocket_launcher", "nuke_launcher"));

    /**
     * Not on the per-shot curve: the beam that hits every tick (compared by its floor), and the single-shot or volley
     * specials whose one pull is a different class (anti-materiel rifle, Grim Reaper, Sonic Rifle volley).
     */
    private static final Set<String> SPECIAL = new HashSet<>(
        Arrays.asList("nuclear_death_ray", "as50", "grim_reaper", "sonic_rifle", "flamethrower"));

    /** Off the floor check: the flamethrower (a stream) and the minigun (one shot per tick: 7 is 140 per second). */
    private static final Set<String> STREAMS = new HashSet<>(Arrays.asList("flamethrower", "minigun"));

    /**
     * The Golden Gun: its per-shot damage (1000) is nominal because the annihilation rule kills outright, so it sits
     * outside both the curve and the tier floor.
     */
    private static final Set<String> KILL_RULE = new HashSet<>(Arrays.asList("golden_gun"));

    private static double maxPerShot(Tier tier) {
        double max = 0;
        for (GunDefinition d : GunDefinitions.ALL) {
            if (d.tier() == tier && d.tool() == null
                && !EXPLOSIVE.contains(d.name())
                && !SPECIAL.contains(d.name())
                && !KILL_RULE.contains(d.name())) {
                max = Math.max(
                    max,
                    d.stats()
                        .damage());
            }
        }
        return max;
    }

    @Test
    void eachTierFromHvUpIsAtLeastTwentyPercentAboveTheOneBelow() {
        Tier[] order = { Tier.LV, Tier.MV, Tier.HV, Tier.EV, Tier.IV, Tier.LUV };
        for (int i = 2; i < order.length; i++) {
            double cur = maxPerShot(order[i]);
            double prev = maxPerShot(order[i - 1]);
            assertTrue(cur >= 1.2 * prev, order[i] + " max " + cur + " must be >= 1.2 x " + order[i - 1] + " " + prev);
        }
    }

    @Test
    void noGunIsBelowItsTierFloor() {
        for (GunDefinition d : GunDefinitions.ALL) {
            if (d.tool() != null || EXPLOSIVE.contains(d.name())
                || STREAMS.contains(d.name())
                || KILL_RULE.contains(d.name())) {
                continue;
            }
            double floor = floor(d.tier());
            assertTrue(
                d.stats()
                    .damage() >= floor,
                d.name() + " "
                    + d.stats()
                        .damage()
                    + " < floor "
                    + floor);
        }
    }

    private static double floor(Tier tier) {
        switch (tier) {
            case HV:
                return 8;
            case EV:
                return 9;
            case IV:
                return 12;
            case LUV:
                return 40;
            case ZPM:
                return 18;
            default:
                return 0;
        }
    }
}
