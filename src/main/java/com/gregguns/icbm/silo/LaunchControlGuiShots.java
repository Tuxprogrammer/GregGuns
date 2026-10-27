package com.gregguns.icbm.silo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
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
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * {@code testing.icbm_i3_gui_shots}: a real client opens the Launch Control of a silo by right-click, watches the
 * panel through a countdown, aborts it with a real mouse click on the Abort button, then launches again and watches
 * the panel at ignition. The block is owned by the connected player and the scenario waits for the client to have the
 * tile before it opens the panel, so this is the path a player takes.
 *
 * <p>
 * The clicks are not sent by the server: the driver script clicks when the client logs a {@code i3gui-click-*}
 * shot (see {@code tools/scenarios/click_on_log.sh}), and the scenario then checks on the server that the countdown
 * stopped and the doors closed. Labels are {@code i3gui-*}. Not part of {@code test all}. Dev environment only.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (The Launch Control panel)
public final class LaunchControlGuiShots implements Scenario {

    static final String NAME = "testing.icbm_i3_gui_shots";
    /** Ticks the client gets to receive the tile before the panel is opened. */
    private static final int SYNC_TICKS = 120;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "A client opens the silo's Launch Control by right-click, watches the countdown, aborts with a mouse click, launches again and watches ignition. Needs 1 client, an external click and Galacticraft; not part of test all.";
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
            return ScenarioResult.skip(NAME, SiloScenarios.gate());
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        final EntityPlayerMP p = players.get(0);
        final int oldCountdown = IcbmConfig.countdownTicks;
        final int oldDoors = IcbmConfig.doorOpenTicks;
        IcbmConfig.countdownTicks = 600;
        IcbmConfig.doorOpenTicks = 60;
        SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
        final IGregTechTileEntity cb = SiloScenarios.launchControl(ctx, 5, -2);
        cb.setOwnerName(p.getCommandSenderName());
        cb.setOwnerUuid(p.getUniqueID());
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.inventory.clearInventory(null, -1);
        p.clearActivePotions();
        p.setHealth(p.getMaxHealth());
        ctx.world()
            .setWorldTime(6000L);
        final ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
        final EntityMissile[] m = { null };
        final double cx = ctx.originX() + 0.5;
        final double cz = ctx.originZ() + 2.5;
        final int bx = ctx.originX() + 5;
        final int by = ctx.originY() + SiloScenarios.CY;
        final int bz = ctx.originZ() - 2;
        final MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();

        plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "off"));
        plan.after(SYNC_TICKS, () -> {
            out.check(SiloScenarios.formed(ctx), "the silo forms");
            SiloScenarios.power(ctx);
            m[0] = EntityMissile.create(MissileTier.T3, ctx.world());
            m[0].setLocationAndAngles(cx, ctx.originY() + SiloScenarios.CY - 2 + EntityMissile.PAD_SURFACE, cz, 0, 0);
            m[0].configure(0, "nuclear", 4, 3, null);
            ctx.world()
                .spawnEntityInWorld(m[0]);
            m[0].fill(m[0].capacityMb(), true);
            cb.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
            c.setXText(Integer.toString(ctx.originX()));
            c.setZText(Integer.toString(ctx.originZ() + 300));
            // Beside the block, looking at it, the way a player stands to use it.
            MissileShotsScenario.cam(p, bx - 1.5, by + 1.8, bz + 0.5, bx + 0.5, by + 0.5, bz + 0.5);
        });
        // The real path: a client right-click on the block; the server opens the panel.
        plan.after(30, () -> MissileShotsScenario.signal(p, "gui", "press", str(bx), str(by), str(bz)));
        plan.after(60, () -> MissileShotsScenario.signal(p, "shot", "i3gui-1-ready"));
        plan.after(10, () -> out.checkEquals("ready", ControlStatus.READY, c.currentStatus()));
        // The driver clicks Launch when it sees this label in the client log; the status must follow without
        // reopening the panel.
        plan.after(10, () -> MissileShotsScenario.signal(p, "shot", "i3gui-click-launch"));
        plan.after(80, () -> {
            out.checkEquals("the click launched: countdown", ControlStatus.COUNTDOWN, c.currentStatus());
            out.checkEquals("the Launch button took the shot", ControlStatus.FIRED, c.lastShot());
            MissileShotsScenario.signal(p, "shot", "i3gui-2-countdown-doors-opening");
        });
        plan.after(100, () -> {
            out.checkEquals("countdown state", ControlStatus.COUNTDOWN, c.currentStatus());
            MissileShotsScenario.signal(p, "shot", "i3gui-3-countdown");
        });
        // The driver clicks Abort when it sees this label.
        plan.after(20, () -> MissileShotsScenario.signal(p, "shot", "i3gui-click-abort"));
        plan.after(80, () -> {
            out.check(m[0].phase() == FlightPhase.PAD, "the click aborted the countdown (phase " + m[0].phase() + ")");
            out.check(c.currentStatus() == ControlStatus.READY, "the missile stays armed: " + c.currentStatus());
            MissileShotsScenario.signal(p, "shot", "i3gui-4-after-abort");
        });
        plan.after(
            70,
            () -> out.checkEquals(
                "doors closed again",
                SiloDoors.State.CLOSED,
                SiloScenarios.silo(ctx)
                    .doors()
                    .state()));
        // Second launch with a short countdown, started by a click: the panel at ignition.
        plan.after(10, () -> {
            IcbmConfig.countdownTicks = 100;
            MissileShotsScenario.signal(p, "shot", "i3gui-click-launch2");
        });
        plan.after(
            80,
            () -> out.check(
                c.lastShot() == ControlStatus.FIRED && c.currentStatus() == ControlStatus.COUNTDOWN,
                "the second click launched again after the abort: last shot " + c
                    .lastShot() + ", now " + c.currentStatus() + ", phase " + m[0].phase()));
        final Runnable placement = () -> {
            // A second block placed the way a player does it: the client right-clicks a wall with the item in hand,
            // then
            // right-clicks the new block. The tile reaches the client through GT's own sync, as in play.
            final int wx = ctx.originX() - 6;
            final int wy = ctx.originY() + SiloScenarios.CY;
            final int wz = ctx.originZ() - 6;
            plan.after(10, () -> {
                ctx.setBlock(-6, SiloScenarios.CY, -6, Blocks.stone, 0);
                p.inventory.setInventorySlotContents(
                    0,
                    GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.LAUNCH_CONTROL].getStackForm(1));
                p.inventoryContainer.detectAndSendChanges();
                p.inventory.currentItem = 0;
                p.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(0));
                MissileShotsScenario.cam(p, wx - 2.5, wy + 1.8, wz + 0.5, wx + 0.5, wy + 0.5, wz + 0.5);
            });
            // Side 4 is the west face: the block lands at wx - 1.
            plan.after(30, () -> MissileShotsScenario.signal(p, "gui", "press", str(wx), str(wy), str(wz)));
            plan.after(60, () -> {
                IGregTechTileEntity placed = SiloScenarios.tile(ctx, -7, 0, -6);
                out.check(
                    placed != null && placed.getMetaTileEntity() instanceof MTELaunchControl,
                    "the client's placement made a Launch Control on the server");
                MissileShotsScenario.signal(p, "gui", "press", str(wx - 1), str(wy), str(wz));
            });
            plan.after(60, () -> MissileShotsScenario.signal(p, "shot", "i3gui-6-placed-by-player"));
            plan.after(10, () -> MissileShotsScenario.signal(p, "close"));
        };
        plan.after(2, new Runnable() {

            @Override
            public void run() {
                if (m[0].phase() == FlightPhase.CLIMB) {
                    out.checkEquals("ignited state", ControlStatus.IGNITED, c.currentStatus());
                    MissileShotsScenario.signal(p, "shot", "i3gui-5-ignited");
                    plan.after(40, () -> {
                        MissileShotsScenario.signal(p, "close");
                        m[0].tickets()
                            .release();
                        m[0].setDead();
                    });
                    placement.run();
                } else if (plan.now < 4000) {
                    plan.after(1, this);
                } else {
                    out.check(false, "the missile never ignited");
                }
            }
        });

        return ScenarioResult.pending(NAME, 6000, t -> {
            plan.run(t);
            if (!plan.idle()) {
                return null;
            }
            IcbmConfig.countdownTicks = oldCountdown;
            IcbmConfig.doorOpenTicks = oldDoors;
            MissileShotsScenario.signal(p, "hud", "on");
            ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
            return out.build();
        });
    }

    private static String str(int v) {
        return Integer.toString(v);
    }
}
