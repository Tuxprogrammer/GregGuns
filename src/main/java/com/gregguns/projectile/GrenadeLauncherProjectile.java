package com.gregguns.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The 40 mm round of the Grenade Launcher: a {@link GrenadeProjectile} that is fired instead of thrown. It falls under
 * the gun's gravity, survives {@link LauncherMath#GRENADE_BOUNCES two bounces}, loses
 * {@link LauncherMath#BOUNCE_FUSE_PENALTY twenty ticks} of fuse with each, and explodes when its fuse runs out, when
 * it meets an entity, or when it hits a surface hard with no bounces left. The blast is the grenade family's: the
 * gun's damage at the centre in full to half the radius, none beyond one and a half, honoring
 * {@code explosions.blockDamage} and every protection hook, and the shooter is not immune to it (PFX-25).
 *
 * <p>
 * Deviation (GUN-04): Techguns' launcher ignored the gun's damage bonus, time to live, accuracy and penetration. Here
 * the gun's stats carry the figures the original hard-coded (fuse 100, spread 0.05, penetration 0), so the stats are
 * honored and an NPC's damage scale applies.
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     GrenadeLauncherProjectile g = new GrenadeLauncherProjectile(world);
 *     g.fire(shooter, stats, offset, 1.0F);
 *     world.spawnEntityInWorld(g);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.7, GrenadeLauncherProjectile);
// docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Grenade Launcher)
public class GrenadeLauncherProjectile extends GrenadeProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its launcher grenade. */
    public static final int ENTITY_ID = 19;

    /**
     * @param world the world
     */
    /** The ICBM warhead the launcher spent a charge on for this grenade (D150), or null. */
    private String icbmWarhead;

    public GrenadeLauncherProjectile(World world) {
        super(world);
        gravity = 0.01F;
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        gravity = (float) stats.projectileGravity();
        arm((float) stats.blastRadius(), LauncherMath.GRENADE_BOUNCES, stats.id());
        setFuse(stats.lifetimeTicks());
        icbmWarhead = com.gregguns.icbm.launcher.LauncherWarheads.consume(firedBy, stats.id());
    }

    @Override
    protected void explode() {
        // D150: an ICBM warhead replaces the grenade's own blast.
        if (!com.gregguns.icbm.launcher.LauncherWarheads.detonate(worldObj, posX, posY, posZ, icbmWarhead, thrower())) {
            super.explode();
        }
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (icbmWarhead != null) {
            tag.setString("icbmWarhead", icbmWarhead);
        }
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        icbmWarhead = tag.hasKey("icbmWarhead") ? tag.getString("icbmWarhead") : null;
    }

    @Override
    protected void onBounce() {
        setFuse(LauncherMath.bounceFuse(getFuseLeft()));
    }
}
