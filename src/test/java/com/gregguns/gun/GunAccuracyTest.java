package com.gregguns.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;

/**
 * The armor accuracy bonus a shot uses ({@link GunAccuracy}) and its effect on spread: the bonus is zero without the
 * armor module, clamped to a fraction, and shrinks every offset of a shot by exactly that fraction.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 5
class GunAccuracyTest {

    @Test
    void theBonusReadsZeroWhenTheArmorModuleIsOff() {
        assertEquals(0.0, GunAccuracy.effectiveBonus(false, 0.3), 0.0);
        assertEquals(0.0, GunAccuracy.effectiveBonus(false, 5.0), 0.0);
    }

    @Test
    void theBonusIsAFractionClampedToZeroAndOne() {
        assertEquals(0.2, GunAccuracy.effectiveBonus(true, 0.2), 1e-12);
        assertEquals(0.0, GunAccuracy.effectiveBonus(true, -0.4), 0.0);
        assertEquals(1.0, GunAccuracy.effectiveBonus(true, 7.0), 0.0);
        assertEquals(0.0, GunAccuracy.effectiveBonus(true, Double.NaN), 0.0);
        assertEquals(0.0, GunAccuracy.effectiveBonus(true, Double.POSITIVE_INFINITY), 0.0);
    }

    @Test
    void aBonusShrinksTheSpreadOfTheBulletAndThePellets() {
        GunStats shotgun = SawedOffDef.STATS;
        ShotContext plain = ShotContext.player(false, 0.0);
        ShotContext armored = ShotContext.player(false, 0.25);
        assertEquals(SpreadModel.mainSpread(shotgun, plain) * 0.75, SpreadModel.mainSpread(shotgun, armored), 1e-12);
        assertEquals(
            SpreadModel.pelletSpread(shotgun, plain) * 0.75,
            SpreadModel.pelletSpread(shotgun, armored),
            1e-12);
    }

    @Test
    void theSameSeededShotsLandExactlyTheBonusTighter() {
        GunStats pistol = PistolDef.STATS;
        double plain = meanOffset(pistol, ShotContext.player(false, 0.0));
        double armored = meanOffset(pistol, ShotContext.player(false, 0.2));
        assertTrue(armored < plain);
        assertEquals(0.8, armored / plain, 1e-9);
    }

    private static double meanOffset(GunStats stats, ShotContext context) {
        Random random = new Random(11L);
        double sum = 0.0;
        int n = 0;
        for (int i = 0; i < 500; i++) {
            for (ShotOffset offset : SpreadModel.roll(random, stats, context, 0)) {
                sum += Math.abs(offset.yawDegrees()) + Math.abs(offset.pitchDegrees());
                n += 2;
            }
        }
        return sum / n;
    }
}
