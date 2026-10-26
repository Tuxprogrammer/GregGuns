package com.gregguns.icbm.explosive;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * The detonate entry point: every ICBM blast, from a warhead block, a missile or a command, starts here. Server
 * side. The engine behind it is filled in by the blast-engine commits; this commit fixes the signature.
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
     * @return true when the blast was accepted; false on a client world, a cancelled {@code ExplosionEvent.Start},
     *         an explosive above {@code icbm.maxBlastTier} or a full blast queue
     */
    public static boolean detonate(World world, double x, double y, double z, ExplosiveDef explosive, int fill,
        double bonusRadius, EntityLivingBase source) {
        return false; // engine arrives in the next commits
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
