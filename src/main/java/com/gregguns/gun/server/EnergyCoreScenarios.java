package com.gregguns.gun.server;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.ChargeSource;
import com.gregguns.gun.logic.FireControl;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadStateMachine;

/**
 * {@code gun.energy.core}: the energy fire core on a live server. A scripted gun with a 3.5-shot battery fires
 * exactly three times through the real {@link GunController}, then clicks dry without starting a reload; the reload
 * key does nothing; creative and NPC sources never drain. No world access, so it needs no arena. Dev environment
 * only.
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12, GGS-02-13)
public final class EnergyCoreScenarios {

    private EnergyCoreScenarios() {}

    /**
     * Registers the scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Core());
    }

    private static final class Battery implements ChargeSource {

        private long eu;

        Battery(long eu) {
            this.eu = eu;
        }

        @Override
        public long stored() {
            return eu;
        }

        @Override
        public long drain(long want) {
            long taken = Math.min(eu, Math.max(0, want));
            eu -= taken;
            return taken;
        }
    }

    private static final class Core implements Scenario {

        @Override
        public String name() {
            return "gun.energy.core";
        }

        @Override
        public String description() {
            return "An energy gun with a 3500 EU battery and 1000 EU shots fires three times, clicks dry once with no reload, ignores the reload key, and never drains for a creative or NPC shooter.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunStats stats = GunStats.builder("energy_core_probe")
                .energy(1000, 10000)
                .fire(FireMode.AUTO, 2)
                .build();
            Battery battery = new Battery(3500);
            AmmoInventory ammo = new AmmoInventory() {

                @Override
                public int count(String ammoType) {
                    return 0;
                }

                @Override
                public int withdraw(String ammoType, int maxItems) {
                    return 0;
                }

                @Override
                public void depositItems(String ammoType, int items) {}

                @Override
                public void depositRounds(String ammoType, int rounds) {}

                @Override
                public ChargeSource charge() {
                    return battery;
                }
            };
            GunController controller = new GunController(new FireControl(0, 0));
            GunState state = GunState.full(stats);
            int fired = 0;
            int dry = 0;
            controller.triggerPressed();
            for (int i = 0; i < 30; i++) {
                var r = controller.tick(stats, state, ammo, true, false);
                fired += r.shot()
                    .wasFired() ? 1 : 0;
                dry += r.outOfAmmo() ? 1 : 0;
                out.check(r.reload() == ReloadStateMachine.Event.NONE, "an energy gun must never reload");
            }
            out.check(fired == 3, "three shots should fire from 3500 EU but " + fired + " did");
            out.check(battery.stored() == 500, "500 EU should remain but " + battery.stored() + " did");
            out.check(dry == 1, "exactly one dry click expected but " + dry);
            out.check(
                controller.forceReload(stats, state, ammo) == ReloadStateMachine.Event.NONE,
                "the reload key must do nothing");

            Battery full = new Battery(10000);
            GunController creative = new GunController(new FireControl(0, 0));
            creative.triggerPressed();
            var shot = creative.tick(stats, state, new AmmoInventory() {

                @Override
                public int count(String ammoType) {
                    return 0;
                }

                @Override
                public int withdraw(String ammoType, int maxItems) {
                    return 0;
                }

                @Override
                public void depositItems(String ammoType, int items) {}

                @Override
                public void depositRounds(String ammoType, int rounds) {}

                @Override
                public ChargeSource charge() {
                    return full;
                }
            }, true, true);
            out.check(
                shot.shot()
                    .wasFired() && full.stored() == 10000,
                "a creative shooter fires without draining");
            GunController npc = new GunController(new FireControl(0, 0));
            npc.triggerPressed();
            out.check(
                npc.tick(stats, state, AmmoInventory.INFINITE, true, false)
                    .shot()
                    .wasFired(),
                "an NPC source fires");
            return out.build();
        }
    }
}
