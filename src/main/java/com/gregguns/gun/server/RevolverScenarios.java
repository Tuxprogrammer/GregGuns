package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.combat.DamageFalloff;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.revolver.*} scenarios: the Revolver fired, reloaded and measured through the same server handler as
 * the pistol's {@code gun.*} scenarios, using {@link GunRig}. They are the template for a new gun's scenarios: build a
 * rig for the gun, drive it, compare with {@code GunStats}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Revolver); docs/testing/README.md
public final class RevolverScenarios {

    private static final GunStats STATS = GunCatalog.REVOLVER;

    private RevolverScenarios() {}

    /**
     * Registers the revolver scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ForcedReload());
        registry.register(new Damage());
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.revolver.recipe",
                "The Revolver is in GT's assembler map (so NEI lists it) at LV, 15 s, matching its sheet inputs, and not below LV.",
                RevolverDef.DEFINITION.assembly()));
        registry.register(GunRecipeScenarios.revolverParts());
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.revolver(), loaded);
    }

    /** One shot: a bullet, the model's damage, one round, and the 6 tick cooldown with a remembered click. */
    private static final class Fire implements Scenario {

        private static final int END_TICK = 14;

        @Override
        public String name() {
            return "gun.revolver.fire";
        }

        @Override
        public String description() {
            return "A loaded revolver fired through the server handler launches a bullet, costs a zombie the DamageModel figure for 8 and one round, and holds the 6 tick cooldown.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 6);
            float[] firstHit = { Float.NaN };
            int[] shotsAt = new int[END_TICK + 1];
            rig.press();
            rig.release();
            rig.tick();
            int afterFirst = rig.loaded();
            int bulletsFirst = rig.bullets();
            return ScenarioResult.pending(name(), END_TICK + 10, ticks -> {
                if (ticks == 1) {
                    rig.press(); // far inside the cooldown: too early to be remembered, so it is dropped
                    rig.release();
                }
                if (ticks == 5) {
                    rig.press(); // just before the cooldown clears: remembered, fires at tick 6 (GUN-29)
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
                float expected = GunRig.expectedLoss(target, STATS.damage());
                out.checkEquals("rounds after the first shot", 5, afterFirst);
                out.checkEquals("bullets launched by the first shot", 1, bulletsFirst);
                out.check(
                    !Float.isNaN(firstHit[0]) && GunRig.near(expected, firstHit[0]),
                    "zombie should lose " + expected + " from the first bullet but lost " + firstHit[0]);
                out.checkEquals(
                    "shots fired 5 ticks later (the early click dropped, still cooling down)",
                    1,
                    shotsAt[5]);
                out.checkEquals("shots fired 6 ticks later (the remembered click)", 2, shotsAt[6]);
                out.checkEquals("rounds at the end", 4, rig.loaded());
                out.checkEquals("bullets launched in all", 2, rig.bullets());
                return out.build();
            });
        }
    }

    /** Empty revolver and one loose bullet: a 45 tick reload gives 6 rounds, and no container comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.revolver.reload";
        }

        @Override
        public String description() {
            return "An empty revolver reloads 45 ticks after the trigger: 6 rounds from one 9mm bullet, which is consumed, with nothing returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            String bullet = AmmoTypes.NINE_MM.roundItemName();
            rig.give(bullet, 1);
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
                out.checkEquals("bullet kept until the swap", 1, bulletsAt[reloadTicks - 1]);
                out.checkEquals("rounds at the 45th tick", 6, loadedAt[reloadTicks]);
                out.checkEquals("bullets left", 0, rig.count(bullet));
                out.checkEquals("empty magazines returned", 0, rig.count(AmmoTypes.NINE_MM_PISTOL.emptyItemName()));
                out.checkEquals("bullets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** GUN-13 for a magazine-less gun: reloading with 5 rounds must not turn one bullet into five. */
    private static final class ForcedReload implements Scenario {

        @Override
        public String name() {
            return "gun.revolver.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a revolver with 5 rounds tops it up to 6 after 45 ticks, consumes the bullet and refunds none: a bullet is worth 6 rounds, so 5 rounds are less than a bullet.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 5);
            String bullet = AmmoTypes.NINE_MM.roundItemName();
            rig.give(bullet, 1);
            int reloadTicks = STATS.reloadTicks();
            rig.tick();
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", 6, rig.loaded());
                out.checkEquals("bullets after (none refunded, none duplicated)", 0, rig.count(bullet));
                return out.build();
            });
        }
    }

    /** Damage drop-off in the world: a zombie past the 12 block start takes less than full damage. */
    private static final class Damage implements Scenario {

        private static final int RANGE = 16;

        @Override
        public String name() {
            return "gun.revolver.damage";
        }

        @Override
        public String description() {
            return "A revolver bullet hits a zombie 16 blocks away for the drop-off figure (8 falling to 6 between 12 and 20 blocks), less than the 8 it does at 6 blocks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.revolver(), STATS.magazineSize(), RANGE + 4);
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
                out.check(lost > 0, "the bullet should have hit");
                out.check(
                    lost < GunRig.expectedLoss(target, STATS.damage()) - 0.1F,
                    "damage at 16 blocks must be below the full figure");
                return out.build();
            });
        }
    }
}
