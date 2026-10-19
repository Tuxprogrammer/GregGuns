package com.gregguns.projectile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The Tesla Rifle's shot: an instant hitscan over the gun's flight (speed 5 for 10 ticks, so 50 blocks) that, on
 * hitting a creature, jumps to up to {@value #MAX_JUMPS} more within {@value #CHAIN_RANGE} blocks of the last one.
 * Targets come from {@link ChainSelect}: nearest first, never the shooter, the dead, allies or a target twice, and
 * only with a clear line. Every segment gets a blue {@link BeamVisual} and every struck target a {@link #FLARE_FX}.
 * The entity ends on its first update; no bolt flies.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.6 TeslaProjectile); fixes PFX-07 (damage factor
// applies to the whole figure), PFX-08 (no forced chain, alive and sight checks), PFX-09 (beams aim at the target's
// center, not offset by a sign slip) and GUN-21 (the jump count and range are the ones in force).
// Deviation: Techguns flew an invisible bolt and drew nothing on a miss; here the shot is instant and the beam is
// drawn to the end of the trace. The chain damage drop is 0 per jump, which matches what Techguns effectively did.
public class TeslaProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its tesla projectile. */
    public static final int ENTITY_ID = 9;
    /** Most extra targets one shot reaches. */
    public static final int MAX_JUMPS = 3;
    /** Longest single jump, blocks. */
    public static final double CHAIN_RANGE = 5.0;
    /** Damage removed per jump. */
    public static final double CHAIN_DROP = 0.0;
    // Spec: sheets-energy-special.md (Tesla Rifle): the damage source is tg_lightning / LIGHTNING, so electrical
    // armor resists it and a hit shocks. The first cut of this class used ENERGY; the sheet says LIGHTNING.
    private static final DamageType DAMAGE_TYPE = DamageType.LIGHTNING;
    /** FX list played on every struck target (the FX catalog owns it; an unknown name is only logged). */
    public static final String FLARE_FX = "TeslaFlare";
    private static final double FX_RANGE = FxSender.RANGE_NEAR;

    /**
     * @param world the world
     */
    public TeslaProjectile(World world) {
        super(world);
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        double range = stats.projectileSpeed() * stats.lifetimeTicks();
        double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (speed > 0 && range > 0) {
            HitscanTracer.Trace trace = HitscanTracer
                .trace(worldObj, firedBy, posX, posY, posZ, motionX, motionY, motionZ, range);
            Hitscan.Result r = trace.result();
            BeamVisual.spawn(
                worldObj,
                posX,
                posY,
                posZ,
                motionX,
                motionY,
                motionZ,
                r.distance(),
                BeamMath.LASER_LIFETIME,
                BeamVisual.STYLE_BLUE);
            Entity hit = trace.entity();
            if (hit != null) {
                onEntityHit(hit, r.endX(), r.endY(), r.endZ());
                FxSender.at(worldObj, FLARE_FX, r.endX(), r.endY(), r.endZ(), FX_RANGE);
                if (hit instanceof EntityLivingBase) {
                    chain((EntityLivingBase) hit);
                }
            }
        }
        motionX = 0;
        motionY = 0;
        motionZ = 0;
    }

    private void chain(EntityLivingBase primary) {
        double cx = primary.posX;
        double cy = primary.posY + primary.height / 2.0;
        double cz = primary.posZ;
        // Every jump may go on from the last target, so the search covers the whole chain's reach.
        double reach = CHAIN_RANGE * MAX_JUMPS;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(cx, cy, cz, cx, cy, cz)
            .expand(reach, reach, reach);
        List<?> near = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);
        List<EntityLivingBase> living = new ArrayList<>();
        List<ChainSelect.Candidate> candidates = new ArrayList<>();
        List<Vec3> centerList = new ArrayList<>();
        for (Object e : near) {
            EntityLivingBase l = (EntityLivingBase) e;
            living.add(l);
            centerList.add(Vec3.createVectorHelper(l.posX, l.posY + l.height / 2.0, l.posZ));
            candidates.add(
                new ChainSelect.Candidate(
                    l.posX,
                    l.posY + l.height / 2.0,
                    l.posZ,
                    l.isEntityAlive(),
                    isFriendly(l),
                    l == shooter,
                    l == primary));
        }
        Vec3[] centers = centerList.toArray(new Vec3[0]);
        List<Integer> picked = ChainSelect.select(
            cx,
            cy,
            cz,
            candidates,
            CHAIN_RANGE,
            MAX_JUMPS,
            (fx, fy, fz, i) -> clearLine(centers, fx, fy, fz, i));
        double fromX = cx;
        double fromY = cy;
        double fromZ = cz;
        for (int i = 0; i < picked.size(); i++) {
            EntityLivingBase target = living.get(picked.get(i));
            double tx = target.posX;
            double ty = target.posY + target.height / 2.0;
            double tz = target.posZ;
            double dx = tx - fromX;
            double dy = ty - fromY;
            double dz = tz - fromZ;
            BeamVisual.spawn(
                worldObj,
                fromX,
                fromY,
                fromZ,
                dx,
                dy,
                dz,
                Math.sqrt(dx * dx + dy * dy + dz * dz),
                BeamMath.LASER_LIFETIME,
                BeamVisual.STYLE_BLUE);
            float dealt = (float) ChainSelect.damageAt(damage, CHAIN_DROP, i);
            if (dealt > 0) {
                DamageSources.hit(target, DAMAGE_TYPE, this, shooter, dealt, penetration, secondaryEffects());
            }
            FxSender.at(worldObj, FLARE_FX, tx, ty, tz, FX_RANGE);
            fromX = tx;
            fromY = ty;
            fromZ = tz;
        }
    }

    private boolean clearLine(Vec3[] centers, double fx, double fy, double fz, int candidate) {
        // Flags: no liquids, skip blocks without a collision box (plants), no last-uncollidable result.
        return worldObj.func_147447_a(Vec3.createVectorHelper(fx, fy, fz), centers[candidate], false, true, false)
            == null;
    }

    private boolean isFriendly(EntityLivingBase e) {
        if (shooter == null) {
            return false;
        }
        if (e.isOnSameTeam(shooter)) {
            return true;
        }
        return e instanceof EntityTameable && ((EntityTameable) e).getOwner() == shooter;
    }

    @Override
    public void onUpdate() {
        setDead();
    }

    @Override
    protected DamageType getDamageType() {
        return DAMAGE_TYPE;
    }
}
