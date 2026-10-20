package com.gregguns.gun.server;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.<name>.*} scenarios of the EV ballistic batch (PDW, Pulse Rifle, Minigun). Each gun gets the shared
 * magazine-gun set from {@link MvGunScenarios} (auto fire, reload, forced-reload refund, recipe, scope), plus:
 * <ul>
 * <li>{@code burst} for a burst-line gun: one pull spends one round and launches 1 + n projectiles;</li>
 * <li>{@code rate} for the Minigun: twenty held ticks spend twenty rounds, one per tick;</li>
 * <li>{@code gate}: the assembly row carries the titanium gate, is found at EV and is refused at HV.</li>
 * </ul>
 * Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (PDW, Pulse Rifle, Minigun); docs/testing/README.md
public final class EvBallisticScenarios {

    private EvBallisticScenarios() {}

    /**
     * Registers one EV ballistic gun's scenarios.
     *
     * @param registry where to add them
     * @param gunName  the gun's registry name, for example {@code "pdw"}
     */
    public static void register(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        MvGunScenarios.register(registry, gunName);
        if (def.stats()
            .pattern()
            .isActive()) {
            registry.register(new Burst(def));
        }
        if (def.stats()
            .fireRateTicks() == 1) {
            registry.register(new EveryTick(def));
        }
        registry.register(
            GunRecipeScenarios.tierGate(
                "gun." + def.name() + ".gate",
                "The " + def.name()
                    + " row carries the titanium gate, an EV assembler finds it with the sheet inputs, and an HV assembler does not.",
                def.assembly()));
    }

    /** One pull spends one round and launches the whole burst line. */
    private static final class Burst implements Scenario {

        private final GunDefinition def;

        Burst(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".burst";
        }

        @Override
        public String description() {
            return "One pull on the " + def.name()
                + " spends one round and launches "
                + def.stats()
                    .pattern()
                    .projectileCount()
                + " bullets in a line.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), stats.magazineSize());
            rig.press();
            rig.tick();
            rig.tick();
            rig.release();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("rounds spent by one pull", 1, rig.shots());
            out.checkEquals("rounds left", stats.magazineSize() - 1, rig.loaded());
            out.checkEquals(
                "bullets launched by one round",
                stats.pattern()
                    .projectileCount(),
                rig.bullets());
            return out.build();
        }
    }

    /** The Minigun fires on every tick while the trigger is held. */
    private static final class EveryTick implements Scenario {

        private static final int HELD = 20;

        private final GunDefinition def;

        EveryTick(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".rate";
        }

        @Override
        public String description() {
            return "Holding the trigger on the " + def.name()
                + " for "
                + HELD
                + " ticks spends "
                + HELD
                + " rounds, one per tick (20 per second), each launching a bullet.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), stats.magazineSize());
            rig.press();
            for (int i = 0; i < HELD; i++) {
                rig.tick();
            }
            rig.release();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            // The pull fires on the first tick after the press, so HELD ticks fire HELD rounds (one per tick).
            out.checkEquals("rounds spent in " + HELD + " ticks", HELD, rig.shots());
            out.checkEquals("bullets launched", HELD, rig.bullets());
            out.checkEquals("rounds left", stats.magazineSize() - HELD, rig.loaded());
            return out.build();
        }
    }
}
