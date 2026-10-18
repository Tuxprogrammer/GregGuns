package com.gregguns.projectile;

import net.minecraft.world.World;

import com.gregguns.explosion.FlashBlast;

/**
 * The thrown Flashbang: a {@link GrenadeProjectile} whose detonation is a {@link FlashBlast} instead of an explosion.
 * Two bounces and a 40 tick fuse; it also pops on touching an entity.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang)
public class FlashbangProjectile extends GrenadeProjectile {

    /** Mod-local entity ID (Techguns never registered its flashbang's). */
    public static final int ENTITY_ID = 25;

    /**
     * @param world the world
     */
    public FlashbangProjectile(World world) {
        super(world);
        setFuse(GrenadeMath.FLASHBANG_FUSE_TICKS);
    }

    @Override
    protected void explode() {
        FlashBlast.detonate(worldObj, thrower(), posX, posY, posZ);
    }
}
