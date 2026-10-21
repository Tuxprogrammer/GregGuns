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
        if (def.stats()
            .hasSpinUp()) {
            registry.register(new SpinUp(def));
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
            // A spin-up gun fires its first round on the tick the barrels reach full spin.
            int lead = stats.hasSpinUp() ? stats.spinUpTicks() - 1 : 0;
            for (int i = 0; i < HELD + lead; i++) {
                rig.tick();
            }
            rig.release();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            // Past the spin-up, every tick fires a round: HELD ticks fire HELD rounds (one per tick).
            out.checkEquals("rounds spent in " + HELD + " ticks", HELD, rig.shots());
            out.checkEquals("bullets launched", HELD, rig.bullets());
            out.checkEquals("rounds left", stats.magazineSize() - HELD, rig.loaded());
            return out.build();
        }
    }

    /** The spin-up fire delay: from rest, after a short release, and after an item swap. */
    private static final class SpinUp implements Scenario {

        private static final int RELEASED = 10;
        private static final int GIVE_UP = 100;

        private final GunDefinition def;

        SpinUp(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".spinup";
        }

        @Override
        public String description() {
            GunStats stats = def.stats();
            return "The first shot of the " + def.name()
                + " leaves "
                + stats.spinUpTicks()
                + " ticks after the trigger is pressed from rest; after a "
                + RELEASED
                + " tick release the barrels have slowed by "
                + RELEASED
                + "/"
                + stats.spinDownTicks()
                + " and the delay is that fraction of the full one; an item swap resets it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), stats.magazineSize());
            ScenarioResult.Builder out = ScenarioResult.builder(name());

            rig.press();
            out.checkEquals("ticks to the first shot from rest", stats.spinUpTicks(), ticksToShot(rig));
            rig.tick();
            rig.tick();
            out.check(rig.shots() >= 3, "the gun should fire every tick once at speed");

            rig.release();
            for (int i = 0; i < RELEASED; i++) {
                rig.tick();
            }
            rig.press();
            int expected = (int) Math.round((double) stats.spinUpTicks() * RELEASED / stats.spinDownTicks());
            out.checkEquals("ticks to fire again after a short release", expected, ticksToShot(rig));

            // A swap to an empty slot and back: the held trigger is dropped and the spin with it.
            rig.shooter.inventory.currentItem = 1;
            rig.tick();
            rig.shooter.inventory.currentItem = 0;
            rig.tick();
            rig.press();
            out.checkEquals("ticks to the first shot after an item swap", stats.spinUpTicks(), ticksToShot(rig));
            rig.release();
            return out.build();
        }

        /** Ticks the held gun until a round leaves; the number of ticks it took (GIVE_UP if none). */
        private static int ticksToShot(GunRig rig) {
            int before = rig.shots();
            for (int n = 1; n <= GIVE_UP; n++) {
                rig.tick();
                if (rig.shots() > before) {
                    return n;
                }
            }
            return GIVE_UP;
        }
    }
}
