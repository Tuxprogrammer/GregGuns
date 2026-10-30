package com.gregguns.icbm.silo;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.missile.ArcPath;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.MissileScenarios;
import com.gregguns.icbm.missile.MissileTier;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * D285: a silo launch climbs straight up through the open doors to the build height before it turns. The silo is
 * buried the way a player builds it: a stone roof lies on the rim around the 3 x 3 opening and a chimney of stone
 * stands on the rim around the opening. The old flight turned onto its arc twelve blocks up, right at the rim, and
 * flew into the chimney; this one must stay on the shaft's column until it is above the build height, break no block
 * of the silo, the roof or the chimney, and still detonate on its target. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/flight.md (The climb), missile-silo.md (Tests)
final class SiloClimbScenario implements Scenario {

    /** The roof reaches this far around the shaft, blocks. */
    private static final int ROOF = 8;
    /** Layers of chimney on the rim above the roof. */
    private static final int CHIMNEY = 8;
    /** The test's countdown: short, the climb is what is tested. */
    private static final int COUNTDOWN = 20;

    @Override
    public String name() {
        return "icbm.silo.climb_clear";
    }

    @Override
    public String description() {
        return "A silo under a stone roof with a chimney around its opening launches a missile that stays on the shaft's column until it is above the build height, breaks no block of the silo, roof or chimney, and detonates on its target.";
    }

