package com.gregguns.icbm.defense;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * A radar's dish as an entity (D125, the way the silo's doors are, D89): the structure's top has no block that could
 * hold a tile entity, so the dish rides on an entity that Minecraft already tracks for every player in range. The
 * controller owns the state and pushes it here; the entity carries it to clients in {@link PacketRadarDish} (spinning,
 * the angle and tick of the last change, the size), sent on a change and repeated once a second for late joiners.
 * Clients compute the angle from the game time with {@link DishSpin}, so nothing is sent per tick. The entity is never
 * saved: the controller makes a new one when its chunk loads, and the entity removes itself when the controller is gone
 * or no longer formed, so a reload can never leave two. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
public final class EntityRadarDish extends Entity {

    private static final int CHECK_TICKS = 20;

    private final DishSpin spin = new DishSpin();
    private float scale = 1.0F;
    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean bound;
    private boolean dirty = true;

    /**
     * @param world the world
     */
    public EntityRadarDish(World world) {
        super(world);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
        this.preventEntitySpawning = false;
        setSize(0.5F, 0.5F);
    }

    @Override
    protected void entityInit() {
        // No DataWatcher slot (GGS-01-11): the state reaches clients through PacketRadarDish.
    }

    /**
     * Client side: takes the state the server sent.
     *
     * @param on    whether the dish turns
     * @param angle the angle at the change, degrees
     * @param start the low 32 bits of the server's game time at the change
     * @param size  the model scale
     */
    void applyState(boolean on, float angle, int start, float size) {
        long now = worldObj.getTotalWorldTime();
        // The start was sent as the low 32 bits of the server's game time.
        long full = now - (int) ((int) now - start);
        spin.restore(on, angle, full);
        scale = size;
    }

    /**
     * Server side: ties this entity to its controller.
     *
     * @param x     controller X
     * @param y     controller Y
     * @param z     controller Z
     * @param scale the model scale (1 for the small radar)
     */
    public void bind(int x, int y, int z, float scale) {
        controllerX = x;
        controllerY = y;
        controllerZ = z;
        this.scale = scale;
        bound = true;
    }

    /**
     * @param x controller X
     * @param y controller Y
     * @param z controller Z
     * @return true when this entity serves that controller
     */
    public boolean servesController(int x, int y, int z) {
        return bound && controllerX == x && controllerY == y && controllerZ == z;
    }

    /**
     * Server side: turns the dish on or off and tells the clients when that changed it.
     *
     * @param on whether the dish should turn
     */
    public void setSpinning(boolean on) {
        if (spin.set(on, worldObj.getTotalWorldTime())) {
            dirty = true;
        }
    }

    /** @return the spin state (the client's copy on a client) */
    public DishSpin spin() {
        return spin;
    }

    /** @return the model scale */
    public float scale() {
        return scale;
    }

    /**
     * @param partialTicks the render partial tick
     * @return the dish's angle, degrees
     */
    public float angle(float partialTicks) {
        long now = worldObj.getTotalWorldTime();
        float a = spin.angle(now);
        return spin.spinning() ? a + DishSpin.DEGREES_PER_TICK * partialTicks : a;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            return;
        }
        if (!bound || ticksExisted % CHECK_TICKS == 0 && !controllerAlive()) {
            setDead();
            return;
        }
        if (dirty || ticksExisted % CHECK_TICKS == 0) {
            dirty = false;
            com.gregguns.network.GregGunsNetwork.channel()
                .sendToAllTracking(new PacketRadarDish(this), this);
        }
    }

    private boolean controllerAlive() {
        if (!worldObj.blockExists(controllerX, controllerY, controllerZ)) {
            return true; // not loaded: leave it, the chunk takes us with it
        }
        TileEntity t = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
        if (!(t instanceof IGregTechTileEntity)) {
            return false;
        }
        Object mte = ((IGregTechTileEntity) t).getMetaTileEntity();
        return mte instanceof RadarDishHost && ((RadarDishHost) mte).isFormed();
    }

    /** Never saved: the controller makes a new one. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distanceSq) {
        return distanceSq < 160.0 * 160.0;
    }

    @Override
    public boolean hitByEntity(Entity other) {
        return true; // not attackable
    }

    @Override
    public boolean isEntityInvulnerable() {
        return true;
    }
}
