package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.CombatShotgunDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.combat_shotgun.*} scenarios: the Combat Shotgun fired (eight pellets for one round, armor
 * penetration included) and reloaded shell by shell through the same server handler as the Sawed Off, plus its
 * assembly recipe. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (Combat Shotgun); docs/testing/README.md
public final class CombatShotgunScenarios {

    private static final GunStats STATS = CombatShotgunDef.STATS;
    private static final String SHELL = AmmoTypes.SHOTGUN_SHELL.roundItemName();

    private CombatShotgunScenarios() {}

    /**
     * Registers the Combat Shotgun scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ReloadForced());
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.combat_shotgun.recipe",
                "The Combat Shotgun is in GT's assembler map (so NEI lists it) at MV, 25 s, matching its sheet inputs, and not below MV.",
                CombatShotgunDef.DEFINITION.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.get(CombatShotgunDef.NAME), loaded);
    }

    /** One shot: 8 projectiles for one round, point-blank damage near eight pellets. */
    private static final class Fire implements Scenario {

        private static final int WAIT = 12;

        @Override
        public String name() {
            return "gun.combat_shotgun.fire";
        }

        @Override
        public String description() {
            return "A loaded Combat Shotgun fired point blank launches 8 pellets for one round, and a zombie right in front loses nearly 8 times the DamageModel figure for 4 damage at 15% penetration.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 1);
            target.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(200.0);
            target.setHealth(200.0F);
            rig.press();
            rig.release();
            rig.tick();
            int afterFirst = rig.loaded();
            int pellets = rig.bullets();
            return ScenarioResult.pending(name(), WAIT + 10, ticks -> {
                rig.tick();
                if (ticks < WAIT) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                float full = STATS.bulletsPerShot() * GunRig.expectedLoss(target, STATS.damage(), STATS.penetration());
                out.checkEquals("pellets launched by one shot", 8, pellets);
                out.checkEquals("rounds after the shot (one round per shot)", STATS.magazineSize() - 1, afterFirst);
                out.check(
                    lost <= full + 0.05F && lost >= 0.75F * full,
                    "zombie at point blank should lose about " + full + " but lost " + lost);
                return out.build();
            });
        }
    }

    /** Empty gun and ten shells: 50 ticks later eight are loaded and two are left. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.combat_shotgun.reload";
        }

        @Override
        public String description() {
            return "An empty Combat Shotgun with 10 shells reloads 50 ticks after the trigger: 8 rounds, 8 shells used, 2 left, and nothing returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            rig.give(SHELL, 10);
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
                out.checkEquals("rounds at the end of the reload", 8, loadedAt[reloadTicks]);
                out.checkEquals("shells left (10 - 8)", 2, rig.count(SHELL));
                out.checkEquals("pellets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** The reload key with rounds left tops up shell by shell, keeping them. */
    private static final class ReloadForced implements Scenario {

        @Override
        public String name() {
            return "gun.combat_shotgun.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a Combat Shotgun with 5 rounds and 5 shells tops it up to 8 after 50 ticks using exactly 3 shells; the rounds already loaded are kept.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 5);
            rig.give(SHELL, 5);
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
                out.checkEquals("rounds after the top-up", 8, rig.loaded());
                out.checkEquals("shells left (only the missing rounds are loaded)", 2, rig.count(SHELL));
                return out.build();
            });
        }
    }
}
