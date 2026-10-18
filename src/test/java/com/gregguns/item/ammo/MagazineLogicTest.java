package com.gregguns.item.ammo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class MagazineLogicTest {

    private static final MagazineType PISTOL = AmmoTypes.NINE_MM_PISTOL;

    @Test
    void nineMmPistolMagazineTakesThreeRounds() {
        assertEquals(3, PISTOL.roundsPerMagazine());
        assertEquals(
            "9mm",
            PISTOL.ammo()
                .id());
        assertEquals(Tier.LV, PISTOL.tier());
    }

    @Test
    void fillableIsLimitedByTheScarcerInput() {
        assertEquals(2, MagazineLogic.fillable(PISTOL, 2, 100));
        assertEquals(3, MagazineLogic.fillable(PISTOL, 10, 10));
        assertEquals(0, MagazineLogic.fillable(PISTOL, 10, 2));
    }

    @Test
    void negativeCountsAreTreatedAsZero() {
        assertEquals(0, MagazineLogic.fillable(PISTOL, -1, 9));
        assertEquals(0, MagazineLogic.fillable(PISTOL, 4, -9));
        assertEquals(0, MagazineLogic.roundsConsumed(PISTOL, -2));
    }

    @Test
    void fillThenUnloadReturnsTheSameRounds() {
        int made = MagazineLogic.fillable(PISTOL, 4, 12);
        assertEquals(12, MagazineLogic.roundsConsumed(PISTOL, made));
        assertEquals(12, MagazineLogic.roundsReturned(PISTOL, made));
    }

    @Test
    void doesNotReproduceITEM07_partialFitNeverDuplicatesAmmo() {
        // 5 rounds produced, room for 2: the caller must drop exactly the other 3.
        int fits = MagazineLogic.returnedToInventory(62, 5, 64);
        assertEquals(2, fits);
        assertEquals(0, MagazineLogic.returnedToInventory(64, 5, 64));
        assertEquals(0, MagazineLogic.returnedToInventory(70, 5, 64));
    }

    @Test
    void magazinesMergeOnlyWithTheirOwnFamily() {
        assertTrue(MagazineLogic.canMerge(PISTOL, PISTOL));
        MagazineType other = new MagazineType("other", AmmoTypes.NINE_MM, "a", "b", 2, Tier.MV);
        assertFalse(MagazineLogic.canMerge(PISTOL, other));
    }

    @Test
    void registryResolvesByIdAndRejectsBadIds() {
        assertEquals(AmmoTypes.NINE_MM, AmmoTypes.ammo("9mm"));
        assertEquals(PISTOL, AmmoTypes.magazine("9mm_pistol"));
        assertNull(AmmoTypes.ammo("nope"));
        assertNotNull(
            AmmoTypes.allMagazines()
                .get("9mm_pistol"));
        assertThrows(IllegalArgumentException.class, () -> new AmmoType("9MM", "x", Tier.LV));
        assertThrows(
            IllegalArgumentException.class,
            () -> new MagazineType("m", AmmoTypes.NINE_MM, "a", "b", 0, Tier.LV));
    }
}
