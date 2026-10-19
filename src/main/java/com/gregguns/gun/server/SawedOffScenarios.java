package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.logic.GunCatalog;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.sawedoff.*} scenarios: the Sawed Off fired and reloaded through the same server handler as the
 * pistol, using {@link GunRig}. The recipe scenarios are in {@code GunRecipeScenarios}. Dev environment only, server
 * side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (Sawed Off); docs/testing/README.md
public final class SawedOffScenarios {

    private static final GunStats STATS = GunCatalog.SAWED_OFF;
    private static final String SHELL = AmmoTypes.SHOTGUN_SHELL.roundItemName();

    private SawedOffScenarios() {}

    /**
     * Registers the Sawed Off scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ReloadForced());
        registry.register(new GunVisualScenario("mp.visual.sawedoff", "saw", GunItems.sawedOff(), SHELL));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.sawedoff.recipe",
                "The Sawed Off is in GT's assembler map (so NEI lists it) at LV, 10 s, matching its sheet inputs, and not below LV.",
                SawedOffDef.DEFINITION.assembly()));
        registry.register(GunRecipeScenarios.shellPress());
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.sawedOff(), loaded);
    }

    /** A zombie with enough health to survive eight pellets, so the whole volley's damage can be measured. */
    private static EntityZombie tankZombie(ScenarioContext ctx, int dx) {
        EntityZombie z = GunRig.zombie(ctx, dx);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(200.0);
        z.setHealth(200.0F);
        return z;
    }

    /** One shot: 8 projectiles, close-range damage near pellets x damage, one round. */
    private static final class Fire implements Scenario {

        private static final int WAIT = 12;

        @Override
        public String name() {
            return "gun.sawedoff.fire";
        }

        @Override
        public String description() {
            return "A loaded Sawed Off fired point blank launches 8 pellets, costs one round, and a zombie right in front loses nearly 8 times the DamageModel figure for 4.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = tankZombie(ctx, 1);
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
                float perPellet = GunRig.expectedLoss(target, STATS.damage());
                float full = STATS.bulletsPerShot() * perPellet;
                out.checkEquals("pellets launched by one shot", STATS.bulletsPerShot(), pellets);
                out.checkEquals("rounds after the shot (one round per shot)", 1, afterFirst);
                out.check(
                    lost <= full + 0.05F && lost >= 0.75F * full,
                    "zombie at point blank should lose about " + full
                        + " (8 x "
                        + perPellet
                        + ", a pellet or two may miss) but lost "
                        + lost);
                return out.build();
            });
        }
    }

    /** Empty gun and three shells: 28 ticks later two are loaded and one is left. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.sawedoff.reload";
        }

        @Override
        public String description() {
            return "An empty Sawed Off with 3 shells reloads 28 ticks after the trigger: 2 rounds, 2 shells used, 1 left, and nothing returned.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            rig.give(SHELL, 3);
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
                out.checkEquals("rounds at the 28th tick", 2, loadedAt[reloadTicks]);
                out.checkEquals("shells left (3 - 2)", 1, rig.count(SHELL));
                out.checkEquals("pellets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** The reload key with one round left tops up shell by shell, keeping the round. */
    private static final class ReloadForced implements Scenario {

        @Override
        public String name() {
            return "gun.sawedoff.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a Sawed Off with 1 round and 2 shells tops it up to 2 rounds after 28 ticks using exactly 1 shell; the round already loaded is kept.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 1);
            rig.give(SHELL, 2);
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
                out.checkEquals("rounds after the top-up", 2, rig.loaded());
                out.checkEquals("shells left (only the missing round is loaded)", 1, rig.count(SHELL));
                return out.build();
            });
        }
    }
}
