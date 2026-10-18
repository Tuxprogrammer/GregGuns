package com.gregguns.projectile;

import net.minecraft.world.World;

import com.gregguns.explosion.FragBlast;

/**
 * The thrown Frag Grenade: the {@link GrenadeProjectile} blast (damage 25, radius 6) followed by the steel fragments
 * of {@link FragBlast}. It keeps two bounces and a 120 tick fuse. If a protection listener cancels the blast, the
 * fragments do not fly either.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Frag Grenade);
// docs/gregguns-spec/04-projectiles-fx/grenades.md
public class FragGrenadeProjectile extends GrenadeProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its frag grenade. */
    public static final int ENTITY_ID = 24;

    /**
     * @param world the world
     */
    public FragGrenadeProjectile(World world) {
        super(world);
        setFuse(GrenadeMath.FRAG_FUSE_TICKS);
    }

    @Override
    protected void explode() {
        if (runBlast().happened()) {
            FragBlast.scatter(worldObj, this, thrower(), posX, posY, posZ);
        }
    }
}
