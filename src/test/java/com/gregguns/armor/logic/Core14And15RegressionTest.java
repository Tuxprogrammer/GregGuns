package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regressions for CORE-14 (step height forcibly clamped, also ARM-07) and CORE-15 (Night Vision toggle-off strips
 * potion Night Vision, also ARM-29). See docs/techguns-spec/BUGS.md.
 */
class Core14And15RegressionTest {

    // ---- CORE-14 -------------------------------------------------------------------------------------------------

    @Test
    void doesNotReproduceCORE14_anotherModsStepHeightIsNeverLowered() {
        // Techguns reset any step height above 0.5 to 0.5 every tick while its own assist was off.
        StepHeightPolicy.State state = StepHeightPolicy.State.UNTOUCHED;
        for (int tick = 0; tick < 5; tick++) {
            StepHeightPolicy.Decision d = StepHeightPolicy.decide(1.25, false, state);
            assertFalse(d.set(), "no write while armor grants no step assist");
            state = d.state();
        }
    }

    @Test
    void doesNotReproduceCORE14_equippingOverAHigherStepHeightLeavesItAlone() {
        StepHeightPolicy.Decision d = StepHeightPolicy.decide(1.5, true, StepHeightPolicy.State.UNTOUCHED);
        assertFalse(d.set());
        assertFalse(
            d.state()
                .owned(),
            "armor does not take ownership of a height it did not set");
        // And unequipping afterwards does not reset it either.
        assertFalse(
            StepHeightPolicy.decide(1.5, false, d.state())
                .set());
    }

    @Test
    void stepAssistRaisesAndRestoresExactlyWhatItReplaced() {
        StepHeightPolicy.Decision on = StepHeightPolicy.decide(0.5, true, StepHeightPolicy.State.UNTOUCHED);
        assertTrue(on.set());
        assertEquals(1.0, on.height());
        assertTrue(
            on.state()
                .owned());
        assertFalse(
            StepHeightPolicy.decide(1.0, true, on.state())
                .set(),
            "steady state writes nothing");
        StepHeightPolicy.Decision off = StepHeightPolicy.decide(1.0, false, on.state());
        assertTrue(off.set());
        assertEquals(0.5, off.height());
        assertFalse(
            off.state()
                .owned());
    }

    @Test
    void stepAssistRestoresTheOriginalNotAHardcodedDefault() {
        StepHeightPolicy.Decision on = StepHeightPolicy.decide(0.6, true, StepHeightPolicy.State.UNTOUCHED);
        StepHeightPolicy.Decision off = StepHeightPolicy.decide(1.0, false, on.state());
        assertEquals(0.6, off.height());
    }

    @Test
    void stepAssistLetsGoWhenAnotherSourceChangedTheHeightMeanwhile() {
        StepHeightPolicy.Decision on = StepHeightPolicy.decide(0.5, true, StepHeightPolicy.State.UNTOUCHED);
        // Another mod raised it to 2.0 while worn: unequipping must not drag it back to 0.5.
        StepHeightPolicy.Decision off = StepHeightPolicy.decide(2.0, false, on.state());
        assertFalse(off.set());
        assertFalse(
            off.state()
                .owned());
    }

    @Test
    void stepAssistRaisesAgainIfSomethingLoweredIt() {
        StepHeightPolicy.Decision on = StepHeightPolicy.decide(0.5, true, StepHeightPolicy.State.UNTOUCHED);
        StepHeightPolicy.Decision again = StepHeightPolicy.decide(0.5, true, on.state());
        assertTrue(again.set());
        assertEquals(1.0, again.height());
        assertEquals(
            0.5,
            StepHeightPolicy.decide(1.0, false, again.state())
                .height());
    }

    // ---- CORE-15 -------------------------------------------------------------------------------------------------

    @Test
    void doesNotReproduceCORE15_aBrewedPotionSurvivesTheToggleGoingOff() {
        // A 3000-tick potion, armor never granted anything.
        NightVisionPolicy.Decision d = NightVisionPolicy.decide(false, false, 3000);
        assertEquals(NightVisionPolicy.Action.NONE, d.action());
        assertFalse(d.owned());
    }

    @Test
    void doesNotReproduceCORE15_aPotionThatOutlastsOurGrantIsNotRemoved() {
        // Armor granted NV last tick, then the player drank a long potion. Removal must leave it alone.
        NightVisionPolicy.Decision d = NightVisionPolicy.decide(false, true, 3000);
        assertEquals(NightVisionPolicy.Action.NONE, d.action());
        assertFalse(d.owned());
    }

    @Test
    void doesNotReproduceCORE15_armorDoesNotRefreshOrShortenAForeignPotion() {
        NightVisionPolicy.Decision d = NightVisionPolicy.decide(true, false, 3000);
        assertEquals(NightVisionPolicy.Action.NONE, d.action(), "no refresh over a foreign effect");
        assertFalse(d.owned());
    }

    @Test
    void armorGrantsRefreshesAndRemovesItsOwnEffect() {
        NightVisionPolicy.Decision grant = NightVisionPolicy.decide(true, false, -1);
        assertEquals(NightVisionPolicy.Action.GRANT, grant.action());
        assertTrue(grant.owned());
        NightVisionPolicy.Decision refresh = NightVisionPolicy.decide(true, true, 200);
        assertEquals(NightVisionPolicy.Action.GRANT, refresh.action());
        assertTrue(refresh.owned());
        NightVisionPolicy.Decision off = NightVisionPolicy.decide(false, true, 150);
        assertEquals(NightVisionPolicy.Action.REMOVE, off.action());
        assertFalse(off.owned());
    }

    @Test
    void nothingHappensWhenNothingIsWantedAndNothingIsOwned() {
        assertEquals(
            NightVisionPolicy.Action.NONE,
            NightVisionPolicy.decide(false, false, -1)
                .action());
    }
}
