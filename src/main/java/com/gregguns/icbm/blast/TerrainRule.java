package com.gregguns.icbm.blast;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.icbm.explosive.BlastShape;

/**
 * Whether an ICBM blast may break terrain (D52): its own per-tier {@code icbm.terrainDamageT<n>} switch decides, and
 * {@code explosions.blockDamage}, which governs hand-held weapons, is not consulted (except for a launcher warhead,
 * which is hand-held: both switches must allow it). An NPC-caused blast still needs
 * {@code mobGriefing}. Claims and protection are applied later, through {@code ExplosionEvent.Detonate} and the break
 * guard. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (terrain); DECISIONS.md D52
public final class TerrainRule {

    private TerrainRule() {}

    /**
     * @param shape       the explosive's crater shape
     * @param tier        the explosive tier number, 1 to 4
     * @param byPlayer    true when a player caused the blast
     * @param mobGriefing the world's {@code mobGriefing} rule
     * @return true when the blast may break blocks
     */
    public static boolean breaks(BlastShape shape, int tier, boolean byPlayer, boolean mobGriefing) {
        return shape != BlastShape.NONE
            && ExplosionRules.mayBreakBlocks(IcbmConfig.terrainDamage(tier), byPlayer, mobGriefing);
    }

    /**
     * The rule for a blast fired from a hand-held launcher (D52, D154): the per-tier switch and
     * {@code explosions.blockDamage} must both allow it.
     *
     * @param handheld    true for a launcher warhead
     * @param blockDamage the {@code explosions.blockDamage} setting
     * @return {@link #breaks(BlastShape, int, boolean, boolean)} and, for a hand-held blast, the block-damage switch
     */
    public static boolean breaks(BlastShape shape, int tier, boolean byPlayer, boolean mobGriefing, boolean handheld,
        boolean blockDamage) {
        return breaks(shape, tier, byPlayer, mobGriefing) && (!handheld || blockDamage);
    }
}
