package com.gregguns.icbm.explosive;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * What a {@link BlastEffect} sees: the blast's world, center, explosive, fill, radius and cause, plus a state
 * compound the engine saves and reloads with the blast. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/explosive-registry.md (effect hook)
public final class BlastContext {

    private final World world;
    private final double x;
    private final double y;
    private final double z;
    private final ExplosiveDef explosive;
    private final int fill;
    private final double radius;
    private final EntityLivingBase source;
    private final NBTTagCompound state;
    private final TickBudget budget;

    /**
     * Engine use only.
     *
     * @param budget the shared budget, see {@link #budget()}
     */
    public BlastContext(World world, double x, double y, double z, ExplosiveDef explosive, int fill, double radius,
        EntityLivingBase source, NBTTagCompound state, TickBudget budget) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.explosive = explosive;
        this.fill = fill;
        this.radius = radius;
        this.source = source;
        this.state = state;
        this.budget = budget;
    }

    /** @return the world (server) */
    public World world() {
        return world;
    }

    /** @return center X */
    public double x() {
        return x;
    }

    /** @return center Y */
    public double y() {
        return y;
    }

    /** @return center Z */
    public double z() {
        return z;
    }

    /** @return the explosive */
    public ExplosiveDef explosive() {
        return explosive;
    }

    /** @return the warhead fill */
    public int fill() {
        return fill;
    }

    /** @return the final blast radius, blocks, with any fuel bonus */
    public double radius() {
        return radius;
    }

    /** @return who set it off, or {@code null}; a player makes block breaks ask the protection mods */
    public EntityLivingBase source() {
        return source;
    }

    /** @return the effect's own persistent state; saved with the blast, never null */
    public NBTTagCompound state() {
        return state;
    }

    /** @return the tick budget every running blast shares; effects with heavy work check it */
    public TickBudget budget() {
        return budget;
    }
}
