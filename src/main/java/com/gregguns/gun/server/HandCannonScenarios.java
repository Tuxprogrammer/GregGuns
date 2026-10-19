package com.gregguns.gun.server;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.combat.DamageFalloff;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.SimpleProjectile;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.handcannon.*} scenarios: the Hand Cannon fired, measured for damage and for the fall of its stone
 * ball, and reloaded, through the same server handler as the other guns, using {@link GunRig}. The recipe scenario is
 * {@code gun.handcannon.recipe} in the recipe package. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Hand Cannon); docs/testing/README.md
public final class HandCannonScenarios {

    private static final GunStats STATS = GunCatalog.HANDCANNON;

    private HandCannonScenarios() {}

    /**
     * Registers the hand cannon scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Fall());
        registry.register(new Reload());
        registry.register(new Damage());
        registry.register(GunRecipeScenarios.handCannon());
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.handcannon(), loaded);
    }

    /** One shot: a gravity projectile, the model's damage, and the only round gone. */
    private static final class Fire implements Scenario {

        private static final int END_TICK = 8;

        @Override
        public String name() {
            return "gun.handcannon.fire";
        }

        @Override
        public String description() {
            return "A loaded hand cannon fired through the server handler launches one SimpleProjectile (the gravity bullet), costs a zombie the DamageModel figure for 8, and leaves the gun empty.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 6);
            Map<GenericProjectile, Boolean> before = projectiles(ctx);
            rig.press();
            rig.release();
            rig.tick();
            int afterFirst = rig.loaded();
            Map<GenericProjectile, Boolean> launched = newSince(ctx, before);
            int simple = 0;
            for (GenericProjectile p : launched.keySet()) {
                if (p instanceof SimpleProjectile) {
                    simple++;
                }
            }
            int launchedCount = launched.size();
            int simpleCount = simple;
            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                rig.tick();
                if (ticks < END_TICK) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                float expected = GunRig.expectedLoss(target, STATS.damage());
                out.checkEquals("rounds after the first shot", 0, afterFirst);
                out.checkEquals("projectiles launched by the shot", 1, launchedCount);
                out.checkEquals("of them SimpleProjectiles (the gravity bullet)", 1, simpleCount);
                out.check(
                    GunRig.near(expected, lost),
                    "zombie at 6 blocks should lose " + expected + " from the ball but lost " + lost);
                out.checkEquals("shots fired", 1, rig.shots());
                return out.build();
            });
        }
    }

    /** The ball falls: after 10 ticks of flight it is lower than where it started, and has travelled forward. */
    private static final class Fall implements Scenario {

        private static final int FLIGHT_TICKS = 10;

        @Override
        public String name() {
            return "gun.handcannon.fall";
        }

        @Override
        public String description() {
            return "The hand cannon's ball, shot level, drops under gravity 0.015: its vertical speed falls by 0.015 blocks per tick squared and after 10 ticks it has travelled 8 to 11 blocks forward.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.handcannon(), STATS.magazineSize(), 30);
            Map<GenericProjectile, Boolean> before = projectiles(ctx);
            rig.press();
            rig.release();
            rig.tick();
            Map<GenericProjectile, Boolean> launched = newSince(ctx, before);
            if (launched.isEmpty()) {
                return ScenarioResult.fail(name(), "the shot launched no projectile");
            }
            GenericProjectile ball = launched.keySet()
                .iterator()
                .next();
            double startX = ball.posX;
            // The shot's random spread tilts the path by up to 1.4 degrees, so the fall is measured as the change in
            // vertical speed (spread does not change it): gravity pulls 0.015 blocks per tick squared.
            double[] y = new double[FLIGHT_TICKS + 1];
            return ScenarioResult.pending(name(), FLIGHT_TICKS + 20, ticks -> {
                rig.tick();
                if (ticks < FLIGHT_TICKS) {
                    y[ticks] = ball.posY;
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!ball.isDead, "the ball should still be flying after 10 ticks");
                double vyEarly = y[3] - y[2];
                double vyLate = y[FLIGHT_TICKS - 1] - y[FLIGHT_TICKS - 2];
                double accel = (vyLate - vyEarly) / (FLIGHT_TICKS - 4);
                out.check(
                    accel < -0.010 && accel > -0.020,
                    "the ball should accelerate downwards by about 0.015 blocks per tick squared but did by " + accel);
                double forward = ball.posX - startX;
                out.check(
                    forward > 8.0 && forward < 11.0,
                    "after about 10 ticks the ball should have gone 8 to 11 blocks forward but went " + forward);
                ball.setDead();
                return out.build();
            });
        }
    }

    /** Empty cannon and one stone bullet: a 30 tick reload loads the one round, and no container comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.handcannon.reload";
        }

        @Override
        public String description() {
            return "An empty hand cannon reloads 30 ticks after the trigger: one round from one stone bullet, which is consumed, with nothing returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            String bullet = AmmoTypes.STONE.roundItemName();
            rig.give(bullet, 2);
            int reloadTicks = STATS.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            int[] bulletsAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                    bulletsAt[ticks] = rig.count(bullet);
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("bullets kept until the swap", 2, bulletsAt[reloadTicks - 1]);
                out.checkEquals("rounds at the 30th tick", 1, loadedAt[reloadTicks]);
                out.checkEquals("bullets left (one consumed)", 1, rig.count(bullet));
                out.checkEquals("projectiles launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** Damage drop-off in the world: a zombie 12 blocks away takes less than the full 8 (drop starts at 10). */
    private static final class Damage implements Scenario {

        private static final int RANGE = 12;

        @Override
        public String name() {
            return "gun.handcannon.damage";
        }

        @Override
        public String description() {
            return "A hand cannon ball hits a zombie 12 blocks away for the drop-off figure (8 falling to 5 between 10 and 25 blocks), less than the 8 it does at 6 blocks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.handcannon(), STATS.magazineSize(), RANGE + 4);
            EntityZombie target = GunRig.zombie(ctx, RANGE);
            double startX = rig.shooter.posX;
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (target.getHealth() >= target.getMaxHealth() && ticks < 30) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                // The hit lands on the near face of the zombie's box, about 0.3 blocks short of its centre.
                double distance = target.posX - 0.3 - startX;
                double raw = DamageFalloff
                    .damageAt(STATS.damage(), STATS.damageMin(), STATS.dropStart(), STATS.dropEnd(), distance);
                float expected = GunRig.expectedLoss(target, raw);
                out.check(
                    Math.abs(expected - lost) < 0.3F,
                    "zombie at " + distance + " blocks should lose about " + expected + " but lost " + lost);
                out.check(lost > 0, "the ball should have hit");
                out.check(
                    lost < GunRig.expectedLoss(target, STATS.damage()) - 0.1F,
                    "damage at 12 blocks must be below the full figure");
                return out.build();
            });
        }
    }

    private static Map<GenericProjectile, Boolean> projectiles(ScenarioContext ctx) {
        Map<GenericProjectile, Boolean> all = new IdentityHashMap<>();
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof GenericProjectile) {
                all.put((GenericProjectile) o, Boolean.TRUE);
            }
        }
        return all;
    }

    private static Map<GenericProjectile, Boolean> newSince(ScenarioContext ctx,
        Map<GenericProjectile, Boolean> before) {
        Map<GenericProjectile, Boolean> out = projectiles(ctx);
        out.keySet()
            .removeAll(before.keySet());
        return out;
    }
}
