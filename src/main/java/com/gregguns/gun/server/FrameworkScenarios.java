package com.gregguns.gun.server;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.BulletLauncher;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.Projectiles;
import com.gregguns.projectile.SimpleProjectile;

/**
 * Scenarios for the M2 gun-framework extensions: projectile kinds, multi-pellet shots and the knockback multiplier.
 * They fire through {@link Projectiles#launcher()} with throwaway {@link GunStats}, so no real shotgun or hand cannon
 * is
 * needed. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 5 (pellets), damage-system.md 5.2 (knockback, GUN-38)
public final class FrameworkScenarios {

    private FrameworkScenarios() {}

    /**
     * Registers the framework scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Kinds());
        registry.register(new Pellets());
        registry.register(new Patterns());
        registry.register(new KnockbackStrength());
    }

    private static GunStats.Builder base(String id) {
        return GunStats.builder(id)
            .damage(4.0)
            .ammoType("ammo_9mm_bullet")
            .magazine(6)
            .projectile(1.5, 25);
    }

    private static void launch(ScenarioContext ctx, FakePlayer shooter, GunStats stats, long seed) {
        ShotOffset[] offsets = SpreadModel.roll(new Random(seed), stats, ShotContext.DEFAULT, 0);
        for (ShotOffset offset : offsets) {
            Projectiles.launcher()
                .launch(ctx.world(), shooter, stats, offset, 1.0f);
        }
    }

    /** Projectiles in the world now that were not in {@code baseline} (earlier scenarios may leave bullets flying). */
    private static Map<GenericProjectile, Boolean> bulletsIn(ScenarioContext ctx,
        Map<GenericProjectile, Boolean> baseline) {
        Map<GenericProjectile, Boolean> seen = new IdentityHashMap<>();
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof GenericProjectile && !baseline.containsKey(o)) {
                seen.put((GenericProjectile) o, Boolean.TRUE);
            }
        }
        return seen;
    }

    /** Ends test bullets so they cannot fly on into the next scenario's targets. */
    private static void endAll(Map<GenericProjectile, Boolean> bullets) {
        for (GenericProjectile p : bullets.keySet()) {
            p.setDead();
        }
    }

    /** The launcher spawns the entity class the gun's ProjectileKind names. */
    private static final class Kinds implements Scenario {

        @Override
        public String name() {
            return "gun.kind";
        }

        @Override
        public String description() {
            return "A gun with ProjectileKind.BULLET spawns a plain bullet and one with GRAVITY_BULLET spawns a SimpleProjectile; a kind with no factory spawns nothing.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.revolver(), 6);
            GunStats plain = base("kind_bullet").projectileKind(ProjectileKind.BULLET)
                .build();
            GunStats drop = base("kind_gravity").projectileKind(ProjectileKind.GRAVITY_BULLET)
                .build();
            GunStats missing = base("kind_missing").projectileKind(ProjectileKind.BIO)
                .build();
            Map<GenericProjectile, Boolean> none = new IdentityHashMap<>();
            Map<GenericProjectile, Boolean> baseline = bulletsIn(ctx, none);
            launch(ctx, rig.shooter, plain, 1);
            Map<GenericProjectile, Boolean> afterPlain = bulletsIn(ctx, baseline);
            int plainSimple = countSimple(afterPlain);
            launch(ctx, rig.shooter, drop, 2);
            Map<GenericProjectile, Boolean> afterDrop = bulletsIn(ctx, baseline);
            // Remove the factory for the duration of the launch, so the scenario does not depend on which kinds
            // happen to be unregistered; restore it afterwards.
            Function<World, GenericProjectile> removed = BulletLauncher.unregister(ProjectileKind.BIO);
            int afterMissing;
            try {
                launch(ctx, rig.shooter, missing, 3);
                afterMissing = bulletsIn(ctx, baseline).size();
            } finally {
                if (removed != null) {
                    BulletLauncher.register(ProjectileKind.BIO, removed);
                }
            }
            endAll(afterDrop);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("projectiles after the BULLET gun", 1, afterPlain.size());
            out.checkEquals("SimpleProjectiles among them", 0, plainSimple);
            out.checkEquals("projectiles after the GRAVITY_BULLET gun", 2, afterDrop.size());
            out.checkEquals("SimpleProjectiles among them", 1, countSimple(afterDrop));
            out.checkEquals("projectiles after the kind with no factory", 2, afterMissing);
            return out.build();
        }

        private static int countSimple(Map<GenericProjectile, Boolean> bullets) {
            int n = 0;
            for (GenericProjectile p : bullets.keySet()) {
                if (p instanceof SimpleProjectile) {
                    n++;
                }
            }
            return n;
        }
    }

    /** A pellet gun spawns the centre projectile plus its pellets, at distinct angles. */
    private static final class Pellets implements Scenario {

        @Override
        public String name() {
            return "gun.pellets";
        }

        @Override
        public String description() {
            return "A gun with 7 extra pellets and a pellet spread spawns 8 projectiles from one shot, and their yaws differ.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.revolver(), 6);
            GunStats shotgun = base("pellet_gun").spread(0.05)
                .pellets(7, 0.2, false)
                .build();
            Map<GenericProjectile, Boolean> baseline = bulletsIn(ctx, new IdentityHashMap<>());
            launch(ctx, rig.shooter, shotgun, 11);
            Map<GenericProjectile, Boolean> bullets = bulletsIn(ctx, baseline);
            double minYaw = Double.MAX_VALUE;
            double maxYaw = -Double.MAX_VALUE;
            for (GenericProjectile p : bullets.keySet()) {
                minYaw = Math.min(minYaw, p.rotationYaw);
                maxYaw = Math.max(maxYaw, p.rotationYaw);
            }
            endAll(bullets);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("projectiles from one shot", 8, bullets.size());
            out.check(maxYaw - minYaw > 1.0, "the pellets should fan out, but the yaw range was " + (maxYaw - minYaw));
            return out.build();
        }
    }

    /** Shot patterns: a burst line, a ring volley and a fan make 2, 16 and 5 projectiles through the launcher loop. */
    private static final class Patterns implements Scenario {

        @Override
        public String name() {
            return "gun.pattern";
        }

        @Override
        public String description() {
            return "One round of a burst-line gun (n=1), a ring-volley gun (1+5+10) and a fan gun (5 beams) spawns 2, 16 and 5 projectiles.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.revolver(), 6);
            GunStats[] guns = { base("pattern_line").pattern(ShotPattern.burstLine(1))
                .build(),
                base("pattern_ring").pattern(ShotPattern.ringVolley(2, 5, 0.075))
                    .build(),
                base("pattern_fan").pattern(ShotPattern.fan(5, 0.15))
                    .build() };
            int[] counts = new int[guns.length];
            for (int i = 0; i < guns.length; i++) {
                Map<GenericProjectile, Boolean> baseline = bulletsIn(ctx, new IdentityHashMap<>());
                ShotOffset[] offsets = SpreadModel.roll(new Random(31 + i), guns[i], ShotContext.DEFAULT, 0);
                Projectiles.launcher()
                    .launchVolley(ctx.world(), rig.shooter, guns[i], offsets, 1.0f);
                Map<GenericProjectile, Boolean> fresh = bulletsIn(ctx, baseline);
                counts[i] = fresh.size();
                endAll(fresh);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("burst-line projectiles", 2, counts[0]);
            out.checkEquals("ring-volley projectiles", 16, counts[1]);
            out.checkEquals("fan projectiles", 5, counts[2]);
            return out.build();
        }
    }

    /** GUN-38: the knockback multiplier changes how hard a target is pushed. */
    private static final class KnockbackStrength implements Scenario {

        private static final double[] MULTIPLIERS = { 0.0, 1.0, 3.0 };
        private static final int RANGE = 4;

        @Override
        public String name() {
            return "gun.knockback";
        }

        @Override
        public String description() {
            return "Three bullets with knockback 0, 1 and 3 hit fresh zombies: the push is none, vanilla's, and three times vanilla's (GUN-38).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.revolver(), 6);
            double[] pushed = new double[MULTIPLIERS.length];
            EntityZombie[] current = { null };
            int[] phase = { 0 };
            int[] waited = { 0 };
            spawn(ctx, rig, current, phase[0]);
            return ScenarioResult.pending(name(), 120, ticks -> {
                EntityZombie z = current[0];
                waited[0]++;
                boolean hit = z.getHealth() < z.getMaxHealth();
                if (!hit && waited[0] < 20) {
                    return null;
                }
                pushed[phase[0]] = hit ? z.motionX : Double.NaN;
                z.setDead();
                phase[0]++;
                waited[0] = 0;
                if (phase[0] < MULTIPLIERS.length) {
                    spawn(ctx, rig, current, phase[0]);
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(Math.abs(pushed[0]) < 0.02, "knockback 0 should not push, but motionX was " + pushed[0]);
                out.check(pushed[1] > 0.05, "knockback 1 should push the zombie away, but motionX was " + pushed[1]);
                out.check(
                    Math.abs(pushed[2] - 3.0 * pushed[1]) < 0.2 * Math.abs(3.0 * pushed[1]),
                    "knockback 3 should push three times as hard as 1: " + pushed[2] + " vs " + pushed[1]);
                return out.build();
            });
        }

        private static void spawn(ScenarioContext ctx, GunRig rig, EntityZombie[] current, int phase) {
            EntityZombie z = GunRig.zombie(ctx, RANGE);
            // No AI: a walking zombie would add its own velocity to the push being measured.
            z.tasks.taskEntries.clear();
            z.targetTasks.taskEntries.clear();
            current[0] = z;
            GunStats stats = base("knockback_" + phase).knockback(MULTIPLIERS[phase])
                .build();
            launch(ctx, rig.shooter, stats, 5);
        }
    }
}
