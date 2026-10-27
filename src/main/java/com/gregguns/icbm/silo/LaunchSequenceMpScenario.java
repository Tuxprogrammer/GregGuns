package com.gregguns.icbm.silo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.MissileShotsScenario;
import com.gregguns.icbm.missile.MissileTier;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * {@code testing.icbm_mp_silo_launch}: the multiplayer check for the launch sequence (D88, D89). Two or more connected
 * clients watch one silo launch from different sides; every client is told to take the same labeled shots when the
 * doors are closed, half open, open with the countdown smoke, at liftoff and closing. Labels are {@code icbm-i3-*}.
 * Not part of {@code test all}. Dev environment only, server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (The sequence)
public final class LaunchSequenceMpScenario implements Scenario {

    static final String NAME = "testing.icbm_mp_silo_launch";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Two clients watch one silo launch (doors, countdown smoke, liftoff, doors closing) and take matching shots. Needs 2 clients and Galacticraft; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public int areaRadius() {
        return 14;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScenarioResult run(ScenarioContext ctx) {
        if (SiloScenarios.gate() != null) {
            return ScenarioResult.skip(NAME, SiloScenarios.gate());
        }
        final List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.size() < 2) {
            return ScenarioResult.fail(NAME, "needs two connected players, have " + players.size());
        }
        final int oldCountdown = IcbmConfig.countdownTicks;
        final int oldDoors = IcbmConfig.doorOpenTicks;
        IcbmConfig.countdownTicks = 200;
        IcbmConfig.doorOpenTicks = 40;
        SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
        final IGregTechTileEntity cb = SiloScenarios.launchControl(ctx, 5, -2);
        for (EntityPlayerMP p : players) {
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
        }
        ctx.world()
            .setWorldTime(6000L);
        final ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
        final EntityMissile[] m = { null };
        final double cx = ctx.originX() + 0.5;
        final double cz = ctx.originZ() + 2.5;
        final double rim = ctx.originY() + SiloScenarios.CY - 2 + SiloShape.HEIGHT;
        // Two sides of the shaft, level with the rim, looking at its middle.
        final double[][] views = { { 9, 4, 11 }, { -9, 4, 11 }, { 0, 6, 15 } };

        plan.after(5, () -> {
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "hud", "off");
            }
        });
        plan.after(120, () -> {
            out.check(SiloScenarios.formed(ctx), "the silo forms");
            SiloScenarios.power(ctx);
            m[0] = SiloScenarios.stand(ctx, MissileTier.T1);
            m[0].fill(m[0].capacityMb(), true);
            cb.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
            MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();
            c.setXText(Integer.toString(ctx.originX()));
            c.setZText(Integer.toString(ctx.originZ() + 300));
            look(players, views, cx, cz, rim, 0.0);
        });
        shot(plan, players, "icbm-i3-1-closed", 60);
        plan.after(10, () -> {
            MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();
            out.checkEquals("fired", ControlStatus.FIRED, c.fire(null));
        });
        // Half open: 20 ticks into the 40-tick swing.
        shot(plan, players, "icbm-i3-2-opening", 20);
        // Open, counting down; the smoke is thick two thirds in (about 120 ticks into the countdown).
        plan.after(40 + 130, () -> {
            out.checkEquals(
                "doors open",
                SiloDoors.State.OPEN,
                SiloScenarios.silo(ctx)
                    .doors()
                    .state());
            look(players, views, cx, cz, rim, 2.0);
        });
        shot(plan, players, "icbm-i3-3-countdown-smoke", 6);
        // The smoke itself is at the floor, behind the glass rings: a low look from outside.
        plan.after(4, () -> {
            for (int i = 0; i < players.size(); i++) {
                double side = i % 2 == 0 ? 8.0 : -8.0;
                MissileShotsScenario.cam(players.get(i), cx + side, rim - 9.0, cz + 1.0, cx, rim - 10.5, cz);
            }
        });
        shot(plan, players, "icbm-i3-3b-smoke-floor", 8);
        // Liftoff: the countdown has about 70 ticks left; ignition, then a few ticks of climb.
        plan.after(70 + 22, () -> look(players, views, cx, cz, rim, 6.0));
        shot(plan, players, "icbm-i3-4-liftoff", 2);
        // Closing: the doors swing shut once the bottom is 4 blocks clear.
        plan.after(40, new Runnable() {

            @Override
            public void run() {
                SiloDoors.State s = SiloScenarios.silo(ctx)
                    .doors()
                    .state();
                if (s != SiloDoors.State.CLOSING && plan.now < 3000) {
                    plan.after(2, this);
                    return;
                }
                look(players, views, cx, cz, rim, 10.0);
                plan.after(12, () -> {
                    for (EntityPlayerMP p : players) {
                        MissileShotsScenario.signal(p, "shot", "icbm-i3-5-closing");
                    }
                });
            }
        });
        plan.after(80, () -> {
            out.checkEquals(
                "doors closed again",
                SiloDoors.State.CLOSED,
                SiloScenarios.silo(ctx)
                    .doors()
                    .state());
            out.check(m[0] != null && m[0].phase() != FlightPhase.PAD, "the missile flew");
            if (m[0] != null) {
                m[0].tickets()
                    .release();
                m[0].setDead();
            }
        });
        return ScenarioResult.pending(NAME, 3600, t -> {
            plan.run(t);
            if (!plan.idle()) {
                return null;
            }
            IcbmConfig.countdownTicks = oldCountdown;
            IcbmConfig.doorOpenTicks = oldDoors;
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "hud", "on");
            }
            ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
            return out.build();
        });
    }

    private static void look(List<EntityPlayerMP> players, double[][] views, double cx, double cz, double rim,
        double up) {
        for (int i = 0; i < players.size(); i++) {
            double[] v = views[i % views.length];
            MissileShotsScenario.cam(players.get(i), cx + v[0], rim + v[1] + up * 0.4, cz + v[2], cx, rim + up, cz);
        }
    }

    private static void shot(MissileShotsScenario.Plan plan, List<EntityPlayerMP> players, String label, int dt) {
        plan.after(dt, () -> {
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "shot", label);
            }
        });
    }
}
