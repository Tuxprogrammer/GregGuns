package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.gun.sound.GunSoundEvents;

/**
 * One explosion that hurts entities on a damage ramp and may remove blocks, with the protection hooks the pack needs.
 * Server side only: a client copy does nothing (fixes PFX-15).
 *
 * <p>
 * Order of events, which is the whole point of the class:
 * <ol>
 * <li>Forge's {@code ExplosionEvent.Start} is posted; if a listener cancels it nothing else happens, not even the
 * sound.</li>
 * <li>The blocks the blast reaches are ray-cast into a vanilla {@link Explosion}'s affected-block list, using each
 * block's full-signature resistance (GGS-04-13). The list is computed even when blocks will not break.</li>
 * <li>{@code ExplosionEvent.Detonate} is posted with the entity list and the live block list. Listeners
 * (ServerUtilities
 * claims and spawn protection) may remove entities or blocks (GGS-04-10).</li>
 * <li>Every entity still in the list takes {@link BlastFalloff} damage through {@link DamageSources#strike} and is
 * pushed; the owner is included unless {@code explosions.selfDamage} is off (PFX-25). Players get their push in an
 * {@code S27PacketExplosion} with no blocks (CORE-19).</li>
 * <li>If blocks may break, each block still in the list is offered to {@link BlockBreakGuard} when a player owns the
 * blast, then dropped and exploded with {@code onBlockExploded}, so a GregTech machine in the blast reacts as it
 * does to any explosion (GGS-04-04).</li>
 * </ol>
 *
 * <pre>
 * {@code
 * new ProjectileExplosion(world, grenade, thrower, "stielgranate", x, y, z, 5.0F, 20.0F, 2.5F, 7.5F)
 *     .explode(ExplosionsConfig.blockDamage);
 * }
 * </pre>
 */
// Spec: docs/techguns-spec/04-projectiles-fx/explosions.md; docs/gregguns-spec/04-projectiles-fx/explosions-pack.md
public final class ProjectileExplosion {

    private static final Logger AUDIT = LogManager.getLogger(ExplosionRules.LOGGER_NAME);
    private static final int GRID = 16;
    private static final float RAY_STEP = 0.3F;

    /** What an explosion did. */
    public static final class Result {

        /** Nothing happened: a client world or a cancelled {@code ExplosionEvent.Start}. */
        public static final Result NONE = new Result(false, 0, 0, 0);

        private final boolean happened;
        private final int entitiesHurt;
        private final int blocksReached;
        private final int blocksBroken;

        Result(boolean happened, int entitiesHurt, int blocksReached, int blocksBroken) {
            this.happened = happened;
            this.entitiesHurt = entitiesHurt;
            this.blocksReached = blocksReached;
            this.blocksBroken = blocksBroken;
        }

        /** @return false when the world was a client or a listener cancelled the start */
        public boolean happened() {
            return happened;
        }

        /** @return entities that took damage */
        public int entitiesHurt() {
            return entitiesHurt;
        }

        /** @return blocks still in the affected list after {@code Detonate} */
        public int blocksReached() {
            return blocksReached;
        }

        /** @return blocks actually removed */
        public int blocksBroken() {
            return blocksBroken;
        }
    }

    private final World world;
    private final Entity exploder;
    private final EntityLivingBase owner;
    private final String weapon;
    private final double x;
    private final double y;
    private final double z;
    private final float size;
    private final float damage;
    private final float innerRadius;
    private final float outerRadius;

