package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxSender;

/**
 * A fast energy bolt (the De-Atomizer's blue blast and the Blaster Rifle's bolt): a straight {@link GenericProjectile}
 * that deals ENERGY damage. It leaves a {@code BlueBlasterTrail} that follows it and a {@code BlueBlasterExplosion}
 * flare where it hits a block or an entity. The area explosion is commented out in Techguns and is not built, so the
 * impact is FX only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.10 EnergyBlastProjectile)
// Deviation: the Blaster Rifle also fires this bolt (plan M5 B4), so it shares the blue trail and impact flare; in
// Techguns its projectile had no impact FX but a vanilla smoke puff. The trail is sent on the bolt's second tick, once
// the entity tracker lists the players that see it (PFX-01 shows what a trail sent too early does).
public class EnergyBlastProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its energy blast. */
    public static final int ENTITY_ID = 26;

    /** FX list that follows the bolt. */
    public static final String TRAIL_FX = "BlueBlasterTrail";

    /** FX list played where the bolt hits. */
    public static final String IMPACT_FX = "BlueBlasterExplosion";

    private static final int TRAIL_TICK = 2;

    /**
     * @param world the world
     */
    public EnergyBlastProjectile(World world) {
        super(world);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote && !isDead && ticksExisted == TRAIL_TICK) {
            FxSender.onEntity(this, TRAIL_FX);
        }
    }

    @Override
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        super.onEntityHit(target, hitX, hitY, hitZ);
        impact(hitX, hitY, hitZ);
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        super.onBlockHit(hit);
        impact(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
    }

    private void impact(double x, double y, double z) {
        if (!worldObj.isRemote) {
            FxSender.at(worldObj, IMPACT_FX, x, y, z, FxSender.RANGE_NEAR);
        }
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.ENERGY;
    }
}
