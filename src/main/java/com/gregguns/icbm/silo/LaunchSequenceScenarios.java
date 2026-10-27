package com.gregguns.icbm.silo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.missile.PadMissiles;
import com.mojang.authlib.GameProfile;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Scenarios of the launch sequence (D88 to D90): the silo doors open, a countdown runs, the missile lifts off and the
 * doors close behind it; abort works until ignition; a pad has the countdown but no doors; the door state survives a
 * save and a silo that comes apart stands down. They run the real 200-tick countdown and 40-tick doors (the dev server
 * otherwise uses a short countdown) and put the config back. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md, 03-missiles/missile-silo.md (The doors)
public final class LaunchSequenceScenarios {

    private static final int COUNTDOWN = 200;
    private static final int DOORS = 40;
    private static final int TARGET = 300;

    private LaunchSequenceScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Sequence());
        registry.register(new LongCountdown());
        registry.register(new Abort());
        registry.register(new NoAbortAfterIgnition());
        registry.register(new PadSequence());
        registry.register(new Reload());
        registry.register(new Unform());
    }

    /** The silo, a Launch Control beside it, a fueled T1 in the shaft, and the config set for the real sequence. */
    private static final class Rig {

        final ScenarioContext ctx;
        final String name;
        final ScenarioResult.Builder out;
        final int oldCountdown = IcbmConfig.countdownTicks;
        final int oldDoors = IcbmConfig.doorOpenTicks;
        final int oldClose = IcbmConfig.doorCloseHeight;
        IGregTechTileEntity cb;
        MTELaunchControl c;
        EntityMissile m;
        FakePlayer player;

        Rig(ScenarioContext ctx, String name) {
            this.ctx = ctx;
            this.name = name;
            this.out = ScenarioResult.builder(name);
            IcbmConfig.countdownTicks = COUNTDOWN;
            IcbmConfig.doorOpenTicks = DOORS;
            IcbmConfig.doorCloseHeight = 4;
            SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
            cb = SiloScenarios.launchControl(ctx, 5, -2);
        }

        int x() {
            return ctx.originX();
        }

        int y() {
            return ctx.originY() + SiloScenarios.CY;
        }

        int z() {
            return ctx.originZ();
        }

        /** @return Y of the top of the rim layer, where the panels lie */
        int rimTop() {
            return y() - 2 + SiloShape.HEIGHT;
        }

        /** The silo has formed: stand a fueled missile in it and aim the control. */
        void arm() {
            SiloScenarios.power(ctx);
            c = (MTELaunchControl) cb.getMetaTileEntity();
            m = SiloScenarios.stand(ctx, MissileTier.T1);
            m.fill(m.capacityMb(), true);
            cb.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 20L, true);
            c.setXText(Integer.toString(x()));
            c.setZText(Integer.toString(z() + TARGET));
            player = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes("gg-seq".getBytes(StandardCharsets.UTF_8)), "[GGSeq]"));
            player.setPosition(x() + 5.5, y() + 1, z() - 1.5);
        }

        MTEMissileSilo silo() {
            return SiloScenarios.silo(ctx);
        }

        SiloDoors.State doors() {
            return silo().doors()
                .state();
        }

        EntitySiloDoors doorEntity() {
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(x() - 1.0, rimTop() - 2.0, z() + 1.0, x() + 2.0, rimTop() + 2.0, z() + 4.0);
            for (Object o : ctx.world()
                .getEntitiesWithinAABB(EntitySiloDoors.class, box)) {
                if (!((EntitySiloDoors) o).isDead) {
                    return (EntitySiloDoors) o;
                }
            }
            return null;
        }

        void restore() {
            IcbmConfig.countdownTicks = oldCountdown;
            IcbmConfig.doorOpenTicks = oldDoors;
            IcbmConfig.doorCloseHeight = oldClose;
            if (m != null && !m.isDead) {
                m.tickets()
                    .release();
                m.setDead();
            }
            ctx.clearArea(14, SiloScenarios.CLEAR_HEIGHT);
        }

        ScenarioResult fail(String why) {
            String where = " (phase " + m.phase()
                + ", doors "
                + doors()
                + ", countdown "
                + m.countdownLeft()
                + ", control "
                + c.currentStatus()
                + ")";
            restore();
            return ScenarioResult.fail(name, why + where);
        }
    }

    private static String gate() {
        return SiloScenarios.gate();
    }

    // ------------------------------------------------------------------------------------------------------------

    /**
     * A silo-only body counts down for longer than the stray-body grace (300 ticks) and still ignites: the stray check
     * once read the cell under the slab-high pad as the block below the floor, saw "no silo" on every check, and stood
     * the missile down after 300 ticks of any countdown (several aborted countdowns added up, too).
     */
    private static final class LongCountdown implements Scenario {

        private static final int LONG = 420;

        @Override
        public String name() {
            return "icbm.launch.silo_long_countdown";
        }

        @Override
        public String description() {
            return "A T3 body in a silo counts down for 420 ticks, longer than the 300 ticks the stray-body check waits, and ignites: the check finds the silo under the missile the whole time.";
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
            IcbmConfig.countdownTicks = LONG;
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 1200, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        // arm() stands a T1; swap in the silo-only body, which is the one the stray check watches.
                        r.m.tickets()
                            .release();
                        r.m.setDead();
                        r.m = SiloScenarios.stand(ctx, MissileTier.T3);
                        r.m.fill(r.m.capacityMb(), true);
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 60) {
                            return null;
                        }
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (r.m.phase() == FlightPhase.WARMUP) {
                            return ticks > mark[0] + DOORS + LONG + 60 ? r.fail("never ignited (stood down?)") : null;
                        }
                        r.out.checkEquals("liftoff", FlightPhase.CLIMB, r.m.phase());
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }
    }

    /** Doors open, countdown, liftoff, doors close once the missile is clear. */
    private static final class Sequence implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.silo_sequence";
        }

        @Override
        public String description() {
            return "Firing a silo: the doors open (40 ticks) and rest open while the 200-tick countdown runs; ignition lifts the missile through open doors; the doors swing shut only once its bottom is 4 blocks above the rim, then rest closed.";
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
            boolean[] sawClosing = { false };
            return ScenarioResult.pending(name(), 900, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        r.out.checkEquals("the doors start closed", SiloDoors.State.CLOSED, r.doors());
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        // Give the controller a moment to make its door entity, then fire.
                        if (r.doorEntity() == null && ticks < mark[0] + 60) {
                            return null;
                        }
                        r.out.check(r.doorEntity() != null, "a door entity stands at the rim");
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        r.out.checkEquals("the doors begin opening", SiloDoors.State.OPENING, r.doors());
                        r.out.checkEquals("the missile is in its warm-up", FlightPhase.WARMUP, r.m.phase());
                        r.out.checkEquals("the full countdown shows", COUNTDOWN, r.m.countdownLeft());
                        r.out.checkEquals(
                            "the control shows the countdown",
                            ControlStatus.COUNTDOWN,
                            r.c.currentStatus());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (ticks < mark[0] + DOORS + 5) {
                            r.out.check(r.m.phase() == FlightPhase.WARMUP, "no liftoff while the doors open");
                            return null;
                        }
                        r.out.checkEquals("the doors are open", SiloDoors.State.OPEN, r.doors());
                        r.out.checkEquals(
                            "a client would see the same state",
                            SiloDoors.State.OPEN,
                            r.doorEntity()
                                .doors()
                                .state());
                        r.out.check(
                            r.m.countdownLeft() > COUNTDOWN - 10 && r.m.countdownLeft() <= COUNTDOWN,
                            "the countdown has barely started (" + r.m.countdownLeft() + ")");
                        stage[0] = 3;
                        return null;
                    case 3:
                        // Doors stay open through the countdown and until ignition.
                        if (r.m.phase() == FlightPhase.WARMUP) {
                            r.out.check(r.doors() == SiloDoors.State.OPEN, "open during the countdown");
                            return ticks > mark[0] + DOORS + COUNTDOWN + 20 ? r.fail("never ignited") : null;
                        }
                        int took = ticks - mark[0];
                        r.out.check(
                            Math.abs(took - (DOORS + COUNTDOWN)) <= 3,
                            "ignition came after the doors and the countdown (" + took + " ticks)");
                        r.out.checkEquals("liftoff", FlightPhase.CLIMB, r.m.phase());
                        stage[0] = 4;
                        return null;
                    case 4: {
                        double bottom = r.m.posY;
                        SiloDoors.State s = r.doors();
                        if (!sawClosing[0] && (s == SiloDoors.State.CLOSING || s == SiloDoors.State.CLOSED)) {
                            sawClosing[0] = true;
                            r.out.check(
                                bottom >= r.rimTop() + IcbmConfig.doorCloseHeight - 1.5,
                                "the doors began to close only once the missile cleared the rim (bottom "
                                    + (bottom - r.rimTop())
                                    + " above)");
                        }
                        if (!sawClosing[0]) {
                            r.out.check(
                                s == SiloDoors.State.OPEN || bottom < r.rimTop() + 6,
                                "open while the missile is in the shaft");
                        }
                        if (s != SiloDoors.State.CLOSED) {
                            return ticks > mark[0] + DOORS + COUNTDOWN + 400 ? r.fail("the doors never closed") : null;
                        }
                        r.out.check(sawClosing[0], "the doors closed");
                        r.out.check(r.m.phase() == FlightPhase.CLIMB || r.m.phase() == FlightPhase.ARC, "still flying");
                        r.restore();
                        return r.out.build();
                    }
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Abort while the doors open, and again during the countdown: the missile stays armed and fueled. */
    private static final class Abort implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.silo_abort";
        }

        @Override
        public String description() {
            return "The Abort button works while the doors open and during the countdown: the countdown stops, the doors swing shut and the missile stays armed and fueled on its pad, ready to fire again.";
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
            return ScenarioResult.pending(name(), 1200, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        r.out.checkEquals("nothing to abort yet", false, r.c.abortFromButton(r.player));
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 15) {
                            return null;
                        }
                        r.out.checkEquals("still opening", SiloDoors.State.OPENING, r.doors());
                        r.out.checkEquals("aborted while the doors open", true, r.c.abortFromButton(r.player));
                        r.out.checkEquals("the missile stood down", FlightPhase.PAD, r.m.phase());
                        r.out.checkEquals("the doors turn around", SiloDoors.State.CLOSING, r.doors());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (r.doors() != SiloDoors.State.CLOSED) {
                            return ticks > mark[0] + 80 ? r.fail("the doors never closed") : null;
                        }
                        r.out.checkEquals("still fueled", r.m.capacityMb(), r.m.fuelMb());
                        r.out.check(r.m.onPad(), "still armed on the pad");
                        r.out.checkEquals(
                            "no chunk ticket is held",
                            0,
                            r.m.tickets()
                                .tickets());
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (ticks < mark[0] + 45) { // the Launch Control's cooldown
                            return null;
                        }
                        r.out.checkEquals("fired again", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (ticks < mark[0] + DOORS + 60) {
                            return null;
                        }
                        r.out.checkEquals("the doors are open", SiloDoors.State.OPEN, r.doors());
                        r.out.check(r.m.countdownLeft() > 0 && r.m.countdownLeft() < COUNTDOWN, "counting down");
                        r.out.checkEquals("aborted during the countdown", true, r.c.abortFromButton(r.player));
                        r.out.checkEquals("the missile stood down", FlightPhase.PAD, r.m.phase());
                        r.out.checkEquals("the doors close", SiloDoors.State.CLOSING, r.doors());
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    case 5:
                        if (r.doors() != SiloDoors.State.CLOSED) {
                            return ticks > mark[0] + 80 ? r.fail("the doors never closed") : null;
                        }
                        r.out.checkEquals("still fueled", r.m.capacityMb(), r.m.fuelMb());
                        r.out.checkEquals(
                            "the control is ready again after its cooldown",
                            true,
                            r.c.currentStatus() == ControlStatus.READY
                                || r.c.currentStatus() == ControlStatus.COOLDOWN);
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Once the countdown ends the launch cannot be stopped. */
    private static final class NoAbortAfterIgnition implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.no_abort_after_ignition";
        }

        @Override
        public String description() {
            return "After ignition the Abort button is refused, the control says so, and the missile flies on.";
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
            return ScenarioResult.pending(name(), 900, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        IcbmConfig.countdownTicks = 40;
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (r.m.phase() == FlightPhase.WARMUP) {
                            r.out.check(r.m.abortable(), "abortable before ignition");
                            return ticks > mark[0] + 200 ? r.fail("never ignited") : null;
                        }
                        r.out.checkEquals("ignited", FlightPhase.CLIMB, r.m.phase());
                        r.out.checkEquals("the missile refuses to abort", false, r.m.abort());
                        r.out.checkEquals("the button is refused", false, r.c.abortFromButton(r.player));
                        r.out.checkEquals(
                            "the site reports the ignition",
                            true,
                            r.silo()
                                .site()
                                .ignited());
                        r.out.checkEquals("and says so", ControlStatus.IGNITED, r.c.currentStatus());
                        r.out.checkEquals(
                            "the site refuses",
                            false,
                            r.silo()
                                .site()
                                .abort());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        r.out.check(r.m.phase() != FlightPhase.PAD && !r.m.isDead, "the missile flies on");
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** A Galacticraft pad: the same countdown and abort, no doors. */
    private static final class PadSequence implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.pad_sequence";
        }

        @Override
        public String description() {
            return "A pad launch runs the 200-tick countdown, can be aborted and fired again, ignites on time, and has no silo doors.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ctx.clearArea(8, 20);
            for (int a = -8; a <= 8; a++) {
                for (int b = -8; b <= 8; b++) {
                    ctx.setBlock(a, -1, b, Blocks.stone, 0);
                }
            }
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    ctx.setBlock(a, 0, b, micdoodle8.mods.galacticraft.core.blocks.GCBlocks.landingPad, 0);
                }
            }
            int old = IcbmConfig.countdownTicks;
            IcbmConfig.countdownTicks = COUNTDOWN;
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World w = ctx.world();
            EntityMissile m = EntityMissile.create(MissileTier.T1, w);
            m.setLocationAndAngles(
                ctx.originX() + 0.5,
                ctx.originY() + EntityMissile.PAD_SURFACE,
                ctx.originZ() + 0.5,
                0,
                0);
            m.configure(0, "condensed", 1, 1, null);
            w.spawnEntityInWorld(m);
            m.fill(m.capacityMb(), true);
            LaunchSite site = PadMissiles.site(w, ctx.originX(), ctx.originY(), ctx.originZ());
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 800, ticks -> {
                if (ticks < 5) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        out.checkEquals(
                            "launched",
                            LaunchResult.OK,
                            site.launch(ctx.originX() + 0.5, ctx.originZ() + 60.5, null));
                        out.checkEquals("the pad shows the full countdown", COUNTDOWN, site.countdownTicks());
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.checkEquals("no ignition yet", FlightPhase.WARMUP, m.phase());
                        out.checkEquals("aborted", true, site.abort());
                        out.checkEquals("armed on the pad again", FlightPhase.PAD, m.phase());
                        out.checkEquals("still fueled", m.capacityMb(), m.fuelMb());
                        out.checkEquals("no countdown left", -1, site.countdownTicks());
                        out.checkEquals(
                            "launched again",
                            LaunchResult.OK,
                            site.launch(ctx.originX() + 0.5, ctx.originZ() + 60.5, null));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (m.phase() == FlightPhase.WARMUP) {
                            return ticks > mark[0] + COUNTDOWN + 30 ? ScenarioResult.fail(name(), "never ignited")
                                : null;
                        }
                        int took = ticks - mark[0];
                        out.check(
                            Math.abs(took - COUNTDOWN) <= 3,
                            "ignition after the countdown, no door wait (" + took + ")");
                        out.checkEquals("liftoff", FlightPhase.CLIMB, m.phase());
                        out.checkEquals("the pad refuses an abort", false, site.abort());
                        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                            ctx.originX() - 14.0,
                            ctx.originY() - 4.0,
                            ctx.originZ() - 14.0,
                            ctx.originX() + 14.0,
                            ctx.originY() + 30.0,
                            ctx.originZ() + 14.0);
                        out.checkEquals(
                            "no door entity near a pad",
                            0,
                            w.getEntitiesWithinAABB(EntitySiloDoors.class, box)
                                .size());
                        IcbmConfig.countdownTicks = old;
                        m.tickets()
                            .release();
                        m.setDead();
                        ctx.clearArea(8, 20);
                        return out.build();
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The door state and the countdown survive a save. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.reload";
        }

        @Override
        public String description() {
            return "Saving the silo and the missile partway through the doors opening and again during the countdown, and loading them into fresh objects, restores the door state and timing and the countdown.";
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
            return ScenarioResult.pending(name(), 600, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 20) {
                            return null;
                        }
                        r.out.checkEquals("mid-opening", SiloDoors.State.OPENING, r.doors());
                        roundTrip(r, "mid-opening");
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (ticks < mark[0] + DOORS + 50) {
                            return null;
                        }
                        r.out.checkEquals("open", SiloDoors.State.OPEN, r.doors());
                        roundTrip(r, "during the countdown");
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }

        private static void roundTrip(Rig r, String when) {
            NBTTagCompound tag = new NBTTagCompound();
            r.silo()
                .saveNBTData(tag);
            SiloDoors a = r.silo()
                .doors();
            SiloDoors.State state = a.state();
            long start = a.start();
            int dur = a.duration();
            double angle = a.progress(
                r.ctx.world()
                    .getTotalWorldTime());
            a.reset(); // as if the silo had just loaded with nothing saved
            a.restore(SiloDoors.State.CLOSED, 0, 1);
            r.silo()
                .loadNBTData(tag);
            SiloDoors b = a;
            r.out.checkEquals("door state " + when, state, b.state());
            r.out.checkEquals("door start " + when, start, b.start());
            r.out.checkEquals("door duration " + when, dur, b.duration());
            long now = r.ctx.world()
                .getTotalWorldTime();
            r.out.check(angle == b.progress(now), "same door angle " + when);
            NBTTagCompound mt = new NBTTagCompound();
            r.m.writeToNBT(mt);
            EntityMissile twin = EntityMissile.create(MissileTier.T1, r.ctx.world());
            twin.readFromNBT(mt);
            r.out.checkEquals("phase " + when, r.m.phase(), twin.phase());
            r.out.checkEquals("countdown " + when, r.m.countdownLeft(), twin.countdownLeft());
            r.out.checkEquals("lead " + when, r.m.warmupLead(), twin.warmupLead());
            r.out.checkEquals("total " + when, r.m.warmupTotal(), twin.warmupTotal());
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** A silo that comes apart mid-sequence stands the countdown down; after ignition the missile flies on. */
    private static final class Unform implements Scenario {

        @Override
        public String name() {
            return "icbm.launch.unform";
        }

        @Override
        public String description() {
            return "Breaking the silo during the countdown, for longer than the grace period, aborts the launch (missile armed on its pad, doors reset and the door entity gone); breaking it after ignition leaves the missile flying.";
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
            return ScenarioResult.pending(name(), 1800, ticks -> {
                if (ticks < SiloScenarios.WARMUP) {
                    return null;
                }
                World w = ctx.world();
                switch (stage[0]) {
                    case 0:
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms");
                        r.arm();
                        // Longer than the grace period, so the stand-down comes before ignition.
                        IcbmConfig.countdownTicks = MTEMissileSilo.UNFORMED_GRACE_TICKS + 200;
                        r.out.checkEquals("fired", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + DOORS + 30) {
                            return null;
                        }
                        r.out.checkEquals("counting down with the doors open", SiloDoors.State.OPEN, r.doors());
                        w.setBlock(r.x() - 1, r.y() + 2, r.z(), Blocks.stone, 0, 3);
                        r.out.check(!SiloScenarios.formed(ctx), "the silo is broken");
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (ticks < mark[0] + MTEMissileSilo.UNFORMED_GRACE_TICKS / 2) {
                            r.out.check(r.m.phase() == FlightPhase.WARMUP, "a short gap does not stop the countdown");
                            return null;
                        }
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (r.m.phase() == FlightPhase.WARMUP) {
                            return ticks > mark[0] + MTEMissileSilo.UNFORMED_GRACE_TICKS + 40
                                ? r.fail("the countdown never stood down")
                                : null;
                        }
                        r.out.checkEquals("the missile stands down armed", FlightPhase.PAD, r.m.phase());
                        r.out.checkEquals("still fueled", r.m.capacityMb(), r.m.fuelMb());
                        r.out.checkEquals("the doors reset", SiloDoors.State.CLOSED, r.doors());
                        r.out.check(r.doorEntity() == null, "the door entity is gone");
                        // Mend it, wait for the cooldown, and fire again; this time break it after ignition.
                        w.setBlock(
                            r.x() - 1,
                            r.y() + 2,
                            r.z(),
                            gregtech.api.GregTechAPI.sBlockConcretes,
                            SiloConcrete.LIGHT,
                            2);
                        r.out.check(SiloScenarios.formed(ctx), "the silo forms again");
                        IcbmConfig.countdownTicks = 40;
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (ticks < mark[0] + 45) {
                            return null;
                        }
                        r.out.checkEquals("fired again", ControlStatus.FIRED, r.c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    case 5:
                        if (r.m.phase() != FlightPhase.CLIMB) {
                            return ticks > mark[0] + 200 ? r.fail("never ignited") : null;
                        }
                        w.setBlock(r.x() - 1, r.y() + 2, r.z(), Blocks.stone, 0, 3);
                        r.out.check(!SiloScenarios.formed(ctx), "the silo is broken after ignition");
                        mark[0] = ticks;
                        stage[0] = 6;
                        return null;
                    case 6:
                        if (ticks < mark[0] + 30) {
                            return null;
                        }
                        r.out.check(r.m.phase() != FlightPhase.PAD && !r.m.isDead, "the missile flies on");
                        r.restore();
                        return r.out.build();
                    default:
                        return null;
                }
            });
        }
    }
}
