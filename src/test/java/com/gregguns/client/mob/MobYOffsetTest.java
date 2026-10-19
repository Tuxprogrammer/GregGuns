package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The offset is in blocks after scaling, so the translation in scaled model space is offset / scale. */
class MobYOffsetTest {

    @Test
    void superMutantBasicLiftsByItsOffsetInBlocks() {
        float t = MobYOffset.scaledTranslation(0.55F, 1.35F);
        assertEquals(0.55F, t * 1.35F, 1.0e-6F);
    }

    @Test
    void zeroOffsetIsNoTranslation() {
        assertEquals(0.0F, MobYOffset.scaledTranslation(0.0F, 2.0F));
    }

    @Test
    void negativeOffsetSinksTheModel() {
        assertEquals(-0.5F, MobYOffset.scaledTranslation(-1.0F, 2.0F));
    }
}
