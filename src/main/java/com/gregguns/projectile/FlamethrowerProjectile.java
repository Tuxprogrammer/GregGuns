package com.gregguns.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The Flamethrower's flame: a short-lived {@link GenericProjectile} that deals FIRE damage with the gun's drop-off,
 * sags a little (gravity 0.01), and dies in water. Only a flame fired by a player sets what it hits alight (through
 * the FIRE type's burning effect, which full GT heat hazmat blocks) and may place fire on a block, and block fire also
 * needs {@code explosions.blockDamage} and a player who may break the block they hit ({@link BlockBreakGuard}), so
 * protection mods apply. See {@link FlameRules}.
 *
 * <p>
 * The client copy only draws flame and smoke particles along the path; a client never decides a hit.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.2); docs/techguns-spec/02-guns/sheets-energy-special.md
// (Flamethrower); docs/gregguns-spec/02-guns/index.md (batch B6)
// Deviation: burning lasts the FIRE type's default (5 s, damage-interop.md) rather than Techguns' fixed 3 s, and the
// fire placement keeps only the plain case (FlameRules). Techguns' block-fire flag was also the block-damage flag and
// was set for NPC Nether Blasters (GUN-05); here the switches are separate.
public class FlamethrowerProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its flame. */
    public static final int ENTITY_ID = 3;

    private boolean ignite;

    /**
     * @param world the world
     */
    public FlamethrowerProjectile(World world) {
        super(world);
        gravity = 0.01F;
    }

    @Override
    public void fire(EntityLivingBase firedBy, GunStats stats, ShotOffset offset, float damageScale) {
        super.fire(firedBy, stats, offset, damageScale);
        gravity = (float) stats.projectileGravity();
        ignite = FlameRules.ignites(firedBy instanceof EntityPlayer);
    }

    /** @return true if this flame ignites what it hits (it was fired by a player) */
    public boolean ignites() {
        return ignite;
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
        if (isDead) {
            return;
        }
        if (isInWater()) {
            // Spec: the flame dies in water (projectiles.md summary table).
            setDead();
            return;
        }
        if (worldObj.isRemote) {
            worldObj.spawnParticle("flame", posX, posY, posZ, motionX * 0.1, motionY * 0.1 - gravity, motionZ * 0.1);
            if (ticksExisted % 3 == 0) {
                worldObj.spawnParticle("smoke", posX, posY, posZ, 0.0, 0.02, 0.0);
            }
        }
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        if (worldObj.isRemote) {
            worldObj.spawnParticle("lava", hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 0, 0, 0);
            return;
        }
        if (!FlameRules.placesFire(ignite, ExplosionsConfig.blockDamage, worldObj.rand.nextDouble())) {
            return;
        }
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
