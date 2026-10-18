package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.projectile.RayBox;

/**
 * The fragments of a Frag Grenade: {@link FragMath#COUNT} rays leave the blast in the fixed {@link FragMath#direction}
 * pattern; each stops at the first entity it meets within {@link FragMath#RANGE} (and at any opaque block before it).
 * Every entity then takes the sum of its fragments' damage in one {@code PROJECTILE} hit, with a share of it skipping
 * armor. Entirely deterministic. Server only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Frag Grenade)
public final class FragBlast {

    private FragBlast() {}

    /**
     * Throws the fragments.
     *
     * @param world  the world; nothing happens on a client
     * @param source the grenade entity (the direct cause), never hit itself
     * @param owner  who threw it, may be {@code null}
     * @param x      centre X
     * @param y      centre Y
     * @param z      centre Z
     * @return how many entities took fragment damage
     */
    @SuppressWarnings("unchecked")
    public static int scatter(World world, Entity source, EntityLivingBase owner, double x, double y, double z) {
        if (world.isRemote) {
            return 0;
        }
        double r = FragMath.RANGE;
        List<Entity> found = world.getEntitiesWithinAABBExcludingEntity(
            source,
            AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r));
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Entity e : found) {
            if (e instanceof EntityLivingBase && !e.isDead && (e != owner || ExplosionsConfig.selfDamage)) {
                targets.add((EntityLivingBase) e);
            }
        }
        Map<EntityLivingBase, double[]> totals = new LinkedHashMap<>();
        for (int i = 0; i < FragMath.COUNT; i++) {
            double[] d = FragMath.direction(i, FragMath.COUNT);
            double vx = d[0] * r;
            double vy = d[1] * r;
            double vz = d[2] * r;
            EntityLivingBase best = null;
            double bestT = Double.MAX_VALUE;
            for (EntityLivingBase e : targets) {
                AxisAlignedBB b = e.boundingBox;
                double t = RayBox.hit(x, y, z, vx, vy, vz, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, 0.0);
                if (t >= 0.0 && t < bestT) {
                    best = e;
                    bestT = t;
                }
            }
            if (best == null || !LineOfSight.clear(world, x, y, z, x + vx * bestT, y + vy * bestT, z + vz * bestT)) {
                continue;
            }
            double[] sum = totals.get(best);
            if (sum == null) {
                sum = new double[1];
                totals.put(best, sum);
            }
            sum[0] += FragMath.damage(r * bestT);
        }
        int hurt = 0;
        for (Map.Entry<EntityLivingBase, double[]> entry : totals.entrySet()) {
            if (DamageSources.hit(
                entry.getKey(),
                DamageType.PROJECTILE,
                source,
                owner,
                (float) entry.getValue()[0],
                FragMath.PENETRATION)) {
                hurt++;
            }
        }
        return hurt;
    }
}
