package com.gregguns.icbm.silo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileShotsScenario;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.WarheadCasing;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Screenshot setups for the two silo screens, for one connected client (the client hooks of {@code docs/testing}). The
 * panel is opened by the client's own right-click ({@code gui press}), so it is the screen a player sees. Neither is
 * part of {@code test all}. Dev environment only, server side.
 * <ul>
 * <li>{@code testing.icbm_control_gui_states}: the silo's own Launch Control with no target and no missile, a target
 * and
 * no missile, a target and a ready missile, and a ready missile with the target cleared. Labels {@code lcgui-*}.</li>
 * <li>{@code testing.icbm_silo_gui_sequence}: the silo controller's screen while the silo stands a body, runs its
 * cycle, seats the warhead, arms it and fuels it (D286). Each stage is shot as a burst of frames a few ticks apart, so
 * a status line that flickers or jumps between two texts shows in the burst. Labels {@code silogui-*}.</li>
 * </ul>
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (GUI) and
// docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The assembly sequence)
final class SiloGuiShots {

    /** Frames in one burst. */
    private static final int BURST = 5;
    /** Frames in the burst around a cycle's end, where the screen changes stage. */
    private static final int LONG_BURST = 10;
    /** Ticks between two frames of a burst: a shot takes four rendered frames on the client. */
    private static final int BURST_GAP = 4;
    /** Ticks the burst around a cycle's end starts before the cycle's nominal end. */
    private static final int LEAD = 20;
    /** Ticks the client gets to receive the tiles before a panel is opened. */
    private static final int SYNC_TICKS = 150;