    @Override
    public int areaRadius() {
        return 14;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (SiloScenarios.gate() != null) {
            return ScenarioResult.skip(name(), SiloScenarios.gate());
        }
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        out.note("build: " + SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV));
        IGregTechTileEntity cb = SiloScenarios.launchControl(ctx, 5, -2);
        final int oldCountdown = IcbmConfig.countdownTicks;
        final boolean oldTerrain = IcbmConfig.terrainDamageT1;
        IcbmConfig.countdownTicks = COUNTDOWN;
        IcbmConfig.terrainDamageT1 = true;
        final MissileScenarios.Target target = new MissileScenarios.Target(ctx, 0, SiloScenarios.DISTANCE, 9);
        final World w = ctx.world();
        // The pad is at the origin's level; the rim's top is twelve blocks up (SiloShape.HEIGHT).
        final int rimTop = SiloShape.HEIGHT;
        final double shaftX = ctx.originX() + 0.5;
        final double shaftZ = ctx.originZ() + 2.5;
        int[] stage = { 0 };
        int[] mark = { 0 };
        int[] blocksBefore = { 0 };
        // Where the missile first left the shaft's column, and where the arc began.
        double[] leftColumnAt = { -1.0 };
        double[] arcStartY = { -1.0 };
        double[] maxY = { 0.0 };
        double[] last = new double[3];
        EntityMissile[] m = new EntityMissile[1];
        return ScenarioResult.pending(name(), 1800, ticks -> {
            if (ticks < SiloScenarios.WARMUP) {
                return null;
            }
            MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();
            switch (stage[0]) {
                case 0: {
                    out.check(SiloScenarios.formed(ctx), "the silo forms");
                    roof(ctx, rimTop);
                    out.check(SiloScenarios.formed(ctx), "the silo is still formed under its roof");
                    blocksBefore[0] = countBlocks(ctx, rimTop);
                    SiloScenarios.power(ctx);
                    m[0] = SiloScenarios.stand(ctx, MissileTier.T1);
                    if (m[0] == null) {
                        return finish(ctx, oldCountdown, oldTerrain, ScenarioResult.fail(name(), "no missile stood"));
                    }
                    m[0].fill(m[0].capacityMb(), true);
                    cb.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
                    c.setXText(Integer.toString(ctx.originX()));
                    c.setZText(Integer.toString(ctx.originZ() + SiloScenarios.DISTANCE));
                    mark[0] = ticks;
                    stage[0] = 1;
                    return null;
                }
                case 1:
                    // Let the controller set up its door entity, then fire.
                    if (ticks < mark[0] + 20) {
                        return null;
                    }
                    out.checkEquals("the Launch Control fires the silo", ControlStatus.FIRED, c.fire(null));
                    mark[0] = ticks;
                    stage[0] = 2;
                    return null;
                case 2: {
                    EntityMissile e = m[0];
                    if (!e.isDead) {
                        last[0] = e.posX;
                        last[1] = e.posY;
                        last[2] = e.posZ;
                        maxY[0] = Math.max(maxY[0], e.posY);
                        // The first tick off the shaft's column must be the arc's first step, from the build height:
                        // a missile that leaves its column lower is turning before it has cleared what stands over
                        // the silo.
                        if (leftColumnAt[0] < 0 && Math.hypot(e.posX - shaftX, e.posZ - shaftZ) > 1e-6) {
                            leftColumnAt[0] = e.posY;
                        }
                        if (arcStartY[0] < 0 && e.phase() == FlightPhase.ARC) {
                            arcStartY[0] = e.posY;
                        }
                        if (ticks > mark[0] + 1500) {
                            return finish(
                                ctx,
                                oldCountdown,
                                oldTerrain,
                                ScenarioResult.fail(
                                    name(),
                                    "still flying: phase " + e
                                        .phase() + " at " + (int) e.posX + ", " + (int) e.posY + ", " + (int) e.posZ));
                        }
                        return null;
                    }
                    // The crater is dug over a few ticks (D87): wait for the first broken block.
                    if (target.removed(w) == 0 && ticks < mark[0] + 1600) {
                        return null;
                    }
                    out.check(
                        leftColumnAt[0] >= w.getHeight() - ArcPath.MAX_DIVE,
                        "the missile stayed on the shaft's column through the roof and chimney up to the build height"
                            + " (left it at y "
                            + leftColumnAt[0]
                            + ")");
                    out.check(
                        arcStartY[0] >= w.getHeight() - 1e-6,
                        "it turned onto its arc only at the build height (arc began at y " + arcStartY[0] + ")");
                    out.checkEquals(
                        "no block of the silo, the roof or the chimney was broken",
                        blocksBefore[0],
                        countBlocks(ctx, rimTop));
                    out.check(SiloScenarios.formed(ctx), "the silo is still formed after its launch");
                    double[] aim = e.aimPoint();
                    out.check(
                        Math.hypot(last[0] - aim[0], last[2] - aim[2]) < 6.0,
                        "it came down at its aim point (" + e.impactReport() + ")");
                    out.check(target.removed(w) > 0, "the blast broke blocks on the target platform");
                    out.checkEquals(
                        "no chunk tickets are left",
                        0,
                        e.tickets()
                            .tickets());
                    out.note("highest y " + (int) maxY[0] + "; " + e.impactReport());
                    return finish(ctx, oldCountdown, oldTerrain, out.build());
                }
                default:
                    return null;
            }
        });
    }

    /**
     * Lays a stone roof on the rim around the 3 x 3 opening (x -1..1, z 1..3 from the controller) and a chimney of
     * stone on the rim ring around it, as a buried silo has.
     */
    private static void roof(ScenarioContext ctx, int rimTop) {
        for (int x = -ROOF; x <= ROOF; x++) {
            for (int z = 2 - ROOF; z <= 2 + ROOF; z++) {
                boolean opening = Math.abs(x) <= 1 && Math.abs(z - 2) <= 1;
                if (opening) {
                    continue;
                }
                ctx.setBlock(x, rimTop, z, Blocks.stone, 0);
                if (Math.abs(x) <= 2 && Math.abs(z - 2) <= 2) {
                    for (int y = rimTop + 1; y <= rimTop + CHIMNEY; y++) {
                        ctx.setBlock(x, y, z, Blocks.stone, 0);
                    }
                }
            }
        }
    }

    /** @return non-air blocks of the silo, its roof and its chimney */
    private static int countBlocks(ScenarioContext ctx, int rimTop) {
        int n = 0;
        for (int x = -ROOF; x <= ROOF; x++) {
            for (int z = 2 - ROOF; z <= 2 + ROOF; z++) {
                for (int y = -1; y <= rimTop + CHIMNEY; y++) {
                    Block b = ctx.world()
                        .getBlock(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z);
                    if (b != Blocks.air) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static ScenarioResult finish(ScenarioContext ctx, int countdown, boolean terrain, ScenarioResult r) {
        IcbmConfig.countdownTicks = countdown;
        IcbmConfig.terrainDamageT1 = terrain;
        MissileScenarios.sweep(ctx);
        ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
        return r;
    }
}
