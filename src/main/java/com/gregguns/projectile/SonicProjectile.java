package com.gregguns.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.VolleyHits;

/**
 * One projectile of the Sonic Rifle's 16-projectile ring volley. All sixteen carry the same {@link VolleyHits}, so an
 * entity struck by several of them is hurt once; a projectile that meets an entity that is already on the list does
 * nothing and flies on. The wave passes through creatures (it does not end on one) and ends on a solid block, or
 * when its 20 ticks run out. Damage is PHYSICAL with the gun's knockback multiplier (x2).
 *
 * <p>
 * The centre projectile (zero offset) is the "main" one: it sends the {@value #TRAIL_FX} trail once, attached to
 * itself, and the {@value #IMPACT_FX} effect where it meets a block. The other fifteen end silently.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.13 SonicShotgunProjectile)
// Deviation: Techguns applied a 0.01 dummy knockback before every hit; the base class's knockback scaling does the
// same job, so it is not repeated. A projectile with no hit list (any other spawner) does no entity damage, as in
// Techguns.
public class SonicProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its sonic projectile. */
    public static final int ENTITY_ID = 20;
    /** Trail FX attached to the main projectile. */
    public static final String TRAIL_FX = "SonicShotgunTrail";
    /** FX where the main projectile meets a block. */
    public static final String IMPACT_FX = "SonicShotgunImpact";

    private VolleyHits volley;
    private boolean main;
    private boolean trailSent;

    /**
     * @param world the world
     */
    public SonicProjectile(World world) {
        super(world);
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        volley = offset.volley();
        // The centre of the volley points straight along the look direction; every ring projectile is off-axis.
        main = offset.yawDegrees() == 0.0 && offset.pitchDegrees() == 0.0;
    }

    /** @return true for the volley's centre projectile, which carries the trail and impact effects */
    public boolean isMain() {
        return main;
    }

    @Override
    public void onUpdate() {
        if (main && !trailSent && !worldObj.isRemote) {
            trailSent = true;
            FxSender.onEntity(this, TRAIL_FX);
        }
        super.onUpdate();
    }

    @Override
    protected boolean endsOnEntityHit() {
        return false;
    }

    @Override
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        if (volley == null || !volley.firstHit(target.getEntityId())) {
            return;
        }
        super.onEntityHit(target, hitX, hitY, hitZ);
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        if (main && !worldObj.isRemote) {
            Vec3 v = hit.hitVec;
            FxSender.at(worldObj, IMPACT_FX, v.xCoord, v.yCoord, v.zCoord, FxSender.RANGE_NEAR);
        }
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.PHYSICAL;
    }
}
