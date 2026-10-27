package com.gregguns.icbm.missile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.emp.EmpMode;
import com.gregguns.icbm.emp.EmpPulse;
import com.gregguns.icbm.emp.EmpScenarios;
import com.gregguns.icbm.emp.MTEEmpTower;
import com.gregguns.icbm.emp.MissileTable;
import com.gregguns.icbm.emp.MissileTable.Outcome;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Exit box "an EMP against a missile in flight" (D16): a pulse with each chain outcome forced destroys, falls,
 * triggers or spares a T1 missile, and a real EMP Tower with the kill chance forced to 1 knocks one down. It lives in
 * this package for the stacking helpers of {@link MissileScenarios}; the EMP track owns it. Dev environment only.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (Missiles)
final class EmpMissileScenarios {

    private EmpMissileScenarios() {}

    /**
     * Loads the chunks under a target platform before the platform is built: a block set in an unloaded chunk is lost,
     * and "the platform is untouched" would then read as a wall of air.
     */
    static MissileScenarios.Target platform(ScenarioContext ctx, int dx, int dz, int radius) {
        World w = ctx.world();
        for (int a = -radius; a <= radius; a += 8) {
            for (int b = -radius; b <= radius; b += 8) {
                w.getChunkProvider()
                    .provideChunk((ctx.originX() + dx + a) >> 4, (ctx.originZ() + dz + b) >> 4);
            }
        }
        return new MissileScenarios.Target(ctx, dx, dz, radius);
    }

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Forced(Outcome.DESTROYED, new MissileTable(1, 0, 0)));
        registry.register(new Forced(Outcome.FRIED, new MissileTable(0, 1, 0)));
        registry.register(new Forced(Outcome.TRIGGERED, new MissileTable(0, 0, 1)));
        registry.register(new Forced(Outcome.UNAFFECTED, new MissileTable(0, 0, 0)));
        registry.register(new FromTower());
    }

    /** A pulse with one outcome forced, at a missile early in its arc. */
    private static final class Forced implements Scenario {

        private final Outcome outcome;
        private final MissileTable table;

        Forced(Outcome outcome, MissileTable table) {
            this.outcome = outcome;
            this.table = table;
        }

        @Override
        public String name() {
            return "icbm.emp.missile_" + outcome.name()
                .toLowerCase(java.util.Locale.ROOT);
        }

        @Override
        public String description() {
            switch (outcome) {
                case DESTROYED:
                    return "An EMP pulse that destroys a missile in its arc removes it at once and its warhead never detonates on the target.";
                case FRIED:
                    return "An EMP pulse that fries a missile's guidance makes it fall and detonate short of its target, with no chunk tickets left.";
                case TRIGGERED:
                    return "An EMP pulse that triggers a missile ends it at once, short of its target.";
                default:
                    return "An EMP pulse that spares a missile leaves it flying on its arc.";
            }
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (MissileScenarios.moduleOff()) {
                return ScenarioResult.fail(name(), "the missile content is not registered");
            }
            MissileScenarios.sweep(ctx);
            ctx.clearArea(8, 20);
            boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = true;
            EntityMissile m = MissileScenarios.armed(ctx, MissileTier.T1, MissileBodies.MAX_GUIDANCE);
            if (m == null) {
                IcbmConfig.terrainDamageT1 = terrain;
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            World w = ctx.world();
            MissileScenarios.Target target = platform(ctx, 0, 70, 9);
            if (target.removed(ctx.world()) != 0) {
                return ScenarioResult.fail(name(), "the target platform was not placed (its chunk is not loaded)");
            }
            EntityPlayer player = ctx.player();
            LaunchResult r = PadMissiles.launch(
                w,
                ctx.originX(),
                ctx.originY(),
                ctx.originZ(),
                ctx.originX() + 0.5,
                ctx.originZ() + 70.5,
                player);
            if (r != LaunchResult.OK) {
                IcbmConfig.terrainDamageT1 = terrain;
                return ScenarioResult.fail(name(), "launch refused: " + r);
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            return ScenarioResult.pending(name(), 1200, ticks -> {
                if (stage[0] == 0) {
                    if (m.phase() != FlightPhase.ARC) {
                        return ticks > 600 ? finish(terrain, ScenarioResult.fail(name(), "never reached the arc"))
                            : null;
                    }
                    EmpPulse.Result res = EmpPulse
                        .fire(w, m.posX, m.posY, m.posZ, 20, EmpMode.MISSILES, 64, table, null);
                    int hit = res.missilesDestroyed + res.missilesFried + res.missilesTriggered;
                    out.checkEquals("the pulse counted its outcome", outcome == Outcome.UNAFFECTED ? 0 : 1, hit);
                    switch (outcome) {
                        case DESTROYED:
                        case TRIGGERED:
                            out.check(m.isDead, "the missile is gone at once");
                            break;
                        case FRIED:
                            out.check(!m.isDead && m.guidanceFried(), "the guidance is fried and it is falling");
                            break;
                        default:
                            out.check(!m.isDead && !m.guidanceFried(), "the missile flies on");
                            break;
                    }
                    mark[0] = ticks;
                    stage[0] = 1;
                    return null;
                }
                // Give a falling missile time to land and a blast time to run.
                if (ticks < mark[0] + (outcome == Outcome.UNAFFECTED ? 40 : 300)) {
                    return null;
                }
                if (outcome == Outcome.UNAFFECTED) {
                    out.check(!m.isDead && m.phase() == FlightPhase.ARC, "it is still on its arc");
                    m.tickets()
                        .release();
                    m.setDead();
                } else {
                    out.check(m.isDead, "the missile ended");
                    out.checkEquals("the target platform is untouched", 0, target.removed(w));
                    out.checkEquals(
                        "no chunk tickets are left",
                        0,
                        m.tickets()
                            .tickets());
                }
                return finish(terrain, out.build());
            });
        }

        private static ScenarioResult finish(boolean terrain, ScenarioResult result) {
            IcbmConfig.terrainDamageT1 = terrain;
            return result;
        }
    }

    /** A real tower, with the kill chance forced to 1, knocks a climbing missile down. */
    private static final class FromTower implements Scenario {

        @Override
        public String name() {
            return "icbm.emp.tower_missile";
        }

        @Override
        public String description() {
            return "A charged EMP Tower within range fires at a missile in flight (kill chance forced to 1): the missile is destroyed, the target is untouched and the tower paid its cost.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (MissileScenarios.moduleOff()) {
                return ScenarioResult.fail(name(), "the missile content is not registered");
            }
            MissileScenarios.sweep(ctx);
            ctx.clearArea(8, 20);
            boolean terrain = IcbmConfig.terrainDamageT1;
            double kill = IcbmConfig.empMissileKillChance;
            IcbmConfig.terrainDamageT1 = true;
            IcbmConfig.empMissileKillChance = 1.0;
            EntityMissile m = MissileScenarios.armed(ctx, MissileTier.T1, MissileBodies.MAX_GUIDANCE);
            if (m == null) {
                IcbmConfig.terrainDamageT1 = terrain;
                IcbmConfig.empMissileKillChance = kill;
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            World w = ctx.world();
            MissileScenarios.Target target = platform(ctx, 0, 70, 9);
            if (target.removed(ctx.world()) != 0) {
                return ScenarioResult.fail(name(), "the target platform was not placed (its chunk is not loaded)");
            }
            EmpScenarios.place(ctx, 4, 0, 4, GregGunsMTEIDs.EMP_TOWER);
            LaunchResult r = PadMissiles.launch(
                w,
                ctx.originX(),
                ctx.originY(),
                ctx.originZ(),
                ctx.originX() + 0.5,
                ctx.originZ() + 70.5,
                ctx.player());
            if (r != LaunchResult.OK) {
                IcbmConfig.terrainDamageT1 = terrain;
                IcbmConfig.empMissileKillChance = kill;
                return ScenarioResult.fail(name(), "launch refused: " + r);
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final long[] before = { 0 };
            return ScenarioResult.pending(name(), 900, ticks -> {
                IGregTechTileEntity base = EmpScenarios.live(ctx, 4, 0, 4);
                MTEEmpTower t = EmpScenarios.tower(base);
                if (stage[0] == 0) {
                    if (t == null || !m.inFlight()) {
                        return ticks > 500 ? restore(terrain, kill, ScenarioResult.fail(name(), "no flight to hit"))
                            : null;
                    }
                    t.setRadius(40);
                    ((gregtech.api.metatileentity.BaseMetaTileEntity) base).setStoredEU(t.cost() + 321);
                    before[0] = base.getStoredEU();
                    out.checkEquals(
                        "the tower fires at the missile",
                        com.gregguns.icbm.emp.EmpRules.Gate.READY,
                        t.fire());
                    out.check(m.isDead, "the missile was destroyed");
                    out.checkEquals("the tower paid its cost", before[0] - t.cost(), base.getStoredEU());
                    mark[0] = ticks;
                    stage[0] = 1;
                    return null;
                }
                if (ticks < mark[0] + 200) {
                    return null;
                }
                out.checkEquals("the target platform is untouched", 0, target.removed(w));
                return restore(terrain, kill, out.build());
            });
        }

        private static ScenarioResult restore(boolean terrain, double kill, ScenarioResult result) {
            IcbmConfig.terrainDamageT1 = terrain;
            IcbmConfig.empMissileKillChance = kill;
            return result;
        }
    }
}
