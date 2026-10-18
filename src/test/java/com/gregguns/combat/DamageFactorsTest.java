package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.combat.DamageFactors.Attacker;
import com.gregguns.combat.DamageFactors.Settings;

/** Selection of the Techguns damage factors and penetration multipliers (damage-system section 5.5). */
class DamageFactorsTest {

    /** Distinct values so a wrong selection shows. */
    private static final Settings S = new Settings(0.5, 0.25, 2.0, 0.4, 0.8);

    @Test
    void playerAgainstPlayerUsesPvpFactorOnlyWhenPvpIsOn() {
        assertEquals(0.5, DamageFactors.damageFactor(S, Attacker.PLAYER, true, true), 1e-9);
        assertEquals(0.0, DamageFactors.damageFactor(S, Attacker.PLAYER, true, false), 1e-9);
    }

    @Test
    void turretAgainstPlayerUsesTurretFactorRegardlessOfPvp() {
        assertEquals(0.25, DamageFactors.damageFactor(S, Attacker.TURRET, true, true), 1e-9);
        assertEquals(0.25, DamageFactors.damageFactor(S, Attacker.TURRET, true, false), 1e-9);
    }

    @Test
    void otherAttackersAgainstPlayersUseTheNpcFactor() {
        assertEquals(2.0, DamageFactors.damageFactor(S, Attacker.NPC, true, true), 1e-9);
    }

    @Test
    void playerAgainstNonPlayerIsUnscaled() {
        assertEquals(1.0, DamageFactors.damageFactor(S, Attacker.PLAYER, false, false), 1e-9);
        assertEquals(1.0, DamageFactors.damageFactor(S, Attacker.PLAYER, false, true), 1e-9);
    }

    @Test
    void npcAndTurretAgainstNonPlayersUseTheNpcFactor() {
        assertEquals(2.0, DamageFactors.damageFactor(S, Attacker.NPC, false, true), 1e-9);
        assertEquals(2.0, DamageFactors.damageFactor(S, Attacker.TURRET, false, true), 1e-9);
    }

    @Test
    void negativeSettingsNeverProduceNegativeDamage() {
        Settings bad = new Settings(-1, -1, -1, -1, -1);
        assertEquals(0.0, DamageFactors.damageFactor(bad, Attacker.NPC, true, true), 1e-9);
        assertEquals(0.0, DamageFactors.penetrationScale(bad, Attacker.PLAYER, true), 1e-9);
    }

    @Test
    void penetrationScaleAppliesOnlyAgainstPlayers() {
        assertEquals(0.8, DamageFactors.penetrationScale(S, Attacker.PLAYER, true), 1e-9);
        assertEquals(0.4, DamageFactors.penetrationScale(S, Attacker.NPC, true), 1e-9);
        assertEquals(0.4, DamageFactors.penetrationScale(S, Attacker.TURRET, true), 1e-9);
        assertEquals(1.0, DamageFactors.penetrationScale(S, Attacker.PLAYER, false), 1e-9);
        assertEquals(1.0, DamageFactors.penetrationScale(S, Attacker.NPC, false), 1e-9);
    }

    @Test
    void scaleFeedsEffectivePenetration() {
        double scale = DamageFactors.penetrationScale(S, Attacker.PLAYER, true);
        assertEquals(0.4 * 0.8, DamageModel.effectivePenetration(0.4, scale, 0.0), 1e-9);
        assertEquals(0.0, DamageModel.effectivePenetration(0.4, 0.0, 0.0), 1e-9);
    }

    @Test
    void anUnknownShooterCountsAsAnNpc() {
        assertEquals(Attacker.NPC, DamageSources.attackerOf(null));
    }
}
