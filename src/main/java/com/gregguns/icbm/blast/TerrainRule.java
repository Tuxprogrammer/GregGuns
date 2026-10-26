package com.gregguns.icbm.blast;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.icbm.explosive.BlastShape;

/**
 * Whether an ICBM blast may break terrain (D52): its own per-tier {@code icbm.terrainDamageT<n>} switch decides, and
 * {@code explosions.blockDamage}, which governs hand-held weapons, is not consulted. An NPC-caused blast still needs
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
}
