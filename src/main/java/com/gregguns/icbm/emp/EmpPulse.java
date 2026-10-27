package com.gregguns.icbm.emp;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.missile.EntityMissile;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import ic2.api.tile.IEnergyStorage;

/**
 * One EMP pulse (D16): the machinery the EMP Tower and the T3 EMP warhead share.
 *
 * <p>
 * Machines: the pulse walks the tile entities of the chunks that are already loaded (never block positions and never
 * a chunk load, so a pulse costs what the base holds, not the volume of the sphere; ICBM-05-10 walked every block of a
 * skewed cube). A tile counts as a device when it is a GT tile entity that stores EU or runs a multiblock, or an IC2
 * energy store. The nearest {@link IcbmConfig#empMaxDevices} devices inside the radius are hit, each drained to zero,
 * and a multiblock controller among them also gets one maintenance issue it did not have. GT cables hold no EU of
 * their own, so there is nothing in them to drain.
 *
 * <p>
 * Missiles: every missile in flight inside the radius rolls {@link MissileTable}. Server thread only.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md
public final class EmpPulse {

    /** What a pulse did. */
    public static final class Result {

        /** Devices in range that the cap left out. */
        public int skippedByCap;
        /** Devices hit. */
        public int devices;
        /** EU taken out of devices. */
        public long euDrained;
        /** Maintenance issues caused. */
        public int issues;
        /** Missiles destroyed outright. */
        public int missilesDestroyed;
        /** Missiles whose guidance was fried. */
        public int missilesFried;
        /** Missiles whose warhead went off. */
        public int missilesTriggered;
        /** Tile entities looked at. */
        public int tilesScanned;
        /** Nanoseconds the whole pulse took. */
        public long nanos;
    }

    private EmpPulse() {}

    /**
     * Fires a pulse with the configured cap and missile table.
     *
     * @param world  the world (server)
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks
     * @param mode   what to reach
     * @return what happened
     */
    public static Result fire(World world, double x, double y, double z, double radius, EmpMode mode) {
        return fire(world, x, y, z, radius, mode, null);
    }

    /**
     * Fires a pulse with the configured cap and missile table, sparing the sender.
     *
     * @param world  the world (server)
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks
     * @param mode   what to reach
     * @param self   the sender's tile entity, or null
     * @return what happened
     */
    public static Result fire(World world, double x, double y, double z, double radius, EmpMode mode, TileEntity self) {
        MissileTable table = new MissileTable(
            IcbmConfig.empMissileKillChance,
            IcbmConfig.empMissileGuidanceChance,
            IcbmConfig.empMissileTriggerChance);
        return fire(world, x, y, z, radius, mode, IcbmConfig.empMaxDevices, table, self);
    }

    /**
     * Fires a pulse.
     *
     * @param world  the world (server)
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks
     * @param mode   what to reach
     * @param cap    most devices to hit
     * @param table  the missile kill chances
     * @return what happened
     */
    public static Result fire(World world, double x, double y, double z, double radius, EmpMode mode, int cap,
        MissileTable table) {
        return fire(world, x, y, z, radius, mode, cap, table, null);
    }

    /**
     * Fires a pulse that spares one tile entity: the tower that sent it does not drain itself or spend a place under
     * the device cap.
     *
     * @param world  the world (server)
     * @param x      center X
     * @param y      center Y
     * @param z      center Z
     * @param radius blocks
     * @param mode   what to reach
     * @param cap    most devices to hit
     * @param table  the missile kill chances
     * @param self   the sender's tile entity, or null
     * @return what happened
     */
    public static Result fire(World world, double x, double y, double z, double radius, EmpMode mode, int cap,
        MissileTable table, TileEntity self) {
        long start = System.nanoTime();
        Result out = new Result();
        if (world.isRemote || radius <= 0.0) {
            return out;
        }
        if (mode.hitsMachines()) {
            hitMachines(world, x, y, z, radius, cap, self, out);
        }
        if (mode.hitsMissiles()) {
            hitMissiles(world, x, y, z, radius, table, out);
        }
        if (world instanceof WorldServer) {
            // Both sides of ICBM-05-13: the spark particles are sent to the clients, not spawned on the server.
            ((WorldServer) world).func_147487_a("largeexplode", x, y + 1.0, z, 1, 0.0, 0.0, 0.0, 0.0);
            ((WorldServer) world).func_147487_a("magicCrit", x, y + 1.0, z, 60, 1.5, 1.0, 1.5, 0.5);
            world.playSoundEffect(x, y, z, "random.fizz", 4.0F, 0.7F);
        }
        out.nanos = System.nanoTime() - start;
        return out;
    }

    // --- machines -------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void hitMachines(World world, double x, double y, double z, double radius, int cap, TileEntity self,
        Result out) {
        double r2 = radius * radius;
        int minCx = ((int) Math.floor(x - radius)) >> 4;
        int maxCx = ((int) Math.floor(x + radius)) >> 4;
        int minCz = ((int) Math.floor(z - radius)) >> 4;
        int maxCz = ((int) Math.floor(z + radius)) >> 4;
        NearestPicker<TileEntity> picker = new NearestPicker<>(cap);
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                // Never load a chunk for a pulse: a chunk that is not loaded has no running machines to hit.
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                for (Object o : ((Map<?, TileEntity>) chunk.chunkTileEntityMap).values()) {
                    TileEntity tile = (TileEntity) o;
                    out.tilesScanned++;
                    double dx = tile.xCoord + 0.5 - x;
                    double dy = tile.yCoord + 0.5 - y;
                    double dz = tile.zCoord + 0.5 - z;
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= r2 && tile != self && !tile.isInvalid() && isDevice(tile)) {
                        picker.offer(tile, d2);
                    }
                }
            }
        }
        out.skippedByCap = (int) Math.max(0, picker.offered() - cap);
        List<TileEntity> hit = picker.nearestFirst();
        for (TileEntity tile : hit) {
            out.devices++;
            out.euDrained += drain(tile);
            if (maintenance(tile, world.rand)) {
                out.issues++;
            }
        }
    }

    /**
     * @param tile any tile entity
     * @return true when a pulse treats it as a device: a GT tile entity that holds EU or runs a multiblock, or an IC2
     *         energy store
     */
    public static boolean isDevice(TileEntity tile) {
        if (tile instanceof BaseMetaTileEntity) {
            BaseMetaTileEntity base = (BaseMetaTileEntity) tile;
            return base.getEUCapacity() > 0 || base.getMetaTileEntity() instanceof MTEMultiBlockBase;
        }
        return tile instanceof IEnergyStorage;
    }

    /**
     * Sets the tile's stored energy to zero.
     *
     * @param tile a device
     * @return EU that was in it
     */
    static long drain(TileEntity tile) {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1263-L1268
        // (setStoredEU is public and clamps below zero to zero)
        if (tile instanceof BaseMetaTileEntity) {
            BaseMetaTileEntity base = (BaseMetaTileEntity) tile;
            long had = base.getStoredEU();
            if (had > 0) {
                base.setStoredEU(0L);
            }
            return Math.max(0L, had);
        }
        if (tile instanceof IEnergyStorage) {
            IEnergyStorage store = (IEnergyStorage) tile;
            int had = store.getStored();
            if (had > 0) {
                store.setStored(0);
            }
            return Math.max(0, had);
        }
        return 0L;
    }

    /**
     * Gives a multiblock controller one maintenance issue it does not have yet.
     *
     * @param tile a device
     * @param rng  randomness
     * @return true when an issue was caused
     */
    static boolean maintenance(TileEntity tile, Random rng) {
        if (!(tile instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) tile).getMetaTileEntity() instanceof MTEMultiBlockBase)) {
            return false;
        }
        MTEMultiBlockBase multi = (MTEMultiBlockBase) ((IGregTechTileEntity) tile).getMetaTileEntity();
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEMultiBlockBase.java#L1458-L1510
        // (six public booleans; causeMaintenanceIssue() picks one at random even when it is already broken, so the
        // pick is made here among the working ones)
        if (!multi.shouldCheckMaintenance()) {
            return false;
        }
        int mask = (multi.mWrench ? 1 : 0) | (multi.mScrewdriver ? 2 : 0)
            | (multi.mSoftMallet ? 4 : 0)
            | (multi.mHardHammer ? 8 : 0)
            | (multi.mSolderingTool ? 16 : 0)
            | (multi.mCrowbar ? 32 : 0);
        switch (EmpRules.pickIssue(mask, rng.nextInt())) {
            case 0:
                multi.mWrench = false;
                break;
            case 1:
                multi.mScrewdriver = false;
                break;
            case 2:
                multi.mSoftMallet = false;
                break;
            case 3:
                multi.mHardHammer = false;
                break;
            case 4:
                multi.mSolderingTool = false;
                break;
            case 5:
                multi.mCrowbar = false;
                break;
            default:
                return false;
        }
        multi.getBaseMetaTileEntity()
            .markDirty();
        return true;
    }

    // --- missiles -------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void hitMissiles(World world, double x, double y, double z, double radius, MissileTable table,
        Result out) {
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<EntityMissile> missiles = world.getEntitiesWithinAABB(EntityMissile.class, box);
        double r2 = radius * radius;
        Random rng = world.rand;
        for (EntityMissile m : missiles) {
            if (m.isDead || !m.inFlight() || sq(m, x, y, z) > r2) {
                continue;
            }
            switch (table.roll(rng.nextDouble(), rng.nextDouble(), rng.nextDouble())) {
                case DESTROYED:
                    m.destroyByEmp();
                    out.missilesDestroyed++;
                    break;
                case FRIED:
                    if (m.fryGuidance()) {
                        out.missilesFried++;
                    }
                    break;
                case TRIGGERED:
                    // The missile detonates and is removed in the same call; ICBM left it alive (ICBM-05-14).
                    m.detonateAt(m.posX, m.posY, m.posZ);
                    out.missilesTriggered++;
                    break;
                default:
                    break;
            }
        }
    }

    private static double sq(Entity e, double x, double y, double z) {
        double dx = e.posX - x;
        double dy = e.posY - y;
        double dz = e.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
