package com.gregguns.icbm.silo;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.icbm.silo.SiloDoors.State;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The silo's two door panels as an entity (D89): there is no block at the rim to hold a tile entity, so the panels
 * ride on an entity that Minecraft already tracks for every player in range. The controller owns the state and
 * pushes it here; the entity carries it to clients in {@link PacketSiloDoors} (state, start time, swing duration),
 * sent when a swing starts or ends and repeated once a second for late joiners. Clients interpolate with
 * {@link SiloDoors#progress}. The entity is never saved: the controller makes a new one when its chunk loads, and the
 * entity removes itself when the controller is gone or no longer a formed silo. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The doors)
public final class EntitySiloDoors extends Entity {

    private static final int CHECK_TICKS = 20;

    private final SiloDoors doors = new SiloDoors();
    private int controllerX;
    private int controllerY;
    private int controllerZ;
    private boolean bound;
    private boolean dirty = true;

    /**
     * @param world the world
     */
    public EntitySiloDoors(World world) {
        super(world);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
        this.preventEntitySpawning = false;
        setSize(0.5F, 0.5F);
    }

    @Override
    protected void entityInit() {
        // No DataWatcher slot (GGS-01-11): the state reaches clients through PacketSiloDoors.
    }

    /**
     * Client side: takes the door state the server sent.
     *
     * @param state the state ordinal
     * @param start the low 32 bits of the server's game time when the swing began
     * @param dur   ticks a full swing takes
     */
    void applyState(int state, int start, int dur) {
        long now = worldObj.getTotalWorldTime();
        // The start was sent as the low 32 bits of the server's game time.
        long full = now - (int) ((int) now - start);
        State[] all = State.values();
        doors.restore(all[Math.max(0, Math.min(all.length - 1, state))], full, dur);
    }

    /**
     * Server side: ties this entity to its controller.
     *
     * @param x controller X
     * @param y controller Y
     * @param z controller Z
     */
    public void bind(int x, int y, int z) {
        controllerX = x;
        controllerY = y;
        controllerZ = z;
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
     * Server side: copies the controller's door state and tells the clients.
     *
     * @param source the controller's machine
     */
    public void push(SiloDoors source) {
        doors.restore(source.state(), source.start(), source.duration());
        dirty = true;
    }

    /** @return the door machine this entity holds (the client's copy on a client) */
    public SiloDoors doors() {
        return doors;
    }

    /**
     * @param partialTicks the render partial tick
     * @return how far open the panels are, 0 to 1
     */
    public double progress(float partialTicks) {
        // The curve is evaluated at fractional time by sampling the two whole ticks around it.
        long now = worldObj.getTotalWorldTime();
        double a = doors.progress(now);
        double b = doors.progress(now + 1);
        return a + (b - a) * partialTicks;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            doors.update(worldObj.getTotalWorldTime());
            return;
        }
        if (!bound || ticksExisted % CHECK_TICKS == 0 && !controllerAlive()) {
            setDead();
            return;
        }
        if (dirty || ticksExisted % CHECK_TICKS == 0) {
            dirty = false;
            com.gregguns.network.GregGunsNetwork.channel()
                .sendToAllTracking(new PacketSiloDoors(this), this);
        }
    }

    private boolean controllerAlive() {
        if (!worldObj.blockExists(controllerX, controllerY, controllerZ)) {
            return true; // not loaded: leave it, the chunk takes us with it
        }
        TileEntity t = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
        return t instanceof IGregTechTileEntity
            && ((IGregTechTileEntity) t).getMetaTileEntity() instanceof MTEMissileSilo
            && ((MTEMissileSilo) ((IGregTechTileEntity) t).getMetaTileEntity()).isFormed();
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
        return distanceSq < 128.0 * 128.0;
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
