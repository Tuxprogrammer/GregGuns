package com.gregguns.combat;

/**
 * The damage-factor and penetration-multiplier selection of the Techguns damage system, as pure logic. Which factor
 * applies depends on who shoots and who is hit; the values come in as {@link Settings} so this class never reads
 * config. {@link DamageSources#hit} builds the settings from {@code DamageFactorsConfig}.
 *
 * <p>
 * Selection (Techguns {@code getDamageFactor}, damage-system section 5.5):
 * <ul>
 * <li>player against player: the PvP factor, or 0 when server PvP is off;</li>
 * <li>turret against a player: the turret factor;</li>
 * <li>any other attacker (NPC, unknown, {@code null}) against a player: the NPC factor;</li>
 * <li>player against a non-player: 1;</li>
 * <li>turret or NPC against a non-player: the NPC factor.</li>
 * </ul>
 * The penetration multiplier applies only against players: the PvP multiplier for a player attacker, the NPC-to-player
 * multiplier for everything else.
 *
 * <p>
 * Hook for M6 and M7: turrets mark themselves with {@link TurretAttacker}; any other non-player shooter counts as an
 * NPC. Pass the shooter to {@link DamageSources#hit} and the factors apply there; do not also scale the damage in
 * {@code ProjectileLauncher#launch} (its {@code damageScale} stays 1 for those callers).
 */
// Spec: docs/techguns-spec/02-guns/damage-system.md sections 5.3 (calcPenetration) and 5.5
public final class DamageFactors {

    /** Who is shooting. */
    public enum Attacker {
        /** A player. */
        PLAYER,
        /** A turret (M6). */
        TURRET,
        /** Any other attacker, including an unknown one. */
        NPC
    }

    /** The five configured values. */
    public static final class Settings {

        private final double pvp;
        private final double turretToPlayer;
        private final double npc;
        private final double penetrationNpcToPlayer;
        private final double penetrationPvp;

        /**
         * @param pvp                    damage factor, player against player
         * @param turretToPlayer         damage factor, turret against a player
         * @param npc                    damage factor, other attackers
         * @param penetrationNpcToPlayer penetration multiplier, non-players against players
         * @param penetrationPvp         penetration multiplier, players against players
         */
        public Settings(double pvp, double turretToPlayer, double npc, double penetrationNpcToPlayer,
            double penetrationPvp) {
            this.pvp = pvp;
            this.turretToPlayer = turretToPlayer;
            this.npc = npc;
            this.penetrationNpcToPlayer = penetrationNpcToPlayer;
            this.penetrationPvp = penetrationPvp;
        }
    }

    private DamageFactors() {}

    /**
     * @param settings       the configured values
     * @param attacker       who shoots
     * @param targetIsPlayer whether the victim is a player
     * @param pvpEnabled     whether the server allows PvP (only read for player against player)
     * @return the multiplier for the hit's damage, never negative
     */
    public static double damageFactor(Settings settings, Attacker attacker, boolean targetIsPlayer,
        boolean pvpEnabled) {
        double factor;
        if (targetIsPlayer) {
            switch (attacker) {
                case PLAYER:
                    factor = pvpEnabled ? settings.pvp : 0.0;
                    break;
                case TURRET:
                    factor = settings.turretToPlayer;
                    break;
                default:
                    factor = settings.npc;
                    break;
            }
        } else {
            factor = attacker == Attacker.PLAYER ? 1.0 : settings.npc;
        }
        return Math.max(0.0, factor);
    }

    /**
     * @param settings       the configured values
     * @param attacker       who shoots
     * @param targetIsPlayer whether the victim is a player
     * @return the {@code scale} for {@link DamageModel#effectivePenetration}; 1 against non-players
     */
    public static double penetrationScale(Settings settings, Attacker attacker, boolean targetIsPlayer) {
        if (!targetIsPlayer) {
            return 1.0;
        }
        return Math.max(0.0, attacker == Attacker.PLAYER ? settings.penetrationPvp : settings.penetrationNpcToPlayer);
    }
}
