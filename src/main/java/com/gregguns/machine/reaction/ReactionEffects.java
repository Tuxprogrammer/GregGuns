package com.gregguns.machine.reaction;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.Pair;

import com.gregguns.core.config.MachinesConfig;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GlassTier;
import gregtech.common.pollution.Pollution;

/**
 * Applies a failed reaction's {@link ReactionRisk} to the world through GT: pollution burst, GT explosions, and
 * radiation through GT's hazard system. Server side only.
 *
 * <p>
 * Nothing here calls {@code explodeMultiblock()} (GGS-08-24): that blasts every hatch at {@code V[8]}.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (GT5-Unofficial: risks, radiation and
// explosions; pollution), GGS-08-21, GGS-08-24
public final class ReactionEffects {

    private ReactionEffects() {}

    /**
     * @param risk a risk
     * @return the GT radiation level for it, from the config; 0 for a non-radiation risk
     */
    public static int radiationLevel(ReactionRisk risk) {
        switch (risk) {
            case RAD_LOW:
                return MachinesConfig.radiationLevelLow;
            case RAD_MEDIUM:
                return MachinesConfig.radiationLevelMedium;
            case RAD_HIGH:
                return MachinesConfig.radiationLevelHigh;
            default:
                return 0;
        }
    }

    /**
     * Applies the risk once.
     *
     * @param machine the controller's tile entity
     * @param risk    the failed run's risk
     * @param retort  whether the machine is the Retort: its glass breaks in an explosion and a failure pollutes
     */
    public static void applyFailure(IGregTechTileEntity machine, ReactionRisk risk, boolean retort) {
        if (machine == null || machine.getWorld() == null) {
            return;
        }
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/pollution/Pollution.java#L340
        if (retort && MachinesConfig.reactionFailurePollution > 0) {
            Pollution.addPollution(machine, MachinesConfig.reactionFailurePollution);
        }
        if (risk.isRadiation()) {
            irradiate(machine, radiationLevel(risk));
        } else if (risk.isExplosion()) {
            int voltageIndex = risk == ReactionRisk.EXPLOSION_LOW ? MachinesConfig.explosionLowVoltageIndex
                : MachinesConfig.explosionMediumVoltageIndex;
            World world = machine.getWorld();
            int x = machine.getXCoord();
            int y = machine.getYCoord();
            int z = machine.getZCoord();
            if (retort) {
                shatterGlass(world, x, y, z, risk == ReactionRisk.EXPLOSION_LOW ? 3 : 4);
            }
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1340-L1366
            machine.doExplosion(GTValues.V[voltageIndex]);
        }
    }

    /**
     * Irradiates every living entity within the configured radius, through GT so hazmat protects.
     *
     * @param machine the controller's tile entity
     * @param level   the GT radiation level
     */
    @SuppressWarnings("unchecked")
    public static void irradiate(IGregTechTileEntity machine, int level) {
        double r = MachinesConfig.reactionRadiationRadius;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            machine.getXCoord() - r,
            machine.getYCoord() - r,
            machine.getZCoord() - r,
            machine.getXCoord() + 1 + r,
            machine.getYCoord() + 1 + r,
            machine.getZCoord() + 1 + r);
        List<EntityLivingBase> entities = machine.getWorld()
            .getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase entity : entities) {
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2018-L2040
            GTUtility.applyRadioactivity(entity, level, 1);
        }
    }

    /** Removes the glass blocks within {@code radius} of the controller, a cube centered on it. */
    private static void shatterGlass(World world, int cx, int cy, int cz, int radius) {
        List<Pair<Block, Integer>> glass = GlassTier.getGlassList();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlock(cx + dx, cy + dy, cz + dz);
                    int meta = world.getBlockMetadata(cx + dx, cy + dy, cz + dz);
                    if (glass.contains(Pair.of(block, meta))) {
                        world.setBlockToAir(cx + dx, cy + dy, cz + dz);
                    }
                }
            }
        }
    }
}
