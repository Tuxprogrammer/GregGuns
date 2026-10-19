package com.gregguns.gun.server;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.<name>.*} scenarios of the MV batch B1 guns (Thompson, Mac10, M4, AK47): full-auto fire at the
 * gun's own interval, the magazine reload (a loaded magazine in, an empty one back), the forced-reload refund of loose
 * rounds, the assembly recipe, and the M4's scope (FOV and spread without an overlay). All four go through the same
 * server handler as the pistol's scenarios, using {@link GunRig}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-handguns-smgs.md (Thompson, Mac10); sheets-rifles-shotguns.md (M4, AK47);
// docs/testing/README.md
public final class MvGunScenarios {

    private MvGunScenarios() {}

    /**
     * Registers one B1 gun's scenarios.
     *
     * @param registry where to add them
     * @param gunName  the gun's registry name, for example {@code "m4"}
     */
    public static void register(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(new Auto(def));
        registry.register(new Reload(def));
        registry.register(new ForcedReload(def));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun." + def.name() + ".recipe",
                "The " + def.name()
                    + " is in GT's assembler map (so NEI lists it) at MV with its sheet inputs, and not below MV.",
                def.assembly()));
        if (def.stats()
            .hasScope()) {
            registry.register(new Zoom(def));
        }
    }

    private static GunRig rig(ScenarioContext ctx, GunDefinition def, int loaded) {
        return new GunRig(ctx, GunItems.get(def.name()), loaded);
    }

    /** Holding the trigger fires one round every {@code fireRateTicks}, each launching a bullet. */
    private static final class Auto implements Scenario {

        private static final int HELD_TICKS = 13;

        private final GunDefinition def;

        Auto(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".auto";
        }

        @Override
        public String description() {
            return "Holding the trigger on the " + def.name()
                + " fires every "
                + def.stats()
                    .fireRateTicks()
                + " ticks without releasing: a round and a bullet per shot, the first on the tick after the pull.";
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
                // The pull is latched and fires on the next server tick (tick index 1 here), then every interval.
                out.checkEquals("shots on the pull tick", 0, shotsAt[0]);
                out.checkEquals("shots on the first firing tick", 1, shotsAt[1]);
                out.checkEquals("shots one tick before the second interval", 1, shotsAt[stats.fireRateTicks()]);
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

    /** The reload key with rounds still loaded refunds whole loose rounds only (GUN-13 and GUN-14, not repeated). */
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

    /** The M4's scope: a plain FOV x0.75 and spread x0.75, toggled by the zoom intent, cancelled by a reload. */
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
            return "A zoom intent on the M4 turns the server zoom state on with FOV factor 0.75 and 0.75 of the spread (0.015); a second intent or a reload turns it off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = rig(ctx, def, stats.magazineSize() - 5);
            rig.give(stats.ammoType(), 1);
            rig.tick();
            GunPlayerData data = GunPlayerData.get(rig.shooter);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(stats.hasScope(), "the M4 is a scoped gun");
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
                        .fovMultiplier(stats) - 0.75)
                    < 1e-9,
                "the FOV factor while zoomed should be 0.75");
            out.check(
                Math.abs(SpreadModel.mainSpread(stats, ShotContext.player(zoomed, 0.0)) - 0.015 * 0.75) < 1e-9,
                "the spread of a zoomed shot should be 0.75 of 0.015");
            out.check(
                Math.abs(SpreadModel.mainSpread(stats, ShotContext.player(false, 0.0)) - 0.015) < 1e-9,
                "the spread of an unzoomed shot should be the full 0.015");

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
