package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.combat.DamageFalloff;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;

/**
 * The {@code gun.boltaction.*} scenarios: the Bolt Action Rifle fired, rechambered, reloaded, zoomed and measured
 * through the same server handler as the pistol's {@code gun.*} scenarios, using {@link GunRig}. Dev environment only,
 * server side. The rifle's recipe scenario is {@code gun.boltaction.recipe} in {@code GunRecipeScenarios}.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (Bolt Action Rifle); docs/testing/README.md
public final class BoltActionScenarios {

    private static final GunStats STATS = GunCatalog.BOLT_ACTION;

    private BoltActionScenarios() {}

    /**
     * Registers the bolt action scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Rechamber());
        registry.register(new Reload());
        registry.register(new Damage());
        registry.register(new Zoom());
        registry.register(
            new GunVisualScenario("mp.visual.boltaction", "bolt", GunItems.boltAction(), "ammo_rifle_bullet"));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.boltAction(), loaded);
    }

    /** One shot: a bullet, the model's damage at close range, one round. */
    private static final class Fire implements Scenario {

        @Override
        public String name() {
            return "gun.boltaction.fire";
        }

        @Override
        public String description() {
            return "A loaded Bolt Action Rifle fired through the server handler launches one bullet, costs a zombie at 6 blocks the DamageModel figure for 16, and uses one round.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 6);
            rig.press();
            rig.release();
            rig.tick();
            int afterFirst = rig.loaded();
            int bullets = rig.bullets();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (target.getHealth() >= target.getMaxHealth() && ticks < 30) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                float expected = GunRig.expectedLoss(target, STATS.damage(), STATS.penetration());
                out.checkEquals("rounds after the first shot", 5, afterFirst);
                out.checkEquals("bullets launched by the first shot", 1, bullets);
                out.check(
                    lost > 0 && GunRig.near(expected, lost),
                    "zombie should lose " + expected + " from the bullet but lost " + lost);
                return out.build();
            });
        }
    }

    /**
     * The rechamber: the rifle fires again only once the 25 tick cycle is over. A click in the middle is dropped, one
     * made just before the cycle ends is remembered and fires at tick 25, and the gun plays its rechamber sound.
     */
    private static final class Rechamber implements Scenario {

        private static final int END_TICK = 28;

        @Override
        public String name() {
            return "gun.boltaction.rechamber";
        }

        @Override
        public String description() {
            return "After a shot the rifle cannot fire for 25 ticks while the bolt is worked: a click at tick 10 is dropped, a click at tick 24 is remembered and fires at tick 25; the gun owns a rechamber sound event.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            rig.press();
            rig.release();
            rig.tick();
            int[] shotsAt = new int[END_TICK + 1];
            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                if (ticks == 10 || ticks == 24) {
                    rig.press(); // 10: far inside the cycle, dropped; 24: remembered, fires at 25
                    rig.release();
                }
                rig.tick();
                shotsAt[Math.min(ticks, END_TICK)] = rig.shots();
                if (ticks < END_TICK) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("fire interval (ticks)", 25, STATS.fireRateTicks());
                out.checkEquals("shots at tick 10 (the early click dropped)", 1, shotsAt[10]);
                out.checkEquals("shots at tick 24 (still cycling)", 1, shotsAt[24]);
                out.checkEquals("shots at tick 25 (the remembered click)", 2, shotsAt[25]);
                out.checkEquals("shots at the end (nothing auto-fired)", 2, shotsAt[END_TICK]);
                out.checkEquals("rounds at the end", 4, rig.loaded());
                out.checkEquals(
                    "rechamber event",
                    GunSoundEvents.BOLT_ACTION_RIFLE_RECHAMBER,
                    GunItems.boltAction()
                        .rechamberEvent());
                return out.build();
            });
        }
    }

    /** Empty rifle and one loose rifle bullet: a 50 tick reload gives 6 rounds, and no container comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.boltaction.reload";
        }

        @Override
        public String description() {
            return "An empty Bolt Action Rifle reloads 50 ticks after the trigger: 6 rounds from one rifle bullet, which is consumed, with nothing returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            String bullet = AmmoTypes.RIFLE.roundItemName();
            rig.give(bullet, 1);
            int reloadTicks = STATS.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("rounds at the 50th tick", 6, loadedAt[reloadTicks]);
                out.checkEquals("rifle bullets left", 0, rig.count(bullet));
                out.checkEquals("bullets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** Damage drop-off in the world: a zombie 25 blocks away, inside the 20 to 30 block fall, takes less than full. */
    private static final class Damage implements Scenario {

        private static final int RANGE = 25;

        @Override
        public String name() {
            return "gun.boltaction.damage";
        }

        @Override
        public String description() {
            return "A zoomed rifle bullet hits a zombie 25 blocks away for the drop-off figure (16 falling to 10 between 20 and 30 blocks), less than the 16 it does up close.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.boltAction(), STATS.magazineSize(), RANGE + 4);
            EntityZombie target = GunRig.zombie(ctx, RANGE);
            double startX = rig.shooter.posX;
            // Zoomed, so the shot's spread (+-2 degrees, 0.9 blocks at 25) shrinks to 0.25 degrees and always hits.
            rig.tick();
            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 50, ticks -> {
                rig.tick();
                if (target.getHealth() >= target.getMaxHealth() && ticks < 40) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                // The hit lands on the near face of the zombie's box, about 0.3 blocks short of its centre.
                double distance = target.posX - 0.3 - startX;
                double raw = DamageFalloff
                    .damageAt(STATS.damage(), STATS.damageMin(), STATS.dropStart(), STATS.dropEnd(), distance);
                float expected = GunRig.expectedLoss(target, raw, STATS.penetration());
                out.check(
                    Math.abs(expected - lost) < 0.3F,
                    "zombie at " + distance + " blocks should lose about " + expected + " but lost " + lost);
                out.check(lost > 0, "the bullet should have hit");
                out.check(
                    lost < GunRig.expectedLoss(target, STATS.damage(), STATS.penetration()) - 0.1F,
                    "damage at 25 blocks must be below the full figure");
                return out.build();
            });
        }
    }

    /**
     * Zoom: a zoom intent turns the server's zoom state on, the FOV factor is 0.35, the spread multiplier the shot
     * uses is 0.125, and a second intent or a reload turns it off.
     */
    private static final class Zoom implements Scenario {

        @Override
        public String name() {
            return "gun.boltaction.zoom";
        }

        @Override
        public String description() {
            return "A zoom intent on the Bolt Action Rifle is accepted: the server zoom state turns on, the FOV factor is 0.35 and shots use 0.125 of the spread; a second intent or a reload turns it off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 3); // part empty, so the reload key has something to do
            rig.give(AmmoTypes.RIFLE.roundItemName(), 1);
            rig.tick(); // registers the held gun with the handler
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(STATS.hasScope(), "the rifle is a scoped gun");
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(STATS),
                "not zoomed before the intent");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            boolean zoomed = data.controller()
                .zoom()
                .isZoomed(STATS);
            out.check(zoomed, "the zoom intent should turn the server zoom state on");
            out.check(
                Math.abs(
                    data.controller()
                        .zoom()
                        .fovMultiplier(STATS) - 0.35)
                    < 1e-9,
                "the FOV factor while zoomed should be 0.35");
            out.check(
                Math.abs(SpreadModel.mainSpread(STATS, ShotContext.player(zoomed, 0.0)) - 0.05 * 0.125) < 1e-9,
                "the spread of a zoomed shot should be 0.125 of 0.05");
            out.check(
                Math.abs(SpreadModel.mainSpread(STATS, ShotContext.player(false, 0.0)) - 0.05) < 1e-9,
                "the spread of an unzoomed shot should be the full 0.05");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(STATS),
                "a second intent turns zoom off");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            ServerGunHandler.onReloadKey(rig.shooter);
            rig.tick();
            out.check(rig.reloading(), "the reload key should start a reload");
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(STATS),
                "starting a reload cancels zoom");
            return out.build();
        }
    }
}
