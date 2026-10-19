package com.gregguns.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageFalloff;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.EntityRegistry;

/**
 * The {@code projectile.*} in-game scenarios. Each lays a stone-floored, air-roofed corridor east from the origin,
 * fires through the installed {@link ProjectileLauncher} and waits for server ticks to pass before checking the
 * outcome. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2); docs/testing/README.md
final class ProjectileScenarios {

    private static final float EPS = 0.05F;
    /** Facing +X (east). */
    private static final float EAST_YAW = -90.0F;
    private static final int GIVE_UP_TICKS = 40;
    private static final String SHOOTER_NAME = "[GregGunsProjectileTest]";

    private ProjectileScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Hit());
        registry.register(new Falloff());
        registry.register(new WallStop());
        registry.register(new OwnShooter());
        HitscanScenarios.registerAll(registry);
    }

    /** Stone floor and clear air for {@code length} blocks east of the origin, three wide. */
    static void corridor(ScenarioContext ctx, int length) {
        for (int dx = -1; dx <= length; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                for (int dy = 0; dy <= 3; dy++) {
                    ctx.setBlock(dx, dy, dz, Blocks.air, 0);
                }
            }
        }
    }

    static FakePlayer shooter(ScenarioContext ctx) {
        FakePlayer fake = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(SHOOTER_NAME.getBytes()), SHOOTER_NAME));
        fake.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, EAST_YAW, 0.0F);
        fake.rotationYawHead = EAST_YAW;
        return fake;
    }

    static EntityZombie zombie(ScenarioContext ctx, int dx) {
        EntityZombie z = new EntityZombie(ctx.world());
        z.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
        // A pumpkin is not armor, but it stops the zombie burning in daylight.
        z.setCurrentItemOrArmor(4, new ItemStack(Blocks.pumpkin));
        return ctx.spawn(z);
    }

    /** One shot with no spread, through the installed launcher. */
    private static List<GenericProjectile> fire(ScenarioContext ctx, EntityLivingBase from, GunStats stats) {
        ShotOffset straight = SpreadModel.roll(ctx.random(), stats, ShotContext.npc(0.0), 0)[0];
        int before = ctx.world().loadedEntityList.size();
        Projectiles.launcher()
            .launch(ctx.world(), from, stats, straight, 1.0F);
        List<GenericProjectile> shots = new ArrayList<>();
        for (Object o : ctx.world().loadedEntityList.subList(before, ctx.world().loadedEntityList.size())) {
            if (o instanceof GenericProjectile) {
                shots.add((GenericProjectile) o);
            }
        }
        return shots;
    }

    private static boolean allDead(List<GenericProjectile> shots) {
        for (GenericProjectile p : shots) {
            if (!p.isDead) {
                return false;
            }
        }
        return true;
    }

    /** Health the bullet should take off a target with this much armor, by the damage model. */
    static float expectedLoss(EntityLivingBase target, double rawDamage) {
        double armor = target.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.PROJECTILE, rawDamage, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    static boolean near(float expected, float actual) {
        return Math.abs(expected - actual) < EPS;
    }

    private static boolean registered() {
        return EntityRegistry.instance()
            .lookupModSpawn(GenericProjectile.class, false) != null;
    }

    /** A 9mm shot at a zombie five blocks away, with a corpse in the way that must not stop it. */
    private static final class Hit implements Scenario {

        @Override
        public String name() {
            return "projectile.hit";
        }

        @Override
        public String description() {
            return "A 9mm shot through the launcher skips a corpse and costs a zombie the full 8 damage (DamageModel).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            corridor(ctx, 10);
            FakePlayer gunman = shooter(ctx);
            EntityZombie corpse = zombie(ctx, 3);
            corpse.setHealth(0.0F);
            EntityZombie target = zombie(ctx, 6);
            List<GenericProjectile> shots = fire(ctx, gunman, GunCatalog.PISTOL);
            return ScenarioResult.pending(name(), GIVE_UP_TICKS, ticks -> {
                if (!allDead(shots) && ticks < GIVE_UP_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(shots.size() == 1, "the launcher should spawn exactly one projectile, got " + shots.size());
                out.check(registered(), "GenericProjectile should be registered with FML (PFX-01)");
                out.check(allDead(shots), "the bullet should be gone after the hit");
                float lost = target.getMaxHealth() - target.getHealth();
                out.check(
                    near(expectedLoss(target, GunCatalog.PISTOL.damage()), lost),
                    "zombie should lose " + expectedLoss(target, GunCatalog.PISTOL.damage()) + " but lost " + lost);
                return out.build();
            });
        }
    }

    /** The same shot from past the drop-off start. */
    private static final class Falloff implements Scenario {

        private static final int RANGE = 21;

        @Override
        public String name() {
            return "projectile.falloff";
        }

        @Override
        public String description() {
            return "A 9mm shot at a zombie 21 blocks away does the drop-off damage (full to 18 blocks, 5 at 25).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            corridor(ctx, RANGE + 3);
            FakePlayer gunman = shooter(ctx);
            EntityZombie target = zombie(ctx, RANGE);
            List<GenericProjectile> shots = fire(ctx, gunman, GunCatalog.PISTOL);
            double startX = shots.isEmpty() ? 0.0 : shots.get(0).posX;
            return ScenarioResult.pending(name(), GIVE_UP_TICKS, ticks -> {
                if (!allDead(shots) && ticks < GIVE_UP_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                GunStats pistol = GunCatalog.PISTOL;
                // The bullet enters the target's box 0.3 (the hit margin) before its near face; the path is level.
                double traveled = target.boundingBox.minX - 0.3 - startX;
                double raw = DamageFalloff
                    .damageAt(pistol.damage(), pistol.damageMin(), pistol.dropStart(), pistol.dropEnd(), traveled);
                float expected = expectedLoss(target, raw);
                float lost = target.getMaxHealth() - target.getHealth();
                out.check(
                    raw < pistol.damage() && raw > pistol.damageMin(),
                    "test setup: " + traveled + " blocks should be inside the drop-off band, raw " + raw);
                out.check(
                    near(expected, lost),
                    "zombie should lose " + expected + " at " + traveled + " but lost " + lost);
                return out.build();
            });
        }
    }

    /** A wall stops the bullet, a plant does not, and nothing is left behind. */
    private static final class WallStop implements Scenario {

        @Override
        public String name() {
            return "projectile.blocks";
        }

        @Override
        public String description() {
            return "A bullet flies through tall grass (PFX-27), stops at a stone wall, spares the zombie behind it (PFX-23) and leaves no entity.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            corridor(ctx, 10);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    ctx.setBlock(4, dy, dz, Blocks.stone, 0);
                }
                ctx.setBlock(2, 0, dz, Blocks.tallgrass, 1);
            }
            FakePlayer gunman = shooter(ctx);
            EntityZombie behind = zombie(ctx, 7);
            List<GenericProjectile> shots = fire(ctx, gunman, GunCatalog.PISTOL);
            double wallX = ctx.originX() + 4.0;
            return ScenarioResult.pending(name(), GIVE_UP_TICKS, ticks -> {
                if (!allDead(shots) && ticks < GIVE_UP_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!shots.isEmpty() && allDead(shots), "the bullet should have ended at the wall");
                for (GenericProjectile p : shots) {
                    out.check(!ctx.world().loadedEntityList.contains(p), "the bullet should be out of the entity list");
                    // The bullet ends on the tick it meets the wall, from the last position before it.
                    out.check(
                        p.posX > ctx.originX() + 3.0 && p.posX < wallX,
                        "bullet ended at x=" + p.posX + ": tall grass must not stop it (PFX-27)");
                }
                out.check(
                    near(0.0F, behind.getMaxHealth() - behind.getHealth()),
                    "the zombie behind the wall should be unhurt (PFX-23)");
                return out.build();
            });
        }
    }

    /** A shooter standing in its own bullet's path is not hit by it. */
    private static final class OwnShooter implements Scenario {

        @Override
        public String name() {
            return "projectile.shooter";
        }

        @Override
        public String description() {
            return "A pig fires a 9mm shot from inside its own box: the pig is not hurt, the pig 3 blocks away is.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            corridor(ctx, 8);
            EntityPig gunPig = new EntityPig(ctx.world());
            gunPig.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, EAST_YAW, 0.0F);
            gunPig.rotationYawHead = EAST_YAW;
            ctx.spawn(gunPig);
            EntityPig targetPig = new EntityPig(ctx.world());
            targetPig.setLocationAndAngles(ctx.originX() + 3.5, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            ctx.spawn(targetPig);
            List<GenericProjectile> shots = fire(ctx, gunPig, GunCatalog.PISTOL);
            return ScenarioResult.pending(name(), GIVE_UP_TICKS, ticks -> {
                if (!allDead(shots) && ticks < GIVE_UP_TICKS - 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    near(gunPig.getMaxHealth(), gunPig.getHealth()),
                    "the shooter should be unhurt, health " + gunPig.getHealth());
                out.check(
                    near(
                        expectedLoss(targetPig, GunCatalog.PISTOL.damage()),
                        targetPig.getMaxHealth() - targetPig.getHealth()),
                    "the target pig should lose the full damage, health " + targetPig.getHealth());
                return out.build();
            });
        }
    }
}
