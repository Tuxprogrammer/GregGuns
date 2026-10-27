package com.gregguns.icbm.explosive;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * The detonate entry point: every ICBM blast, from a warhead block, a missile or a command, starts here. Server
 * side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md
public final class IcbmBlasts {

    private IcbmBlasts() {}

    /**
     * Starts a blast.
     *
     * @param world       the server world
     * @param x           center X
     * @param y           center Y
     * @param z           center Z
     * @param explosive   the explosive
     * @param fill        the warhead fill (clamped to the explosive's range)
     * @param bonusRadius extra radius from unburned fuel (D20), 0 for none, never negative
     * @param source      who set it off (a player, or an NPC), or {@code null}
     * @return true when the blast was accepted; false on a client world, a canceled {@code ExplosionEvent.Start},
     *         an explosive above {@code icbm.maxBlastTier} or a full blast queue
     */
    public static boolean detonate(World world, double x, double y, double z, ExplosiveDef explosive, int fill,
        double bonusRadius, EntityLivingBase source) {
        return com.gregguns.icbm.blast.BlastEngine.instance()
            .start(world, x, y, z, explosive, fill, bonusRadius, source);
    }

    /**
     * Starts a launcher-warhead blast (D154): hand-held, so {@code explosions.blockDamage} also governs its terrain,
     * and its center damage is capped.
     *
     * @param damageCap the most center damage, half hearts
     * @return see {@link #detonate(World, double, double, double, ExplosiveDef, int, double, EntityLivingBase)}
     */
    public static boolean detonateHandheld(World world, double x, double y, double z, ExplosiveDef explosive, int fill,
        EntityLivingBase source, double damageCap) {
        return com.gregguns.icbm.blast.BlastEngine.instance()
            .start(world, x, y, z, explosive, fill, 0.0, source, damageCap, true);
    }

    /**
     * Starts a blast with no fuel bonus.
     *
     * @return see {@link #detonate(World, double, double, double, ExplosiveDef, int, double, EntityLivingBase)}
     */
    public static boolean detonate(World world, double x, double y, double z, ExplosiveDef explosive, int fill,
        EntityLivingBase source) {
        return detonate(world, x, y, z, explosive, fill, 0.0, source);
    }
}
