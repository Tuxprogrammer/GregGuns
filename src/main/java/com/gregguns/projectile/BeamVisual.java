package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * A beam drawn from a point along a direction for a few ticks. It deals no damage: the hitscan has already done that.
 * The server spawns it, clients draw it ({@code RenderBeam}), and it dies after its lifetime. Its position is the
 * start of the beam and its yaw and pitch use the projectile convention ({@link Ballistics}).
 *
 * <pre>
 * {@code
 * BeamVisual.spawn(world, x, y, z, dirX, dirY, dirZ, length, BeamMath.LASER_LIFETIME, BeamVisual.STYLE_LASER);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (1 Registration row 18, 3.15 LaserBeam2)
public class BeamVisual extends Entity implements IEntityAdditionalSpawnData {

    /** Mod-local entity ID; the same number Techguns uses for its laser beam. */
    public static final int ENTITY_ID = 18;
    /** Red laser texture. */
    public static final int STYLE_LASER = 0;
    /** Blue beam texture. */
    public static final int STYLE_BLUE = 1;

    private float length;
    private int lifetime = BeamMath.LASER_LIFETIME;
    private int seed;
    private int style;

    /**
     * Builds an empty beam: the form FML uses on the client and when loading from NBT.
     *
     * @param world the world
     */
    public BeamVisual(World world) {
        super(world);
        setSize(0.1F, 0.1F);
        // The entity sits at the start but the beam is long: never cull it by the distance to its start.
        renderDistanceWeight = 8.0;
        ignoreFrustumCheck = true;
    }

    /**
     * Spawns a beam on the server. A client world is ignored.
     *
     * @param world    the world
     * @param x        start X
     * @param y        start Y
     * @param z        start Z
     * @param dirX     direction X, any length above zero
     * @param dirY     direction Y
     * @param dirZ     direction Z
     * @param length   beam length in blocks
     * @param lifetime ticks to show it
     * @param style    {@link #STYLE_LASER} or {@link #STYLE_BLUE}
     * @return the beam, or {@code null} on a client world
     */
    public static BeamVisual spawn(World world, double x, double y, double z, double dirX, double dirY, double dirZ,
        double length, int lifetime, int style) {
        if (world.isRemote) {
            return null;
        }
        BeamVisual beam = new BeamVisual(world);
        beam.length = (float) length;
        beam.lifetime = lifetime;
        beam.style = style;
        beam.seed = world.rand.nextInt();
        float yaw = Ballistics.yawOf(dirX, dirZ);
        float pitch = Ballistics.pitchOf(dirX, dirY, dirZ);
        beam.setLocationAndAngles(x, y, z, yaw, pitch);
        beam.prevRotationYaw = yaw;
        beam.prevRotationPitch = pitch;
        world.spawnEntityInWorld(beam);
        return beam;
    }

    /** @return beam length in blocks */
    public float getLength() {
        return length;
    }

    /** @return ticks the beam is shown in all */
    public int getLifetime() {
        return lifetime;
    }

    /** @return flicker seed */
    public int getSeed() {
        return seed;
    }

    /** @return texture variant */
    public int getStyle() {
        return style;
    }

    @Override
    protected void entityInit() {}

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void onUpdate() {
        // No super.onUpdate(): a beam does not move, burn or fall.
        if (++ticksExisted >= lifetime) {
            setDead();
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setFloat("length", length);
        tag.setInteger("lifetime", lifetime);
        tag.setInteger("seed", seed);
        tag.setInteger("style", style);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        length = tag.getFloat("length");
        lifetime = tag.getInteger("lifetime");
        seed = tag.getInteger("seed");
        style = tag.getInteger("style");
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        new BeamSpawnData(length, lifetime, seed, style).write(buffer);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        BeamSpawnData data = BeamSpawnData.read(additionalData);
        length = data.length();
        lifetime = data.lifetime();
        seed = data.seed();
        style = data.style();
    }
}
