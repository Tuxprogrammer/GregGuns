package com.gregguns.icbm.warhead.effects;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;
import com.gregguns.icbm.warhead.WarheadBlast;

/**
 * The effects of the T1 and T2 blast types (gas, fire, push, sonic, breaching and the small status effects), as
 * {@link BlastEffect}s for the shared registry. The engine already did terrain and the damage ramp; these add the rest.
 * Every block change asks {@link BlockBreakGuard} and obeys {@code icbm.terrainDamageT<n>}. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md
public final class Effects {

    /** Gas clouds last this long, ticks (30 s). */
    public static final int GAS_TICKS = 600;
    /** How often a gas cloud acts, ticks. */
    public static final int GAS_PERIOD = 10;
    /** Depth of a breaching tunnel, blocks. */
    public static final int BREACH_DEPTH = 24;
    /** Hardest block a breaching tunnel or a sonic blast removes. */
    public static final float BREACH_MAX_HARDNESS = 50.0F;
    /** Most fire blocks one incendiary blast places. */
    public static final int FIRE_MAX_BLOCKS = 400;

    private Effects() {}

    /**
     * @param kind a blast kind
     * @return its effect; {@link BlastEffect#NONE} for a kind without one
     */
    public static BlastEffect forKind(WarheadBlast.Kind kind) {
        switch (kind) {
            case SHRAPNEL:
                return new Burn(5, 0, 0);
            case ANVIL:
                return new Burn(0, 200, 2);
            case FIRE:
                return new Fire();
            case GAS_DEBILITATION:
                return new Gas(
                    "spell",
                    new PotionEffect(Potion.moveSlowdown.id, 60, 1),
                    new PotionEffect(Potion.weakness.id, 60, 1),
                    new PotionEffect(Potion.confusion.id, 80, 0));
            case GAS_CHEMICAL:
                return new Gas("spell", new PotionEffect(Potion.poison.id, 100, 1));
            case GAS_CONTAGIOUS:
                return new Gas(
                    "spell",
                    new PotionEffect(Potion.poison.id, 120, 3),
                    new PotionEffect(Potion.hunger.id, 120, 1));
            case SMOKE:
                return new Gas("largesmoke");
            case REPEL:
                return new Push(true);
            case ATTRACT:
                return new Push(false);
            case SONIC:
                return new Sonic();
            case BREACH:
                return new Breach();
            default:
                return BlastEffect.NONE;
        }
    }

    @SuppressWarnings("unchecked")
    static List<EntityLivingBase> living(World world, double x, double y, double z, double r) {
        return world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r));
    }

    static double distance(Entity e, double x, double y, double z) {
        double dx = e.posX - x;
        double dy = e.posY + e.height / 2.0 - y;
        double dz = e.posZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** @return true when blocks may be changed by this blast: the config allows it and the guard does not object */
    static boolean mayChange(BlastContext ctx, int x, int y, int z) {
        if (!IcbmConfig.terrainDamage(
            ctx.explosive()
                .tier()
                .number())) {
            return false;
        }
        Block block = ctx.world()
            .getBlock(x, y, z);
        EntityPlayer p = ctx.source() instanceof EntityPlayer ? (EntityPlayer) ctx.source() : null;
        return p == null || BlockBreakGuard.mayBreak(
            ctx.world(),
            x,
            y,
            z,
            block,
            ctx.world()
                .getBlockMetadata(x, y, z),
            p);
    }

    static void particles(BlastContext ctx, String name, int count, double spread) {
        if (ctx.world() instanceof WorldServer) {
            ((WorldServer) ctx.world())
                .func_147487_a(name, ctx.x(), ctx.y(), ctx.z(), count, spread, spread / 2.0, spread, 0.02);
        }
    }

    /** Sets everything in the zone alight and/or slows it (shrapnel and anvil fragments). */
    static final class Burn implements BlastEffect {

        private final int fireSeconds;
        private final int slowTicks;
        private final int slowLevel;

        Burn(int fireSeconds, int slowTicks, int slowLevel) {
            this.fireSeconds = fireSeconds;
            this.slowTicks = slowTicks;
            this.slowLevel = slowLevel;
        }

        @Override
        public void onDetonate(BlastContext ctx) {
            for (EntityLivingBase e : living(ctx.world(), ctx.x(), ctx.y(), ctx.z(), ctx.radius())) {
                if (distance(e, ctx.x(), ctx.y(), ctx.z()) > ctx.radius()) {
                    continue;
                }
                if (fireSeconds > 0) {
                    e.setFire(fireSeconds);
                }
                if (slowTicks > 0) {
                    e.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, slowTicks, slowLevel));
                }
            }
            particles(ctx, "largeexplode", 8, ctx.radius() / 4.0);
        }
    }

    /** A cloud that applies potion effects to everything in it every {@link #GAS_PERIOD} ticks. */
    static final class Gas implements BlastEffect {

        private final String particle;
        private final PotionEffect[] effects;

        Gas(String particle, PotionEffect... effects) {
            this.particle = particle;
            this.effects = effects;
        }

        @Override
        public boolean onTick(BlastContext ctx, int age) {
            if (age % GAS_PERIOD == 0) {
                particles(ctx, particle, 24, ctx.radius() / 2.0);
                for (EntityLivingBase e : living(ctx.world(), ctx.x(), ctx.y(), ctx.z(), ctx.radius())) {
                    if (distance(e, ctx.x(), ctx.y(), ctx.z()) > ctx.radius()) {
                        continue;
                    }
                    for (PotionEffect pe : effects) {
                        e.addPotionEffect(new PotionEffect(pe.getPotionID(), pe.getDuration(), pe.getAmplifier()));
                    }
                }
            }
            return age < GAS_TICKS;
        }
    }

    /** Fire on the ground and on every living thing in the zone. */
    static final class Fire implements BlastEffect {

        @Override
        public void onDetonate(BlastContext ctx) {
            World world = ctx.world();
            int r = (int) Math.ceil(ctx.radius());
            int cx = (int) Math.floor(ctx.x());
            int cy = (int) Math.floor(ctx.y());
            int cz = (int) Math.floor(ctx.z());
            for (EntityLivingBase e : living(world, ctx.x(), ctx.y(), ctx.z(), ctx.radius())) {
                if (distance(e, ctx.x(), ctx.y(), ctx.z()) <= ctx.radius()) {
                    e.setFire(10);
                }
            }
            int placed = 0;
            for (int dx = -r; dx <= r && placed < FIRE_MAX_BLOCKS; dx++) {
                for (int dz = -r; dz <= r && placed < FIRE_MAX_BLOCKS; dz++) {
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    for (int dy = r; dy >= -r; dy--) {
                        int x = cx + dx;
                        int y = cy + dy;
                        int z = cz + dz;
                        if (y < 1 || y > 255
                            || !world.isAirBlock(x, y, z)
                            || !world.getBlock(x, y - 1, z)
                                .isSideSolid(world, x, y - 1, z, net.minecraftforge.common.util.ForgeDirection.UP)) {
                            continue;
                        }
                        if (world.rand.nextInt(3) == 0 && mayChange(ctx, x, y, z)) {
                            world.setBlock(x, y, z, net.minecraft.init.Blocks.fire);
                            placed++;
                        }
                        break;
                    }
                }
            }
            particles(ctx, "flame", 40, ctx.radius() / 2.0);
        }
    }

    /** Throws entities away from the center, or pulls them in. */
    static final class Push implements BlastEffect {

        private static final double STRENGTH = 2.0;
        private final boolean away;

        Push(boolean away) {
            this.away = away;
        }

        @Override
        public void onDetonate(BlastContext ctx) {
            for (EntityLivingBase e : living(ctx.world(), ctx.x(), ctx.y(), ctx.z(), ctx.radius())) {
                double d = distance(e, ctx.x(), ctx.y(), ctx.z());
                if (d > ctx.radius() || d < 1.0E-3) {
                    continue;
                }
                double scale = STRENGTH * (1.0 - d / ctx.radius()) * (away ? 1.0 : -1.0);
                e.motionX += (e.posX - ctx.x()) / d * scale;
                e.motionY += ((e.posY + e.height / 2.0 - ctx.y()) / d) * scale * 0.5 + (away ? 0.4 : 0.0);
                e.motionZ += (e.posZ - ctx.z()) / d * scale;
                e.velocityChanged = true;
            }
            particles(ctx, away ? "explode" : "portal", 30, ctx.radius() / 3.0);
        }
    }

    /** Shatters glass and stuns everything in the zone. */
    static final class Sonic implements BlastEffect {

        @Override
        public void onDetonate(BlastContext ctx) {
            World world = ctx.world();
            for (EntityLivingBase e : living(world, ctx.x(), ctx.y(), ctx.z(), ctx.radius())) {
                if (distance(e, ctx.x(), ctx.y(), ctx.z()) <= ctx.radius()) {
                    e.addPotionEffect(new PotionEffect(Potion.confusion.id, 200, 0));
                    e.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 100, 2));
                }
            }
            int r = (int) Math.ceil(ctx.radius());
            int cx = (int) Math.floor(ctx.x());
            int cy = (int) Math.floor(ctx.y());
            int cz = (int) Math.floor(ctx.z());
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz > ctx.radius() * ctx.radius()) {
                            continue;
                        }
                        Block b = world.getBlock(cx + dx, cy + dy, cz + dz);
                        if (b.getMaterial() == net.minecraft.block.material.Material.glass
                            && mayChange(ctx, cx + dx, cy + dy, cz + dz)) {
                            world.setBlockToAir(cx + dx, cy + dy, cz + dz);
                        }
                    }
                }
            }
            particles(ctx, "explode", 20, ctx.radius() / 3.0);
        }
    }

    /** Bores a 3x3 tunnel straight down from the center, stopping at the first block that is too hard. */
    static final class Breach implements BlastEffect {

        @Override
        public void onDetonate(BlastContext ctx) {
            World world = ctx.world();
            int cx = (int) Math.floor(ctx.x());
            int cy = (int) Math.floor(ctx.y());
            int cz = (int) Math.floor(ctx.z());
            // Collect the tunnel first, so it goes through the same Detonate event and break path as every blast.
            net.minecraft.world.Explosion vanilla = new net.minecraft.world.Explosion(
                world,
                null,
                ctx.x(),
                ctx.y(),
                ctx.z(),
                (float) Math.max(ctx.radius(), 3.0));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int step = 0; step < BREACH_DEPTH; step++) {
                        int x = cx + dx;
                        int y = cy - step;
                        int z = cz + dz;
                        if (y < 1) {
                            break;
                        }
                        Block b = world.getBlock(x, y, z);
                        if (b.isAir(world, x, y, z)) {
                            continue;
                        }
                        float h = b.getBlockHardness(world, x, y, z);
                        if (h < 0 || h > BREACH_MAX_HARDNESS || !mayChange(ctx, x, y, z)) {
                            break;
                        }
                        vanilla.affectedBlockPositions.add(new net.minecraft.world.ChunkPosition(x, y, z));
                    }
                }
            }
            // Listeners (claims, protection) may veto or trim the list; the survivors break through breakBlock.
            net.minecraftforge.event.ForgeEventFactory
                .onExplosionDetonate(world, vanilla, new java.util.ArrayList<net.minecraft.entity.Entity>(), 0.0);
            EntityPlayer breaker = ctx.source() instanceof EntityPlayer ? (EntityPlayer) ctx.source() : null;
            for (Object o : vanilla.affectedBlockPositions) {
                net.minecraft.world.ChunkPosition pos = (net.minecraft.world.ChunkPosition) o;
                ProjectileExplosion.breakBlock(
                    world,
                    vanilla,
                    breaker,
                    (float) vanilla.explosionSize,
                    pos.chunkPosX,
                    pos.chunkPosY,
                    pos.chunkPosZ,
                    IcbmConfig.blastDrops);
            }
            particles(ctx, "smoke", 20, 1.0);
        }
    }
}
