package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.recipe.GunTable;
import com.gregguns.recipe.ProductionTable;

/**
 * The tier progression checks of design 0021: what a player gets for climbing a tier never drops below the tier
 * before it. These are floors and orderings, not exact numbers, so a later retune that keeps the progression passes.
 */
// Spec: docs/design/0021-tier-balance-pass.md
class TierBalanceTest {

    /** GT's TierEU.RECIPE_* values, copied so the test needs no GT classes. */
    private static long eut(Tier tier) {
        switch (tier) {
            case STEAM:
                return 8;
            case LV:
                return 30;
            case MV:
                return 120;
            case HV:
                return 480;
            case EV:
                return 1_920;
            case IV:
                return 7_680;
            case LUV:
                return 30_720;
            default:
                return 122_880;
        }
    }

    /** Seconds of continuous fire in one full charge of an energy gun. */
    private static double fireSeconds(String name) {
        GunStats s = GunDefinitions.byName(name)
            .stats();
        return s.magazineSize() * s.fireRateTicks() / 20.0;
    }

    @Test
    void aLuvChargeOfTheAlienBlasterOutlastsTheIvDeatomizerAndBlaster() {
        assertTrue(
            GunDefinitions.byName("alien_blaster")
                .stats()
                .magazineSize()
                >= GunDefinitions.byName("deatomizer_pistol")
                    .stats()
                    .magazineSize());
        assertTrue(fireSeconds("alien_blaster") >= 8.0, "Alien Blaster fires " + fireSeconds("alien_blaster") + " s");
    }

    @Test
    void everyEnergyRifleFiresAtLeastFourSecondsOnAFullCharge() {
        for (GunDefinition d : GunDefinitions.ALL) {
            if (d.stats()
                .isEnergy() && d.tool() == null) {
                assertTrue(fireSeconds(d.name()) >= 4.0, d.name() + " fires " + fireSeconds(d.name()) + " s");
            }
        }
    }

    @Test
    void gunRecipeEuRisesWithTier() {
        Map<Tier, Long> min = new EnumMap<>(Tier.class);
        Map<Tier, Long> max = new EnumMap<>(Tier.class);
        for (ProductionTable.Row row : GunTable.ROWS) {
            long eu = (long) row.seconds() * 20L * eut(row.tier());
            min.merge(row.tier(), eu, Math::min);
            max.merge(row.tier(), eu, Math::max);
        }
        Tier prev = null;
        for (Tier t : new Tier[] { Tier.LV, Tier.MV, Tier.HV, Tier.EV, Tier.IV, Tier.LUV }) {
            if (!min.containsKey(t)) {
                continue;
            }
            if (prev != null) {
                assertTrue(min.get(t) > max.get(prev), t + " guns cost more EU than the cheapest-to-dearest " + prev);
            }
            prev = t;
        }
    }

    @Test
    void bestArmorProtectionRisesWithTier() {
        Map<Tier, Double> best = new EnumMap<>(Tier.class);
        for (SetDef set : ArmorCatalog.ALL) {
            best.merge(
                set.tier(),
                set.material()
                    .total(DamageType.PROJECTILE),
                Math::max);
        }
        double prev = 0;
        for (Tier t : new Tier[] { Tier.STEAM, Tier.LV, Tier.MV, Tier.EV, Tier.IV }) {
            assertTrue(best.get(t) >= prev, t + " best set protects at least as well as the tier below");
            prev = best.get(t);
        }
    }

    @Test
    void poweredSetsWalkAtLeastAsLongAsTheEvHevSuit() {
        double hev = ArmorCatalog.T3_HEV.power()
            .capacityUnits();
        assertTrue(
            ArmorCatalog.T3_POWER.power()
                .capacityUnits() >= hev);
        assertTrue(
            ArmorCatalog.T3_EXO.power()
                .capacityUnits()
                >= ArmorCatalog.T3_POWER.power()
                    .capacityUnits());
    }
}
