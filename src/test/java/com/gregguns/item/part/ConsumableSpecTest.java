package com.gregguns.item.part;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConsumableSpecTest {

    /** Techguns' medkit sheet: the use sound and the arm swing every 7 ticks while in use. */
    @Test
    void feedbackFallsEverySevenTicksOfEveryConsumable() {
        for (ConsumableSpec spec : new ConsumableSpec[] { ConsumableSpec.BANDAGES, ConsumableSpec.MEDKIT }) {
            int ticks = 0;
            for (int left = spec.useTicks(); left > 0; left--) {
                if (ConsumableSpec.isFeedbackTick(left)) ticks++;
            }
            assertEquals(spec.useTicks() / ConsumableSpec.FEEDBACK_PERIOD, ticks);
            assertTrue(ticks >= 7, "a use is heard several times, not once");
        }
        assertFalse(ConsumableSpec.isFeedbackTick(0));
        assertTrue(ConsumableSpec.isFeedbackTick(7));
        assertFalse(ConsumableSpec.isFeedbackTick(8));
    }

    @Test
    void theFullHealthNoticeWaitsOutItsCooldown() {
        assertFalse(ConsumableSpec.noticeDue(100, 100));
        assertFalse(ConsumableSpec.noticeDue(100, 100 + ConsumableSpec.NOTICE_COOLDOWN - 1));
        assertTrue(ConsumableSpec.noticeDue(100, 100 + ConsumableSpec.NOTICE_COOLDOWN));
        assertTrue(ConsumableSpec.noticeDue(100, 50), "a clock that went back never mutes the notice");
    }

    @Test
    void onlyAHurtPlayerCanUseOne() {
        assertTrue(ConsumableSpec.canUse(19.5F, 20F));
        assertFalse(ConsumableSpec.canUse(20F, 20F));
    }
}
