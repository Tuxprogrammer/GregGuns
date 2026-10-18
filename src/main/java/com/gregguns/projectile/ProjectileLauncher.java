package com.gregguns.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The seam between guns and projectiles: a gun that has fired asks the launcher to put one projectile into the world
 * per {@link ShotOffset}. The projectile framework supplies the implementation; guns, NPCs and turrets only call it.
 * Server side only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.2 Spawn position and initial velocity)
public interface ProjectileLauncher {

    /**
     * Spawns one projectile for one shot direction.
     *
     * @param world       the shooter's world (server)
     * @param shooter     the player, NPC or turret that fired; becomes the damage source's attacker
     * @param stats       the firing gun's stats: damage, drop-off, speed, lifetime, penetration, projectile kind
     * @param offset      the spread offset for this projectile, from {@code SpreadModel}
     * @param damageScale multiplier on the gun's damage, for NPC and turret damage factors; 1 for players
     */
    void launch(World world, EntityLivingBase shooter, GunStats stats, ShotOffset offset, float damageScale);
}
