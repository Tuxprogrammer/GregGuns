package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.<name>.*} scenarios of the HV batch B4 magazine guns, the AS50 and the LMG: the AS50's point-blank
 * hit at 25% penetration, the LMG's sustained full-auto fire from its 100-round magazine, the magazine reload and the
 * forced-reload refund, the assembly recipe (the found recipe's output is ours), and each gun's own scope numbers. All
 * go through the same server handler as the other guns, using {@link GunRig}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (AS50, LMG); docs/testing/README.md
public final class HeavyGunScenarios {

    private HeavyGunScenarios() {}

    /**
     * Registers one B4 gun's scenarios.
     *
     * @param registry where to add them
     * @param gunName  {@code "as50"} or {@code "lmg"}
     */
    public static void register(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        if (def.stats()
            .fireMode() == FireMode.AUTO) {
            registry.register(new Sustained(def));
        } else {
            registry.register(new PointBlank(def));
        }
        registry.register(new Reload(def));
        registry.register(new ForcedReload(def));
        registry.register(new Zoom(def));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun." + def.name() + ".recipe",
                "The " + def.name()
                    + " is in GT's assembler map (so NEI lists it) at HV with its sheet inputs, not below HV, and the found recipe's output is ours.",
                def.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, GunDefinition def, int loaded) {
        return new GunRig(ctx, GunItems.get(def.name()), loaded);
    }

    /** One pull on a zombie in front: one round, one bullet, damage per the damage model at the gun's penetration. */
    private static final class PointBlank implements Scenario {

        private static final int WAIT = 12;

        private final GunDefinition def;

        PointBlank(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "A loaded " + def.name()
                + " fired at a zombie in front spends one round, launches one bullet, and the zombie loses the DamageModel figure for "
                + def.stats()
                    .damage()
                + " damage at "
                + (int) Math.round(
                    def.stats()
                        .penetration() * 100)
                + "% penetration.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, stats.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 1);
            target.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(200.0);
            target.setHealth(200.0F);
            rig.press();
            rig.release();
            rig.tick();
            int afterFirst = rig.loaded();
            int bullets = rig.bullets();
            return ScenarioResult.pending(name(), WAIT + 10, ticks -> {
                rig.tick();
                if (ticks < WAIT) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                float lost = target.getMaxHealth() - target.getHealth();
                float expected = GunRig.expectedLoss(target, stats.damage(), stats.penetration());
                out.checkEquals("bullets launched by one pull", 1, bullets);
                out.checkEquals("rounds after the shot", stats.magazineSize() - 1, afterFirst);
                out.check(
                    lost <= expected + 0.05F && lost >= 0.75F * expected,
                    "zombie at point blank should lose about " + expected + " but lost " + lost);
                return out.build();
            });
        }
    }

    /** Holding the trigger on the 100-round magazine: a round and a bullet every interval, and the clip drains. */
    private static final class Sustained implements Scenario {

        private static final int HELD_TICKS = 61;

        private final GunDefinition def;

        Sustained(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".auto";
        }

        @Override
        public String description() {
            return "Holding the trigger on the " + def.name()
                + " for 61 ticks fires every 2 ticks without releasing: 31 rounds and bullets from its 100-round magazine, 69 left, first shot on the tick after the pull.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, stats.magazineSize());
            rig.press();
            int[] shotsAt = new int[HELD_TICKS];
            return ScenarioResult.pending(name(), HELD_TICKS + 10, ticks -> {
                rig.tick();
                if (ticks < HELD_TICKS) {
                    shotsAt[ticks] = rig.shots();
                    return null;
                }
                rig.release();
                int expected = 1 + (HELD_TICKS - 1) / stats.fireRateTicks();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("clip size", 100, stats.magazineSize());
                out.checkEquals("shots on the pull tick", 0, shotsAt[0]);
                out.checkEquals("shots on the first firing tick", 1, shotsAt[1]);
                out.checkEquals("shots on the second interval", 2, shotsAt[stats.fireRateTicks() + 1]);
                out.checkEquals("shots over " + HELD_TICKS + " held ticks", expected, rig.shots());
                out.checkEquals("rounds left", stats.magazineSize() - expected, rig.loaded());
                out.checkEquals("bullets launched", expected, rig.bullets());
                return out.build();
            });
        }
    }

    /** An empty gun with one loaded magazine: the dry trigger starts the reload, which fills the clip. */
    private static final class Reload implements Scenario {

        private final GunDefinition def;

        Reload(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload";
        }

        @Override
        public String description() {
            return "An empty " + def.name()
                + " reloads after "
                + def.stats()
                    .reloadTicks()
                + " ticks: the clip fills from one loaded magazine, which is consumed, and an empty magazine comes back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, 0);
            rig.give(stats.ammoType(), 1);
            int reloadTicks = stats.reloadTicks();
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
                out.checkEquals("rounds at the end of the reload", stats.magazineSize(), loadedAt[reloadTicks]);
                out.checkEquals("loaded magazines left", 0, rig.count(stats.ammoType()));
                out.checkEquals("empty magazines returned", 1, rig.count(stats.emptyContainerType()));
                out.checkEquals("bullets launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** The reload key with rounds still loaded refunds whole loose rounds only (GUN-13). */
    private static final class ForcedReload implements Scenario {

        private final GunDefinition def;

        ForcedReload(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a part-loaded " + def.name()
                + " swaps the magazine and refunds one loose round per "
                + GunItems.roundsPerLooseItem(def.stats())
                + " rounds left, floored.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            int perItem = GunItems.roundsPerLooseItem(stats);
            int loaded = stats.magazineSize() - 3; // 3 fired; more than one item's worth is still loaded
            GunRig rig = rig(ctx, def, loaded);
            rig.give(stats.ammoType(), 1);
            int reloadTicks = stats.reloadTicks();
            rig.tick(); // registers the held gun with the handler
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            String loose = stats.looseAmmoType();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", stats.magazineSize(), rig.loaded());
                out.checkEquals("loose rounds refunded", loaded / perItem, rig.count(loose));
                out.checkEquals("empty magazines returned", 1, rig.count(stats.emptyContainerType()));
                out.checkEquals("loaded magazines left", 0, rig.count(stats.ammoType()));
                return out.build();
            });
        }
    }

    /** The scope: the gun's own FOV factor and spread multiplier, toggled by the zoom intent, cancelled by a reload. */
    private static final class Zoom implements Scenario {

        private final GunDefinition def;

        Zoom(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".zoom";
        }

        @Override
        public String description() {
            GunStats stats = def.stats();
            return "A zoom intent on the " + def.name()
                + " turns the server zoom state on with FOV factor "
                + stats.zoomFactor()
                + " and "
                + stats.zoomSpreadMultiplier()
                + " of the spread; a second intent or a reload turns it off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, stats.magazineSize() - 5);
            rig.give(stats.ammoType(), 1);
            rig.tick();
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(stats.hasScope(), "the " + def.name() + " is a scoped gun");
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(stats),
                "not zoomed before the intent");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            boolean zoomed = data.controller()
                .zoom()
                .isZoomed(stats);
            out.check(zoomed, "the zoom intent should turn the server zoom state on");
            out.check(
                Math.abs(
                    data.controller()
                        .zoom()
                        .fovMultiplier(stats) - stats.zoomFactor())
                    < 1e-9,
                "the FOV factor while zoomed should be " + stats.zoomFactor());
            out.check(
                Math.abs(
                    SpreadModel.mainSpread(stats, ShotContext.player(zoomed, 0.0))
                        - stats.spread() * stats.zoomSpreadMultiplier())
                    < 1e-9,
                "the spread of a zoomed shot should be the zoom multiple of the base spread");
            out.check(
                Math.abs(SpreadModel.mainSpread(stats, ShotContext.player(false, 0.0)) - stats.spread()) < 1e-9,
                "the spread of an unzoomed shot should be the full base spread");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(stats),
                "a second intent turns zoom off");

            ServerGunHandler.onZoomToggle(rig.shooter);
            rig.tick();
            ServerGunHandler.onReloadKey(rig.shooter);
            rig.tick();
            out.check(rig.reloading(), "the reload key should start a reload");
            out.check(
                !data.controller()
                    .zoom()
                    .isZoomed(stats),
                "starting a reload cancels zoom");
            return out.build();
        }
    }
}
