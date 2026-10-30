package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.item.ammo.AmmoCompatibility;

/** What an armory display chest holds for the gun above it. */
class ArmoryDisplayTest {

    @Test
    void aMagazineGunGetsItsLoadedMagazinesAndLooseRounds() {
        List<ArmoryDisplay.Stock> s = ArmoryDisplay.stockFor("gregguns:ak47");
        assertEquals(2, s.size());
        assertTrue(
            AmmoCompatibility.DEFAULT.gunsTaking(
                s.get(0)
                    .item())
                .contains("ak47"));
        assertTrue(
            s.get(1)
                .max() >= 32,
            "a lot of rounds");
    }

    @Test
    void aLooseRoundGunGetsItsRounds() {
        List<ArmoryDisplay.Stock> s = ArmoryDisplay.stockFor("gregguns:combat_shotgun");
        assertEquals(
            "ammo_shotgun_shell",
            s.get(0)
                .item());
    }

    @Test
    void anEnergyGunBurnsNoAmmoAndAnUnknownNameHoldsNothing() {
        assertTrue(
            ArmoryDisplay.stockFor("gregguns:laser_rifle")
                .isEmpty());
        assertTrue(
            ArmoryDisplay.stockFor("gregguns:no_such_gun")
                .isEmpty());
        assertTrue(
            ArmoryDisplay.stockFor(null)
                .isEmpty());
    }

    @Test
    void theGunOfADisplayIsAGunAndStableForItsCell() {
        for (int c = 4; c <= 12; c++) {
            String a = ArmoryDisplay.gunAt(c, 99L, 10, 20);
            assertEquals(a, ArmoryDisplay.gunAt(c, 99L, 10, 20));
            assertTrue(a == null || ArmoryDisplay.IS_GUN.test(a));
        }
    }
}
