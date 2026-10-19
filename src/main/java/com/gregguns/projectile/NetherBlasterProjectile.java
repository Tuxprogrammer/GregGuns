package com.gregguns.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.fx.FxSender;

/**
 * The Nether Blaster's bolt: a slow (0.85 blocks per tick), straight {@link GenericProjectile} that deals FIRE damage
 * with the gun's drop-off, burns what it hits, and may set fire to the block it lands on. It dies in water. The server
 * sends the {@code CyberDemonBlasterTrail} FX on the bolt once; the impact FX ({@code NetherBlasterImpactFlare} and
 * {@code MediumExplosionSmoke}) go out from the server on both block and entity hits.
 *
 * <p>
 * Fire on blocks follows {@link FlameRules}: only a player's shot, only with {@code explosions.blockDamage} on, half
 * the time, and only where the shooter may break the block ({@link BlockBreakGuard}), so protection mods apply.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.8 CyberdemonBlasterProjectile);
// docs/techguns-spec/02-guns/sheets-energy-special.md (Nether Blaster); BUGS.md GUN-05, PFX-05
// Deviation (PFX-05): Techguns made the entity-hit impact FX in a server-only hook, so a dedicated server never showed
// it. The server sends it as a packet here.
// Deviation (GUN-05): Techguns' ignite flag was also the block-damage flag and was always on for NPC shooters. Here the
// bolt burns what it hits for any shooter (the FIRE type's burning), but block fire needs a player, block damage on and
// build rights, as for the flame (FlameRules).
// Deviation: Techguns' bolt did not set its target alight despite its ignite time field; the plan (PLAN-M5 section 2)
// says the bolt ignites, so it does.
public class NetherBlasterProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its Nether Blaster bolt. */
    public static final int ENTITY_ID = 13;

    /** The trail FX list, sent on the bolt. */
    public static final String TRAIL_FX = "CyberDemonBlasterTrail";
    /** The impact flare FX list. */
    public static final String IMPACT_FX = "NetherBlasterImpactFlare";
    /** The impact smoke FX list. */
    public static final String SMOKE_FX = "MediumExplosionSmoke";

    private boolean trailSent;

    /**
     * @param world the world
     */
    public NetherBlasterProjectile(World world) {
        super(world);
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.FIRE;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (isDead) {
            return;
        }
        if (isInWater()) {
            // Spec: the bolt dies in water (projectiles.md summary table).
            setDead();
            return;
        }
        if (!worldObj.isRemote && !trailSent) {
            trailSent = true;
            FxSender.onEntity(this, TRAIL_FX);
        }
    }

    @Override
    protected void onHitEffect(Entity target) {
        impactFx(posX, posY, posZ);
    }

    @Override
    protected void onBlockHit(MovingObjectPosition hit) {
        super.onBlockHit(hit);
        if (worldObj.isRemote) {
            return;
        }
        impactFx(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
        if (!(shooter instanceof EntityPlayer)) {
            return;
        }
        if (!FlameRules.placesFire(true, ExplosionsConfig.blockDamage, worldObj.rand.nextDouble())) {
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

    private void impactFx(double x, double y, double z) {
        if (worldObj.isRemote) {
            return;
        }
        FxSender.at(worldObj, IMPACT_FX, x, y, z, FxSender.RANGE_NEAR);
        FxSender.at(worldObj, SMOKE_FX, x, y, z, FxSender.RANGE_NEAR);
    }
}
