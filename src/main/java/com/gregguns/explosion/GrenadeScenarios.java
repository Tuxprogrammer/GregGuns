package com.gregguns.explosion;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.projectile.GrenadeMath;
import com.gregguns.projectile.GrenadeProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The {@code grenade.*} in-game scenarios: the fuse and blast falloff, the bounce, and the protection rules of the
 * explosion (config, cancelled events, listeners that edit the affected-block list). Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/explosions-pack.md (GGS-04-10, 11, 12, 14); docs/testing/README.md
public final class GrenadeScenarios {

    private static final float EPS = 0.3F;
    private static final String NAME = "[GregGunsGrenadeTest]";
    private static final int RADIUS = 9;

    private GrenadeScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry the registry to add to
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fuse());
        registry.register(new Bounce());
        registry.register(new Protected());
    }

    /** A stone floor with clear air above, 19 by 19, all recorded for cleanup. */
    private static void chamber(ScenarioContext ctx) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                ctx.setBlock(dx, -2, dz, Blocks.stone, 0);
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                for (int dy = 0; dy <= 6; dy++) {
                    ctx.setBlock(dx, dy, dz, Blocks.air, 0);
                }
            }
        }
    }

    /** A zombie that cannot move or burn, with 100 health so a 20 damage blast does not kill it. */
    private static EntityZombie dummy(ScenarioContext ctx, double dx) {
        EntityZombie z = new EntityZombie(ctx.world());
        z.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
        z.setCurrentItemOrArmor(4, new net.minecraft.item.ItemStack(Blocks.pumpkin));
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(100.0);
        z.setHealth(100.0F);
        z.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2000, 10));
        return ctx.spawn(z);
    }

    private static float expectedLoss(EntityLivingBase target, double raw) {
        double armor = target.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.EXPLOSION, raw, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    private static float lost(EntityLivingBase e) {
        return e.getMaxHealth() - e.getHealth();
    }

    private static FakePlayer fakePlayer(ScenarioContext ctx) {
        FakePlayer fake = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(NAME.getBytes()), NAME));
        fake.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 8.5, 0.0F, 0.0F);
        return fake;
    }

    /** A grenade sitting at rest at the given offset, thrown by {@code thrower}. */
    private static GrenadeProjectile placed(ScenarioContext ctx, EntityLivingBase thrower, double dy) {
        GrenadeProjectile g = new GrenadeProjectile(ctx.world());
        g.throwFrom(thrower, 1.0, 20.0F, 5.0F, GrenadeMath.STIELGRANATE_BOUNCES, "stielgranate");
        g.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + dy, ctx.originZ() + 0.5, 0.0F, 0.0F);
        g.motionX = 0.0;
        g.motionY = 0.0;
        g.motionZ = 0.0;
        return ctx.spawn(g);
    }

    /** The grenade explodes when its fuse runs out, and damage falls off with distance. */
    private static final class Fuse implements Scenario {

        private static final int TIMEOUT = GrenadeMath.FUSE_TICKS + 60;

        @Override
        public String name() {
            return "grenade.fuse";
        }

        @Override
        public String description() {
            return "A grenade at rest explodes after its 200 tick fuse, not before; zombies at 1.5, 5 and 8 blocks lose full, ramp and no damage, and the thrower inside the radius is hurt too (PFX-25). Waits about 10 s.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            chamber(ctx);
            EntityZombie near = dummy(ctx, 1.5);
            EntityZombie mid = dummy(ctx, 5.0);
            EntityZombie far = dummy(ctx, 8.0);
            EntityZombie thrower = dummy(ctx, -2.0);
            GrenadeProjectile grenade = placed(ctx, thrower, 0.05);
            boolean[] aliveEarly = { false };
            return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                if (ticks == GrenadeMath.FUSE_TICKS - 20) {
                    aliveEarly[0] = !grenade.isDead && lost(near) == 0.0F;
                }
                if (!grenade.isDead && ticks < TIMEOUT - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(aliveEarly[0], "the grenade should still be live, and nobody hurt, 20 ticks before the fuse");
                out.check(grenade.isDead, "the grenade should have exploded by now");
                out.check(
                    ticks >= GrenadeMath.FUSE_TICKS - 2 && ticks <= GrenadeMath.FUSE_TICKS + 3,
                    "the grenade should explode on the 200th tick, it did at poll " + ticks);
                check(out, grenade, near, "near");
                check(out, grenade, mid, "mid");
                check(out, grenade, far, "far");
                check(out, grenade, thrower, "thrower");
                out.check(lost(far) == 0.0F, "a zombie past 1.5 radii should be untouched, lost " + lost(far));
                out.check(lost(near) > lost(mid) && lost(mid) > 0.0F, "damage should fall with distance");
                out.check(
                    ctx.world()
                        .getBlock(ctx.originX(), ctx.originY() - 1, ctx.originZ()) == Blocks.stone,
                    "with explosions.blockDamage off the floor should be intact");
                return out.build();
            });
        }

        private static void check(ScenarioResult.Builder out, GrenadeProjectile g, EntityLivingBase e, String label) {
            double dx = e.posX - g.posX;
            double dy = e.posY - g.posY;
            double dz = e.posZ - g.posZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float expected = expectedLoss(e, BlastFalloff.damage(20.0, 2.5, 7.5, distance));
            out.check(
                Math.abs(expected - lost(e)) < EPS,
                label + " (distance " + distance + ") should lose " + expected + " but lost " + lost(e));
        }
    }

    /** A dropped grenade bounces, loses bounces, and settles for its fuse. */
    private static final class Bounce implements Scenario {

        @Override
        public String name() {
            return "grenade.bounce";
        }

        @Override
        public String description() {
            return "A grenade dropped from 3 blocks bounces off the floor (bounces left falls, still live), then settles on the floor instead of spending every bounce at once.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            chamber(ctx);
            FakePlayer owner = fakePlayer(ctx);
            GrenadeProjectile grenade = placed(ctx, owner, 3.0);
            return ScenarioResult.pending(name(), 80, ticks -> {
                if (ticks < 50) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!grenade.isDead, "the grenade should still be live after 50 ticks (fuse 200)");
                out.check(
                    grenade.getBouncesLeft() < GrenadeMath.STIELGRANATE_BOUNCES,
                    "it should have bounced at least once, bounces left " + grenade.getBouncesLeft());
                out.check(grenade.getBouncesLeft() >= 0, "bounces left should not go negative");
                double height = grenade.posY - ctx.originY();
                out.check(height >= -0.1 && height < 0.6, "it should lie on the floor, height " + height);
                out.check(Math.abs(grenade.motionY) < 0.1, "it should have settled, motionY " + grenade.motionY);
                return out.build();
            });
        }
    }

    /** What a trial of the explosion did. */
    private static final class Trial {

        final ProjectileExplosion.Result result;
        final float victimLost;
        final float ownerLost;
        final boolean floorIntact;

        Trial(ProjectileExplosion.Result result, float victimLost, float ownerLost, boolean floorIntact) {
            this.result = result;
            this.victimLost = victimLost;
            this.ownerLost = ownerLost;
            this.floorIntact = floorIntact;
        }
    }

    /** Cancels the explosion before it starts. */
    public static final class CancelStart {

        /**
         * @param event the start event
         */
        @SubscribeEvent
        public void onStart(ExplosionEvent.Start event) {
            event.setCanceled(true);
        }
    }

    /** Clears the affected-block list, as ServerUtilities does for protected blocks. */
    public static final class ClearBlocks {

        /**
         * @param event the detonate event
         */
        @SubscribeEvent
        public void onDetonate(ExplosionEvent.Detonate event) {
            event.getAffectedBlocks()
                .clear();
        }
    }

    /** Cancels every break, as a claim does. */
    public static final class CancelBreaks {

        /** How many break events it saw. */
        public int seen;

        /**
         * @param event the break event
         */
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            seen++;
            event.setCanceled(true);
        }
    }

    /** The explosion honors the config flag and every protection hook. */
    private static final class Protected implements Scenario {

        @Override
        public String name() {
            return "grenade.protected";
        }

        @Override
        public String description() {
            return "Explosion protection: block damage off or a cancelled ExplosionEvent.Start breaks nothing, a Detonate listener that clears the list or a cancelled BreakEvent keeps the floor, entity damage still lands (GGS-04-11), block damage on breaks the floor, selfDamage off spares the owner.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            try {
                Trial off = trial(ctx, false, true, null);
                out.check(off.result.happened(), "with block damage off the explosion should still happen");
                out.check(off.result.blocksReached() > 0, "the reach is still computed with block damage off");
                out.check(off.result.blocksBroken() == 0 && off.floorIntact, "block damage off: nothing may break");
                out.check(off.victimLost > 0.0F, "block damage off: entities are still hurt, lost " + off.victimLost);

                Trial on = trial(ctx, true, true, null);
                out.check(
                    on.result.blocksBroken() > 0 && !on.floorIntact,
                    "block damage on: the floor should break, broke " + on.result.blocksBroken());
                out.check(Math.abs(on.victimLost - off.victimLost) < EPS, "block damage must not change entity damage");

                Trial canceled = trial(ctx, true, true, new CancelStart());
                out.check(
                    !canceled.result.happened() && canceled.victimLost == 0.0F && canceled.floorIntact,
                    "a cancelled ExplosionEvent.Start must stop all effects, victim lost " + canceled.victimLost);

                Trial cleared = trial(ctx, true, true, new ClearBlocks());
                out.check(
                    cleared.result.blocksBroken() == 0 && cleared.floorIntact,
                    "a Detonate listener that clears the list must keep every block (GGS-04-10)");
                out.check(
                    Math.abs(cleared.victimLost - off.victimLost) < EPS && cleared.victimLost > 0.0F,
                    "clearing the block list must not change entity damage (GGS-04-11), lost " + cleared.victimLost);

                CancelBreaks breaks = new CancelBreaks();
                Trial denied = trial(ctx, true, true, breaks);
                out.check(breaks.seen > 0, "a player's blast should post a BreakEvent per block (GGS-04-12)");
                out.check(
                    denied.result.blocksBroken() == 0 && denied.floorIntact,
                    "cancelled BreakEvents must keep the blocks, broke " + denied.result.blocksBroken());

                Trial self = trial(ctx, false, true, null);
                Trial noSelf = trial(ctx, false, false, null);
                out.check(self.ownerLost > 0.0F, "the owner should be hurt by their own blast (PFX-25)");
                out.check(
                    noSelf.ownerLost == 0.0F,
                    "with selfDamage off the owner is spared, lost " + noSelf.ownerLost);
            } finally {
                ExplosionsConfig.blockDamage = savedBlocks;
                ExplosionsConfig.selfDamage = savedSelf;
            }
            return out.build();
        }

        private static Trial trial(ScenarioContext ctx, boolean blockDamage, boolean selfDamage, Object listener) {
            chamber(ctx);
            // The owner is a fake player for block-breaking trials (BreakEvent needs a player) and a zombie for the
            // self-damage trial, which can be hurt.
            EntityZombie victim = dummy(ctx, 3.0);
            EntityZombie zombieOwner = dummy(ctx, -2.0);
            EntityLivingBase owner = listener instanceof CancelBreaks || blockDamage ? fakePlayer(ctx) : zombieOwner;
            ExplosionsConfig.selfDamage = selfDamage;
            if (listener != null) {
                MinecraftForge.EVENT_BUS.register(listener);
            }
            ProjectileExplosion.Result result;
            try {
                result = new ProjectileExplosion(
                    ctx.world(),
                    null,
                    owner,
                    "stielgranate",
                    ctx.originX() + 0.5,
                    ctx.originY() + 0.5,
                    ctx.originZ() + 0.5,
                    5.0F,
                    20.0F,
                    2.5F,
                    7.5F).explode(blockDamage);
            } finally {
                if (listener != null) {
                    MinecraftForge.EVENT_BUS.unregister(listener);
                }
            }
            boolean intact = true;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    intact &= ctx.world()
                        .getBlock(ctx.originX() + dx, ctx.originY() - 1, ctx.originZ() + dz) == Blocks.stone;
                }
            }
            Trial t = new Trial(result, lost(victim), lost(zombieOwner), intact);
            ctx.despawn(victim);
            ctx.despawn(zombieOwner);
            return t;
        }
    }
}
