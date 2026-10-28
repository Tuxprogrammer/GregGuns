package com.gregguns.icbm.defense;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * What a radar controller does to keep exactly one {@link EntityRadarDish} (D125): spawn it when the structure is
 * formed, set it spinning or still, and remove it when the structure is gone. The dish stands on the structure's top
 * plane, {@code up} blocks above the controller block's floor and in the middle of the footprint, which is one block
 * behind the controller. The entity is never saved, so a reload makes a new one and this class looks for a live one
 * that already serves the controller before it spawns another. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
final class RadarDishKeeper {

    /**
     * Scan steps ({@link RadarRules#SCAN_TICKS} ticks each) between looks at whether the dish still exists: one second.
     */
    static final int CHECK_STEPS = 20 / RadarRules.SCAN_TICKS;

    private final int up;
    private final float scale;
    private EntityRadarDish dish;

    /**
     * @param up    blocks from the controller block's floor to the dish plane
     * @param scale the model scale
     */
    RadarDishKeeper(int up, float scale) {
        this.up = up;
        this.scale = scale;
    }

    /**
     * Makes sure the dish exists and sets whether it turns.
     *
     * @param base  the controller's tile
     * @param spin  whether the dish should turn
     * @param check whether to look for a missing dish now (false between the periodic checks)
     */
    void update(IGregTechTileEntity base, boolean spin, boolean check) {
        World w = base.getWorld();
        if (w == null) {
            return;
        }
        if ((dish == null || dish.isDead || dish.worldObj != w) && check) {
            dish = find(base, w);
        }
        if (dish != null && !dish.isDead) {
            dish.setSpinning(spin);
        }
    }

    /** Removes the dish, if there is one. */
    void remove() {
        if (dish != null) {
            dish.setDead();
            dish = null;
        }
    }

    /** @return the live dish, or null */
    EntityRadarDish dish() {
        return dish != null && !dish.isDead ? dish : null;
    }

    private EntityRadarDish find(IGregTechTileEntity base, World w) {
        ForgeDirection back = base.getBackFacing();
        double x = base.getXCoord() + 0.5 + back.offsetX;
        double z = base.getZCoord() + 0.5 + back.offsetZ;
        double y = base.getYCoord() + up;
        if (!w.blockExists((int) Math.floor(x), base.getYCoord(), (int) Math.floor(z))) {
            return null;
        }
        AxisAlignedBB near = AxisAlignedBB.getBoundingBox(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0);
        for (Object o : w.getEntitiesWithinAABB(EntityRadarDish.class, near)) {
            EntityRadarDish e = (EntityRadarDish) o;
            if (!e.isDead && e.servesController(base.getXCoord(), base.getYCoord(), base.getZCoord())) {
                return e;
            }
        }
        EntityRadarDish e = new EntityRadarDish(w);
        e.setPosition(x, y, z);
        e.bind(base.getXCoord(), base.getYCoord(), base.getZCoord(), scale);
        return w.spawnEntityInWorld(e) ? e : null;
    }
}
