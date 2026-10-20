package com.gregguns.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.fx.FxSender;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The Alien Blaster's bolt: a straight FIRE-damage {@link GenericProjectile} that leaves an {@code AlienBlasterTrail}
 * behind it and ends in a tiny explosion (power 0.5, full damage within 0.25 blocks, none beyond 0.75) that never
 * breaks a block, with the {@code AlienExplosion} flare. A bolt fired by a player also sets what it hits alight (the
 * FIRE type's burning effect, which full GT heat hazmat blocks) and, when {@code explosions.blockDamage} is on, puts
 * fire on the face it hits; a bolt fired by anything else (an NPC, a turret) never ignites.
 *
 * <p>
 * The trail is the reason this class is a registered, tracked entity (PFX-01: Techguns never registered it, so the
 * trail packet found no entity on any client). It is sent on the bolt's second tick, once the server's entity tracker
 * lists the players who see it; {@link #trailSentTick()} records when, for the scenario that proves it.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.9 AlienBlasterProjectile); explosions.md; fixes PFX-01
// Deviation: Techguns ran the explosion on both sides (PFX-15) and let it hurt the entity the bolt had just hit; here
// it runs on the server only and leaves the direct target out (as the rocket does), so a hit is not counted twice.
// Deviation: the burn is the FIRE type's secondary effect (5 s, respects hazmat) rather than a fixed 3 s set-fire.
public class AlienBlasterProjectile extends GenericProjectile {

    /** Mod-local entity ID; Techguns never registered this projectile (PFX-01), so the number is ours. */
    public static final int ENTITY_ID = 27;

    /** FX list that follows the bolt. */
    public static final String TRAIL_FX = "AlienBlasterTrail";

    /** FX list played where the bolt ends. */
    public static final String IMPACT_FX = "AlienExplosion";

    /** The tick on which the trail is sent; the entity tracker has listed the watchers by then. */
    public static final int TRAIL_TICK = 2;

    /** Vanilla power of the tiny blast; drives only the push, since blocks are never broken. */
    public static final float BLAST_SIZE = 0.5F;
    /** Radius of full blast damage, blocks. */
    public static final float BLAST_INNER = 0.25F;
    /** Radius where blast damage reaches zero, blocks. */
    public static final float BLAST_OUTER = 0.75F;

    private boolean ignite;
    private String weaponName = "alien_blaster";
    private int trailSentTick = -1;

    /**
     * @param world the world
     */
    public AlienBlasterProjectile(World world) {
        super(world);
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        ignite = FlameRules.ignites(firedBy instanceof EntityPlayer);
        weaponName = stats.id();
    }

    /** @return true if this bolt ignites what it hits (it was fired by a player) */
    public boolean ignites() {
        return ignite;
    }

    /** @return the bolt's age, in ticks, when it sent its trail FX, or -1 if it has not yet (always -1 on a client) */
    public int trailSentTick() {
        return trailSentTick;
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.FIRE;
    }

    @Override
    protected boolean secondaryEffects() {
        return ignite;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote && !isDead && trailSentTick < 0 && ticksExisted >= TRAIL_TICK) {
            trailSentTick = ticksExisted;
            FxSender.onEntity(this, TRAIL_FX);
        }
    }

    @Override
    protected void onEntityHit(Entity target, double hitX, double hitY, double hitZ) {
        super.onEntityHit(target, hitX, hitY, hitZ);
        blast(hitX, hitY, hitZ, target);
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        super.onBlockHit(hit);
        if (worldObj.isRemote) {
            return;
        }
        if (ignite && ExplosionsConfig.blockDamage) {
            burnFace(hit);
        }
        blast(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, null);
    }

    private void blast(double x, double y, double z, Entity direct) {
        if (worldObj.isRemote) {
            return;
        }
        FxSender.at(worldObj, IMPACT_FX, x, y, z, FxSender.RANGE_NEAR);
        // Never breaks blocks: the request is false, so the gate in ProjectileExplosion is not even consulted.
        new ProjectileExplosion(
            worldObj,
            this,
            shooter,
            weaponName,
            x,
            y,
            z,
            BLAST_SIZE,
            (int) damage,
            BLAST_INNER,
            BLAST_OUTER).excluding(direct)
                .explode(false);
    }

    private void burnFace(MovingObjectPosition hit) {
        if (!(shooter instanceof EntityPlayer)) {
            return;
        }
        Block target = worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        int meta = worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
        if (!BlockBreakGuard
            .mayBreak(worldObj, hit.blockX, hit.blockY, hit.blockZ, target, meta, (EntityPlayer) shooter)) {
            return;
        }
        int[] cell = FlameRules.cellInFront(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);
        if (worldObj.isAirBlock(cell[0], cell[1], cell[2])
            && Blocks.fire.canPlaceBlockAt(worldObj, cell[0], cell[1], cell[2])) {
            worldObj.setBlock(cell[0], cell[1], cell[2], Blocks.fire);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("ignite", ignite);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        ignite = tag.getBoolean("ignite");
    }
}
