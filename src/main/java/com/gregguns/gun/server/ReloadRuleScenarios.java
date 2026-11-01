package com.gregguns.gun.server;

import com.gregguns.core.config.GunsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.CombatShotgunDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.LmgDef;
import com.gregguns.gun.def.M4Def;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.def.ThompsonDef;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemGun;

/**
 * The scenarios of the beta reload rules (design record 0026), through the real server handler with {@link GunRig}:
 * <ul>
 * <li>{@code gun.<name>.reload.single} (Thompson, M4, LMG): an empty magazine-fed gun with only loose rounds loads one
 * round after the full reload time for one loose item, and with a magazine at hand the magazine wins.</li>
 * <li>{@code gun.revolver.reload.chamber}: the chambers go in one by one at {@code floor(k x 45 / 6)} ticks, the
 * bullet is paid at the first, and the run ends at the old 45 ticks.</li>
 * <li>{@code gun.combat_shotgun.reload.interrupt}: a pull after three shells stops the reload and fires on that
 * tick.</li>
 * <li>{@code gun.revolver.reload.idle} and {@code .idle_off}: a part-empty revolver held idle starts its chambers
 * after {@code guns.idleReloadDelayTicks}, a pull stops it, and 0 turns it off.</li>
 * </ul>
 * Dev environment only, server side.
 */
// Spec: docs/design/0026-single-round-and-chamber-reloads.md; docs/gregguns-spec/02-guns/index.md (reloading)
public final class ReloadRuleScenarios {

