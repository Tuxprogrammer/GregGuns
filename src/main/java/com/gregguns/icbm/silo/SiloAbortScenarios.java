package com.gregguns.icbm.silo;

import static com.gregguns.icbm.silo.SiloScenarios.CLEAR_HEIGHT;
import static com.gregguns.icbm.silo.SiloScenarios.CY;
import static com.gregguns.icbm.silo.SiloScenarios.WARMUP;
import static com.gregguns.icbm.silo.SiloScenarios.busItems;
import static com.gregguns.icbm.silo.SiloScenarios.formed;
import static com.gregguns.icbm.silo.SiloScenarios.gate;
import static com.gregguns.icbm.silo.SiloScenarios.power;
import static com.gregguns.icbm.silo.SiloScenarios.putBus;
import static com.gregguns.icbm.silo.SiloScenarios.silo;
import static com.gregguns.icbm.silo.SiloSequenceScenarios.bodyStands;
import static com.gregguns.icbm.silo.SiloSequenceScenarios.removeBody;
import static com.gregguns.icbm.silo.SiloSequenceScenarios.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.machine.GregGunsMTEIDs;
import com.mojang.authlib.GameProfile;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Scenarios of a launch that stops before ignition while the next body already waits in the Input Bus (D286's "any
 * order", D342): the shaft counts as busy for the whole countdown, so the silo never stands the next body under the
 * missile; after the stop the missile stands armed and fueled with a clear column and fires again without a player
 * touching the shaft; once it has climbed out, the waiting body goes up. One scenario per way a countdown stops: the
 * Abort button (D88), the stand-down of a silo that comes apart (D90), and the stray-body check of a silo-only body
 * whose controller is gone (D90). Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The assembly sequence, The doors)
final class SiloAbortScenarios {

    private static final int DOORS = 40;
    private static final int TARGET = 300;
    /** The countdown of the relaunch: short, the scenario only needs it to ignite. */
    private static final int RELAUNCH_COUNTDOWN = 40;
    /** The Launch Control's cooldown after an abort, with a margin. */
    private static final int COOLDOWN = 50;
    /** The owner the dev builder gives a silo ({@code DevStructureBuilder}). */
    private static final String OWNER = "gregguns-test";

