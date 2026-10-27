package com.gregguns.explosion.nuclear;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gregguns.core.config.ExplosionsConfig;

import gregtech.api.util.GTUtility;

/**
 * Exposure (D21): about once a second, a player standing on irradiated earth (an irradiated block in the 3x3 under
 * their feet or the layer below that) gets GT's radioactivity effect.
 * GT's own check skips anyone in full hazmat for the radiological hazard (GT hazmat, GregGuns armor that declares it,
 * the Hazmat enchantment, any hazard protector), so protection is whatever GT says it is, and skips undead and
 * arthropods. The check is a fixed handful of block reads per player, never per block of the fallout. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
// Source: GTUtility.applyRadioactivity
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2018
public final class RadiationExposure {

    /** Ticks between checks. */
    public static final int PERIOD_TICKS = 20;

    private RadiationExposure() {}

    /** @param world a server world with fallout in it */
    static void check(World world) {
        if (FalloutBlocks.irradiated() == null || ExplosionsConfig.falloutRadiationLevel < 1) {
            return;
        }
        for (Object o : world.playerEntities) {
            expose(world, (EntityPlayer) o);
        }
    }

    /**
     * One exposure check for one player.
     *
     * @param world  the player's world
     * @param player a player
     * @return true when GT's radioactivity was applied (false for creative players, players not on irradiated earth and
     *         anyone GT's own check protects)
     */
    public static boolean expose(World world, EntityPlayer player) {
        if (FalloutBlocks.irradiated() == null || ExplosionsConfig.falloutRadiationLevel < 1
            || FalloutEngine.exempt(player)
            || !touching(world, player)) {
            return false;
        }
        return GTUtility.applyRadioactivity(player, ExplosionsConfig.falloutRadiationLevel, 1);
    }

    /**
     * @return true when the player stands on an irradiated block: the 3x3 under their feet and the layer below (18
     *         block reads)
     */
    public static boolean touching(World world, EntityPlayer player) {
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.boundingBox.minY - 0.01);
        int pz = (int) Math.floor(player.posZ);
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (world.getBlock(px + dx, py + dy, pz + dz) == FalloutBlocks.irradiated()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
