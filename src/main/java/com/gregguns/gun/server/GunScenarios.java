package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageFactors;
import com.gregguns.combat.DamageSources;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;

/**
 * The {@code gun.*} in-game scenarios: a {@link FakePlayer} holding a real {@code pistol_9mm} stack is driven through
 * the same {@link ServerGunHandler} entry points the trigger, zoom and reload packets reach ({@code onTrigger},
 * {@code onZoomToggle}, {@code onReloadKey}) and the same per-tick method the player tick event calls
 * ({@code onPlayerTick}). Nothing about firing is re-implemented: the scenarios only stand in for the network and for
 * the world's player tick, which a fake player does not get. No seam was needed in the handler, because its entry
 * points already take an {@code EntityPlayerMP}, which {@code FakePlayer} is. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/02-guns/index.md, docs/techguns-spec/02-guns/gun-lifecycle.md; docs/testing/README.md
public final class GunScenarios {

    private static final String TARGET_NAME = "[GregGunsGunTarget]";

    private GunScenarios() {}

    /**
     * Registers the gun scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ForcedReload());
        registry.register(new DryFire());
        registry.register(new HitsPlayer());
        registry.register(new ZoomNoOp());
    }

    // ----- scenarios -------------------------------------------------------------------------------------------

    /** Fire at a zombie: bullet launched, damage per the model, ammo down by one, cooldown holds, buffered click. */
    private static final class Fire implements Scenario {

        private static final int RANGE = 6;
        private static final int END_TICK = 30;

        @Override
        public String name() {
            return "gun.fire";
        }

        @Override
        public String description() {
            return "A loaded pistol fired through the server handler launches a bullet, costs a zombie the DamageModel figure and one round, and holds the 4 tick cooldown (GUN-29 buffer).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), GunCatalog.PISTOL.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, RANGE);
            float[] firstHit = { Float.NaN };
            int[] shotsAt = new int[END_TICK + 1];

            rig.press();
            rig.release();
            rig.tick(); // tick 0: the shot
            int afterFirst = rig.loaded();
            int bulletsFirst = rig.bullets();

            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                // 'ticks' counts the ticks after the shot.
                if (ticks == 1) {
                    rig.press(); // a click inside the cooldown, too early to be remembered until it clears
                    rig.release();
                }
                if (ticks == 3) {
                    rig.press(); // remembered: fires the tick the cooldown clears (tick 4)
                    rig.release();
                }
                rig.tick();
                shotsAt[Math.min(ticks, END_TICK)] = rig.shots();
                if (Float.isNaN(firstHit[0]) && target.getHealth() < target.getMaxHealth()) {
                    firstHit[0] = target.getMaxHealth() - target.getHealth();
                }
                if (ticks < END_TICK) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float expected = GunRig.expectedLoss(target, GunCatalog.PISTOL.damage());
                out.checkEquals("rounds after the first shot", 17, afterFirst);
                out.checkEquals("bullets launched by the first shot", 1, bulletsFirst);
                out.check(
                    !Float.isNaN(firstHit[0]) && GunRig.near(expected, firstHit[0]),
                    "zombie should lose " + expected + " from the first bullet but lost " + firstHit[0]);
                out.checkEquals("shots fired 1 tick later (click inside the cooldown)", 1, shotsAt[1]);
                out.checkEquals("shots fired 3 ticks later (still cooling down)", 1, shotsAt[3]);
                out.checkEquals("shots fired 4 ticks later (the remembered click)", 2, shotsAt[4]);
                out.checkEquals("rounds at the end", 16, rig.loaded());
                out.checkEquals("bullets launched in all", 2, rig.bullets());
                return out.build();
            });
        }
    }

    /** Empty pistol with a magazine: the dry trigger starts a 35 tick reload, then 18 rounds and an empty magazine. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.reload";
        }

        @Override
        public String description() {
            return "An empty pistol reloads 35 ticks after the trigger: 18 rounds, the magazine consumed, an empty magazine returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), 0);
            String loadedMag = AmmoTypes.NINE_MM_PISTOL.loadedItemName();
            rig.give(loadedMag, 1);
            int reloadTicks = GunCatalog.PISTOL.reloadTicks();
            rig.press();
            rig.release();
            rig.tick(); // tick 0: empty, so the reload starts
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            int[] magsAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                    magsAt[ticks] = rig.count(loadedMag);
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("magazine kept until the swap", 1, magsAt[reloadTicks - 1]);
                out.checkEquals("rounds at the 35th tick", 18, loadedAt[reloadTicks]);
                out.checkEquals("loaded magazines left", 0, rig.count(loadedMag));
                out.checkEquals("empty magazines returned", 1, rig.count(AmmoTypes.NINE_MM_PISTOL.emptyItemName()));
                out.checkEquals("bullets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** GUN-13: a forced reload with rounds loaded keeps them, to whole-bullet rounding. */
    private static final class ForcedReload implements Scenario {

        @Override
        public String name() {
            return "gun.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a pistol with 7 rounds swaps the magazine after 35 ticks and refunds floor(7/6)=1 bullet (GUN-13); the leftover round is the documented rounding.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), 7);
            rig.give(AmmoTypes.NINE_MM_PISTOL.loadedItemName(), 1);
            int reloadTicks = GunCatalog.PISTOL.reloadTicks();
            rig.tick(); // registers the held gun with the handler
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", 18, rig.loaded());
                out.checkEquals("loose bullets refunded", 1, rig.count(AmmoTypes.NINE_MM.roundItemName()));
                out.checkEquals("empty magazines returned", 1, rig.count(AmmoTypes.NINE_MM_PISTOL.emptyItemName()));
                out.checkEquals("loaded magazines left", 0, rig.count(AmmoTypes.NINE_MM_PISTOL.loadedItemName()));
                return out.build();
            });
        }
    }

    /** No rounds and no magazines: the trigger does nothing and nothing throws. */
    private static final class DryFire implements Scenario {

        @Override
        public String name() {
            return "gun.dryfire";
        }

        @Override
        public String description() {
            return "An empty pistol with no magazines fires nothing, starts no reload, loses no state and does not crash.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), 0);
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 20, ticks -> {
                if (ticks == 2) {
                    rig.press();
                    rig.release();
                }
                rig.tick();
                if (ticks < 10) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds", 0, rig.loaded());
                out.checkEquals("bullets launched", 0, rig.bullets());
                out.check(!rig.reloading(), "no reload should start without a magazine");
                return out.build();
            });
        }
    }

    /** A shot at a second fake player damages them. */
    private static final class HitsPlayer implements Scenario {

        @Override
        public String name() {
            return "gun.player";
        }

        @Override
        public String description() {
            return "A shot from one fake player hits another fake player 6 blocks away and costs them the DamageModel figure.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), GunCatalog.PISTOL.magazineSize());
            // A fake player has no network handler, so the entity tracker would crash on it if it were spawned through
            // the world. Put it into its chunk only: hit tests find it there, and it is never ticked or tracked.
            FakePlayer victim = GunRig.fakePlayer(ctx, TARGET_NAME, 6);
            Chunk chunk = ctx.world()
                .getChunkFromBlockCoords((int) victim.posX, (int) victim.posZ);
            chunk.addEntity(victim);
            float before = victim.getHealth();
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (!rig.bulletsGone() && ticks < 35) {
                    return null;
                }
                chunk.removeEntity(victim);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = before - victim.getHealth();
                // Player-on-player hits carry the configured PvP damage factor (DamageFactorsConfig, synced).
                double pvpFactor = DamageFactors.damageFactor(
                    DamageSources.settingsFromConfig(),
                    DamageFactors.Attacker.PLAYER,
                    true,
                    MinecraftServer.getServer()
                        .isPVPEnabled());
                float expected = GunRig.expectedLoss(victim, (float) (GunCatalog.PISTOL.damage() * pvpFactor));
                out.check(GunRig.near(expected, lost), "the player should lose " + expected + " but lost " + lost);
                out.checkEquals("bullets launched", 1, rig.bullets());
                return out.build();
            });
        }
    }

    /** The unscoped pistol accepts a zoom intent and nothing changes. */
    private static final class ZoomNoOp implements Scenario {

        @Override
        public String name() {
            return "gun.zoom";
        }

        @Override
        public String description() {
            return "A zoom intent on the unscoped pistol is accepted and leaves the zoom state off and the FOV factor at 1.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.pistol(), GunCatalog.PISTOL.magazineSize());
            rig.tick(); // registers the held gun with the handler
            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            GunStats stats = GunCatalog.PISTOL;
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(stats),
                "an unscoped gun must not zoom");
            out.check(
                data.controller()
                    .zoom()
                    .fovMultiplier(stats) == 1.0,
                "the FOV factor should stay 1");
            out.checkEquals("rounds untouched", stats.magazineSize(), rig.loaded());
            return out.build();
        }
    }
}