    private SiloGuiShots() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new ControlStates());
        registry.register(new SequenceScreen());
    }

    private static EntityPlayerMP firstPlayer() {
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        return players.isEmpty() ? null : players.get(0);
    }

    /** Creative, empty hands, full health, noon: the same light for every shot. */
    private static void prepare(ScenarioContext ctx, EntityPlayerMP p) {
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.inventory.clearInventory(null, -1);
        p.clearActivePotions();
        p.setHealth(p.getMaxHealth());
        ctx.world()
            .setWorldTime(6000L);
        ctx.world()
            .getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
    }

    private static String str(int v) {
        return Integer.toString(v);
    }

    /** Queues {@link #BURST} shots {@link #BURST_GAP} ticks apart, labeled {@code <label>-a}, {@code -b}, ... */
    private static void burst(MissileShotsScenario.Plan plan, EntityPlayerMP p, String label) {
        burst(plan, p, label, BURST);
    }

    /** Queues {@code frames} shots {@link #BURST_GAP} ticks apart, labeled {@code <label>-a}, {@code -b}, ... */
    private static void burst(MissileShotsScenario.Plan plan, EntityPlayerMP p, String label, int frames) {
        for (int i = 0; i < frames; i++) {
            String frame = label + "-" + (char) ('a' + i);
            plan.after(i == 0 ? 0 : BURST_GAP, () -> MissileShotsScenario.signal(p, "shot", frame));
        }
    }

    /** The silo's own Launch Control in each state its status line has to tell apart. */
    private static final class ControlStates implements Scenario {

        @Override
        public String name() {
            return "testing.icbm_control_gui_states";
        }

        @Override
        public String description() {
            return "Opens the silo's own Launch Control with a client right-click and shoots it with no target and no missile, a target and no missile, a target and a ready missile, and a ready missile with no target. Needs 1 client; not part of test all.";
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
        public ScenarioResult run(ScenarioContext ctx) {
            if (SiloScenarios.gate() != null) {
                return ScenarioResult.skip(name(), SiloScenarios.gate());
            }
            final EntityPlayerMP p = firstPlayer();
            if (p == null) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
            final IGregTechTileEntity cb = SiloScenarios.ownControl(ctx);
            if (cb == null || !(cb.getMetaTileEntity() instanceof MTELaunchControl)) {
                return ScenarioResult.fail(name(), "the silo has no Launch Control of its own");
            }
            cb.setOwnerName(p.getCommandSenderName());
            cb.setOwnerUuid(p.getUniqueID());
            final MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();
            prepare(ctx, p);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
            final EntityMissile[] m = { null };
            final int bx = ctx.originX() + com.gregguns.machine.DevStructureBuilder.SILO_LC[0];
            final int by = ctx.originY() + SiloScenarios.CY + com.gregguns.machine.DevStructureBuilder.SILO_LC[1];
            final int bz = ctx.originZ();

            plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "off"));
            plan.after(SYNC_TICKS, () -> {
                out.check(SiloScenarios.formed(ctx), "the silo forms");
                SiloScenarios.power(ctx);
                // Beside the block, looking at it, the way a player stands to use it.
                MissileShotsScenario.cam(p, bx - 1.5, by + 1.8, bz + 0.5, bx + 0.5, by + 0.5, bz + 0.5);
            });
            plan.after(30, () -> MissileShotsScenario.signal(p, "gui", "press", str(bx), str(by), str(bz)));
            plan.after(60, () -> {
                out.checkEquals("1: no target", ControlStatus.NO_TARGET, c.currentStatus());
                MissileShotsScenario.signal(p, "shot", "lcgui-1-no-target-no-missile");
            });
            plan.after(10, () -> {
                c.setXText(Integer.toString(ctx.originX()));
                c.setZText(Integer.toString(ctx.originZ() + 300));
            });
            plan.after(40, () -> {
                out.checkEquals("2: target, no missile", ControlStatus.NO_MISSILE, c.currentStatus());
                MissileShotsScenario.signal(p, "shot", "lcgui-2-target-no-missile");
            });
            plan.after(10, () -> {
                m[0] = EntityMissile.create(MissileTier.T3, ctx.world());
                m[0].setLocationAndAngles(
                    ctx.originX() + 0.5,
                    ctx.originY() + SiloScenarios.CY - 2 + EntityMissile.PAD_SURFACE,
                    ctx.originZ() + 2.5,
                    0,
                    0);
                m[0].configure(0, "nuclear", 4, 3, null);
                ctx.world()
                    .spawnEntityInWorld(m[0]);
                m[0].fill(m[0].capacityMb(), true);
            });
            plan.after(40, () -> {
                out.checkEquals("3: target and a ready missile", ControlStatus.READY, c.currentStatus());
                MissileShotsScenario.signal(p, "shot", "lcgui-3-target-missile-ready");
            });
            plan.after(10, () -> c.setXText(""));
            plan.after(40, () -> {
                out.checkEquals("4: the target cleared", ControlStatus.NO_TARGET, c.currentStatus());
                MissileShotsScenario.signal(p, "shot", "lcgui-4-no-target-missile-ready");
            });
            plan.after(20, () -> MissileShotsScenario.signal(p, "close"));
            return ScenarioResult.pending(name(), 2000, t -> {
                plan.run(t);
                if (!plan.idle()) {
                    return null;
                }
                if (m[0] != null) {
                    m[0].setDead();
                }
                MissileShotsScenario.signal(p, "hud", "on");
                ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
                return out.note("read the lcgui-*.png shots")
                    .build();
            });
        }
    }

    /** The silo controller's screen through the whole assembly sequence, in bursts. */
    private static final class SequenceScreen implements Scenario {

        @Override
        public String name() {
            return "testing.icbm_silo_gui_sequence";
        }

        @Override
        public String description() {
            return "Opens the silo controller with a client right-click and shoots bursts of its screen while the silo stands a body, runs the body cycle, seats the warhead, arms it and fuels it. Needs 1 client; not part of test all.";
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
        public ScenarioResult run(ScenarioContext ctx) {
            if (SiloScenarios.gate() != null) {
                return ScenarioResult.skip(name(), SiloScenarios.gate());
            }
            final EntityPlayerMP p = firstPlayer();
            if (p == null) {
                return ScenarioResult.fail(name(), "needs a connected player");
            }
            SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
            final IGregTechTileEntity controller = SiloScenarios.tile(ctx, 0, 0, 0);
            if (controller != null) {
                controller.setOwnerName(p.getCommandSenderName());
                controller.setOwnerUuid(p.getUniqueID());
            }
            prepare(ctx, p);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + SiloScenarios.CY;
            final int cz = ctx.originZ();
            final int burstTicks = (BURST - 1) * BURST_GAP;

            plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "off"));
            plan.after(SYNC_TICKS, () -> {
                out.check(SiloScenarios.formed(ctx), "the silo forms");
                // In front of the controller, outside the shaft, looking at it.
                MissileShotsScenario.cam(p, cx + 0.5, cy + 1.1, cz - 2.0, cx + 0.5, cy + 0.5, cz + 0.5);
            });
            plan.after(30, () -> MissileShotsScenario.signal(p, "gui", "press", str(cx), str(cy), str(cz)));
            plan.after(60, () -> {});
            burst(plan, p, "silogui-0-idle");
            plan.after(10, () -> SiloScenarios.putBus(ctx, MissileBodies.body(MissileTier.T1)));
            burst(plan, p, "silogui-1-body-placed");
            plan.after(60, () -> out.checkEquals("1: the body cycle runs", SiloSequence.Stage.BODY, stage(ctx)));
            burst(plan, p, "silogui-2-body-cycle");
            // From the body's placing to here: one burst, 60 ticks, one burst. The next burst starts LEAD ticks before
            // the cycle's nominal end and runs past it, so it spans the change of stage.
            plan.after(SiloRules.BODY_TICKS - (60 + 2 * burstTicks) - LEAD, () -> {});
            burst(plan, p, "silogui-3-body-cycle-ends", LONG_BURST);
            plan.after(40, () -> out.checkEquals("3: the body waits", SiloSequence.Stage.BODY_DONE, stage(ctx)));
            burst(plan, p, "silogui-4-body-waits");
            plan.after(10, () -> SiloScenarios.putBus(ctx, SiloScenarios.warhead(WarheadCasing.T1, "condensed")));
            burst(plan, p, "silogui-5-warhead-placed");
            plan.after(60, () -> out.checkEquals("5: the mating cycle runs", SiloSequence.Stage.WARHEAD, stage(ctx)));
            burst(plan, p, "silogui-6-warhead-cycle");
            plan.after(SiloRules.WARHEAD_TICKS - (60 + 2 * burstTicks) - LEAD, () -> {});
            burst(plan, p, "silogui-7-warhead-cycle-ends", LONG_BURST);
            plan.after(40, () -> {
                MTEMissileSilo s = SiloScenarios.silo(ctx);
                out.check(s != null && s.missile() != null, "7: the missile is armed");
            });
            burst(plan, p, "silogui-8-armed-no-fuel");
            plan.after(10, () -> SiloSequenceScenarios.pour(ctx, out, "nitrofuel"));
            burst(plan, p, "silogui-9-fueling");
            plan.after(100, () -> {});
            burst(plan, p, "silogui-10-fueling-later");
            plan.after(20, () -> MissileShotsScenario.signal(p, "close"));
            return ScenarioResult.pending(name(), 3000, t -> {
                SiloScenarios.power(ctx);
                plan.run(t);
                if (!plan.idle()) {
                    return null;
                }
                MTEMissileSilo s = SiloScenarios.silo(ctx);
                EntityMissile m = s == null ? null : s.missile();
                if (m != null) {
                    m.setDead();
                }
                SiloSequenceScenarios.removeBody(ctx);
                MissileShotsScenario.signal(p, "hud", "on");
                ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
                return out.note("read the silogui-*.png bursts")
                    .build();
            });
        }

        private static SiloSequence.Stage stage(ScenarioContext ctx) {
            MTEMissileSilo s = SiloScenarios.silo(ctx);
            return s == null ? null : s.stage();
        }
    }
}
