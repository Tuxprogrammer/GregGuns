package com.gregguns.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BlastFxSelectorTest {

    @Test
    void aNukeSizeBlastGetsTheNukeList() {
        assertEquals("NukeExplosion", BlastFxSelector.listFor(ExplosionRules.NUKE_FX_MIN_SIZE));
        assertEquals("NukeExplosion", BlastFxSelector.listFor(10F));
    }

    @Test
    void aSmallerBlastGetsTheRocketList() {
        assertEquals("RocketExplosion", BlastFxSelector.listFor(ExplosionRules.NUKE_FX_MIN_SIZE - 0.1F));
        assertEquals("RocketExplosion", BlastFxSelector.listFor(1F));
    }
}
