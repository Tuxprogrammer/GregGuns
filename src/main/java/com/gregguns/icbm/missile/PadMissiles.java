package com.gregguns.icbm.missile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * The seam to the launch-control block (04-launch-control): ask what stands on a Galacticraft pad and launch it. Pad
 * coordinates are any block of the 3x3 pad area; the pad's top surface is the block's own Y. Server side for
 * {@link #launch}; {@link #status} also works on a client world, as far as the missile entity is tracked there.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (The pad API)
public final class PadMissiles {

    private PadMissiles() {}

    /**
     * @param world a world
     * @param padX  X of any block of the pad
     * @param padY  Y of the pad block
     * @param padZ  Z of any block of the pad
     * @return what stands there; {@link MissileStatus#NONE} for nothing
     */
    public static MissileStatus status(World world, int padX, int padY, int padZ) {
        EntityMissile m = find(world, padX, padY, padZ);
        return m == null ? MissileStatus.NONE : m.status();
    }

    /**
     * @param world   a world
     * @param padX    X of any block of the pad
     * @param padY    Y of the pad block
     * @param padZ    Z of any block of the pad
     * @param targetX the target X
     * @param targetZ the target Z
     * @return the largest launch range of the missile on the pad in blocks, or 0 when there is none
     */
    public static int maxRange(World world, int padX, int padY, int padZ) {
        EntityMissile m = find(world, padX, padY, padZ);
        return m == null ? 0 : m.maxRange();
    }

    /**
     * The blast radius the missile on the pad would have at a target, with the fuel bonus (D20), for the GUI.
     *
     * @return blocks; 0 when there is no missile
     */
    public static double expectedRadius(World world, int padX, int padY, int padZ, double targetX, double targetZ) {
        EntityMissile m = find(world, padX, padY, padZ);
        return m == null ? 0.0 : m.expectedRadius(targetX, targetZ);
    }

    /**
     * Launches the missile on the pad at x, z. Checks every rule (armed, fueled, range, in-flight cap) and returns why
     * it refused.
     *
     * @param world   the server world
     * @param padX    X of any block of the pad
     * @param padY    Y of the pad block
     * @param padZ    Z of any block of the pad
     * @param targetX the target X
     * @param targetZ the target Z
     * @param source  who fired it, may be null (redstone, a cover)
     * @return the outcome
     */
    public static LaunchResult launch(World world, int padX, int padY, int padZ, double targetX, double targetZ,
        EntityPlayer source) {
        if (world == null || world.isRemote || !finite(targetX) || !finite(targetZ)) {
            return LaunchResult.BAD_TARGET;
        }
        EntityMissile m = find(world, padX, padY, padZ);
        if (m == null) {
            return LaunchResult.NO_MISSILE;
        }
        return m.launch(targetX, targetZ, source);
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    /**
     * @return the unlaunched missile entity standing on that pad, or null
     */
    static EntityMissile find(World world, int padX, int padY, int padZ) {
        if (world == null) {
            return null;
        }
        net.minecraft.util.AxisAlignedBB box = net.minecraft.util.AxisAlignedBB
            .getBoundingBox(padX - 1.0, padY, padZ - 1.0, padX + 2.0, padY + 3.0, padZ + 2.0);
        for (Object o : world.getEntitiesWithinAABB(EntityMissile.class, box)) {
            EntityMissile m = (EntityMissile) o;
            if (!m.isDead && m.onPad()) {
                return m;
            }
        }
        return null;
    }
}
