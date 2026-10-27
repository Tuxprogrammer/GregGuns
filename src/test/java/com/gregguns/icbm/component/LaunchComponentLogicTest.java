package com.gregguns.icbm.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.LaunchGate;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.missile.MissileStatus;
import com.gregguns.icbm.missile.MissileTier;

/** The {@code gg_silo} and {@code gg_launch_control} calls over a fake machine (GGI-01-02, 04, 05). */
class LaunchComponentLogicTest {

    /** A launcher that runs the real {@link LaunchGate} like the Launch Control does. */
    private static final class Fake implements LaunchView {

        LaunchTarget target = LaunchTarget.NONE;
        MissileStatus missile = MissileStatus.NONE;
        boolean energy = true;
        boolean cooling;
        TargetStick.Outcome stick = TargetStick.Outcome.NO_STICK;
        int launches;

        @Override
        public LaunchTarget launchTarget() {
            return target;
        }

        @Override
        public void aimAt(int x, int z) {
            target = LaunchTarget.of(x, z);
        }

        @Override
        public TargetStick.Outcome loadTargetFromStick() {
            if (stick == TargetStick.Outcome.LOADED) {
                target = LaunchTarget.of(500, 500);
            }
            return stick;
        }

        @Override
        public MissileStatus missileStatus() {
            return missile;
        }

        @Override
        public String fuelFluid() {
            return missile.present() ? "nitrofuel" : "";
        }

        @Override
        public ControlStatus launchStatus() {
            int min = missile.present() ? missile.tier()
                .minRange() : 0;
            int max = missile.present() ? 1000 : 0;
            return LaunchGate.check(
                target,
                true,
                energy,
                cooling,
                missile.present(),
                missile.armed(),
                missile.fueled(),
                target.distanceFrom(0, 0),
                min,
                max);
        }

        @Override
        public ControlStatus launchNow() {
            ControlStatus s = launchStatus();
            if (s != ControlStatus.READY) {
                return s;
            }
            launches++;
            return ControlStatus.FIRED;
        }

        @Override
        public int launches() {
            return launches;
        }

        @Override
        public Map<String, Object> launchInfo() {
            return Collections.<String, Object>singletonMap("euPerShot", 300);
        }
    }

    private static MissileStatus loaded() {
        return new MissileStatus(true, MissileTier.T1, 2000, 2000, 0, "he", 1);
    }

    @Test
    void launchGoesThroughTheGateAndRefusalsCostNothing() {
        Fake f = new Fake();
        LaunchComponentLogic c = new LaunchComponentLogic(f);
        assertEquals(
            "no_target",
            c.launch()
                .reason());
        assertTrue(
            c.setTarget(300, 0)
                .isOk());
        assertEquals(
            "no_missile",
            c.launch()
                .reason());
        f.missile = loaded();
        f.energy = false;
        assertEquals(
            "no_power",
            c.launch()
                .reason());
        f.energy = true;
        f.cooling = true;
        assertEquals(
            "cooldown",
            c.launch()
                .reason());
        assertEquals(0, f.launches, "a refused call launches nothing");
        f.cooling = false;
        assertTrue(
            c.canLaunch()
                .isOk());
        assertTrue(
            c.launch()
                .isOk());
        assertEquals(1, f.launches);
    }

    @Test
    void everyRefusalReasonIsAControlStatusWordTheGuiShows() {
        // The reply word is the lowercase status name, the suffix of the status lang key (ICBM-04-16).
        for (ControlStatus s : ControlStatus.values()) {
            assertTrue(
                s.langKey()
                    .endsWith(
                        "." + s.name()
                            .toLowerCase(java.util.Locale.ROOT)));
        }
    }

    @Test
    void tooCloseAndOutOfRangeAreRefused() {
        Fake f = new Fake();
        f.missile = loaded();
        LaunchComponentLogic c = new LaunchComponentLogic(f);
        c.setTarget(5, 5);
        assertEquals(
            "too_close",
            c.launch()
                .reason());
        c.setTarget(5000, 0);
        assertEquals(
            "out_of_range",
            c.launch()
                .reason());
    }

    @Test
    void aTargetPastTheBorderIsRefusedAndChangesNothing() {
        Fake f = new Fake();
        LaunchComponentLogic c = new LaunchComponentLogic(f);
        c.setTarget(10, 20);
        Reply bad = c.setTarget(31_000_000, 0);
        assertFalse(bad.isOk());
        assertEquals("bad_target", bad.reason());
        assertEquals(10, f.target.x());
        assertFalse(
            c.setTarget(Double.NaN, 0)
                .isOk());
    }

    @Test
    void recordsAreNilWhenThereIsNothingAndFullWhenThereIs() {
        Fake f = new Fake();
        LaunchComponentLogic c = new LaunchComponentLogic(f);
        assertNull(c.getMissile());
        assertNull(c.getTarget());
        assertEquals(
            0,
            c.getFuel()
                .get("required"));
        f.missile = loaded();
        c.setTarget(40, -7);
        assertEquals(
            1,
            c.getMissile()
                .get("tier"));
        assertEquals(
            true,
            c.getMissile()
                .get("fueled"));
        assertEquals(
            40,
            c.getTarget()
                .get("x"));
        assertEquals(
            -7,
            c.getTarget()
                .get("z"));
        assertEquals(
            "nitrofuel",
            c.getFuel()
                .get("fluid"));
        assertEquals(
            2000,
            c.getFuel()
                .get("amount"));
        assertEquals(
            "no_target".equals(
                c.getInfo()
                    .get("status")),
            false);
        assertEquals(
            300,
            c.getInfo()
                .get("euPerShot"));
    }

    @Test
    void stickOutcomesBecomeReasons() {
        Fake f = new Fake();
        LaunchComponentLogic c = new LaunchComponentLogic(f);
        assertEquals(
            "no_stick",
            c.setTargetFromStick()
                .reason());
        f.stick = TargetStick.Outcome.WRONG_DIMENSION;
        assertEquals(
            "wrong_dimension",
            c.setTargetFromStick()
                .reason());
        f.stick = TargetStick.Outcome.LOADED;
        assertTrue(
            c.setTargetFromStick()
                .isOk());
        assertEquals(
            500,
            c.getTarget()
                .get("x"));
    }

    @Test
    void launcherSignalsFireOnTransitionsOnly() {
        SignalTracker t = new SignalTracker("launcher_fired", "launcher_ready");
        assertTrue(
            t.sample(true, 0)
                .isEmpty(),
            "the first sample only seeds");
        assertTrue(
            t.sample(true, 0)
                .isEmpty());
        assertEquals(Collections.singletonList("launcher_fired"), t.sample(false, 1));
        assertTrue(
            t.sample(false, 1)
                .isEmpty());
        assertEquals(Collections.singletonList("launcher_ready"), t.sample(true, 1));
    }
}