    /**
     * @param world       the world
     * @param exploder    the entity that explodes (the grenade), never damaged by it; may be {@code null}
     * @param owner       who is responsible (the thrower), may be {@code null}
     * @param weapon      the weapon's name, for the audit log
     * @param x           centre X
     * @param y           centre Y
     * @param z           centre Z
     * @param size        the vanilla explosion power; drives the block ray-cast and the push
     * @param damage      damage at the centre, half hearts
     * @param innerRadius radius of full damage, blocks
     * @param outerRadius radius where damage reaches zero, blocks
     */
    public ProjectileExplosion(World world, Entity exploder, EntityLivingBase owner, String weapon, double x, double y,
        double z, float size, float damage, float innerRadius, float outerRadius) {
        this.world = world;
        this.exploder = exploder;
        this.owner = owner;
        this.weapon = weapon;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.damage = damage;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    /**
     * Runs the explosion. Server side only.
     *
     * @param blockDamage the requested block damage (normally {@code ExplosionsConfig.blockDamage}); the
     *                    {@code mobGriefing} rule can still veto it for an NPC-caused blast
     * @return what happened
     */
    @SuppressWarnings("unchecked")
    public Result explode(boolean blockDamage) {
        if (world.isRemote) {
            return Result.NONE;
        }
        boolean breakBlocks = ExplosionRules.mayBreakBlocks(
            blockDamage,
            owner instanceof EntityPlayer,
            world.getGameRules()
                .getGameRuleBooleanValue("mobGriefing"));
        Explosion vanilla = new Explosion(world, exploder, x, y, z, size);
        vanilla.isSmoking = breakBlocks;
        if (ForgeEventFactory.onExplosionStart(world, vanilla)) {
            return Result.NONE;
        }
        vanilla.affectedBlockPositions.addAll(rayCast());

        List<Entity> victims = new ArrayList<>(
            world.getEntitiesWithinAABBExcludingEntity(
                exploder,
                AxisAlignedBB.getBoundingBox(
                    x - outerRadius - 1,
                    y - outerRadius - 1,
                    z - outerRadius - 1,
                    x + outerRadius + 1,
                    y + outerRadius + 1,
                    z + outerRadius + 1)));
        if (!ExplosionsConfig.selfDamage) {
            victims.remove(owner);
        }
        // Source: the live list is what the listeners edit; ServerUtilities clears and refills it.
        // https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/handlers/ServerUtilitiesWorldEventHandler.java#L105-L139
        ForgeEventFactory.onExplosionDetonate(world, vanilla, victims, size * 2.0);

        int hurt = hurtEntities(victims);
        int reached = vanilla.affectedBlockPositions.size();
        int broken = breakBlocks ? breakBlocks(vanilla) : 0;
        playEffects();
        if (broken > 0) {
            AUDIT.info(
                ExplosionRules.logLine(
                    world.provider.dimensionId,
                    x,
                    y,
                    z,
                    size,
                    owner == null ? null : owner.getCommandSenderName(),
                    weapon,
                    broken));
        }
        return new Result(true, hurt, reached, broken);
    }

    /** The vanilla block ray-cast: 1352 rays from the cube surface, each with a random strength. */
    private Set<ChunkPosition> rayCast() {
        Set<ChunkPosition> reached = new HashSet<>();
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                for (int k = 0; k < GRID; k++) {
                    if (i != 0 && i != GRID - 1 && j != 0 && j != GRID - 1 && k != 0 && k != GRID - 1) {
                        continue;
                    }
                    castRay(
                        i / (GRID - 1.0) * 2.0 - 1.0,
                        j / (GRID - 1.0) * 2.0 - 1.0,
                        k / (GRID - 1.0) * 2.0 - 1.0,
                        reached);
                }
            }
        }
        return reached;
    }

    private void castRay(double dx, double dy, double dz, Set<ChunkPosition> reached) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx = dx / length * RAY_STEP;
        dy = dy / length * RAY_STEP;
        dz = dz / length * RAY_STEP;
        float intensity = size * (0.7F + world.rand.nextFloat() * 0.6F);
        double cx = x;
        double cy = y;
        double cz = z;
        for (; intensity > 0.0F; intensity -= RAY_STEP * 0.75F) {
            int bx = MathHelper.floor_double(cx);
            int by = MathHelper.floor_double(cy);
            int bz = MathHelper.floor_double(cz);
            Block block = world.getBlock(bx, by, bz);
            if (block.getMaterial() != net.minecraft.block.material.Material.air) {
                // The full-signature resistance, so GregTech's override is used (GGS-04-13).
                // Source:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockMachines.java#L686-L695
                float resistance = block.getExplosionResistance(exploder, world, bx, by, bz, x, y, z);
                intensity -= (resistance + RAY_STEP) * RAY_STEP;
            }
            if (intensity > 0.0F) {
                reached.add(new ChunkPosition(bx, by, bz));
            }
            cx += dx;
            cy += dy;
            cz += dz;
        }
    }

    private int hurtEntities(List<Entity> victims) {
        int hurt = 0;
        Map<EntityPlayerMP, Vec3> pushed = new HashMap<>();
        Vec3 centre = Vec3.createVectorHelper(x, y, z);
        for (Entity victim : victims) {
            if (victim == exploder || victim.isDead) {
                continue;
            }
            double dx = victim.posX - x;
            double dy = victim.posY - y;
            double dz = victim.posZ - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double amount = BlastFalloff.damage(damage, innerRadius, outerRadius, distance);
            if (amount <= 0.0) {
                continue;
            }
            Entity attacker = owner != null ? owner : exploder;
            if (DamageSources.strike(victim, DamageType.EXPLOSION, attacker, (float) amount, 0.0)) {
                hurt++;
            }
            push(victim, centre, distance, pushed);
        }
        for (Map.Entry<EntityPlayerMP, Vec3> entry : pushed.entrySet()) {
            // No blocks in the packet: it only carries the push, which vanilla's client applies on receipt.
            entry.getKey().playerNetServerHandler
                .sendPacket(new S27PacketExplosion(x, y, z, size, new ArrayList<ChunkPosition>(), entry.getValue()));
        }
        return hurt;
    }

    private void push(Entity victim, Vec3 centre, double distance, Map<EntityPlayerMP, Vec3> pushed) {
        double dx = victim.posX - x;
        double dy = victim.posY + victim.getEyeHeight() - y;
        double dz = victim.posZ - z;
        double length = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
        if (length == 0.0) {
            return;
        }
        double strength = (1.0 - distance / (size * 2.0)) * world.getBlockDensity(centre, victim.boundingBox);
        if (strength <= 0.0) {
            return;
        }
        if (victim instanceof EntityLivingBase) {
            strength = EnchantmentProtection.func_92092_a(victim, strength);
        }
        double mx = dx / length * strength;
        double my = dy / length * strength;
        double mz = dz / length * strength;
        victim.motionX += mx;
        victim.motionY += my;
        victim.motionZ += mz;
        if (victim instanceof EntityPlayerMP) {
            pushed.put((EntityPlayerMP) victim, Vec3.createVectorHelper(mx, my, mz));
        }
    }

    /** Breaks the blocks that are still listed; the list is read after {@code Detonate} (GGS-04-10). */
    private int breakBlocks(Explosion vanilla) {
        int broken = 0;
        EntityPlayer player = owner instanceof EntityPlayer ? (EntityPlayer) owner : null;
        for (Object o : new ArrayList<Object>(vanilla.affectedBlockPositions)) {
            ChunkPosition pos = (ChunkPosition) o;
            Block block = world.getBlock(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
            if (block.getMaterial() == net.minecraft.block.material.Material.air) {
                continue;
            }
            int meta = world.getBlockMetadata(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
            if (player != null
                && !BlockBreakGuard.mayBreak(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, block, meta, player)) {
                continue;
            }
            if (block.canDropFromExplosion(vanilla)) {
                block.dropBlockAsItemWithChance(
                    world,
                    pos.chunkPosX,
                    pos.chunkPosY,
                    pos.chunkPosZ,
                    meta,
                    1.0F / size,
                    0);
            }
            block.onBlockExploded(world, pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, vanilla);
            broken++;
        }
        return broken;
    }

    private void playEffects() {
        float pitch = (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F;
        world.playSoundEffect(x, y, z, GunSoundEvents.qualified(GunSoundEvents.EXPLOSION), 4.0F, pitch);
        if (world instanceof WorldServer) {
            ((WorldServer) world).func_147487_a("hugeexplosion", x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
