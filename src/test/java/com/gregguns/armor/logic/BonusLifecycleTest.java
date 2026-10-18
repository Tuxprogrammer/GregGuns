package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Equip, unequip, death, dimension change and logout, as the game side drives them. */
class BonusLifecycleTest {

    private static final BonusMap SCOUT = BonusMap.of(ArmorBonus.SPEED, 0.5, ArmorBonus.JUMP, 0.16);
    private final UUID player = UUID.randomUUID();

    @Test
    void equipAppliesOnceAndStaysQuietWhileNothingChanges() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        BonusLifecycle.Change first = l.update(player, SCOUT);
        assertTrue(first.changed());
        assertTrue(
            first.before()
                .isEmpty());
        assertEquals(SCOUT, first.now());
        assertFalse(
            l.update(player, SCOUT)
                .changed(),
            "an unchanged tick must not rewrite attributes");
    }

    @Test
    void unequipRemoves() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        l.update(player, SCOUT);
        BonusLifecycle.Change off = l.update(player, BonusMap.EMPTY);
        assertTrue(off.changed());
        assertEquals(SCOUT, off.before());
        assertTrue(
            l.applied(player)
                .isEmpty());
        assertEquals(0, l.size());
        assertFalse(
            l.update(player, BonusMap.EMPTY)
                .changed());
    }

    @Test
    void takingOnePieceOffChangesOnlyTheDifference() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        l.update(player, SCOUT);
        BonusLifecycle.Change c = l.update(player, BonusMap.of(ArmorBonus.SPEED, 0.375, ArmorBonus.JUMP, 0.14));
        assertTrue(c.changed());
        assertEquals(
            0.375,
            l.applied(player)
                .get(ArmorBonus.SPEED),
            1e-9);
    }

    @Test
    void deathLogoutAndDimensionChangeForgetEverything() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        l.update(player, SCOUT);
        BonusMap stripped = l.forget(player);
        assertEquals(SCOUT, stripped, "the caller gets what to strip");
        assertTrue(
            l.applied(player)
                .isEmpty());
        assertEquals(0, l.size());
        // The next tick re-applies whatever the player still wears.
        assertTrue(
            l.update(player, SCOUT)
                .changed());
    }

    @Test
    void forgettingAnUnknownPlayerIsHarmless() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        assertTrue(
            l.forget(player)
                .isEmpty());
    }

    @Test
    void playersAreIndependent() {
        BonusLifecycle<UUID> l = new BonusLifecycle<>();
        UUID other = UUID.randomUUID();
        l.update(player, SCOUT);
        l.update(other, BonusMap.of(ArmorBonus.BREAK_SPEED, 0.2));
        l.forget(player);
        assertEquals(
            0.2,
            l.applied(other)
                .get(ArmorBonus.BREAK_SPEED),
            1e-9);
        assertEquals(1, l.size());
    }
}