    private SiloAbortScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        for (Stop stop : Stop.values()) {
            registry.register(new Recovery(stop));
        }
    }

    /** How the countdown is stopped. */
    private enum Stop {

        /** The Launch Control's Abort button (D88). */
        BUTTON("icbm.silo.sequence_abort", MissileTier.T1, 200, "the Abort button stops the countdown"),
        /** A wall block broken for longer than the grace period: the controller stands the launch down (D90). */
        STAND_DOWN("icbm.silo.sequence_stand_down", MissileTier.T1, MTEMissileSilo.UNFORMED_GRACE_TICKS + 300,
            "a wall broken for longer than the grace period stands the launch down, and the wall is mended"),
        /**
         * The controller is gone, so nothing ticks for the silo: the T3's own stray-body check stands it down after 300
         * ticks (D90), and a new controller is placed.
         */
        STRAY("icbm.silo.sequence_stray", MissileTier.T3, 900,
            "with the controller gone, the T3's stray-body check stands it down, and a new controller is placed");

        final String id;
        final MissileTier tier;
        final int countdown;
        final String how;

        Stop(String id, MissileTier tier, int countdown, String how) {
            this.id = id;
            this.tier = tier;
            this.countdown = countdown;
            this.how = how;
        }
    }

    /** Fire with a body waiting in the bus, stop the countdown, relaunch, and see the waiting body go up. */
    private static final class Recovery implements Scenario {

        private final Stop stop;

        Recovery(Stop stop) {
            this.stop = stop;
        }

        @Override
        public String name() {
            return stop.id;
        }

        @Override
        public String description() {
            return "A fueled " + stop.tier
                + " stands in the silo and a second body waits in the Input Bus. It is fired, and "
                + stop.how
                + ". Through the countdown and the stop the body stays in the bus and nothing is placed in the shaft; the missile stands armed and fueled and fires again with no player touching the shaft; once it has climbed out, the waiting body goes up on the pad.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            Rig r = new Rig(ctx, name());
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 3000, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                power(ctx);
                // At no point before the relaunch has climbed out may the silo stand the next body in the shaft.
                if (stage[0] >= 1 && stage[0] <= 4 && bodyStands(ctx)) {
                    return r.fail("the silo stood the waiting body in the shaft while the missile stood there");
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(formed(ctx), "the silo forms");
                        r.arm(stop.tier);
                        putBus(ctx, MissileBodies.body(MissileTier.T1));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 60) {
                            return null;
                        }
                        r.out.checkEquals("the next body waits in the bus", 1, busItems(ctx));
                        r.out.check(status(silo(ctx), "gregguns_silo_full"), "status: the missile is fueled and ready");
                        IcbmConfig.countdownTicks = stop.countdown;
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        r.out.checkEquals("the missile is in its warm-up", FlightPhase.WARMUP, r.m.phase());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        // Long enough for the shaft watch to look several times and for the doors to open.
                        if (ticks < mark[0] + DOORS + 60) {
                            return null;
                        }
                        r.out.checkEquals("still counting down", FlightPhase.WARMUP, r.m.phase());
                        r.out.checkEquals("the next body still waits in the bus", 1, busItems(ctx));
                        r.out.check(
                            status(silo(ctx), "gregguns_silo_launching"),
                            "status: a launch is under way, the shaft is busy");
                        r.stop(stop);
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (r.m.phase() == FlightPhase.WARMUP) {
                            return ticks > mark[0] + 400 ? r.fail("the countdown never stopped") : null;
                        }
                        r.out.checkEquals("the missile stood down on its pad", FlightPhase.PAD, r.m.phase());
                        r.mend(stop);
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (ticks < mark[0] + COOLDOWN || silo(ctx) == null || !silo(ctx).isFormed()) {
                            return ticks > mark[0] + 300 ? r.fail("the silo never formed again") : null;
                        }
                        r.out.checkEquals("still fueled", r.m.capacityMb(), r.m.fuelMb());
                        r.out.checkEquals("the next body still waits in the bus", 1, busItems(ctx));
                        r.c = r.control();
                        IcbmConfig.countdownTicks = RELAUNCH_COUNTDOWN;
                        ControlStatus again = r.c == null ? ControlStatus.NO_PAD : r.c.fire(null);
                        if (again != ControlStatus.FIRED) {
                            return r.fail("the relaunch was refused: " + again);
                        }
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    case 5:
                        if (r.m.phase() == FlightPhase.WARMUP || r.m.phase() == FlightPhase.PAD) {
                            return ticks > mark[0] + DOORS + RELAUNCH_COUNTDOWN + 100 ? r.fail("never ignited") : null;
                        }
                        r.out.checkEquals("liftoff", FlightPhase.CLIMB, r.m.phase());
                        mark[0] = ticks;
                        stage[0] = 6;
                        return null;
                    case 6:
                        if (!bodyStands(ctx)) {
                            return ticks > mark[0] + 400
                                ? r.fail("the waiting body never went up after the missile left")
                                : null;
                        }
                        r.out.checkEquals("the body left the bus", 0, busItems(ctx));
                        r.out.check(
                            r.m.isDead || r.m.posY > r.padY() + 12,
                            "the body went up only once the missile was out of the shaft");
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }
    }

    /** The silo with its own Launch Control, a fueled missile in the shaft, and the config the scenario changes. */
    private static final class Rig {

        final ScenarioContext ctx;
        final String name;
        final ScenarioResult.Builder out;
        final int oldCountdown = IcbmConfig.countdownTicks;
        final int oldDoors = IcbmConfig.doorOpenTicks;
        MTELaunchControl c;
        EntityMissile m;
        FakePlayer player;

        Rig(ScenarioContext ctx, String name) {
            this.ctx = ctx;
            this.name = name;
            this.out = ScenarioResult.builder(name);
            IcbmConfig.doorOpenTicks = DOORS;
            SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
        }

        int x() {
            return ctx.originX();
        }

        int y() {
            return ctx.originY() + CY;
        }

        int z() {
            return ctx.originZ();
        }

        int padY() {
            return y() - SiloShape.FLOOR_SURFACE_BELOW_CONTROLLER - 1;
        }

        MTELaunchControl control() {
            IGregTechTileEntity t = SiloScenarios.ownControl(ctx);
            return t != null && t.getMetaTileEntity() instanceof MTELaunchControl
                ? (MTELaunchControl) t.getMetaTileEntity()
                : null;
        }

        /** Stands a fueled missile in the shaft and aims the silo's own Launch Control. */
        void arm(MissileTier tier) {
            c = control();
            m = SiloScenarios.stand(ctx, tier);
            m.fill(m.capacityMb(), true);
            c.setXText(Integer.toString(x()));
            c.setZText(Integer.toString(z() + TARGET));
            player = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes("gg-abort".getBytes(StandardCharsets.UTF_8)), "[GGAbort]"));
            player.setPosition(x() + 5.5, y() + 1, z() - 1.5);
        }

        /** Stops the countdown the way the scenario names. */
        void stop(Stop how) {
            World w = ctx.world();
            switch (how) {
                case BUTTON:
                    out.checkEquals("the Abort button stops the countdown", true, c.abortFromButton(player));
                    break;
                case STAND_DOWN:
                    w.setBlock(x() - 1, y() + 2, z(), Blocks.stone, 0, 3);
                    out.check(!formed(ctx), "the silo is broken");
                    break;
                case STRAY:
                default:
                    w.setBlockToAir(x(), y(), z());
                    out.check(silo(ctx) == null, "the controller is gone");
                    break;
            }
        }

        /** Puts back what {@link #stop} broke. */
        void mend(Stop how) {
            World w = ctx.world();
            switch (how) {
                case STAND_DOWN:
                    w.setBlock(x() - 1, y() + 2, z(), gregtech.api.GregTechAPI.sBlockConcretes, SiloConcrete.LIGHT, 2);
                    out.check(formed(ctx), "the silo forms again");
                    break;
                case STRAY:
                    placeController();
                    break;
                case BUTTON:
                default:
                    break;
            }
        }

        /** A new controller where the old one stood, facing north and owned as the dev builder does it. */
        private void placeController() {
            IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.MISSILE_SILO];
            ctx.world()
                .setBlock(x(), y(), z(), GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType(), 3);
            TileEntity t = ctx.world()
                .getTileEntity(x(), y(), z());
            if (!(t instanceof IGregTechTileEntity)) {
                out.check(false, "a new controller could be placed");
                return;
            }
            IGregTechTileEntity b = (IGregTechTileEntity) t;
            b.setInitialValuesAsNBT(null, (short) GregGunsMTEIDs.MISSILE_SILO);
            b.setFrontFacing(ForgeDirection.NORTH);
            b.setOwnerName(OWNER);
            b.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
        }

        void restore() {
            IcbmConfig.countdownTicks = oldCountdown;
            IcbmConfig.doorOpenTicks = oldDoors;
            if (m != null && !m.isDead) {
                m.tickets()
                    .release();
                m.setDead();
            }
            removeBody(ctx);
            ctx.clearArea(14, CLEAR_HEIGHT);
        }

        ScenarioResult fail(String why) {
            MTEMissileSilo s = silo(ctx);
            String where = " (phase " + (m == null ? "none" : m.phase())
                + ", bus "
                + busItems(ctx)
                + ", silo "
                + (s == null ? "gone"
                    : (s.isFormed() ? "formed" : "unformed") + " "
                        + s.stage()
                        + " "
                        + SiloSequenceScenarios.statusOf(s))
                + ")";
            restore();
            return ScenarioResult.fail(name, why + where);
        }
    }
}
