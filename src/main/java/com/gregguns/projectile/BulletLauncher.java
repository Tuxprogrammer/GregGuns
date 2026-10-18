package com.gregguns.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The installed {@link ProjectileLauncher}: spawns one {@link GenericProjectile} per shot direction on the server.
 * Server only; a client world is ignored, because the server alone decides hits.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.2)
public final class BulletLauncher implements ProjectileLauncher {

    @Override
    public void launch(World world, EntityLivingBase shooter, GunStats stats, ShotOffset offset, float damageScale) {
        if (world.isRemote) {
            return;
        }
        GenericProjectile bullet = new GenericProjectile(world);
        bullet.fire(shooter, stats, offset, damageScale);
        world.spawnEntityInWorld(bullet);
    }
}
