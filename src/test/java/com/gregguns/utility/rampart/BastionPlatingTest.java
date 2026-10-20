package com.gregguns.utility.rampart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.TurretPlate;

/** GGS-08-78, GGS-09-35 and GGS-09-38: Bastion Plating metas, armor and recipes. */
class BastionPlatingTest {

    @Test
    void thePlatingsAreMetasSevenToTenInOrder() {
        int meta = 7;
        for (BastionPlating p : BastionPlating.values()) {
            assertEquals(meta++, p.meta(), p.name());
        }
    }

    @Test
    void armorValuesAreFiveTenFifteenTwenty() {
        assertEquals(5, BastionPlating.IRON.armorValue());
        assertEquals(10, BastionPlating.STEEL.armorValue());
        assertEquals(15, BastionPlating.ALUMINUM.armorValue());
        assertEquals(20, BastionPlating.CARBON.armorValue());
    }

    @Test
    void eachPlatingConsumesItsMatchingTurretPlate() {
        assertSame(TurretPlate.IRON, BastionPlating.IRON.plate());
        assertSame(TurretPlate.STEEL, BastionPlating.STEEL.plate());
        assertSame(TurretPlate.ALUMINUM, BastionPlating.ALUMINUM.plate());
        assertSame(TurretPlate.CARBON, BastionPlating.CARBON.plate());
    }

    @Test
    void theArmorOfAWallIsTheLowestPlatingValue() {
        // GGS-08-78
        assertEquals(
            5,
            BastionPlating.lowestArmor(
                BastionPlating.lowest(10, 9, 7, 8)
                    .meta()));
        assertEquals(
            10,
            BastionPlating.lowest(8, 9, 10)
                .armorValue());
        assertSame(BastionPlating.CARBON, BastionPlating.lowest(10, 10));
    }

    @Test
    void metasThatAreNotPlatingAreIgnoredAndAnEmptyWallHasNoArmor() {
        assertNull(BastionPlating.lowest());
        assertNull(BastionPlating.lowest(0, 3, 6, 11));
        assertSame(BastionPlating.STEEL, BastionPlating.lowest(3, 8, 0));
        assertEquals(0, BastionPlating.lowestArmor(Integer.MAX_VALUE));
        assertEquals(0, BastionPlating.lowestArmor(6));
    }

    @Test
    void oneAssemblerRunMakesFourBlocksFromOnePlate() {
        // GGS-09-35
        assertEquals(4, BastionPlating.YIELD);
        assertEquals(2, BastionPlating.CONCRETE);
        assertEquals(200, BastionPlating.DURATION_TICKS);
    }

    @Test
    void recipesRunAtTheirTierAndNeverAboveIt() {
        // GGS-09-38: the voltage is the plating tier, never above it, never a lower tier voltage.
        for (BastionPlating p : BastionPlating.values()) {
            assertTrue(
                p.recipeEut() <= p.recipeTier()
                    .voltage(),
                p.name());
            assertTrue(
                p.recipeEut() > p.recipeTier()
                    .voltage() / 4,
                p.name() + " is not a lower tier recipe");
            assertTrue(
                p == BastionPlating.IRON || p.recipeTier()
                    .voltageIndex()
                    >= p.plate()
                        .tier()
                        .voltageIndex(),
                p.name() + " must be made at least at its plate tier");
        }
    }

    @Test
    void theCircuitsAreOneToThreeAndTenForCarbon() {
        // Carbon is on 10, not 4: GT's Concrete row (one concrete, circuit 4) is a subset of its inputs.
        assertEquals(1, BastionPlating.IRON.circuit());
        assertEquals(2, BastionPlating.STEEL.circuit());
        assertEquals(3, BastionPlating.ALUMINUM.circuit());
        assertEquals(10, BastionPlating.CARBON.circuit());
    }
}
