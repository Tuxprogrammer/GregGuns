package com.gregguns.item.melee;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeleeMathTest {

    @Test
    void knifeAndCrowbarBonusesMatchTheSheets() {
        assertEquals(6.5F, MeleeMath.knifeBonus());
        assertEquals(4.5F, MeleeMath.crowbarBonus());
        // With the player's own 1: 7.5 and 5.5 per hit.
        assertEquals(7.5F, MeleeMath.HAND_DAMAGE + MeleeMath.knifeBonus());
        assertEquals(5.5F, MeleeMath.HAND_DAMAGE + MeleeMath.crowbarBonus());
    }

    @Test
    void criticalMultipliesTheAttributeBeforeTheEnchantBonus() {
        assertEquals(7.5F, MeleeMath.damage(7.5F, 0.0F, false));
        assertEquals(11.25F + 2.0F, MeleeMath.damage(7.5F, 2.0F, true));
        assertEquals(2.0F, MeleeMath.damage(0.0F, 2.0F, true));
    }

    @Test
    void sprintingAddsOneKnockback() {
        assertEquals(1, MeleeMath.knockback(0, true));
        assertEquals(2, MeleeMath.knockback(2, false));
    }
}
