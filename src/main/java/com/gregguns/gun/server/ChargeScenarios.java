package com.gregguns.gun.server;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.ChargeMode;
import com.gregguns.gun.logic.ChargeRelease;
import com.gregguns.gun.logic.ChargeState;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;

/**
 * {@code gun.charge.hold}: the charge-hold core on a live server. A scripted Bio-style gun (30 ticks, three levels)
 * and a hammer-style gun (20 ticks, up to 10 units) are held for set tick counts through the real
 * {@link ChargeState}, and the rounds taken from a {@link GunState} match the level or scaled amount, never more than
 * is loaded (GUN-02). No world access. Dev environment only.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Bio Gun, Power Hammer); fixes GUN-02.
public final class ChargeScenarios {

    private ChargeScenarios() {}

    /**
     * Registers the scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Hold());
    }

    private static final class Hold implements Scenario {

        @Override
        public String name() {
            return "gun.charge.hold";
        }

        @Override
        public String description() {
            return "Holding a charge gun for 10, 20 and 40 ticks gives Bio levels 1, 2 and 3; with 2 rounds loaded a full charge spends 2 and never more than loaded; a hammer-style charge spends floor(10 * charge) clamped to 1..10 (GUN-02).";
        }

        private static int shoot(GunStats stats, GunState state, int ticks) {
            ChargeState charge = new ChargeState();
            charge.begin(state.loaded() > 0);
            for (int i = 0; i < ticks; i++) {
                charge.tick(stats);
            }
            ChargeRelease release = charge.release(stats, state.loaded());
            return release == null ? 0 : state.use(release.units());
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunStats bio = GunStats.builder("charge_probe_bio")
                .ammoType("bio")
                .magazine(30)
                .charge(30, ChargeMode.LEVELS, 0)
                .build();
            GunStats hammer = GunStats.builder("charge_probe_hammer")
                .ammoType("air")
                .magazine(300)
                .charge(20, ChargeMode.SCALED, 10)
                .build();
            GunState full = GunState.full(bio);
            out.checkEquals("level 1 spends 1", 1, shoot(bio, full, 10));
            out.checkEquals("level 2 spends 2", 2, shoot(bio, full, 20));
            out.checkEquals("level 3 spends 3", 3, shoot(bio, full, 40));
            out.checkEquals("rounds left", 24, full.loaded());
            GunState low = new GunState(2);
            out.checkEquals("a full charge with 2 loaded spends 2", 2, shoot(bio, low, 40));
            out.checkEquals("an empty gun spends nothing", 0, shoot(bio, low, 40));
            GunState air = GunState.full(hammer);
            out.checkEquals("half a hammer charge spends 5", 5, shoot(hammer, air, 10));
            out.checkEquals("a tap spends 1", 1, shoot(hammer, air, 0));
            out.checkEquals("a full hammer charge spends 10", 10, shoot(hammer, air, 20));
            out.checkEquals("air left", 284, air.loaded());
            return out.build();
        }
    }
}
