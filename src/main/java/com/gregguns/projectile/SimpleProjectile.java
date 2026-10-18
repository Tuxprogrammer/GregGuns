package com.gregguns.projectile;

import net.minecraft.world.World;

/**
 * A bullet that drops: a {@link GenericProjectile} with gravity, for the hand cannon's ball. Gravity is part of the
 * spawn data, so the client copy falls the same way as the server's.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.1 SimpleProjectile, 4.2)
// Deviation: Techguns also stores a texture path and scale here that nothing reads (PFX-34); GregGuns drops them.
public class SimpleProjectile extends GenericProjectile {

    /** Gravity of the hand cannon's ball, blocks per tick squared. */
    public static final float DEFAULT_GRAVITY = 0.015F;

    /**
     * @param world the world
     */
    public SimpleProjectile(World world) {
        super(world);
        gravity = DEFAULT_GRAVITY;
    }

    /**
     * @param newGravity blocks per tick squared pulled downwards; set before spawning
     */
    public void setGravity(float newGravity) {
        gravity = newGravity;
    }
}