    private ReloadRuleScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Single(ThompsonDef.NAME));
        registry.register(new Single(M4Def.NAME));
        registry.register(new Single(LmgDef.NAME));
        registry.register(new Chamber());
        registry.register(new Interrupt());
        registry.register(new Idle());
        registry.register(new IdleOff());
    }

    private static GunRig rig(ScenarioContext ctx, String gun, int loaded) {
        return new GunRig(ctx, GunItems.get(gun), loaded);
    }

    private static int credit(GunRig rig) {
        GunStats stats = ((ItemGun) rig.gun.getItem()).stats();
        return GunNbt.read(rig.gun.getTagCompound(), stats)
            .credit();
    }

    /** One loose round per reload with no magazine; the magazine first when there is one. */
    private static final class Single implements Scenario {

        private final GunDefinition def;
        private final GunStats stats;

        Single(String gun) {
            this.def = GunDefinitions.byName(gun);
            this.stats = def.stats();
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload.single";
        }

        @Override
        public String description() {
            return "An empty " + def.name()
                + " with only loose rounds loads exactly one round after the full reload time, for one loose item and no empty magazine; given a magazine, the reload key then swaps in the magazine and leaves the loose rounds alone.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, def.name(), 0);
            String loose = stats.looseAmmoType();
            String empty = stats.emptyContainerType();
            rig.give(loose, 3);
            int reload = stats.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reload + 2];
            int[] looseAt = new int[reload + 2];
            int[] emptiesAfterLoose = { -1 };
            boolean[] secondStarted = { false };
            int end = 2 * reload + 6;
            return ScenarioResult.pending(name(), end + 20, ticks -> {
                if (ticks == reload + 2) {
                    emptiesAfterLoose[0] = rig.count(empty);
                    rig.give(stats.ammoType(), 1);
                    ServerGunHandler.onReloadKey(rig.shooter);
                    secondStarted[0] = rig.reloading();
                }
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                    looseAt[ticks] = rig.count(loose);
                }
                if (ticks < end) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should start a reload from loose rounds");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reload - 1]);
                out.checkEquals("loose rounds one tick before the end", 3, looseAt[reload - 1]);
                out.checkEquals("rounds at the end of the reload", 1, loadedAt[reload]);
                out.checkEquals("loose rounds at the end of the reload", 2, looseAt[reload]);
                out.checkEquals("empty magazines from the loose load", 0, emptiesAfterLoose[0]);
                out.check(secondStarted[0], "the reload key with a magazine at hand should start a reload");
                out.checkEquals("rounds after the magazine swap", stats.magazineSize(), rig.loaded());
                out.checkEquals("loose rounds after the magazine swap (1 round refunds none)", 2, rig.count(loose));
                out.checkEquals("loaded magazines left", 0, rig.count(stats.ammoType()));
                out.checkEquals("empty magazines back", 1, rig.count(empty));
                return out.build();
            });
        }
    }

    /** The revolver's chambers, one by one. */
    private static final class Chamber implements Scenario {

        private static final GunStats STATS = RevolverDef.STATS;

        @Override
        public String name() {
            return "gun.revolver.reload.chamber";
        }

        @Override
        public String description() {
            return "An empty revolver with one 9mm bullet loads one chamber at a time: a round at 7, 15, 22, 30, 37 and 45 ticks, the bullet consumed at the first with five rounds of chamber credit, and the gun full at the old 45 tick reload time.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, RevolverDef.NAME, 0);
            String bullet = STATS.ammoType();
            rig.give(bullet, 1);
            int reload = STATS.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reload + 2];
            int[] bulletsAt = new int[reload + 2];
            int[] creditAt = new int[reload + 2];
            return ScenarioResult.pending(name(), reload + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                    bulletsAt[ticks] = rig.count(bullet);
                    creditAt[ticks] = credit(rig);
                }
                if (ticks < reload + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should start a chamber reload");
                int[] boundaries = new int[STATS.magazineSize()];
                for (int k = 0; k < boundaries.length; k++) {
                    boundaries[k] = STATS.chamberTicksBetween(0, k + 1);
                    out.checkEquals("rounds one tick before chamber " + (k + 1), k, loadedAt[boundaries[k] - 1]);
                    out.checkEquals("rounds at chamber " + (k + 1), k + 1, loadedAt[boundaries[k]]);
                }
                out.checkEquals("last chamber at the old reload time", reload, boundaries[boundaries.length - 1]);
                out.checkEquals("bullet kept until the first chamber", 1, bulletsAt[boundaries[0] - 1]);
                out.checkEquals("bullet paid at the first chamber", 0, bulletsAt[boundaries[0]]);
                out.checkEquals("credit after the first chamber", 5, creditAt[boundaries[0]]);
                out.checkEquals("credit at the end", 0, creditAt[reload]);
                out.checkEquals("rounds at the end", 6, rig.loaded());
                out.check(!rig.reloading(), "the reload should be over");
                return out.build();
            });
        }
    }

    /** A pull mid-run fires at once with the shells loaded so far. */
    private static final class Interrupt implements Scenario {

        private static final GunStats STATS = CombatShotgunDef.STATS;

        @Override
        public String name() {
            return "gun.combat_shotgun.reload.interrupt";
        }

        @Override
        public String description() {
            return "An empty Combat Shotgun with 8 shells reloads shell by shell; a trigger pull after the third shell stops the reload and fires on the same tick, leaving 2 rounds loaded and 5 shells in the inventory.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, CombatShotgunDef.NAME, 0);
            String shell = STATS.ammoType();
            rig.give(shell, 8);
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int three = STATS.chamberTicksBetween(0, 3);
            int[] loadedBeforePull = { -1 };
            int[] shotsBefore = { -1 };
            int[] shotsAfter = { -1 };
            boolean[] reloadingAfter = { true };
            int end = three + 10;
            return ScenarioResult.pending(name(), end + 20, ticks -> {
                if (ticks == three + 1) {
                    loadedBeforePull[0] = rig.loaded();
                    shotsBefore[0] = rig.bullets();
                    rig.press();
                    rig.release();
                }
                rig.tick();
                if (ticks == three + 1) {
                    shotsAfter[0] = rig.bullets();
                    reloadingAfter[0] = rig.reloading();
                }
                if (ticks < end) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should start a chamber reload");
                out.checkEquals("shells loaded when the trigger is pulled", 3, loadedBeforePull[0]);
                out.checkEquals(
                    "pellets launched on the pull's tick",
                    STATS.bulletsPerShot(),
                    shotsAfter[0] - shotsBefore[0]);
                out.check(!reloadingAfter[0], "the pull should stop the reload");
                out.checkEquals("rounds left", 2, rig.loaded());
                out.checkEquals("shells left in the inventory", 5, rig.count(shell));
                return out.build();
            });
        }
    }

    /** Sets the idle delay for one scenario and restores it on every path. */
    private static ScenarioResult withDelay(int delay, ScenarioPollerFactory body) {
        int saved = GunsConfig.idleReloadDelayTicks;
        GunsConfig.idleReloadDelayTicks = delay;
        ScenarioResult result;
        try {
            result = body.start();
        } catch (RuntimeException | Error e) {
            GunsConfig.idleReloadDelayTicks = saved;
            throw e;
        }
        if (!result.isPending()) {
            GunsConfig.idleReloadDelayTicks = saved;
            return result;
        }
        ScenarioPoller inner = result.poller();
        return ScenarioResult.pending(result.scenario(), result.timeoutTicks(), ticks -> {
            ScenarioResult answer;
            try {
                answer = inner.poll(ticks);
            } catch (RuntimeException | Error e) {
                GunsConfig.idleReloadDelayTicks = saved;
                throw e;
            }
            if (answer != null) {
                GunsConfig.idleReloadDelayTicks = saved;
            }
            return answer;
        });
    }

    /** Builds a scenario's first result. */
    private interface ScenarioPollerFactory {

        ScenarioResult start();
    }

    /** A part-empty revolver held idle tops itself up; a pull stops it. */
    private static final class Idle implements Scenario {

        /** The default delay; the scenario sets it for its run. */
        private static final int DELAY = 40;

        @Override
        public String name() {
            return "gun.revolver.reload.idle";
        }

        @Override
        public String description() {
            return "A revolver with 2 rounds and a 9mm bullet, held without firing, starts loading its chambers by itself after guns.idleReloadDelayTicks (40), not a tick sooner; two chambers later a trigger pull stops it and fires.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            int delay = DELAY;
            return withDelay(delay, () -> {
                GunStats stats = RevolverDef.STATS;
                GunRig rig = rig(ctx, RevolverDef.NAME, 2);
                rig.give(stats.ammoType(), 1);
                int twoChambers = stats.chamberTicksBetween(2, 4);
                int pullAt = delay + twoChambers + 1;
                int end = pullAt + 5;
                boolean[] before = { false };
                boolean[] at = { false };
                int[] loadedBeforePull = { -1 };
                boolean[] reloadingAfterPull = { true };
                int[] shotsBefore = { -1 };
                return ScenarioResult.pending(name(), end + 20, ticks -> {
                    if (ticks == pullAt) {
                        loadedBeforePull[0] = rig.loaded();
                        shotsBefore[0] = rig.bullets();
                        rig.press();
                        rig.release();
                    }
                    rig.tick();
                    if (ticks == delay - 1) {
                        before[0] = rig.reloading();
                    }
                    if (ticks == delay) {
                        at[0] = rig.reloading();
                    }
                    if (ticks == pullAt) {
                        reloadingAfterPull[0] = rig.reloading();
                    }
                    if (ticks < end) {
                        return null;
                    }
                    ScenarioResult.Builder out = ScenarioResult.builder(name());
                    out.check(!before[0], "no idle reload one tick before the delay");
                    out.check(at[0], "the idle reload should start at the delay");
                    out.checkEquals("rounds two chambers in", 4, loadedBeforePull[0]);
                    out.check(!reloadingAfterPull[0], "the pull should stop the idle reload");
                    out.checkEquals("bullets fired by the pull", 1, rig.bullets() - shotsBefore[0]);
                    out.checkEquals("rounds after the shot", 3, rig.loaded());
                    out.checkEquals("credit left from the bullet", 4, credit(rig));
                    out.checkEquals("bullets left", 0, rig.count(stats.ammoType()));
                    return out.build();
                });
            });
        }
    }

    /** At 0 the idle reload never starts. */
    private static final class IdleOff implements Scenario {

        @Override
        public String name() {
            return "gun.revolver.reload.idle_off";
        }

        @Override
        public String description() {
            return "With guns.idleReloadDelayTicks at 0, a revolver with 2 rounds and a 9mm bullet held idle for 200 ticks never starts a reload.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            return withDelay(0, () -> {
                GunRig rig = rig(ctx, RevolverDef.NAME, 2);
                rig.give(RevolverDef.STATS.ammoType(), 1);
                boolean[] ever = { false };
                return ScenarioResult.pending(name(), 220, ticks -> {
                    rig.tick();
                    ever[0] |= rig.reloading();
                    if (ticks < 200) {
                        return null;
                    }
                    ScenarioResult.Builder out = ScenarioResult.builder(name());
                    out.check(!ever[0], "no reload should start with the idle reload off");
                    out.checkEquals("rounds", 2, rig.loaded());
                    out.checkEquals("bullets", 1, rig.count(RevolverDef.STATS.ammoType()));
                    return out.build();
                });
            });
        }
    }
}
